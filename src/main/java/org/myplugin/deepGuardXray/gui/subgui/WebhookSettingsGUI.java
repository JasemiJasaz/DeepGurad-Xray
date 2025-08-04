package org.myplugin.deepGuardXray.gui.subgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.managers.WebhookManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WebhookSettingsGUI {
    public static final String PERMISSION = "deepguardx.gui_webhook";
    private final Inventory inv;
    private final ConfigManager configManager;
    private final deepGuardXray plugin;

    public WebhookSettingsGUI(ConfigManager configManager, deepGuardXray plugin) {
        this.configManager = configManager;
        this.plugin = plugin;

        inv = Bukkit.createInventory(null, 36, Component.text("🔔 Discord Webhook Settings").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));

        initializeItems();
    }

    public static void handleClick(Player player, int slot, Inventory inventory, ConfigManager configManager, deepGuardXray plugin) {

        if (slot == 10) {
            player.closeInventory();
            player.sendMessage(Component.text("Please enter your Discord webhook URL in chat:").color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("(Your webhook URL will not be visible to other players)").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));


            plugin.getChatInputHandler().registerChatInputHandler(player.getUniqueId(), (input) -> {
                if (input.startsWith("https://discord.com/api/webhooks/") || input.startsWith("https://discordapp.com/api/webhooks/")) {

                    configManager.setWebhookUrl(input);
                    plugin.getConfig().set("webhook.url", input);
                    plugin.saveConfig();

                    player.sendMessage(Component.text("Webhook URL set successfully!").color(NamedTextColor.GREEN));


                    Bukkit.getScheduler().runTask(plugin, () -> {
                        new WebhookSettingsGUI(configManager, plugin).openInventory(player);
                    });
                } else {
                    player.sendMessage(Component.text("Invalid webhook URL. It must start with https://discord.com/api/webhooks/").color(NamedTextColor.RED));


                    Bukkit.getScheduler().runTask(plugin, () -> {
                        new WebhookSettingsGUI(configManager, plugin).openInventory(player);
                    });
                }

                return true;
            });

            return;
        }


        if (slot == 11) {
            toggleWebhookAlert(player, "xray_detection", configManager, plugin);
        } else if (slot == 12) {
            toggleWebhookAlert(player, "suspicious_mining", configManager, plugin);
        } else if (slot == 13) {
            toggleWebhookAlert(player, "ml_analysis", configManager, plugin);
        } else if (slot == 14) {
            toggleWebhookAlert(player, "punishment_applied", configManager, plugin);
        } else if (slot == 15) {
            toggleWebhookAlert(player, "staff_actions", configManager, plugin);
        } else if (slot == 16) {
            toggleWebhookAlert(player, "appeal_updates", configManager, plugin);
        } else if (slot == 21) {
            String webhookUrl = configManager.getWebhookUrl();


            if (webhookUrl != null && !webhookUrl.isEmpty() && (webhookUrl.startsWith("https://discord.com/api/webhooks/") || webhookUrl.startsWith("https://discordapp.com/api/webhooks/"))) {

                player.sendMessage(Component.text("Sending test message to Discord...").color(NamedTextColor.YELLOW));

                WebhookManager.sendTestMessage(webhookUrl, player.getName());

                player.sendMessage(Component.text("Test message sent! Check your Discord channel.").color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Please set up a valid webhook URL first!").color(NamedTextColor.RED));


                if (webhookUrl != null && !webhookUrl.isEmpty()) {
                    player.sendMessage(Component.text("Debug: Current URL doesn't start with expected prefix").color(NamedTextColor.GRAY));


                    String safePrefix = webhookUrl.length() > 10 ? webhookUrl.substring(0, 10) + "..." : webhookUrl;
                    player.sendMessage(Component.text("Debug: URL starts with: " + safePrefix).color(NamedTextColor.GRAY));
                }
            }
        } else if (slot == 31) {
            org.myplugin.deepGuardXray.gui.StaffMenuGUI staffMenu = new org.myplugin.deepGuardXray.gui.StaffMenuGUI();
            staffMenu.openInventory(player);
        }
    }

    private static void toggleWebhookAlert(Player player, String alertType, ConfigManager configManager, deepGuardXray plugin) {
        boolean currentState = configManager.isWebhookAlertEnabled(alertType);
        configManager.setWebhookAlertEnabled(alertType, !currentState);


        plugin.getConfig().set("webhook.alerts." + alertType, !currentState);
        plugin.saveConfig();


        player.sendMessage(Component.text(formatAlertTypeName(alertType) + " alerts are now ").color(NamedTextColor.GOLD).append(Component.text(!currentState ? "enabled" : "disabled").color(!currentState ? NamedTextColor.GREEN : NamedTextColor.RED)));


        new WebhookSettingsGUI(configManager, plugin).openInventory(player);
    }

    /**
     * Format alert type name for display in messages
     */
    private static String formatAlertTypeName(String alertType) {

        if (alertType.equals("ml_analysis")) {
            return "ML Analysis";
        }


        String[] words = alertType.split("_");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }

        return formatted.toString().trim();
    }

    private void initializeItems() {

        ItemStack border = createGuiItem(Material.PURPLE_STAINED_GLASS_PANE, Component.text("◼").color(NamedTextColor.DARK_GRAY), false);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
        }
        for (int i = 27; i < 36; i++) {
            inv.setItem(i, border);
        }
        inv.setItem(9, border);
        inv.setItem(18, border);
        inv.setItem(17, border);
        inv.setItem(26, border);


        String webhookUrl = configManager.getWebhookUrl();
        boolean webhookConfigured = webhookUrl != null && !webhookUrl.isEmpty() && (webhookUrl.startsWith("https://discord.com/api/webhooks/") || webhookUrl.startsWith("https://discordapp.com/api/webhooks/"));


        inv.setItem(10, createGuiItem(webhookConfigured ? Material.LIME_WOOL : Material.RED_WOOL, Component.text("Discord Webhook URL").color(webhookConfigured ? NamedTextColor.GREEN : NamedTextColor.RED), false, Component.text(webhookConfigured ? "✓ Configured" : "✗ Not Configured").color(webhookConfigured ? NamedTextColor.GREEN : NamedTextColor.RED), Component.text("Click to " + (webhookConfigured ? "edit" : "set") + " the webhook URL").color(NamedTextColor.GRAY)));


        inv.setItem(11, createAlertToggle("xray_detection", "X-Ray Detection Alerts", Material.DIAMOND_ORE, Component.text("Alerts for decoy ore mining").color(NamedTextColor.GRAY), Component.text("Basic detection method").color(NamedTextColor.YELLOW), Component.text("Not 100% reliable on its own").color(NamedTextColor.RED)));

        inv.setItem(12, createAlertToggle("suspicious_mining", "Suspicious Mining Alerts", Material.NETHERITE_PICKAXE, Component.text("Alerts for rapid ore mining patterns").color(NamedTextColor.GRAY), Component.text("May flag legitimate players").color(NamedTextColor.YELLOW), Component.text("Use with other detection methods").color(NamedTextColor.GOLD)));

        inv.setItem(13, createAlertToggle("ml_analysis", "ML Analysis Results", Material.BEACON, Component.text("Machine learning-based detection").color(NamedTextColor.AQUA), Component.text("Most reliable cheater detection").color(NamedTextColor.GREEN), Component.text("Recommended for enforcement").color(NamedTextColor.GREEN), Component.text("⭐ Primary detection method").color(NamedTextColor.GOLD)));

        inv.setItem(14, createAlertToggle("punishment_applied", "Punishment Alerts", Material.BARRIER));
        inv.setItem(15, createAlertToggle("staff_actions", "Staff Action Logs", Material.SHIELD));
        inv.setItem(16, createAlertToggle("appeal_updates", "Appeal System Alerts", Material.PAPER));


        inv.setItem(21, createGuiItem(Material.REDSTONE, Component.text("Test Webhook").color(NamedTextColor.GOLD), true, Component.text("Sends a test message to Discord").color(NamedTextColor.GRAY), Component.text("to verify your webhook is working").color(NamedTextColor.GRAY)));


        inv.setItem(31, createGuiItem(Material.ARROW, Component.text("Back to ⛏ Staff Control Panel").color(NamedTextColor.AQUA), false));
    }

    private ItemStack createAlertToggle(String alertType, String displayName, Material material, Component... additionalLore) {
        boolean enabled = configManager.isWebhookAlertEnabled(alertType);

        List<Component> loreList = new ArrayList<>();
        loreList.add(Component.text(enabled ? "✓ Enabled" : "✗ Disabled").color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED));


        if (additionalLore.length > 0) {
            loreList.addAll(Arrays.asList(additionalLore));
        }


        loreList.add(Component.text("Click to toggle").color(NamedTextColor.GRAY));

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName).color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        meta.lore(loreList);

        if (enabled) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material material, Component name, boolean enchanted, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);

        if (lore.length > 0) {
            List<Component> loreList = new ArrayList<>();
            Collections.addAll(loreList, lore);
            meta.lore(loreList);
        }

        if (enchanted) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    public void openInventory(Player player) {
        player.openInventory(inv);
    }
}