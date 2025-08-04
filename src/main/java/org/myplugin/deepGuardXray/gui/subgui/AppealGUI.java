package org.myplugin.deepGuardXray.gui.subgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.managers.AppealManager;
import org.myplugin.deepGuardXray.managers.AppealManager.Appeal;

import java.util.*;

public class AppealGUI {
    private static final int PAGE_SIZE = 36;
    private static final int ITEMS_PER_ROW = 9;
    private final Inventory inv;
    private final deepGuardXray plugin;
    private final AppealManager appealManager;
    private final Map<Integer, Appeal> appealSlots = new HashMap<>();
    private final int page;
    private final int maxPage;

    public AppealGUI(deepGuardXray plugin, AppealManager appealManager, int page) {
        this.plugin = plugin;
        this.appealManager = appealManager;
        this.page = Math.max(0, page);


        List<Appeal> pendingAppeals = appealManager.getPendingAppeals();


        this.maxPage = (int) Math.ceil((double) pendingAppeals.size() / PAGE_SIZE);


        inv = Bukkit.createInventory(null, 54, Component.text("📋 DeepGuardX Appeal System").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));


        initializeItems(pendingAppeals);
    }

    /**
     * Handle click events in the appeal list GUI
     */
    public static boolean handleClick(Player player, int slot, Inventory inventory, deepGuardXray plugin, AppealManager appealManager) {

        AppealGUI gui = getGuiFromInventory(inventory, plugin, appealManager);
        if (gui == null) return false;


        if (slot == 48 || slot == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();

            org.myplugin.deepGuardXray.gui.StaffMenuGUI staffMenu = new org.myplugin.deepGuardXray.gui.StaffMenuGUI();
            staffMenu.openInventory(player);
            return true;
        }


        if (slot == 45 && gui.page > 0) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new AppealGUI(plugin, appealManager, gui.page - 1).openInventory(player);
            return true;
        }


        if (slot == 53 && gui.page < gui.maxPage - 1) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new AppealGUI(plugin, appealManager, gui.page + 1).openInventory(player);
            return true;
        }


        if (gui.appealSlots.containsKey(slot)) {
            Appeal appeal = gui.appealSlots.get(slot);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new AppealDetailsGUI(plugin, appealManager, appeal).openInventory(player);
            return true;
        }

        return false;
    }

    /**
     * Get the AppealGUI instance from an inventory
     */
    private static AppealGUI getGuiFromInventory(Inventory inventory, deepGuardXray plugin, AppealManager appealManager) {

        if (inventory.getViewers().isEmpty()) return null;


        Component title = inventory.getViewers().get(0).getOpenInventory().title();
        if (title == null) return null;

        String titleStr = title.toString();
        if (titleStr.contains("Appeal System")) {


            return new AppealGUI(plugin, appealManager, 0);
        }

        return null;
    }

    private void initializeItems(List<Appeal> appeals) {

        appeals.sort(Comparator.comparing(Appeal::getTimestamp).reversed());


        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, appeals.size());

        for (int i = startIndex; i < endIndex; i++) {
            Appeal appeal = appeals.get(i);
            int slot = (i - startIndex) % PAGE_SIZE;


            ItemStack item = createAppealItem(appeal);
            inv.setItem(slot, item);
            appealSlots.put(slot, appeal);
        }


        if (maxPage > 1) {

            if (page > 0) {
                ItemStack prevItem = createGuiItem(Material.ARROW, Component.text("◀ Previous Page").color(NamedTextColor.YELLOW), false, Component.text("Go to page " + page).color(NamedTextColor.GRAY));
                inv.setItem(45, prevItem);
            }


            if (page < maxPage - 1) {
                ItemStack nextItem = createGuiItem(Material.ARROW, Component.text("Next Page ▶").color(NamedTextColor.YELLOW), false, Component.text("Go to page " + (page + 2)).color(NamedTextColor.GRAY));
                inv.setItem(53, nextItem);
            }


            ItemStack pageItem = createGuiItem(Material.PAPER, Component.text("Page " + (page + 1) + "/" + maxPage).color(NamedTextColor.GOLD), false);
            inv.setItem(49, pageItem);
        }


        ItemStack backItem = createGuiItem(Material.BARRIER, Component.text("↩ Back to Staff Menu").color(NamedTextColor.RED), false);
        inv.setItem(maxPage > 1 ? 48 : 49, backItem);


        ItemStack countItem = createGuiItem(Material.BOOK, Component.text(appeals.size() + " Pending Appeals").color(NamedTextColor.AQUA), false);
        inv.setItem(maxPage > 1 ? 50 : 51, countItem);
    }

    private ItemStack createAppealItem(Appeal appeal) {
        Material material;
        switch (appeal.getPunishmentLevel()) {
            case 1:
                material = Material.YELLOW_WOOL;
                break;
            case 2:
                material = Material.ORANGE_WOOL;
                break;
            case 3:
                material = Material.RED_WOOL;
                break;
            case 4:
                material = Material.PURPLE_WOOL;
                break;
            case 5:
                material = Material.MAGENTA_WOOL;
                break;
            case 6:
                material = Material.BLACK_WOOL;
                break;
            default:
                material = Material.WHITE_WOOL;
        }


        ItemStack item = new ItemStack(Material.PLAYER_HEAD);


        SkullMeta meta = (SkullMeta) item.getItemMeta();


        Player player = Bukkit.getPlayer(appeal.getPlayerId());
        if (player != null) {
            meta.setOwningPlayer(player);
        } else {

            meta.setOwningPlayer(Bukkit.getOfflinePlayer(appeal.getPlayerId()));
        }


        meta.displayName(Component.text("Appeal #" + appeal.getId() + ": ").color(NamedTextColor.GOLD).append(Component.text(appeal.getPlayerName()).color(NamedTextColor.YELLOW)));


        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Level " + appeal.getPunishmentLevel() + " Punishment").color(NamedTextColor.RED));
        lore.add(Component.text("Submitted: " + appeal.getFormattedTimestamp()).color(NamedTextColor.GRAY));
        lore.add(Component.empty());


        String reasonPreview = appeal.getReason().length() > 30 ? appeal.getReason().substring(0, 27) + "..." : appeal.getReason();
        lore.add(Component.text("Reason: ").color(NamedTextColor.WHITE).append(Component.text(reasonPreview).color(NamedTextColor.YELLOW)));

        lore.add(Component.empty());
        lore.add(Component.text("» Click to view details").color(NamedTextColor.GREEN));

        meta.lore(lore);


        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

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
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public void openInventory(Player player) {
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);
    }
}