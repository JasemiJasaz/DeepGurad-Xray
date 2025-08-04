package org.myplugin.deepGuardXray.gui.subgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.gui.StaffMenuGUI;
import org.myplugin.deepGuardXray.ml.MLDataManager;
import org.myplugin.deepGuardXray.ml.MLManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI for Machine Learning Analysis and Management
 */
public class MLAnalysisGUI {

    public static final String PERMISSION = "deepguardx.gui_ml";
    public static final int MAX_PLAYERS = 5;
    private static final int INVENTORY_SIZE = 54;
    private static final int STATUS_SLOT = 4;
    private static final int TOGGLE_SLOT = 13;
    private static final int ANALYZE_PLAYER_SLOT = 26;
    private static final int TRAINING_STATS_SLOT = 17;
    private static final int AUTO_ANALYSIS_SLOT = 8;
    private static final int NORMAL_TRAINING_ROW = 3;
    private static final int CHEATER_TRAINING_ROW = 4;
    private static final int ADD_NORMAL_PLAYER_SLOT = getSlot(NORMAL_TRAINING_ROW, 8);
    private static final int ADD_CHEATER_PLAYER_SLOT = getSlot(CHEATER_TRAINING_ROW, 8);
    private static final int MONITORING_ROW = 2;
    private static final int BACK_SLOT = 49;
    private static final int REPORTS_SLOT = 53;
    private final deepGuardXray plugin;
    private final MLManager mlManager;

    public MLAnalysisGUI(deepGuardXray plugin) {
        this.plugin = plugin;
        this.mlManager = plugin.getMLManager();
    }

