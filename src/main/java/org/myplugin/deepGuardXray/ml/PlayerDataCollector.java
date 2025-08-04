package org.myplugin.deepGuardXray.ml;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;
import org.myplugin.deepGuardXray.protocol.PlayerProtocolData;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Collects and stores mining-related data for players to detect X-ray behavior.
 */
public class PlayerDataCollector {
    private static final long MOVEMENT_RECORD_INTERVAL = 500;
    private final Map<UUID, PlayerMiningData> playerDataMap = new ConcurrentHashMap<>();
    private final MLConfig mlConfig;
    private final Set<Material> rareMaterials = new HashSet<>();
    private final Map<UUID, Long> lastMovementRecordTime = new ConcurrentHashMap<>();


    public PlayerDataCollector(MLConfig mlConfig) {
        this.mlConfig = mlConfig;
        initRareMaterials();
    }

    private void initRareMaterials() {
        rareMaterials.add(Material.DIAMOND_ORE);
        rareMaterials.add(Material.DEEPSLATE_DIAMOND_ORE);
        rareMaterials.add(Material.ANCIENT_DEBRIS);
        rareMaterials.add(Material.EMERALD_ORE);
        rareMaterials.add(Material.DEEPSLATE_EMERALD_ORE);
    }

    /**
     * Start collecting data for a player
     *
     * @param player           The player to collect data for
     * @param isLabeledCheater Whether this player is being labeled as a cheater for training
     */
    public void startCollecting(Player player, boolean isLabeledCheater) {
        UUID playerId = player.getUniqueId();

        playerDataMap.put(playerId, new PlayerProtocolData(player.getName(), isLabeledCheater));
    }

    /**
     * Stop collecting data for a player
     *
     * @param player The player to stop collecting data for
     * @return The collected data or null if no data was collected
     */
    public PlayerMiningData stopCollecting(Player player) {
        return playerDataMap.remove(player.getUniqueId());
    }

    /**
     * Process a block break event to collect data
     *
     * @param event The block break event
     */
    public void processBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!playerDataMap.containsKey(playerId)) {
            return;
        }

        PlayerMiningData data = playerDataMap.get(playerId);
        Block block = event.getBlock();
        Material blockType = block.getType();
        Vector lookDirection = player.getLocation().getDirection();
        data.recordBlockBreak(block, lookDirection);
    }

    /**
     * Get the mining data for a player
     *
     * @param playerId The player's UUID
     * @return The player's mining data or null
     */
    public PlayerMiningData getPlayerData(UUID playerId) {
        return playerDataMap.get(playerId);
    }

    /**
     * Get the player data specifically as PlayerProtocolData
     *
     * @param playerId The player's UUID
     * @return The player's protocol data or null
     */
    public PlayerProtocolData getPlayerProtocolData(UUID playerId) {
        PlayerMiningData data = playerDataMap.get(playerId);
        if (data instanceof PlayerProtocolData) {
            return (PlayerProtocolData) data;
        }
        return null;
    }

    /**
     * Check if we're currently collecting data for a player
     *
     * @param playerId The player's UUID
     * @return True if collecting data for this player
     */
    public boolean isCollectingData(UUID playerId) {
        return playerDataMap.containsKey(playerId);
    }

    /**
     * Save all collected data to files
     */
    public void saveAllData() {
        for (PlayerMiningData data : playerDataMap.values()) {
            data.calculateDerivedFeatures();
            MLDataManager.savePlayerData(data);
        }
    }

    /**
     * Process a player movement event with throttling
     *
     * @param player The player who moved
     */
    public void processPlayerMove(Player player) {
        UUID playerId = player.getUniqueId();

        if (!playerDataMap.containsKey(playerId)) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastRecordTime = lastMovementRecordTime.getOrDefault(playerId, 0L);

        if (currentTime - lastRecordTime < MOVEMENT_RECORD_INTERVAL) {
            return;
        }


        lastMovementRecordTime.put(playerId, currentTime);


        PlayerMiningData data = playerDataMap.get(playerId);
        Vector position = player.getLocation().toVector();
        data.recordPlayerPosition(position);
    }
}