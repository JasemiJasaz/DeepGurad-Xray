/**
 * DeepGuard-XRay - Advanced Anti-Xray Plugin for Minecraft
 *
 * Created by Jasemi - 2025
 *
 * -------------------------------------------------------------------------------
 * Developer Note
 * -------------------------------------------------------------------------------
 * This is my first ever Minecraft plugin — so parts of the code might be messy :D
 * The plugin originally started on Spigot API 1.21, switched mid-development to
 * Paper 1.21, and then again to Paper 1.20.6. So... yeah, expect some weirdness.
 *
 * You may also find some unfinished or unused functions lying around. XD
 * Honestly, I'm surprised it still works.
 *
 * As the saying goes: "If it works, don't touch it." 😆
 *
 * This project is now public and open to the community — feel free to learn from it,
 * improve it, or just laugh at the spaghetti inside. Enjoy!
 */


package org.myplugin.deepGuardXray;

import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import org.myplugin.deepGuardXray.alerts.StaffAlertManager;
import org.myplugin.deepGuardXray.commands.AntiXrayCommand;
import org.myplugin.deepGuardXray.commands.CommandHider;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.gui.GuiListener;
import org.myplugin.deepGuardXray.gui.PlayerStatsGuiListener;
import org.myplugin.deepGuardXray.gui.StaffMenuGUI;
import org.myplugin.deepGuardXray.listeners.BlockListener;
import org.myplugin.deepGuardXray.listeners.ChatListener;
import org.myplugin.deepGuardXray.listeners.UpdateListener;
import org.myplugin.deepGuardXray.managers.*;
import org.myplugin.deepGuardXray.ml.MLConfig;
import org.myplugin.deepGuardXray.ml.MLDataManager;
import org.myplugin.deepGuardXray.ml.MLManager;
import org.myplugin.deepGuardXray.protocol.ProtocolHandler;
import org.myplugin.deepGuardXray.punishments.handlers.Paranoia.ParanoiaHandler;
import org.myplugin.deepGuardXray.utils.ChatInputHandler;
import org.myplugin.deepGuardXray.utils.UpdateApplier;
import org.myplugin.deepGuardXray.utils.UpdateChecker;
import java.io.File;

public class deepGuardXray extends JavaPlugin {
    private static deepGuardXray instance;
    private ConfigManager configManager;
    private StaffAlertManager staffAlertManager;
    private DecoyManager decoyManager;
    private PunishmentManager punishmentManager;
    private AppealManager appealManager;
    private UpdateChecker updateChecker;
    private UpdateApplier updateApplier;
    private ParanoiaHandler paranoiaHandler;
    private ChatInputHandler chatInputHandler;
    private MLManager mlManager;
    private ProtocolHandler protocolHandler;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        updateApplier = new UpdateApplier(this);
        updateApplier.performPendingCleanup();
        boolean updatePending = updateApplier.checkForPendingUpdate();
        configManager = new ConfigManager(this);
        configManager.setupDefaultPunishmentOptions();
        configManager.setupDefaultAutoSaveConfigs();
        getConfigManager().initializeWebhookSettings();
        int resourceId = 122967;
        this.updateChecker = new UpdateChecker(this, resourceId);
        if (getConfig().getBoolean("check-for-updates", true)) {
            getServer().getPluginManager().registerEvents(new UpdateListener(this, updateChecker), this);

            if (getConfig().getBoolean("periodic-update-checks.enabled", true)) {
                int checkIntervalHours = getConfig().getInt("periodic-update-checks.interval-hours", 24);
                updateChecker.setupPeriodicChecks(checkIntervalHours);
                getLogger().info("Scheduled automatic update checks every " + checkIntervalHours + " hours");
            }

        } else {
            getLogger().info("Update checking is disabled in config.yml");
        }

        appealManager = new AppealManager(this);

        int pluginId = 25174;
        Metrics metrics = new Metrics(this, pluginId);
        getLogger().info("bStats metrics enabled!");

        if (getConfig().getBoolean("check-for-updates", true)) {
            getServer().getPluginManager().registerEvents(new UpdateListener(this, updateChecker), this);
        }

        chatInputHandler = new ChatInputHandler(this);
        WebhookManager.initialize(this, getConfigManager());

        SuspiciousManager.initialize(this);
        StatsManager.initialize(this, configManager);

        staffAlertManager = new StaffAlertManager(this, configManager);
        decoyManager = new DecoyManager(this, configManager);

        punishmentManager = new PunishmentManager(configManager, this);
        getServer().getPluginManager().registerEvents(
                new ChatListener(punishmentManager, this),
                this
        );
        StaffMenuGUI.setPlugin(this);
        StaffMenuGUI mainMenuGUI = new StaffMenuGUI();

        paranoiaHandler = new ParanoiaHandler(this, punishmentManager, configManager);

