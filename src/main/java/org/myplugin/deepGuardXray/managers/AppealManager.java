package org.myplugin.deepGuardXray.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.myplugin.deepGuardXray.deepGuardXray;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class AppealManager implements Listener {
    private final deepGuardXray plugin;
    private final File appealFile;
    private final Map<Integer, Appeal> appealById = new HashMap<>();
    private final Map<UUID, List<Appeal>> appealsByPlayer = new HashMap<>();
    private int nextAppealId = 1;

    public AppealManager(deepGuardXray plugin) {
        this.plugin = plugin;
        this.appealFile = new File(plugin.getDataFolder(), "appeals.yml");


        plugin.getServer().getPluginManager().registerEvents(this, plugin);


        loadAppeals();
    }

    /**
     * Create a new appeal
     */
    public Appeal createAppeal(Player player, int punishmentLevel, String reason) {
        UUID playerId = player.getUniqueId();


        for (Appeal existingAppeal : getAppealsForPlayer(playerId)) {
            if ((existingAppeal.getStatus() == AppealStatus.PENDING || existingAppeal.getStatus() == AppealStatus.UNDER_REVIEW) && existingAppeal.getPunishmentLevel() == punishmentLevel) {
                return existingAppeal;
            }
        }


        int appealId = getNextAppealId();


        Appeal appeal = new Appeal(appealId, playerId, player.getName(), punishmentLevel, reason);


        appealById.put(appealId, appeal);
        appealsByPlayer.computeIfAbsent(playerId, k -> new ArrayList<>()).add(appeal);


        saveAppeals();


        if (plugin.getConfigManager().isWebhookAlertEnabled("appeal_updates")) {
            plugin.getWebhookManager().sendNewAppealAlert(appealId, player.getName(), punishmentLevel, reason);
        }


        notifyStaffAboutNewAppeal(appeal);

        return appeal;
    }

    /**
     * Get the next available appeal ID
     */
    private synchronized int getNextAppealId() {
        int id = nextAppealId++;
        return id;
    }

    /**
     * Get an appeal by ID
     */
    public Appeal getAppeal(int appealId) {
        return appealById.get(appealId);
    }

    /**
     * Get all appeals for a player
     */
    public List<Appeal> getAppealsForPlayer(UUID playerId) {
        return appealsByPlayer.getOrDefault(playerId, new ArrayList<>());
    }

    /**
     * Get the currently active appeal for a player (pending or under review)
     */
    public Appeal getActiveAppealForPlayer(UUID playerId) {
        List<Appeal> appeals = getAppealsForPlayer(playerId);


        int currentPunishmentLevel = plugin.getPunishmentManager().getPlayerPunishmentLevel(playerId);


        for (Appeal appeal : appeals) {
            if ((appeal.getStatus() == AppealStatus.PENDING || appeal.getStatus() == AppealStatus.UNDER_REVIEW) && appeal.getPunishmentLevel() == currentPunishmentLevel) {
                return appeal;
            }
        }


        return null;
    }

    /**
     * Get all pending appeals
     */
    public List<Appeal> getPendingAppeals() {
        return appealById.values().stream().filter(appeal -> appeal.getStatus() == AppealStatus.PENDING || appeal.getStatus() == AppealStatus.UNDER_REVIEW).collect(Collectors.toList());
    }

    /**
     * Update an appeal status
     */
    public void updateAppealStatus(int appealId, AppealStatus newStatus, String staffName, String response) {
        Appeal appeal = appealById.get(appealId);
        if (appeal != null) {
            appeal.setStatus(newStatus);
            appeal.setStaffName(staffName);
            appeal.setStaffResponse(response);


            saveAppeals();


            if (plugin.getConfigManager().isWebhookAlertEnabled("appeal_updates")) {
                plugin.getWebhookManager().sendAppealStatusAlert(appealId, appeal.getPlayerName(), appeal.getPunishmentLevel(), newStatus, staffName, response);
            }


            if (newStatus == AppealStatus.APPROVED) {
                Player player = Bukkit.getPlayer(appeal.getPlayerId());
                if (player != null && player.isOnline()) {
                    plugin.getPunishmentManager().removePunishment(player);
                    player.sendMessage(Component.text("Your appeal has been approved! Your punishment has been removed.").color(NamedTextColor.GREEN));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                }
            } else if (newStatus == AppealStatus.DENIED) {

                Player player = Bukkit.getPlayer(appeal.getPlayerId());
                if (player != null && player.isOnline()) {
                    player.sendMessage(Component.text("Your appeal has been denied.").color(NamedTextColor.RED));
                    if (!response.isEmpty()) {
                        player.sendMessage(Component.text("Reason: " + response).color(NamedTextColor.YELLOW));
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                }
            } else if (newStatus == AppealStatus.UNDER_REVIEW) {

                Player player = Bukkit.getPlayer(appeal.getPlayerId());
                if (player != null && player.isOnline()) {
                    player.sendMessage(Component.text("Your appeal is now under review by staff.").color(NamedTextColor.AQUA));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
                }
            }
        }
    }

    /**
     * Notify staff about a new appeal
     */
    private void notifyStaffAboutNewAppeal(Appeal appeal) {
        Component message = Component.text("[").color(NamedTextColor.DARK_GRAY).append(Component.text("DeepGuardX").color(NamedTextColor.RED)).append(Component.text("] ").color(NamedTextColor.DARK_GRAY)).append(Component.text("New appeal #" + appeal.getId() + " from ").color(NamedTextColor.YELLOW)).append(Component.text(appeal.getPlayerName()).color(NamedTextColor.AQUA)).append(Component.text(" (Level " + appeal.getPunishmentLevel() + ")").color(NamedTextColor.YELLOW));


        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("deepguardx.staff")) {
                staff.sendMessage(message);
                staff.playSound(staff.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.2f);
            }
        }
    }

    /**
     * Notify staff about pending appeals when they join
     */
    @EventHandler
    public void onStaffJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();


        if (player.hasPermission("deepguardx.staff")) {

            List<Appeal> pendingAppeals = getPendingAppeals();

            if (!pendingAppeals.isEmpty()) {

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage(Component.text("╔═════════════════════════════╗").color(NamedTextColor.AQUA));
                    player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("DeepGuardX Appeal System").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
                    player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("There ").color(NamedTextColor.WHITE)).append(Component.text(pendingAppeals.size() == 1 ? "is " : "are ").color(NamedTextColor.WHITE)).append(Component.text(pendingAppeals.size()).color(NamedTextColor.GOLD)).append(Component.text(" pending ").color(NamedTextColor.WHITE)).append(Component.text(pendingAppeals.size() == 1 ? "appeal" : "appeals").color(NamedTextColor.WHITE)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
                    player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("Check the Staff GUI to review them").color(NamedTextColor.YELLOW)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
                    player.sendMessage(Component.text("╚═════════════════════════════╝").color(NamedTextColor.AQUA));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.2f);
                }, 60L);
            }
        }
    }

    /**
     * Load appeals from file
     */
    private void loadAppeals() {
        if (!appealFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(appealFile);
        ConfigurationSection appealsSection = config.getConfigurationSection("appeals");

        if (appealsSection == null) {
            return;
        }


        appealById.clear();
        appealsByPlayer.clear();


        int highestId = 0;


        for (String key : appealsSection.getKeys(false)) {
            ConfigurationSection appealSection = appealsSection.getConfigurationSection(key);
            if (appealSection == null) continue;

            int id = Integer.parseInt(key);
            highestId = Math.max(highestId, id);

            UUID playerId = UUID.fromString(appealSection.getString("playerId"));
            String playerName = appealSection.getString("playerName");
            int punishmentLevel = appealSection.getInt("punishmentLevel");
            String reason = appealSection.getString("reason");
            long timestamp = appealSection.getLong("timestamp");
            AppealStatus status = AppealStatus.fromString(appealSection.getString("status"));
            String staffResponse = appealSection.getString("staffResponse", "");
            String staffName = appealSection.getString("staffName", "");

            Appeal appeal = new Appeal(id, playerId, playerName, punishmentLevel, reason, timestamp, status, staffResponse, staffName);


            appealById.put(id, appeal);
            appealsByPlayer.computeIfAbsent(playerId, k -> new ArrayList<>()).add(appeal);
        }


        nextAppealId = highestId + 1;
    }

    /**
     * Save appeals to file
     */
    private void saveAppeals() {
        FileConfiguration config = new YamlConfiguration();


        ConfigurationSection appealsSection = config.createSection("appeals");


        for (Appeal appeal : appealById.values()) {
            ConfigurationSection appealSection = appealsSection.createSection(String.valueOf(appeal.getId()));
            appealSection.set("playerId", appeal.getPlayerId().toString());
            appealSection.set("playerName", appeal.getPlayerName());
            appealSection.set("punishmentLevel", appeal.getPunishmentLevel());
            appealSection.set("reason", appeal.getReason());
            appealSection.set("timestamp", appeal.getTimestamp());
            appealSection.set("status", appeal.getStatus().name());
            appealSection.set("staffResponse", appeal.getStaffResponse());
            appealSection.set("staffName", appeal.getStaffName());
        }

        try {
            config.save(appealFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save appeals data: " + e.getMessage());
        }
    }

    /**
     * Appeal status enum
     */
    public enum AppealStatus {
        PENDING("Pending", TextColor.color(255, 215, 0)), APPROVED("Approved", TextColor.color(50, 205, 50)), DENIED("Denied", TextColor.color(255, 0, 0)), UNDER_REVIEW("Under Review", TextColor.color(30, 144, 255));

        private final String displayName;
        private final TextColor color;

        AppealStatus(String displayName, TextColor color) {
            this.displayName = displayName;
            this.color = color;
        }

        public static AppealStatus fromString(String statusStr) {
            try {
                return valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return PENDING;
            }
        }

        public String getDisplayName() {
            return displayName;
        }

        public TextColor getColor() {
            return color;
        }
    }

    /**
     * Appeal data structure
     */
    public static class Appeal {
        private final int id;
        private final UUID playerId;
        private final String playerName;
        private final int punishmentLevel;
        private final String reason;
        private final long timestamp;
        private AppealStatus status;
        private String staffResponse;
        private String staffName;

        public Appeal(int id, UUID playerId, String playerName, int punishmentLevel, String reason) {
            this.id = id;
            this.playerId = playerId;
            this.playerName = playerName;
            this.punishmentLevel = punishmentLevel;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
            this.status = AppealStatus.PENDING;
            this.staffResponse = "";
            this.staffName = "";
        }


        public Appeal(int id, UUID playerId, String playerName, int punishmentLevel, String reason, long timestamp, AppealStatus status, String staffResponse, String staffName) {
            this.id = id;
            this.playerId = playerId;
            this.playerName = playerName;
            this.punishmentLevel = punishmentLevel;
            this.reason = reason;
            this.timestamp = timestamp;
            this.status = status;
            this.staffResponse = staffResponse;
            this.staffName = staffName;
        }


        public int getId() {
            return id;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getPunishmentLevel() {
            return punishmentLevel;
        }

        public String getReason() {
            return reason;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public AppealStatus getStatus() {
            return status;
        }

        public void setStatus(AppealStatus status) {
            this.status = status;
        }

        public String getStaffResponse() {
            return staffResponse;
        }

        public void setStaffResponse(String staffResponse) {
            this.staffResponse = staffResponse;
        }

        public String getStaffName() {
            return staffName;
        }

        public void setStaffName(String staffName) {
            this.staffName = staffName;
        }


        public String getFormattedTimestamp() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date(timestamp));
        }
    }
}