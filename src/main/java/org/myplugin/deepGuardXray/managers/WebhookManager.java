package org.myplugin.deepGuardXray.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class WebhookManager {

    private static WebhookManager instance;
    private static deepGuardXray plugin;
    private static ConfigManager configManager;

    /**
     * Private constructor for singleton pattern
     */
    private WebhookManager(deepGuardXray pluginInstance, ConfigManager configManagerInstance) {
        plugin = pluginInstance;
        configManager = configManagerInstance;
    }

    /**
     * Initialize the WebhookManager with the plugin instance and config manager
     */
    public static void initialize(deepGuardXray pluginInstance, ConfigManager configManagerInstance) {
        if (instance == null) {
            instance = new WebhookManager(pluginInstance, configManagerInstance);
        }
        plugin = pluginInstance;
        configManager = configManagerInstance;
    }

    /**
     * Gets the instance of WebhookManager
     *
     * @return The WebhookManager instance
     */
    public static WebhookManager getInstance() {
        return instance;
    }

    /**
     * Sends a test message to verify webhook is working
     */
    public static void sendTestMessage(String webhookUrl, String staffName) {
        String serverName = Bukkit.getServer().getName();
        if (serverName.isEmpty()) {
            serverName = "Minecraft Server";
        }

        String jsonPayload = String.format("{\"embeds\":[{" + "\"title\":\"DeepGuardXray Test Message\"," + "\"description\":\"This is a test message from DeepGuardXray plugin.\"," + "\"color\":5814783," + "\"fields\":[" + "{\"name\":\"Server\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Sent By\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Plugin Version\",\"value\":\"%s\",\"inline\":true}" + "]," + "\"footer\":{\"text\":\"DeepGuardXray Webhook • %s\"}" + "}]," + "\"username\":\"DeepGuardXray\"," + "\"avatar_url\":\"https://www.spigotmc.org/data/resource_icons/122/122967.jpg?1742479697\"" + "}", serverName, staffName, plugin.getDescription().getVersion(), getCurrentTimestamp());

        sendWebhookAsync(webhookUrl, jsonPayload);
    }

    private static boolean shouldSendAlert(String alertType) {
        return configManager != null && configManager.isWebhookAlertEnabled(alertType);
    }

    private static void sendWebhookAsync(String webhookUrl, String jsonPayload) {

        if (webhookUrl == null || webhookUrl.isEmpty() || (!webhookUrl.startsWith("https://discord.com/api/webhooks/") && !webhookUrl.startsWith("https://discordapp.com/api/webhooks/"))) {

            plugin.getLogger().warning("Invalid webhook URL format. Must start with https://discord.com/api/webhooks/");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                byte[] postData = jsonPayload.getBytes(StandardCharsets.UTF_8);
                connection.setRequestProperty("Content-Length", String.valueOf(postData.length));

                try (OutputStream stream = connection.getOutputStream()) {
                    stream.write(postData);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == 204) {

                    plugin.getLogger().info("Webhook message sent successfully");
                } else {
                    plugin.getLogger().warning("Failed to send webhook message. Response code: " + responseCode);


                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream()))) {
                        String line;
                        StringBuilder error = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            error.append(line);
                        }
                        plugin.getLogger().warning("Error details: " + error);
                    } catch (Exception e) {

                    }
                }

                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().severe("Error sending webhook: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private static String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Sends an ML analysis result alert to Discord
     * This is the most reliable cheater detection method
     *
     * @param player     The player who was analyzed
     * @param isCheater  Whether the ML model predicts this player is cheating
     * @param confidence The confidence score of the prediction (0-100)
     * @param features   Key features that influenced the decision
     */
    public void sendMLAnalysisAlert(Player player, boolean isCheater, double confidence, String features) {
        if (!shouldSendAlert("ml_analysis")) return;

        String webhookUrl = configManager.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;


        int color;
        String title;
        String prediction;

        if (isCheater) {
            color = 10038562;
            title = "🧪 ML DETECTION: X-Ray Cheater";
            prediction = "CHEATER";
        } else {
            color = 3066993;
            title = "🧪 ML DETECTION: Legitimate Player";
            prediction = "LEGITIMATE";
        }


        String confidenceStr = String.format("%.2f%%", confidence);


        String conclusion = "";
        if (features != null && !features.isEmpty()) {

            if (features.contains("Conclusion:")) {
                conclusion = features.substring(features.indexOf("Conclusion:") + "Conclusion:".length()).trim();
            } else {

                int endOfFirstSentence = features.indexOf(". ");
                if (endOfFirstSentence > 0) {
                    conclusion = features.substring(0, endOfFirstSentence + 1);
                } else {
                    conclusion = features;
                }


                if (conclusion.length() > 150) {
                    conclusion = conclusion.substring(0, 147) + "...";
                }
            }
        }


        String inGameNote = "Check in-game report for detailed analysis using `/dgx ml report " + player.getName() + "`";

        String jsonPayload = String.format("{\"embeds\":[{" + "\"title\":\"%s\"," + "\"description\":\"Machine Learning analysis has completed.\"," + "\"color\":%d," + "\"fields\":[" + "{\"name\":\"Player\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Prediction\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Confidence\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Conclusion\",\"value\":\"%s\",\"inline\":false}," + "{\"name\":\"Note\",\"value\":\"%s\",\"inline\":false}," + "{\"name\":\"Recommendation\",\"value\":\"**%s**\",\"inline\":false}" + "]," + "\"footer\":{\"text\":\"DeepGuardXray ML Detection • %s\"}" + "}]," + "\"username\":\"DeepGuardXray\"," + "\"avatar_url\":\"https://www.spigotmc.org/data/resource_icons/122/122967.jpg?1742479697\"" + "}", title, color, player.getName(), prediction, confidenceStr, escapeJson(conclusion), escapeJson(inGameNote), getRecommendation(isCheater, confidence), getCurrentTimestamp());

        sendWebhookAsync(webhookUrl, jsonPayload);
    }

    /**
     * Generate a recommendation based on the ML prediction and confidence
     */
    private String getRecommendation(boolean isCheater, double confidence) {
        if (isCheater) {
            if (confidence >= 90) {
                return "Take immediate action - extremely likely to be using X-ray";
            } else if (confidence >= 75) {
                return "Monitor closely and consider action - highly likely to be using X-ray";
            } else if (confidence >= 60) {
                return "Continue monitoring - moderately likely to be using X-ray";
            } else {
                return "Weak indication - gather more data before action";
            }
        } else {
            if (confidence >= 90) {
                return "No action needed - extremely likely to be legitimate";
            } else if (confidence >= 75) {
                return "No action needed - likely legitimate";
            } else {
                return "Continue monitoring - prediction uncertain";
            }
        }
    }

    /**
     * Sends an appeal status update alert to Discord
     */
    public void sendAppealStatusAlert(int appealId, String playerName, int punishmentLevel, AppealManager.AppealStatus status, String staffName, String response) {
        if (!shouldSendAlert("appeal_updates")) return;

        String webhookUrl = configManager.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;


        int color;
        String emoji;
        switch (status) {
            case APPROVED:
                color = 5763719;
                emoji = "✅";
                break;
            case DENIED:
                color = 15548997;
                emoji = "❌";
                break;
            case UNDER_REVIEW:
                color = 16776960;
                emoji = "⚠️";
                break;
            default:
                color = 5814783;
                emoji = "📋";
        }


        String displayResponse = response;
        if (displayResponse != null && displayResponse.length() > 300) {
            displayResponse = displayResponse.substring(0, 297) + "...";
        }

        String responseField = (displayResponse != null && !displayResponse.isEmpty()) ? ",{\"name\":\"Staff Response\",\"value\":\"" + escapeJson(displayResponse) + "\",\"inline\":false}" : "";

        String jsonPayload = String.format("{\"embeds\":[{" + "\"title\":\"%s Appeal Status Update\"," + "\"description\":\"An appeal has been %s.\"," + "\"color\":%d," + "\"fields\":[" + "{\"name\":\"Appeal ID\",\"value\":\"#%d\",\"inline\":true}," + "{\"name\":\"Player\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Punishment Level\",\"value\":\"%d\",\"inline\":true}," + "{\"name\":\"Status\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Staff Member\",\"value\":\"%s\",\"inline\":true}" + "%s" + "]," + "\"footer\":{\"text\":\"DeepGuardXray • %s\"}" + "}]," + "\"username\":\"DeepGuardXray\"," + "\"avatar_url\":\"https://www.spigotmc.org/data/resource_icons/122/122967.jpg?1742479697\"" + "}", emoji, status.getDisplayName().toLowerCase(), color, appealId, playerName, punishmentLevel, status.getDisplayName(), staffName, responseField, getCurrentTimestamp());

        sendWebhookAsync(webhookUrl, jsonPayload);
    }

    /**
     * Sends a notification about a new appeal being submitted
     */
    public void sendNewAppealAlert(int appealId, String playerName, int punishmentLevel, String reason) {
        if (!shouldSendAlert("appeal_updates")) return;

        String webhookUrl = configManager.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;


        String displayReason = reason;
        if (displayReason != null && displayReason.length() > 300) {
            displayReason = displayReason.substring(0, 297) + "...";
        }

        String jsonPayload = String.format("{\"embeds\":[{" + "\"title\":\"📋 New Appeal Submitted\"," + "\"description\":\"A player has submitted a new appeal.\"," + "\"color\":5814783," + "\"fields\":[" + "{\"name\":\"Appeal ID\",\"value\":\"#%d\",\"inline\":true}," + "{\"name\":\"Player\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Punishment Level\",\"value\":\"%d\",\"inline\":true}," + "{\"name\":\"Reason\",\"value\":\"%s\",\"inline\":false}" + "]," + "\"footer\":{\"text\":\"DeepGuardXray • %s\"}" + "}]," + "\"username\":\"DeepGuardXray\"," + "\"avatar_url\":\"https://www.spigotmc.org/data/resource_icons/122/122967.jpg?1742479697\"" + "}", appealId, playerName, punishmentLevel, escapeJson(displayReason), getCurrentTimestamp());

        sendWebhookAsync(webhookUrl, jsonPayload);
    }

    /**
     * Helper method to escape special characters in JSON strings
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /**
     * Sends a punishment applied alert to Discord with admin information
     */
    public void sendPunishmentAlertWithAdmin(Player player, int level, String punishmentType, String adminName) {
        if (!shouldSendAlert("punishment_applied")) return;

        String webhookUrl = configManager.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String jsonPayload = String.format("{\"embeds\":[{" + "\"title\":\"🛑 Punishment Applied\"," + "\"description\":\"Punishment has been applied to a player.\"," + "\"color\":10038562," + "\"fields\":[" + "{\"name\":\"Player\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Level\",\"value\":\"%d\",\"inline\":true}," + "{\"name\":\"Type\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Applied By\",\"value\":\"%s\",\"inline\":true}" + "]," + "\"footer\":{\"text\":\"DeepGuardXray • %s\"}" + "}]," + "\"username\":\"DeepGuardXray\"," + "\"avatar_url\":\"https://www.spigotmc.org/data/resource_icons/122/122967.jpg?1742479697\"" + "}", player.getName(), level, punishmentType, adminName, getCurrentTimestamp());

        sendWebhookAsync(webhookUrl, jsonPayload);
    }

    /**
     * Sends an X-Ray detection alert to Discord
     */
    public void sendXrayAlert(Player player, String oreType, Location location, double confidence) {
        if (!shouldSendAlert("xray_detection")) return;

        String webhookUrl = configManager.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String coords = String.format("X: %d, Y: %d, Z: %d", location.getBlockX(), location.getBlockY(), location.getBlockZ());

        String world = location.getWorld().getName();

        String jsonPayload = String.format("{\"embeds\":[{" + "\"title\":\"⛏ X-Ray Detection Alert (Decoy Ore)\"," + "\"description\":\"Player mined hidden decoy ore! Basic detection method.\"," + "\"color\":16711680," + "\"fields\":[" + "{\"name\":\"Player\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Ore Type\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Confidence\",\"value\":\"%.1f%%\",\"inline\":true}," + "{\"name\":\"Location\",\"value\":\"%s, %s\",\"inline\":false}," + "{\"name\":\"Note\",\"value\":\"This is a basic detection method and should be confirmed with ML Analysis\",\"inline\":false}" + "]," + "\"footer\":{\"text\":\"DeepGuardXray • %s\"}" + "}]," + "\"username\":\"DeepGuardXray\"," + "\"avatar_url\":\"https://www.spigotmc.org/data/resource_icons/122/122967.jpg?1742479697\"" + "}", player.getName(), oreType, confidence, coords, world, getCurrentTimestamp());

        sendWebhookAsync(webhookUrl, jsonPayload);
    }

    /**
     * Sends a suspicious mining pattern alert to Discord
     */
    public void sendSuspiciousMiningAlert(Player player, String pattern, double score) {
        if (!shouldSendAlert("suspicious_mining")) return;

        String webhookUrl = configManager.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String jsonPayload = String.format("{\"embeds\":[{" + "\"title\":\"⚠️ Suspicious Mining Pattern\"," + "\"description\":\"Player mined unusually high amounts of ore in a short time.\"," + "\"color\":16750848," + "\"fields\":[" + "{\"name\":\"Player\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Pattern\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Suspicion Score\",\"value\":\"%.1f/100\",\"inline\":true}," + "{\"name\":\"Note\",\"value\":\"May flag legitimate players. Use ML Analysis for confirmation.\",\"inline\":false}" + "]," + "\"footer\":{\"text\":\"DeepGuardXray • %s\"}" + "}]," + "\"username\":\"DeepGuardXray\"," + "\"avatar_url\":\"https://www.spigotmc.org/data/resource_icons/122/122967.jpg?1742479697\"" + "}", player.getName(), pattern, score, getCurrentTimestamp());

        sendWebhookAsync(webhookUrl, jsonPayload);
    }

    /**
     * Sends a punishment applied alert to Discord
     */
    public void sendPunishmentAlert(Player player, int level, String punishmentType) {
        if (!shouldSendAlert("punishment_applied")) return;

        String webhookUrl = configManager.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String jsonPayload = String.format("{\"embeds\":[{" + "\"title\":\"🛑 Punishment Applied\"," + "\"description\":\"Punishment has been applied to a player.\"," + "\"color\":10038562," + "\"fields\":[" + "{\"name\":\"Player\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Level\",\"value\":\"%d\",\"inline\":true}," + "{\"name\":\"Type\",\"value\":\"%s\",\"inline\":true}" + "]," + "\"footer\":{\"text\":\"DeepGuardXray • %s\"}" + "}]," + "\"username\":\"DeepGuardXray\"," + "\"avatar_url\":\"https://www.spigotmc.org/data/resource_icons/122/122967.jpg?1742479697\"" + "}", player.getName(), level, punishmentType, getCurrentTimestamp());

        sendWebhookAsync(webhookUrl, jsonPayload);
    }

    public void sendStaffActionLog(String staffName, String action, String target) {
        if (!shouldSendAlert("staff_actions")) return;

        String webhookUrl = configManager.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String jsonPayload = String.format("{\"embeds\":[{" + "\"title\":\"👮 Staff Action Log\"," + "\"description\":\"A staff member performed an action.\"," + "\"color\":3447003," + "\"fields\":[" + "{\"name\":\"Staff\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Action\",\"value\":\"%s\",\"inline\":true}," + "{\"name\":\"Target\",\"value\":\"%s\",\"inline\":true}" + "]," + "\"footer\":{\"text\":\"DeepGuardXray • %s\"}" + "}]," + "\"username\":\"DeepGuardXray\"," + "\"avatar_url\":\"https://www.spigotmc.org/data/resource_icons/122/122967.jpg?1742479697\"" + "}", staffName, action, target, getCurrentTimestamp());

        sendWebhookAsync(webhookUrl, jsonPayload);
    }
}