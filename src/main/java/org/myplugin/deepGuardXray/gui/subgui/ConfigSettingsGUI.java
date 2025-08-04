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
import org.myplugin.deepGuardXray.gui.StaffMenuGUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigSettingsGUI {

    public static final String PERMISSION = "deepguardx.gui_config";
    private final static String TITLE = "⚙ Plugin Configuration";
    private final Inventory inv;
    private final ConfigManager configManager;
    private final deepGuardXray plugin;

    public ConfigSettingsGUI(ConfigManager configManager, deepGuardXray plugin) {
        this.configManager = configManager;
        this.plugin = plugin;


        inv = Bukkit.createInventory(null, 27, Component.text(TITLE).color(NamedTextColor.AQUA));

        initializeItems();
    }

    public static void handleClick(Player player, int slot, Inventory inventory, ConfigManager configManager, deepGuardXray plugin) {

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You don't have permission to modify the config settings.").color(NamedTextColor.RED));
            player.closeInventory();
            return;
        }


        switch (slot) {
            case 11:
                new DecoySettingsGUI(configManager, plugin).openInventory(player);
                break;
            case 13:
                new AutoSaveSettingsGUI(configManager, plugin).openInventory(player);
                break;
            case 15:
                new StaffSettingsGUI(configManager, plugin).openInventory(player);
                break;
            case 22:
                new StaffMenuGUI().openInventory(player);
                break;
        }
    }

    private void initializeItems() {

        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", false);


        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(18 + i, border);
        }
        inv.setItem(9, border);
        inv.setItem(17, border);


        inv.setItem(4, createGuiItem(Material.REDSTONE, "Plugin Configuration", true, TextDecoration.BOLD, NamedTextColor.RED, Component.text("Select a category to configure").color(NamedTextColor.GRAY)));


        inv.setItem(11, createGuiItem(Material.DIAMOND_PICKAXE, "Decoy Settings", true, TextDecoration.BOLD, NamedTextColor.AQUA, Component.text("Configure decoy ore generation").color(NamedTextColor.GRAY), Component.text("Settings for detecting X-ray").color(NamedTextColor.GRAY)));

        inv.setItem(13, createGuiItem(Material.CLOCK, "Auto-Save Settings", true, TextDecoration.BOLD, NamedTextColor.YELLOW, Component.text("Configure automatic data saving").color(NamedTextColor.GRAY), Component.text("Manage data persistence").color(NamedTextColor.GRAY)));

        inv.setItem(15, createGuiItem(Material.SHIELD, "Staff Settings", true, TextDecoration.BOLD, NamedTextColor.GREEN, Component.text("Configure staff alerts and tools").color(NamedTextColor.GRAY), Component.text("Manage staff notifications").color(NamedTextColor.GRAY)));


        inv.setItem(22, createGuiItem(Material.BARRIER, "Back to ⛏ Staff Control Panel", false, null, NamedTextColor.RED));
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

    public void openInventory(Player player) {

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You don't have permission to access the config settings.").color(NamedTextColor.RED));
            return;
        }

        player.openInventory(inv);
    }
}