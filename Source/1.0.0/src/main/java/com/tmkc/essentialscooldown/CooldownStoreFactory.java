package com.tmkc.essentialscooldown;

import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/** Chooses the cooldown store that matches the running server. */
final class CooldownStoreFactory {

    private CooldownStoreFactory() {
    }

    static CooldownStore create(Plugin plugin, Logger logger) {
        return create(plugin, logger, new MultiLibBridge());
    }

    /** Overload for tests: inject the MultiPaper bridge. */
    static CooldownStore create(Plugin plugin, Logger logger, MultiPaperBridge bridge) {
        if (isMultiPaper(bridge)) {
            logger.info("MultiPaper detected - cooldowns are shared and persistent across servers.");
            return new MultiPaperCooldownStore(bridge, plugin, logger);
        }
        return new LocalCooldownStore();
    }

    private static boolean isMultiPaper(MultiPaperBridge bridge) {
        try {
            return bridge.isMultiPaper();
        } catch (Throwable t) {
            return false;
        }
    }
}