        getServer().getPluginManager().registerEvents(
                new BlockListener(configManager, staffAlertManager, decoyManager, punishmentManager, paranoiaHandler),
                this
        );
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStatsGuiListener(), this);

        initializeMLSystem();

        if (configManager.isCommandHidingEnabled()) {
            CommandHider commandHider = new CommandHider(this);

            getServer().getPluginManager().registerEvents(commandHider, this);

            getCommand("deepguard").setExecutor(commandHider);
            getCommand("deepguard").setTabCompleter(commandHider);
            if (getCommand("dgx") != null) {
                getCommand("dgx").setExecutor(commandHider);
                getCommand("dgx").setTabCompleter(commandHider);
            }

            if (getCommand("deepguardx") != null) {
                getCommand("deepguardx").setExecutor(commandHider);
                getCommand("deepguardx").setTabCompleter(commandHider);
            }

            getLogger().info("Command hiding feature enabled - commands will be hidden from players without permission");
        } else {
            getCommand("deepguard").setExecutor(new AntiXrayCommand(this, updateChecker));
        }

        StatsManager.updateAutoSaveSettings();
        SuspiciousManager.updateAutoSaveSettings();
        punishmentManager.updateAutoSaveSettings();

        ensureCompleteConfig();

        getLogger().info("DeepGuard-XRay Plugin enabled with config settings!");

        if (updatePending) {
            getLogger().info("=================================================");
            getLogger().info(" DeepGuard-XRay will be automatically updated when the server shuts down!");
            getLogger().info("=================================================");
        }
    }

    /**
     * Initialize the Machine Learning system
     */
    private void initializeMLSystem() {
        MLDataManager.initialize(this);
        MLConfig mlConfig = new MLConfig(this);

        mlManager = new MLManager(this, configManager);

        if (mlConfig.isEnabled()) {
            getLogger().info("ML detection system enabled!");

            if (mlManager.isTrained()) {
                getLogger().info("ML model successfully loaded with pre-trained data!");
            } else {
                getLogger().warning("ML model is not trained. Use '/deepguardx ml train <player> <cheater|normal>' to collect training data.");
            }

            if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
                try {
                    protocolHandler = new ProtocolHandler(this, mlManager.getDataCollector(), configManager);
                    getLogger().info("ProtocolLib detected, advanced detection features enabled!");
                } catch (Exception e) {
                    getLogger().severe("Failed to initialize ProtocolLib integration: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                getLogger().warning("ProtocolLib not found! Advanced protocol-based detection features will be disabled.");
            }
        } else {
            getLogger().info("ML detection system is disabled. Use '/deepguardx ml enable' to enable it.");
        }
    }

    @Override
    public void saveConfig() {
        super.saveConfig();

        try {
            File configFile = new File(getDataFolder(), "config.yml");

            if (getConfigManager().isDebugEnabled()) {
                getLogger().info("Forcefully saving config to " + configFile.getAbsolutePath());
            }

            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                saveResource("config.yml", false);
            }
        } catch (Exception e) {
            getLogger().warning("Error ensuring config file exists: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        StatsManager.saveAllData();
        SuspiciousManager.saveAllData();

        if (protocolHandler != null) {
            protocolHandler.shutdown();
        }

        if (punishmentManager != null) {
            punishmentManager.onDisable();
        }

        if (updateApplier != null && updateApplier.isUpdatePending()) {
            getLogger().info("Applying DeepGuard-XRay update during shutdown...");
            updateApplier.applyUpdateOnShutdown();
        }
        if (paranoiaHandler != null) {
            paranoiaHandler.cleanup();
        }

    }
    private void ensureCompleteConfig() {
        boolean configUpdated = false;

        configUpdated |= ensureConfigPath("command-hiding.enabled", false);
        configUpdated |= ensureConfigPath("command-hiding.messages.error-line1", "§cUnknown or incomplete command, see below for error");
        configUpdated |= ensureConfigPath("command-hiding.messages.error-line2", "§c§n{command}§r§c§o<--[HERE]");
        configUpdated |= ensureConfigPath("check-for-updates", true);
        configUpdated |= ensureConfigPath("periodic-update-checks.enabled", true);
        configUpdated |= ensureConfigPath("periodic-update-checks.interval-hours", 24);

        if (configUpdated) {
            saveConfig();
            getLogger().info("Added missing configuration options to config.yml");
        }
    }

    private boolean ensureConfigPath(String path, Object defaultValue) {
        if (!getConfig().isSet(path)) {
            getConfig().set(path, defaultValue);
            return true;
        }
        return false;
    }

    public ProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    public MLManager getMLManager() {
        return mlManager;
    }

    public ChatInputHandler getChatInputHandler() {
        return chatInputHandler;
    }

    public WebhookManager getWebhookManager() {
        return WebhookManager.getInstance();
    }

    public AppealManager getAppealManager() {
        return appealManager;
    }

    public static deepGuardXray getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StaffAlertManager getStaffAlertManager() {
        return staffAlertManager;
    }

    public void setStaffAlertManager(StaffAlertManager staffAlertManager) {
        this.staffAlertManager = staffAlertManager;
    }

    public DecoyManager getDecoyManager() {
        return decoyManager;
    }

    public void setDecoyManager(DecoyManager decoyManager) {
        this.decoyManager = decoyManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public UpdateApplier getUpdateApplier() {
        return updateApplier;
    }

    public boolean isAutoUpdateEnabled() {
        return getConfig().getBoolean("auto-updates.enabled", true);
    }
}