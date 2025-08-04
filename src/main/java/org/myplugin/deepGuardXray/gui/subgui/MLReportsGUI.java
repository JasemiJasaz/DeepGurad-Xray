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
import org.myplugin.deepGuardXray.ml.MLDataManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * GUI for displaying ML analysis reports with pagination
 */
public class MLReportsGUI {

    public static final String PERMISSION = "deepguardx.gui_ml";


    private static final int INVENTORY_SIZE = 54;


    private static final int REPORTS_PER_PAGE = 28;


    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int BACK_SLOT = 49;


    private static final int PAGE_INDICATOR_SLOT = 47;


    private static final Map<Integer, String> REPORT_PATHS = new HashMap<>();
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final deepGuardXray plugin;
    private final int currentPage;
    private final String playerFilter;

    /**
     * Create a new ML Reports GUI
     *
     * @param plugin The plugin instance
     * @param page   The page number (0-indexed)
     */
    public MLReportsGUI(deepGuardXray plugin, int page) {
        this(plugin, page, null);
    }

    /**
     * Create a new ML Reports GUI with optional player filter
     *
     * @param plugin       The plugin instance
     * @param page         The page number (0-indexed)
     * @param playerFilter Optional player name to filter reports by
     */
    public MLReportsGUI(deepGuardXray plugin, int page, String playerFilter) {
        this.plugin = plugin;
        this.currentPage = Math.max(0, page);
        this.playerFilter = playerFilter;
    }

    /**
     * Handle clicks in the ML Reports GUI
     */
    public static void handleClick(Player player, int slot, Inventory inventory, deepGuardXray plugin) {

        if (slot == BACK_SLOT) {
            new MLAnalysisGUI(plugin).openInventory(player);
            return;
        }


        if (slot == PREV_PAGE_SLOT) {

            ItemStack pageIndicator = inventory.getItem(PAGE_INDICATOR_SLOT);
            if (pageIndicator != null && pageIndicator.hasItemMeta()) {
                String name = pageIndicator.getItemMeta().displayName().toString();
                try {
                    int currentPage = Integer.parseInt(name.split(" ")[1]) - 1;


                    String title = inventory.getType().getDefaultTitle();
                    String playerFilter = null;
                    if (title.contains(": ")) {
                        playerFilter = title.substring(title.indexOf(": ") + 2);
                    }


                    new MLReportsGUI(plugin, currentPage - 1, playerFilter).openInventory(player);
                } catch (Exception e) {

                    new MLReportsGUI(plugin, 0).openInventory(player);
                }
            }
            return;
        }


        if (slot == NEXT_PAGE_SLOT) {

            ItemStack pageIndicator = inventory.getItem(PAGE_INDICATOR_SLOT);
            if (pageIndicator != null && pageIndicator.hasItemMeta()) {
                String name = pageIndicator.getItemMeta().displayName().toString();
                try {
                    int currentPage = Integer.parseInt(name.split(" ")[1]) - 1;


                    String title = inventory.getType().getDefaultTitle();
                    String playerFilter = null;
                    if (title.contains(": ")) {
                        playerFilter = title.split(": ")[1];
                    }


                    new MLReportsGUI(plugin, currentPage + 1, playerFilter).openInventory(player);
                } catch (Exception e) {

                    new MLReportsGUI(plugin, 0).openInventory(player);
                }
            }
            return;
        }


        if (slot == 46) {
            String title = inventory.getType().getDefaultTitle();

            if (title.contains(": ")) {

                new MLReportsGUI(plugin, 0).openInventory(player);
            } else {

                player.closeInventory();
                player.sendMessage(Component.text("Please type the name of the player to filter reports for.").color(NamedTextColor.YELLOW));


                player.sendMessage(Component.text("Use: /dgx ml reports player <name>").color(NamedTextColor.YELLOW));
            }
            return;
        }


        String reportPath = REPORT_PATHS.get(slot);
        if (reportPath != null) {

            openDetailedReport(player, reportPath, plugin);
        }
    }

