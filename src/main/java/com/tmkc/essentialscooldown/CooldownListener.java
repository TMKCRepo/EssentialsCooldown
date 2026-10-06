package com.tmkc.essentialscooldown;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Intercepts configured commands before EssentialsX runs them, enforcing a
 * configurable cooldown.
 *
 * <p>Uses {@link PlayerCommandPreprocessEvent} at LOWEST priority so the check
 * runs before any other plugin's handler and can cancel the command outright.
 * The listener is {@code ignoreCancelled = true}: if another plugin already
 * blocked the command, we do not consume the cooldown or send our own message.
 *
 * <p>Messages use {@link ChatColor} rather than Adventure so the plugin works
 * identically on Bukkit, Spigot, Paper, Folia, ShreddedPaper and MultiPaper.
 * Cooldown state lives in a {@link CooldownStore}, which is shared between
 * servers under MultiPaper.
 */
final class CooldownListener implements Listener {

    private final ConfigManager config;
    private final CooldownStore store;

    CooldownListener(ConfigManager config, CooldownStore store) {
        this.config = config;
        this.store = store;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        String label = commandLabel(event.getMessage());
        if (label == null) {
            return;
        }
        CommandGroup group = config.groupFor(label);
        if (group == null) {
            return;
        }

        if (player.hasPermission("essentialscooldown.bypass")) {
            return;
        }

        long seconds = group.secondsFor(player);

        // 0 = the command is disabled for this rank.
        if (seconds <= 0L) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You do not have access to /" + group.key() + ".");
            return;
        }

        long now = System.currentTimeMillis();
        long expiresAt = store.expiry(player.getUniqueId(), group.key());

        if (expiresAt > now) {
            long remaining = (expiresAt - now + 999L) / 1000L; // round up to whole seconds
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You must wait " + ChatColor.YELLOW
                    + formatDuration(remaining) + ChatColor.RED
                    + " before using /" + group.key() + " again.");
            return;
        }

        // Allowed: start the cooldown.
        store.start(player.getUniqueId(), group.key(), now + seconds * 1000L);
    }

    /**
     * Extract the command label from a raw message like {@code /heal Steve} or
     * {@code /minecraft:heal}. Returns lower-case, namespace-stripped, or
     * {@code null} if the message is not a command.
     */
    private static String commandLabel(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty() || rawMessage.charAt(0) != '/') {
            return null;
        }
        String body = rawMessage.substring(1).trim();
        if (body.isEmpty()) {
            return null;
        }
        // Split on ANY whitespace (space or tab) - Minecraft treats both as separators.
        int end = 0;
        while (end < body.length() && !Character.isWhitespace(body.charAt(end))) {
            end++;
        }
        body = body.substring(0, end);
        int colon = body.indexOf(':');
        if (colon >= 0) {
            body = body.substring(colon + 1);
        }
        return body.toLowerCase(Locale.ROOT);
    }

    /** Render a second count as e.g. {@code 4m 12s} or {@code 45s}. */
    private static String formatDuration(long totalSeconds) {
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0L && seconds > 0L) {
            return minutes + "m " + seconds + "s";
        }
        if (minutes > 0L) {
            return minutes + "m";
        }
        return seconds + "s";
    }
}
