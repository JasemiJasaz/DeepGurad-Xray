package org.myplugin.deepGuardXray.ml;

import org.bukkit.Bukkit;
import org.myplugin.deepGuardXray.deepGuardXray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Manages saving, loading, and processing ML data files
 */
public class MLDataManager {
    private static final String TRAINING_DIR = "training";
    private static final String ANALYSIS_DIR = "analysis";
    private static final String REPORTS_DIR = "reports";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private static deepGuardXray plugin;
    private static File dataDir;

    /**
     * Initialize the data manager
     *
     * @param plugin The plugin instance
     */
    public static void initialize(deepGuardXray plugin) {
        MLDataManager.plugin = plugin;


        dataDir = new File(plugin.getDataFolder(), "ml-data");
        File trainingDir = new File(dataDir, TRAINING_DIR);
        File analysisDir = new File(dataDir, ANALYSIS_DIR);
        File reportsDir = new File(dataDir, REPORTS_DIR);

        if (!dataDir.exists() && !dataDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create ML data directory!");
        }

        if (!trainingDir.exists() && !trainingDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create ML training data directory!");
        }

        if (!analysisDir.exists() && !analysisDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create ML analysis data directory!");
        }

        if (!reportsDir.exists() && !reportsDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create ML reports directory!");
        }


        extractBundledTrainingData();
    }

    /**
     * Save player data to a file
     *
     * @param data The player mining data to save
     */
    public static void savePlayerData(PlayerMiningData data) {
        if (plugin == null) {
            Bukkit.getLogger().severe("MLDataManager not initialized!");
            return;
        }


        if (data.getFeatures().isEmpty()) {
            data.calculateDerivedFeatures();
        }


        String dirName = ANALYSIS_DIR;
        File saveDir = new File(dataDir, dirName);


        String timestamp = DATE_FORMAT.format(new Date());


        String label = "analysis";
        String filename = String.format("%s_%s_%s.json", data.getPlayerName(), label, timestamp);

        File outputFile = new File(saveDir, filename);

        savePlayerDataToFile(data, outputFile, label, timestamp);
    }

    /**
     * Save player data specifically for training purposes.
     * This ensures it goes into the training directory regardless of label.
     *
     * @param data The player mining data to save for training
     */
    public static void saveTrainingData(PlayerMiningData data) {
        if (plugin == null) {
            Bukkit.getLogger().severe("MLDataManager not initialized!");
            return;
        }


        if (data.getFeatures().isEmpty()) {
            data.calculateDerivedFeatures();
        }


        File saveDir = new File(dataDir, TRAINING_DIR);


        String timestamp = DATE_FORMAT.format(new Date());
        String label = data.isLabeledAsCheater() ? "cheater" : "normal";
        String filename = String.format("%s_%s_%s.json", data.getPlayerName(), label, timestamp);

        File outputFile = new File(saveDir, filename);

        savePlayerDataToFile(data, outputFile, label, timestamp);

        plugin.getLogger().info("Saved ML training data for " + data.getPlayerName() + " labeled as " + label + " to " + outputFile.getName());
    }

