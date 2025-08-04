package org.myplugin.deepGuardXray.punishments.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.punishments.AbstractPunishmentHandler;

/**
 * Handles the Area Restriction punishment which prevents mining ores below Y-level -40.
 */
public class AreaRestrictionHandler extends AbstractPunishmentHandler {

    public AreaRestrictionHandler(deepGuardXray plugin, ConfigManager configManager, PunishmentManager punishmentManager) {
        super(plugin, configManager, punishmentManager);
    }

    @Override
    public boolean processBlockBreak(Player player, Block block) {
        
        if (block.getY() >= -40 || !isOre(block.getType())) {
            return false;
        }

        player.sendMessage(Component.text("You are restricted from mining ores below Y-level -40!", NamedTextColor.RED));

        
        return true;
    }

    @Override
    public boolean isActive(Player player) {
        return punishmentManager.hasAreaRestriction(player.getUniqueId());
    }
}