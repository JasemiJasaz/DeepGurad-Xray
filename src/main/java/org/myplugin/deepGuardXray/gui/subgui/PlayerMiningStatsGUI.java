package org.myplugin.deepGuardXray.gui.subgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.myplugin.deepGuardXray.managers.StatsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerMiningStatsGUI {
    private final Inventory inv;
    private final UUID targetPlayerId;
    private final String targetPlayerName;

    public PlayerMiningStatsGUI(UUID playerId, String playerName) {
        this.targetPlayerId = playerId;
        this.targetPlayerName = playerName;

        inv = Bukkit.createInventory(null, 27, Component.text(targetPlayerName + "'s Mining Stats").color(NamedTextColor.BLUE));
        initializeItems();
    }

    /**
     * Handle clicks in this GUI
     */
    public static void handleClick(Player staff, int slot) {

        if (slot == 26) {
            new PlayerStatsMainGUI(0).openInventory(staff);
        }
    }

    private void initializeItems() {

        Map<Material, Integer> oreStats = StatsManager.getOreStats(targetPlayerId);

        if (oreStats.isEmpty()) {

            ItemStack noStats = new ItemStack(Material.BARRIER);
            ItemMeta noStatsMeta = noStats.getItemMeta();
            noStatsMeta.displayName(Component.text("No Mining Stats").color(NamedTextColor.RED));

            List<Component> noStatsLore = new ArrayList<>();
            noStatsLore.add(Component.text("This player hasn't mined any ores yet").color(NamedTextColor.GRAY));
            noStatsMeta.lore(noStatsLore);

            noStats.setItemMeta(noStatsMeta);
            inv.setItem(13, noStats);
        } else {

            int index = 0;
            for (Map.Entry<Material, Integer> entry : oreStats.entrySet()) {
                ItemStack oreItem = new ItemStack(entry.getKey(), 1);
                ItemMeta meta = oreItem.getItemMeta();
                meta.displayName(Component.text(formatMaterialName(entry.getKey().name())).color(NamedTextColor.GOLD));

                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Mined: " + entry.getValue()).color(NamedTextColor.GRAY));
                meta.lore(lore);

                oreItem.setItemMeta(meta);
                inv.setItem(index, oreItem);
                index++;
                if (index >= inv.getSize() - 1) break;
            }
        }


        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.displayName(Component.text("Back to 📊 Player Analytics").color(NamedTextColor.GREEN));
        backButton.setItemMeta(backMeta);
        inv.setItem(26, backButton);
    }

    /**
     * Format material name to be more readable
     */
    private String formatMaterialName(String name) {
        String[] words = name.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase()).append(" ");
            }
        }

        return result.toString().trim();
    }

    public void openInventory(Player player) {
        player.openInventory(inv);
    }

    public Inventory getInventory() {
        return inv;
    }
}