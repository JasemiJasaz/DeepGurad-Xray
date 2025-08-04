package org.myplugin.deepGuardXray.ml;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Stores mining-related data for a player session
 */
public class PlayerMiningData {

    private final String playerName;
    private final long sessionStartTime;
    private final boolean labeledAsCheater;


    private final List<BlockBreakRecord> blockBreaks = new ArrayList<>();
    private final List<BlockBreakRecord> oreBreaks = new ArrayList<>();
    private final Set<Material> valuableOreMaterials = new HashSet<>();


    private final Set<Material> diamondOreMaterials = new HashSet<>();
    private final Set<Material> netheriteOreMaterials = new HashSet<>();
    private final List<BlockBreakRecord> diamondOreBreaks = new ArrayList<>();
    private final List<BlockBreakRecord> netheriteOreBreaks = new ArrayList<>();


    private final List<OreVein> diamondVeins = new ArrayList<>();
    private final List<OreVein> netheriteVeins = new ArrayList<>();
    private final int VEIN_PROXIMITY_THRESHOLD = 3;
    private final List<Double> diamondDistances = new ArrayList<>();
    private final List<Double> netheriteDistances = new ArrayList<>();
    private final List<Long> idleTimes = new ArrayList<>();
    private final Map<String, Double> features = new HashMap<>();
    private final List<YLevelRecord> yLevelRecords = new ArrayList<>();
    private final List<Double> yLevelChanges = new ArrayList<>();
    private final List<Long> timeAtSameYLevel = new ArrayList<>();
    private final double Y_LEVEL_CHANGE_THRESHOLD = 1.0;
    private final Map<Material, Integer> oreCounts = new HashMap<>();
    private final Map<Material, List<OreTimeWindow>> oreTimeWindows = new HashMap<>();
    private OreVein currentDiamondVein = null;
    private OreVein currentNetheriteVein = null;
    private Vector lastDiamondPosition = null;
    private Vector lastNetheritePosition = null;
    private Long lastBlockBreakTime = null;
    private int totalBlocksBroken = 0;
    private int totalOresMined = 0;
    private Double lastYLevel = null;
    private long lastYLevelChangeTime = 0;
    private int totalYLevelChanges = 0;
    private double totalYLevelChangeAmount = 0;

    public PlayerMiningData(String playerName, boolean labeledAsCheater) {
        this.playerName = playerName;
        this.sessionStartTime = System.currentTimeMillis();
        this.labeledAsCheater = labeledAsCheater;


        initializeValuableOreMaterials();


        initializeDiamondAndNetheriteOreMaterials();


        for (Material oreMaterial : valuableOreMaterials) {
            oreTimeWindows.put(oreMaterial, new ArrayList<>());
        }
    }

    private void initializeValuableOreMaterials() {
        valuableOreMaterials.add(Material.DIAMOND_ORE);
        valuableOreMaterials.add(Material.DEEPSLATE_DIAMOND_ORE);
        valuableOreMaterials.add(Material.ANCIENT_DEBRIS);
        valuableOreMaterials.add(Material.EMERALD_ORE);
        valuableOreMaterials.add(Material.DEEPSLATE_EMERALD_ORE);
        valuableOreMaterials.add(Material.GOLD_ORE);
        valuableOreMaterials.add(Material.DEEPSLATE_GOLD_ORE);
        valuableOreMaterials.add(Material.IRON_ORE);
        valuableOreMaterials.add(Material.DEEPSLATE_IRON_ORE);
        valuableOreMaterials.add(Material.LAPIS_ORE);
        valuableOreMaterials.add(Material.DEEPSLATE_LAPIS_ORE);
        valuableOreMaterials.add(Material.REDSTONE_ORE);
        valuableOreMaterials.add(Material.DEEPSLATE_REDSTONE_ORE);
        valuableOreMaterials.add(Material.COPPER_ORE);
        valuableOreMaterials.add(Material.DEEPSLATE_COPPER_ORE);
        valuableOreMaterials.add(Material.NETHER_GOLD_ORE);
        valuableOreMaterials.add(Material.NETHER_QUARTZ_ORE);
    }

    /**
     * Initialize sets to track specifically diamond and netherite ores
     */
    private void initializeDiamondAndNetheriteOreMaterials() {

        diamondOreMaterials.add(Material.DIAMOND_ORE);
        diamondOreMaterials.add(Material.DEEPSLATE_DIAMOND_ORE);


        netheriteOreMaterials.add(Material.ANCIENT_DEBRIS);
    }

