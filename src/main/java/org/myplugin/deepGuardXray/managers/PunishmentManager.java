package org.myplugin.deepGuardXray.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.listeners.PunishmentListener;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class PunishmentManager {

    private final ConfigManager configManager;
    private final deepGuardXray plugin;
    private final Map<UUID, Long> miningLicenseSuspension = new HashMap<>();
    private final Map<UUID, Long> restrictedAreas = new HashMap<>();
    private final Map<UUID, Long> resourceTaxActive = new HashMap<>();
    private final Map<UUID, Long> cursedPickaxeActive = new HashMap<>();
    private final Map<UUID, Long> stoneVisionActive = new HashMap<>();
    private final Map<UUID, Long> paranoiaModeActive = new HashMap<>();
    private final Map<UUID, Integer> fakeDiamondsRemaining = new HashMap<>();
    private final Set<UUID> permanentMiningDebuff = new HashSet<>();
    private final Set<Block> fakeOreBlocks = new HashSet<>();
    private final Map<UUID, Long> fakeOreVeinsActive = new HashMap<>();
    private final Map<UUID, Long> foolsGoldActive = new HashMap<>();
    private final Map<UUID, Map.Entry<Component, Long>> miningReputations = new HashMap<>();
    private final Map<UUID, Integer> playerPunishmentLevels = new HashMap<>();
    private File punishmentDataFile;
    private FileConfiguration punishmentConfig;
    private int autoSaveTaskId = -1;

    public PunishmentManager(ConfigManager configManager, deepGuardXray plugin) {
        this.configManager = configManager;
        this.plugin = plugin;


        initializeDataStorage();


        Bukkit.getPluginManager().registerEvents(new PunishmentListener(this, plugin.getDecoyManager(), configManager, plugin), plugin);


        startCleanupTask();


        startDisabledLevelCheckTask();
    }

    /**
     * Initialize punishment data storage
     */
    private void initializeDataStorage() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }


        punishmentDataFile = new File(plugin.getDataFolder(), "punishment_data.yml");


        if (!punishmentDataFile.exists()) {
            try {
                punishmentDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create punishment_data.yml", e);
            }
        }


        punishmentConfig = YamlConfiguration.loadConfiguration(punishmentDataFile);


        loadPunishmentData();


        if (configManager.isPunishmentAutoSaveEnabled()) {
            startAutoSaveTask();
        }
    }


    private void startAutoSaveTask() {

        if (autoSaveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(autoSaveTaskId);
        }


        long autoSaveInterval = configManager.getPunishmentAutoSaveInterval() * 1200L;


        autoSaveTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (configManager.isPunishmentAutoSaveLoggingEnabled()) {
                plugin.getLogger().info("Auto-saving punishment data...");
            }

            savePunishmentData();

            if (configManager.isPunishmentAutoSaveLoggingEnabled()) {
                plugin.getLogger().info("Auto-save complete for punishment data");
            }
        }, autoSaveInterval, autoSaveInterval);

        if (configManager.isPunishmentAutoSaveLoggingEnabled()) {
            plugin.getLogger().info("Started auto-save task for punishment data (every " + configManager.getPunishmentAutoSaveInterval() + " minutes)");
        }
    }


    public void updateAutoSaveSettings() {

        if (configManager.isPunishmentAutoSaveEnabled() && autoSaveTaskId == -1) {
            startAutoSaveTask();
        } else if (!configManager.isPunishmentAutoSaveEnabled() && autoSaveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;

            if (configManager.isPunishmentAutoSaveLoggingEnabled()) {
                plugin.getLogger().info("Auto-save for punishment data has been disabled");
            }
        } else if (configManager.isPunishmentAutoSaveEnabled()) {
            startAutoSaveTask();
        }
    }

    /**
     * Save all punishment data to file
     */
    public void savePunishmentData() {
        try {

            punishmentConfig.set("punishments", null);


            saveLongMap("miningLicenseSuspension", miningLicenseSuspension);
            saveLongMap("restrictedAreas", restrictedAreas);
            saveLongMap("resourceTaxActive", resourceTaxActive);
            saveLongMap("cursedPickaxeActive", cursedPickaxeActive);
            saveLongMap("stoneVisionActive", stoneVisionActive);
            saveLongMap("paranoiaModeActive", paranoiaModeActive);
            saveLongMap("fakeOreVeinsActive", fakeOreVeinsActive);
            saveLongMap("foolsGoldActive", foolsGoldActive);


            saveIntegerMap("fakeDiamondsRemaining", fakeDiamondsRemaining);
            saveIntegerMap("playerPunishmentLevels", playerPunishmentLevels);


            saveUUIDSet("permanentMiningDebuff", permanentMiningDebuff);


            saveMiningReputations();


            punishmentConfig.save(punishmentDataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save punishment data", e);
        }
    }

    /**
     * Load all punishment data from file
     */
    private void loadPunishmentData() {
        ConfigurationSection section = punishmentConfig.getConfigurationSection("punishments");
        if (section == null) {
            plugin.getLogger().info("No saved punishment data found.");
            return;
        }


        loadLongMap("miningLicenseSuspension", miningLicenseSuspension);
        loadLongMap("restrictedAreas", restrictedAreas);
        loadLongMap("resourceTaxActive", resourceTaxActive);
        loadLongMap("cursedPickaxeActive", cursedPickaxeActive);
        loadLongMap("stoneVisionActive", stoneVisionActive);
        loadLongMap("paranoiaModeActive", paranoiaModeActive);
        loadLongMap("fakeOreVeinsActive", fakeOreVeinsActive);
        loadLongMap("foolsGoldActive", foolsGoldActive);


        loadIntegerMap("fakeDiamondsRemaining", fakeDiamondsRemaining);
        loadIntegerMap("playerPunishmentLevels", playerPunishmentLevels);


        loadUUIDSet("permanentMiningDebuff", permanentMiningDebuff);


        loadMiningReputations();

        plugin.getLogger().info("Loaded punishment data successfully");


        cleanupExpiredPunishments();
    }

    /**
     * Clean up any punishments that have expired while the server was offline
     */
    private void cleanupExpiredPunishments() {
        long currentTime = System.currentTimeMillis();


        cleanupExpiredMaps(currentTime);

        plugin.getLogger().info("Cleaned up expired punishments");
    }

    /**
     * Save a UUID to Long map to config
     */
    private void saveLongMap(String mapName, Map<UUID, Long> map) {
        for (Map.Entry<UUID, Long> entry : map.entrySet()) {
            punishmentConfig.set("punishments." + mapName + "." + entry.getKey().toString(), entry.getValue());
        }
    }

    /**
     * Load a UUID to Long map from config
     */
    private void loadLongMap(String mapName, Map<UUID, Long> map) {
        map.clear();

        ConfigurationSection section = punishmentConfig.getConfigurationSection("punishments." + mapName);
        if (section != null) {
            for (String uuidString : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    long value = section.getLong(uuidString);
                    map.put(uuid, value);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in punishment data: " + uuidString);
                }
            }
        }
    }

    /**
     * Save a UUID to Integer map to config
     */
    private void saveIntegerMap(String mapName, Map<UUID, Integer> map) {
        for (Map.Entry<UUID, Integer> entry : map.entrySet()) {
            punishmentConfig.set("punishments." + mapName + "." + entry.getKey().toString(), entry.getValue());
        }
    }

    /**
     * Load a UUID to Integer map from config
     */
    private void loadIntegerMap(String mapName, Map<UUID, Integer> map) {
        map.clear();

        ConfigurationSection section = punishmentConfig.getConfigurationSection("punishments." + mapName);
        if (section != null) {
            for (String uuidString : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    int value = section.getInt(uuidString);
                    map.put(uuid, value);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in punishment data: " + uuidString);
                }
            }
        }
    }

    /**
     * Save a UUID set to config
     */
    private void saveUUIDSet(String setName, Set<UUID> set) {
        List<String> uuidStrings = new ArrayList<>();
        for (UUID uuid : set) {
            uuidStrings.add(uuid.toString());
        }
        punishmentConfig.set("punishments." + setName, uuidStrings);
    }

    /**
     * Load a UUID set from config
     */
    private void loadUUIDSet(String setName, Set<UUID> set) {
        set.clear();

        List<String> uuidStrings = punishmentConfig.getStringList("punishments." + setName);
        for (String uuidString : uuidStrings) {
            try {
                set.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in punishment data: " + uuidString);
            }
        }
    }

    /**
     * Save mining reputations (complex objects with Component)
     */
    private void saveMiningReputations() {
        for (Map.Entry<UUID, Map.Entry<Component, Long>> entry : miningReputations.entrySet()) {
            UUID uuid = entry.getKey();
            Component reputation = entry.getValue().getKey();
            Long expiry = entry.getValue().getValue();


            String serializedComponent = GsonComponentSerializer.gson().serialize(reputation);

            punishmentConfig.set("punishments.miningReputations." + uuid.toString() + ".reputation", serializedComponent);
            punishmentConfig.set("punishments.miningReputations." + uuid + ".expiry", expiry);
        }
    }

    /**
     * Load mining reputations
     */
    private void loadMiningReputations() {
        miningReputations.clear();

        ConfigurationSection section = punishmentConfig.getConfigurationSection("punishments.miningReputations");
        if (section != null) {
            for (String uuidString : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String serializedComponent = section.getString(uuidString + ".reputation");
                    Long expiry = section.getLong(uuidString + ".expiry");


                    Component reputation = GsonComponentSerializer.gson().deserialize(serializedComponent);

                    miningReputations.put(uuid, new AbstractMap.SimpleEntry<>(reputation, expiry));
                } catch (Exception e) {
                    plugin.getLogger().warning("Error loading mining reputation for: " + uuidString + " - " + e.getMessage());
                }
            }
        }
    }

    /**
     * Apply a specific punishment level to a player directly
     *
     * @param player The player to punish
     * @param level  The punishment level (0-6)
     */
    public void setPunishmentLevel(Player player, int level) {
        UUID playerId = player.getUniqueId();


        int currentLevel = playerPunishmentLevels.getOrDefault(playerId, 0);
        if (currentLevel > 0) {
            removeAllPunishmentEffects(player, currentLevel);
        }


        if (level > 0 && level <= 6) {
            applyPunishment(player, level);
            playerPunishmentLevels.put(playerId, level);
        } else if (level == 0) {

            playerPunishmentLevels.remove(playerId);
        }
    }

    /**
     * Remove all punishments from a player
     *
     * @param player The player to unpunish
     */
    public void removePunishment(Player player) {
        UUID playerId = player.getUniqueId();
        int currentLevel = playerPunishmentLevels.getOrDefault(playerId, 0);

        if (currentLevel > 0) {
            removeAllPunishmentEffects(player, currentLevel);
            playerPunishmentLevels.remove(playerId);
        }
    }

    /**
     * Starts a task to check for disabled levels and remove their effects
     */
    private void startDisabledLevelCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();


                    Integer currentLevel = playerPunishmentLevels.get(playerId);


                    if (currentLevel != null && currentLevel > 0) {
                        if (!configManager.isPunishmentEnabled(currentLevel)) {

                            removeAllPunishmentEffects(player, currentLevel);
                            playerPunishmentLevels.remove(playerId);

                            player.sendMessage(Component.text("Your punishment has been lifted due to a system update.", NamedTextColor.GREEN));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1200L, 6000L);
    }

    /**
     * Starts a repeating task to clean up expired punishment states
     * Optimized to reduce server load
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            private int runCount = 0;

            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();


                cleanupExpiredMaps(currentTime);


                if (getRunCount() % 3 == 0) {
                    cleanupFakeOreBlocks();
                }
            }

            private int getRunCount() {
                return runCount++;
            }
        }.runTaskTimer(plugin, 1200L, 6000L);
    }

    /**
     * Clean up expired maps more efficiently
     */
    private void cleanupExpiredMaps(long currentTime) {

        miningLicenseSuspension.values().removeIf(expiry -> expiry < currentTime);
        restrictedAreas.values().removeIf(expiry -> expiry < currentTime);
        resourceTaxActive.values().removeIf(expiry -> expiry < currentTime);
        cursedPickaxeActive.values().removeIf(expiry -> expiry < currentTime);
        stoneVisionActive.values().removeIf(expiry -> expiry < currentTime);
        paranoiaModeActive.values().removeIf(expiry -> expiry < currentTime);
        fakeOreVeinsActive.values().removeIf(expiry -> expiry < currentTime);
        foolsGoldActive.values().removeIf(expiry -> expiry < currentTime);
        miningReputations.entrySet().removeIf(entry -> entry.getValue().getValue() < currentTime);
    }

    /**
     * Clean up fake ore blocks that might have been missed
     */
    private void cleanupFakeOreBlocks() {


        fakeOreBlocks.removeIf(block -> {


            return !isOre(block.getType());
        });
    }

    /**
     * Helper method to check if a material is any ore
     * Added to PunishmentManager to avoid duplicate code
     */
    private boolean isOre(Material material) {
        switch (material) {
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
            case NETHER_GOLD_ORE:
            case NETHER_QUARTZ_ORE:
            case ANCIENT_DEBRIS:
                return true;
            default:
                return false;
        }
    }

    /**
     * Resets a player's mining reputation to "Trusted Miner"
     *
     * @param playerId UUID of the player
     */
    public void resetMiningReputation(UUID playerId) {
        miningReputations.remove(playerId);
    }

    /**
     * Sets a player's mining reputation
     *
     * @param playerId   UUID of the player
     * @param reputation The reputation string to set
     */
    public void setMiningReputation(UUID playerId, Component reputation, long duration) {
        long expiry = System.currentTimeMillis() + duration;
        miningReputations.put(playerId, new AbstractMap.SimpleEntry<>(reputation, expiry));
    }

    /**
     * Checks if a player has the "Untrusted Miner" reputation
     *
     * @param playerId UUID of the player
     * @return true if player has the Untrusted Miner reputation
     */
    public boolean isUntrustedMiner(UUID playerId) {
        Component reputation = getMiningReputation(playerId);

        return reputation != null && reputation.color() == NamedTextColor.RED && reputation.toString().contains("Untrusted Miner");
    }

    /**
     * Get the current mining reputation of a player
     */
    public Component getMiningReputation(UUID playerId) {
        if (miningReputations.containsKey(playerId)) {
            Map.Entry<Component, Long> entry = miningReputations.get(playerId);

            if (entry.getValue() > System.currentTimeMillis()) {
                return entry.getKey();
            } else {

                miningReputations.remove(playerId);
            }
        }
        return Component.text("Trusted Miner");
    }

    /**
     * Check if a player has a mining license suspension
     */
    public boolean hasMiningLicenseSuspension(UUID playerId) {
        return miningLicenseSuspension.containsKey(playerId) && miningLicenseSuspension.get(playerId) > System.currentTimeMillis();
    }

    /**
     * Get remaining time for mining license suspension in milliseconds
     */
    public long getMiningLicenseSuspensionTime(UUID playerId) {
        if (!hasMiningLicenseSuspension(playerId)) return 0;
        return miningLicenseSuspension.get(playerId) - System.currentTimeMillis();
    }

    /**
     * Check if a player has area restrictions
     */
    public boolean hasAreaRestriction(UUID playerId) {
        return restrictedAreas.containsKey(playerId) && restrictedAreas.get(playerId) > System.currentTimeMillis();
    }

    /**
     * Check if a player has resource tax active
     */
    public boolean hasResourceTax(UUID playerId) {
        return resourceTaxActive.containsKey(playerId) && resourceTaxActive.get(playerId) > System.currentTimeMillis();
    }

    /**
     * Check if a player has cursed pickaxe effect
     */
    public boolean hasCursedPickaxe(UUID playerId) {
        return cursedPickaxeActive.containsKey(playerId) && cursedPickaxeActive.get(playerId) > System.currentTimeMillis();
    }

    /**
     * Check if a player has stone vision effect
     */
    public boolean hasStoneVision(UUID playerId) {
        return stoneVisionActive.containsKey(playerId) && stoneVisionActive.get(playerId) > System.currentTimeMillis();
    }


    public boolean hasFoolsGold(UUID playerId) {
        return foolsGoldActive.containsKey(playerId) && foolsGoldActive.get(playerId) > System.currentTimeMillis();
    }

    /**
     * Check if a player has paranoia mode active
     */
    public boolean hasParanoiaMode(UUID playerId) {
        return paranoiaModeActive.containsKey(playerId) && paranoiaModeActive.get(playerId) > System.currentTimeMillis();
    }

    /**
     * Check if a player has fake diamonds remaining to be swapped
     */
    public boolean hasFakeDiamonds(UUID playerId) {
        return fakeDiamondsRemaining.containsKey(playerId) && fakeDiamondsRemaining.get(playerId) > 0;
    }

    /**
     * Get number of fake diamonds remaining for this player
     */
    public int getFakeDiamondsRemaining(UUID playerId) {
        return fakeDiamondsRemaining.getOrDefault(playerId, 0);
    }

    /**
     * Decrement fake diamonds remaining
     */
    public void decrementFakeDiamonds(UUID playerId) {
        int remaining = fakeDiamondsRemaining.getOrDefault(playerId, 0) - 1;
        if (remaining <= 0) {
            fakeDiamondsRemaining.remove(playerId);
        } else {
            fakeDiamondsRemaining.put(playerId, remaining);
        }
    }

    /**
     * Check if a player has permanent mining debuff
     */
    public boolean hasPermanentMiningDebuff(UUID playerId) {
        return permanentMiningDebuff.contains(playerId);
    }

    /**
     * Check if a block is a fake ore
     */
    public boolean isFakeOre(Block block) {
        return fakeOreBlocks.contains(block);
    }


    public boolean hasFakeOreVeins(UUID playerId) {
        return fakeOreVeinsActive.containsKey(playerId) && fakeOreVeinsActive.get(playerId) > System.currentTimeMillis();
    }

    /**
     * Improved register fake ore with better cleanup management
     */
    public void registerFakeOre(Block block) {

        if (!fakeOreBlocks.contains(block)) {
            fakeOreBlocks.add(block);


            new BukkitRunnable() {
                @Override
                public void run() {
                    fakeOreBlocks.remove(block);
                }
            }.runTaskLater(plugin, 36000L);
        }
    }

    /**
     * Remove a block from fake ores
     */
    public void removeFakeOre(Block block) {
        fakeOreBlocks.remove(block);
    }

    /**
     * Checks the player's suspicious count and applies punishment if the configured punishment for that level is enabled.
     *
     * @param player The player to check for punishment.
     */
    public void checkAndPunish(Player player) {
        UUID playerId = player.getUniqueId();
        int suspiciousCount = SuspiciousManager.getSuspiciousCounts().getOrDefault(playerId, 0);
        int level = suspiciousCount / 10;


        if (level < 1 || level > 6) {
            return;
        }


        if (configManager.isPunishmentEnabled(level)) {

            int currentLevel = playerPunishmentLevels.getOrDefault(playerId, 0);


            if (currentLevel > 0 && currentLevel < level) {
                removeAllPunishmentEffects(player, currentLevel);
            }


            if (currentLevel != level) {
                applyPunishment(player, level);
                playerPunishmentLevels.put(playerId, level);
            }
        }
    }

    /**
     * Called when an admin disables a punishment level in the config.
     * Immediately checks all online players and removes effects for the disabled level.
     *
     * @param level     The punishment level that was disabled
     * @param adminName The name of the admin who disabled the punishment level
     */
    public void onPunishmentLevelDisabled(int level, String adminName) {

        if (level < 1 || level > 6) {
            return;
        }

        int affectedPlayers = 0;


        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();


            Integer currentLevel = playerPunishmentLevels.get(playerId);


            if (currentLevel != null && currentLevel == level) {
                removeAllPunishmentEffects(player, level);
                playerPunishmentLevels.remove(playerId);

                player.sendMessage(Component.text("Your punishment has been lifted due to a system update.", NamedTextColor.GREEN));

                affectedPlayers++;
            }
        }


        plugin.getLogger().info("Admin " + adminName + " disabled punishment level " + level + ", affecting " + affectedPlayers + " players.");


        notifyAdminsAboutRemoval(level, adminName, affectedPlayers);
    }

    /**
     * Notify admins when a punishment is automatically removed
     */
    private void notifyAdminsAboutRemoval(int level, String adminName, int affectedCount) {
        Component message = Component.text("[DeepGuardX] ", NamedTextColor.BLUE).append(Component.text("Punishment level " + level + " was disabled by ", NamedTextColor.AQUA)).append(Component.text(adminName, NamedTextColor.YELLOW)).append(Component.text(", affecting " + affectedCount + " player" + (affectedCount == 1 ? "" : "s") + ".", NamedTextColor.AQUA));

        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("deepguardx.staff")) {
                admin.sendMessage(message);
            }
        }
    }

    public void removeAllPunishmentEffects(Player player, int level) {
        UUID playerId = player.getUniqueId();

        switch (level) {
            case 1:

                if (configManager.isPunishmentOptionEnabled(level, "mining_fatigue")) {
                    player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                }


                fakeDiamondsRemaining.remove(playerId);


                if (configManager.isPunishmentOptionEnabled(level, "heavy_pickaxe")) {
                    player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                }
                break;

            case 2:

                fakeOreVeinsActive.remove(playerId);


                if (configManager.isPunishmentOptionEnabled(level, "xray_vision_blur")) {
                    player.removePotionEffect(PotionEffectType.NAUSEA);
                }


                paranoiaModeActive.remove(playerId);
                break;

            case 3:

                miningLicenseSuspension.remove(playerId);


                resourceTaxActive.remove(playerId);


                player.removeMetadata("decoy_attraction", plugin);


                foolsGoldActive.remove(playerId);
                break;

            case 4:

                miningReputations.remove(playerId);


                restrictedAreas.remove(playerId);


                cursedPickaxeActive.remove(playerId);

            case 5:

                permanentMiningDebuff.remove(playerId);


                player.removeMetadata("requires_staff_review", plugin);


                stoneVisionActive.remove(playerId);
                break;

            case 6:


                break;
        }
    }

    /**
     * Gets a descriptive name for a punishment level
     */
    private String getPunishmentName(int level) {
        switch (level) {
            case 1:
                return "Warning Phase";
            case 2:
                return "Minor Consequences";
            case 3:
                return "Moderate Punishment";
            case 4:
                return "Severe Consequences";
            case 5:
                return "Critical Response";
            case 6:
                return "Maximum Enforcement";
            default:
                return "Level " + level + " Punishment";
        }
    }

    /**
     * Applies punishments based on the player's level and enabled options.
     */
    private void applyPunishment(Player player, int level) {
        UUID playerId = player.getUniqueId();


        if (plugin.getConfigManager().isWebhookAlertEnabled("punishment_applied")) {
            plugin.getWebhookManager().sendPunishmentAlert(player, level, getPunishmentName(level));
        }


        if (configManager.isPunishmentOptionEnabled(level, "admin_alert")) {
            alertAdmins(player, level);
        }


        if (configManager.isPunishmentOptionEnabled(level, "warning_message")) {
            sendWarningMessage(player, level);
        }

        switch (level) {
            case 1:
                if (configManager.isPunishmentOptionEnabled(level, "mining_fatigue")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 6000, 0, false, false, false));
                    player.sendMessage(Component.text("Your arms feel tired from suspicious mining techniques...", NamedTextColor.RED));
                }

                if (configManager.isPunishmentOptionEnabled(level, "fake_diamonds")) {
                    int fakeCount = ThreadLocalRandom.current().nextInt(1, 4);
                    fakeDiamondsRemaining.put(playerId, fakeCount);
                }


                if (configManager.isPunishmentOptionEnabled(level, "heavy_pickaxe")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 12000, 1, false, false, false));
                    player.sendMessage(Component.text("Your pickaxe feels suspiciously heavy...", NamedTextColor.RED));
                }
                break;

            case 2:
                if (configManager.isPunishmentOptionEnabled(level, "fake_ore_veins")) {
                    spawnFakeOreVeins(player);
                }

                if (configManager.isPunishmentOptionEnabled(level, "inventory_drop")) {
                    dropInventoryItems(player, 25);
                }

                if (configManager.isPunishmentOptionEnabled(level, "xray_vision_blur")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 6000, 0, false, false, false));
                    player.sendMessage(Component.text("Your suspicious vision is becoming blurry underground...", NamedTextColor.RED));
                }

                if (configManager.isPunishmentOptionEnabled(level, "tool_damage")) {
                    damageCurrentTool(player, 50);
                }

                if (configManager.isPunishmentOptionEnabled(level, "paranoia_mode")) {
                    long duration = System.currentTimeMillis() + (30 * 60 * 1000);
                    paranoiaModeActive.put(playerId, duration);
                    player.sendMessage(Component.text("The caves seem to be unusually active around you...", NamedTextColor.RED));
                }
                break;

            case 3:
                if (configManager.isPunishmentOptionEnabled(level, "temporary_kick")) {
                    Component kickMessage = Component.text("❌ DeepGuardX has detected suspicious X-ray patterns.\n", NamedTextColor.RED).append(Component.text("Please rejoin and mine responsibly.", NamedTextColor.YELLOW));
                    player.kick(kickMessage);
                }

                if (configManager.isPunishmentOptionEnabled(level, "mining_license_suspension")) {
                    long duration = System.currentTimeMillis() + (2 * 60 * 60 * 1000);
                    miningLicenseSuspension.put(playerId, duration);
                    player.sendMessage(Component.text("Your mining license for valuable ores has been suspended for 2 hours!", NamedTextColor.RED));
                }

                if (configManager.isPunishmentOptionEnabled(level, "resource_tax")) {
                    long duration = System.currentTimeMillis() + (3 * 60 * 60 * 1000);
                    resourceTaxActive.put(playerId, duration);
                    player.sendMessage(Component.text("The mining authorities have imposed a 50% resource tax on your mining!", NamedTextColor.RED));
                    player.sendMessage(Component.text("All mining drops will be reduced by half (rounded down).", NamedTextColor.YELLOW));
                }

                if (configManager.isPunishmentOptionEnabled(level, "decoy_attraction")) {


                    player.setMetadata("decoy_attraction", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    player.sendMessage(Component.text("Strange... the cave walls seem to shimmer with false promises...", NamedTextColor.RED));
                }

                if (configManager.isPunishmentOptionEnabled(level, "fools_gold")) {

                    long duration = System.currentTimeMillis() + (30 * 60 * 1000);
                    foolsGoldActive.put(playerId, duration);
                    player.sendMessage(Component.text("Your treasure detection appears to be malfunctioning...", NamedTextColor.RED));
                    player.sendMessage(Component.text("For the next 30 minutes, your diamonds might not be what they seem.", NamedTextColor.YELLOW));
                }
                break;

            case 4:
                if (configManager.isPunishmentOptionEnabled(level, "extended_ban")) {
                    Component banReason = Component.text("DeepGuardX: Suspicious mining patterns detected\n", NamedTextColor.RED).append(Component.text("Your account is suspended for 3 days.", NamedTextColor.YELLOW));

                    Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banReason.toString(), new java.util.Date(System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000), "DeepGuardX System");

                    Component kickMessage = Component.text("DeepGuardX: You have been banned for 3 days due to suspicious mining behavior.", NamedTextColor.RED);
                    player.kick(kickMessage);
                }

                if (configManager.isPunishmentOptionEnabled(level, "mining_reputation")) {

                    long duration = 7 * 24 * 60 * 60 * 1000;
                    Component reputationText = Component.text("Untrusted Miner", NamedTextColor.RED);
                    setMiningReputation(playerId, reputationText, duration);


                    player.sendMessage(Component.text("You have been flagged as an ", NamedTextColor.RED).append(Component.text("Untrusted Miner", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("!", NamedTextColor.RED)));

                    player.sendMessage(Component.text("This status will be visible to ", NamedTextColor.YELLOW).append(Component.text("all players", NamedTextColor.GOLD)).append(Component.text(" in chat for ", NamedTextColor.YELLOW)).append(Component.text("7 days", NamedTextColor.GOLD)).append(Component.text(".", NamedTextColor.YELLOW)));

                    player.sendMessage(Component.text("Your suspicious mining behavior will be publicly displayed.", NamedTextColor.GRAY));


                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
                }

                if (configManager.isPunishmentOptionEnabled(level, "restricted_areas")) {
                    long duration = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000);
                    restrictedAreas.put(playerId, duration);
                    player.sendMessage(Component.text("You are now restricted from mining below Y-level -40 for 7 days!", NamedTextColor.RED));
                }

                if (configManager.isPunishmentOptionEnabled(level, "cursed_pickaxe")) {
                    long duration = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000);
                    cursedPickaxeActive.put(playerId, duration);
                    player.sendMessage(Component.text("Your mining tools have been cursed! They may break when mining valuable ores.", NamedTextColor.RED));
                }
                break;

            case 5:
                if (configManager.isPunishmentOptionEnabled(level, "long_term_ban")) {
                    Component banReason = Component.text("DeepGuardX: Critical level of suspicious activity detected\n", NamedTextColor.RED).append(Component.text("Your account is suspended for 14 days.", NamedTextColor.YELLOW));

                    Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banReason.toString(), new java.util.Date(System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000), "DeepGuardX System");

                    Component kickMessage = Component.text("DeepGuardX: You have been banned for 14 days due to critical level of suspicious mining.", NamedTextColor.RED);
                    player.kick(kickMessage);
                }

                if (configManager.isPunishmentOptionEnabled(level, "public_notification")) {

                    player.setMetadata("public_notification", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                }

                if (configManager.isPunishmentOptionEnabled(level, "permanent_mining_debuff")) {
                    permanentMiningDebuff.add(playerId);
                    player.sendMessage(Component.text("Your mining abilities have been permanently restricted below Y-level 0!", NamedTextColor.RED));
                    player.sendMessage(Component.text("You will experience Mining Fatigue II when mining below this level.", NamedTextColor.YELLOW));
                }

                if (configManager.isPunishmentOptionEnabled(level, "staff_review")) {

                    player.setMetadata("requires_staff_review", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    player.sendMessage(Component.text("You now require staff approval to mine ores below Y-level 25!", NamedTextColor.RED));
                }

                if (configManager.isPunishmentOptionEnabled(level, "stone_vision")) {
                    long duration = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000);
                    stoneVisionActive.put(playerId, duration);
                    player.sendMessage(Component.text("Your vision has been altered - all ores now appear as stone to you!", NamedTextColor.RED));
                }
                break;

            case 6:
                if (configManager.isPunishmentOptionEnabled(level, "permanent_ban")) {
                    if (configManager.isPunishmentOptionEnabled(level, "tnt_execution")) {

                        Location loc = player.getLocation();
                        World world = loc.getWorld();


                        world.spawnParticle(Particle.EXPLOSION, loc, 5, 0.5, 0.5, 0.5);
                        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);


                        player.damage(player.getHealth() - 1);


                        player.sendMessage(Component.text("The DeepGuard system has detected critical levels of X-ray usage!", NamedTextColor.RED));


                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline()) {
                                    Component banReason = Component.text("DeepGuardX: Maximum level violation detected\n", NamedTextColor.RED).append(Component.text("Your account has been permanently banned.", NamedTextColor.YELLOW));

                                    Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banReason.toString(), null, "DeepGuardX System");

                                    Component kickMessage = Component.text("DeepGuardX: You have been permanently banned for maximum-level X-ray detection.", NamedTextColor.RED);
                                    player.kick(kickMessage);
                                }
                            }
                        }.runTaskLater(plugin, 60L);
                    } else {

                        Component banReason = Component.text("DeepGuardX: Maximum level violation detected\n", NamedTextColor.RED).append(Component.text("Your account has been permanently banned.", NamedTextColor.YELLOW));

                        Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banReason.toString(), null, "DeepGuardX System");

                        Component kickMessage = Component.text("DeepGuardX: You have been permanently banned for maximum-level X-ray detection.", NamedTextColor.RED);
                        player.kick(kickMessage);
                    }
                }

                if (configManager.isPunishmentOptionEnabled(level, "ip_tracking")) {

                    String ip = player.getAddress().getAddress().getHostAddress();
                    List<String> trackedIPs = plugin.getConfig().getStringList("tracked_ips");
                    if (!trackedIPs.contains(ip)) {
                        trackedIPs.add(ip);
                        plugin.getConfig().set("tracked_ips", trackedIPs);
                        plugin.saveConfig();
                    }
                }

                if (configManager.isPunishmentOptionEnabled(level, "security_report")) {

                    generateSecurityReport(player);
                }
                break;

            default:

                Component kickMessage = Component.text("Punished for suspicious behavior.", NamedTextColor.RED);
                player.kick(kickMessage);
        }
    }

    /**
     * Send a modern, attractive warning message to the player
     */
    private void sendWarningMessage(Player player, int level) {

        String titleText = "⚠ WARNING ⚠";
        String subtitleText;


        TextColor titleColor;
        TextColor borderColor;
        TextColor messageColor;
        TextColor highlightColor;


        Sound alertSound;
        float volume;
        float pitch;


        Particle warningParticle;


        String message1, message2, message3;


        switch (level) {
            case 1:
                message1 = "Suspicious mining activity detected!";
                message2 = "This is your first warning.";
                message3 = "Further violations will be punished.";
                subtitleText = "Level 1 - First Warning";
                titleColor = TextColor.color(255, 255, 0);
                borderColor = TextColor.color(230, 126, 34);
                messageColor = TextColor.color(236, 240, 241);
                highlightColor = TextColor.color(241, 196, 15);
                alertSound = Sound.BLOCK_NOTE_BLOCK_PLING;
                volume = 0.8f;
                pitch = 1.0f;
                warningParticle = Particle.END_ROD;
                break;

            case 2:
                message1 = "Continued suspicious mining detected!";
                message2 = "This is your second warning.";
                message3 = "Your actions are being monitored closely.";
                subtitleText = "Level 2 - Second Warning";
                titleColor = TextColor.color(255, 165, 0);
                borderColor = TextColor.color(231, 76, 60);
                messageColor = TextColor.color(236, 240, 241);
                highlightColor = TextColor.color(230, 126, 34);
                alertSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
                volume = 1.0f;
                pitch = 0.8f;
                warningParticle = Particle.SMOKE;
                break;

            case 3:
                message1 = "Multiple violations of mining policy!";
                message2 = "This is a serious offense.";
                message3 = "Further violations will result in severe action.";
                subtitleText = "Level 3 - Serious Offense";
                titleColor = TextColor.color(255, 69, 0);
                borderColor = TextColor.color(192, 57, 43);
                messageColor = TextColor.color(236, 240, 241);
                highlightColor = TextColor.color(231, 76, 60);
                alertSound = Sound.ENTITY_ELDER_GUARDIAN_CURSE;
                volume = 0.4f;
                pitch = 1.0f;
                warningParticle = Particle.ANGRY_VILLAGER;
                break;

            case 4:
                message1 = "Severe mining violations detected!";
                message2 = "This is a major offense.";
                message3 = "Your mining privileges are now restricted.";
                subtitleText = "Level 4 - Major Offense";
                titleColor = TextColor.color(255, 0, 0);
                borderColor = TextColor.color(153, 0, 0);
                messageColor = TextColor.color(236, 240, 241);
                highlightColor = TextColor.color(192, 57, 43);
                alertSound = Sound.BLOCK_BELL_USE;
                volume = 1.0f;
                pitch = 0.6f;
                warningParticle = Particle.FLAME;
                break;

            case 5:
                message1 = "Critical violation of server mining policy!";
                message2 = "Your actions have been flagged for review.";
                message3 = "Mining privileges severely restricted.";
                subtitleText = "Level 5 - Critical Violation";
                titleColor = TextColor.color(139, 0, 0);
                borderColor = TextColor.color(88, 0, 0);
                messageColor = TextColor.color(236, 240, 241);
                highlightColor = TextColor.color(153, 0, 0);
                alertSound = Sound.ENTITY_WITHER_SPAWN;
                volume = 0.3f;
                pitch = 0.5f;
                warningParticle = Particle.SOUL_FIRE_FLAME;
                break;

            case 6:
                message1 = "MAXIMUM VIOLATION DETECTED!";
                message2 = "Your account is subject to permanent action.";
                message3 = "All mining activity has been logged for review.";
                subtitleText = "Level 6 - MAXIMUM VIOLATION";
                titleColor = TextColor.color(128, 0, 128);
                borderColor = TextColor.color(75, 0, 130);
                messageColor = TextColor.color(236, 240, 241);
                highlightColor = TextColor.color(139, 0, 139);
                alertSound = Sound.ENTITY_LIGHTNING_BOLT_THUNDER;
                volume = 0.7f;
                pitch = 0.3f;
                warningParticle = Particle.SOUL;
                break;

            default:
                message1 = "Suspicious mining activity detected!";
                message2 = "This behavior is not permitted.";
                message3 = "Further violations will be punished.";
                subtitleText = "Mining Violation Detected";
                titleColor = TextColor.color(255, 255, 0);
                borderColor = TextColor.color(230, 126, 34);
                messageColor = TextColor.color(236, 240, 241);
                highlightColor = TextColor.color(241, 196, 15);
                alertSound = Sound.BLOCK_NOTE_BLOCK_BASS;
                volume = 0.8f;
                pitch = 1.0f;
                warningParticle = Particle.END_ROD;
        }


        player.showTitle(net.kyori.adventure.title.Title.title(Component.text(titleText).color(titleColor).decoration(TextDecoration.BOLD, true), Component.text(subtitleText).color(highlightColor), net.kyori.adventure.title.Title.Times.times(net.kyori.adventure.util.Ticks.duration(10), net.kyori.adventure.util.Ticks.duration(70), net.kyori.adventure.util.Ticks.duration(20))));


        Component topBorder = Component.text("╔═════════ " + getStars(level) + " ═════════╗").color(borderColor);
        Component titleLine = Component.text("║ ").color(borderColor).append(Component.text("DeepGuardX WARNING").color(titleColor).decoration(TextDecoration.BOLD, true)).append(Component.text(" ║").color(borderColor));
        Component separator = Component.text("╠════════════════════════════╣").color(borderColor);
        Component message1Line = Component.text("║ ").color(borderColor).append(Component.text(message1).color(messageColor)).append(Component.text(" ║").color(borderColor));
        Component message2Line = Component.text("║ ").color(borderColor).append(Component.text(message2).color(messageColor)).append(Component.text(" ║").color(borderColor));
        Component message3Line = Component.text("║ ").color(borderColor).append(Component.text(message3).color(messageColor)).append(Component.text(" ║").color(borderColor));
        Component bottomBorder = Component.text("╚════════════════════════════╝").color(borderColor);


        Component appealLine = Component.text("║      ").color(borderColor).append(Component.text("[APPEAL]").color(TextColor.color(255, 215, 0)).decoration(TextDecoration.BOLD, true).clickEvent(ClickEvent.suggestCommand("/dgx appeal ")).hoverEvent(HoverEvent.showText(Component.text("Click to appeal this warning to staff")))).append(Component.text("      ║").color(borderColor));


        player.sendMessage(Component.text(""));
        player.sendMessage(topBorder);
        player.sendMessage(titleLine);
        player.sendMessage(separator);
        player.sendMessage(message1Line);
        player.sendMessage(message2Line);
        player.sendMessage(message3Line);
        player.sendMessage(separator);
        player.sendMessage(appealLine);
        player.sendMessage(bottomBorder);
        player.sendMessage(Component.text(""));


        player.sendMessage(Component.text("If you believe this detection was incorrect, click [APPEAL] to explain your case to the staff team.").color(TextColor.color(200, 200, 200)));


        player.playSound(player.getLocation(), alertSound, volume, pitch);


        Location loc = player.getLocation();
        for (int i = 0; i < 20; i++) {
            double angle = i * (Math.PI / 10);
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            player.getWorld().spawnParticle(warningParticle, loc.getX() + x, loc.getY() + 1, loc.getZ() + z, 3, 0, 0, 0, 0.05);
        }


        Component actionBarMsg = Component.text("⚠ Warning Level " + level + " ⚠").color(titleColor);
        player.sendActionBar(actionBarMsg);


        if (level >= 4) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 30, level - 3));
        }
    }

    /**
     * Helper method to generate stars based on warning level
     */
    private String getStars(int level) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < level; i++) {
            stars.append("★");
        }
        for (int i = level; i < 6; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }

    /**
     * Send a modern, interactive alert to all online staff members
     */
    private void alertAdmins(Player violator, int level) {

        String severityText;
        TextColor severityColor;

        switch (level) {
            case 1:
                severityText = "potential";
                severityColor = TextColor.color(255, 255, 0);
                break;
            case 2:
                severityText = "suspicious";
                severityColor = TextColor.color(255, 165, 0);
                break;
            case 3:
                severityText = "concerning";
                severityColor = TextColor.color(255, 69, 0);
                break;
            case 4:
                severityText = "severe";
                severityColor = TextColor.color(255, 0, 0);
                break;
            case 5:
                severityText = "critical";
                severityColor = TextColor.color(139, 0, 0);
                break;
            case 6:
                severityText = "MAXIMUM";
                severityColor = TextColor.color(128, 0, 128);
                break;
            default:
                severityText = "suspicious";
                severityColor = TextColor.color(255, 165, 0);
        }


        int blocksMinedRecently = getPlayerRecentBlocksMined(violator);
        String locationInfo = String.format("%.1f, %.1f, %.1f (%s)", violator.getLocation().getX(), violator.getLocation().getY(), violator.getLocation().getZ(), violator.getWorld().getName());


        Component prefix = Component.text("[").color(TextColor.color(169, 169, 169)).append(Component.text("DeepGuardX").color(TextColor.color(220, 20, 60))).append(Component.text("] ").color(TextColor.color(169, 169, 169)));

        Component playerName = Component.text(violator.getName()).color(severityColor).hoverEvent(HoverEvent.showText(Component.text("Player Info:\n").append(Component.text("UUID: " + violator.getUniqueId() + "\n")).append(Component.text("IP: [Redacted]\n")).append(Component.text("Join Date: " + getPlayerJoinDate(violator) + "\n")).append(Component.text("Recent Blocks Mined: " + blocksMinedRecently))));

        Component severityInfo = Component.text(" triggered a " + severityText + " Level " + level + " X-ray detection.").color(severityColor);


        Component message = prefix.append(playerName).append(severityInfo);


        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("deepguardx.staff")) {
                admin.sendMessage(message);


                float pitch = Math.min(2.0f, 0.5f + (level * 0.25f));
                Sound alertSound = (level >= 5) ? Sound.ENTITY_ELDER_GUARDIAN_DEATH : Sound.BLOCK_NOTE_BLOCK_PLING;
                admin.playSound(admin.getLocation(), alertSound, 0.5f, pitch);


                if (level >= 4) {
                    Component alertBar = Component.text("⚠ ").color(TextColor.color(255, 0, 0)).append(Component.text("X-RAY ALERT: " + violator.getName() + " - Level " + level)).append(Component.text(" ⚠").color(TextColor.color(255, 0, 0)));


                    admin.sendActionBar(alertBar);
                }
            }
        }


        Bukkit.getConsoleSender().sendMessage("[DeepGuardX] ALERT: " + violator.getName() + " triggered a " + severityText + " Level " + level + " X-ray detection at " + locationInfo);
    }

    /**
     * Get player join date - implement this based on your data storage
     */
    private String getPlayerJoinDate(Player player) {

        return "Unknown";
    }

    /**
     * Get player recent blocks mined - implement this based on your tracking
     */
    private int getPlayerRecentBlocksMined(Player player) {

        return 0;
    }


    /**
     * Spawn fake ore veins around a player
     */
    private void spawnFakeOreVeins(Player player) {
        UUID playerId = player.getUniqueId();

        long duration = System.currentTimeMillis() + (2 * 60 * 60 * 1000);
        fakeOreVeinsActive.put(playerId, duration);
        player.sendMessage(Component.text("The ore veins around you seem... unstable. Mining valuable ores may yield only stone.", NamedTextColor.RED));
    }

    /**
     * Drop a percentage of a player's ore items from their inventory
     * The items are actually removed entirely, not dropped in the world
     */
    private void dropInventoryItems(Player player, int percentage) {
        if (percentage <= 0) return;

        UUID playerId = player.getUniqueId();
        java.util.Random random = new java.util.Random();
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();


        List<Map.Entry<Integer, ItemStack>> oreItems = new ArrayList<>();


        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && isOreItem(item.getType())) {
                oreItems.add(new AbstractMap.SimpleEntry<>(i, item));
            }
        }


        if (oreItems.isEmpty()) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("No ore items found in inventory for player " + player.getName());
            }
            return;
        }


        int itemsToRemove = Math.max(1, (oreItems.size() * percentage) / 100);


        itemsToRemove = Math.min(itemsToRemove, oreItems.size());

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Removing " + itemsToRemove + " ore items from player " + player.getName() + "'s inventory (" + percentage + "% of " + oreItems.size() + " ore items)");
        }


        int totalItemsRemoved = 0;
        Map<Material, Integer> removedCounts = new HashMap<>();


        for (int i = 0; i < itemsToRemove; i++) {
            if (oreItems.isEmpty()) break;


            int randomIndex = random.nextInt(oreItems.size());
            Map.Entry<Integer, ItemStack> selectedEntry = oreItems.remove(randomIndex);
            int slot = selectedEntry.getKey();
            ItemStack selectedItem = selectedEntry.getValue();


            Material material = selectedItem.getType();
            int amount = selectedItem.getAmount();
            totalItemsRemoved += amount;


            removedCounts.put(material, removedCounts.getOrDefault(material, 0) + amount);


            inventory.setItem(slot, null);
        }


        player.sendMessage(Component.text("The DeepGuard system has confiscated " + totalItemsRemoved + " ore items from your inventory!", NamedTextColor.RED));


        if (!removedCounts.isEmpty()) {
            for (Map.Entry<Material, Integer> entry : removedCounts.entrySet()) {
                player.sendMessage(Component.text("- " + entry.getValue() + " " + formatItemName(entry.getKey()), NamedTextColor.YELLOW));
            }
        }


        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
    }

    /**
     * Helper method to format material names for display
     */
    private String formatItemName(Material material) {
        String name = material.name();
        name = name.replace('_', ' ').toLowerCase();

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Helper method to check if an item is an ore or ore-related item
     */
    private boolean isOreItem(Material material) {
        String name = material.name();
        return name.contains("ORE") || name.contains("INGOT") || name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("GOLD") || name.contains("IRON") || name.contains("COPPER") || name.contains("COAL") || name.contains("LAPIS") || name.contains("REDSTONE") || name.contains("QUARTZ") || name.contains("NETHERITE") || name.contains("RAW_") || name.equals("ANCIENT_DEBRIS");
    }

    /**
     * Damage the player's current tool by a percentage
     */
    private void damageCurrentTool(Player player, int percentage) {

    }

    /**
     * Generate a detailed security report for admins
     */
    private void generateSecurityReport(Player player) {

    }

    /**
     * Get the count of blocks mined by a player (example function)
     */
    private int getMinedCount(UUID playerId, Material material) {

        return ThreadLocalRandom.current().nextInt(100, 500);
    }

    /**
     * Get the current punishment level of a player
     */
    public int getPlayerPunishmentLevel(UUID playerId) {
        return playerPunishmentLevels.getOrDefault(playerId, 0);
    }

    public void onDisable() {

        savePunishmentData();


        if (autoSaveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
        }
    }
}