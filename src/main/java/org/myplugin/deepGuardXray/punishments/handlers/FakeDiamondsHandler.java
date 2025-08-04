package org.myplugin.deepGuardXray.punishments.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.punishments.AbstractPunishmentHandler;

/**
 * Handles the Fake Diamonds punishment which replaces diamond ore with coal.
 */
public class FakeDiamondsHandler extends AbstractPunishmentHandler {

    public FakeDiamondsHandler(deepGuardXray plugin, ConfigManager configManager, PunishmentManager punishmentManager) {
        super(plugin, configManager, punishmentManager);
    }

    @Override
    public boolean processBlockBreak(Player player, Block block) {
        Material ore = block.getType();

        
        if (ore != Material.DIAMOND_ORE && ore != Material.DEEPSLATE_DIAMOND_ORE) {
            return false;
        }

        
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.COAL, 1));
        player.sendMessage(Component.text("The diamond crumbles to coal in your hands!", NamedTextColor.RED));

        
        punishmentManager.decrementFakeDiamonds(player.getUniqueId());

        
        
        block.setType(Material.AIR);

        
        return true;
    }

    @Override
    public boolean isActive(Player player) {
        return punishmentManager.hasFakeDiamonds(player.getUniqueId());
    }
}