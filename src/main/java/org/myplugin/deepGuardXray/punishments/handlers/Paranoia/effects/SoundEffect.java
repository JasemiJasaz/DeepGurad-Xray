package org.myplugin.deepGuardXray.punishments.handlers.Paranoia.effects;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles all sound-related paranoia effects
 */
public class SoundEffect {

    private final deepGuardXray plugin;
    private final ConfigManager configManager;

    
    private final Sound[] scarySounds = {
            Sound.ENTITY_CREEPER_PRIMED,
            Sound.ENTITY_ENDERMAN_SCREAM,
            Sound.ENTITY_GHAST_WARN,
            Sound.ENTITY_WITHER_AMBIENT,
            Sound.ENTITY_ZOMBIE_AMBIENT,
            Sound.ENTITY_SKELETON_AMBIENT,
            Sound.ENTITY_SPIDER_AMBIENT,
            Sound.AMBIENT_CAVE,
            Sound.BLOCK_STONE_BREAK,
            Sound.ENTITY_TNT_PRIMED,
            Sound.BLOCK_CHORUS_FLOWER_DEATH,
            Sound.ENTITY_WARDEN_NEARBY_CLOSER,
            Sound.ENTITY_WARDEN_HEARTBEAT
    };

    public SoundEffect(deepGuardXray plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Plays one of the defined scary sounds with randomized pitch and volume
     */
    public void playParanoiaSound(Player player) {
        
        Sound sound = scarySounds[ThreadLocalRandom.current().nextInt(scarySounds.length)];

        
        Location playerLoc = player.getLocation();
        Location soundLoc;

        
        if (ThreadLocalRandom.current().nextBoolean()) {
            
            double distance = 3 + ThreadLocalRandom.current().nextDouble() * 7;
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;

            double x = playerLoc.getX() + Math.cos(angle) * distance;
            double z = playerLoc.getZ() + Math.sin(angle) * distance;

            soundLoc = new Location(playerLoc.getWorld(), x, playerLoc.getY(), z);
        } else {
            soundLoc = playerLoc;
        }

        
        float volume = 0.3f + ThreadLocalRandom.current().nextFloat() * 0.7f;
        float pitch = 0.7f + ThreadLocalRandom.current().nextFloat() * 0.6f;

        
        player.playSound(soundLoc, sound, volume, pitch);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Playing paranoia sound " + sound + " to " + player.getName());
        }
    }

    /**
     * Plays distant mining sounds to create paranoia
     */
    public void playDistantMiningSound(Player player) {
        
        Location playerLoc = player.getLocation();
        double distance = 15 + ThreadLocalRandom.current().nextDouble() * 10; 
        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;

        double x = playerLoc.getX() + Math.cos(angle) * distance;
        double z = playerLoc.getZ() + Math.sin(angle) * distance;

        Location soundLoc = new Location(playerLoc.getWorld(), x, playerLoc.getY(), z);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Playing distant mining sounds for " + player.getName() + " at " +
                    Math.round(distance) + " blocks away");
        }

        
        Sound[] miningSounds = {
                Sound.BLOCK_STONE_BREAK,
                Sound.BLOCK_STONE_HIT
        };

        
        int soundCount = 2 + ThreadLocalRandom.current().nextInt(3);

        for (int i = 0; i < soundCount; i++) {
            Sound sound = miningSounds[ThreadLocalRandom.current().nextInt(miningSounds.length)];

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.playSound(soundLoc, sound, 0.4f, 0.8f + ThreadLocalRandom.current().nextFloat() * 0.4f);
                    }
                }
            }.runTaskLater(plugin, i * 10L); 
        }
    }
}