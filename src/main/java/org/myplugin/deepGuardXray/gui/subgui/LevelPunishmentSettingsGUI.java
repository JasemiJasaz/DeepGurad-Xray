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

public class LevelPunishmentSettingsGUI {
    private final Inventory inv;
    private final ConfigManager configManager;
    private final int level;

    public LevelPunishmentSettingsGUI(ConfigManager configManager, int level) {
        this.configManager = configManager;
        this.level = level;

        inv = Bukkit.createInventory(null, 54, Component.text("Settings for Level " + level + " Punishment").color(NamedTextColor.AQUA));
        initializeItems();
    }

    /**
     * Handle clicks in this GUI
     */
    public static void handleClick(Player player, int slot, int level, ConfigManager configManager) {

        String optionKey = getOptionKeyForSlot(level, slot);

        if (optionKey != null) {

            boolean currentValue = configManager.isPunishmentOptionEnabled(level, optionKey);
            configManager.setPunishmentOptionEnabled(level, optionKey, !currentValue);


            new LevelPunishmentSettingsGUI(configManager, level).openInventory(player);
        } else if (slot == 49) {
            new PunishmentSettingsGUI(configManager).openInventory(player);
        }
    }

    /**
     * Maps GUI slots to option keys based on level
     */
    private static String getOptionKeyForSlot(int level, int slot) {
        switch (level) {
            case 1:
                switch (slot) {
                    case 10:
                        return "mining_fatigue";
                    case 14:
                        return "fake_diamonds";
                    case 28:
                        return "heavy_pickaxe";
                }
                break;

            case 2:
                switch (slot) {
                    case 10:
                        return "fake_ore_veins";
                    case 12:
                        return "inventory_drop";
                    case 14:
                        return "xray_vision_blur";
                    case 16:
                        return "tool_damage";
                    case 28:
                        return "paranoia_mode";
                }
                break;

            case 3:
                switch (slot) {
                    case 10:
                        return "temporary_kick";
                    case 12:
                        return "mining_license_suspension";
                    case 14:
                        return "resource_tax";
                    case 16:
                        return "decoy_attraction";
                    case 28:
                        return "fools_gold";
                }
                break;

            case 4:
                switch (slot) {
                    case 10:
                        return "extended_ban";
                    case 12:
                        return "mining_reputation";
                    case 14:
                        return "restricted_areas";
                    case 28:
                        return "cursed_pickaxe";
                }
                break;

            case 5:
                switch (slot) {
                    case 10:
                        return "long_term_ban";
                    case 12:
                        return "public_notification";
                    case 14:
                        return "permanent_mining_debuff";
                    case 16:
                        return "staff_review";
                    case 28:
                        return "stone_vision";
                }
                break;

            case 6:
                switch (slot) {
                    case 10:
                        return "permanent_ban";
                    case 12:
                        return "ip_tracking";
                    case 14:
                        return "security_report";
                    case 28:
                        return "tnt_execution";
                }
                break;
        }

        return null;
    }

