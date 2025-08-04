package org.myplugin.deepGuardXray.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Utility class to directly save config values to disk,
 * bypassing any potential issues with Bukkit's config system
 */
public class DirectConfigSaver {

    /**
     * Directly update a boolean config value in the plugin's config.yml
     *
     * @param plugin The plugin instance
     * @param path The config path (e.g., "debug.enabled")
     * @param value The new boolean value
     * @return True if the save was successful, false otherwise
     */
    public static boolean saveBoolean(JavaPlugin plugin, String path, boolean value) {
        try {
            
            File configFile = new File(plugin.getDataFolder(), "config.yml");

            
            if (!configFile.exists()) {
                plugin.getLogger().severe("Config file doesn't exist at: " + configFile.getAbsolutePath());
                return false;
            }

            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            
            config.set(path, value);

            
            config.save(configFile);

            
            plugin.reloadConfig();

            plugin.getLogger().info("Successfully saved " + path + "=" + value + " to config.yml");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config value " + path + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Directly update an integer config value in the plugin's config.yml
     *
     * @param plugin The plugin instance
     * @param path The config path (e.g., "decoy.oreThreshold")
     * @param value The new integer value
     * @return True if the save was successful, false otherwise
     */
    public static boolean saveInteger(JavaPlugin plugin, String path, int value) {
        try {
            
            File configFile = new File(plugin.getDataFolder(), "config.yml");

            
            if (!configFile.exists()) {
                plugin.getLogger().severe("Config file doesn't exist at: " + configFile.getAbsolutePath());
                return false;
            }

            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            
            config.set(path, value);

            
            config.save(configFile);

            
            plugin.reloadConfig();

            plugin.getLogger().info("Successfully saved " + path + "=" + value + " to config.yml");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config value " + path + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Directly update a double config value in the plugin's config.yml
     *
     * @param plugin The plugin instance
     * @param path The config path (e.g., "decoy.distance")
     * @param value The new double value
     * @return True if the save was successful, false otherwise
     */
    public static boolean saveDouble(JavaPlugin plugin, String path, double value) {
        try {
            
            File configFile = new File(plugin.getDataFolder(), "config.yml");

            
            if (!configFile.exists()) {
                plugin.getLogger().severe("Config file doesn't exist at: " + configFile.getAbsolutePath());
                return false;
            }

            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            
            config.set(path, value);

            
            config.save(configFile);

            
            plugin.reloadConfig();

            plugin.getLogger().info("Successfully saved " + path + "=" + value + " to config.yml");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config value " + path + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Directly update a long config value in the plugin's config.yml
     *
     * @param plugin The plugin instance
     * @param path The config path (e.g., "staff.oreResetTime")
     * @param value The new long value
     * @return True if the save was successful, false otherwise
     */
    public static boolean saveLong(JavaPlugin plugin, String path, long value) {
        try {
            
            File configFile = new File(plugin.getDataFolder(), "config.yml");

            
            if (!configFile.exists()) {
                plugin.getLogger().severe("Config file doesn't exist at: " + configFile.getAbsolutePath());
                return false;
            }

            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            
            config.set(path, value);

            
            config.save(configFile);

            
            plugin.reloadConfig();

            plugin.getLogger().info("Successfully saved " + path + "=" + value + " to config.yml");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config value " + path + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}