    /**
     * Open a detailed report view for a player
     */
    private static void openDetailedReport(Player player, String reportPath, deepGuardXray plugin) {
        try {

            String reportContent = new String(Files.readAllBytes(Paths.get(reportPath)));


            Pattern playerPattern = Pattern.compile("\"playerName\":\\s*\"([^\"]*)\"");
            Matcher playerMatcher = playerPattern.matcher(reportContent);
            String playerName = playerMatcher.find() ? playerMatcher.group(1) : "Unknown";


            double suspicionScore = extractSuspicionScoreStatic(reportContent);


            player.sendMessage(Component.text("---- ML Analysis Report for " + playerName + " ----").color(NamedTextColor.GOLD));

            player.sendMessage(Component.text("Suspicion Score: " + String.format("%.1f%%", suspicionScore * 100)).color(getSuspicionColorComponent(suspicionScore)));


            String conclusion = extractConclusionStatic(reportContent);
            player.sendMessage(Component.text("Conclusion: ").color(NamedTextColor.GOLD).append(Component.text(conclusion).color(NamedTextColor.WHITE)));


            List<String> reasoningSteps = extractReasoningSteps(reportContent);

            player.sendMessage(Component.text("Analysis Process:").color(NamedTextColor.GOLD));

            int stepNumber = 1;
            for (String step : reasoningSteps) {
                player.sendMessage(Component.text(stepNumber + ". ").color(NamedTextColor.YELLOW).append(Component.text(step).color(NamedTextColor.GRAY)));
                stepNumber++;
            }


            Map<String, Double> metrics = extractKeyMetrics(reportContent);

            if (!metrics.isEmpty()) {
                player.sendMessage(Component.text("Key Player Metrics:").color(NamedTextColor.GOLD));

                for (Map.Entry<String, Double> entry : metrics.entrySet()) {
                    String key = entry.getKey();
                    Double value = entry.getValue();


                    String displayKey = key.replace("_", " ").replace("ore count", "ore blocks").replace("ore rate", "ore blocks/min");


                    String[] words = displayKey.split(" ");
                    StringBuilder formattedKey = new StringBuilder();
                    for (String word : words) {
                        if (word.length() > 0) {
                            formattedKey.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
                        }
                    }


                    String formattedValue;
                    if (key.contains("rate") || key.contains("per_")) {
                        formattedValue = String.format("%.2f", value);
                    } else if (value % 1 == 0) {
                        formattedValue = String.format("%.0f", value);
                    } else {
                        formattedValue = String.format("%.2f", value);
                    }

                    player.sendMessage(Component.text("  " + formattedKey.toString().trim() + ": ").color(NamedTextColor.YELLOW).append(Component.text(formattedValue).color(NamedTextColor.WHITE)));
                }
            }

            player.sendMessage(Component.text("----------------------------------------").color(NamedTextColor.GOLD));

        } catch (Exception e) {
            player.sendMessage(Component.text("Error loading report: " + e.getMessage()).color(NamedTextColor.RED));
            plugin.getLogger().warning("Error loading report from " + reportPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract suspicion score from report content (static version)
     */
    private static double extractSuspicionScoreStatic(String reportContent) {
        try {
            Pattern pattern = Pattern.compile("\"suspicionScore\":\\s*([0-9.]+)");
            Matcher matcher = pattern.matcher(reportContent);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {

        }
        return 0.0;
    }

    /**
     * Extract conclusion from report content (static version)
     */
    private static String extractConclusionStatic(String reportContent) {
        try {
            Pattern pattern = Pattern.compile("\"conclusion\":\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(reportContent);
            if (matcher.find()) {
                return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
            }
        } catch (Exception e) {

        }
        return "No conclusion available";
    }

    /**
     * Extract reasoning steps from report content
     */
    private static List<String> extractReasoningSteps(String reportContent) {
        List<String> steps = new ArrayList<>();

        try {

            int stepsStart = reportContent.indexOf("\"reasoningSteps\"");
            if (stepsStart == -1) return steps;

            int arrayStart = reportContent.indexOf("[", stepsStart);
            int arrayEnd = reportContent.indexOf("]", arrayStart);

            if (arrayStart == -1 || arrayEnd == -1) return steps;


            String stepsArray = reportContent.substring(arrayStart + 1, arrayEnd);


            String[] rawSteps = stepsArray.split(",\\s*\"");

            for (String step : rawSteps) {

                step = step.trim();
                if (step.startsWith("\"")) {
                    step = step.substring(1);
                }
                if (step.endsWith("\"")) {
                    step = step.substring(0, step.length() - 1);
                }


                step = step.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");

                if (!step.isEmpty()) {
                    steps.add(step);
                }
            }

        } catch (Exception e) {

        }

        return steps;
    }

    /**
     * Extract key metrics from report content
     */
    private static Map<String, Double> extractKeyMetrics(String reportContent) {
        Map<String, Double> metrics = new LinkedHashMap<>();

        try {

            if (!reportContent.contains("\"playerData\"")) {
                return metrics;
            }


            String[] keyMetricPatterns = {"sessionDuration", "total_blocks_mined", "total_ores_mined", "ores_per_minute", "diamonds_per_hour", "ore_count_diamond", "ore_count_iron", "ore_count_gold", "ore_count_redstone", "ore_count_lapis", "ore_count_emerald", "ore_count_coal", "ore_rate_diamond", "ore_rate_iron", "ore_rate_gold", "head_direction_changes", "head_direction_change_rate"};


            for (String metricName : keyMetricPatterns) {
                Pattern pattern = Pattern.compile("\"" + metricName + "\":\\s*([0-9.]+)");
                Matcher matcher = pattern.matcher(reportContent);
                if (matcher.find()) {
                    metrics.put(metricName, Double.parseDouble(matcher.group(1)));
                }
            }

        } catch (Exception e) {

        }

        return metrics;
    }

    /**
     * Get NamedTextColor for suspicion level
     */
    private static NamedTextColor getSuspicionColorComponent(double score) {
        if (score >= 0.8) return NamedTextColor.RED;
        if (score >= 0.6) return NamedTextColor.GOLD;
        if (score >= 0.4) return NamedTextColor.YELLOW;
        if (score >= 0.2) return NamedTextColor.GREEN;
        return NamedTextColor.GRAY;
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
     * Open the ML Reports GUI for a player
     */
    public void openInventory(Player player) {

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You don't have permission to access the ML Reports GUI.").color(NamedTextColor.RED));
            return;
        }


        String title = "📋 ML Analysis Reports";
        if (playerFilter != null) {
            title = "📋 ML Analysis Reports: " + playerFilter;
        }

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, Component.text(title).color(NamedTextColor.DARK_AQUA));


        addReportsFromManager(inventory);


        addNavigationBar(inventory);


        player.openInventory(inventory);
    }

    /**
     * Add navigation bar to the inventory
     */
    private void addNavigationBar(Inventory inventory) {

        List<String> reportPaths = MLDataManager.getPlayerReports(playerFilter);
        int totalReports = reportPaths.size();
        int totalPages = Math.max(1, (totalReports + REPORTS_PER_PAGE - 1) / REPORTS_PER_PAGE);


        StringBuilder pagesIndicator = new StringBuilder();
        for (int i = 0; i < totalPages; i++) {
            if (i == currentPage) {
                pagesIndicator.append("§a").append(i + 1);
            } else {
                pagesIndicator.append("§7").append(i + 1);
            }

            if (i < totalPages - 1) {
                pagesIndicator.append("§7/");
            }
        }


        inventory.setItem(PAGE_INDICATOR_SLOT, createItem(Material.PAPER, "§ePage " + (currentPage + 1) + " of " + totalPages, List.of("§7Total Reports: " + totalReports, "§7Pages: " + pagesIndicator)));


        if (currentPage > 0) {
            inventory.setItem(PREV_PAGE_SLOT, createItem(Material.ARROW, "§aPrevious Page", List.of("§7Go to page " + currentPage)));
        } else {

            inventory.setItem(PREV_PAGE_SLOT, createItem(Material.GRAY_DYE, "§8Previous Page", List.of("§7You are on the first page")));
        }


        if (currentPage < totalPages - 1) {
            inventory.setItem(NEXT_PAGE_SLOT, createItem(Material.ARROW, "§aNext Page", List.of("§7Go to page " + (currentPage + 2))));
        } else {

            inventory.setItem(NEXT_PAGE_SLOT, createItem(Material.GRAY_DYE, "§8Next Page", List.of("§7You are on the last page")));
        }


        inventory.setItem(BACK_SLOT, createItem(Material.BARRIER, "§cBack to ML Analysis", List.of("§7Return to the main ML Analysis menu")));
    }

    /**
     * Add actual reports from the ML data manager
     */
    private void addReportsFromManager(Inventory inventory) {

        List<String> reportPaths = MLDataManager.getPlayerReports(playerFilter);


        int startIndex = currentPage * REPORTS_PER_PAGE;
        int endIndex = Math.min(startIndex + REPORTS_PER_PAGE, reportPaths.size());


        if (startIndex >= reportPaths.size()) {

            ItemStack noReports = createItem(Material.BARRIER, "§cNo reports found", List.of("§7There are no ML analysis reports to display."));

            inventory.setItem(22, noReports);
            return;
        }


        List<String> pageReports = reportPaths.subList(startIndex, endIndex);


        int slot = 0;
        for (String reportPath : pageReports) {
            try {

                File reportFile = new File(reportPath);
                String fileName = reportFile.getName();


                String reportContent = new String(Files.readAllBytes(Paths.get(reportPath)));
                String playerName = "Unknown";


                Pattern nameJsonPattern = Pattern.compile("\"playerName\":\\s*\"([^\"]*)\"");
                Matcher nameJsonMatcher = nameJsonPattern.matcher(reportContent);
                if (nameJsonMatcher.find()) {
                    playerName = nameJsonMatcher.group(1);
                } else {

                    Pattern pattern = Pattern.compile("([^_]+)_(.+)_(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})\\.json");
                    Matcher matcher = pattern.matcher(fileName);
                    if (matcher.find()) {
                        playerName = matcher.group(1);
                    }
                }


                Pattern pattern = Pattern.compile("([^_]+)_(.+)_(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})\\.json");
                Matcher matcher = pattern.matcher(fileName);

                String suspicionLevel = "unknown";
                Date timestamp = new Date();

                if (matcher.find()) {

                    suspicionLevel = matcher.group(2);


                    SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                    try {
                        timestamp = fileFormat.parse(matcher.group(3));
                    } catch (Exception e) {

                    }
                }


                double suspicionScore = extractSuspicionScore(reportContent);
                String conclusion = extractConclusion(reportContent);


                String shortConclusion = conclusion;
                if (conclusion.length() > 100) {
                    shortConclusion = conclusion.substring(0, 97) + "...";
                }


                Material material;
                if (suspicionScore >= 0.8) {
                    material = Material.RED_CONCRETE;
                } else if (suspicionScore >= 0.6) {
                    material = Material.ORANGE_CONCRETE;
                } else if (suspicionScore >= 0.4) {
                    material = Material.YELLOW_CONCRETE;
                } else {
                    material = Material.LIME_CONCRETE;
                }


                List<String> lore = new ArrayList<>();
                lore.add("§7Date: §f" + DISPLAY_DATE_FORMAT.format(timestamp));
                lore.add("§7Suspicion Score: §" + getSuspicionColor(suspicionScore) + String.format("%.1f%%", suspicionScore * 100));
                lore.add("§7Level: §" + getSuspicionColor(suspicionScore) + formatSuspicionLevel(suspicionLevel));
                lore.add("");
                lore.add("§7Conclusion:");


                List<String> wrappedConclusion = wrapText(shortConclusion, 35);
                for (String line : wrappedConclusion) {
                    lore.add("§8" + line);
                }

                lore.add("");
                lore.add("§eClick to view detailed report");


                int row = slot / 7;
                int col = slot % 7;
                int inventorySlot = row * 9 + col;


                ItemStack reportItem;
                try {
                    reportItem = createPlayerHead(playerName, "§" + getSuspicionColor(suspicionScore) + playerName, lore);
                } catch (Exception e) {

                    reportItem = createItem(material, "§" + getSuspicionColor(suspicionScore) + playerName, lore);
                }


                REPORT_PATHS.put(inventorySlot, reportPath);


                inventory.setItem(inventorySlot, reportItem);

                slot++;

            } catch (Exception e) {
                plugin.getLogger().warning("Error loading report from " + reportPath + ": " + e.getMessage());
            }
        }
    }

    /**
     * Create a player head item
     */
    private ItemStack createPlayerHead(String playerName, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.displayName(Component.text(displayName));

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


        meta.setOwner(playerName);

        head.setItemMeta(meta);
        return head;
    }

    /**
     * Extract suspicion score from report content
     */
    private double extractSuspicionScore(String reportContent) {
        try {
            Pattern pattern = Pattern.compile("\"suspicionScore\":\\s*([0-9.]+)");
            Matcher matcher = pattern.matcher(reportContent);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {

        }
        return 0.0;
    }

    /**
     * Extract conclusion from report content
     */
    private String extractConclusion(String reportContent) {
        try {
            Pattern pattern = Pattern.compile("\"conclusion\":\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(reportContent);
            if (matcher.find()) {
                return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
            }
        } catch (Exception e) {

        }
        return "No conclusion available";
    }

    /**
     * Format suspicion level for display
     */
    private String formatSuspicionLevel(String level) {
        return switch (level) {
            case "high_risk" -> "High Risk";
            case "suspicious" -> "Suspicious";
            case "medium_risk" -> "Medium Risk";
            case "low_risk" -> "Low Risk";
            case "normal" -> "Normal";
            default -> level.replace("_", " ");
        };
    }

    /**
     * Get color code for suspicion level
     */
    private char getSuspicionColor(double score) {
        if (score >= 0.8) return 'c';
        if (score >= 0.6) return '6';
        if (score >= 0.4) return 'e';
        if (score >= 0.2) return 'a';
        return '7';
    }

    /**
     * Wrap text to specified width
     */
    private List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        String[] words = text.split(" ");
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= width) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}