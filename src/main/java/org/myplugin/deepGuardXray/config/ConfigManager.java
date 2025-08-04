package org.myplugin.deepGuardXray.config;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.myplugin.deepGuardXray.utils.WebhookSecurity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final JavaPlugin plugin;
    private int oreThreshold;
    private long timeWindowTicks;
    private double decoyDistance;
    private double decoyFieldOffset;
    private double maxDecayDistance;
    private long decoyRevertDelay;
    private boolean warnOnDecoy;
    private boolean decoyEnabled;
    private boolean decoyRequireBuried;
    private int decoySearchRadius;
    private int buriedThreshold;
    private boolean debugEnabled;
    private boolean staffOreAlerts;
    private long staffOreResetTime;
    private boolean staffAlertEnabled;
    private boolean statsAutoSaveEnabled;
    private int statsAutoSaveInterval;
    private boolean statsAutoSaveLogging;
    private boolean suspiciousAutoSaveEnabled;
    private int suspiciousAutoSaveInterval;
    private boolean suspiciousAutoSaveLogging;
    private boolean punishmentAutoSaveEnabled;
    private int punishmentAutoSaveInterval;
    private boolean punishmentAutoSaveLogging;
    private boolean commandHidingEnabled;
    private String commandHidingErrorLine1;
    private String commandHidingErrorLine2;
    private final Set<Material> naturalOres = new HashSet<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfiguration();
    }

    /**
     * Load settings from the configuration file.
     */
    private void loadConfiguration() {
        oreThreshold = plugin.getConfig().getInt("decoy.oreThreshold", 8);
        timeWindowTicks = plugin.getConfig().getLong("decoy.timeWindowTicks", 3800L);
        decoyDistance = plugin.getConfig().getDouble("decoy.distance", 2.0);
        decoyFieldOffset = plugin.getConfig().getDouble("decoy.fieldOffset", 0.5);
        maxDecayDistance = plugin.getConfig().getDouble("decoy.maxDistance", 10.0);
        decoyRevertDelay = plugin.getConfig().getLong("decoy.revertDelay", 1200L);
        warnOnDecoy = plugin.getConfig().getBoolean("decoy.warnOnDecoy", true);
        decoyEnabled = plugin.getConfig().getBoolean("decoy.enabled", true);
        decoyRequireBuried = plugin.getConfig().getBoolean("decoy.requireBuried", true);
        decoySearchRadius = plugin.getConfig().getInt("decoy.searchRadius", 3);
        buriedThreshold = plugin.getConfig().getInt("decoy.buriedThreshold", 4);
        debugEnabled = plugin.getConfig().getBoolean("debug.enabled", false);
        staffOreAlerts = plugin.getConfig().getBoolean("staff.oreAlerts", true);
        staffOreResetTime = plugin.getConfig().getLong("staff.oreResetTime", 36000L);
        staffAlertEnabled = plugin.getConfig().getBoolean("staff.alertEnabled", true);
        statsAutoSaveEnabled = plugin.getConfig().getBoolean("stats.autoSave.enabled", true);
        statsAutoSaveInterval = plugin.getConfig().getInt("stats.autoSave.intervalMinutes", 10);
        statsAutoSaveLogging = plugin.getConfig().getBoolean("stats.autoSave.logging", false);
        suspiciousAutoSaveEnabled = plugin.getConfig().getBoolean("suspicious.autoSave.enabled", true);
        suspiciousAutoSaveInterval = plugin.getConfig().getInt("suspicious.autoSave.intervalMinutes", 10);
        suspiciousAutoSaveLogging = plugin.getConfig().getBoolean("suspicious.autoSave.logging", true);
        punishmentAutoSaveEnabled = plugin.getConfig().getBoolean("punishment.autoSave.enabled", true);
        punishmentAutoSaveInterval = plugin.getConfig().getInt("punishment.autoSave.intervalMinutes", 10);
        punishmentAutoSaveLogging = plugin.getConfig().getBoolean("punishment.autoSave.logging", true);
        commandHidingEnabled = plugin.getConfig().getBoolean("command-hiding.enabled", false);
        commandHidingErrorLine1 = plugin.getConfig().getString("command-hiding.messages.error-line1", "§cUnknown or incomplete command, see below for error");
        commandHidingErrorLine2 = plugin.getConfig().getString("command-hiding.messages.error-line2", "§c§n{command}§r§c§o<--[HERE]");

        List<String> oreList = plugin.getConfig().getStringList("ores.natural");
        for (String oreName : oreList) {
            try {
                Material mat = Material.valueOf(oreName.toUpperCase());
                naturalOres.add(mat);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid ore in config: " + oreName);
            }
        }
    }

    /**
     * Set up default stats auto-save configuration values if they don't exist
     */

    public void setupDefaultAutoSaveConfigs() {
        setupDefaultStatsAutoSaveConfig();

        if (!plugin.getConfig().isSet("suspicious.autoSave.enabled")) {
            plugin.getConfig().set("suspicious.autoSave.enabled", true);
        }
        if (!plugin.getConfig().isSet("suspicious.autoSave.intervalMinutes")) {
            plugin.getConfig().set("suspicious.autoSave.intervalMinutes", 10);
        }
        if (!plugin.getConfig().isSet("suspicious.autoSave.logging")) {
            plugin.getConfig().set("suspicious.autoSave.logging", true);
        }

        if (!plugin.getConfig().isSet("punishment.autoSave.enabled")) {
            plugin.getConfig().set("punishment.autoSave.enabled", true);
        }
        if (!plugin.getConfig().isSet("punishment.autoSave.intervalMinutes")) {
            plugin.getConfig().set("punishment.autoSave.intervalMinutes", 10);
        }
        if (!plugin.getConfig().isSet("punishment.autoSave.logging")) {
            plugin.getConfig().set("punishment.autoSave.logging", true);
        }

        plugin.saveConfig();
    }

    public void setupDefaultStatsAutoSaveConfig() {
        if (!plugin.getConfig().isSet("stats.autoSave.enabled")) {
            plugin.getConfig().set("stats.autoSave.enabled", true);
        }

        if (!plugin.getConfig().isSet("stats.autoSave.intervalMinutes")) {
            plugin.getConfig().set("stats.autoSave.intervalMinutes", 10);
        }

        if (!plugin.getConfig().isSet("stats.autoSave.logging")) {
            plugin.getConfig().set("stats.autoSave.logging", false);
        }

        plugin.saveConfig();
    }

    /**
     * Check if punishment for a given level is enabled
     */
    public boolean isPunishmentEnabled(int level) {
        return plugin.getConfig().getBoolean("punishment.enabled." + level, false);
    }

    /**
     * Set punishment enabled status for a level
     */
    public void setPunishmentEnabled(int level, boolean enabled) {
        plugin.getConfig().set("punishment.enabled." + level, enabled);
        plugin.saveConfig();
    }

    /**
     * Check if a specific punishment option is enabled for a given level
     *
     * @param level  The punishment level
     * @param option The option key
     * @return true if the option is enabled, false otherwise
     */
    public boolean isPunishmentOptionEnabled(int level, String option) {
        String path = "punishment.levels." + level + ".options." + option;
        return plugin.getConfig().getBoolean(path, false);
    }

    /**
     * Set whether a specific punishment option is enabled for a given level
     *
     * @param level   The punishment level
     * @param option  The option key
     * @param enabled Whether the option should be enabled
     */
    public void setPunishmentOptionEnabled(int level, String option, boolean enabled) {
        String path = "punishment.levels." + level + ".options." + option;
        plugin.getConfig().set(path, enabled);
        plugin.saveConfig();
    }

    /**
     * Set up default punishment options if they don't exist in the config
     */
    public void setupDefaultPunishmentOptions() {
        for (int i = 1; i <= 6; i++) {
            if (!plugin.getConfig().isSet("punishment.enabled." + i)) {
                plugin.getConfig().set("punishment.enabled." + i, false);
            }

            setDefaultOption(i, "admin_alert", true);
            setDefaultOption(i, "warning_message", i <= 3);
        }


        setDefaultOption(1, "mining_fatigue", true);
        setDefaultOption(1, "fake_diamonds", false);
        setDefaultOption(1, "admin_alert", true);
        setDefaultOption(1, "heavy_pickaxe", false);

        setDefaultOption(2, "fake_ore_veins", true);
        setDefaultOption(2, "inventory_drop", false);
        setDefaultOption(2, "xray_vision_blur", true);
        setDefaultOption(2, "tool_damage", false);
        setDefaultOption(2, "paranoia_mode", false);

        setDefaultOption(3, "temporary_kick", true);
        setDefaultOption(3, "mining_license_suspension", false);
        setDefaultOption(3, "resource_tax", false);
        setDefaultOption(3, "decoy_attraction", true);
        setDefaultOption(3, "fools_gold", false);

        setDefaultOption(4, "extended_ban", true);
        setDefaultOption(4, "mining_reputation", false);
        setDefaultOption(4, "restricted_areas", false);
        setDefaultOption(4, "cursed_pickaxe", false);

        setDefaultOption(5, "long_term_ban", true);
        setDefaultOption(5, "public_notification", false);
        setDefaultOption(5, "permanent_mining_debuff", false);
        setDefaultOption(5, "staff_review", false);
        setDefaultOption(5, "stone_vision", false);

        setDefaultOption(6, "permanent_ban", true);
        setDefaultOption(6, "ip_tracking", false);
        setDefaultOption(6, "security_report", true);
        setDefaultOption(6, "tnt_execution", false);

        plugin.saveConfig();
    }

    /**
     * Helper method to set default option if not already set
     */
    private void setDefaultOption(int level, String option, boolean defaultValue) {
        String path = "punishment.levels." + level + ".options." + option;
        if (!plugin.getConfig().isSet(path)) {
            plugin.getConfig().set(path, defaultValue);
        }
    }

    /**
     * Reload the configuration from file
     */
    public void reloadConfig() {
        plugin.reloadConfig();

        loadConfiguration();
    }

    /**
     * Gets the Discord webhook URL
     */
    public String getWebhookUrl() {
        String storedUrl = plugin.getConfig().getString("webhook.url", "");

        if (storedUrl == null || storedUrl.isEmpty()) {
            return "";
        }

        if (storedUrl.startsWith("https://")) {
            return storedUrl;
        }

        try {
            return WebhookSecurity.deobfuscateWebhookUrl(storedUrl);
        } catch (Exception e) {

            return storedUrl;
        }
    }

    /**
     * Sets the Discord webhook URL (encoded)
     */
    public void setWebhookUrl(String url) {
        plugin.getConfig().set("webhook.url", url);
        plugin.saveConfig();
    }

    /**
     * Checks if a specific webhook alert type is enabled
     */
    public boolean isWebhookAlertEnabled(String alertType) {
        return plugin.getConfig().getBoolean("webhook.alerts." + alertType, false);
    }

    /**
     * Sets whether a specific webhook alert type is enabled
     */
    public void setWebhookAlertEnabled(String alertType, boolean enabled) {
        plugin.getConfig().set("webhook.alerts." + alertType, enabled);
        plugin.saveConfig();
    }

    /**
     * Initialize default webhook settings if they don't exist
     */
    public void initializeWebhookSettings() {
        if (!plugin.getConfig().contains("webhook.url")) {
            plugin.getConfig().set("webhook.url", "");
        }

        if (!plugin.getConfig().contains("webhook.alerts.xray_detection")) {
            plugin.getConfig().set("webhook.alerts.xray_detection", true);
        }
        if (!plugin.getConfig().contains("webhook.alerts.suspicious_mining")) {
            plugin.getConfig().set("webhook.alerts.suspicious_mining", true);
        }
        if (!plugin.getConfig().contains("webhook.alerts.punishment_applied")) {
            plugin.getConfig().set("webhook.alerts.punishment_applied", true);
        }
        if (!plugin.getConfig().contains("webhook.alerts.staff_actions")) {
            plugin.getConfig().set("webhook.alerts.staff_actions", false);
        }
        if (!plugin.getConfig().contains("webhook.alerts.appeal_updates")) {
            plugin.getConfig().set("webhook.alerts.appeal_updates", true);
        }

        plugin.saveConfig();
    }

    public int getOreThreshold() {
        return oreThreshold;
    }

    public long getTimeWindowTicks() {
        return timeWindowTicks;
    }

    public double getDecoyDistance() {
        return decoyDistance;
    }

    public double getDecoyFieldOffset() {
        return decoyFieldOffset;
    }

    public double getMaxDecayDistance() {
        return maxDecayDistance;
    }

    public long getDecoyRevertDelay() {
        return decoyRevertDelay;
    }

    public boolean isWarnOnDecoy() {
        return warnOnDecoy;
    }

    public boolean isDecoyEnabled() {
        return decoyEnabled;
    }

    public boolean isDecoyRequireBuried() {
        return decoyRequireBuried;
    }

    public int getDecoySearchRadius() {
        return decoySearchRadius;
    }

    public int getBuriedThreshold() {
        return buriedThreshold;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isStaffOreAlerts() {
        return staffOreAlerts;
    }

    public long getStaffOreResetTime() {
        return staffOreResetTime;
    }

    public boolean isStaffAlertEnabled() {
        return staffAlertEnabled;
    }

    public Set<Material> getNaturalOres() {
        return naturalOres;
    }

    public boolean isCommandHidingEnabled() {
        return commandHidingEnabled;
    }

    public void setCommandHidingEnabled(boolean enabled) {
        commandHidingEnabled = enabled;
        plugin.getConfig().set("command-hiding.enabled", enabled);
        plugin.saveConfig();
    }

    public String getCommandHidingErrorLine1() {
        return commandHidingErrorLine1;
    }

    public String getCommandHidingErrorLine2() {
        return commandHidingErrorLine2;
    }

    public void setupDefaultCommandHidingConfig() {
        if (!plugin.getConfig().isSet("command-hiding.enabled")) {
            plugin.getConfig().set("command-hiding.enabled", false);
        }

        if (!plugin.getConfig().isSet("command-hiding.messages.error-line1")) {
            plugin.getConfig().set("command-hiding.messages.error-line1", "§cUnknown or incomplete command, see below for error");
        }

        if (!plugin.getConfig().isSet("command-hiding.messages.error-line2")) {
            plugin.getConfig().set("command-hiding.messages.error-line2", "§c§n{command}§r§c§o<--[HERE]");
        }

        plugin.saveConfig();
    }

    public boolean isStatsAutoSaveEnabled() {
        return statsAutoSaveEnabled;
    }

    /**
     * Set whether stats auto-save is enabled
     *
     * @param enabled True to enable auto-save, false to disable
     */
    public void setStatsAutoSaveEnabled(boolean enabled) {
        statsAutoSaveEnabled = enabled;
        plugin.getConfig().set("stats.autoSave.enabled", enabled);
        plugin.saveConfig();
    }

    public int getStatsAutoSaveInterval() {
        return statsAutoSaveInterval;
    }

    /**
     * Set the interval for stats auto-save in minutes
     *
     * @param minutes Minutes between auto-saves
     */
    public void setStatsAutoSaveInterval(int minutes) {
        if (minutes < 1) minutes = 1;

        statsAutoSaveInterval = minutes;
        plugin.getConfig().set("stats.autoSave.intervalMinutes", minutes);
        plugin.saveConfig();
    }

    public boolean isStatsAutoSaveLoggingEnabled() {
        return statsAutoSaveLogging;
    }

    /**
     * Set whether stats auto-save logging is enabled
     *
     * @param enabled True to enable logging, false to disable
     */
    public void setStatsAutoSaveLoggingEnabled(boolean enabled) {
        statsAutoSaveLogging = enabled;
        plugin.getConfig().set("stats.autoSave.logging", enabled);
        plugin.saveConfig();
    }

    public boolean isSuspiciousAutoSaveEnabled() {
        return suspiciousAutoSaveEnabled;
    }

    public void setSuspiciousAutoSaveEnabled(boolean enabled) {
        suspiciousAutoSaveEnabled = enabled;
        plugin.getConfig().set("suspicious.autoSave.enabled", enabled);
        plugin.saveConfig();
    }

    public int getSuspiciousAutoSaveInterval() {
        return suspiciousAutoSaveInterval;
    }

    public void setSuspiciousAutoSaveInterval(int minutes) {
        if (minutes < 1) minutes = 1;

        suspiciousAutoSaveInterval = minutes;
        plugin.getConfig().set("suspicious.autoSave.intervalMinutes", minutes);
        plugin.saveConfig();
    }

    public boolean isSuspiciousAutoSaveLoggingEnabled() {
        return suspiciousAutoSaveLogging;
    }

    public void setSuspiciousAutoSaveLoggingEnabled(boolean enabled) {
        suspiciousAutoSaveLogging = enabled;
        plugin.getConfig().set("suspicious.autoSave.logging", enabled);
        plugin.saveConfig();
    }

    public boolean isPunishmentAutoSaveEnabled() {
        return punishmentAutoSaveEnabled;
    }

    public void setPunishmentAutoSaveEnabled(boolean enabled) {
        punishmentAutoSaveEnabled = enabled;
        plugin.getConfig().set("punishment.autoSave.enabled", enabled);
        plugin.saveConfig();
    }

    public int getPunishmentAutoSaveInterval() {
        return punishmentAutoSaveInterval;
    }

    public void setPunishmentAutoSaveInterval(int minutes) {
        if (minutes < 1) minutes = 1;

        punishmentAutoSaveInterval = minutes;
        plugin.getConfig().set("punishment.autoSave.intervalMinutes", minutes);
        plugin.saveConfig();
    }

    public boolean isPunishmentAutoSaveLoggingEnabled() {
        return punishmentAutoSaveLogging;
    }

    public void setPunishmentAutoSaveLoggingEnabled(boolean enabled) {
        punishmentAutoSaveLogging = enabled;
        plugin.getConfig().set("punishment.autoSave.logging", enabled);
        plugin.saveConfig();
    }
}