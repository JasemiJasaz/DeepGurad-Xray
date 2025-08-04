package org.myplugin.deepGuardXray.punishments.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.punishments.AbstractPunishmentHandler;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import static org.myplugin.deepGuardXray.utils.LocationUtils.formatLocation;

/**
 * Handles the Fool's Gold punishment which replaces diamond ore with gold/copper.
 */
public class FoolsGoldHandler extends AbstractPunishmentHandler {

    public FoolsGoldHandler(deepGuardXray plugin, ConfigManager configManager, PunishmentManager punishmentManager) {
        super(plugin, configManager, punishmentManager);
    }

    @Override
    public boolean processBlockBreak(Player player, Block block) {
        Material ore = block.getType();


        if (ore != Material.DIAMOND_ORE && ore != Material.DEEPSLATE_DIAMOND_ORE) {
            return false;
        }


        Collection<ItemStack> originalDrops = block.getDrops(player.getInventory().getItemInMainHand());


        for (ItemStack drop : originalDrops) {
            if (drop.getType() == Material.DIAMOND) {

                Material fakeMaterial;
                if (ThreadLocalRandom.current().nextBoolean()) {
                    fakeMaterial = Material.RAW_COPPER;
                } else {
                    fakeMaterial = Material.RAW_GOLD;
                }


                ItemStack fakeDrop = new ItemStack(fakeMaterial, drop.getAmount());
                block.getWorld().dropItemNaturally(block.getLocation(), fakeDrop);


                player.getWorld().spawnParticle(Particle.WITCH, block.getLocation().add(0.5, 0.5, 0.5), 15, 0.4, 0.4, 0.4, 0.01);
            } else {

                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }


        if (ThreadLocalRandom.current().nextInt(2) == 0) {
            player.sendMessage(Component.text("The diamond's luster seems... different somehow.", NamedTextColor.GOLD));
        }


        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Fool's Gold punishment triggered for " + player.getName() + " at " + formatLocation(block.getLocation()) + " - Diamonds replaced with copper/gold.");
        }


        block.setType(Material.AIR);


        return true;
    }

    @Override
    public boolean isActive(Player player) {
        return punishmentManager.hasFoolsGold(player.getUniqueId());
    }
}