package org.myplugin.deepGuardXray.gui.subgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.myplugin.deepGuardXray.config.ConfigManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class OreConfigGUI {
    public static final String PERMISSION = "deepguardx.gui_oreconfig";
    private final Inventory inv;
    private final ConfigManager configManager;
    private final Material[] availableOres = new Material[]{

            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,

            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS};

    public OreConfigGUI(ConfigManager configManager) {
        this.configManager = configManager;

        inv = Bukkit.createInventory(null, 27, Component.text("⛏ Ore Management").color(NamedTextColor.AQUA));
        initializeItems();
    }

    private void initializeItems() {

        inv.clear();

        Set<Material> naturalOres = configManager.getNaturalOres();

        int slot = 0;
        for (Material ore : availableOres) {

            ItemStack item = new ItemStack(ore);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(ore.name()).color(NamedTextColor.GOLD));


            List<Component> lore = new ArrayList<>();
            if (naturalOres.contains(ore)) {
                lore.add(Component.text("Selected").color(NamedTextColor.GREEN));

                meta.addEnchant(Enchantment.MENDING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(Component.text("Not Selected").color(NamedTextColor.RED));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
        }

        inv.setItem(26, createGuiItem(Material.BARRIER, "Back to ⛏ Staff Control Panel"));
    }

    private ItemStack createGuiItem(Material material, String name, Component... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.GOLD));

        if (lore.length > 0) {
            meta.lore(Arrays.asList(lore));
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Opens this GUI for the given player.
     */
    public void openInventory(Player player) {
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You do not have permission to configure ores.").color(NamedTextColor.RED));
            return;
        }
        player.openInventory(inv);
    }

    /**
     * Call this method after toggling an ore to refresh the GUI.
     */
    public void refresh() {
        initializeItems();
    }
}