    /**
     * Record a block break event
     */
    public void recordBlockBreak(Block block, Vector lookDirection) {
        long timestamp = System.currentTimeMillis();
        Vector blockPosition = block.getLocation().toVector();
        Material blockType = block.getType();


        if (lastBlockBreakTime != null) {
            long idleTime = timestamp - lastBlockBreakTime;

            if (idleTime > 0 && idleTime < 3600000) {
                idleTimes.add(idleTime);
            }
        }
        lastBlockBreakTime = timestamp;


        BlockBreakRecord record = new BlockBreakRecord(timestamp, blockType, blockPosition);
        blockBreaks.add(record);
        totalBlocksBroken++;


        if (valuableOreMaterials.contains(blockType)) {
            recordOreFound(block);
        }


        if (diamondOreMaterials.contains(blockType)) {
            recordDiamondOreFound(record);
        }


        if (netheriteOreMaterials.contains(blockType)) {
            recordNetheriteOreFound(record);
        }
    }

    /**
     * Records player position including Y-level during movement
     *
     * @param position The player's current position
     */

    public void recordPlayerPosition(Vector position) {
        long timestamp = System.currentTimeMillis();
        double currentYLevel = position.getY();


        YLevelRecord record = new YLevelRecord(timestamp, currentYLevel, position);
        yLevelRecords.add(record);


        if (lastYLevel != null) {
            double yChange = Math.abs(currentYLevel - lastYLevel);


            if (yChange >= Y_LEVEL_CHANGE_THRESHOLD) {
                yLevelChanges.add(yChange);
                totalYLevelChanges++;
                totalYLevelChangeAmount += yChange;


                long timeAtLevel = timestamp - lastYLevelChangeTime;
                if (timeAtLevel > 0 && timeAtLevel < 3600000) {
                    timeAtSameYLevel.add(timeAtLevel);
                }

                lastYLevelChangeTime = timestamp;
            }
        } else {

            lastYLevelChangeTime = timestamp;
        }

        lastYLevel = currentYLevel;
    }

    /**
     * Record an ore discovery
     */
    public void recordOreFound(Block block) {
        long timestamp = System.currentTimeMillis();
        Material blockType = block.getType();

        BlockBreakRecord record = new BlockBreakRecord(timestamp, blockType, block.getLocation().toVector());
        oreBreaks.add(record);
        totalOresMined++;


        oreCounts.put(blockType, oreCounts.getOrDefault(blockType, 0) + 1);


        updateTimeWindows(blockType, timestamp);
    }

    /**
     * Record diamond ore found and track vein information
     */
    private void recordDiamondOreFound(BlockBreakRecord record) {
        diamondOreBreaks.add(record);


        if (lastDiamondPosition != null) {
            double distance = lastDiamondPosition.distance(record.position);
            diamondDistances.add(distance);
        }
        lastDiamondPosition = record.position;


        if (currentDiamondVein == null) {

            currentDiamondVein = new OreVein();
            currentDiamondVein.addOre(record);
            diamondVeins.add(currentDiamondVein);
        } else {

            boolean belongsToCurrentVein = false;
            for (BlockBreakRecord veinOre : currentDiamondVein.ores) {
                double distance = veinOre.position.distance(record.position);
                if (distance <= VEIN_PROXIMITY_THRESHOLD) {
                    belongsToCurrentVein = true;
                    break;
                }
            }

            if (belongsToCurrentVein) {

                currentDiamondVein.addOre(record);
            } else {

                currentDiamondVein = new OreVein();
                currentDiamondVein.addOre(record);
                diamondVeins.add(currentDiamondVein);
            }
        }
    }

    /**
     * Record netherite ore found and track vein information
     */
    private void recordNetheriteOreFound(BlockBreakRecord record) {
        netheriteOreBreaks.add(record);


        if (lastNetheritePosition != null) {
            double distance = lastNetheritePosition.distance(record.position);
            netheriteDistances.add(distance);
        }
        lastNetheritePosition = record.position;


        if (currentNetheriteVein == null) {

            currentNetheriteVein = new OreVein();
            currentNetheriteVein.addOre(record);
            netheriteVeins.add(currentNetheriteVein);
        } else {

            boolean belongsToCurrentVein = false;
            for (BlockBreakRecord veinOre : currentNetheriteVein.ores) {
                double distance = veinOre.position.distance(record.position);
                if (distance <= VEIN_PROXIMITY_THRESHOLD) {
                    belongsToCurrentVein = true;
                    break;
                }
            }

            if (belongsToCurrentVein) {

                currentNetheriteVein.addOre(record);
            } else {

                currentNetheriteVein = new OreVein();
                currentNetheriteVein.addOre(record);
                netheriteVeins.add(currentNetheriteVein);
            }
        }
    }

