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
import org.myplugin.deepGuardXray.utils.DirectConfigSaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DecoySettingsGUI {
    private final static String TITLE = "⚙ Decoy Settings";
    private final Inventory inv;
    private final ConfigManager configManager;
    private final deepGuardXray plugin;

    public DecoySettingsGUI(ConfigManager configManager, deepGuardXray plugin) {
        this.configManager = configManager;
        this.plugin = plugin;


        inv = Bukkit.createInventory(null, 36, Component.text(TITLE).color(NamedTextColor.AQUA));

        initializeItems();
    }

    public static void handleClick(Player player, int slot, Inventory inventory, ClickType clickType, ConfigManager configManager, deepGuardXray plugin) {

        if (!player.hasPermission(ConfigSettingsGUI.PERMISSION)) {
            player.sendMessage(Component.text("You don't have permission to modify the config settings.").color(NamedTextColor.RED));
            player.closeInventory();
            return;
        }


        if (slot == 31) {
            new ConfigSettingsGUI(configManager, plugin).openInventory(player);
            return;
        }


        boolean isBorder = slot < 9 || slot >= 27 || slot % 9 == 0 || slot % 9 == 8;
        boolean isHeader = slot == 4;

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


                player.sendMessage(Component.text("Setting ").color(NamedTextColor.GOLD).append(Component.text(configPath).color(NamedTextColor.YELLOW)).append(Component.text(" changed to ").color(NamedTextColor.GOLD)).append(Component.text(newValue ? "ENABLED" : "DISABLED").color(newValue ? NamedTextColor.GREEN : NamedTextColor.RED)));


                if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                    plugin.getWebhookManager().sendStaffActionLog(player.getName(), "Changed Configuration", configPath + " → " + (newValue ? "ENABLED" : "DISABLED"));
                }


                new DecoySettingsGUI(configManager, plugin).openInventory(player);
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

                        if (configPath.equals("decoy.timeWindowTicks") || configPath.equals("decoy.revertDelay")) {

                            newValue = currentValue + 1200;
                        } else {
                            newValue = currentValue + 1;
                        }
                    } else {

                        if (configPath.equals("decoy.timeWindowTicks") || configPath.equals("decoy.revertDelay")) {

                            newValue = Math.max(1200, currentValue - 1200);
                        } else {
                            newValue = Math.max(1, currentValue - 1);
                        }
                    }


                    boolean success = DirectConfigSaver.saveInteger(plugin, configPath, newValue);

                    if (success) {

                        plugin.reloadConfig();
                        configManager.reloadConfig();


                        new DecoySettingsGUI(configManager, plugin).openInventory(player);


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
            } else {
                try {
                    double currentValue = Double.parseDouble(extractedValue);
                    double newValue;

                    if (isLeftClick) {
                        newValue = currentValue + 0.1;
                    } else {
                        newValue = Math.max(0.1, currentValue - 0.1);
                    }


                    newValue = Math.round(newValue * 10) / 10.0;


                    boolean success = DirectConfigSaver.saveDouble(plugin, configPath, newValue);

                    if (success) {

                        plugin.reloadConfig();
                        configManager.reloadConfig();


                        new DecoySettingsGUI(configManager, plugin).openInventory(player);


                        player.sendMessage(Component.text("Setting ").color(NamedTextColor.GOLD).append(Component.text(configPath).color(NamedTextColor.YELLOW)).append(Component.text(" updated to ").color(NamedTextColor.GOLD)).append(Component.text(String.valueOf(newValue)).color(NamedTextColor.AQUA)));


                        if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                            plugin.getWebhookManager().sendStaffActionLog(player.getName(), "Changed Configuration", configPath + " → " + newValue);
                        }
                    } else {
                        player.sendMessage(Component.text("Failed to save config change. Check console for details.").color(NamedTextColor.RED));
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().severe("Error parsing double value: " + extractedValue);
                    player.sendMessage(Component.text("Error parsing number. Please report this to a developer.").color(NamedTextColor.RED));
                }
            }
        }
    }

    private void initializeItems() {

        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", false);


        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(27 + i, border);
        }


        inv.setItem(4, createGuiItem(Material.DIAMOND_PICKAXE, "Decoy Settings", true, TextDecoration.BOLD, NamedTextColor.AQUA, Component.text("Configure decoy ore generation").color(NamedTextColor.GRAY)));


        inv.setItem(31, createGuiItem(Material.BARRIER, "Back to Plugin Configuration", false, null, NamedTextColor.RED));


        addDecoySettings();
    }

    private void addDecoySettings() {


        inv.setItem(10, createToggleItem("Decoy Enabled", configManager.isDecoyEnabled(), "Enables or disables the decoy system", "decoy.enabled"));

        inv.setItem(11, createNumberItem("Ore Threshold", configManager.getOreThreshold(), "Number of ores before decoy triggers", "decoy.oreThreshold"));

        inv.setItem(12, createNumberItem("Time Window (ticks)", (int) configManager.getTimeWindowTicks(), "Time window for counting ores (1200 = 1 minute)", "decoy.timeWindowTicks"));


        inv.setItem(19, createNumberItem("Decoy Distance", (int) configManager.getDecoyDistance(), "Distance from mined block to place decoy", "decoy.distance"));

        inv.setItem(20, createNumberItem("Field Offset", configManager.getDecoyFieldOffset(), "Offset used in decoy placement", "decoy.fieldOffset"));

        inv.setItem(21, createNumberItem("Max Decay Distance", configManager.getMaxDecayDistance(), "Max distance before decoy reverts", "decoy.maxDistance"));


        inv.setItem(16, createNumberItem("Revert Delay (ticks)", configManager.getDecoyRevertDelay(), "Delay before decoy reverts (1200 = 1 minute)", "decoy.revertDelay"));

        inv.setItem(25, createToggleItem("Warn On Decoy", configManager.isWarnOnDecoy(), "Warn players when they break a decoy", "decoy.warnOnDecoy"));
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