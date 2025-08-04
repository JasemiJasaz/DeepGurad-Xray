package org.myplugin.deepGuardXray.gui;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Updated StaffMenuGUI with appeal system integration
 */
public class StaffMenuGUI {
    private static deepGuardXray plugin;
    private final Inventory inv;

    public StaffMenuGUI() {

        inv = Bukkit.createInventory(null, 54, Component.text("✧ ").color(NamedTextColor.AQUA).append(Component.text("DeepGuard").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD)).append(Component.text("-").color(NamedTextColor.DARK_GRAY)).append(Component.text("XRay").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)).append(Component.text(" Control Panel").color(NamedTextColor.GOLD)).append(Component.text(" ✧").color(NamedTextColor.AQUA)));
        initializeItems();
    }

    public static void setPlugin(deepGuardXray deepGuardXray) {
        plugin = deepGuardXray;
    }

    private void initializeItems() {

        createBorder();


        addSectionTitles();


        addMainButtons();


        addAppealsButton();


        addDecorativeElements();


        addPluginInfo();
    }

    private void createBorder() {

        ItemStack topBorder = createGuiItem(Material.BLUE_STAINED_GLASS_PANE, Component.text(" ").color(NamedTextColor.DARK_GRAY), false);
        ItemStack bottomBorder = createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Component.text(" ").color(NamedTextColor.DARK_GRAY), false);


        ItemStack sideBorder = createGuiItem(Material.CYAN_STAINED_GLASS_PANE, Component.text(" ").color(NamedTextColor.DARK_GRAY), false);


        ItemStack cornerBorder = createGuiItem(Material.PURPLE_STAINED_GLASS_PANE, Component.text("DeepGuard-XRay").color(NamedTextColor.LIGHT_PURPLE), true);


        for (int i = 0; i < 9; i++) {
            inv.setItem(i, topBorder);
        }


        for (int i = 45; i < 54; i++) {
            inv.setItem(i, bottomBorder);
        }


        for (int i = 1; i <= 4; i++) {
            inv.setItem(i * 9, sideBorder);
            inv.setItem(i * 9 + 8, sideBorder);
        }


        inv.setItem(0, cornerBorder);
        inv.setItem(8, cornerBorder);
        inv.setItem(45, cornerBorder);
        inv.setItem(53, cornerBorder);
    }

    private void addSectionTitles() {

        inv.setItem(2, createGuiItem(Material.OBSERVER, Component.text("✦ Analysis & Monitoring ✦").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD), true));


        inv.setItem(6, createGuiItem(Material.LODESTONE, Component.text("✦ Management & Configuration ✦").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD), true));
    }

    private void addMainButtons() {


        inv.setItem(10, createEnhancedItem(Material.PLAYER_HEAD, Component.text("📊 Player Analytics").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD), true, Arrays.asList(Component.text("Detailed mining statistics and player data").color(NamedTextColor.GRAY), Component.text("• View ore mining trends").color(NamedTextColor.WHITE), Component.text("• Track player mining history").color(NamedTextColor.WHITE), Component.empty(), Component.text("» Click to access analytics dashboard").color(NamedTextColor.GREEN)), "MHF_Question"));


        inv.setItem(19, createGuiItem(Material.SPYGLASS, Component.text("🔍 Suspicious Activity").color(NamedTextColor.RED).decorate(TextDecoration.BOLD), true, Component.text("Monitor potentially suspicious players").color(NamedTextColor.GRAY), Component.text("• Recent flagged behaviors").color(NamedTextColor.WHITE), Component.text("• Abnormal mining patterns").color(NamedTextColor.WHITE), Component.text("• X-Ray probability assessment").color(NamedTextColor.WHITE), Component.empty(), Component.text("» Click to investigate").color(NamedTextColor.RED)));


        inv.setItem(28, createGuiItem(Material.DRAGON_HEAD, Component.text("🤖 ML Analysis").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD), true, Component.text("Advanced reasoning-based detection").color(NamedTextColor.GRAY), Component.text("• Mining style recognition").color(NamedTextColor.WHITE), Component.text("• Gets smarter over time [With More training data]").color(NamedTextColor.WHITE), Component.text("• Ore discovery pattern detection").color(NamedTextColor.WHITE), Component.text("• Step-by-step reasoning engine").color(NamedTextColor.WHITE), Component.empty(), Component.text("» Click to access ML tools").color(NamedTextColor.DARK_PURPLE)));


        inv.setItem(14, createGuiItem(Material.WRITABLE_BOOK, Component.text("⚖ Punishment System").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD), true, Component.text("Configure automated enforcement").color(NamedTextColor.GRAY), Component.text("• Manage punishment tiers").color(NamedTextColor.WHITE), Component.text("• Configure response actions").color(NamedTextColor.WHITE), Component.text("• Set escalation thresholds").color(NamedTextColor.WHITE), Component.empty(), Component.text("» Click to manage").color(NamedTextColor.GOLD)));


        inv.setItem(23, createGuiItem(Material.END_CRYSTAL, Component.text("🔔 Discord Webhook").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD), true, Component.text("Real-time Discord notifications").color(NamedTextColor.GRAY), Component.text("• Customize alert channels").color(NamedTextColor.WHITE), Component.text("• Configure notification types").color(NamedTextColor.WHITE), Component.empty(), Component.text("» Click to configure").color(NamedTextColor.LIGHT_PURPLE)));


        inv.setItem(32, createGuiItem(Material.DIAMOND_PICKAXE, Component.text("⛏ Ore Management").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD), true, Component.text("Configure ore tracking system").color(NamedTextColor.GRAY), Component.text("• Set tracked ore types").color(NamedTextColor.WHITE), Component.text("• Configure detection thresholds").color(NamedTextColor.WHITE), Component.text("• Manage decoy ore placement").color(NamedTextColor.WHITE), Component.empty(), Component.text("» Click to configure").color(NamedTextColor.AQUA)));


        inv.setItem(25, createGuiItem(Material.COMMAND_BLOCK, Component.text("⚙ Plugin Configuration").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD), true, Component.text("Advanced plugin settings").color(NamedTextColor.GRAY), Component.text("• Core functionality options").color(NamedTextColor.WHITE), Component.text("• Staff permission controls").color(NamedTextColor.WHITE), Component.empty(), Component.text("» Click to configure").color(NamedTextColor.LIGHT_PURPLE)));
    }

    /**
     * Add the appeals button with pending count
     */
    private void addAppealsButton() {

        int pendingCount = 0;

        if (plugin != null && plugin.getAppealManager() != null) {
            pendingCount = plugin.getAppealManager().getPendingAppeals().size();
        }


        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Manage player punishment appeals").color(NamedTextColor.GRAY));
        lore.add(Component.empty());


        if (pendingCount > 0) {
            lore.add(Component.text("• ").color(NamedTextColor.WHITE).append(Component.text(pendingCount + " pending ").color(NamedTextColor.YELLOW)).append(Component.text(pendingCount == 1 ? "appeal" : "appeals").color(NamedTextColor.WHITE)));
        } else {
            lore.add(Component.text("• No pending appeals").color(NamedTextColor.WHITE));
        }

        lore.add(Component.text("• Review player submissions").color(NamedTextColor.WHITE));
        lore.add(Component.text("• Approve or deny appeals").color(NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(Component.text("» Click to manage appeals").color(NamedTextColor.GOLD));


        Material material = pendingCount > 0 ? Material.FILLED_MAP : Material.MAP;


        ItemStack appealsItem = createGuiItem(material, Component.text("📋 Player Appeals").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD), pendingCount > 0, lore.toArray(new Component[0]));


        inv.setItem(34, appealsItem);
    }

    private void addDecorativeElements() {

        ItemStack centerIcon = createGuiItem(Material.NETHER_STAR, Component.text("DeepGuard-XRay").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD), true, Component.text("Advanced X-Ray Protection").color(NamedTextColor.YELLOW), Component.text("Powerful anti-cheat system").color(NamedTextColor.YELLOW));
        inv.setItem(4, centerIcon);


        ItemStack redstone = createGuiItem(Material.REDSTONE, Component.text("Active Protection").color(NamedTextColor.RED), false);
        ItemStack compass = createGuiItem(Material.COMPASS, Component.text("Player Monitoring").color(NamedTextColor.BLUE), false);
        ItemStack barrier = createGuiItem(Material.BARRIER, Component.text("Cheat Prevention").color(NamedTextColor.DARK_RED), false);


        inv.setItem(37, redstone);
        inv.setItem(43, compass);
        inv.setItem(40, barrier);
    }

    private void addPluginInfo() {

        String version = "v" + (plugin != null ? plugin.getDescription().getVersion() : "1.0.0");
        ItemStack versionItem = createGuiItem(Material.BOOK, Component.text("Plugin Information").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD), false, Component.text("DeepGuard-XRay " + version).color(NamedTextColor.YELLOW), Component.text("Developed by Jasemi").color(NamedTextColor.AQUA), Component.empty(), Component.text("Active protection systems:").color(NamedTextColor.GREEN), Component.text("✓ Mining pattern analysis").color(NamedTextColor.WHITE), Component.text("✓ ML-based detection").color(NamedTextColor.WHITE), Component.text("✓ Decoy ore system").color(NamedTextColor.WHITE), Component.text("✓ Player appeals system").color(NamedTextColor.WHITE));
        inv.setItem(49, versionItem);
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
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }


        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEnhancedItem(Material material, Component name, boolean enchanted, List<Component> lore, String skullOwner) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (material == Material.PLAYER_HEAD && skullOwner != null && meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwningPlayer(Bukkit.getOfflinePlayer(skullOwner));
        }

        meta.displayName(name);

        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore);
        }

        if (enchanted) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }


        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }

    public void openInventory(Player player) {
        player.openInventory(inv);

        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.7f, 1.2f);
    }
}