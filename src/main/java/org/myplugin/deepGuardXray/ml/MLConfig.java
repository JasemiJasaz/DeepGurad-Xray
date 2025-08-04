package org.myplugin.deepGuardXray.ml;

import org.bukkit.configuration.file.FileConfiguration;
import org.myplugin.deepGuardXray.deepGuardXray;

/**
 * Configuration for the machine learning component
 */
public class MLConfig {
    private final deepGuardXray plugin;

    private boolean enabled = true;

    private int trainingSessionDuration = 10 * 60;

    private double detectionThreshold = 0.75;

    private int positionUpdateInterval = 5;

    private boolean autoAnalysisEnabled = true;
    private int suspiciousThreshold = 5;
    private int maxAutoAnalysisPlayers = 5;

    public MLConfig(deepGuardXray plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Load configuration from config.yml
     */
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("ml.enabled")) {
            config.set("ml.enabled", enabled);
        }
        if (!config.contains("ml.trainingSessionDuration")) {
            config.set("ml.trainingSessionDuration", trainingSessionDuration);
        }
        if (!config.contains("ml.detectionThreshold")) {
            config.set("ml.detectionThreshold", detectionThreshold);
        }
        if (!config.contains("ml.positionUpdateInterval")) {
            config.set("ml.positionUpdateInterval", positionUpdateInterval);
        }

        if (!config.contains("ml.autoAnalysis.enabled")) {
            config.set("ml.autoAnalysis.enabled", autoAnalysisEnabled);
        }
        if (!config.contains("ml.autoAnalysis.suspiciousThreshold")) {
            config.set("ml.autoAnalysis.suspiciousThreshold", suspiciousThreshold);
        }
        if (!config.contains("ml.autoAnalysis.maxPlayers")) {
            config.set("ml.autoAnalysis.maxPlayers", maxAutoAnalysisPlayers);
        }

        plugin.saveConfig();

        enabled = config.getBoolean("ml.enabled");
        trainingSessionDuration = config.getInt("ml.trainingSessionDuration");
        detectionThreshold = config.getDouble("ml.detectionThreshold");
        positionUpdateInterval = config.getInt("ml.positionUpdateInterval");

        autoAnalysisEnabled = config.getBoolean("ml.autoAnalysis.enabled");
        suspiciousThreshold = config.getInt("ml.autoAnalysis.suspiciousThreshold");
        maxAutoAnalysisPlayers = config.getInt("ml.autoAnalysis.maxPlayers");
    }

    /**
     * Save configuration to config.yml
     */
    public void saveConfig() {
        FileConfiguration config = plugin.getConfig();

        config.set("ml.enabled", enabled);
        config.set("ml.trainingSessionDuration", trainingSessionDuration);
        config.set("ml.detectionThreshold", detectionThreshold);
        config.set("ml.positionUpdateInterval", positionUpdateInterval);

        config.set("ml.autoAnalysis.enabled", autoAnalysisEnabled);
        config.set("ml.autoAnalysis.suspiciousThreshold", suspiciousThreshold);
        config.set("ml.autoAnalysis.maxPlayers", maxAutoAnalysisPlayers);

        plugin.saveConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveConfig();
    }

    public int getTrainingSessionDuration() {
        return trainingSessionDuration;
    }

    public double getDetectionThreshold() {
        return detectionThreshold;
    }

    public void setDetectionThreshold(double detectionThreshold) {
        this.detectionThreshold = detectionThreshold;
        saveConfig();
    }

    public int getPositionUpdateInterval() {
        return positionUpdateInterval;
    }

    public boolean isAutoAnalysisEnabled() {
        return autoAnalysisEnabled;
    }

    public void setAutoAnalysisEnabled(boolean autoAnalysisEnabled) {
        this.autoAnalysisEnabled = autoAnalysisEnabled;
        saveConfig();
    }

    public int getSuspiciousThreshold() {
        return suspiciousThreshold;
    }

    public void setSuspiciousThreshold(int suspiciousThreshold) {
        this.suspiciousThreshold = suspiciousThreshold;
        saveConfig();
    }

    public int getMaxAutoAnalysisPlayers() {
        return maxAutoAnalysisPlayers;
    }

    public void setMaxAutoAnalysisPlayers(int maxAutoAnalysisPlayers) {
        this.maxAutoAnalysisPlayers = maxAutoAnalysisPlayers;
        saveConfig();
    }
}