    /**
     * Update time-based windows for ore mining rates
     */
    private void updateTimeWindows(Material oreType, long timestamp) {
        List<OreTimeWindow> windows = oreTimeWindows.get(oreType);


        if (windows.isEmpty()) {
            windows.add(new OreTimeWindow(60000));
            windows.add(new OreTimeWindow(180000));
            windows.add(new OreTimeWindow(300000));
            windows.add(new OreTimeWindow(600000));
        }


        for (OreTimeWindow window : windows) {
            window.addOre(timestamp);
        }
    }

    /**
     * Determine if a vein forms a line pattern (at least 3 ores in almost straight line)
     */
    private boolean isVeinInLinePattern(OreVein vein) {
        if (vein.ores.size() < 3) {
            return false;
        }


        List<Vector> positions = new ArrayList<>();
        for (BlockBreakRecord ore : vein.ores) {
            positions.add(ore.position);
        }


        if (positions.size() == 3) {
            double d12 = positions.get(0).distance(positions.get(1));
            double d23 = positions.get(1).distance(positions.get(2));
            double d13 = positions.get(0).distance(positions.get(2));


            return Math.abs(d13 - (d12 + d23)) < 0.5;
        }


        if (positions.size() > 3) {
            Vector prev = null;
            Vector current = null;
            Vector direction = null;
            double totalAngleDeviation = 0;
            int angleCount = 0;

            for (Vector pos : positions) {
                if (prev == null) {
                    prev = pos;
                    continue;
                }
                if (current == null) {
                    current = pos;
                    direction = current.clone().subtract(prev);
                    direction.normalize();
                    continue;
                }


                Vector newDirection = pos.clone().subtract(current);
                newDirection.normalize();


                double dot = direction.dot(newDirection);

                dot = Math.max(-1, Math.min(1, dot));
                double angle = Math.acos(dot);


                double deviation = Math.abs(angle - Math.PI);
                totalAngleDeviation += deviation;
                angleCount++;


                prev = current;
                current = pos;
                direction = newDirection;
            }


            if (angleCount > 0) {
                double avgDeviation = totalAngleDeviation / angleCount;
                return avgDeviation < 0.3;
            }
        }

        return false;
    }