    /**
     * Save detailed detection report to a file in the reports directory
     *
     * @param playerName The name of the player
     * @param result     The detection result with detailed analysis
     * @param playerData Optional player mining data to include with the report
     * @return The path to the saved report file
     */
    public static String saveDetectionReport(String playerName, ReasoningMLModel.DetectionResult result, PlayerMiningData playerData) {
        if (plugin == null) {
            Bukkit.getLogger().severe("MLDataManager not initialized!");
            return null;
        }


        File saveDir = new File(dataDir, REPORTS_DIR);


        String timestamp = DATE_FORMAT.format(new Date());
        String suspicionLevel = getSuspicionLevelTag(result.getProbability());
        String filename = String.format("%s_%s_%s.json", playerName, suspicionLevel, timestamp);

        File outputFile = new File(saveDir, filename);

        try (FileWriter writer = new FileWriter(outputFile)) {

            StringBuilder json = new StringBuilder("{\n");


            json.append("  \"playerName\": \"").append(playerName).append("\",\n");
            json.append("  \"timestamp\": \"").append(timestamp).append("\",\n");
            json.append("  \"suspicionScore\": ").append(result.getProbability()).append(",\n");
            json.append("  \"suspicionLevel\": \"").append(suspicionLevel).append("\",\n");
            json.append("  \"conclusion\": \"").append(escapeJsonString(result.getConclusion())).append("\",\n");


            json.append("  \"reasoningSteps\": [\n");
            List<String> steps = result.getReasoningSteps();
            for (int i = 0; i < steps.size(); i++) {
                json.append("    \"").append(escapeJsonString(steps.get(i))).append("\"");
                if (i < steps.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]");


            if (playerData != null) {
                json.append(",\n  \"playerData\": {\n");
                json.append("    \"sessionDuration\": ").append(playerData.getTotalMiningTimeMs() / 1000.0).append(",\n");


                json.append("    \"features\": {\n");


                Map<String, List<Map.Entry<String, Double>>> featureCategories = new LinkedHashMap<>();


                featureCategories.put("basic_metrics", new ArrayList<>());
                featureCategories.put("ore_counts", new ArrayList<>());
                featureCategories.put("ore_rates", new ArrayList<>());
                featureCategories.put("head_movement", new ArrayList<>());
                featureCategories.put("concurrent_actions", new ArrayList<>());
                featureCategories.put("other", new ArrayList<>());


                for (Map.Entry<String, Double> feature : playerData.getFeatures().entrySet()) {
                    String key = feature.getKey();

                    if (key.startsWith("total_") || key.equals("sessionDuration")) {
                        featureCategories.get("basic_metrics").add(feature);
                    } else if (key.startsWith("ore_count_")) {
                        featureCategories.get("ore_counts").add(feature);
                    } else if (key.startsWith("ore_rate_")) {
                        featureCategories.get("ore_rates").add(feature);
                    } else if (key.startsWith("head_")) {
                        featureCategories.get("head_movement").add(feature);
                    } else if (key.startsWith("concurrent_")) {
                        featureCategories.get("concurrent_actions").add(feature);
                    } else {
                        featureCategories.get("other").add(feature);
                    }
                }


                boolean firstCategory = true;
                for (Map.Entry<String, List<Map.Entry<String, Double>>> category : featureCategories.entrySet()) {
                    if (category.getValue().isEmpty()) {
                        continue;
                    }

                    if (!firstCategory) {
                        json.append(",\n");
                    }
                    firstCategory = false;

                    json.append("      // ").append(category.getKey().replace("_", " ")).append("\n");


                    category.getValue().sort(Comparator.comparing(Map.Entry::getKey));

                    boolean firstFeature = true;
                    for (Map.Entry<String, Double> feature : category.getValue()) {
                        if (!firstFeature) {
                            json.append(",\n");
                        }
                        firstFeature = false;

                        json.append("      \"").append(feature.getKey()).append("\": ").append(feature.getValue());
                    }
                }

                json.append("\n    }\n");
                json.append("  }");
            }

            json.append("\n}");

            writer.write(json.toString());

            plugin.getLogger().info("Saved detection report for " + playerName + " to " + outputFile.getName());
            return outputFile.getPath();

        } catch (IOException e) {
            plugin.getLogger().severe("Error saving detection report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a text tag representing the suspicion level
     */
    private static String getSuspicionLevelTag(double suspicionScore) {
        if (suspicionScore >= 0.8) return "high_risk";
        if (suspicionScore >= 0.6) return "suspicious";
        if (suspicionScore >= 0.4) return "medium_risk";
        if (suspicionScore >= 0.2) return "low_risk";
        return "normal";
    }

    /**
     * Escape special characters in strings for JSON output
     */
    private static String escapeJsonString(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\b", "\\b").replace("\f", "\\f").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /**
     * Internal method to save player data to a file with organized feature output
     */
    private static void savePlayerDataToFile(PlayerMiningData data, File outputFile, String label, String timestamp) {
        try (FileWriter writer = new FileWriter(outputFile)) {

            StringBuilder json = new StringBuilder("{\n");


            json.append("  \"playerName\": \"").append(data.getPlayerName()).append("\",\n");
            json.append("  \"label\": \"").append(label).append("\",\n");
            json.append("  \"timestamp\": \"").append(timestamp).append("\",\n");
            json.append("  \"sessionDuration\": ").append(data.getTotalMiningTimeMs() / 1000.0).append(",\n");


            json.append("  \"features\": {\n");


            Map<String, List<Map.Entry<String, Double>>> featureCategories = new LinkedHashMap<>();


            featureCategories.put("basic_metrics", new ArrayList<>());
            featureCategories.put("ore_counts", new ArrayList<>());
            featureCategories.put("ore_rates", new ArrayList<>());
            featureCategories.put("head_movement", new ArrayList<>());
            featureCategories.put("concurrent_actions", new ArrayList<>());
            featureCategories.put("other", new ArrayList<>());


            for (Map.Entry<String, Double> feature : data.getFeatures().entrySet()) {
                String key = feature.getKey();

                if (key.startsWith("total_") || key.equals("sessionDuration")) {
                    featureCategories.get("basic_metrics").add(feature);
                } else if (key.startsWith("ore_count_")) {
                    featureCategories.get("ore_counts").add(feature);
                } else if (key.startsWith("ore_rate_")) {
                    featureCategories.get("ore_rates").add(feature);
                } else if (key.startsWith("head_")) {
                    featureCategories.get("head_movement").add(feature);
                } else if (key.startsWith("concurrent_")) {
                    featureCategories.get("concurrent_actions").add(feature);
                } else {
                    featureCategories.get("other").add(feature);
                }
            }


            boolean firstCategory = true;
            for (Map.Entry<String, List<Map.Entry<String, Double>>> category : featureCategories.entrySet()) {
                if (category.getValue().isEmpty()) {
                    continue;
                }

                if (!firstCategory) {
                    json.append(",\n");
                }
                firstCategory = false;

                json.append("    // ").append(category.getKey().replace("_", " ")).append("\n");


                category.getValue().sort(Comparator.comparing(Map.Entry::getKey));

                boolean firstFeature = true;
                for (Map.Entry<String, Double> feature : category.getValue()) {
                    if (!firstFeature) {
                        json.append(",\n");
                    }
                    firstFeature = false;

                    json.append("    \"").append(feature.getKey()).append("\": ").append(feature.getValue());
                }
            }

            json.append("\n  }\n");
            json.append("}");

            writer.write(json.toString());

            plugin.getLogger().info("Saved ML data for " + data.getPlayerName() + " to " + outputFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving ML data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load all training data for the ML model
     *
     * @return A list of feature vectors and their labels
     */
    public static MLTrainingData loadTrainingData() {
        List<Map<String, Double>> normalFeatures = new ArrayList<>();
        List<Map<String, Double>> cheaterFeatures = new ArrayList<>();

        File trainingDir = new File(dataDir, TRAINING_DIR);
        if (!trainingDir.exists()) {
            plugin.getLogger().warning("Training data directory does not exist!");
            return new MLTrainingData(normalFeatures, cheaterFeatures);
        }


        File[] files = trainingDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            plugin.getLogger().warning("No training data files found!");
            return new MLTrainingData(normalFeatures, cheaterFeatures);
        }

        for (File file : files) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(file.getPath())));


                Map<String, Double> features = new HashMap<>();
                String label = content.contains("\"label\": \"cheater\"") ? "cheater" : "normal";


                int featuresStart = content.indexOf("\"features\": {") + 13;
                int featuresEnd = content.lastIndexOf("}");
                String featuresStr = content.substring(featuresStart, featuresEnd);


                String[] lines = featuresStr.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("//")) continue;


                    if (line.endsWith(",")) {
                        line = line.substring(0, line.length() - 1);
                    }

                    int keyEnd = line.indexOf(":");
                    if (keyEnd > 0) {
                        String key = line.substring(0, keyEnd).trim().replace("\"", "");
                        String valueStr = line.substring(keyEnd + 1).trim();
                        try {
                            double value = Double.parseDouble(valueStr);
                            features.put(key, value);
                        } catch (NumberFormatException ignored) {

                        }
                    }
                }


                if ("cheater".equals(label)) {
                    cheaterFeatures.add(features);
                } else {
                    normalFeatures.add(features);
                }

            } catch (IOException e) {
                plugin.getLogger().warning("Error reading training file " + file.getName() + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + normalFeatures.size() + " normal player records and " + cheaterFeatures.size() + " cheater records for training");

        return new MLTrainingData(normalFeatures, cheaterFeatures);
    }

    /**
     * Get a list of saved reports for a specific player
     *
     * @param playerName The player name to search for, or null for all reports
     * @return A list of report file paths
     */
    public static List<String> getPlayerReports(String playerName) {
        List<String> reports = new ArrayList<>();

        File reportsDir = new File(dataDir, REPORTS_DIR);
        if (!reportsDir.exists()) {
            return reports;
        }


        File[] files = reportsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return reports;
        }


        for (File file : files) {
            if (playerName == null || file.getName().startsWith(playerName + "_")) {
                reports.add(file.getPath());
            }
        }


        reports.sort((path1, path2) -> {
            File file1 = new File(path1);
            File file2 = new File(path2);

            return Long.compare(file2.lastModified(), file1.lastModified());
        });

        return reports;
    }

    /**
     * Extracts bundled training data from the plugin JAR if no training data exists
     */
    public static void extractBundledTrainingData() {

        File trainingDir = new File(dataDir, TRAINING_DIR);
        if (!trainingDir.exists()) {
            trainingDir.mkdirs();
        }


        File[] existingFiles = trainingDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (existingFiles != null && existingFiles.length > 0) {
            plugin.getLogger().info("Training data already exists, skipping extraction");
            return;
        }

        plugin.getLogger().info("No existing training data found, extracting bundled data...");

        try {

            String resourcePath = "ml-data/training";


            java.net.URL url = plugin.getClass().getClassLoader().getResource(resourcePath);
            if (url == null) {
                plugin.getLogger().warning("No bundled training data found in JAR");
                return;
            }


            if (url.getProtocol().equals("jar")) {
                String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
                java.util.jar.JarFile jar = new java.util.jar.JarFile(java.net.URLDecoder.decode(jarPath, StandardCharsets.UTF_8));

                java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                String pathToMatch = resourcePath + "/";
                int extractedCount = 0;


                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (name.startsWith(pathToMatch) && !entry.isDirectory() && name.endsWith(".json")) {
                        String filename = name.substring(pathToMatch.length());
                        java.io.InputStream is = plugin.getClass().getClassLoader().getResourceAsStream(name);
                        if (is != null) {

                            File outFile = new File(trainingDir, filename);
                            java.nio.file.Files.copy(is, outFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            extractedCount++;
                            is.close();
                        }
                    }
                }

                jar.close();
                plugin.getLogger().info("Extracted " + extractedCount + " training files from plugin JAR");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error extracting bundled training data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Class to hold training data
     */
    public static class MLTrainingData {
        private final List<Map<String, Double>> normalFeatures;
        private final List<Map<String, Double>> cheaterFeatures;

        public MLTrainingData(List<Map<String, Double>> normalFeatures, List<Map<String, Double>> cheaterFeatures) {
            this.normalFeatures = normalFeatures;
            this.cheaterFeatures = cheaterFeatures;
        }

        public List<Map<String, Double>> getNormalFeatures() {
            return normalFeatures;
        }

        public List<Map<String, Double>> getCheaterFeatures() {
            return cheaterFeatures;
        }

        public boolean hasEnoughData() {

            return normalFeatures.size() >= 3 && cheaterFeatures.size() >= 3;
        }
    }
}