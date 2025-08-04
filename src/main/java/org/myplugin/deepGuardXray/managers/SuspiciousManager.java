package org.myplugin.deepGuardXray.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.ml.MLConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class SuspiciousManager {

    private static final Map<UUID, Integer> suspiciousCounts = new HashMap<>();


    private static File suspiciousFile;
    private static FileConfiguration suspiciousConfig;
    private static deepGuardXray plugin;
    private static int autoSaveTaskId = -1;

    /**
     * Initialize the SuspiciousManager with the plugin instance
     *
     * @param pluginInstance The main plugin instance
     */
    public static void initialize(deepGuardXray pluginInstance) {
        plugin = pluginInstance;


        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }


        suspiciousFile = new File(plugin.getDataFolder(), "suspicious_counts.yml");


        if (!suspiciousFile.exists()) {
            try {
                suspiciousFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create suspicious_counts.yml", e);
            }
        }


        suspiciousConfig = YamlConfiguration.loadConfiguration(suspiciousFile);


        loadSuspiciousCounts();

    }

    /**
     * Loads suspicious counts from the file
     */
    private static void loadSuspiciousCounts() {
        suspiciousCounts.clear();

        if (suspiciousConfig.contains("players")) {
            for (String uuidString : suspiciousConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    int count = suspiciousConfig.getInt("players." + uuidString);
                    suspiciousCounts.put(uuid, count);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in suspicious_counts.yml: " + uuidString);
                }
            }
        }

        plugin.getLogger().info("Loaded suspicious counts for " + suspiciousCounts.size() + " players");
    }

    /**
     * Saves suspicious counts to the file
     */
    private static void saveSuspiciousCounts() {

        suspiciousConfig.set("players", null);


        for (Map.Entry<UUID, Integer> entry : suspiciousCounts.entrySet()) {
            suspiciousConfig.set("players." + entry.getKey().toString(), entry.getValue());
        }


        try {
            suspiciousConfig.save(suspiciousFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save suspicious_counts.yml", e);
        }
    }

    /**
     * Add suspicious count to a player (no immediate save, relies on auto-save)
     *
     * @param playerId UUID of the player
     */
    public static void addSuspicious(UUID playerId) {

        int previousCount = suspiciousCounts.getOrDefault(playerId, 0);


        suspiciousCounts.merge(playerId, 1, Integer::sum);


        int newCount = suspiciousCounts.get(playerId);


        if (plugin != null && plugin.getMLManager() != null) {
            MLConfig mlConfig = plugin.getMLManager().getMLConfig();


            if (mlConfig != null && mlConfig.isAutoAnalysisEnabled()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {

                    int threshold = mlConfig.getSuspiciousThreshold();


                    if (previousCount < threshold && newCount >= threshold) {
                        if (!plugin.getMLManager().hasExistingReport(playerId, player.getName())) {

                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                plugin.getMLManager().queuePlayerForAnalysis(playerId);


                                plugin.getLogger().info("Player " + player.getName() + " crossed the suspicious threshold (" + threshold + ") and was queued for auto-analysis");
                            });
                        } else {
                            plugin.getLogger().info("Player " + player.getName() + " crossed the suspicious threshold but already has a report");
                        }
                    } else if (newCount > previousCount && previousCount >= threshold) {
                        if (!plugin.getMLManager().hasExistingReport(playerId, player.getName()) && !plugin.getMLManager().getPlayersUnderAnalysis().contains(playerId) && !plugin.getMLManager().isPlayerInAnalysisQueue(playerId)) {


                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                plugin.getMLManager().queuePlayerForAnalysis(playerId);


                                plugin.getLogger().info("Player " + player.getName() + " suspicious count increased from " + previousCount + " to " + newCount + " (above threshold) and was queued for auto-analysis");
                            });
                        }
                    }
                }
            }
        }


    }

    /**
     * Get suspicious counts for all players
     *
     * @return Map of player UUIDs to suspicious counts
     */
    public static Map<UUID, Integer> getSuspiciousCounts() {
        return suspiciousCounts;
    }

    /**
     * Set suspicious count for a player (no immediate save, relies on auto-save)
     *
     * @param playerId UUID of the player
     * @param count    The count to set
     */
    public static void setSuspiciousCount(UUID playerId, int count) {
        suspiciousCounts.put(playerId, count);

    }

    /**
     * Remove suspicious count for a player (no immediate save, relies on auto-save)
     *
     * @param playerId UUID of the player
     */
    public static void removeSuspicious(UUID playerId) {
        suspiciousCounts.remove(playerId);

    }

    /**
     * Force an immediate save of all data
     * Useful when making important changes that shouldn't wait for auto-save
     */
    public static void forceSave() {
        saveSuspiciousCounts();
        plugin.getLogger().info("Force-saved suspicious counts for " + suspiciousCounts.size() + " players");
    }

    /**
     * Save all suspicious counts (called on plugin disable)
     */
    public static void saveAllData() {
        saveSuspiciousCounts();
        plugin.getLogger().info("Saved suspicious counts for " + suspiciousCounts.size() + " players");


        if (autoSaveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
        }
    }

    /**
     * Starts the auto-save task that runs every 10 minutes
     */
    private static void startAutoSaveTask() {

        if (autoSaveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(autoSaveTaskId);
        }


        long autoSaveInterval = plugin.getConfigManager().getSuspiciousAutoSaveInterval() * 1200L;


        autoSaveTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (plugin.getConfigManager().isSuspiciousAutoSaveLoggingEnabled()) {
                plugin.getLogger().info("Auto-saving suspicious player data...");
            }

            saveSuspiciousCounts();

            if (plugin.getConfigManager().isSuspiciousAutoSaveLoggingEnabled()) {
                plugin.getLogger().info("Auto-save complete for " + suspiciousCounts.size() + " players");
            }
        }, autoSaveInterval, autoSaveInterval);

        if (plugin.getConfigManager().isSuspiciousAutoSaveLoggingEnabled()) {
            plugin.getLogger().info("Started auto-save task for suspicious player data (every " + plugin.getConfigManager().getSuspiciousAutoSaveInterval() + " minutes)");
        }
    }

    /**
     * Update auto-save settings from config
     */
    public static void updateAutoSaveSettings() {

        if (plugin.getConfigManager() == null) {
            return;
        }


        if (plugin.getConfigManager().isSuspiciousAutoSaveEnabled() && autoSaveTaskId == -1) {
            startAutoSaveTask();
        } else if (!plugin.getConfigManager().isSuspiciousAutoSaveEnabled() && autoSaveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;

            if (plugin.getConfigManager().isSuspiciousAutoSaveLoggingEnabled()) {
                plugin.getLogger().info("Auto-save for suspicious player data has been disabled");
            }
        } else if (plugin.getConfigManager().isSuspiciousAutoSaveEnabled()) {
            startAutoSaveTask();
        }
    }
}