    /**
     * Calculate all derived features from raw data
     */
    public void calculateDerivedFeatures() {

        long totalMiningTimeMs = System.currentTimeMillis() - sessionStartTime;
        features.put("total_mining_time_seconds", totalMiningTimeMs / 1000.0);


        features.put("total_blocks_broken", (double) totalBlocksBroken);


        features.put("total_ores_mined", (double) totalOresMined);


        for (Material oreMaterial : valuableOreMaterials) {
            String oreName = oreMaterial.toString().toLowerCase();
            features.put("ore_count_" + oreName, (double) oreCounts.getOrDefault(oreMaterial, 0));
        }


        for (Material oreMaterial : valuableOreMaterials) {
            if (oreTimeWindows.containsKey(oreMaterial)) {
                List<OreTimeWindow> windows = oreTimeWindows.get(oreMaterial);
                String oreName = oreMaterial.toString().toLowerCase();


                for (OreTimeWindow window : windows) {
                    String windowName;
                    if (window.windowSizeMs == 60000) windowName = "1min";
                    else if (window.windowSizeMs == 180000) windowName = "3min";
                    else if (window.windowSizeMs == 300000) windowName = "5min";
                    else if (window.windowSizeMs == 600000) windowName = "10min";
                    else continue;

                    features.put("ore_rate_" + oreName + "_" + windowName, window.getMaxRate());
                }
            }
        }


        int totalDiamondVeins = diamondVeins.size();
        int totalDiamondOresInVeins = 0;
        int diamondVeinsInLinePattern = 0;

        for (OreVein vein : diamondVeins) {
            totalDiamondOresInVeins += vein.ores.size();
            if (isVeinInLinePattern(vein)) {
                diamondVeinsInLinePattern++;
            }
        }


        double avgDiamondVeinSize = totalDiamondVeins > 0 ? (double) totalDiamondOresInVeins / totalDiamondVeins : 0;
        features.put("avg_diamond_vein_size", avgDiamondVeinSize);


        int totalNetheriteVeins = netheriteVeins.size();
        int totalNetheriteOresInVeins = 0;

        for (OreVein vein : netheriteVeins) {
            totalNetheriteOresInVeins += vein.ores.size();
        }


        double avgAncientDebrisVeinSize = totalNetheriteVeins > 0 ? (double) totalNetheriteOresInVeins / totalNetheriteVeins : 0;
        features.put("avg_ancient_debris_vein_size", avgAncientDebrisVeinSize);


        double avgDiamondDistance = 0;
        if (!diamondDistances.isEmpty()) {
            double totalDiamondDistance = 0;
            for (double distance : diamondDistances) {
                totalDiamondDistance += distance;
            }
            avgDiamondDistance = totalDiamondDistance / diamondDistances.size();
        }
        features.put("avg_distance_between_diamonds", avgDiamondDistance);


        double avgNetheriteDistance = 0;
        if (!netheriteDistances.isEmpty()) {
            double totalNetheriteDistance = 0;
            for (double distance : netheriteDistances) {
                totalNetheriteDistance += distance;
            }
            avgNetheriteDistance = totalNetheriteDistance / netheriteDistances.size();
        }
        features.put("avg_distance_between_netherite", avgNetheriteDistance);


        double avgIdleTime = 0;
        if (!idleTimes.isEmpty()) {
            long totalIdleTime = 0;
            for (long idleTime : idleTimes) {
                totalIdleTime += idleTime;
            }
            avgIdleTime = totalIdleTime / (double) idleTimes.size() / 1000.0;
        }
        features.put("avg_idle_time_seconds", avgIdleTime);


        double avgYLevelChange = 0;
        if (!yLevelChanges.isEmpty()) {
            double totalChanges = 0;
            for (double change : yLevelChanges) {
                totalChanges += change;
            }
            avgYLevelChange = totalChanges / yLevelChanges.size();
        }
        features.put("avg_y_level_change", avgYLevelChange);


        double avgTimeAtSameYLevel = 0;
        if (!timeAtSameYLevel.isEmpty()) {
            long totalTime = 0;
            for (long time : timeAtSameYLevel) {
                totalTime += time;
            }
            avgTimeAtSameYLevel = totalTime / (double) timeAtSameYLevel.size() / 1000.0;
        }
        features.put("avg_time_at_same_y_level", avgTimeAtSameYLevel);


        double miningTimeMinutes = totalMiningTimeMs / 60000.0;
        double yLevelChangesPerMinute = totalYLevelChanges / miningTimeMinutes;
        features.put("y_level_changes_per_minute", yLevelChangesPerMinute);


        Map<Integer, Long> yLevelFrequency = new HashMap<>();
        long totalRecordedTime = 0;


        for (int i = 1; i < yLevelRecords.size(); i++) {
            YLevelRecord current = yLevelRecords.get(i);
            YLevelRecord previous = yLevelRecords.get(i - 1);

            int yInt = (int) previous.yLevel;
            long timeSpent = current.timestamp - previous.timestamp;

            if (timeSpent > 0 && timeSpent < 3600000) {
                yLevelFrequency.put(yInt, yLevelFrequency.getOrDefault(yInt, 0L) + timeSpent);
                totalRecordedTime += timeSpent;
            }
        }


        long maxTimeAtYLevel = 0;
        int mostCommonYLevel = 0;

        for (Map.Entry<Integer, Long> entry : yLevelFrequency.entrySet()) {
            if (entry.getValue() > maxTimeAtYLevel) {
                maxTimeAtYLevel = entry.getValue();
                mostCommonYLevel = entry.getKey();
            }
        }

        double percentTimeAtMostCommonYLevel = 0;
        if (totalRecordedTime > 0) {
            percentTimeAtMostCommonYLevel = (maxTimeAtYLevel * 100.0) / totalRecordedTime;
        }

        features.put("percent_time_at_most_common_y", percentTimeAtMostCommonYLevel);
        features.put("most_common_y_level", (double) mostCommonYLevel);


        List<Double> yChangesBeforeValuableOre = new ArrayList<>();
        List<Double> yChangesAfterValuableOre = new ArrayList<>();


        final long TIME_WINDOW = 10000;

        for (BlockBreakRecord oreBreak : oreBreaks) {
            if (valuableOreMaterials.contains(oreBreak.material)) {

                List<YLevelRecord> beforeRecords = new ArrayList<>();
                List<YLevelRecord> afterRecords = new ArrayList<>();


                for (YLevelRecord yRecord : yLevelRecords) {
                    long timeDiff = yRecord.timestamp - oreBreak.timestamp;

                    if (timeDiff < 0 && Math.abs(timeDiff) <= TIME_WINDOW) {
                        beforeRecords.add(yRecord);
                    } else if (timeDiff > 0 && timeDiff <= TIME_WINDOW) {
                        afterRecords.add(yRecord);
                    }
                }


                if (beforeRecords.size() >= 2) {
                    double totalYChange = Math.abs(beforeRecords.get(0).yLevel - beforeRecords.get(beforeRecords.size() - 1).yLevel);
                    yChangesBeforeValuableOre.add(totalYChange);
                }


                if (afterRecords.size() >= 2) {
                    double totalYChange = Math.abs(afterRecords.get(0).yLevel - afterRecords.get(afterRecords.size() - 1).yLevel);
                    yChangesAfterValuableOre.add(totalYChange);
                }
            }
        }


        double avgYChangeBeforeOre = 0;
        if (!yChangesBeforeValuableOre.isEmpty()) {
            double sum = 0;
            for (Double change : yChangesBeforeValuableOre) {
                sum += change;
            }
            avgYChangeBeforeOre = sum / yChangesBeforeValuableOre.size();
        }
        features.put("avg_y_change_before_ore", avgYChangeBeforeOre);


        double avgYChangeAfterOre = 0;
        if (!yChangesAfterValuableOre.isEmpty()) {
            double sum = 0;
            for (Double change : yChangesAfterValuableOre) {
                sum += change;
            }
            avgYChangeAfterOre = sum / yChangesAfterValuableOre.size();
        }
        features.put("avg_y_change_after_ore", avgYChangeAfterOre);


        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (YLevelRecord record : yLevelRecords) {
            minY = Math.min(minY, record.yLevel);
            maxY = Math.max(maxY, record.yLevel);
        }

        double yRange = (minY != Double.MAX_VALUE && maxY != Double.MIN_VALUE) ? (maxY - minY) : 0;
        features.put("y_level_range", yRange);


        double yLevelVariance = 0;
        if (yLevelRecords.size() > 1) {

            double sumY = 0;
            for (YLevelRecord record : yLevelRecords) {
                sumY += record.yLevel;
            }
            double meanY = sumY / yLevelRecords.size();


            double sumSquaredDiff = 0;
            for (YLevelRecord record : yLevelRecords) {
                double diff = record.yLevel - meanY;
                sumSquaredDiff += diff * diff;
            }

            yLevelVariance = sumSquaredDiff / yLevelRecords.size();
        }
        features.put("y_level_variance", yLevelVariance);
        features.put("y_level_std_dev", Math.sqrt(yLevelVariance));
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isLabeledAsCheater() {
        return labeledAsCheater;
    }

    public Map<String, Double> getFeatures() {
        return features;
    }

    public List<BlockBreakRecord> getBlockBreaks() {
        return blockBreaks;
    }

    public List<BlockBreakRecord> getOreBreaks() {
        return oreBreaks;
    }

    public long getTotalMiningTimeMs() {
        return System.currentTimeMillis() - sessionStartTime;
    }

    static class YLevelRecord {
        final long timestamp;
        final double yLevel;
        final Vector position;

        YLevelRecord(long timestamp, double yLevel, Vector position) {
            this.timestamp = timestamp;
            this.yLevel = yLevel;
            this.position = position;
        }
    }

    static class BlockBreakRecord {
        final long timestamp;
        final Material material;
        final Vector position;

        BlockBreakRecord(long timestamp, Material material, Vector position) {
            this.timestamp = timestamp;
            this.material = material;
            this.position = position;
        }
    }

    /**
     * Helper class to track ore veins
     */
    static class OreVein {
        final List<BlockBreakRecord> ores = new ArrayList<>();

        void addOre(BlockBreakRecord ore) {
            ores.add(ore);
        }

        int getSize() {
            return ores.size();
        }
    }

    /**
     * Helper class to track ore mining rates over a specific time window
     */
    static class OreTimeWindow {
        final long windowSizeMs;
        final List<Long> oreTimestamps = new ArrayList<>();

        OreTimeWindow(long windowSizeMs) {
            this.windowSizeMs = windowSizeMs;
        }

        void addOre(long timestamp) {
            oreTimestamps.add(timestamp);


            long cutoffTime = timestamp - windowSizeMs;
            oreTimestamps.removeIf(t -> t < cutoffTime);
        }

        /**
         * Get the maximum mining rate within this time window
         *
         * @return Ores per minute
         */
        double getMaxRate() {
            if (oreTimestamps.isEmpty()) return 0.0;

            int count = oreTimestamps.size();

            return (count * 60000.0) / windowSizeMs;
        }
    }
}