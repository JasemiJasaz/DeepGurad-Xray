package org.myplugin.deepGuardXray.managers;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class StatsManager {

    public static final Set<Material> trackedOres = new HashSet<>(Arrays.asList(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS

    ));


    private static final Map<UUID, Map<Material, Integer>> cumulativeStats = new HashMap<>();


    private static deepGuardXray plugin;
    private static ConfigManager configManager;
    private static File statsFile;
    private static int autoSaveTaskId = -1;

    /**
     * Initialize the StatsManager with the plugin instance
     *
     * @param pluginInstance The main plugin instance
     * @param configManager  The configuration manager
     */
    public static void initialize(deepGuardXray pluginInstance, ConfigManager configManager) {
        plugin = pluginInstance;
        StatsManager.configManager = configManager;


        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }


        statsFile = new File(plugin.getDataFolder(), "mining_stats.yml");


        if (statsFile.exists()) {
            loadStats(statsFile);
        }


    }

    /**
     * Call this method each time a player mines an ore.
     * Only tracked ores are counted.
     */
    public static void addOreMined(UUID playerId, Material ore) {
        if (!trackedOres.contains(ore)) {
            return;
        }
        cumulativeStats.computeIfAbsent(playerId, k -> new HashMap<>()).merge(ore, 1, Integer::sum);
    }

    /**
     * Retrieve the ore stats for a given player.
     */
    public static Map<Material, Integer> getOreStats(UUID playerId) {
        return cumulativeStats.getOrDefault(playerId, new HashMap<>());
    }

    /**
     * Check if a player has any mining stats
     */
    public static boolean hasStats(UUID playerId) {
        return cumulativeStats.containsKey(playerId) && !cumulativeStats.get(playerId).isEmpty();
    }

    /**
     * Get all player UUIDs that have mining statistics
     */
    public static Set<UUID> getAllPlayerIds() {

        return cumulativeStats.keySet();
    }

    /**
     * Starts the auto-save task that runs periodically based on config
     */
    private static void startAutoSaveTask() {

        if (autoSaveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(autoSaveTaskId);
        }


        long autoSaveInterval = configManager.getStatsAutoSaveInterval() * 1200L;


        autoSaveTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (configManager.isStatsAutoSaveLoggingEnabled()) {
                plugin.getLogger().info("Auto-saving mining statistics...");
            }

            saveStats(statsFile);

            if (configManager.isStatsAutoSaveLoggingEnabled()) {
                plugin.getLogger().info("Auto-save complete for mining statistics (" + cumulativeStats.size() + " players)");
            }
        }, autoSaveInterval, autoSaveInterval);

        if (configManager.isStatsAutoSaveLoggingEnabled()) {
            plugin.getLogger().info("Started auto-save task for mining statistics (every " + configManager.getStatsAutoSaveInterval() + " minutes)");
        }
    }

    /**
     * Force an immediate save of all mining statistics
     */
    public static void forceSave() {
        saveStats(statsFile);
        if (configManager.isStatsAutoSaveLoggingEnabled()) {
            plugin.getLogger().info("Force-saved mining statistics for " + cumulativeStats.size() + " players");
        }
    }

    /**
     * Save all mining statistics (called on plugin disable)
     */
    public static void saveAllData() {

        if (statsFile != null && statsFile.exists()) {
            saveStats(statsFile);

            if (configManager != null && configManager.isStatsAutoSaveLoggingEnabled()) {
                plugin.getLogger().info("Saved mining statistics for " + cumulativeStats.size() + " players on shutdown");
            }
        } else if (plugin != null) {

            File newStatsFile = new File(plugin.getDataFolder(), "mining_stats.yml");
            if (!newStatsFile.exists()) {
                try {
                    newStatsFile.createNewFile();
                } catch (IOException e) {
                    if (plugin != null) {
                        plugin.getLogger().log(Level.SEVERE, "Could not create mining_stats.yml", e);
                    }
                    return;
                }
            }
            saveStats(newStatsFile);
        }


        if (autoSaveTaskId != -1 && plugin != null) {
            plugin.getServer().getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
        }
    }

    /**
     * Update auto-save settings from config (call this when config changes)
     */
    public static void updateAutoSaveSettings() {

        if (configManager == null || plugin == null) {
            return;
        }


        if (configManager.isStatsAutoSaveEnabled() && autoSaveTaskId == -1) {
            startAutoSaveTask();
        } else if (!configManager.isStatsAutoSaveEnabled() && autoSaveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;

            if (configManager.isStatsAutoSaveLoggingEnabled()) {
                plugin.getLogger().info("Auto-save for mining statistics has been disabled");
            }
        } else if (configManager.isStatsAutoSaveEnabled()) {
            startAutoSaveTask();
        }
    }

    /**
     * Save the cumulative stats to a file.
     */
    public static void saveStats(File file) {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Map<Material, Integer>> entry : cumulativeStats.entrySet()) {
            String uuid = entry.getKey().toString();
            Map<Material, Integer> ores = entry.getValue();
            for (Map.Entry<Material, Integer> oreEntry : ores.entrySet()) {
                String path = "stats." + uuid + "." + oreEntry.getKey().name();
                config.set(path, oreEntry.getValue());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save mining stats to " + file.getName(), e);
        }
    }

    /**
     * Load the cumulative stats from a file.
     */
    public static void loadStats(File file) {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains("stats")) {
            for (String uuidString : config.getConfigurationSection("stats").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                Map<Material, Integer> ores = new HashMap<>();
                for (String oreName : config.getConfigurationSection("stats." + uuidString).getKeys(false)) {
                    int count = config.getInt("stats." + uuidString + "." + oreName);
                    Material ore = Material.getMaterial(oreName);
                    if (ore != null) {
                        ores.put(ore, count);
                    }
                }
                cumulativeStats.put(uuid, ores);
            }
        }

        if (configManager != null && configManager.isStatsAutoSaveLoggingEnabled()) {
            plugin.getLogger().info("Loaded mining statistics for " + cumulativeStats.size() + " players");
        }
    }
}