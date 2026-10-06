package com.tmkc.essentialscooldown;

import com.github.puregero.multilib.MultiLib;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Real bridge onto MultiLib. Only referenced when MultiPaper is present. */
final class MultiLibBridge implements MultiPaperBridge {

    @Override
    public boolean isMultiPaper() {
        return MultiLib.isMultiPaper();
    }

    @Override
    public CompletableFuture<Long> set(String key, long value) {
        return MultiLib.getDataStorage().set(key, value);
    }

    @Override
    public CompletableFuture<Map<String, String>> list(String prefix) {
        return MultiLib.getDataStorage().list(prefix);
    }

    @Override
    public void notify(String channel, String data) {
        MultiLib.notify(channel, data);
    }

    @Override
    public void onString(Plugin plugin, String channel, Consumer<String> handler) {
        MultiLib.onString(plugin, channel, handler);
    }
}
