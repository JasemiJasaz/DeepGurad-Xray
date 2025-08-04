package org.myplugin.deepGuardXray.gui.subgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.managers.AppealManager;
import org.myplugin.deepGuardXray.managers.AppealManager.Appeal;
import org.myplugin.deepGuardXray.managers.AppealManager.AppealStatus;

import java.util.*;

public class AppealDetailsGUI {
    private static final Map<UUID, Integer> staffEnteringResponse = new HashMap<>();
    private final Inventory inv;
    private final deepGuardXray plugin;
    private final AppealManager appealManager;
    private final Appeal appeal;

    public AppealDetailsGUI(deepGuardXray plugin, AppealManager appealManager, Appeal appeal) {
        this.plugin = plugin;
        this.appealManager = appealManager;
        this.appeal = appeal;


        inv = Bukkit.createInventory(null, 54, Component.text("Appeal #" + appeal.getId() + " Details").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));


        initializeItems();
    }

    /**
     * Handle click events in the appeal details GUI
     */
    public static boolean handleClick(Player player, int slot, ClickType clickType, Inventory inventory, deepGuardXray plugin, AppealManager appealManager) {

        Component title = player.getOpenInventory().title();
        if (title == null || !title.toString().contains("Appeal #")) {
            return false;
        }


        String titleStr = title.toString();
        int appealId = Integer.parseInt(titleStr.substring(titleStr.indexOf("#") + 1, titleStr.indexOf(" Details")));


        Appeal appeal = appealManager.getAppeal(appealId);
        if (appeal == null) {
            player.sendMessage(Component.text("Appeal not found!").color(NamedTextColor.RED));
            player.closeInventory();
            return true;
        }


        if (slot == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new AppealGUI(plugin, appealManager, 0).openInventory(player);
            return true;
        }


        if (slot == 38) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
            player.closeInventory();


            staffEnteringResponse.put(player.getUniqueId(), appealId);


            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("╔═════════════════════════════════╗").color(NamedTextColor.GREEN));
            player.sendMessage(Component.text("║ ").color(NamedTextColor.GREEN).append(Component.text("Enter your response for this appeal:").color(NamedTextColor.WHITE)).append(Component.text(" ║").color(NamedTextColor.GREEN)));
            player.sendMessage(Component.text("║ ").color(NamedTextColor.GREEN).append(Component.text("Your next message will be sent to the").color(NamedTextColor.YELLOW)).append(Component.text(" ║").color(NamedTextColor.GREEN)));
            player.sendMessage(Component.text("║ ").color(NamedTextColor.GREEN).append(Component.text("player as part of the approval.").color(NamedTextColor.YELLOW)).append(Component.text(" ║").color(NamedTextColor.GREEN)));
            player.sendMessage(Component.text("║ ").color(NamedTextColor.GREEN).append(Component.text("Type ").color(NamedTextColor.WHITE)).append(Component.text("cancel").color(NamedTextColor.RED)).append(Component.text(" to cancel this action.").color(NamedTextColor.WHITE)).append(Component.text(" ║").color(NamedTextColor.GREEN)));
            player.sendMessage(Component.text("╚═════════════════════════════════╝").color(NamedTextColor.GREEN));


            listenForChatResponse(player, plugin, appealManager, appeal, AppealStatus.APPROVED);

            return true;
        }


        if (slot == 42) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1.0f);
            player.closeInventory();


            staffEnteringResponse.put(player.getUniqueId(), appealId);


            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("╔═════════════════════════════════╗").color(NamedTextColor.RED));
            player.sendMessage(Component.text("║ ").color(NamedTextColor.RED).append(Component.text("Enter your reason for denying:").color(NamedTextColor.WHITE)).append(Component.text(" ║").color(NamedTextColor.RED)));
            player.sendMessage(Component.text("║ ").color(NamedTextColor.RED).append(Component.text("Your next message will be sent to the").color(NamedTextColor.YELLOW)).append(Component.text(" ║").color(NamedTextColor.RED)));
            player.sendMessage(Component.text("║ ").color(NamedTextColor.RED).append(Component.text("player as part of the denial.").color(NamedTextColor.YELLOW)).append(Component.text(" ║").color(NamedTextColor.RED)));
            player.sendMessage(Component.text("║ ").color(NamedTextColor.RED).append(Component.text("Type ").color(NamedTextColor.WHITE)).append(Component.text("cancel").color(NamedTextColor.RED)).append(Component.text(" to cancel this action.").color(NamedTextColor.WHITE)).append(Component.text(" ║").color(NamedTextColor.RED)));
            player.sendMessage(Component.text("╚═════════════════════════════════╝").color(NamedTextColor.RED));


            listenForChatResponse(player, plugin, appealManager, appeal, AppealStatus.DENIED);

            return true;
        }


        if (slot == 40) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);


            appealManager.updateAppealStatus(appealId, AppealStatus.UNDER_REVIEW, player.getName(), "");


            player.sendMessage(Component.text("Appeal #" + appealId + " marked as under review.").color(NamedTextColor.GOLD));


            new AppealGUI(plugin, appealManager, 0).openInventory(player);

            return true;
        }

        return false;
    }

    /**
     * Listen for a chat response from a staff member
     */
    private static void listenForChatResponse(Player player, deepGuardXray plugin, AppealManager appealManager, Appeal appeal, AppealStatus newStatus) {

        plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {

                if (!event.getPlayer().equals(player)) return;


                if (!staffEnteringResponse.containsKey(player.getUniqueId())) return;


                event.setCancelled(true);


                String message = event.getMessage();


                if (message.equalsIgnoreCase("cancel")) {
                    staffEnteringResponse.remove(player.getUniqueId());
                    player.sendMessage(Component.text("Appeal response canceled.").color(NamedTextColor.YELLOW));


                    org.bukkit.event.HandlerList.unregisterAll(this);


                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        new AppealDetailsGUI(plugin, appealManager, appeal).openInventory(player);
                    });

                    return;
                }


                int appealId = staffEnteringResponse.get(player.getUniqueId());


                staffEnteringResponse.remove(player.getUniqueId());


                org.bukkit.event.HandlerList.unregisterAll(this);


                plugin.getServer().getScheduler().runTask(plugin, () -> {

                    appealManager.updateAppealStatus(appealId, newStatus, player.getName(), message);


                    if (newStatus == AppealStatus.APPROVED) {
                        player.sendMessage(Component.text("Appeal #" + appealId + " approved successfully!").color(NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("Appeal #" + appealId + " denied.").color(NamedTextColor.RED));
                    }


                    new AppealGUI(plugin, appealManager, 0).openInventory(player);
                });
            }
        }, plugin);
    }

    private void initializeItems() {

        createBorder();


        addPlayerInfo();


        addAppealDetails();


        addActionButtons();
    }

    private void createBorder() {
        ItemStack borderItem = createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Component.text(" ").color(NamedTextColor.WHITE), false);


        for (int i = 0; i < 9; i++) {
            inv.setItem(i, borderItem);
            inv.setItem(45 + i, borderItem);
        }


        for (int i = 1; i < 5; i++) {
            inv.setItem(i * 9, borderItem);
            inv.setItem(i * 9 + 8, borderItem);
        }
    }

    private void addPlayerInfo() {

        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();


        Player targetPlayer = Bukkit.getPlayer(appeal.getPlayerId());
        if (targetPlayer != null) {
            skullMeta.setOwningPlayer(targetPlayer);
        } else {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(appeal.getPlayerId()));
        }


        skullMeta.displayName(Component.text(appeal.getPlayerName()).color(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Punishment Level: ").color(NamedTextColor.GRAY).append(Component.text(appeal.getPunishmentLevel()).color(NamedTextColor.RED)));
        lore.add(Component.text("Appeal Date: ").color(NamedTextColor.GRAY).append(Component.text(appeal.getFormattedTimestamp()).color(NamedTextColor.YELLOW)));
        lore.add(Component.text("Status: ").color(NamedTextColor.GRAY).append(Component.text(appeal.getStatus().getDisplayName()).color(appeal.getStatus().getColor())));

        skullMeta.lore(lore);
        playerHead.setItemMeta(skullMeta);


        inv.setItem(4, playerHead);
    }

    private void addAppealDetails() {

        String reason = appeal.getReason();
        List<String> reasonLines = breakIntoLines(reason, 30);


        for (int i = 0; i < Math.min(reasonLines.size(), 15); i++) {
            Material material = i == 0 ? Material.WRITABLE_BOOK : Material.PAPER;
            Component name = i == 0 ? Component.text("Appeal Reason:").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true) : Component.text("Continued...").color(NamedTextColor.YELLOW);

            ItemStack reasonItem = createGuiItem(material, name, false, Component.text(reasonLines.get(i)).color(NamedTextColor.WHITE));


            int row = 2 + (i / 5);
            int col = 2 + (i % 5);
            inv.setItem(row * 9 + col, reasonItem);
        }
    }

    private void addActionButtons() {

        ItemStack approveItem = createGuiItem(Material.LIME_WOOL, Component.text("APPROVE APPEAL").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true), true, Component.text("Click to approve this appeal").color(NamedTextColor.GRAY), Component.text("This will remove the player's punishment").color(NamedTextColor.GRAY), Component.empty(), Component.text("You'll be asked to enter a response").color(NamedTextColor.YELLOW));
        inv.setItem(38, approveItem);


        ItemStack denyItem = createGuiItem(Material.RED_WOOL, Component.text("DENY APPEAL").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true), true, Component.text("Click to deny this appeal").color(NamedTextColor.GRAY), Component.text("The punishment will remain active").color(NamedTextColor.GRAY), Component.empty(), Component.text("You'll be asked to enter a response").color(NamedTextColor.YELLOW));
        inv.setItem(42, denyItem);


        ItemStack reviewItem = createGuiItem(Material.ORANGE_WOOL, Component.text("MARK AS UNDER REVIEW").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true), true, Component.text("Click to mark as under review").color(NamedTextColor.GRAY), Component.text("This will notify the player").color(NamedTextColor.GRAY), Component.text("that their appeal is being reviewed").color(NamedTextColor.GRAY));
        inv.setItem(40, reviewItem);


        ItemStack backItem = createGuiItem(Material.ARROW, Component.text("← Back to Appeal List").color(NamedTextColor.YELLOW), false);
        inv.setItem(49, backItem);
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

    private List<String> breakIntoLines(String text, int maxLength) {
        List<String> lines = new ArrayList<>();


        if (text.length() <= maxLength) {
            lines.add(text);
            return lines;
        }


        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {

            if (currentLine.length() + word.length() + 1 > maxLength) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {

                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }


        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    public void openInventory(Player player) {
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
    }
}