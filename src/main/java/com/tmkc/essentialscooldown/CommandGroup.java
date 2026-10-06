package com.tmkc.essentialscooldown;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * One cooldown group: a command plus its aliases, and an ORDERED list of
 * rank cooldowns.
 *
 * <p>Ranks are declared per command in config.yml, most specific first. The
 * permission node granting a rank named {@code hyper} is
 * {@code essentialscooldown.hyper}, so adding a rank is a config change only.
 *
 * <p>When a player holds none of the listed rank nodes, the group's
 * {@code fallback-rank} value is used.
 */
final class CommandGroup {

    /** Every rank permission node starts with this. */
    static final String PERMISSION_PREFIX = "essentialscooldown.";

    private final String key;
    private final Set<String> commands;
    /** Rank (lower-case) -> seconds, in the order they appear in the config. */
    private final Map<String, Long> secondsByRank;
    /** Seconds used when the player holds none of the listed rank nodes. */
    private final long fallbackSeconds;

    CommandGroup(String key, Set<String> commands, Map<String, Long> secondsByRank, long fallbackSeconds) {
        this.key = key;
        this.commands = Set.copyOf(commands);
        Map<String, Long> copy = new LinkedHashMap<>();
        secondsByRank.forEach((rank, seconds) -> copy.put(rank, Math.max(0L, seconds)));
        this.secondsByRank = Collections.unmodifiableMap(copy);
        this.fallbackSeconds = Math.max(0L, fallbackSeconds);
    }

    /** The config key, e.g. {@code heal}. */
    String key() {
        return key;
    }

    /** True if {@code label} (lower-case, namespace already stripped) is this command or one of its aliases. */
    boolean matches(String label) {
        return commands.contains(label);
    }

    /** Seconds configured for {@code rank}, or {@code null} if that rank is not listed for this command. */
    Long secondsForRank(String rank) {
        return secondsByRank.get(rank);
    }

    /**
     * Cooldown for this player, in seconds. Ranks are checked in the order they
     * are written in the config and the first one the player holds wins. A player
     * holding none of them gets the fallback value.
     *
     * <p>A return value of {@code 0} means the command is not allowed at all.
     */
    long secondsFor(Player player) {
        for (Map.Entry<String, Long> entry : secondsByRank.entrySet()) {
            if (player.hasPermission(PERMISSION_PREFIX + entry.getKey())) {
                return entry.getValue();
            }
        }
        return fallbackSeconds;
    }

    /** e.g. {@code player=300s, hyper=60s} in config order. */
    String describe() {
        StringBuilder sb = new StringBuilder();
        secondsByRank.forEach((rank, seconds) -> {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(rank).append('=').append(seconds).append('s');
        });
        return sb.toString();
    }
}
