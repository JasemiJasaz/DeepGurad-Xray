package org.myplugin.deepGuardXray.alerts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.myplugin.deepGuardXray.config.ConfigManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.myplugin.deepGuardXray.utils.LocationUtils.formatLocation;
import static org.myplugin.deepGuardXray.utils.LocationUtils.getFriendlyWorldName;

public class StaffAlertManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Set<UUID> oreAlertToggledOff = new HashSet<>();
    private final Map<UUID, Map<Material, OreCounter>> staffOreTracker = new HashMap<>();

    public StaffAlertManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Get a formatted and colored ore name from a Material
     *
     * @param ore The ore Material
     * @return Formatted and colored ore name as Component
     */
    public Component getFormattedOreName(Material ore) {
        String oreName = ore.name();
        String formattedName = oreName.replace("_", " ");
        String[] words = formattedName.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        formattedName = result.toString().trim();
        return switch (ore) {
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> Component.text(formattedName).color(NamedTextColor.AQUA);
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> Component.text(formattedName).color(NamedTextColor.GREEN);
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE ->
                    Component.text(formattedName).color(NamedTextColor.GOLD);
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Component.text(formattedName).color(NamedTextColor.GRAY);
            case COAL_ORE, DEEPSLATE_COAL_ORE -> Component.text(formattedName).color(NamedTextColor.DARK_GRAY);
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Component.text(formattedName).color(NamedTextColor.GOLD);
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> Component.text(formattedName).color(NamedTextColor.RED);
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> Component.text(formattedName).color(NamedTextColor.BLUE);
            case NETHER_QUARTZ_ORE -> Component.text(formattedName).color(NamedTextColor.WHITE);
            case ANCIENT_DEBRIS -> Component.text(formattedName).color(NamedTextColor.DARK_PURPLE);
            default -> Component.text(formattedName).color(NamedTextColor.YELLOW);
        };
    }

    /**
     * Get a formatted ore name with legacy color codes for logging
     *
     * @param ore The ore Material
     * @return Formatted ore name with legacy color codes
     */
    public String getFormattedOreNameLegacy(Material ore) {
        String oreName = ore.name();
        String formattedName = oreName.replace("_", " ");
        String[] words = formattedName.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return result.toString().trim();
    }

    public boolean toggleOreAlert(Player player) {
        UUID uuid = player.getUniqueId();
        if (oreAlertToggledOff.contains(uuid)) {
            oreAlertToggledOff.remove(uuid);
            return false;
        } else {
            oreAlertToggledOff.add(uuid);
            return true;
        }
    }

    public boolean areAlertsToggledOff(UUID uuid) {
        return oreAlertToggledOff.contains(uuid);
    }

    public void updateStaffOreCounter(Player player, Material ore, Location loc) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long resetTimeMillis = configManager.getStaffOreResetTime() * 50L;
        long detectionTimeWindow = 120000L;
        long alertCooldown = 300000L;

        Map<Material, OreCounter> oreMap = staffOreTracker.computeIfAbsent(uuid, k -> new HashMap<>());
        OreCounter counter = oreMap.get(ore);
        if (counter == null) {
            counter = new OreCounter();
        } else if (currentTime - counter.lastUpdate > resetTimeMillis) {
            counter = new OreCounter();
        }

        counter.miningTimestamps.add(currentTime);
        counter.count++;
        counter.lastUpdate = currentTime;
        oreMap.put(ore, counter);

        int recentOreCount = counter.getCountInTimeWindow(detectionTimeWindow);

        boolean isSuspiciousMining = false;
        double suspicionScore = 0.0;
        String pattern = "";

        if ((ore == Material.DIAMOND_ORE || ore == Material.DEEPSLATE_DIAMOND_ORE) && recentOreCount >= 8) {
            isSuspiciousMining = true;
            suspicionScore = Math.min(recentOreCount * 3, 100.0);
            pattern = "High Diamond Frequency (2min)";
        } else if ((ore == Material.ANCIENT_DEBRIS) && recentOreCount >= 4) {
            isSuspiciousMining = true;
            suspicionScore = Math.min(recentOreCount * 15.0, 100.0);
            pattern = "High Ancient Debris Frequency (2min)";
        } else if ((ore == Material.EMERALD_ORE || ore == Material.DEEPSLATE_EMERALD_ORE) && recentOreCount >= 12) {
            isSuspiciousMining = true;
            suspicionScore = Math.min(recentOreCount * 6.0, 100);
            pattern = "High Emerald Frequency (2min)";
        }

        if (loc.getY() < 0 && (ore == Material.DIAMOND_ORE || ore == Material.DEEPSLATE_DIAMOND_ORE || ore == Material.ANCIENT_DEBRIS)) {
            suspicionScore += 2;
        }

        if (isSuspiciousMining && (currentTime - counter.lastAlertSent > alertCooldown) && plugin instanceof org.myplugin.deepGuardXray.deepGuardXray dgxPlugin) {

            if (dgxPlugin.getConfigManager().isWebhookAlertEnabled("suspicious_mining")) {
                dgxPlugin.getWebhookManager().sendSuspiciousMiningAlert(player, pattern, suspicionScore);

                counter.lastAlertSent = currentTime;
            }
        }

        if (isSuspiciousMining || configManager.isStaffOreAlerts()) {
            String friendlyWorld = getFriendlyWorldName(player.getWorld());
            String formattedLoc = formatLocation(loc);
            Component formattedOreName = getFormattedOreName(ore);

            String logMessage;
            if (isSuspiciousMining) {
                logMessage = "SUSPICIOUS: Player " + player.getName() + " mined " + recentOreCount + " " + getFormattedOreNameLegacy(ore) + " in 2 minutes at " + friendlyWorld;
            } else {
                logMessage = "Player " + player.getName() + " mined " + counter.count + " " + getFormattedOreNameLegacy(ore) + " at " + friendlyWorld;
            }

            Component baseMessage;
            if (isSuspiciousMining) {
                baseMessage = Component.text("[SUSPICIOUS] ").color(NamedTextColor.RED).append(Component.text("Player " + player.getName() + " mined " + recentOreCount + " ").color(NamedTextColor.YELLOW)).append(formattedOreName).append(Component.text(" in 2 minutes at " + friendlyWorld).color(NamedTextColor.YELLOW));
            } else {
                baseMessage = Component.text("[OreAlert] ").color(NamedTextColor.AQUA).append(Component.text("Player " + player.getName() + " mined " + counter.count + " ").color(NamedTextColor.RED)).append(formattedOreName).append(Component.text(" at " + friendlyWorld).color(NamedTextColor.RED));
            }

            Component teleportComponent = Component.text(" [Click to teleport]").color(NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand("/dgx teleport " + player.getName())).hoverEvent(HoverEvent.showText(Component.text("Teleport to " + player.getName())));

            Component fullMessage = baseMessage.append(teleportComponent);

            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (online.hasPermission("deepguardx.staff") && !areAlertsToggledOff(online.getUniqueId()) && (isSuspiciousMining || configManager.isStaffOreAlerts())) {
                    online.sendMessage(fullMessage);
                }
            }

            logDecoyEvent(logMessage);
        }
    }

    public void logDecoyEvent(String message) {
        File logFile = new File(plugin.getDataFolder(), "decoy-warnings.log");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true))) {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            bw.write("[" + timeStamp + "] " + message);
            bw.newLine();
        } catch (IOException e) {
            plugin.getLogger().severe("Could not write to decoy-warnings.log: " + e.getMessage());
        }
    }

    public void alertStaffWithTeleport(Player player, Location loc, String rawMessage) {
        String friendlyWorld = getFriendlyWorldName(loc.getWorld());
        String formattedLoc = formatLocation(loc);
        String logMessage = rawMessage.replace(loc.toString(), friendlyWorld + " " + formattedLoc);

        Component baseMsg = Component.text("[DeepGuardX] ").color(NamedTextColor.GOLD).append(Component.text(logMessage).color(NamedTextColor.RED));

        Component tpComponent = Component.text(" [Click to teleport]").color(NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/dgx teleport " + player.getName())).hoverEvent(HoverEvent.showText(Component.text("Teleport to " + player.getName())));

        Component fullMessage = baseMsg.append(tpComponent);

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission("deepguardx.staff")) {
                online.sendMessage(fullMessage);
            }
        }

        logDecoyEvent(logMessage);
    }

    private static class OreCounter {
        int count = 0;
        long lastUpdate = 0;
        long lastAlertSent = 0;
        List<Long> miningTimestamps = new ArrayList<>();

        public int getCountInTimeWindow(long timeWindowMillis) {
            long currentTime = System.currentTimeMillis();
            long cutoffTime = currentTime - timeWindowMillis;

            miningTimestamps.removeIf(timestamp -> timestamp < cutoffTime);

            return miningTimestamps.size();
        }
    }
}