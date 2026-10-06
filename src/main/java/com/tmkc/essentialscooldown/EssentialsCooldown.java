package com.tmkc.essentialscooldown;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class EssentialsCooldown extends JavaPlugin implements Listener {

    private static final String PREFIX = ChatColor.GOLD + "[EssentialsCooldown] " + ChatColor.RESET;

    private ConfigManager configManager;
    private CooldownListener cooldownListener;
    private CooldownStore cooldownStore;
    private PlatformDetector platform;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        platform = new PlatformDetector(this);

        if (getServer().getPluginManager().getPlugin("Essentials") == null) {
            getLogger().severe("EssentialsX was not found. Disabling EssentialsCooldown.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(getLogger(), new ServerAliasResolver(this));
        int groups = configManager.load(getConfig());

        cooldownStore = CooldownStoreFactory.create(this, getLogger());
        cooldownListener = new CooldownListener(configManager, cooldownStore);
        getServer().getPluginManager().registerEvents(cooldownListener, this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Running on " + platform.describe() + ".");
        getLogger().info("Cooldown storage: " + cooldownStore.describe() + ".");
        getLogger().info("Enabled \u2014 enforcing cooldowns for " + groups + " command group(s).");
        getLogger().info("Fallback rank: "
                + (configManager.fallbackRank().isEmpty() ? "(none - last listed rank)"
                        : configManager.fallbackRank()));
        for (CommandGroup group : configManager.allGroups()) {
            getLogger().info("  " + group.key() + ": " + group.describe());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("EssentialsCooldown disabled.");
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        if (cooldownStore != null) {
            cooldownStore.onJoin(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        if (cooldownStore != null) {
            cooldownStore.onQuit(event.getPlayer().getUniqueId());
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("essentialscooldown")) {
            return false;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(PREFIX + ChatColor.GRAY + "Usage: /" + label + " reload");
            return true;
        }

        reloadConfig();
        int groups = configManager.load(getConfig());
        cooldownStore.clearAll();
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Reloaded \u2014 " + groups
                + " command group(s) active, local cooldowns cleared.");
        getLogger().info(sender.getName() + " reloaded the configuration (" + groups + " group(s)).");
        return true;
    }
}
