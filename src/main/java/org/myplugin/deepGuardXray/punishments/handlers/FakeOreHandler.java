package org.myplugin.deepGuardXray.punishments.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.managers.StatsManager;
import org.myplugin.deepGuardXray.punishments.AbstractPunishmentHandler;

/**
 * Handles fake ores that are placed by the punishment manager.
 */
public class FakeOreHandler extends AbstractPunishmentHandler {

    public FakeOreHandler(deepGuardXray plugin, ConfigManager configManager, PunishmentManager punishmentManager) {
        super(plugin, configManager, punishmentManager);
    }

    @Override
    public boolean processBlockBreak(Player player, Block block) {
        
        if (!punishmentManager.isFakeOre(block)) {
            return false;
        }

        Material ore = block.getType();

        
        player.playEffect(block.getLocation(), Effect.STEP_SOUND, Material.STONE);

        
        Material originalType = Material.STONE;
        if (block.hasMetadata("original_type")) {
            String typeName = block.getMetadata("original_type").get(0).asString();
            try {
                originalType = Material.valueOf(typeName);
            } catch (Exception e) {
                originalType = Material.STONE;
            }
        }

        
        block.setType(originalType);

        
        punishmentManager.removeFakeOre(block);

        player.sendMessage(Component.text("The ore vein wasn't real!", NamedTextColor.RED));

        
        StatsManager.addOreMined(player.getUniqueId(), ore);

        
        return true;
    }

    @Override
    public boolean isActive(Player player) {
        
        
        return true;
    }
}