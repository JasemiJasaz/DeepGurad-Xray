package org.myplugin.deepGuardXray.ml;

import org.myplugin.deepGuardXray.deepGuardXray;

import java.util.*;

/**
 * Improved Reasoning-based model for X-ray cheat detection
 * Now with mining style recognition and contextual analysis
 */
public class ReasoningMLModel {
    private final deepGuardXray plugin;
    private final MLConfig config;


    private final Map<String, ReferenceStats> referenceStats = new HashMap<>();


    private final double suspicionThreshold = 0.7;
    private boolean trained = false;


    public ReasoningMLModel(deepGuardXray plugin, MLConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Train the model using collected data
     *
     * @return True if training was successful
     */
    public boolean train() {
        MLDataManager.MLTrainingData trainingData = MLDataManager.loadTrainingData();

        if (!trainingData.hasEnoughData()) {
            plugin.getLogger().warning("Not enough training data to train the model");
            return false;
        }

        List<Map<String, Double>> normalFeatures = trainingData.getNormalFeatures();
        List<Map<String, Double>> cheaterFeatures = trainingData.getCheaterFeatures();


        Set<String> allFeatures = new HashSet<>();
        normalFeatures.forEach(map -> allFeatures.addAll(map.keySet()));
        cheaterFeatures.forEach(map -> allFeatures.addAll(map.keySet()));


        for (String feature : allFeatures) {
            calculateReferenceStats(feature, normalFeatures, cheaterFeatures);
        }

        plugin.getLogger().info("Trained reference statistics for " + referenceStats.size() + " features");
        trained = true;
        return true;
    }

    /**
     * Calculate reference statistics for a specific feature
     */
    private void calculateReferenceStats(String feature, List<Map<String, Double>> normalFeatures, List<Map<String, Double>> cheaterFeatures) {

        double normalSum = 0;
        int normalCount = 0;

        for (Map<String, Double> player : normalFeatures) {
            if (player.containsKey(feature)) {
                normalSum += player.get(feature);
                normalCount++;
            }
        }

        double normalMean = normalCount > 0 ? normalSum / normalCount : 0;


        double normalSumSqDiff = 0;
        for (Map<String, Double> player : normalFeatures) {
            if (player.containsKey(feature)) {
                double diff = player.get(feature) - normalMean;
                normalSumSqDiff += diff * diff;
            }
        }
        double normalStdDev = normalCount > 1 ? Math.sqrt(normalSumSqDiff / (normalCount - 1)) : 1.0;
        normalStdDev = Math.max(0.00001, normalStdDev);


        double cheaterSum = 0;
        int cheaterCount = 0;

        for (Map<String, Double> player : cheaterFeatures) {
            if (player.containsKey(feature)) {
                cheaterSum += player.get(feature);
                cheaterCount++;
            }
        }

        double cheaterMean = cheaterCount > 0 ? cheaterSum / cheaterCount : 0;


        double cheaterSumSqDiff = 0;
        for (Map<String, Double> player : cheaterFeatures) {
            if (player.containsKey(feature)) {
                double diff = player.get(feature) - cheaterMean;
                cheaterSumSqDiff += diff * diff;
            }
        }
        double cheaterStdDev = cheaterCount > 1 ? Math.sqrt(cheaterSumSqDiff / (cheaterCount - 1)) : 1.0;
        cheaterStdDev = Math.max(0.00001, cheaterStdDev);


        referenceStats.put(feature, new ReferenceStats(normalMean, normalStdDev, cheaterMean, cheaterStdDev));
    }

    /**
     * Predict if a player is cheating using reasoning with mining style recognition
     *
     * @param features The player's feature map
     * @return DetectionResult containing probability and detailed reasoning
     */
    public DetectionResult predict(Map<String, Double> features) {
        if (!trained || referenceStats.isEmpty()) {
            return new DetectionResult(0.5, "Model not trained", new ArrayList<>());
        }


        List<String> reasoningSteps = new ArrayList<>();


        MiningStyle miningStyle = identifyMiningStyle(features);
        reasoningSteps.add("Step 0: Mining Style Identification - " + describeMiningStyle(miningStyle, features));


        AnalysisResult blocksResult = analyzeBlocksBroken(features, miningStyle);
        reasoningSteps.add("Step 1: " + blocksResult.reason);


        AnalysisResult oreMiningResult = analyzeOreMiningRatio(features, miningStyle);
        reasoningSteps.add("Step 2: " + oreMiningResult.reason);


        AnalysisResult focusedMiningResult = analyzeFocusedMining(features, miningStyle);
        reasoningSteps.add("Step 3: " + focusedMiningResult.reason);


        AnalysisResult oreRateResult = analyzeOreFindingRates(features, miningStyle);
        reasoningSteps.add("Step 4: " + oreRateResult.reason);


        AnalysisResult patternResult = analyzeMiningPatterns(features, miningStyle);
        reasoningSteps.add("Step 5: " + patternResult.reason);


        AnalysisResult behaviorResult = analyzeBehaviorPatterns(features, miningStyle);
        reasoningSteps.add("Step 6: " + behaviorResult.reason);


        double[] weights = getWeightsByMiningStyle(miningStyle);
        double[] scores = {blocksResult.suspicionScore, oreMiningResult.suspicionScore, focusedMiningResult.suspicionScore, oreRateResult.suspicionScore, patternResult.suspicionScore, behaviorResult.suspicionScore};

        double finalScore = 0;
        for (int i = 0; i < weights.length; i++) {
            finalScore += weights[i] * scores[i];
        }


        StringBuilder conclusion = new StringBuilder();

        if (finalScore > suspicionThreshold) {
            conclusion.append("Player behavior indicates likely X-ray cheating. ");
        } else if (finalScore > 0.65) {
            conclusion.append("Player behavior appears suspicious. ");
        } else {
            conclusion.append("Player behavior appears normal. ");
            if (miningStyle == MiningStyle.BRANCH_MINER) {
                conclusion.append("Detected as a legitimate branch miner. ");
            } else if (miningStyle == MiningStyle.CAVE_MINER) {
                conclusion.append("Detected as a legitimate cave explorer. ");
            }
        }


        double maxScore = 0;
        int maxIndex = 0;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxIndex = i;
            }
        }

