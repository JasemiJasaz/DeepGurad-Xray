package org.myplugin.deepGuardXray.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

public class UpdateApplier {

    private final JavaPlugin plugin;
    private final Logger logger;
    private boolean updatePending = false;
    private File currentPluginFile = null;

    public UpdateApplier(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Checks if there's a pending update at startup
     * @return true if an update is pending
     */
    public boolean checkForPendingUpdate() {
        
        File pluginsDirectory = plugin.getServer().getUpdateFolderFile().getParentFile();
        logger.info("Server plugins directory: " + pluginsDirectory.getAbsolutePath());

        
        File updateMarker = new File(plugin.getDataFolder(), "pending_update.txt");

        if (!updateMarker.exists()) {
            return false;
        }

        try {
            
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(updateMarker)) {
                props.load(fis);
            }

            
            String version = props.getProperty("version");
            String currentPluginPath = props.getProperty("current_plugin");
            String newPluginPath = props.getProperty("new_plugin");
            boolean alreadyDeleted = Boolean.parseBoolean(props.getProperty("already_deleted", "false"));

            if (version == null || currentPluginPath == null || newPluginPath == null) {
                logger.warning("Invalid update marker file. Missing required properties.");
                return false;
            }

            
            if (!alreadyDeleted) {
                currentPluginFile = new File(currentPluginPath);
                if (!currentPluginFile.exists()) {
                    logger.info("Old plugin file already removed, no cleanup needed.");
                    alreadyDeleted = true;
                }
            }

            
            File newPluginFile = new File(newPluginPath);
            if (!newPluginFile.exists()) {
                logger.warning("New plugin file does not exist: " + newPluginPath);
                updateMarker.delete(); 
                return false;
            }

            this.updatePending = true;

            logger.info("=================================================");
            logger.info(" DeepGuard-XRay: Update ready to use!");
            logger.info(" Current version: " + plugin.getDescription().getVersion());
            logger.info(" New version: " + version);
            logger.info(" Old plugin will be removed during server shutdown.");
            logger.info("=================================================");

            return true;
        } catch (Exception e) {
            logger.severe("Failed to check for pending update: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cleans up old plugin file during server shutdown
     */
    public void applyUpdateOnShutdown() {
        if (!updatePending) {
            return;
        }

        
        
        if (currentPluginFile == null) {
            
            File updateMarker = new File(plugin.getDataFolder(), "pending_update.txt");
            if (updateMarker.exists()) {
                updateMarker.delete();
                logger.info("Update complete, removed marker file.");
            }
            return;
        }

        try {
            logger.info("=================================================");
            logger.info(" DeepGuard-XRay: Cleaning up old plugin file");

            
            File updateMarker = new File(plugin.getDataFolder(), "pending_update.txt");
            if (updateMarker.exists()) {
                updateMarker.delete();
                logger.info(" Removed update marker file");
            }

            
            

            
            if (currentPluginFile.exists()) {
                currentPluginFile.deleteOnExit();
                logger.info(" Old plugin file will be deleted when the server fully stops");
            }

            
            File pluginDataFolder = plugin.getDataFolder();
            File cleanupMarker = new File(pluginDataFolder, "cleanup_marker.txt");
            try (FileWriter writer = new FileWriter(cleanupMarker)) {
                writer.write("path=" + currentPluginFile.getAbsolutePath() + "\n");
                writer.write("timestamp=" + System.currentTimeMillis() + "\n");
            }
            logger.info(" Created cleanup marker for next server start");

            logger.info("=================================================");
        } catch (Exception e) {
            logger.severe("Failed during update cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if there's a pending update
     */
    public boolean isUpdatePending() {
        return updatePending;
    }

    /**
     * Checks for and performs any pending cleanup from previous server run
     * This should be called during plugin onEnable
     */
    public void performPendingCleanup() {
        File cleanupMarker = new File(plugin.getDataFolder(), "cleanup_marker.txt");
        if (!cleanupMarker.exists()) {
            return;
        }

        try {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(cleanupMarker)) {
                props.load(fis);
            }

            String filePath = props.getProperty("path");
            if (filePath != null) {
                File fileToDelete = new File(filePath);
                if (fileToDelete.exists()) {
                    boolean deleted = fileToDelete.delete();
                    logger.info("Cleanup of old plugin file " +
                            (deleted ? "successful" : "failed") +
                            ": " + filePath);
                }
            }

            
            cleanupMarker.delete();
        } catch (Exception e) {
            logger.warning("Error during cleanup: " + e.getMessage());
        }
    }
}