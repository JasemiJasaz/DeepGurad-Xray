package org.myplugin.deepGuardXray.punishments.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.punishments.AbstractPunishmentHandler;

/**
 * Handles the Mining License Suspension punishment which prevents mining valuable ores.
 */
public class MiningLicenseSuspensionHandler extends AbstractPunishmentHandler {

    public MiningLicenseSuspensionHandler(deepGuardXray plugin, ConfigManager configManager, PunishmentManager punishmentManager) {
        super(plugin, configManager, punishmentManager);
    }

    @Override
    public boolean processBlockBreak(Player player, Block block) {
        Material ore = block.getType();

        
        if (!isValuableOre(ore)) {
            return false;
        }

        
        long remainingTime = punishmentManager.getMiningLicenseSuspensionTime(player.getUniqueId()) / (60 * 1000);

        
        player.sendMessage(Component.text("Your mining license for valuable ores is suspended!").color(NamedTextColor.RED));
        player.sendMessage(Component.text("Time remaining: " + remainingTime + " minutes").color(NamedTextColor.YELLOW));

        
        return true;
    }

    @Override
    public boolean isActive(Player player) {
        return punishmentManager.hasMiningLicenseSuspension(player.getUniqueId());
    }
}