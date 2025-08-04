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
import org.myplugin.deepGuardXray.managers.SuspiciousManager;
import org.myplugin.deepGuardXray.ml.MLDataManager;

import java.util.*;

public class SuspiciousPlayersGUI {
    public static final String PERMISSION = "deepguardx.gui_suspicious";
    private final List<UUID> suspiciousPlayers;
    private final int itemsPerPage = 36;
    private final int currentPage;
    private final int totalPages;
    private final Map<Integer, UUID> playerSlots = new HashMap<>();
    private final Inventory inv;

    public SuspiciousPlayersGUI(int currentPage) {
        this.currentPage = currentPage;

        inv = Bukkit.createInventory(null, 54, Component.text("🔍 Suspicious Activity").color(NamedTextColor.AQUA));


        suspiciousPlayers = new ArrayList<>();


        Map<UUID, Integer> counts = SuspiciousManager.getSuspiciousCounts();
        suspiciousPlayers.addAll(counts.keySet());

        totalPages = (int) Math.ceil((double) suspiciousPlayers.size() / itemsPerPage);
        initializeItems();
    }

    /**
     * Handle click events in this GUI
     */
    public static void handleClick(Player staff, int slot, Inventory clickedInventory) {
        SuspiciousPlayersGUI gui = new SuspiciousPlayersGUI(0);


        if (slot == 45 && clickedInventory.getItem(45).getType() == Material.ARROW) {

            Component title = clickedInventory.getItem(46).getItemMeta().displayName();
            String titleText = title.toString();
            int currentPage = Integer.parseInt(titleText.split("/")[0].split(" ")[1]) - 1;
            new SuspiciousPlayersGUI(currentPage - 1).openInventory(staff);
        } else if (slot == 53 && clickedInventory.getItem(53).getType() == Material.ARROW) {

            Component title = clickedInventory.getItem(46).getItemMeta().displayName();
            String titleText = title.toString();
            int currentPage = Integer.parseInt(titleText.split("/")[0].split(" ")[1]) - 1;
            new SuspiciousPlayersGUI(currentPage + 1).openInventory(staff);
        } else if (slot == 49) {

            new StaffMenuGUI().openInventory(staff);
        } else if (slot < 36 && clickedInventory.getItem(slot) != null && clickedInventory.getItem(slot).getType() == Material.PLAYER_HEAD) {

            UUID playerId = gui.playerSlots.get(slot);
            if (playerId != null) {
                Player target = Bukkit.getPlayer(playerId);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);

                if (target != null) {

                    int reportCount = MLDataManager.getPlayerReports(target.getName()).size();


                    staff.sendMessage(Component.text("Basic information for ").color(NamedTextColor.YELLOW).append(Component.text(target.getName()).color(NamedTextColor.GOLD)).append(Component.text(".").color(NamedTextColor.YELLOW)));

                    if (reportCount > 0) {
                        staff.sendMessage(Component.text("This player has ").color(NamedTextColor.YELLOW).append(Component.text(reportCount).color(NamedTextColor.GOLD)).append(Component.text(" ML reports. Check ").color(NamedTextColor.YELLOW)).append(Component.text("ML Reports").color(NamedTextColor.AQUA)).append(Component.text(" for detailed analysis.").color(NamedTextColor.YELLOW)));


                        if (staff.hasPermission(MLReportsGUI.PERMISSION)) {
                            staff.sendMessage(Component.text("Use ").color(NamedTextColor.GRAY).append(Component.text("ml reports gui " + target.getName()).color(NamedTextColor.WHITE)).append(Component.text(" to view all reports.").color(NamedTextColor.GRAY)));
                        }
                    } else {
                        staff.sendMessage(Component.text("This player has no ML reports yet.").color(NamedTextColor.YELLOW));
                    }
                } else if (offlinePlayer != null && offlinePlayer.getName() != null) {

                    int reportCount = MLDataManager.getPlayerReports(offlinePlayer.getName()).size();

                    staff.sendMessage(Component.text("Player ").color(NamedTextColor.YELLOW).append(Component.text(offlinePlayer.getName()).color(NamedTextColor.GOLD)).append(Component.text(" is offline.").color(NamedTextColor.YELLOW)));

                    if (reportCount > 0) {
                        staff.sendMessage(Component.text("This player has ").color(NamedTextColor.YELLOW).append(Component.text(reportCount).color(NamedTextColor.GOLD)).append(Component.text(" ML reports. Check ").color(NamedTextColor.YELLOW)).append(Component.text("ML Reports").color(NamedTextColor.AQUA)).append(Component.text(" for detailed analysis.").color(NamedTextColor.YELLOW)));


                        if (staff.hasPermission(MLReportsGUI.PERMISSION)) {
                            staff.sendMessage(Component.text("Use ").color(NamedTextColor.GRAY).append(Component.text("ml reports gui " + offlinePlayer.getName()).color(NamedTextColor.WHITE)).append(Component.text(" to view all reports.").color(NamedTextColor.GRAY)));
                        }
                    } else {
                        staff.sendMessage(Component.text("This player has no ML reports yet.").color(NamedTextColor.YELLOW));
                    }
                } else {
                    staff.sendMessage(Component.text("Player information unavailable.").color(NamedTextColor.RED));
                }
            }
        }
    }

    private void initializeItems() {
        inv.clear();
        playerSlots.clear();


        setNavigationItems();

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, suspiciousPlayers.size());
        int slot = 0;


        for (int i = startIndex; i < endIndex; i++) {
            if (slot >= 36) break;
            UUID uuid = suspiciousPlayers.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);


            int count = 0;
            if (SuspiciousManager.getSuspiciousCounts().containsKey(uuid)) {
                count = SuspiciousManager.getSuspiciousCounts().get(uuid);
            }


            int reportCount = 0;
            if (op.getName() != null) {

                reportCount = MLDataManager.getPlayerReports(op.getName()).size();
            }

            inv.setItem(slot, createPlayerHead(op, count, reportCount));
            playerSlots.put(slot, uuid);
            slot++;
        }


        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.displayName(Component.text("Back to ⛏ Staff Control Panel").color(NamedTextColor.RED));
        backButton.setItemMeta(backMeta);
        inv.setItem(49, backButton);
    }

    private void setNavigationItems() {

        if (currentPage > 0) {
            inv.setItem(45, createNavItem(Material.ARROW, "Previous Page", NamedTextColor.GREEN));
        } else {
            inv.setItem(45, createNavItem(Material.BARRIER, "No Previous Page", NamedTextColor.RED));
        }


        inv.setItem(46, createNavItem(Material.PAPER, "Page " + (currentPage + 1) + "/" + Math.max(1, totalPages), NamedTextColor.YELLOW));


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

    private ItemStack createPlayerHead(OfflinePlayer op, int suspiciousCount, int reportCount) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(op);
        meta.displayName(Component.text(op.getName()).color(NamedTextColor.GOLD));


        int level = suspiciousCount / 10;
        int xpForNextLevel = (level + 1) * 10;
        int xpThisLevel = suspiciousCount % 10;
        int progressPercent = (int) ((xpThisLevel / 10.0) * 100);
        Component progressBar = getProgressBar(progressPercent, 10);


        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Decoy Ore Mined: " + suspiciousCount).color(NamedTextColor.GRAY));
        lore.add(Component.text("Level: " + level).color(NamedTextColor.GRAY));
        lore.add(Component.text("Progress: ").color(NamedTextColor.GRAY).append(progressBar).append(Component.text(" " + progressPercent + "%").color(NamedTextColor.GRAY)));

        lore.add(getSuspiciousStatusComponent(suspiciousCount));


        NamedTextColor reportColor = NamedTextColor.GRAY;
        if (reportCount > 10) {
            reportColor = NamedTextColor.RED;
        } else if (reportCount > 5) {
            reportColor = NamedTextColor.GOLD;
        } else if (reportCount > 0) {
            reportColor = NamedTextColor.YELLOW;
        }

        lore.add(Component.text("ML Reports: ").color(NamedTextColor.GRAY).append(Component.text(reportCount).color(reportColor)));


        lore.add(Component.text(""));
        lore.add(Component.text("Click to view details").color(NamedTextColor.WHITE));

        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private Component getProgressBar(int percent, int totalBars) {
        int filledBars = (int) Math.round((percent / 100.0) * totalBars);
        Component bar = Component.empty();

        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                bar = bar.append(Component.text("|").color(NamedTextColor.GREEN));
            } else {
                bar = bar.append(Component.text("|").color(NamedTextColor.DARK_GRAY));
            }
        }

        return bar;
    }

    private Component getSuspiciousStatusComponent(int suspiciousCount) {
        Component baseText = Component.text("Status: ").color(NamedTextColor.GRAY);

        if (suspiciousCount < 10) {
            return baseText.append(Component.text("Low").color(NamedTextColor.GREEN)).append(Component.text(" (Might be mined by luck)").color(NamedTextColor.GRAY));
        } else if (suspiciousCount < 20) {
            return baseText.append(Component.text("Mid").color(NamedTextColor.YELLOW)).append(Component.text(" (It's probably okay, right?)").color(NamedTextColor.GRAY));
        } else if (suspiciousCount < 30) {
            return baseText.append(Component.text("High").color(NamedTextColor.GOLD)).append(Component.text(" (Something's definitely off)").color(NamedTextColor.GRAY));
        } else if (suspiciousCount < 40) {
            return baseText.append(Component.text("Extreme").color(NamedTextColor.RED)).append(Component.text(" (This one is seriously fishy!)").color(NamedTextColor.GRAY));
        } else if (suspiciousCount < 50) {
            return baseText.append(Component.text("Insane").color(NamedTextColor.DARK_RED)).append(Component.text(" (No way, that's downright criminal!)").color(NamedTextColor.GRAY));
        } else {
            return baseText.append(Component.text("Godlike").color(NamedTextColor.LIGHT_PURPLE)).append(Component.text(" (Absolutely beyond reason!)").color(NamedTextColor.GRAY));
        }
    }

    /**
     * Opens this GUI for the given player, checking for permission
     */
    public void openInventory(Player player) {
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You do not have permission to view suspicious players.").color(NamedTextColor.RED));
            return;
        }
        player.openInventory(inv);
    }

    public Map<Integer, UUID> getPlayerSlots() {
        return playerSlots;
    }
}