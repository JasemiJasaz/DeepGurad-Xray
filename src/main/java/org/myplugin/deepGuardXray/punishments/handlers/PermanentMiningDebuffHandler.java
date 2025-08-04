package org.myplugin.deepGuardXray.punishments.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.punishments.AbstractPunishmentHandler;

/**
 * Handles the Permanent Mining Debuff punishment which applies Mining Fatigue below Y-level 0.
 */
public class PermanentMiningDebuffHandler extends AbstractPunishmentHandler {

    public PermanentMiningDebuffHandler(deepGuardXray plugin, ConfigManager configManager, PunishmentManager punishmentManager) {
        super(plugin, configManager, punishmentManager);
    }

    @Override
    public boolean processBlockBreak(Player player, Block block) {
        
        if (block.getY() >= 0 || !isOre(block.getType())) {
            return false;
        }

        
        if (!player.hasPotionEffect(PotionEffectType.MINING_FATIGUE) ||
                player.getPotionEffect(PotionEffectType.MINING_FATIGUE).getAmplifier() < 1) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 1)); 
            player.sendMessage(Component.text("Mining below Y-level 0 is significantly slowed due to your mining debuff!").color(NamedTextColor.RED));
        }

        
        return false;
    }

    @Override
    public boolean isActive(Player player) {
        return punishmentManager.hasPermanentMiningDebuff(player.getUniqueId());
    }
}