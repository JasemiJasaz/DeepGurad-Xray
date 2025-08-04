package org.myplugin.deepGuardXray.punishments.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.punishments.AbstractPunishmentHandler;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles the Resource Tax punishment which reduces ore drops.
 */
public class ResourceTaxHandler extends AbstractPunishmentHandler {

    public ResourceTaxHandler(deepGuardXray plugin, ConfigManager configManager, PunishmentManager punishmentManager) {
        super(plugin, configManager, punishmentManager);
    }

    @Override
    public boolean processBlockBreak(Player player, Block block) {
        Material ore = block.getType();


        if (!isOre(ore)) {
            return false;
        }


        Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand());


        if (!drops.isEmpty()) {

            int totalTaxCollected = 0;
            String oreName = formatItemName(ore);


            for (ItemStack drop : drops) {
                int originalAmount = drop.getAmount();
                int taxedAmount = 0;
                int taxAmount = 0;

                if (isValuableOre(ore)) {

                    if (originalAmount == 1) {

                        if (ThreadLocalRandom.current().nextInt(100) < 25) {
                            taxedAmount = 1;
                            taxAmount = 0;
                        } else {
                            taxedAmount = 0;
                            taxAmount = 1;
                        }
                    } else {

                        taxAmount = (int) Math.ceil(originalAmount * 0.75);
                        taxedAmount = originalAmount - taxAmount;
                    }
                } else {

                    if (originalAmount == 1) {

                        if (ThreadLocalRandom.current().nextBoolean()) {
                            taxedAmount = 1;
                            taxAmount = 0;
                        } else {
                            taxedAmount = 0;
                            taxAmount = 1;
                        }
                    } else {

                        taxAmount = originalAmount / 2;
                        if (originalAmount % 2 == 1) {

                            if (ThreadLocalRandom.current().nextBoolean()) {
                                taxAmount++;
                            }
                        }
                        taxedAmount = originalAmount - taxAmount;
                    }
                }


                totalTaxCollected += taxAmount;


                if (taxedAmount > 0) {
                    ItemStack taxedDrop = drop.clone();
                    taxedDrop.setAmount(taxedAmount);
                    block.getWorld().dropItemNaturally(block.getLocation(), taxedDrop);
                }
            }


            if (totalTaxCollected > 0) {

                player.getWorld().spawnParticle(Particle.DUST, block.getLocation().add(0.5, 0.5, 0.5), 8, 0.2, 0.2, 0.2, 0.01, new Particle.DustOptions(Color.RED, 1.0f));


                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.5f);


                String taxRate = isValuableOre(ore) ? "75%" : "50%";
                player.sendMessage(Component.text("Resource tax collected: " + totalTaxCollected + " " + oreName + " (" + taxRate + " tax rate)").color(NamedTextColor.RED));


                if (ThreadLocalRandom.current().nextInt(10) == 0) {
                    player.sendMessage(Component.text("The tax rate is determined by your suspicious mining behavior.").color(NamedTextColor.GRAY));
                }
            }
        }


        block.setType(Material.AIR);


        return true;
    }

    @Override
    public boolean isActive(Player player) {
        return punishmentManager.hasResourceTax(player.getUniqueId());
    }
}