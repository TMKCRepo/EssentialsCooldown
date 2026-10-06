package com.tmkc.essentialscooldown;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Detects the running server platform so behaviour can degrade gracefully.
 *
 * <p>Supports Bukkit, Spigot, Paper, Folia, ShreddedPaper and MultiPaper. The
 * threaded-region check follows Paper's own guidance: it tests for the region
 * API method rather than a Folia-only class name, so it also matches forks such
 * as ShreddedPaper.
 */
final class PlatformDetector {

    private final String brand;
    private final boolean regionScheduler;

    PlatformDetector(JavaPlugin plugin) {
        String detected;
        try {
            detected = plugin.getServer().getName();
        } catch (Throwable t) {
            detected = null;
        }
        this.brand = (detected == null || detected.isBlank()) ? "Unknown" : detected;
        this.regionScheduler = detectRegionScheduler();
    }

    /** Server brand, e.g. {@code Paper}, {@code Folia}, {@code Purpur}. */
    String brand() {
        return brand;
    }

    /** True on Folia and forks exposing Paper's region scheduler (ShreddedPaper). */
    boolean hasRegionScheduler() {
        return regionScheduler;
    }

    /** One-line summary for the startup log. */
    String describe() {
        return brand + (regionScheduler ? " (region-scheduler API present)" : "");
    }

    /**
     * Paper's recommended probe: presence of the region API method, which is true
     * on Folia and ShreddedPaper and false on plain Bukkit/Spigot/Paper.
     */
    private static boolean detectRegionScheduler() {
        try {
            Bukkit.class.getMethod("getRegionScheduler");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
