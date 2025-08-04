package org.myplugin.deepGuardXray.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.myplugin.deepGuardXray.alerts.StaffAlertManager;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.managers.DecoyManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.managers.StatsManager;
import org.myplugin.deepGuardXray.managers.SuspiciousManager;
import org.myplugin.deepGuardXray.punishments.PunishmentHandlerManager;
import org.myplugin.deepGuardXray.punishments.handlers.Paranoia.ParanoiaHandler;

import java.util.UUID;

import static org.myplugin.deepGuardXray.utils.LocationUtils.formatLocation;
import static org.myplugin.deepGuardXray.utils.LocationUtils.getFriendlyWorldName;

public class BlockListener implements Listener {
    private final ConfigManager configManager;
    private final StaffAlertManager staffAlertManager;
    private final DecoyManager decoyManager;
    private final PunishmentManager punishmentManager;
    private final deepGuardXray plugin;
    private final PunishmentHandlerManager punishmentHandlerManager;
    private final ParanoiaHandler paranoiaHandler;

    public BlockListener(ConfigManager configManager, StaffAlertManager staffAlertManager, DecoyManager decoyManager, PunishmentManager punishmentManager, ParanoiaHandler paranoiaHandler) {
        this.configManager = configManager;
        this.staffAlertManager = staffAlertManager;
        this.decoyManager = decoyManager;
        this.punishmentManager = punishmentManager;
        this.paranoiaHandler = paranoiaHandler;
        this.plugin = deepGuardXray.getInstance();
        this.punishmentHandlerManager = new PunishmentHandlerManager(plugin, configManager, punishmentManager, paranoiaHandler);
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Material mat = block.getType();
        if (configManager.getNaturalOres().contains(mat)) {
            Location loc = block.getLocation();
            decoyManager.addPlayerPlacedOre(loc);


            if (configManager.isDebugEnabled()) {
                Player player = event.getPlayer();
                plugin.getLogger().info("Player " + player.getName() + " placed " + mat + " at " + formatLocation(loc) + ", added to player-placed tracking");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material ore = block.getType();
        Location loc = block.getLocation();
        UUID playerId = player.getUniqueId();


        if (configManager.isDebugEnabled() && configManager.getNaturalOres().contains(ore)) {
            plugin.getLogger().info("[EARLY CHECK] BlockListener processing block break for " + player.getName() + " at " + formatLocation(loc));
        }


        if (configManager.isDebugEnabled() && configManager.getNaturalOres().contains(ore)) {
            boolean isCurrentlyPlayerPlaced = decoyManager.isPlayerPlacedOre(loc);
            plugin.getLogger().info("isPlayerPlacedOre returns: " + isCurrentlyPlayerPlaced + " for location " + formatLocation(loc));
        }

        if (decoyManager.isPlayerPlacedOre(loc)) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Player-placed ore detected for " + player.getName() + " at " + formatLocation(loc) + ", skipping processing");
            }
            decoyManager.removePlayerPlacedOre(loc);
            return;
        }


        boolean cancelEvent = punishmentHandlerManager.processBlockBreak(event, player, block);


        if (cancelEvent) {
            event.setCancelled(true);
            return;
        }


        if (punishmentManager.hasParanoiaMode(playerId) || player.getLocation().getY() < 30) {

            paranoiaHandler.processBlockBreak(player, block);
        }


        StatsManager.addOreMined(player.getUniqueId(), ore);


        if (!configManager.getNaturalOres().contains(ore)) {
            return;
        }

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Processing natural ore break for " + player.getName() + " at " + formatLocation(loc));
        }


        decoyManager.trackOreBreak(player, block, ore);


        if (decoyManager.isDecoy(loc)) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Decoy ore detected for " + player.getName() + " at " + formatLocation(loc));
            }

            SuspiciousManager.addSuspicious(player.getUniqueId());


            punishmentManager.checkAndPunish(player);

            if (configManager.isWarnOnDecoy()) {

                player.sendMessage(Component.text("Suspicious mining behavior detected!", NamedTextColor.RED));
            }
            String friendlyWorld = getFriendlyWorldName(loc.getWorld());
            String formattedLoc = formatLocation(loc);
            String rawMessage = "Player " + player.getName() + " broke a decoy ore at " + friendlyWorld + " " + formattedLoc;
            event.getPlayer().getServer().getLogger().warning(rawMessage);
            if (configManager.isStaffAlertEnabled()) {
                staffAlertManager.alertStaffWithTeleport(player, loc, rawMessage);
            }
            staffAlertManager.logDecoyEvent(rawMessage);


            if (plugin.getConfigManager().isWebhookAlertEnabled("xray_detection")) {

                plugin.getWebhookManager().sendXrayAlert(player, ore.name(), loc, 100.0);
            }

            decoyManager.removeDecoy(loc);
            return;
        }


        if (configManager.isStaffOreAlerts()) {
            staffAlertManager.updateStaffOreCounter(player, ore, loc);
        }
    }
}