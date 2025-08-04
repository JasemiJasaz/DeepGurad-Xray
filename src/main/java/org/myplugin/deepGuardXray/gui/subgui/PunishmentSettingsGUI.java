package org.myplugin.deepGuardXray.gui.subgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.myplugin.deepGuardXray.config.ConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PunishmentSettingsGUI {
    public static final String PERMISSION = "deepguardx.gui_punishment";
    private final Inventory inv;
    private final ConfigManager configManager;

    public PunishmentSettingsGUI(ConfigManager configManager) {
        this.configManager = configManager;

        inv = Bukkit.createInventory(null, 54, Component.text("⚖ Punishment System").color(NamedTextColor.AQUA));
        initializeItems();
    }

    private void initializeItems() {


        for (int level = 1; level <= 6; level++) {
            int row = level - 1;
            int labelSlot = row * 9;
            int punishmentSlot = row * 9 + 1;
            int adminAlertSlot = row * 9 + 2;
            int warningMsgSlot = row * 9 + 3;
            int additionalSlot = row * 9 + 5;


            String levelTitle = "";
            String levelDescription = "";

            switch (level) {
                case 1:
                    levelTitle = "Level 1: Warning Phase";
                    levelDescription = "Initial warnings and minor effects";
                    break;
                case 2:
                    levelTitle = "Level 2: Minor Consequences";
                    levelDescription = "Minor penalties and annoyances";
                    break;
                case 3:
                    levelTitle = "Level 3: Moderate Punishment";
                    levelDescription = "Temporary kicks and mining restrictions";
                    break;
                case 4:
                    levelTitle = "Level 4: Severe Consequences";
                    levelDescription = "Extended bans and significant restrictions";
                    break;
                case 5:
                    levelTitle = "Level 5: Critical Response";
                    levelDescription = "Long-term ban and permanent effects";
                    break;
                case 6:
                    levelTitle = "Level 6: Maximum Enforcement";
                    levelDescription = "Permanent ban and security measures";
                    break;
            }


            inv.setItem(labelSlot, createGuiItem(Material.PAPER, levelTitle, Component.text(levelDescription).color(NamedTextColor.GRAY)));


            boolean punishmentEnabled = configManager.isPunishmentEnabled(level);
            Material toggleMaterial = punishmentEnabled ? Material.GREEN_WOOL : Material.RED_WOOL;
            String toggleStatus = punishmentEnabled ? "Enabled" : "Disabled";
            inv.setItem(punishmentSlot, createGuiItem(toggleMaterial, "Level " + level + " Punishment", Component.text("Status: " + toggleStatus).color(punishmentEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)));


            boolean adminAlertEnabled = configManager.isPunishmentOptionEnabled(level, "admin_alert");
            Material adminAlertMaterial = adminAlertEnabled ? Material.BELL : Material.GRAY_DYE;
            String adminAlertStatus = adminAlertEnabled ? "Enabled" : "Disabled";
            inv.setItem(adminAlertSlot, createGuiItem(adminAlertMaterial, "Admin Alerts", Component.text("Status: " + adminAlertStatus).color(adminAlertEnabled ? NamedTextColor.GREEN : NamedTextColor.RED), Component.text("Notify staff when this punishment is triggered").color(NamedTextColor.GRAY)));


            boolean warningMsgEnabled = configManager.isPunishmentOptionEnabled(level, "warning_message");
            Material warningMsgMaterial = warningMsgEnabled ? Material.BOOK : Material.GRAY_DYE;
            String warningMsgStatus = warningMsgEnabled ? "Enabled" : "Disabled";
            inv.setItem(warningMsgSlot, createGuiItem(warningMsgMaterial, "Warning Messages", Component.text("Status: " + warningMsgStatus).color(warningMsgEnabled ? NamedTextColor.GREEN : NamedTextColor.RED), Component.text("Show warning messages to players").color(NamedTextColor.GRAY)));


            inv.setItem(additionalSlot, createGuiItem(Material.WRITABLE_BOOK, "Advanced settings", Component.text("Click to configure Level " + level).color(NamedTextColor.GRAY)));
        }


        inv.setItem(52, createGuiItem(Material.OAK_SIGN, "Icon Guide", Component.text("🟢/🔴 - Enable/Disable level").color(NamedTextColor.GRAY), Component.text("🔔 - Admin alerts").color(NamedTextColor.GRAY), Component.text("📖 - Warning messages").color(NamedTextColor.GRAY), Component.text("📝 - Advanced settings").color(NamedTextColor.GRAY)));


        inv.setItem(53, createGuiItem(Material.BARRIER, "Back to ⛏ Staff Control Panel"));
    }

    private ItemStack createGuiItem(Material material, String name, Component... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.GOLD));

        if (lore.length > 0) {
            List<Component> loreList = new ArrayList<>();
            Collections.addAll(loreList, lore);
            meta.lore(loreList);
        }

        item.setItemMeta(meta);
        return item;
    }

    public void openInventory(Player player) {
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You do not have permission to manage punishment settings.").color(NamedTextColor.RED));
            return;
        }
        player.openInventory(inv);
    }
}