    private void initializeItems() {

        inv.setItem(4, createGuiItem(Material.PAPER, "Level " + level + " Punishment Settings", Component.text("Configure which punishments are active for this level").color(NamedTextColor.GRAY), Component.text("Toggle each punishment on or off").color(NamedTextColor.GRAY)));


        switch (level) {
            case 1:
                addToggleOption(10, "mining_fatigue", Material.IRON_PICKAXE, "Mining Fatigue", "Apply Mining Fatigue I effect for 5 minutes");

                addToggleOption(14, "fake_diamonds", Material.COAL, "Fake Diamonds", "Replace the next 1-3 diamond ores they mine with coal");

                addToggleOption(28, "heavy_pickaxe", Material.NETHERITE_PICKAXE, "Heavy Pickaxe", "Make their pickaxe 'heavy' (slow dig speed) for 10 minutes", "with message 'Your pickaxe feels suspiciously heavy...'");
                break;

            case 2:
                addToggleOption(10, "fake_ore_veins", Material.DIAMOND_ORE, "Fake Ore Veins", "Have 50% chance mined ore veins turn to stone");

                addToggleOption(12, "inventory_drop", Material.DROPPER, "Inventory Drop", "Force them to drop 25% of their current inventory");

                addToggleOption(14, "xray_vision_blur", Material.GLASS, "X-Ray Vision Blur", "Apply Nausea effect when underground");

                addToggleOption(16, "tool_damage", Material.DAMAGED_ANVIL, "Tool Damage", "Damage their current mining tool by 50%");

                addToggleOption(28, "paranoia_mode", Material.CREEPER_HEAD, "Paranoia Mode", "Creates a psychological horror experience underground", "Includes ghost mobs, whispers, visual hallucinations", "and potentially startling jump scares", " ", "WARNING: May cause distress for some players", "Not recommended for those with heart conditions", " ", "Performance Note: Can be resource-intensive", "with many players. Consider disabling for busy servers.");
                break;

            case 3:
                addToggleOption(10, "temporary_kick", Material.IRON_DOOR, "Temporary Kick", "With a custom message about X-ray detection");

                addToggleOption(12, "mining_license_suspension", Material.FILLED_MAP, "Mining License Suspension", "Unable to mine diamond/emerald ore for 2 hours");

                addToggleOption(14, "resource_tax", Material.GOLD_INGOT, "Resource Tax", "All the ore will drop 50% less");

                addToggleOption(16, "decoy_attraction", Material.TIPPED_ARROW, "Decoy Attraction", "Generate more decoy ores around them than normal players");

                addToggleOption(28, "fools_gold", Material.GOLD_ORE, "Fool's Gold", "All the diamond will drop raw copper for 30 minutes");
                break;

            case 4:
                addToggleOption(10, "extended_ban", Material.REDSTONE_BLOCK, "Extended Ban", "3-day ban with detailed explanation");

                addToggleOption(12, "mining_reputation", Material.NAME_TAG, "Mining Reputation System", "Upon return, they start with 'Untrusted Miner' status visible to all players");

                addToggleOption(14, "restricted_areas", Material.BARRIER, "Restricted Areas", "Forbidden from mining below Y-level -40 for a week");

                addToggleOption(28, "cursed_pickaxe", Material.ENCHANTED_BOOK, "Cursed Pickaxe", "Their tools randomly break when mining valuable ores for a week");
                break;

            case 5:
                addToggleOption(10, "long_term_ban", Material.BEDROCK, "Long-Term Ban", "14-day ban");

                addToggleOption(12, "public_notification", Material.JUKEBOX, "Public Notification", "Server-wide announcement when they return");

                addToggleOption(14, "permanent_mining_debuff", Material.WITHER_SKELETON_SKULL, "Permanent Mining Debuff", "Always receive Mining Fatigue I when below Y-level 0");

                addToggleOption(16, "staff_review", Material.WRITABLE_BOOK, "Staff Review Required", "Must be cleared by staff to mine deeper than Y-level 25");

                addToggleOption(28, "stone_vision", Material.STONE, "Stone Vision", "All ores appear as stone blocks to them for 1 week after return");
                break;

            case 6:
                addToggleOption(10, "permanent_ban", Material.TNT, "Permanent Ban", "Account banned permanently");

                addToggleOption(12, "ip_tracking", Material.COMPASS, "IP Tracking", "Associated IPs flagged for monitoring");

                addToggleOption(14, "security_report", Material.BOOK, "Server Security Report", "Detailed log of all suspicious activities for admin review");

                addToggleOption(28, "tnt_execution", Material.TNT_MINECART, "TNT Execution", "Before the ban, make their last moments memorable by spawning TNT", "at their feet with message 'The DeepGuard system has detected", "critical levels of X-ray usage!'");
                break;
        }


        inv.setItem(49, createGuiItem(Material.BARRIER, "Back to ⚖ Punishment System"));
    }

    /**
     * Add a toggleable option to the GUI
     */
    private void addToggleOption(int slot, String configKey, Material material, String name, String... lore) {
        boolean enabled = configManager.isPunishmentOptionEnabled(level, configKey);
        Material displayMaterial = enabled ? material : Material.GRAY_DYE;

        List<Component> loreList = new ArrayList<>();
        loreList.add(enabled ? Component.text("Enabled").color(NamedTextColor.GREEN) : Component.text("Disabled").color(NamedTextColor.RED));
        loreList.add(Component.text(" "));

        for (String loreLine : lore) {
            loreList.add(Component.text(loreLine).color(NamedTextColor.GRAY));
        }

        loreList.add(Component.text(" "));
        loreList.add(Component.text("Click to toggle").color(NamedTextColor.YELLOW));

        inv.setItem(slot, createGuiItem(displayMaterial, name, loreList));
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

    /**
     * Helper method to create an item with colored lore
     */
    private ItemStack createGuiItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.GOLD));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void openInventory(Player player) {
        player.openInventory(inv);
    }
}