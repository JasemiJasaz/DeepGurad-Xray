package org.myplugin.deepGuardXray.gui.subgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.managers.StatsManager;
import org.myplugin.deepGuardXray.managers.SuspiciousManager;
import org.myplugin.deepGuardXray.utils.DirectConfigSaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoSaveSettingsGUI {
    private final static String TITLE = "⚙ Auto-Save Settings";
    private final Inventory inv;
    private final ConfigManager configManager;
    private final deepGuardXray plugin;

    public AutoSaveSettingsGUI(ConfigManager configManager, deepGuardXray plugin) {
        this.configManager = configManager;
        this.plugin = plugin;


        inv = Bukkit.createInventory(null, 45, Component.text(TITLE).color(NamedTextColor.AQUA));

        initializeItems();
    }

    public static void handleClick(Player player, int slot, Inventory inventory, ClickType clickType, ConfigManager configManager, deepGuardXray plugin) {

        if (!player.hasPermission(ConfigSettingsGUI.PERMISSION)) {
            player.sendMessage(Component.text("You don't have permission to modify the config settings.").color(NamedTextColor.RED));
            player.closeInventory();
            return;
        }


        if (slot == 40) {
            new ConfigSettingsGUI(configManager, plugin).openInventory(player);
            return;
        }


        boolean isBorder = slot < 9 || slot >= 36 || slot % 9 == 0 || slot % 9 == 8 || slot % 9 == 3 || slot % 9 == 6;


        boolean isHeader = slot == 4 || slot == 11 || slot == 14 || slot == 17;

        if (isBorder || isHeader) {
            return;
        }


        ItemStack clicked = inventory.getItem(slot);
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasLore()) {
            return;
        }


        List<Component> lore = clicked.getItemMeta().lore();
        if (lore == null || lore.isEmpty()) {
            return;
        }


        Component configPathComponent = lore.get(lore.size() - 1);

        String configPathLine = PlainTextComponentSerializer.plainText().serialize(configPathComponent);


        if (!configPathLine.contains("Config path:")) {
            return;
        }


        String configPath = configPathLine.substring(configPathLine.lastIndexOf("Config path:") + 12).trim();
        plugin.getLogger().info("Processing click for config path: " + configPath);


        if (clicked.getType() == Material.LIME_CONCRETE || clicked.getType() == Material.RED_CONCRETE) {
            boolean currentValue = clicked.getType() == Material.LIME_CONCRETE;
            boolean newValue = !currentValue;


            boolean success = DirectConfigSaver.saveBoolean(plugin, configPath, newValue);

            if (success) {

                plugin.reloadConfig();
                configManager.reloadConfig();


                if (configPath.startsWith("stats.autoSave.")) {
                    StatsManager.updateAutoSaveSettings();
                } else if (configPath.startsWith("suspicious.autoSave.")) {
                    SuspiciousManager.updateAutoSaveSettings();
                } else if (configPath.startsWith("punishment.autoSave.")) {
                    plugin.getPunishmentManager().updateAutoSaveSettings();
                }


                player.sendMessage(Component.text("Setting ").color(NamedTextColor.GOLD).append(Component.text(configPath).color(NamedTextColor.YELLOW)).append(Component.text(" changed to ").color(NamedTextColor.GOLD)).append(Component.text(newValue ? "ENABLED" : "DISABLED").color(newValue ? NamedTextColor.GREEN : NamedTextColor.RED)));


                if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                    plugin.getWebhookManager().sendStaffActionLog(player.getName(), "Changed Configuration", configPath + " → " + (newValue ? "ENABLED" : "DISABLED"));
                }


                new AutoSaveSettingsGUI(configManager, plugin).openInventory(player);
            } else {
                player.sendMessage(Component.text("Failed to save config change. Check console for details.").color(NamedTextColor.RED));
            }

            return;
        }


        if (clicked.getType() == Material.PAPER) {

            Component valueLine = lore.get(1);
            String valueStr = PlainTextComponentSerializer.plainText().serialize(valueLine);
            String extractedValue = valueStr.substring(valueStr.lastIndexOf(": ") + 2).trim();


            boolean isInteger = !extractedValue.contains(".");


            boolean isLeftClick = (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT);
            boolean isRightClick = (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT);

            if (!isLeftClick && !isRightClick) {
                player.sendMessage(Component.text("Please use left-click to increase or right-click to decrease the value.").color(NamedTextColor.RED));
                return;
            }

            if (isInteger) {
                try {
                    int currentValue = Integer.parseInt(extractedValue);
                    int newValue;

                    if (isLeftClick) {

                        newValue = currentValue + 5;
                    } else {

                        newValue = Math.max(1, currentValue - 5);
                    }


                    boolean success = DirectConfigSaver.saveInteger(plugin, configPath, newValue);

                    if (success) {

                        plugin.reloadConfig();
                        configManager.reloadConfig();


                        if (configPath.startsWith("stats.autoSave.")) {
                            StatsManager.updateAutoSaveSettings();
                        } else if (configPath.startsWith("suspicious.autoSave.")) {
                            SuspiciousManager.updateAutoSaveSettings();
                        } else if (configPath.startsWith("punishment.autoSave.")) {
                            plugin.getPunishmentManager().updateAutoSaveSettings();
                        }


                        new AutoSaveSettingsGUI(configManager, plugin).openInventory(player);


                        player.sendMessage(Component.text("Setting ").color(NamedTextColor.GOLD).append(Component.text(configPath).color(NamedTextColor.YELLOW)).append(Component.text(" updated to ").color(NamedTextColor.GOLD)).append(Component.text(String.valueOf(newValue)).color(NamedTextColor.AQUA)));


                        if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                            plugin.getWebhookManager().sendStaffActionLog(player.getName(), "Changed Configuration", configPath + " → " + newValue);
                        }
                    } else {
                        player.sendMessage(Component.text("Failed to save config change. Check console for details.").color(NamedTextColor.RED));
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().severe("Error parsing integer value: " + extractedValue);
                    player.sendMessage(Component.text("Error parsing number. Please report this to a developer.").color(NamedTextColor.RED));
                }
            }
        }
    }

    private void initializeItems() {


        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", false);
        ItemStack separator = createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", false);


        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(36 + i, border);
        }


        for (int row = 1; row < 4; row++) {
            inv.setItem(row * 9, border);
            inv.setItem(row * 9 + 8, border);
        }


        for (int row = 1; row < 4; row++) {
            inv.setItem(row * 9 + 3, separator);
        }


        for (int row = 1; row < 4; row++) {
            inv.setItem(row * 9 + 6, separator);
        }


        inv.setItem(4, createGuiItem(Material.CLOCK, "Auto-Save Settings", true, TextDecoration.BOLD, NamedTextColor.YELLOW, Component.text("Configure automatic data saving").color(NamedTextColor.GRAY)));


        inv.setItem(11, createGuiItem(Material.EXPERIENCE_BOTTLE, "Stats Manager", true, TextDecoration.BOLD, NamedTextColor.GREEN, Component.text("Mining statistics auto-save settings").color(NamedTextColor.GRAY)));


        inv.setItem(14, createGuiItem(Material.ENDER_EYE, "Suspicious Manager", true, TextDecoration.BOLD, NamedTextColor.LIGHT_PURPLE, Component.text("Suspicious player data auto-save settings").color(NamedTextColor.GRAY)));


        inv.setItem(17, createGuiItem(Material.IRON_AXE, "Punishment Manager", true, TextDecoration.BOLD, NamedTextColor.RED, Component.text("Punishment data auto-save settings").color(NamedTextColor.GRAY)));


        inv.setItem(40, createGuiItem(Material.BARRIER, "Back to Plugin Configuration", false, null, NamedTextColor.RED));


        addAutoSaveSettings();
    }

    private void addAutoSaveSettings() {

        inv.setItem(10, createToggleItem("Stats Auto-Save Enabled", configManager.isStatsAutoSaveEnabled(), "Enable/disable automatic saving of mining statistics", "stats.autoSave.enabled"));

        inv.setItem(19, createNumberItem("Stats Save Interval", configManager.getStatsAutoSaveInterval(), "Minutes between auto-saves for mining stats", "stats.autoSave.intervalMinutes"));

        inv.setItem(28, createToggleItem("Stats Save Logging", configManager.isStatsAutoSaveLoggingEnabled(), "Enable/disable logging of auto-save operations for stats", "stats.autoSave.logging"));


        inv.setItem(13, createToggleItem("Suspicious Auto-Save", configManager.isSuspiciousAutoSaveEnabled(), "Enable/disable automatic saving of suspicious player data", "suspicious.autoSave.enabled"));

        inv.setItem(22, createNumberItem("Suspicious Save Interval", configManager.getSuspiciousAutoSaveInterval(), "Minutes between auto-saves for suspicious data", "suspicious.autoSave.intervalMinutes"));

        inv.setItem(31, createToggleItem("Suspicious Save Logging", configManager.isSuspiciousAutoSaveLoggingEnabled(), "Enable/disable logging of auto-save operations for suspicious data", "suspicious.autoSave.logging"));


        inv.setItem(16, createToggleItem("Punishment Auto-Save", configManager.isPunishmentAutoSaveEnabled(), "Enable/disable automatic saving of punishment data", "punishment.autoSave.enabled"));

        inv.setItem(25, createNumberItem("Punishment Save Interval", configManager.getPunishmentAutoSaveInterval(), "Minutes between auto-saves for punishment data", "punishment.autoSave.intervalMinutes"));

        inv.setItem(34, createToggleItem("Punishment Save Logging", configManager.isPunishmentAutoSaveLoggingEnabled(), "Enable/disable logging of auto-save operations for punishment data", "punishment.autoSave.logging"));
    }

    private ItemStack createToggleItem(String name, boolean value, String description, String configPath) {
        Material material = value ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        Component status = value ? Component.text("ENABLED").color(NamedTextColor.GREEN) : Component.text("DISABLED").color(NamedTextColor.RED);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(description).color(NamedTextColor.GRAY));
        lore.add(status);
        lore.add(Component.text("Click to toggle").color(NamedTextColor.YELLOW));
        lore.add(Component.text("Config path: " + configPath).color(NamedTextColor.DARK_GRAY));

        return createGuiItemWithLore(material, Component.text(name).color(NamedTextColor.GOLD), value, lore);
    }

    private ItemStack createNumberItem(String name, Number value, String description, String configPath) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(description).color(NamedTextColor.GRAY));
        lore.add(Component.text("Current value: ").color(NamedTextColor.AQUA).append(Component.text(value.toString()).color(NamedTextColor.WHITE)));
        lore.add(Component.text("Left-click to increase").color(NamedTextColor.YELLOW));
        lore.add(Component.text("Right-click to decrease").color(NamedTextColor.YELLOW));
        lore.add(Component.text("Config path: " + configPath).color(NamedTextColor.DARK_GRAY));

        return createGuiItemWithLore(Material.PAPER, Component.text(name).color(NamedTextColor.GOLD), false, lore);
    }

    private ItemStack createGuiItem(Material material, String name, boolean enchanted, TextDecoration decoration, NamedTextColor color, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        Component displayName = Component.text(name);
        if (color != null) {
            displayName = displayName.color(color);
        }
        if (decoration != null) {
            displayName = displayName.decorate(decoration);
        }
        meta.displayName(displayName);

        if (lore.length > 0) {
            List<Component> itemLore = new ArrayList<>();
            Collections.addAll(itemLore, lore);
            meta.lore(itemLore);
        }

        if (enchanted) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material material, String name, boolean enchanted) {
        return createGuiItem(material, name, enchanted, null, null);
    }

    private ItemStack createGuiItemWithLore(Material material, Component displayName, boolean enchanted, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(displayName);

        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore);
        }

        if (enchanted) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    public void openInventory(Player player) {

        if (!player.hasPermission(ConfigSettingsGUI.PERMISSION)) {
            player.sendMessage(Component.text("You don't have permission to access the config settings.").color(NamedTextColor.RED));
            return;
        }

        player.openInventory(inv);
    }
}