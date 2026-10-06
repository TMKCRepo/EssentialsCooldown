package com.tmkc.essentialscooldown;

import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The slice of MultiLib this plugin uses, behind an interface so the sync logic
 * can be exercised without a MultiPaper server.
 */
interface MultiPaperBridge {

    /** True when the running server is MultiPaper. */
    boolean isMultiPaper();

    /** Persist a value in the shared, cross-server store. */
    CompletableFuture<Long> set(String key, long value);

    /** Read every shared entry whose key starts with {@code prefix}. */
    CompletableFuture<Map<String, String>> list(String prefix);

    /** Broadcast a payload to the same channel on every other server. */
    void notify(String channel, String data);

    /** Receive payloads broadcast on {@code channel} by other servers. */
    void onString(Plugin plugin, String channel, Consumer<String> handler);
}
