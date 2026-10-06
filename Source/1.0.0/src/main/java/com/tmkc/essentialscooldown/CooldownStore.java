package com.tmkc.essentialscooldown;

import java.util.UUID;

/**
 * Where cooldowns live.
 *
 * <p>On a single server this is a plain in-memory map. Under MultiPaper it is a
 * shared, persistent store so a cooldown started on one server still applies
 * after the player moves to another server (or a server restarts).
 */
interface CooldownStore {

    /**
     * Current expiry time (epoch millis) for this player and command group, or
     * {@code 0} if they have no active cooldown. Must be safe to call from the
     * command thread.
     */
    long expiry(UUID playerId, String groupKey);

    /** Record that a cooldown started and expires at {@code expiresAtMillis}. */
    void start(UUID playerId, String groupKey, long expiresAtMillis);

    /** Forget every locally cached cooldown (used on reload). */
    void clearAll();

    /** A player joined: load any cooldowns they already have. */
    void onJoin(org.bukkit.entity.Player player);

    /** A player left. */
    void onQuit(UUID playerId);

    /** Human-readable description for the startup log. */
    String describe();
}
