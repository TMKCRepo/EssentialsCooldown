package com.tmkc.essentialscooldown;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Reads a {@link FileConfiguration} into {@link CommandGroup}s.
 *
 * <p>Ranks are listed per command, most specific first:
 *
 * <pre>
 * commands:
 *   heal:
 *     aliases: [eheal]
 *     cooldowns:
 *       - hyper: 60
 *       - player: 300
 * </pre>
 *
 * <p>The permission node for a rank is {@code essentialscooldown.<rank>}.
 * {@code fallback-rank} names the rank applied to players holding none of the
 * listed nodes.
 */
final class ConfigManager {

    /** Rank names become permission segments, so keep them simple and safe. */
    private static final Pattern VALID_RANK = Pattern.compile("[a-z0-9_-]+");

    private final Logger logger;
    private final AliasResolver aliasResolver;
    private final Map<String, CommandGroup> groups = new LinkedHashMap<>();
    /** Lower-cased command label/alias -> owning group. */
    private final Map<String, CommandGroup> lookup = new LinkedHashMap<>();
    private String fallbackRank = "";

    /** No auto-detection: only the aliases written in the config are used. */
    ConfigManager(Logger logger) {
        this(logger, null);
    }

    /**
     * @param aliasResolver consulted for the server's real alias list of each
     *                      command, so a hand-written entry cannot be dodged
     *                      through an alias the author did not know. May be null.
     */
    ConfigManager(Logger logger, AliasResolver aliasResolver) {
        this.logger = logger;
        this.aliasResolver = aliasResolver;
    }

    /** (Re)load from the given configuration. Returns the number of command groups loaded. */
    int load(FileConfiguration config) {
        groups.clear();
        lookup.clear();
        loadFallbackRank(config);

        ConfigurationSection commands = config.getConfigurationSection("commands");
        if (commands == null) {
            logger.warning("config.yml has no 'commands' section - nothing to enforce.");
            return 0;
        }

        for (String key : commands.getKeys(false)) {
            ConfigurationSection section = commands.getConfigurationSection(key);
            if (section == null) {
                logger.warning("Skipping 'commands." + key + "': not a section.");
                continue;
            }

            Map<String, Long> perRank = readCooldowns(key, section);
            if (perRank.isEmpty()) {
                logger.warning("Skipping 'commands." + key + "': no usable cooldowns.");
                continue;
            }

            long fallback = resolveFallback(key, perRank);

            Set<String> labels = new LinkedHashSet<>();
            labels.add(key.toLowerCase(Locale.ROOT));
            for (String alias : section.getStringList("aliases")) {
                if (alias != null && !alias.isBlank()) {
                    labels.add(alias.toLowerCase(Locale.ROOT));
                }
            }
            addServerAliases(key, labels);

            CommandGroup group = new CommandGroup(key.toLowerCase(Locale.ROOT), labels, perRank, fallback);
            groups.put(group.key(), group);
            for (String label : labels) {
                CommandGroup previous = lookup.put(label, group);
                if (previous != null) {
                    logger.warning("Command/alias '" + label
                            + "' is declared by both '" + previous.key() + "' and '" + group.key()
                            + "'; the latter wins.");
                }
            }
        }

        return groups.size();
    }

    /**
     * Merge in every alias the server registers for this command, so a copied
     * config block covers aliases the author did not list by hand.
     */
    private void addServerAliases(String commandKey, Set<String> labels) {
        if (aliasResolver == null) {
            return;
        }
        Set<String> discovered = aliasResolver.aliasesFor(commandKey.toLowerCase(Locale.ROOT));
        if (discovered == null || discovered.isEmpty()) {
            return;
        }
        int before = labels.size();
        for (String alias : discovered) {
            if (alias != null && !alias.isBlank()) {
                labels.add(alias.toLowerCase(Locale.ROOT));
            }
        }
        if (labels.size() > before) {
            logger.info("commands." + commandKey + ": auto-detected "
                    + (labels.size() - before) + " extra alias(es) from the server.");
        }
    }

