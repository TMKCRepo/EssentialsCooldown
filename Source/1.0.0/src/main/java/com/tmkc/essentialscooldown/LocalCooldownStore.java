package com.tmkc.essentialscooldown;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-server cooldown store: an in-memory map keyed by player, then by
 * command group. Thread-safe, because Folia/ShreddedPaper may run commands on
 * several region threads at once.
 */
final class LocalCooldownStore implements CooldownStore {

    /** Player UUID -> (group key -> epoch millis when the cooldown expires). */
    private final Map<UUID, Map<String, Long>> expiry = new ConcurrentHashMap<>();

    @Override
    public long expiry(UUID playerId, String groupKey) {
        return expiry.getOrDefault(playerId, Map.of()).getOrDefault(groupKey, 0L);
    }

    @Override
    public void start(UUID playerId, String groupKey, long expiresAtMillis) {
        expiry.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(groupKey, expiresAtMillis);
    }

    @Override
    public void clearAll() {
        expiry.clear();
    }

    @Override
    public void onJoin(Player player) {
        // Nothing to load: cooldowns only live for this server's lifetime.
    }

    @Override
    public void onQuit(UUID playerId) {
        expiry.remove(playerId);
    }

    @Override
    public String describe() {
        return "local (in-memory, per-server)";
    }
}
