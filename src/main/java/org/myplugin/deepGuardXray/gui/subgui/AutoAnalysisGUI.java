package org.myplugin.deepGuardXray.gui.subgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.ml.MLConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI for configuring Automatic ML Analysis based on suspicious count
 */
public class AutoAnalysisGUI {
    public static final String PERMISSION = "deepguardx.gui_ml";

    private static final int INVENTORY_SIZE = 27;

    private static final int STATUS_SLOT = 4;
    private static final int TOGGLE_SLOT = 10;
    private static final int THRESHOLD_SLOT = 12;
    private static final int INCREASE_THRESHOLD_SLOT = 13;
    private static final int DECREASE_THRESHOLD_SLOT = 11;
    private static final int WAITING_LIST_SLOT = 16;
    private static final int BACK_SLOT = 22;

    private final deepGuardXray plugin;
    private final MLConfig mlConfig;

    public AutoAnalysisGUI(deepGuardXray plugin) {
        this.plugin = plugin;
        this.mlConfig = plugin.getMLManager().getMLConfig();
    }

    /**
     * Handle clicks in the Auto Analysis GUI
     */
    public static void handleClick(Player player, int slot, Inventory inventory, deepGuardXray plugin) {
        MLConfig mlConfig = plugin.getMLManager().getMLConfig();

        if (slot == TOGGLE_SLOT) {
            boolean currentStatus = mlConfig.isAutoAnalysisEnabled();

            plugin.getMLManager().setAutoAnalysisEnabled(!currentStatus);

            player.playSound(player.getLocation(), currentStatus ? org.bukkit.Sound.BLOCK_STONE_BUTTON_CLICK_OFF : org.bukkit.Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.5f, 1.0f);

            player.sendMessage(Component.text("Auto Analysis " + (!currentStatus ? "enabled" : "disabled")).color(!currentStatus ? NamedTextColor.GREEN : NamedTextColor.RED));

            new AutoAnalysisGUI(plugin).openInventory(player);
            return;
        }

        if (slot == BACK_SLOT) {
            new MLAnalysisGUI(plugin).openInventory(player);
            return;
        }

        if (slot == INCREASE_THRESHOLD_SLOT) {
            int currentThreshold = mlConfig.getSuspiciousThreshold();
            mlConfig.setSuspiciousThreshold(currentThreshold + 1);

            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
            player.sendMessage(Component.text("Suspicious count threshold increased to " + (currentThreshold + 1)).color(NamedTextColor.GREEN));

            new AutoAnalysisGUI(plugin).openInventory(player);
            return;
        }

        if (slot == DECREASE_THRESHOLD_SLOT) {
            int currentThreshold = mlConfig.getSuspiciousThreshold();
            if (currentThreshold > 1) {
                mlConfig.setSuspiciousThreshold(currentThreshold - 1);

                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.8f);
                player.sendMessage(Component.text("Suspicious count threshold decreased to " + (currentThreshold - 1)).color(NamedTextColor.YELLOW));
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                player.sendMessage(Component.text("Threshold cannot be lower than 1").color(NamedTextColor.RED));
            }

            new AutoAnalysisGUI(plugin).openInventory(player);
        }
    }

    /**
     * Helper method to create an item
     */
    private static ItemStack createItem(Material material, String name) {
        return createItem(material, name, Collections.emptyList());
    }

    /**
     * Helper method to create an item with lore
     */
    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name));

        if (!lore.isEmpty()) {
            List<Component> loreComponents = lore.stream().map(line -> {
                if (line.startsWith("§")) {
                    return Component.text(line);
                } else {
                    return Component.text(line).color(NamedTextColor.GRAY);
                }
            }).collect(Collectors.toList());

            meta.lore(loreComponents);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Open the Auto Analysis GUI for a player
     */
    public void openInventory(Player player) {
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You don't have permission to access the Auto Analysis GUI.").color(NamedTextColor.RED));
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, Component.text("⚙ Auto ML Analysis Settings").color(NamedTextColor.DARK_AQUA));

        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, filler);
        }

        boolean isEnabled = mlConfig.isAutoAnalysisEnabled();
        inventory.setItem(STATUS_SLOT, createStatusItem(isEnabled));
        inventory.setItem(TOGGLE_SLOT, createToggleItem(isEnabled));
        inventory.setItem(THRESHOLD_SLOT, createThresholdItem(mlConfig.getSuspiciousThreshold()));
        inventory.setItem(INCREASE_THRESHOLD_SLOT, createItem(Material.LIME_DYE, "§a▲ Increase Threshold", List.of("§7Click to increase the suspicious", "§7count threshold by 1")));
        inventory.setItem(DECREASE_THRESHOLD_SLOT, createItem(Material.RED_DYE, "§c▼ Decrease Threshold", List.of("§7Click to decrease the suspicious", "§7count threshold by 1")));
        inventory.setItem(WAITING_LIST_SLOT, createWaitingListItem());
        inventory.setItem(BACK_SLOT, createItem(Material.ARROW, "§fBack to ML Analysis", List.of("§7Return to the ML analysis menu")));

        player.openInventory(inventory);
    }

    /**
     * status indicator item
     */
    private ItemStack createStatusItem(boolean isEnabled) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();

        if (isEnabled) {
            material = Material.LIME_CONCRETE;
            name = "✅ Auto Analysis: ENABLED";
            lore.add("§aThe system will automatically analyze");
            lore.add("§aplayers when they reach the suspicious");
            lore.add("§acount threshold");
        } else {
            material = Material.RED_CONCRETE;
            name = "❌ Auto Analysis: DISABLED";
            lore.add("§cAutomatic player analysis is currently");
            lore.add("§cdisabled. Enable it to automatically");
            lore.add("§canalyze suspicious players");
        }

        return createItem(material, name, lore);
    }

    /**
     * toggle button item
     */
    private ItemStack createToggleItem(boolean isEnabled) {
        Material material = isEnabled ? Material.REDSTONE_BLOCK : Material.EMERALD_BLOCK;
        String name = isEnabled ? "§cDisable Auto Analysis" : "§aEnable Auto Analysis";
        List<String> lore = new ArrayList<>();

        lore.add(isEnabled ? "§eClick to disable automatic analysis" : "§eClick to enable automatic analysis");
        lore.add("§7When enabled, players with suspicious");
        lore.add("§7counts higher than the threshold will");
        lore.add("§7be automatically analyzed");

        return createItem(material, name, lore);
    }

    /**
     * threshold setting item
     */
    private ItemStack createThresholdItem(int threshold) {
        return createItem(Material.HOPPER, "§bSuspicious Count Threshold: §f" + threshold, List.of("§7Players need this many suspicious", "§7counts to trigger automatic analysis", "§7Current setting: §f" + threshold));
    }

    /**
     * waiting list info item
     */
    private ItemStack createWaitingListItem() {
        int queueSize = plugin.getMLManager().getAutoAnalysisQueueSize();
        int currentlyAnalyzing = plugin.getMLManager().getPlayersUnderAnalysis().size();
        int maxPlayers = mlConfig.getMaxAutoAnalysisPlayers();

        List<String> lore = new ArrayList<>();
        lore.add("§7Maximum players analyzed at once: §f" + maxPlayers);
        lore.add("§7Currently analyzing: §f" + currentlyAnalyzing + "/" + maxPlayers);
        lore.add("§7Players in waiting queue: §f" + queueSize);
        lore.add("");
        lore.add("§7Players are analyzed in real-time when");
        lore.add("§7their suspicious count exceeds the");
        lore.add("§7threshold during mining activity");
        lore.add("");
        lore.add("§7Players with higher suspicious counts");
        lore.add("§7are prioritized in the queue");
        lore.add("");
        lore.add("§7Players are only analyzed once");
        lore.add("§7to avoid redundant processing");

        return createItem(Material.PAPER, "§eWaiting List Information", lore);
    }
}