    /** Read {@code fallback-rank} (with a legacy fallback to {@code default-rank}). */
    private void loadFallbackRank(FileConfiguration config) {
        String raw = config.getString("fallback-rank", "");
        if (raw == null || raw.isBlank()) {
            String legacy = config.getString("default-rank", "");
            if (legacy != null && !legacy.isBlank()) {
                logger.warning("'default-rank' has been renamed to 'fallback-rank' - "
                        + "please update config.yml.");
                raw = legacy;
            }
        }
        if (raw == null || raw.isBlank()) {
            logger.warning("No 'fallback-rank' set - commands without a matching rank node will "
                    + "use their last listed rank.");
            fallbackRank = "";
            return;
        }
        String rank = raw.trim().toLowerCase(Locale.ROOT);
        if (!VALID_RANK.matcher(rank).matches()) {
            logger.warning("Invalid 'fallback-rank: " + raw + "' - using the last listed rank instead.");
            rank = "";
        }
        fallbackRank = rank;
    }

    /**
     * Read a command's {@code cooldowns} entry, preserving config order.
     * Accepts the list form (preferred) and, for older configs, the map form.
     */
    private Map<String, Long> readCooldowns(String commandKey, ConfigurationSection section) {
        Map<String, Long> perRank = new LinkedHashMap<>();

        List<?> list = section.getList("cooldowns");
        if (list != null && !list.isEmpty()) {
            for (Object element : list) {
                if (!(element instanceof Map<?, ?> entry)) {
                    logger.warning("'commands." + commandKey + ".cooldowns': ignoring entry '"
                            + element + "' - expected '- rank: seconds'.");
                    continue;
                }
                for (Map.Entry<?, ?> pair : entry.entrySet()) {
                    addRank(commandKey, perRank, String.valueOf(pair.getKey()), pair.getValue());
                }
            }
            return perRank;
        }

        // Legacy map form: cooldowns: { player: 300 }
        ConfigurationSection map = section.getConfigurationSection("cooldowns");
        if (map != null) {
            for (String rankKey : map.getKeys(false)) {
                addRank(commandKey, perRank, rankKey, map.get(rankKey));
            }
        }
        return perRank;
    }

    private void addRank(String commandKey, Map<String, Long> perRank, String rawRank, Object rawSeconds) {
        String rank = rawRank.trim().toLowerCase(Locale.ROOT);
        if (!VALID_RANK.matcher(rank).matches()) {
            logger.warning("'commands." + commandKey + ".cooldowns': ignoring invalid rank '"
                    + rawRank + "' - use letters, digits, '_' or '-' only.");
            return;
        }
        long seconds = 0L;
        if (rawSeconds instanceof Number number) {
            seconds = number.longValue();
        } else if (rawSeconds != null) {
            try {
                seconds = Long.parseLong(String.valueOf(rawSeconds).trim());
            } catch (NumberFormatException ex) {
                logger.warning("'commands." + commandKey + ".cooldowns." + rawRank + "': '"
                        + rawSeconds + "' is not a number - ignoring.");
                return;
            }
        }
        if (perRank.containsKey(rank)) {
            logger.warning("'commands." + commandKey + ".cooldowns': rank '" + rank
                    + "' listed more than once - the first entry wins.");
            return;
        }
        perRank.put(rank, Math.max(0L, seconds));
    }

    /** The seconds used when no listed rank node matched. */
    private long resolveFallback(String commandKey, Map<String, Long> perRank) {
        if (!fallbackRank.isEmpty()) {
            Long value = perRank.get(fallbackRank);
            if (value != null) {
                return value;
            }
            logger.warning("'commands." + commandKey + "' does not list the fallback rank '"
                    + fallbackRank + "' - using its last listed rank instead.");
        }
        long last = 0L;
        for (Long value : perRank.values()) {
            last = value;
        }
        return last;
    }

    /** The rank applied to players holding none of a command's rank nodes. */
    String fallbackRank() {
        return fallbackRank;
    }

    /** The group handling {@code label} (already lower-cased, namespace stripped), or {@code null}. */
    CommandGroup groupFor(String label) {
        return lookup.get(label);
    }

    /** All loaded groups, for logging / status output. */
    List<CommandGroup> allGroups() {
        return new ArrayList<>(groups.values());
    }
}
