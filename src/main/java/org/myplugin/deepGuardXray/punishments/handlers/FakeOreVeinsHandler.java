package org.myplugin.deepGuardXray.punishments.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.punishments.AbstractPunishmentHandler;

import java.util.concurrent.ThreadLocalRandom;

import static org.myplugin.deepGuardXray.utils.LocationUtils.formatLocation;

/**
 * Handles the Fake Ore Veins punishment which turns valuable ores into stone sometimes.
 */
public class FakeOreVeinsHandler extends AbstractPunishmentHandler {

    public FakeOreVeinsHandler(deepGuardXray plugin, ConfigManager configManager, PunishmentManager punishmentManager) {
        super(plugin, configManager, punishmentManager);
    }

    @Override
    public boolean processBlockBreak(Player player, Block block) {
        Material ore = block.getType();

        
        if (!isValuableOre(ore)) {
            return false;
        }

        
        if (ThreadLocalRandom.current().nextBoolean()) {
            
            Material stoneType = ore.name().contains("DEEPSLATE") ? Material.DEEPSLATE : Material.STONE;

            
            player.playEffect(block.getLocation(), Effect.STEP_SOUND, stoneType);

            
            block.setType(stoneType);

            
            player.sendMessage(Component.text("The ore vein crumbles to stone as you mine it!", NamedTextColor.RED));

            
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);

            
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Fake ore veins punishment triggered for " +
                        player.getName() + " at " + formatLocation(block.getLocation()) +
                        " - " + ore.name() + " turned to " + stoneType.name());
            }

            
            return true;
        }

        
        return false;
    }

    @Override
    public boolean isActive(Player player) {
        return punishmentManager.hasFakeOreVeins(player.getUniqueId());
    }
}