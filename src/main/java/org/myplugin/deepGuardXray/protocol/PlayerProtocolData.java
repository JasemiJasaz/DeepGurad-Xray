package org.myplugin.deepGuardXray.protocol;

import org.myplugin.deepGuardXray.ml.PlayerMiningData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Extends PlayerMiningData to include protocol-based metrics for ML detection
 * Head movement tracking has been removed
 */
public class PlayerProtocolData extends PlayerMiningData {


    private static final long ACTION_DEDUP_THRESHOLD = 500;
    private final Map<String, Integer> concurrentActionCounts = new HashMap<>();
    private final Map<String, Long> lastActionRecordedTime = new HashMap<>();
    private Logger logger;

    /**
     * Create protocol data for a player
     */
    public PlayerProtocolData(String playerName, boolean labeledAsCheater, Logger logger) {
        super(playerName, labeledAsCheater);
        this.logger = logger;
    }

    /**
     * Alternative constructor for UUID-based creation
     */
    public PlayerProtocolData(UUID playerId, Logger logger) {
        super("Unknown", false);
        this.logger = logger;
    }

    /**
     * Legacy constructors without logger for backward compatibility
     */
    public PlayerProtocolData(String playerName, boolean labeledAsCheater) {
        super(playerName, labeledAsCheater);
    }

    public PlayerProtocolData(UUID playerId) {
        super("Unknown", false);
    }

    /**
     * Record a concurrent action that shouldn't be possible
     * Includes deduplication to prevent multiple counts from duplicate packets
     */
    public void recordConcurrentAction(String actionType) {
        long currentTime = System.currentTimeMillis();


        Long lastRecorded = lastActionRecordedTime.get(actionType);
        if (lastRecorded != null && (currentTime - lastRecorded) < ACTION_DEDUP_THRESHOLD) {
            return;
        }


        lastActionRecordedTime.put(actionType, currentTime);
        concurrentActionCounts.put(actionType, concurrentActionCounts.getOrDefault(actionType, 0) + 1);
    }

    /**
     * Get count for a specific concurrent action
     */
    public int getConcurrentActionCount(String actionType) {
        return concurrentActionCounts.getOrDefault(actionType, 0);
    }

    /**
     * Get all concurrent action counts
     */
    public Map<String, Integer> getAllConcurrentActionCounts() {
        return new HashMap<>(concurrentActionCounts);
    }

    /**
     * Calculate all derived features from raw data
     * Extends the base implementation to add protocol-based features
     */
    @Override
    public void calculateDerivedFeatures() {

        super.calculateDerivedFeatures();


        Map<String, Double> features = getFeatures();


        for (Map.Entry<String, Integer> entry : concurrentActionCounts.entrySet()) {
            features.put("concurrent_" + entry.getKey().toLowerCase(), (double) entry.getValue());
        }
    }
}