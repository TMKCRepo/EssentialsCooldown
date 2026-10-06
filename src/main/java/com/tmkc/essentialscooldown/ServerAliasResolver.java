package com.tmkc.essentialscooldown;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves command aliases from the running server so a hand-written config
 * entry cannot be dodged through an alias the author did not know about.
 *
 * <p>Deliberately portable: it uses {@code Server#getPluginCommand} (present on
 * Bukkit, Spigot, Paper, Folia, ShreddedPaper and MultiPaper) as the primary
 * path, and only falls back to reflective {@code CommandMap} access for
 * non-plugin commands or servers that do not expose the command map. Nothing
 * here references an implementation-only type at compile time.
 */
final class ServerAliasResolver implements AliasResolver {

    private final JavaPlugin plugin;

    ServerAliasResolver(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Set<String> aliasesFor(String command) {
        String wanted = command.toLowerCase(Locale.ROOT);
        Set<String> labels = new LinkedHashSet<>();

        // 1. Portable path: PluginCommand#getName + #getAliases.
        try {
            PluginCommand pluginCommand = plugin.getServer().getPluginCommand(wanted);
            if (pluginCommand != null) {
                addLabel(labels, pluginCommand.getName());
                addLabels(labels, pluginCommand.getAliases());
                if (!labels.isEmpty()) {
                    return labels;
                }
            }
        } catch (Throwable ignored) {
            // Fall through to the reflective path.
        }

        // 2. Fallback: reach the command map reflectively and group every label
        //    that points at the same Command object.
        try {
            CommandMap commandMap = resolveCommandMap();
            if (commandMap == null) {
                return labels;
            }
            Map<String, Command> known = resolveKnownCommands(commandMap);
            if (known == null || known.isEmpty()) {
                return labels;
            }

            Command target = known.get(wanted);
            if (target == null) {
                for (Map.Entry<String, Command> entry : known.entrySet()) {
                    if (stripNamespace(entry.getKey()).equals(wanted)) {
                        target = entry.getValue();
                        break;
                    }
                }
            }
            if (target == null) {
                return labels;
            }
            for (Map.Entry<String, Command> entry : known.entrySet()) {
                if (entry.getValue() == target) {
                    addLabel(labels, stripNamespace(entry.getKey()));
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().fine("Alias auto-detection unavailable for /" + command
                    + ": " + t.getMessage());
        }
        return labels;
    }

    /** {@code Server#getCommandMap()} (Paper), else CraftBukkit's {@code getCommandMap} field. */
    private CommandMap resolveCommandMap() {
        try {
            Method method = plugin.getServer().getClass().getMethod("getCommandMap");
            Object value = method.invoke(plugin.getServer());
            if (value instanceof CommandMap map) {
                return map;
            }
        } catch (Throwable ignored) {
            // Not a Paper-style server; try the field below.
        }
        try {
            Field field = plugin.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            Object value = field.get(plugin.getServer());
            if (value instanceof CommandMap map) {
                return map;
            }
        } catch (Throwable ignored) {
            // Neither path exists.
        }
        return null;
    }

    /** {@code SimpleCommandMap#getKnownCommands()} via reflection, when available. */
    @SuppressWarnings("unchecked")
    private Map<String, Command> resolveKnownCommands(CommandMap commandMap) {
        try {
            Method method = commandMap.getClass().getMethod("getKnownCommands");
            Object value = method.invoke(commandMap);
            if (value instanceof Map<?, ?> map) {
                return (Map<String, Command>) map;
            }
        } catch (Throwable ignored) {
            // Not exposed.
        }
        return null;
    }

    private static void addLabels(Set<String> labels, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            addLabel(labels, value);
        }
    }

    private static void addLabel(Set<String> labels, String label) {
        if (label == null || label.isBlank()) {
            return;
        }
        labels.add(stripNamespace(label));
    }

    private static String stripNamespace(String label) {
        int colon = label.indexOf(':');
        String bare = colon >= 0 ? label.substring(colon + 1) : label;
        return bare.toLowerCase(Locale.ROOT);
    }
}
