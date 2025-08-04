package org.myplugin.deepGuardXray.punishments.handlers.Paranoia.effects;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.punishments.handlers.Paranoia.util.LocationUtils;
import org.myplugin.deepGuardXray.punishments.handlers.Paranoia.util.ParanoiaDataHolder;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles the intense Warden nightmare event
 */
public class NightmareEffect {

    private final deepGuardXray plugin;
    private final ConfigManager configManager;
    private final ParanoiaDataHolder dataHolder;

    
    private final String[] creepyMessages = {
            "HE IS WATCHING YOU",
            "YOU CANNOT ESCAPE MY GAZE",
            "I SEE EVERYTHING YOU MINE",
            "YOUR SINS ARE KNOWN TO ME",
            "NOWHERE TO HIDE",
            "I AM ALWAYS WATCHING",
            "THE DARKNESS REMEMBERS",
            "THE VOID CLAIMS ALL CHEATERS",
            "YOUR MINING PATTERNS HAVE BEEN RECORDED"
    };

    public NightmareEffect(deepGuardXray plugin, ConfigManager configManager, ParanoiaDataHolder dataHolder) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataHolder = dataHolder;
    }

    /**
     * Triggers an intense paranoia nightmare event featuring a Warden with proper emergence animation
     * 1. Block breaking sounds that gradually escalate
     * 2. Emerging Warden with proper animation
     * 3. Freezes player's view to look at Warden
     * 4. Blindness effect
     * 5. Terrifying message
     */
    public void triggerNightmareEvent(Player player) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Starting nightmare event for " + player.getName());
        }

        
        final Location playerLoc = player.getLocation().clone();
        final World world = player.getWorld();

        
        final LocationUtils.LocationHolder wardenLocationHolder = new LocationUtils.LocationHolder();

        
        Vector direction = player.getLocation().getDirection().normalize();
        
        double spawnDistance = 10 + ThreadLocalRandom.current().nextDouble() * 5;
        Location possibleLocation = player.getEyeLocation().add(direction.multiply(spawnDistance));

        
        Location safeLocation = LocationUtils.findSafeLocationNearby(possibleLocation);
        if (safeLocation == null) {
            
            safeLocation = LocationUtils.findValidSpawnLocationInSight(player.getLocation(), world, 8, 16);

            
            if (safeLocation == null) {
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("Cancelled Nightmare Event for " + player.getName() + " - no valid spawn location");
                }
                return;
            }
        }

        
        wardenLocationHolder.location = safeLocation;

        
        if (configManager.isDebugEnabled()) {
            double distance = wardenLocationHolder.location.distance(player.getLocation());
            plugin.getLogger().info("Nightmare Event for " + player.getName() + " - Warden spawning " + distance + " blocks away");
        }

        
        
        int[] delays = {40, 35, 30, 25, 22, 20, 18, 16, 14, 12, 10, 8, 6, 5, 4, 3, 2, 1}; 
        Sound[] breakingSounds = {
                Sound.BLOCK_STONE_BREAK,
                Sound.BLOCK_GRAVEL_BREAK,
                Sound.BLOCK_WOOD_BREAK
        };

        
        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);

        
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 220, 0, false, false, false));

        
        player.playSound(wardenLocationHolder.location, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.3f, 0.6f);

        
        for (int i = 0; i < delays.length; i++) {
            final int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;

                    
                    double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
                    double distance = 1 + ThreadLocalRandom.current().nextDouble() * 3;
                    double x = wardenLocationHolder.location.getX() + Math.cos(angle) * distance;
                    double z = wardenLocationHolder.location.getZ() + Math.sin(angle) * distance;
                    Location soundLoc = new Location(world, x, wardenLocationHolder.location.getY(), z);

                    
                    Sound sound = breakingSounds[ThreadLocalRandom.current().nextInt(breakingSounds.length)];
                    float volume = 0.3f + (index * 0.04f); 
                    float pitch = 0.8f - (index * 0.03f);  

                    player.playSound(soundLoc, sound, volume, pitch);

                    
                    if (index > 8) {
                        world.spawnParticle(Particle.FALLING_DUST,
                                soundLoc,
                                10, 0.5, 0.5, 0.5, 0.05,
                                Bukkit.createBlockData(Material.SCULK));
                    }

                    
                    if (index > 12) {
                        world.spawnParticle(Particle.SOUL,
                                wardenLocationHolder.location,
                                20, 1.0, 0.5, 1.0, 0.01);
                        world.spawnParticle(Particle.PORTAL,
                                wardenLocationHolder.location,
                                15, 0.7, 0.3, 0.7, 0.01);

                        
                        if (index % 3 == 0) {
                            player.playSound(soundLoc, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.4f, 0.5f);
                        }
                    }
                }
            }.runTaskLater(plugin, i * delays[i]);
        }

        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                
                Vector lookDirection = wardenLocationHolder.location.clone().add(0, 1, 0) 
                        .subtract(player.getEyeLocation()).toVector().normalize();
                Location playerLookLoc = player.getLocation().clone();
                playerLookLoc.setDirection(lookDirection);
                player.teleport(playerLookLoc);

                
                world.spawnParticle(Particle.SOUL,
                        wardenLocationHolder.location,
                        20, 1.0, 0.5, 1.0, 0.01);
                world.spawnParticle(Particle.PORTAL,
                        wardenLocationHolder.location,
                        15, 0.7, 0.3, 0.7, 0.01);

                
                player.playSound(wardenLocationHolder.location, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 0.6f);
                player.playSound(wardenLocationHolder.location, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);

                
                new BukkitRunnable() {
                    int count = 0;

                    @Override
                    public void run() {
                        if (!player.isOnline() || count >= 3) {
                            this.cancel();
                            return;
                        }

                        
                        player.setVelocity(new Vector(
                                (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.03,
                                0.05,
                                (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.03
                        ));

                        
                        for (int i = 0; i < 3; i++) {
                            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
                            double dist = ThreadLocalRandom.current().nextDouble() * 1.5;
                            Location particleLoc = wardenLocationHolder.location.clone().add(
                                    Math.cos(angle) * dist,
                                    0.1,
                                    Math.sin(angle) * dist
                            );

                            world.spawnParticle(Particle.FALLING_DUST,
                                    particleLoc,
                                    15, 0.5, 0.1, 0.5, 0.05,
                                    Bukkit.createBlockData(Material.SCULK));
                        }

                        count++;
                    }
                }.runTaskTimer(plugin, 0, 5L); 

                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        
                        Warden warden = (Warden) world.spawnEntity(wardenLocationHolder.location, EntityType.WARDEN);
                        warden.setMetadata("nightmare_ghost", new FixedMetadataValue(plugin, true));

                        
                        
                        warden.setAI(true);

                        
                        warden.setAnger(player, 150);

                        
                        for (int i = 0; i < 5; i++) {
                            final int index = i;
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (!player.isOnline() || warden.isDead()) return;

                                    
                                    world.spawnParticle(Particle.SOUL,
                                            warden.getLocation().add(0, 0.5, 0),
                                            15, 0.7, 0.5, 0.7, 0.01);

                                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(15, 82, 186), 1.0f);
                                    world.spawnParticle(Particle.DUST,
                                            warden.getLocation().add(0, 0.1, 0),
                                            20, 0.7, 0.1, 0.7, 0.05, dustOptions);
                                }
                            }.runTaskLater(plugin, i * 8L); 
                        }

                        
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (!player.isOnline() || warden.isDead()) return;

                                
                                player.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_ANGRY, 1.0f, 0.6f);

                                
                                
                                warden.setAI(false);

                                
                                handleWardenStaring(player, warden);
                            }
                        }.runTaskLater(plugin, 40L); 
                    }
                }.runTaskLater(plugin, 20L); 
            }
        }.runTaskLater(plugin, 200L); 
    }

    /**
     * Handles the Warden staring phase of the nightmare event
     */
    private void handleWardenStaring(Player player, Warden warden) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Starting Warden staring phase for " + player.getName());
        }

        
        final int STARING_DURATION_TICKS = 100; 

        
        new BukkitRunnable() {
            int ticksRemaining = STARING_DURATION_TICKS;

            @Override
            public void run() {
                if (!player.isOnline() || warden == null || warden.isDead()) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Warden staring phase cancelled early for " + player.getName());
                    }
                    this.cancel();
                    return;
                }

                
                Vector direction = warden.getLocation().clone().add(0, 2, 0) 
                        .subtract(player.getEyeLocation()).toVector().normalize();

                
                Location playerLoc = player.getLocation();
                playerLoc.setDirection(direction);
                player.teleport(playerLoc);

                
                if (ticksRemaining % 20 == 0) { 
                    
                    Sound[] wardenSounds = {
                            Sound.ENTITY_WARDEN_HEARTBEAT,
                            Sound.ENTITY_WARDEN_LISTENING,
                            Sound.ENTITY_WARDEN_LISTENING_ANGRY,
                            Sound.ENTITY_WARDEN_SNIFF
                    };
                    Sound sound = wardenSounds[ThreadLocalRandom.current().nextInt(wardenSounds.length)];
                    player.playSound(warden.getLocation(), sound, 1.0f, 0.5f);

                    
                    if (ticksRemaining < STARING_DURATION_TICKS / 2) {
                        
                        for (int i = 0; i < 8; i++) {
                            double angle = i * Math.PI / 4;
                            Location particleLoc = player.getEyeLocation().add(
                                    Math.cos(angle) * 0.5,
                                    Math.sin(angle) * 0.5,
                                    0);
                            Particle.DustOptions darkDust;

                            
                            if (i % 2 == 0) {
                                darkDust = new Particle.DustOptions(Color.fromRGB(0, 0, 50), 2.0f); 
                            } else {
                                darkDust = new Particle.DustOptions(Color.BLACK, 2.0f);
                            }

                            player.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, darkDust);
                        }
                    }
                }

                
                if (ticksRemaining % 5 == 0) {
                    
                    Vector wardenLookDir = player.getEyeLocation()
                            .subtract(warden.getLocation()).toVector().normalize();
                    Location wardenLookLoc = warden.getLocation().clone();
                    wardenLookLoc.setDirection(wardenLookDir);
                    warden.teleport(wardenLookLoc);

                    
                    if (ticksRemaining < 50 && ticksRemaining % 15 == 0) {
                        warden.getWorld().spawnParticle(Particle.EXPLOSION,
                                warden.getLocation().add(0, 2, 0),
                                2, 0.1, 0.1, 0.1, 0);
                        warden.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                                warden.getLocation().add(0, 2, 0),
                                15, 0.3, 0.3, 0.3, 0.05);
                    }
                }

                
                if (ticksRemaining < 40 && ticksRemaining % 10 == 0 && warden.getLocation().distance(player.getLocation()) > 4) {
                    
                    Vector moveVector = player.getLocation().toVector()
                            .subtract(warden.getLocation().toVector())
                            .normalize()
                            .multiply(1.0); 

                    Location newLoc = warden.getLocation().add(moveVector);
                    warden.teleport(newLoc);

                    
                    player.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_STEP, 1.0f, 0.7f);

                    
                    Particle.DustOptions sculkDust = new Particle.DustOptions(Color.fromRGB(15, 82, 186), 1.0f);
                    warden.getWorld().spawnParticle(Particle.DUST,
                            warden.getLocation(),
                            15, 0.5, 0.1, 0.5, 0.05, sculkDust);
                    
                    warden.getWorld().spawnParticle(Particle.SOUL,
                            warden.getLocation(),
                            8, 0.3, 0.1, 0.3, 0.01);
                }

                
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 6, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20, 128, false, false, false));

                
                ticksRemaining--;

                
                if (ticksRemaining == 10) {
                    
                    player.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 0.8f);

                    
                    warden.getWorld().spawnParticle(Particle.EXPLOSION,
                            warden.getLocation().add(0, 2, 0),
                            2, 0.1, 0.1, 0.1, 0);
                    warden.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            warden.getLocation().add(0, 2, 0),
                            15, 0.3, 0.3, 0.3, 0.05);
                }

                
                if (ticksRemaining <= 0) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Warden staring phase completed for " + player.getName() + " - moving to sonic boom phase");
                    }

                    
                    warden.setAI(true);
                    warden.setAnger(player, 150); 

                    
                    player.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.8f);

                    
                    new BukkitRunnable() {
                        int count = 0;

                        @Override
                        public void run() {
                            if (count >= 10 || !player.isOnline() || warden.isDead()) {
                                this.cancel();
                                return;
                            }

                            
                            player.setVelocity(new Vector(
                                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.05,
                                    0.02,
                                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.05
                            ));

                            
                            if (count % 2 == 0) {
                                warden.getWorld().spawnParticle(Particle.EXPLOSION,
                                        warden.getLocation().add(0, 1.5, 0),
                                        1, 0.1, 0.1, 0.1, 0);
                            }

                            
                            warden.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                                    warden.getLocation().add(0, 2, 0),
                                    10, 0.3, 0.3, 0.3, 0.05);

                            count++;
                        }
                    }.runTaskTimer(plugin, 0, 2L); 

                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() && !warden.isDead()) {
                                
                                handleBlindnessPhase(player, warden);
                            }
                        }
                    }.runTaskLater(plugin, 35L); 
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Handles the blindness phase of the nightmare event
     */
    private void handleBlindnessPhase(Player player, Entity ghost) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Starting blindness phase for " + player.getName());
        }
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false, false));

        
        ghost.getWorld().spawnParticle(Particle.EXPLOSION,
                ghost.getLocation(),
                3, 0.5, 1.0, 0.5, 0.01);

        ghost.getWorld().spawnParticle(Particle.PORTAL,
                ghost.getLocation(),
                50, 0.5, 1.0, 0.5, 0.01);

        
        player.playSound(ghost.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.5f);

        
        ghost.remove();

        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Blindness phase message cancelled - player offline: " + player.getName());
                    }
                    return;
                }

                
                String message = creepyMessages[ThreadLocalRandom.current().nextInt(creepyMessages.length)];

                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("Sending nightmare message to " + player.getName() + ": " + message);
                }

                
                player.sendMessage("");
                player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + message);
                player.sendMessage("");

                
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.5f);

                
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0, false, false, false));
            }
        }.runTaskLater(plugin, 40L); 
    }

    /**
     * Clean up any remaining nightmare entities
     */
    public void cleanup() {
        int removedEntities = 0;

        
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.hasMetadata("nightmare_ghost")) {
                    entity.remove();
                    removedEntities++;
                }
            }
        }

        if (configManager.isDebugEnabled() && removedEntities > 0) {
            plugin.getLogger().info("NightmareEffect cleanup: removed " + removedEntities + " remaining nightmare entities");
        }
    }
}