    /**
     * Handle clicks in the ML Analysis GUI
     */
    public static void handleClick(Player player, int slot, Inventory inventory, deepGuardXray plugin) {
        MLManager mlManager = plugin.getMLManager();


        if (slot == TOGGLE_SLOT) {
            boolean currentStatus = mlManager.isEnabled();
            mlManager.setEnabled(!currentStatus);


            player.playSound(player.getLocation(), currentStatus ? org.bukkit.Sound.BLOCK_STONE_BUTTON_CLICK_OFF : org.bukkit.Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.5f, 1.0f);


            player.sendMessage(Component.text("ML System " + (!currentStatus ? "enabled" : "disabled")).color(!currentStatus ? NamedTextColor.GREEN : NamedTextColor.RED));


            new MLAnalysisGUI(plugin).openInventory(player);
            return;
        }


        if (slot == BACK_SLOT) {
            new StaffMenuGUI().openInventory(player);
            return;
        }


        if (slot == AUTO_ANALYSIS_SLOT) {
            new AutoAnalysisGUI(plugin).openInventory(player);
            return;
        }


        if (slot == ANALYZE_PLAYER_SLOT) {

            if (mlManager.getPlayersUnderAnalysis().size() >= MAX_PLAYERS) {
                player.sendMessage(Component.text("Maximum number of players already being analyzed!").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }


            new PlayerSelectorGUI(plugin, PlayerSelectorGUI.SelectionType.ANALYZE).openInventory(player);
            return;
        }


        if (slot == ADD_NORMAL_PLAYER_SLOT) {

            long currentCount = mlManager.getPlayersInTraining().entrySet().stream().filter(e -> !e.getValue()).count();

            if (currentCount >= MAX_PLAYERS) {
                player.sendMessage(Component.text("Maximum number of normal players already in training!").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }


            new PlayerSelectorGUI(plugin, PlayerSelectorGUI.SelectionType.TRAIN_NORMAL).openInventory(player);
            return;
        }


        if (slot == ADD_CHEATER_PLAYER_SLOT) {

            long currentCount = mlManager.getPlayersInTraining().entrySet().stream().filter(Map.Entry::getValue).count();

            if (currentCount >= MAX_PLAYERS) {
                player.sendMessage(Component.text("Maximum number of cheaters already in training!").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }


            new PlayerSelectorGUI(plugin, PlayerSelectorGUI.SelectionType.TRAIN_CHEATER).openInventory(player);
            return;
        }

        if (slot == REPORTS_SLOT) {

            new MLReportsGUI(plugin, 0).openInventory(player);
            return;
        }


        if ((isInRow(slot, NORMAL_TRAINING_ROW) || isInRow(slot, CHEATER_TRAINING_ROW)) && slot % 9 != 0) {
            ItemStack clicked = inventory.getItem(slot);
            if (clicked != null && clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta.getOwningPlayer() != null) {
                    Player target = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());
                    if (target != null) {

                        mlManager.cancelTraining(target);

                        player.sendMessage(Component.text("Cancelled training for " + target.getName()).color(NamedTextColor.YELLOW));


                        new MLAnalysisGUI(plugin).openInventory(player);
                    }
                }
            }
        }
    }

    /**
     * Helper method to get slot index from row and column
     */
    private static int getSlot(int row, int col) {
        return row * 9 + col;
    }

    /**
     * Helper method to check if a slot is in a specific row
     */
    private static boolean isInRow(int slot, int row) {
        return slot >= row * 9 && slot < (row + 1) * 9;
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
     * Open the ML Analysis GUI for a player
     */
    public void openInventory(Player player) {

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You don't have permission to access the ML Analysis GUI.").color(NamedTextColor.RED));
            return;
        }


        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, Component.text("🤖 ML Analysis").color(NamedTextColor.DARK_AQUA));


        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, filler);
        }


        boolean isEnabled = mlManager.isEnabled();
        boolean isTrained = mlManager.isTrained();
        inventory.setItem(STATUS_SLOT, createStatusItem(isEnabled, isTrained));


        inventory.setItem(TOGGLE_SLOT, createToggleItem(isEnabled));


        inventory.setItem(TRAINING_STATS_SLOT, createTrainingStatsItem());


        inventory.setItem(REPORTS_SLOT, createReportsItem());


        addActiveMonitoringItems(inventory);


        addTrainingItems(inventory);


        inventory.setItem(AUTO_ANALYSIS_SLOT, createAutoAnalysisItem(mlManager.getMLConfig().isAutoAnalysisEnabled()));


        inventory.setItem(ANALYZE_PLAYER_SLOT, createAnalyzePlayerItem());
        inventory.setItem(ADD_NORMAL_PLAYER_SLOT, createAddNormalPlayerItem());
        inventory.setItem(ADD_CHEATER_PLAYER_SLOT, createAddCheaterPlayerItem());


        inventory.setItem(BACK_SLOT, createItem(Material.ARROW, "Back to Main Menu", List.of("Return to the main staff menu")));


        player.openInventory(inventory);
    }

    /**
     * Create the reports button item
     */
    private ItemStack createReportsItem() {
        return createItem(Material.PAPER, "§b📋 ML Analysis Reports", List.of("§eView detailed reports of ML analysis", "§eand detection results"));
    }

    private ItemStack createAutoAnalysisItem(boolean isEnabled) {
        Material material = Material.REPEATER;
        String name = "§b⚙ Auto Analysis Settings";

        List<String> lore = new ArrayList<>();
        lore.add("§eManage automatic ML analysis based");
        lore.add("§eon player suspicious counts");
        lore.add("");
        lore.add(isEnabled ? "§aCurrently: §aENABLED" : "§cCurrently: §cDISABLED");
        lore.add("§7Click to configure settings");

        return createItem(material, name, lore);
    }

    /**
     * Create the status indicator item
     */
    private ItemStack createStatusItem(boolean isEnabled, boolean isTrained) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();

        if (isEnabled) {
            material = Material.LIME_CONCRETE;
            name = "✅ ML System: ENABLED";
        } else {
            material = Material.RED_CONCRETE;
            name = "❌ ML System: DISABLED";
        }

        lore.add(isTrained ? "§aModel: TRAINED" : "§cModel: NOT TRAINED");

        if (!isTrained) {
            lore.add("");
            lore.add("§eYou need to collect training data");
            lore.add("§efrom normal players and cheaters.");
        }

        return createItem(material, name, lore);
    }

    /**
     * Create the toggle button item
     */
    private ItemStack createToggleItem(boolean isEnabled) {
        Material material = isEnabled ? Material.REDSTONE_BLOCK : Material.EMERALD_BLOCK;
        String name = isEnabled ? "§cDisable ML System" : "§aEnable ML System";
        List<String> lore = new ArrayList<>();

        lore.add(isEnabled ? "§eClick to disable the ML system" : "§eClick to enable the ML system");

        return createItem(material, name, lore);
    }

    /**
     * Create the analyze player button item
     */
    private ItemStack createAnalyzePlayerItem() {
        return createItem(Material.SPYGLASS, "§b📊 Select Player to Analyze", List.of("§eClick to select a player to analyze", "§efor potential X-ray usage"));
    }

    /**
     * Create button to add normal player for training
     */
    private ItemStack createAddNormalPlayerItem() {
        return createItem(Material.EMERALD, "§a📈 Select Normal Player for Training", List.of("§eClick to select a player to use", "§efor training as a NORMAL player", "", "§c⚠ WARNING:", "§fOnly select players you are CERTAIN", "§fare legitimate. Bad training data", "§fwill reduce detection accuracy.", "", "§fRecommended: Use yourself or other", "§ftrusted staff members for training."));
    }

    /**
     * Create button to add cheater for training
     */
    private ItemStack createAddCheaterPlayerItem() {
        return createItem(Material.REDSTONE, "§c🔍 Select Cheater for Training", List.of("§eClick to select a player to use", "§efor training as a CHEATER", "", "§c⚠ WARNING:", "§fOnly select players you have CONFIRMED", "§fare using X-ray. Never guess or assume.", "§fFalse data will harm the ML system.", "", "§fRecommended: Ask trusted staff members", "§for use X-ray yourself and mine diamonds", "§fto generate accurate training data."));
    }

    /**
     * Create the training statistics item
     */
    private ItemStack createTrainingStatsItem() {
        MLDataManager.MLTrainingData trainingData = MLDataManager.loadTrainingData();
        int normalCount = trainingData.getNormalFeatures().size();
        int cheaterCount = trainingData.getCheaterFeatures().size();
        int totalCount = normalCount + cheaterCount;

        List<String> lore = new ArrayList<>();
        lore.add("§eCurrent Training Data:");
        lore.add("§f• §aTotal Samples: §f" + totalCount);
        lore.add("§f• §aNormal Players: §f" + normalCount);
        lore.add("§f• §cCheaters: §f" + cheaterCount);
        lore.add("");

        if (!trainingData.hasEnoughData()) {
            lore.add("§cNot enough training data!");
            lore.add("§cNeed at least 3 samples of each type.");
        } else {
            lore.add("§aSufficient training data available.");
            lore.add("§a(always keep the samples balance)");
        }

        return createItem(Material.BOOK, "§dTraining Data Statistics", lore);
    }

    /**
     * Add items representing players currently being monitored
     */
    private void addActiveMonitoringItems(Inventory inventory) {

        inventory.setItem(getSlot(MONITORING_ROW, 0), createItem(Material.OBSERVER, "§bCurrently Monitoring", List.of("§ePlayers currently under analysis", "§eMax: " + MAX_PLAYERS + " players")));


        List<UUID> monitoredPlayers = new ArrayList<>(mlManager.getPlayersUnderAnalysis());


        for (int i = 0; i < 7; i++) {
            int slot = getSlot(MONITORING_ROW, i + 1);

            if (i < monitoredPlayers.size()) {
                UUID playerId = monitoredPlayers.get(i);
                Player player = Bukkit.getPlayer(playerId);

                if (player != null) {

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwningPlayer(player);
                    meta.displayName(Component.text(player.getName()).color(NamedTextColor.AQUA));

                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Currently analyzing...").color(NamedTextColor.YELLOW));


                    long remainingTime = getRemainingAnalysisTime(playerId);
                    if (remainingTime > 0) {
                        lore.add(Component.text("Time remaining: ").color(NamedTextColor.GRAY).append(Component.text(formatTime(remainingTime)).color(NamedTextColor.WHITE)));
                    }

                    double score = mlManager.getDetectionScore(playerId);
                    if (score >= 0) {
                        NamedTextColor scoreColor = score > 0.7 ? NamedTextColor.RED : score > 0.4 ? NamedTextColor.GOLD : NamedTextColor.GREEN;

                        lore.add(Component.text("Suspicion Score: ").color(NamedTextColor.GRAY).append(Component.text(String.format("%.1f%%", score * 100)).color(scoreColor)));
                    }


                    meta.lore(lore);
                    head.setItemMeta(meta);

                    inventory.setItem(slot, head);
                } else {

                    inventory.setItem(slot, createItem(Material.BARRIER, "§cOffline Player", List.of("§7This player is no longer online")));
                }
            } else if (i < MAX_PLAYERS) {

                inventory.setItem(slot, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Empty Slot", List.of("§7No player being monitored in this slot")));
            } else {

                inventory.setItem(slot, createItem(Material.RED_STAINED_GLASS_PANE, "§cLocked Slot", List.of("§7Maximum number of players reached")));
            }
        }
    }

    /**
     * Add items representing players currently being used for training
     */
    private void addTrainingItems(Inventory inventory) {

        inventory.setItem(getSlot(NORMAL_TRAINING_ROW, 0), createItem(Material.IRON_PICKAXE, "§aNormal Player Training", List.of("§ePlayers providing normal data", "§eMax: " + MAX_PLAYERS + " players")));


        inventory.setItem(getSlot(CHEATER_TRAINING_ROW, 0), createItem(Material.DIAMOND_PICKAXE, "§cCheater Training", List.of("§ePlayers providing cheater data", "§eMax: " + MAX_PLAYERS + " players")));


        Map<UUID, Boolean> trainingPlayers = mlManager.getPlayersInTraining();


        List<UUID> normalPlayers = trainingPlayers.entrySet().stream().filter(entry -> !entry.getValue()).map(Map.Entry::getKey).collect(Collectors.toList());

        List<UUID> cheaterPlayers = trainingPlayers.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList());


        for (int i = 0; i < 7; i++) {
            int slot = getSlot(NORMAL_TRAINING_ROW, i + 1);

            if (i < normalPlayers.size()) {
                UUID playerId = normalPlayers.get(i);
                Player player = Bukkit.getPlayer(playerId);

                if (player != null) {

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwningPlayer(player);
                    meta.displayName(Component.text(player.getName()).color(NamedTextColor.GREEN));

                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Training as normal player").color(NamedTextColor.GREEN));


                    long remainingTime = getRemainingTrainingTime(playerId);
                    if (remainingTime > 0) {
                        lore.add(Component.text("Time remaining: ").color(NamedTextColor.GRAY).append(Component.text(formatTime(remainingTime)).color(NamedTextColor.WHITE)));
                    }

                    lore.add(Component.text("Click to cancel training").color(NamedTextColor.GRAY));

                    meta.lore(lore);
                    head.setItemMeta(meta);

                    inventory.setItem(slot, head);
                }
            } else if (i < MAX_PLAYERS) {

                inventory.setItem(slot, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Empty Slot", List.of("§7No normal player in training")));
            } else {

                inventory.setItem(slot, createItem(Material.RED_STAINED_GLASS_PANE, "§cLocked Slot", List.of("§7Maximum number of players reached")));
            }
        }


        for (int i = 0; i < 7; i++) {
            int slot = getSlot(CHEATER_TRAINING_ROW, i + 1);

            if (i < cheaterPlayers.size()) {
                UUID playerId = cheaterPlayers.get(i);
                Player player = Bukkit.getPlayer(playerId);

                if (player != null) {

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwningPlayer(player);
                    meta.displayName(Component.text(player.getName()).color(NamedTextColor.RED));

                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Training as cheater").color(NamedTextColor.RED));


                    long remainingTime = getRemainingTrainingTime(playerId);
                    if (remainingTime > 0) {
                        lore.add(Component.text("Time remaining: ").color(NamedTextColor.GRAY).append(Component.text(formatTime(remainingTime)).color(NamedTextColor.WHITE)));
                    }

                    lore.add(Component.text("Click to cancel training").color(NamedTextColor.GRAY));

                    meta.lore(lore);
                    head.setItemMeta(meta);

                    inventory.setItem(slot, head);
                }
            } else if (i < MAX_PLAYERS) {

                inventory.setItem(slot, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Empty Slot", List.of("§7No cheater in training")));
            } else {

                inventory.setItem(slot, createItem(Material.RED_STAINED_GLASS_PANE, "§cLocked Slot", List.of("§7Maximum number of players reached")));
            }
        }
    }

    /**
     * Format time in seconds to a readable format
     *
     * @param seconds Time in seconds
     * @return Formatted time string (e.g., "10m 30s")
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (remainingSeconds == 0) {
            return minutes + "m";
        }

        return minutes + "m " + remainingSeconds + "s";
    }

    /**
     * Get remaining analysis time for a player
     *
     * @param playerId Player UUID
     * @return Remaining time in seconds, or -1 if not analyzing
     */
    private long getRemainingAnalysisTime(UUID playerId) {
        return mlManager.getRemainingAnalysisTime(playerId);
    }

    /**
     * Get remaining training time for a player
     *
     * @param playerId Player UUID
     * @return Remaining time in seconds, or -1 if not training
     */
    private long getRemainingTrainingTime(UUID playerId) {
        return mlManager.getRemainingTrainingTime(playerId);
    }
}