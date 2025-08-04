package org.myplugin.deepGuardXray.punishments;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.punishments.handlers.*;
import org.myplugin.deepGuardXray.punishments.handlers.Paranoia.ParanoiaHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all punishment handlers and processes block break events.
 */
public class PunishmentHandlerManager {
    private final List<PunishmentHandler> handlers = new ArrayList<>();
    private final deepGuardXray plugin;
    private final ConfigManager configManager;

    public PunishmentHandlerManager(deepGuardXray plugin, ConfigManager configManager, PunishmentManager punishmentManager, ParanoiaHandler paranoiaHandler) {
        this.plugin = plugin;
        this.configManager = configManager;

        
        handlers.add(new FoolsGoldHandler(plugin, configManager, punishmentManager));
        handlers.add(new FakeOreVeinsHandler(plugin, configManager, punishmentManager));
        handlers.add(new FakeOreHandler(plugin, configManager, punishmentManager));
        handlers.add(new MiningLicenseSuspensionHandler(plugin, configManager, punishmentManager));
        handlers.add(new AreaRestrictionHandler(plugin, configManager, punishmentManager));
        handlers.add(new PermanentMiningDebuffHandler(plugin, configManager, punishmentManager));
        handlers.add(new StaffReviewHandler(plugin, configManager, punishmentManager));
        handlers.add(new ResourceTaxHandler(plugin, configManager, punishmentManager));
        handlers.add(new CursedPickaxeHandler(plugin, configManager, punishmentManager));
        handlers.add(new FakeDiamondsHandler(plugin, configManager, punishmentManager));

        
    }

    /**
     * Process a block break event through all handlers.
     *
     * @param event The BlockBreakEvent
     * @param player The player breaking the block
     * @param block The block being broken
     * @return true if the event should be cancelled, false otherwise
     */
    public boolean processBlockBreak(BlockBreakEvent event, Player player, Block block) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Processing block break through punishment handlers for " +
                    player.getName() + " at " + block.getLocation());
        }

        for (PunishmentHandler handler : handlers) {
            
            if (handler.isActive(player)) {
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("  - Checking handler: " + handler.getClass().getSimpleName());
                }

                
                boolean cancel = handler.processBlockBreak(player, block);

                
                if (cancel) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("  - Handler " + handler.getClass().getSimpleName() +
                                " cancelled further processing");
                    }
                    return true;
                }
            }
        }

        
        return false;
    }
}