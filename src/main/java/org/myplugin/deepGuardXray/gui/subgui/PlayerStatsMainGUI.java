package org.myplugin.deepGuardXray.gui.subgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.myplugin.deepGuardXray.gui.StaffMenuGUI;
import org.myplugin.deepGuardXray.managers.StatsManager;

import java.util.*;

public class PlayerStatsMainGUI {
    private final int itemsPerPage = 45;
    private final List<OfflinePlayer> playersWithStats;
    private final int totalPages;
    private final Map<Integer, UUID> playerSlots = new HashMap<>();
    private final Inventory inv;
    private final int currentPage;

    public PlayerStatsMainGUI(int currentPage) {
        this.currentPage = currentPage;

        inv = Bukkit.createInventory(null, 54, Component.text("📊 Player Analytics").color(NamedTextColor.AQUA));


        playersWithStats = getPlayersWithStats();


        totalPages = Math.max(1, (int) Math.ceil((double) playersWithStats.size() / itemsPerPage));

        initializeItems();
    }

    /**
     * Handle click events in this GUI
     */
    public static void handleClick(Player staff, int slot, Inventory clickedInventory) {

        if (slot < 45 && clickedInventory.getItem(slot) != null && clickedInventory.getItem(slot).getType() == Material.PLAYER_HEAD) {


            PlayerStatsMainGUI gui = new PlayerStatsMainGUI(0);


            UUID targetId = gui.playerSlots.get(slot);

            if (targetId != null) {

                ItemStack head = clickedInventory.getItem(slot);
                String playerName = "Unknown";

                if (head.hasItemMeta() && head.getItemMeta().hasDisplayName()) {
                    Component nameComponent = head.getItemMeta().displayName();
                    playerName = nameComponent.toString().replace("\"", "");
                }


                new PlayerMiningStatsGUI(targetId, playerName).openInventory(staff);
            }
        } else if (slot == 45 && clickedInventory.getItem(45).getType() == Material.ARROW) {

            Component title = clickedInventory.getItem(49).getItemMeta().displayName();
            String titleText = title.toString();
            int currentPage = Integer.parseInt(titleText.split("/")[0].split(" ")[1].trim()) - 1;
            new PlayerStatsMainGUI(currentPage - 1).openInventory(staff);
        } else if (slot == 53 && clickedInventory.getItem(53).getType() == Material.ARROW) {

            Component title = clickedInventory.getItem(49).getItemMeta().displayName();
            String titleText = title.toString();
            int currentPage = Integer.parseInt(titleText.split("/")[0].split(" ")[1].trim()) - 1;
            new PlayerStatsMainGUI(currentPage + 1).openInventory(staff);
        } else if (slot == 47) {

            new StaffMenuGUI().openInventory(staff);
        }
    }

    /**
     * Get all players who have mining statistics
     */
    private List<OfflinePlayer> getPlayersWithStats() {
        List<OfflinePlayer> players = new ArrayList<>();


        for (UUID playerId : StatsManager.getAllPlayerIds()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);

            if (offlinePlayer != null && offlinePlayer.getName() != null) {
                players.add(offlinePlayer);
            }
        }

        return players;
    }

    private void initializeItems() {
        inv.clear();
        playerSlots.clear();


        setNavigationItems();


        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, playersWithStats.size());


        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;


            if (slot >= 45) break;

            OfflinePlayer p = playersWithStats.get(i);
            inv.setItem(slot, createPlayerHead(p));
            playerSlots.put(slot, p.getUniqueId());
        }
    }

    private void setNavigationItems() {

        if (currentPage > 0) {
            inv.setItem(45, createNavItem(Material.ARROW, "Previous Page", NamedTextColor.GREEN));
        } else {
            inv.setItem(45, createNavItem(Material.BARRIER, "No Previous Page", NamedTextColor.RED));
        }


        inv.setItem(47, createNavItem(Material.BARRIER, "Back to ⛏ Staff Control Panel", NamedTextColor.RED));


        inv.setItem(49, createNavItem(Material.PAPER, "Page " + (currentPage + 1) + " / " + Math.max(1, totalPages), NamedTextColor.YELLOW));


        if (currentPage < totalPages - 1) {
            inv.setItem(53, createNavItem(Material.ARROW, "Next Page", NamedTextColor.GREEN));
        } else {
            inv.setItem(53, createNavItem(Material.BARRIER, "No Next Page", NamedTextColor.RED));
        }
    }

    private ItemStack createNavItem(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(color));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerHead(OfflinePlayer player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text(player.getName()).color(NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to view mining stats").color(NamedTextColor.GRAY));


        boolean isOnline = player.isOnline();
        lore.add(Component.text("Status: " + (isOnline ? "Online" : "Offline")).color(isOnline ? NamedTextColor.GREEN : NamedTextColor.RED));

        lore.add(Component.text("Player: " + player.getName()).color(NamedTextColor.WHITE));
        meta.lore(lore);

        head.setItemMeta(meta);
        return head;
    }

    public void openInventory(Player player) {
        player.openInventory(inv);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public Map<Integer, UUID> getPlayerSlots() {
        return playerSlots;
    }
}