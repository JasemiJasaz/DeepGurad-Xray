package org.myplugin.deepGuardXray.punishments.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.punishments.AbstractPunishmentHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.myplugin.deepGuardXray.utils.LocationUtils.formatLocation;

/**
 * Handles the Cursed Pickaxe punishment which damages tools when mining ores.
 */
public class CursedPickaxeHandler extends AbstractPunishmentHandler {

    public CursedPickaxeHandler(deepGuardXray plugin, ConfigManager configManager, PunishmentManager punishmentManager) {
        super(plugin, configManager, punishmentManager);
    }

    @Override
    public boolean processBlockBreak(Player player, Block block) {
        Material ore = block.getType();

        
        if (!isOre(ore)) {
            return false;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();

        
        if (tool != null && tool.getType().name().contains("PICKAXE")) {
            
            if (isValuableOre(ore)) {
                
                player.getInventory().setItemInMainHand(null);

                
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.7f);
                player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.8f, 0.5f);

                
                player.getWorld().spawnParticle(Particle.LAVA,
                        block.getLocation().add(0.5, 0.5, 0.5), 15, 0.4, 0.4, 0.4, 0.1);
                player.getWorld().spawnParticle(Particle.ITEM,
                        block.getLocation().add(0.5, 0.5, 0.5), 25, 0.5, 0.5, 0.5, 0.1,
                        new ItemStack(tool.getType()));

                
                player.sendMessage(Component.text("Your pickaxe SHATTERS into pieces as punishment for your suspicious mining!", NamedTextColor.DARK_RED));

                
                damageRandomInventoryItems(player, 2);

                
                if (ThreadLocalRandom.current().nextInt(100) < 5) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 6000, 2)); 
                    player.sendMessage(Component.text("Your arms ache terribly from the cursed tool breaking...", NamedTextColor.RED));
                }

                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("Cursed pickaxe triggered for " + player.getName() +
                            " mining " + ore.name() + " - Tool destroyed with additional penalties");
                }
            }
            
            else {
                
                if (ThreadLocalRandom.current().nextInt(100) < 50) {
                    
                    player.getInventory().setItemInMainHand(null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    player.sendMessage(Component.text("Your cursed pickaxe breaks while mining ore!", NamedTextColor.RED));

                    
                    player.getWorld().spawnParticle(Particle.ITEM,
                            block.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.05,
                            new ItemStack(tool.getType()));
                }
                
                else if (tool.getItemMeta() instanceof Damageable) {
                    Damageable damageable = (Damageable) tool.getItemMeta();

                    
                    int maxDurability = tool.getType().getMaxDurability();
                    int currentDamage = damageable.getDamage();
                    int remainingDurability = maxDurability - currentDamage;
                    int newDamage = currentDamage + (int)(remainingDurability * 0.9); 

                    
                    if (newDamage >= maxDurability) {
                        
                        player.getInventory().setItemInMainHand(null);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        player.sendMessage(Component.text("Your cursed pickaxe finally gives out!", NamedTextColor.RED));
                    } else {
                        
                        damageable.setDamage(newDamage);
                        tool.setItemMeta((ItemMeta) damageable);
                        player.sendMessage(Component.text("Your cursed pickaxe is barely holding together!", NamedTextColor.RED));

                        
                        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 1.5f);
                    }
                }
            }
        }

        
        return false;
    }

    @Override
    public boolean isActive(Player player) {
        return punishmentManager.hasCursedPickaxe(player.getUniqueId());
    }

    /**
     * Helper method to damage random items in player's inventory
     * @param player The player
     * @param itemCount Maximum number of items to damage
     */
    private void damageRandomInventoryItems(Player player, int itemCount) {
        Random random = new Random();
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        List<Integer> toolSlots = new ArrayList<>();

        
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && isTool(item.getType())) {
                toolSlots.add(i);
            }
        }

        
        if (toolSlots.isEmpty()) {
            return;
        }

        
        int damaged = 0;
        while (!toolSlots.isEmpty() && damaged < itemCount) {
            
            int randomIndex = random.nextInt(toolSlots.size());
            int slot = toolSlots.remove(randomIndex);
            ItemStack item = inventory.getItem(slot);

            if (item != null && item.getItemMeta() instanceof Damageable) {
                Damageable damageable = (Damageable) item.getItemMeta();

                
                int maxDurability = item.getType().getMaxDurability();
                int currentDamage = damageable.getDamage();
                int remainingDurability = maxDurability - currentDamage;
                int newDamage = currentDamage + (int)(remainingDurability * 0.75);

                
                if (newDamage >= maxDurability) {
                    
                    inventory.setItem(slot, null);
                    player.sendMessage(Component.text("Your " + formatItemName(item.getType()) + " has been destroyed by the curse!", NamedTextColor.RED));
                } else {
                    
                    damageable.setDamage(newDamage);
                    item.setItemMeta((ItemMeta) damageable);
                }

                damaged++;
            }
        }

        if (damaged > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 0.9f);
            player.sendMessage(Component.text("The curse spreads to other tools in your inventory!", NamedTextColor.RED));
        }
    }

    /**
     * Helper method to check if a material is a tool
     */
    private boolean isTool(Material material) {
        String name = material.name();
        return name.contains("PICKAXE") ||
                name.contains("AXE") ||
                name.contains("SHOVEL") ||
                name.contains("HOE") ||
                name.contains("SWORD") ||
                name.contains("SHEARS");
    }
}