        if (maxScore > 0.7) {
            conclusion.append("Most suspicious aspect: ");
            switch (maxIndex) {
                case 0:
                    conclusion.append("block breaking rate");
                    break;
                case 1:
                    conclusion.append("ore mining ratio");
                    break;
                case 2:
                    conclusion.append("focused ore targeting");
                    break;
                case 3:
                    conclusion.append("ore finding rates");
                    break;
                case 4:
                    conclusion.append("mining patterns");
                    break;
                case 5:
                    conclusion.append("behavior patterns");
                    break;
            }
        }

        return new DetectionResult(finalScore, conclusion.toString(), reasoningSteps);
    }

    /**
     * Get appropriate weights for each analysis step based on mining style
     */
    private double[] getWeightsByMiningStyle(MiningStyle style) {
        switch (style) {
            case BRANCH_MINER:


                return new double[]{0.05, 0.2, 0.15, 0.25, 0.25, 0.1};
            case CAVE_MINER:


                return new double[]{0.1, 0.15, 0.25, 0.25, 0.15, 0.1};
            case XRAY_CHEATER:
            case UNKNOWN:
            default:

                return new double[]{0.15, 0.2, 0.15, 0.2, 0.15, 0.15};
        }
    }

    /**
     * NEW METHOD: Identify the mining style based on key indicators
     * This is crucial for contextual analysis
     */
    private MiningStyle identifyMiningStyle(Map<String, Double> features) {

        boolean likelyBranchMiner = isBranchMiner(features);


        boolean likelyCaveMiner = isCaveMiner(features);


        boolean likelyXray = false;


        if (features.containsKey("total_ores_mined") && features.containsKey("total_blocks_broken")) {
            double oreRatio = features.get("total_ores_mined") / features.get("total_blocks_broken") * 100;


            String mostMinedOre = getMostMinedOre(features);
            boolean focusedOnValuableOre = mostMinedOre != null && (mostMinedOre.contains("diamond") || mostMinedOre.contains("ancient_debris"));


            double suspiciousDiamondRate = 0;
            for (String key : features.keySet()) {
                if (key.startsWith("ore_rate_") && key.contains("diamond")) {
                    ReferenceStats stats = referenceStats.getOrDefault(key, null);
                    if (stats != null) {
                        suspiciousDiamondRate = Math.max(suspiciousDiamondRate, stats.calculateSuspicionScore(features.get(key)));
                    }
                }
            }


            likelyXray = oreRatio > 15 && focusedOnValuableOre && suspiciousDiamondRate > 0.7;
        }


        if (likelyXray) {
            return MiningStyle.XRAY_CHEATER;
        } else if (likelyBranchMiner) {
            return MiningStyle.BRANCH_MINER;
        } else if (likelyCaveMiner) {
            return MiningStyle.CAVE_MINER;
        } else {
            return MiningStyle.UNKNOWN;
        }
    }

    /**
     * Check if player matches branch mining pattern
     */
    private boolean isBranchMiner(Map<String, Double> features) {

        boolean consistentYLevel = features.getOrDefault("percent_time_at_most_common_y", 0.0) > 80;


        boolean lowYVariance = features.getOrDefault("y_level_variance", 999.0) < 20;


        boolean fewYChanges = features.getOrDefault("y_level_changes_per_minute", 999.0) < 5;


        boolean smallYRange = features.getOrDefault("y_level_range", 999.0) < 10;


        boolean manyBlocksBroken = false;
        if (features.containsKey("total_blocks_broken")) {
            double blocksBroken = features.get("total_blocks_broken");
            double sessionDuration = features.getOrDefault("sessionDuration", 600.0);
            double blocksPerMinute = blocksBroken / (sessionDuration / 60);
            manyBlocksBroken = blocksPerMinute > 40;
        }


        return consistentYLevel && (lowYVariance || smallYRange) && fewYChanges;
    }

    /**
     * Check if player matches cave mining pattern
     */
    private boolean isCaveMiner(Map<String, Double> features) {

        boolean highYVariance = features.getOrDefault("y_level_variance", 0.0) > 50;


        boolean manyYChanges = features.getOrDefault("y_level_changes_per_minute", 0.0) > 10;


        boolean variedYLevels = features.getOrDefault("percent_time_at_most_common_y", 100.0) < 50;


        boolean largeYRange = features.getOrDefault("y_level_range", 0.0) > 20;


        return (highYVariance || largeYRange) && (manyYChanges || variedYLevels);
    }

    /**
     * Get description of identified mining style
     */
    private String describeMiningStyle(MiningStyle style, Map<String, Double> features) {
        StringBuilder description = new StringBuilder();

        switch (style) {
            case BRANCH_MINER:
                description.append("Player identified as a branch miner. ");
                if (features.containsKey("most_common_y_level")) {
                    description.append("Mining primarily at Y=").append((int) features.get("most_common_y_level").doubleValue()).append(". ");
                }
                description.append("Shows consistent Y-level (").append(String.format("%.1f", features.getOrDefault("percent_time_at_most_common_y", 0.0))).append("% at one level), few vertical movements, and systematic horizontal tunneling.");
                break;

            case CAVE_MINER:
                description.append("Player identified as a cave explorer. ");
                description.append("Shows variable Y-levels with frequent elevation changes (").append(String.format("%.1f", features.getOrDefault("y_level_changes_per_minute", 0.0))).append(" per minute) and explores a vertical range of approximately ").append((int) features.getOrDefault("y_level_range", 0.0).doubleValue()).append(" blocks.");
                break;

            case XRAY_CHEATER:
                description.append("Player shows strong indicators of X-ray cheating. ");
                String mostMinedOre = getMostMinedOre(features);
                if (mostMinedOre != null) {
                    description.append("Focused primarily on ").append(mostMinedOre).append(". ");
                }
                description.append("Movement patterns and ore discovery rates are highly suspicious.");
                break;

            case UNKNOWN:
            default:
                description.append("Mining style could not be clearly identified. ");
                description.append("Will analyze individual behaviors to determine legitimacy.");
                break;
        }

        return description.toString();
    }

    /**
     * Helper method to find the most mined ore type
     */
    private String getMostMinedOre(Map<String, Double> features) {
        String mostMinedOre = null;
        double mostMinedAmount = 0;

        for (Map.Entry<String, Double> entry : features.entrySet()) {
            if (entry.getKey().startsWith("ore_count_")) {
                String oreType = entry.getKey().substring("ore_count_".length());
                double amount = entry.getValue();

                if (amount > mostMinedAmount) {
                    mostMinedAmount = amount;
                    mostMinedOre = oreType;
                }
            }
        }

        return mostMinedOre;
    }

    /**
     * Step 1: Analyze total blocks broken with mining style context
     */
    private AnalysisResult analyzeBlocksBroken(Map<String, Double> features, MiningStyle style) {
        if (!features.containsKey("total_blocks_broken")) {
            return new AnalysisResult(0.5, "No data on blocks broken");
        }

        double blocksBroken = features.get("total_blocks_broken");
        double suspicionScore = 0.0;


        ReferenceStats stats = referenceStats.getOrDefault("total_blocks_broken", new ReferenceStats(800, 200, 1200, 300));


        suspicionScore = stats.calculateSuspicionScore(blocksBroken);


        if (style == MiningStyle.BRANCH_MINER) {

            suspicionScore = Math.max(0, suspicionScore - 0.4);

            if (blocksBroken > stats.normalMean && blocksBroken < stats.normalMean * 2) {
                suspicionScore = Math.max(0, suspicionScore - 0.2);
            }
        }


        StringBuilder reason = new StringBuilder();
        reason.append("Player broke ").append((int) blocksBroken).append(" blocks, which ");

        if (style == MiningStyle.BRANCH_MINER) {
            reason.append("is expected for branch mining but ");
        }

        reason.append(stats.getComparisonDescription(blocksBroken)).append(". ");


        double sessionDuration = features.getOrDefault("sessionDuration", 600.0);
        double blocksPerMinute = blocksBroken / (sessionDuration / 60);
        reason.append("Mining rate: ").append(String.format("%.1f", blocksPerMinute)).append(" blocks per minute.");

        if (style == MiningStyle.BRANCH_MINER && suspicionScore < 0.3) {
            reason.append(" This rate is consistent with efficient branch mining.");
        }

        return new AnalysisResult(suspicionScore, reason.toString());
    }

    /**
     * Step 2: Analyze ore mining ratio with mining style context
     */
    private AnalysisResult analyzeOreMiningRatio(Map<String, Double> features, MiningStyle style) {
        if (!features.containsKey("total_blocks_broken") || !features.containsKey("total_ores_mined")) {
            return new AnalysisResult(0.5, "Insufficient data for ore mining ratio analysis");
        }

        double totalBlocks = features.get("total_blocks_broken");
        double totalOres = features.get("total_ores_mined");

        if (totalBlocks == 0) {
            return new AnalysisResult(0.5, "No blocks broken, cannot calculate ore ratio");
        }

        double oreRatio = totalOres / totalBlocks * 100;


        ReferenceStats oreRatioStats = new ReferenceStats(5.0, 2.0, 20.0, 10.0);

        if (referenceStats.containsKey("ore_ratio")) {
            oreRatioStats = referenceStats.get("ore_ratio");
        } else {

            double normalRatio = 0.0, cheaterRatio = 0.0;
            int normalCount = 0, cheaterCount = 0;


            if (referenceStats.containsKey("total_blocks_broken") && referenceStats.containsKey("total_ores_mined")) {

                ReferenceStats blocksStats = referenceStats.get("total_blocks_broken");
                ReferenceStats oresStats = referenceStats.get("total_ores_mined");

                normalRatio = oresStats.normalMean / blocksStats.normalMean * 100;
                cheaterRatio = oresStats.cheaterMean / blocksStats.cheaterMean * 100;


                oreRatioStats = new ReferenceStats(normalRatio, normalRatio * 0.4, cheaterRatio, cheaterRatio * 0.5);
            }
        }


        double suspicionScore = oreRatioStats.calculateSuspicionScore(oreRatio);


        if (style == MiningStyle.BRANCH_MINER) {


            if (oreRatio < 8) {
                suspicionScore = Math.max(0, suspicionScore - 0.3);
            }
        } else if (style == MiningStyle.CAVE_MINER) {

            if (oreRatio < 12) {
                suspicionScore = Math.max(0, suspicionScore - 0.3);
            }
        }


        StringBuilder reason = new StringBuilder();
        reason.append(String.format("%.1f%%", oreRatio)).append(" of blocks broken were ores (").append((int) totalOres).append(" ores out of ").append((int) totalBlocks).append(" blocks), which ");

        if (style == MiningStyle.BRANCH_MINER) {
            reason.append("for branch mining ");
        } else if (style == MiningStyle.CAVE_MINER) {
            reason.append("for cave exploration ");
        }

        reason.append(oreRatioStats.getComparisonDescription(oreRatio)).append(".");

        if (style == MiningStyle.CAVE_MINER && oreRatio > oreRatioStats.normalMean) {
            reason.append(" Note: Cave miners typically find more exposed ores than branch miners.");
        }

        return new AnalysisResult(suspicionScore, reason.toString());
    }

    /**
     * Step 3: Analyze focused mining (specific ore targeting) with mining style context
     */
    private AnalysisResult analyzeFocusedMining(Map<String, Double> features, MiningStyle style) {
        if (!features.containsKey("total_ores_mined")) {
            return new AnalysisResult(0.5, "No data on ores mined");
        }

        double totalOres = features.get("total_ores_mined");
        if (totalOres == 0) {
            return new AnalysisResult(0, "No ores mined");
        }


        String mostMinedOre = null;
        double mostMinedAmount = 0;

        Map<String, Double> oreCounts = new HashMap<>();
        for (Map.Entry<String, Double> entry : features.entrySet()) {
            if (entry.getKey().startsWith("ore_count_")) {
                String oreType = entry.getKey().substring("ore_count_".length());
                double amount = entry.getValue();
                oreCounts.put(oreType, amount);

                if (amount > mostMinedAmount) {
                    mostMinedAmount = amount;
                    mostMinedOre = oreType;
                }
            }
        }

        if (mostMinedOre == null) {
            return new AnalysisResult(0.5, "No specific ore type data found");
        }


        double focusRatio = mostMinedAmount / totalOres * 100;


        ReferenceStats focusStats = new ReferenceStats(60.0, 15.0, 90.0, 10.0);


        double suspicionScore = focusStats.calculateSuspicionScore(focusRatio);


        boolean isValuableOre = false;
        if (mostMinedOre.contains("diamond") || mostMinedOre.contains("ancient_debris") || mostMinedOre.contains("emerald")) {
            isValuableOre = true;


            if (style != MiningStyle.BRANCH_MINER) {
                suspicionScore = Math.max(suspicionScore, suspicionScore * 1.3);
            }
        }


        if (style == MiningStyle.BRANCH_MINER && focusRatio > 70 && focusRatio < 95) {


            if (features.containsKey("most_common_y_level")) {
                double yLevel = features.get("most_common_y_level");
                if (yLevel >= -59 && yLevel <= -50) {
                    suspicionScore = Math.max(0, suspicionScore - 0.4);
                }
            }
        }


        StringBuilder reason = new StringBuilder();
        reason.append("Player's mining is ").append(String.format("%.1f%%", focusRatio)).append(" focused on ").append(mostMinedOre).append(" (").append((int) mostMinedAmount).append(" out of ").append((int) totalOres).append(" total ores), which ");

        reason.append(focusStats.getComparisonDescription(focusRatio)).append(".");

        if (style == MiningStyle.BRANCH_MINER && isValuableOre) {

            reason.append(" For branch miners at diamond level, high focus on diamonds is expected. ");
            if (features.containsKey("most_common_y_level")) {
                double yLevel = features.get("most_common_y_level");
                reason.append("Player is mining at Y=").append((int) yLevel).append(". ");

                if (yLevel >= -59 && yLevel <= -50) {
                    reason.append("This is optimal diamond mining level.");
                }
            }
        } else if (isValuableOre && focusRatio > 80 && style != MiningStyle.BRANCH_MINER) {
            reason.append(" Highly suspicious focus on valuable ore type.");
        }

        return new AnalysisResult(suspicionScore, reason.toString());
    }

    /**
     * Step 4: Analyze ore finding rates with mining style context
     */
    private AnalysisResult analyzeOreFindingRates(Map<String, Double> features, MiningStyle style) {

        Map<String, Double> oreRates = new HashMap<>();
        for (Map.Entry<String, Double> entry : features.entrySet()) {
            if (entry.getKey().startsWith("ore_rate_")) {
                oreRates.put(entry.getKey(), entry.getValue());
            }
        }

        if (oreRates.isEmpty()) {
            return new AnalysisResult(0.5, "No ore rate data available");
        }


        double maxSuspicionScore = 0;
        String mostSuspiciousRate = null;
        StringBuilder reason = new StringBuilder("Analysis of ore finding rates: ");

        for (Map.Entry<String, Double> rateEntry : oreRates.entrySet()) {
            String rateKey = rateEntry.getKey();
            double rateValue = rateEntry.getValue();


            ReferenceStats rateStats = referenceStats.getOrDefault(rateKey, null);
            if (rateStats == null) {


                String[] parts = rateKey.split("_");
                if (parts.length >= 4) {
                    String oreType = parts[2];


                    if (oreType.contains("diamond")) {
                        rateStats = new ReferenceStats(3.0, 2.0, 15.0, 7.0);
                    } else if (oreType.contains("ancient_debris")) {
                        rateStats = new ReferenceStats(1.0, 0.5, 5.0, 2.0);
                    } else {
                        rateStats = new ReferenceStats(8.0, 4.0, 20.0, 10.0);
                    }
                } else {

                    rateStats = new ReferenceStats(5.0, 3.0, 15.0, 7.0);
                }
            }


            double suspicionScore = rateStats.calculateSuspicionScore(rateValue);


            if (style == MiningStyle.BRANCH_MINER && rateKey.contains("diamond")) {


                if (rateValue > rateStats.normalMean && rateValue < rateStats.cheaterMean * 0.7) {
                    suspicionScore = Math.max(0, suspicionScore - 0.3);
                }
            } else if (style == MiningStyle.CAVE_MINER) {


                if (rateKey.contains("1min") && rateValue > rateStats.normalMean * 1.5) {

                    suspicionScore = Math.max(0, suspicionScore - 0.2);
                }
            }


            if (suspicionScore > maxSuspicionScore) {
                maxSuspicionScore = suspicionScore;
                mostSuspiciousRate = rateKey;
            }


            if (rateKey.contains("diamond") || rateKey.contains("ancient_debris")) {
                reason.append("\n- ").append(rateKey).append(": ").append(String.format("%.1f", rateValue)).append(" ores per timeframe, which ");

                if (style == MiningStyle.BRANCH_MINER && rateKey.contains("diamond")) {
                    reason.append("for branch mining ");
                } else if (style == MiningStyle.CAVE_MINER && rateKey.contains("1min")) {
                    reason.append("for cave exploration (short-term spike) ");
                }

                reason.append(rateStats.getComparisonDescription(rateValue));
            }
        }


        if (mostSuspiciousRate != null) {
            double mostSuspiciousValue = oreRates.get(mostSuspiciousRate);
            reason.append("\nMost notable: ").append(mostSuspiciousRate).append(" at ").append(String.format("%.1f", mostSuspiciousValue));

            if (maxSuspicionScore > 0.7) {
                reason.append(" is highly unusual compared to normal players");


                if (style == MiningStyle.BRANCH_MINER) {
                    reason.append(", even accounting for branch mining techniques");
                } else if (style == MiningStyle.CAVE_MINER) {
                    reason.append(", even accounting for cave exploration advantages");
                }
                reason.append(".");
            } else {
                reason.append(" appears within reasonable limits");


                if (style == MiningStyle.BRANCH_MINER) {
                    reason.append(" for branch mining");
                } else if (style == MiningStyle.CAVE_MINER) {
                    reason.append(" for cave exploration");
                }
                reason.append(".");
            }
        }

        return new AnalysisResult(maxSuspicionScore, reason.toString());
    }

    /**
     * Step 5: Analyze mining patterns (vein size, distances) with mining style context
     */
    private AnalysisResult analyzeMiningPatterns(Map<String, Double> features, MiningStyle style) {
        StringBuilder reason = new StringBuilder("Mining pattern analysis: ");
        double overallSuspicionScore = 0;
        int analyzedPatterns = 0;


        if (features.containsKey("avg_diamond_vein_size")) {
            double veinSize = features.get("avg_diamond_vein_size");
            ReferenceStats veinStats = referenceStats.getOrDefault("avg_diamond_vein_size", new ReferenceStats(2.8, 0.7, 4.5, 1.0));

            double veinSuspicion = veinStats.calculateSuspicionScore(veinSize);


            overallSuspicionScore += veinSuspicion;
            analyzedPatterns++;

            reason.append("\n- Average diamond vein size: ").append(String.format("%.2f", veinSize)).append(", which ").append(veinStats.getComparisonDescription(veinSize));
        }


        if (features.containsKey("avg_distance_between_diamonds")) {
            double distance = features.get("avg_distance_between_diamonds");
            ReferenceStats distanceStats = referenceStats.getOrDefault("avg_distance_between_diamonds", new ReferenceStats(15.0, 7.0, 3.5, 1.5));

            double distanceSuspicion = distanceStats.calculateSuspicionScore(distance);


            if (style == MiningStyle.BRANCH_MINER) {


                if (distance > 5.0 && distance < distanceStats.normalMean) {
                    distanceSuspicion = Math.max(0, distanceSuspicion - 0.3);
                }
            }

            overallSuspicionScore += distanceSuspicion;
            analyzedPatterns++;

            reason.append("\n- Average distance between diamonds: ").append(String.format("%.2f", distance)).append(" blocks, which ");

            if (style == MiningStyle.BRANCH_MINER && distance > 5.0) {
                reason.append("for branch mining ");
            }

            reason.append(distanceStats.getComparisonDescription(distance));


            if (distance < 5.0) {
                reason.append(" This is suspiciously close - diamonds are normally far apart.");
            }
        }


        if (features.containsKey("avg_y_change_after_ore")) {
            double yChangeAfterOre = features.get("avg_y_change_after_ore");
            ReferenceStats yChangeStats = referenceStats.getOrDefault("avg_y_change_after_ore", new ReferenceStats(8.0, 3.0, 4.0, 2.0));

            double yChangeSuspicion = yChangeStats.calculateSuspicionScore(yChangeAfterOre);


            if (style == MiningStyle.BRANCH_MINER) {


                yChangeSuspicion = Math.max(0, yChangeSuspicion - 0.5);
            }

            overallSuspicionScore += yChangeSuspicion;
            analyzedPatterns++;

            reason.append("\n- Average Y-level change after finding ore: ").append(String.format("%.2f", yChangeAfterOre)).append(" blocks, which ");

            if (style == MiningStyle.BRANCH_MINER) {
                reason.append("is expected for branch mining but ");
            }

            reason.append(yChangeStats.getComparisonDescription(yChangeAfterOre));

            if (style == MiningStyle.BRANCH_MINER && yChangeAfterOre < 1.0) {
                reason.append(" Branch miners typically maintain their Y-level while mining.");
            }
        }


        if (analyzedPatterns == 0) {
            return new AnalysisResult(0.5, "Insufficient data for mining pattern analysis");
        }


        double finalScore = overallSuspicionScore / analyzedPatterns;

        return new AnalysisResult(finalScore, reason.toString());
    }

    /**
     * Step 6: Analyze behavior patterns (y-level, movement) with mining style context
     * This analysis is heavily influenced by mining style
     */
    private AnalysisResult analyzeBehaviorPatterns(Map<String, Double> features, MiningStyle style) {
        StringBuilder reason = new StringBuilder("Behavior pattern analysis: ");
        double overallSuspicionScore = 0;
        int analyzedPatterns = 0;


        if (features.containsKey("y_level_variance")) {
            double variance = features.get("y_level_variance");
            ReferenceStats varianceStats = referenceStats.getOrDefault("y_level_variance", new ReferenceStats(120.0, 40.0, 60.0, 30.0));


            double varianceSuspicion = varianceStats.calculateSuspicionScore(variance);


            if (style == MiningStyle.BRANCH_MINER) {

                varianceSuspicion = 0.0;
            }

            overallSuspicionScore += varianceSuspicion;
            analyzedPatterns++;

            reason.append("\n- Y-level variance: ").append(String.format("%.2f", variance));

            if (style == MiningStyle.BRANCH_MINER) {
                reason.append(", which is normal for branch mining (they stay at one level)");
            } else {
                reason.append(", which ").append(varianceStats.getComparisonDescription(variance));
            }
        }


        if (features.containsKey("percent_time_at_most_common_y")) {
            double percentTimeAtY = features.get("percent_time_at_most_common_y");
            ReferenceStats timeAtYStats = referenceStats.getOrDefault("percent_time_at_most_common_y", new ReferenceStats(20.0, 10.0, 60.0, 20.0));


            double timeAtYSuspicion = timeAtYStats.calculateSuspicionScore(percentTimeAtY);


            if (style == MiningStyle.BRANCH_MINER) {

                timeAtYSuspicion = 0.0;
            }

            overallSuspicionScore += timeAtYSuspicion;
            analyzedPatterns++;

            reason.append("\n- Percent time at most common Y-level: ").append(String.format("%.2f%%", percentTimeAtY));

            if (style == MiningStyle.BRANCH_MINER) {
                reason.append(", which is normal for branch mining (they stay at one level)");
            } else {
                reason.append(", which ").append(timeAtYStats.getComparisonDescription(percentTimeAtY));
            }


            if (features.containsKey("most_common_y_level")) {
                double mostCommonY = features.get("most_common_y_level");
                reason.append(" (Most common Y-level: ").append((int) mostCommonY).append(")");


                if (mostCommonY >= -59 && mostCommonY <= -50) {
                    if (style != MiningStyle.BRANCH_MINER) {
                        reason.append(" - This is within the diamond-rich Y level range, which increases suspicion.");
                        overallSuspicionScore += 0.1;
                    } else {
                        reason.append(" - This is within optimal diamond mining range, which is expected for branch miners.");
                    }
                }
            }
        }


        if (features.containsKey("y_level_changes_per_minute")) {
            double changesPerMinute = features.get("y_level_changes_per_minute");
            ReferenceStats changesStats = referenceStats.getOrDefault("y_level_changes_per_minute", new ReferenceStats(15.0, 7.0, 35.0, 15.0));


            double changesSuspicion = changesStats.calculateSuspicionScore(changesPerMinute);


            if (style == MiningStyle.BRANCH_MINER) {

                if (changesPerMinute < 5.0) {
                    changesSuspicion = 0.0;
                }
            } else if (style == MiningStyle.CAVE_MINER) {

                if (changesPerMinute > changesStats.normalMean && changesPerMinute < changesStats.cheaterMean) {
                    changesSuspicion = Math.max(0, changesSuspicion - 0.3);
                }
            }

            overallSuspicionScore += changesSuspicion;
            analyzedPatterns++;

            reason.append("\n- Y-level changes per minute: ").append(String.format("%.2f", changesPerMinute));

            if (style == MiningStyle.BRANCH_MINER) {
                reason.append(", which is ");
                if (changesPerMinute < 5.0) {
                    reason.append("normal for branch mining (minimal vertical movement)");
                } else {
                    reason.append("unusual for branch mining (should have minimal vertical movement)");
                }
            } else if (style == MiningStyle.CAVE_MINER) {
                reason.append(", which is appropriate for cave exploration");
            } else {
                reason.append(", which ").append(changesStats.getComparisonDescription(changesPerMinute));
            }
        }


        if (analyzedPatterns == 0) {
            return new AnalysisResult(0.5, "Insufficient data for behavior pattern analysis");
        }


        double finalScore = overallSuspicionScore / analyzedPatterns;

        return new AnalysisResult(finalScore, reason.toString());
    }

    /**
     * Check if the model has been trained
     *
     * @return True if the model has been trained
     */
    public boolean isTrained() {
        return trained;
    }

    private enum MiningStyle {
        UNKNOWN, BRANCH_MINER, CAVE_MINER, XRAY_CHEATER
    }

    private static class AnalysisResult {
        double suspicionScore;
        String reason;

        public AnalysisResult(double suspicionScore, String reason) {
            this.suspicionScore = suspicionScore;
            this.reason = reason;
        }
    }

    private static class ReferenceStats {
        double normalMean;
        double normalStdDev;
        double cheaterMean;
        double cheaterStdDev;

        public ReferenceStats(double normalMean, double normalStdDev, double cheaterMean, double cheaterStdDev) {
            this.normalMean = normalMean;
            this.normalStdDev = normalStdDev;
            this.cheaterMean = cheaterMean;
            this.cheaterStdDev = cheaterStdDev;
        }


        public double calculateSuspicionScore(double value) {

            double normalZScore = Math.abs((value - normalMean) / normalStdDev);
            double cheaterZScore = Math.abs((value - cheaterMean) / cheaterStdDev);


            if (normalZScore > cheaterZScore) {


                return 1.0 / (1.0 + Math.exp(-(normalZScore - cheaterZScore)));
            } else {
                return 0.0;
            }
        }


        public String getComparisonDescription(double value) {
            double normalDist = Math.abs(value - normalMean);
            double cheaterDist = Math.abs(value - cheaterMean);

            if (normalDist < cheaterDist) {
                double ratio = cheaterDist / (normalDist > 0 ? normalDist : 1);
                if (ratio > 5) {
                    return "strongly resembles normal player behavior";
                } else if (ratio > 2) {
                    return "resembles normal player behavior";
                } else {
                    return "somewhat resembles normal player behavior";
                }
            } else {
                double ratio = normalDist / (cheaterDist > 0 ? cheaterDist : 1);
                if (ratio > 5) {
                    return "strongly resembles cheater behavior";
                } else if (ratio > 2) {
                    return "resembles cheater behavior";
                } else {
                    return "somewhat resembles cheater behavior";
                }
            }
        }
    }

    /**
     * Class to hold detection results with detailed reasoning
     */
    public static class DetectionResult {
        private final double probability;
        private final String conclusion;
        private final List<String> reasoningSteps;

        public DetectionResult(double probability, String conclusion, List<String> reasoningSteps) {
            this.probability = probability;
            this.conclusion = conclusion;
            this.reasoningSteps = reasoningSteps;
        }

        public double getProbability() {
            return probability;
        }

        public String getConclusion() {
            return conclusion;
        }

        public List<String> getReasoningSteps() {
            return reasoningSteps;
        }

        public String getDetailedReport() {
            StringBuilder report = new StringBuilder();
            report.append("X-Ray Detection Report\n");
            report.append("---------------------\n");
            report.append("Suspicion Score: ").append(String.format("%.2f", probability * 100)).append("%\n");
            report.append("Conclusion: ").append(conclusion).append("\n\n");
            report.append("Detailed Reasoning:\n");

            for (String step : reasoningSteps) {
                report.append(step).append("\n\n");
            }

            return report.toString();
        }
    }
}