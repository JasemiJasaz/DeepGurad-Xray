package org.myplugin.deepGuardXray.utils;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.myplugin.deepGuardXray.deepGuardXray;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class ChatInputHandler implements Listener {
    private final deepGuardXray plugin;
    private final Map<UUID, Function<String, Boolean>> chatInputHandlers = new HashMap<>();

    public ChatInputHandler(deepGuardXray plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Register a chat input handler for a player
     * @param playerId Player UUID
     * @param handler Handler function that processes the input and returns true if input should be consumed
     */
    public void registerChatInputHandler(UUID playerId, Function<String, Boolean> handler) {
        chatInputHandlers.put(playerId, handler);
    }

    /**
     * Unregister a chat input handler for a player
     * @param playerId Player UUID
     */
    public void unregisterChatInputHandler(UUID playerId) {
        chatInputHandlers.remove(playerId);
    }

    /**
     * We use LOWEST priority to ensure we process and cancel the chat event before
     * any other plugin sees it, keeping the webhook URL private
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        if (chatInputHandlers.containsKey(playerId)) {
            Function<String, Boolean> handler = chatInputHandlers.get(playerId);
            String message = event.getMessage();

            
            if (message.contains("discord.com/api/webhooks/") ||
                    message.contains("discordapp.com/api/webhooks/")) {
                
                event.setCancelled(true);

                
                boolean consumeEvent = handler.apply(message);

                
                if (consumeEvent) {
                    unregisterChatInputHandler(playerId);
                }

                return;
            }

            
            boolean consumeEvent = handler.apply(message);

            
            if (consumeEvent) {
                event.setCancelled(true);
                unregisterChatInputHandler(playerId);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        
        unregisterChatInputHandler(event.getPlayer().getUniqueId());
    }
}