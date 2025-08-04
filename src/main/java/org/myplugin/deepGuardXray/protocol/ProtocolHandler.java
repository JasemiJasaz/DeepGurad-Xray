package org.myplugin.deepGuardXray.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.ml.PlayerDataCollector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protocol handler for monitoring player behaviors using ProtocolLib
 * Head movement tracking has been removed
 */
public class ProtocolHandler {
    private final deepGuardXray plugin;
    private final ProtocolManager protocolManager;
    private final PlayerDataCollector dataCollector;


    private final Map<UUID, Boolean> inventoryOpenMap = new ConcurrentHashMap<>();


    private final Map<UUID, PlayerActionTracker> playerActionTrackers = new ConcurrentHashMap<>();

    public ProtocolHandler(deepGuardXray plugin, PlayerDataCollector dataCollector, ConfigManager configManager) {
        this.plugin = plugin;
        this.dataCollector = dataCollector;
        this.protocolManager = ProtocolLibrary.getProtocolManager();


        registerInventoryListeners();
        registerConcurrentActionListeners();

        plugin.getLogger().info("Advanced packet-based detection enabled with ProtocolLib.");
    }

    /**
     * Register listeners for inventory packets
     */
    private void registerInventoryListeners() {

        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.WINDOW_CLICK, PacketType.Play.Server.OPEN_WINDOW, PacketType.Play.Client.CLOSE_WINDOW) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                UUID playerId = player.getUniqueId();


                if (!dataCollector.isCollectingData(playerId)) {
                    return;
                }


                if (event.getPacketType() == PacketType.Play.Client.WINDOW_CLICK) {
                    inventoryOpenMap.put(playerId, true);


                    checkConcurrentActions(playerId, "INVENTORY_CLICK");
                } else if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
                    inventoryOpenMap.put(playerId, false);
                }
            }

            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                UUID playerId = player.getUniqueId();


                if (!dataCollector.isCollectingData(playerId)) {
                    return;
                }


                if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
                    inventoryOpenMap.put(playerId, true);
                }
            }
        });
    }

    /**
     * Register listeners for concurrent action detection
     */
    private void registerConcurrentActionListeners() {

        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                UUID playerId = player.getUniqueId();


                if (!dataCollector.isCollectingData(playerId)) {
                    return;
                }


                String actionType = null;

                if (event.getPacketType() == PacketType.Play.Client.BLOCK_DIG) {
                    int action = event.getPacket().getEnumModifier(EnumWrappers.PlayerDigType.class, 2).read(0).ordinal();


                    if (action == 0 || action == 2) {
                        actionType = "DIGGING";


                        boolean inventoryOpen = inventoryOpenMap.getOrDefault(playerId, false);
                        if (inventoryOpen) {
                            PlayerProtocolData protocolData = dataCollector.getPlayerProtocolData(playerId);
                            if (protocolData != null) {
                                protocolData.recordConcurrentAction("INVENTORY_OPEN_WHILE_DIGGING");
                            }
                        }
                    }
                }


                if (actionType != null) {
                    checkConcurrentActions(playerId, actionType);
                }
            }
        });


        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.CHAT) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                UUID playerId = player.getUniqueId();


                if (!dataCollector.isCollectingData(playerId)) {
                    return;
                }


                String message = event.getPacket().getStrings().read(0);
                if (!message.startsWith("/")) {
                    checkConcurrentActions(playerId, "CHATTING");
                }
            }
        });
    }

    /**
     * Check for concurrent actions that shouldn't be possible
     */
    private void checkConcurrentActions(UUID playerId, String newAction) {
        PlayerActionTracker tracker = playerActionTrackers.computeIfAbsent(playerId, k -> new PlayerActionTracker());


        PlayerProtocolData protocolData = dataCollector.getPlayerProtocolData(playerId);
        if (protocolData == null) {
            return;
        }

        Player player = plugin.getServer().getPlayer(playerId);
        String playerName = player != null ? player.getName() : playerId.toString();


        boolean wasDigging = tracker.isActionActive("DIGGING");
        boolean wasChatting = tracker.isActionActive("CHATTING");


        long timestamp = System.currentTimeMillis();
        boolean isDuplicate = tracker.addAction(newAction, timestamp);


        if (isDuplicate) {
            plugin.getLogger().fine("Skipping duplicate " + newAction + " packet from " + playerName);
            return;
        }


        boolean inventoryOpen = inventoryOpenMap.getOrDefault(playerId, false);


        if (inventoryOpen && newAction.equals("DIGGING")) {
            protocolData.recordConcurrentAction("INVENTORY_OPEN_WHILE_DIGGING");
        }


        if ((newAction.equals("DIGGING") && wasChatting) || (newAction.equals("CHATTING") && wasDigging)) {
            protocolData.recordConcurrentAction("CHATTING_WHILE_DIGGING");
        }
    }

    /**
     * Check if a player's inventory is currently open
     */
    public boolean isInventoryOpen(UUID playerId) {
        return inventoryOpenMap.getOrDefault(playerId, false);
    }

    /**
     * Clean up resources when a player leaves
     */
    public void cleanupPlayer(UUID playerId) {
        inventoryOpenMap.remove(playerId);
        playerActionTrackers.remove(playerId);
    }

    /**
     * Clean up when the plugin disables
     */
    public void shutdown() {
        inventoryOpenMap.clear();
        playerActionTrackers.clear();
    }

    public boolean isPlayerDigging(UUID playerId) {
        PlayerActionTracker tracker = playerActionTrackers.get(playerId);
        return tracker != null && tracker.isActionActive("DIGGING");
    }

    /**
     * Track player actions for detecting concurrent actions
     */
    private static class PlayerActionTracker {
        private static final long ACTION_EXPIRY_TIME = 350;
        private static final long DUPLICATE_THRESHOLD = 100;
        private final Map<String, Long> lastActionTimes = new HashMap<>();
        private final Map<String, Long> lastProcessedTimes = new HashMap<>();

        /**
         * Add an action with deduplication
         *
         * @param action    The action type
         * @param timestamp Current timestamp
         * @return true if this is a duplicate packet that should be ignored
         */
        boolean addAction(String action, long timestamp) {

            Long lastProcessed = lastProcessedTimes.get(action);
            if (lastProcessed != null && (timestamp - lastProcessed) < DUPLICATE_THRESHOLD) {
                return true;
            }


            lastActionTimes.put(action, timestamp);
            lastProcessedTimes.put(action, timestamp);
            return false;
        }

        boolean isActionActive(String action) {
            Long lastTime = lastActionTimes.get(action);
            if (lastTime == null) {
                return false;
            }

            return System.currentTimeMillis() - lastTime < ACTION_EXPIRY_TIME;
        }
    }
}