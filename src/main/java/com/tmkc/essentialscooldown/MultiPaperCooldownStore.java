package com.tmkc.essentialscooldown;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MultiPaper cooldown store: cooldowns are shared between every server in the
 * network and persist across restarts.
 *
 * <p>Reads are served from a local cache so the command check can decide
 * synchronously (the command event cannot wait on I/O). Writes update the cache
 * immediately, persist to MultiLib's shared data storage, and broadcast a
 * notification so the other servers update instantly.
 *
 * <p>A player's commands are only ever processed by the server they are
 * connected to, so there is no split-brain on the enforcement decision; the
 * shared state exists so the cooldown follows the player when they switch
 * servers.
 */
final class MultiPaperCooldownStore implements CooldownStore {

    static final String KEY_PREFIX = "essentialscooldown:cd:";
    static final String CHANNEL = "essentialscooldown:sync";
    private static final String FIELD_SEPARATOR = "|";

    private final MultiPaperBridge bridge;
    private final Logger logger;
    /** Player UUID -> (group key -> epoch millis when the cooldown expires). */
    private final Map<UUID, Map<String, Long>> cache = new ConcurrentHashMap<>();

    MultiPaperCooldownStore(MultiPaperBridge bridge, Plugin plugin, Logger logger) {
        this.bridge = bridge;
        this.logger = logger;
        try {
            bridge.onString(plugin, CHANNEL, this::onSyncMessage);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Could not register the MultiPaper sync channel", t);
        }
    }

    @Override
    public long expiry(UUID playerId, String groupKey) {
        return cache.getOrDefault(playerId, Map.of()).getOrDefault(groupKey, 0L);
    }

    @Override
    public void start(UUID playerId, String groupKey, long expiresAtMillis) {
        // 1. Local cache first: the decision on this server is immediate.
        cache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(groupKey, expiresAtMillis);
        // 2. Persist to the shared store so it survives a server switch or restart.
        try {
            bridge.set(key(playerId, groupKey), expiresAtMillis);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Could not persist cooldown for " + playerId, t);
        }
        // 3. Tell the other servers so they update instantly.
        try {
            bridge.notify(CHANNEL, playerId + FIELD_SEPARATOR + groupKey
                    + FIELD_SEPARATOR + expiresAtMillis);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Could not broadcast cooldown for " + playerId, t);
        }
    }

    /** Another server started a cooldown; mirror it locally. */
    void onSyncMessage(String data) {
        if (data == null) {
            return;
        }
        String[] parts = data.split("\\|");
        if (parts.length != 3) {
            return;
        }
        try {
            UUID playerId = UUID.fromString(parts[0]);
            long expiresAt = Long.parseLong(parts[2]);
            cache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(parts[1], expiresAt);
        } catch (RuntimeException ignored) {
            // Malformed or not ours: ignore.
        }
    }

    @Override
    public void onJoin(Player player) {
        UUID playerId = player.getUniqueId();
        String prefix = KEY_PREFIX + playerId + ":";
        try {
            CompletableFuture<Map<String, String>> future = bridge.list(prefix);
            future.thenAccept(entries -> {
                Map<String, Long> mine = new LinkedHashMap<>();
                long now = System.currentTimeMillis();
                entries.forEach((key, value) -> {
                    String group = key.startsWith(prefix) ? key.substring(prefix.length()) : key;
                    try {
                        long expiresAt = Long.parseLong(value);
                        if (expiresAt > now) {
                            mine.put(group, expiresAt);
                        }
                    } catch (NumberFormatException ignored) {
                        // Not a timestamp: skip.
                    }
                });
                if (mine.isEmpty()) {
                    cache.remove(playerId);
                } else {
                    cache.put(playerId, new ConcurrentHashMap<>(mine));
                }
            }).exceptionally(t -> {
                logger.log(Level.FINE, "Could not load cooldowns for " + playerId, t);
                return null;
            });
        } catch (Throwable t) {
            logger.log(Level.FINE, "Could not load cooldowns for " + playerId, t);
        }
    }

    @Override
    public void onQuit(UUID playerId) {
        // Drop the local copy only: the shared entry must survive so the cooldown
        // still applies if the player rejoins on another server.
        cache.remove(playerId);
    }

    @Override
    public void clearAll() {
        cache.clear();
    }

    @Override
    public String describe() {
        return "MultiPaper (shared + persistent across servers)";
    }

    private static String key(UUID playerId, String groupKey) {
        return KEY_PREFIX + playerId + ":" + groupKey;
    }

    /** True when the running server is MultiPaper; never throws. */
    static boolean isAvailable() {
        try {
            return new MultiLibBridge().isMultiPaper();
        } catch (Throwable t) {
            return false;
        }
    }
}
