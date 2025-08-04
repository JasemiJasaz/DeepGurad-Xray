package org.myplugin.deepGuardXray.punishments.handlers.Paranoia;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;
import org.myplugin.deepGuardXray.punishments.handlers.Paranoia.cooldown.CooldownManager;
import org.myplugin.deepGuardXray.punishments.handlers.Paranoia.effects.*;
import org.myplugin.deepGuardXray.punishments.handlers.Paranoia.util.LocationUtils;
import org.myplugin.deepGuardXray.punishments.handlers.Paranoia.util.ParanoiaDataHolder;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Main handler class for paranoia effects.
 * Coordinates different effect types and manages the main paranoia task.
 */
public class ParanoiaHandler implements Listener {

    private final deepGuardXray plugin;
    private final PunishmentManager punishmentManager;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;

    
    private final GhostEntityEffect ghostEntityEffect;
    private final MessageEffect messageEffect;
    private final NightmareEffect nightmareEffect;
    private final SoundEffect soundEffect;
    private final VisualEffect visualEffect;
    private final EnvironmentEffect environmentEffect;

    
    private final ParanoiaDataHolder dataHolder;

    public ParanoiaHandler(deepGuardXray plugin, PunishmentManager punishmentManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
        this.configManager = configManager;

        
        this.cooldownManager = new CooldownManager();

        
        this.dataHolder = new ParanoiaDataHolder();

        
        this.ghostEntityEffect = new GhostEntityEffect(plugin, configManager, dataHolder);
        this.messageEffect = new MessageEffect(plugin, configManager);
        this.nightmareEffect = new NightmareEffect(plugin, configManager, dataHolder);
        this.soundEffect = new SoundEffect(plugin, configManager);
        this.visualEffect = new VisualEffect(plugin, configManager);
        this.environmentEffect = new EnvironmentEffect(plugin, configManager);

        
        Bukkit.getPluginManager().registerEvents(this, plugin);

        
        startParanoiaTask();
    }

    /**
     * Processes block breaking to trigger paranoia effects
     * Called from the BlockListener class to avoid duplicate event handlers
     */
    public void processBlockBreak(Player player, Block block) {
        UUID playerId = player.getUniqueId();

        
        boolean hasParanoiaMode = punishmentManager.hasParanoiaMode(playerId);
        boolean isBelowThreshold = player.getLocation().getY() < 30;

        
        if (!(hasParanoiaMode && isBelowThreshold)) {
            return;
        }

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Processing block break paranoia for " + player.getName() +
                    " at Y:" + player.getLocation().getBlockY());
        }

        
        int y = block.getY();
        int baseChance = 5; 

        
        int depthBonus = Math.max(0, (64 - y) / 5);
        int totalChance = Math.min(20, baseChance + depthBonus);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Block break paranoia for " + player.getName() + " at Y:" + y +
                    " with " + totalChance + "% chance");
        }

        
        if (ThreadLocalRandom.current().nextInt(100) < totalChance) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Paranoia effect triggered for " + player.getName() + " from block break");
            }
            
            applyRandomParanoiaEffect(player, block);
        }
    }

    /**
     * Applies a random paranoia effect when a player mines a block
     */
    private void applyRandomParanoiaEffect(Player player, Block block) {
        int effect = ThreadLocalRandom.current().nextInt(10);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Applying random paranoia effect #" + effect + " for " + player.getName());
        }

        switch (effect) {
            case 0: 
                if (!cooldownManager.isOnCooldown(player.getUniqueId(), CooldownManager.CooldownType.SOUND)) {
                    soundEffect.playParanoiaSound(player);
                    cooldownManager.setCooldown(player.getUniqueId(), CooldownManager.CooldownType.SOUND);
                }
                break;

            case 1: 
                if (!cooldownManager.isOnCooldown(player.getUniqueId(), CooldownManager.CooldownType.GHOST_MOB)) {
                    ghostEntityEffect.spawnGhostMob(player, block.getLocation());
                    cooldownManager.setCooldown(player.getUniqueId(), CooldownManager.CooldownType.GHOST_MOB);
                }
                break;

            case 2: 
                if (!cooldownManager.isOnCooldown(player.getUniqueId(), CooldownManager.CooldownType.MESSAGE)) {
                    messageEffect.sendFakeMessage(player);
                    cooldownManager.setCooldown(player.getUniqueId(), CooldownManager.CooldownType.MESSAGE);
                }
                break;

            case 3: 
                if (!cooldownManager.isOnCooldown(player.getUniqueId(), CooldownManager.CooldownType.VISUAL)) {
                    visualEffect.createVisualEffect(player, block.getLocation());
                    cooldownManager.setCooldown(player.getUniqueId(), CooldownManager.CooldownType.VISUAL);
                }
                break;

            case 4: 
                environmentEffect.simulateCaveIn(player, block);
                break;

            case 5: 
                if (!cooldownManager.isOnCooldown(player.getUniqueId(), CooldownManager.CooldownType.FAKE_DAMAGE)) {
                    environmentEffect.applyFakeDamage(player);
                    cooldownManager.setCooldown(player.getUniqueId(), CooldownManager.CooldownType.FAKE_DAMAGE);
                }
                break;

            case 6: 
                environmentEffect.createFakeOreIllusion(player, block);
                break;

            case 7: 
                visualEffect.applyBlindness(player);
                break;

            case 8: 
                environmentEffect.createTeleportIllusion(player);
                break;

            case 9: 
                environmentEffect.createFakeExplosion(player);
                break;
        }
    }

    /**
     * Handles player movement to create occasional paranoia effects
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        
        if (event.getFrom().distanceSquared(event.getTo()) < 0.01) {
            return;
        }

        
        boolean hasParanoiaMode = punishmentManager.hasParanoiaMode(playerId);
        boolean isBelowThreshold = player.getLocation().getY() < 30;

        
        if (!(hasParanoiaMode && isBelowThreshold)) {
            return;
        }

        
        if (ThreadLocalRandom.current().nextInt(1000) < 5) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Movement paranoia effect triggered for " + player.getName() +
                        " at Y:" + player.getLocation().getBlockY());
            }
            
            applyMovementParanoiaEffect(player);
        }
    }

    /**
     * Applies movement-based paranoia effects
     */
    private void applyMovementParanoiaEffect(Player player) {
        int effect = ThreadLocalRandom.current().nextInt(5);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Applying movement paranoia effect #" + effect + " for " + player.getName());
        }

        switch (effect) {
            case 0: 
                ghostEntityEffect.spawnPeripheralGhostMob(player);
                break;

            case 1: 
                soundEffect.playDistantMiningSound(player);
                break;

            case 2: 
                visualEffect.applyCameraShake(player);
                break;

            case 3: 
                environmentEffect.applyTripwireEffect(player);
                break;

            case 4: 
                ghostEntityEffect.createFakePlayerSighting(player);
                break;
        }
    }

    /**
     * Starts the global paranoia task that periodically checks for players with paranoia effect
     */
    private void startParanoiaTask() {
        final Map<UUID, Integer> failedNightmareRolls = new HashMap<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                int affectedPlayers = 0;
                int effectsTriggered = 0;
                int nightmareRolls = 0;

                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();

                    
                    boolean hasParanoiaMode = punishmentManager.hasParanoiaMode(playerId);
                    boolean isBelowThreshold = player.getLocation().getY() < 30;

                    
                    if (!(hasParanoiaMode && isBelowThreshold)) {
                        continue;
                    }

                    affectedPlayers++;

                    
                    int playerY = player.getLocation().getBlockY();

                    
                    if (playerY < 0) {
                        nightmareRolls++;

                        
                        boolean isOnCooldown = cooldownManager.isOnCooldown(playerId, CooldownManager.CooldownType.NIGHTMARE);
                        boolean wasOnCooldownBefore = false;

                        
                        if (isOnCooldown) {
                            wasOnCooldownBefore = true;
                        } else {
                            
                            wasOnCooldownBefore = player.hasMetadata("nightmare_was_on_cooldown");
                        }

                        
                        if (isOnCooldown) {
                            
                            long remainingCooldown = cooldownManager.getRemainingCooldown(playerId, CooldownManager.CooldownType.NIGHTMARE);

                            if (configManager.isDebugEnabled()) {
                                plugin.getLogger().info("Nightmare check skipped for " + player.getName() +
                                        " - still on cooldown for " + (remainingCooldown / 1000) + " seconds");
                            }

                            
                            if (failedNightmareRolls.containsKey(playerId)) {
                                if (configManager.isDebugEnabled()) {
                                    plugin.getLogger().info("Nightmare tension reset for " + player.getName() +
                                            " - still on cooldown");
                                }
                                failedNightmareRolls.remove(playerId);
                            }

                            
                            player.setMetadata("nightmare_was_on_cooldown", new FixedMetadataValue(plugin, true));
                            continue;
                        } else {
                            

                            
                            if (wasOnCooldownBefore) {
                                if (configManager.isDebugEnabled()) {
                                    plugin.getLogger().info("★ Nightmare cooldown expired for " + player.getName() +
                                            " - starting fresh tension building");
                                }
                                
                                player.removeMetadata("nightmare_was_on_cooldown", plugin);
                                
                                failedNightmareRolls.remove(playerId);
                            }
                        }

                        
                        int baseChance = 5; 
                        int depthBonus = Math.abs(playerY) * 3; 
                        int consecutiveFailedRolls = failedNightmareRolls.getOrDefault(playerId, 0);
                        int failedBonus = consecutiveFailedRolls * 5; 
                        int nightmareChance = Math.min(500, baseChance + depthBonus + failedBonus);

                        if (configManager.isDebugEnabled()) {
                            plugin.getLogger().info("Nightmare roll for " + player.getName() +
                                    " at Y:" + playerY +
                                    " with " + (nightmareChance / 10.0) + "% chance" +
                                    " (base: " + (baseChance / 10.0) + "%, depth: " + (depthBonus / 10.0) +
                                    "%, tension: " + (failedBonus / 10.0) + "%, " +
                                    "after " + consecutiveFailedRolls + " failed rolls)");
                        }

                        
                        if (ThreadLocalRandom.current().nextInt(3000) < nightmareChance) {
                            if (configManager.isDebugEnabled()) {
                                plugin.getLogger().info("★★★ NIGHTMARE EVENT triggered for " + player.getName() +
                                        " at Y:" + playerY + " after " + consecutiveFailedRolls + " failed rolls");
                            }

                            
                            failedNightmareRolls.remove(playerId);

                            
                            nightmareEffect.triggerNightmareEvent(player);

                            
                            cooldownManager.setCooldown(playerId, CooldownManager.CooldownType.NIGHTMARE);

                            
                            continue;
                        } else {
                            
                            failedNightmareRolls.put(playerId, consecutiveFailedRolls + 1);

                            if (configManager.isDebugEnabled() && consecutiveFailedRolls > 3) {
                                plugin.getLogger().info("Nightmare tension building for " + player.getName() +
                                        " - now at " + (consecutiveFailedRolls + 1) + " failed rolls");
                            }
                        }
                    } else {
                        
                        if (failedNightmareRolls.containsKey(playerId)) {
                            if (configManager.isDebugEnabled()) {
                                plugin.getLogger().info("Nightmare tension reset for " + player.getName() +
                                        " - moved above Y=0");
                            }
                            failedNightmareRolls.remove(playerId);
                        }
                    }

                    
                    if (ThreadLocalRandom.current().nextInt(100) < 5) {
                        effectsTriggered++;
                        
                        int effect = ThreadLocalRandom.current().nextInt(3);

                        if (configManager.isDebugEnabled()) {
                            plugin.getLogger().info("Ambient paranoia effect #" + effect +
                                    " triggered for " + player.getName() +
                                    " at Y:" + player.getLocation().getBlockY());
                        }

                        switch (effect) {
                            case 0: 
                                if (!cooldownManager.isOnCooldown(playerId, CooldownManager.CooldownType.SOUND)) {
                                    soundEffect.playParanoiaSound(player);
                                    cooldownManager.setCooldown(playerId, CooldownManager.CooldownType.SOUND);
                                }
                                break;

                            case 1: 
                                if (ThreadLocalRandom.current().nextInt(3) == 0 && 
                                        !cooldownManager.isOnCooldown(playerId, CooldownManager.CooldownType.MESSAGE)) {
                                    messageEffect.sendFakeMessage(player);
                                    cooldownManager.setCooldown(playerId, CooldownManager.CooldownType.MESSAGE);
                                }
                                break;

                            case 2: 
                                if (ThreadLocalRandom.current().nextInt(5) == 0 && 
                                        !cooldownManager.isOnCooldown(playerId, CooldownManager.CooldownType.VISUAL)) {
                                    visualEffect.createVisualEffect(player, player.getLocation());
                                    cooldownManager.setCooldown(playerId, CooldownManager.CooldownType.VISUAL);
                                }
                                break;
                        }
                    }
                }

                
                ghostEntityEffect.trackAndUpdateGhostMobs();

                if (configManager.isDebugEnabled() && affectedPlayers > 0) {
                    plugin.getLogger().info("Paranoia task: " + affectedPlayers + " players affected, " +
                            effectsTriggered + " ambient effects triggered");
                }
            }
        }.runTaskTimer(plugin, 60L, 60L); 
    }

    /**
     * Trigger a nightmare event directly (for API purposes)
     */
    public void triggerNightmare(Player player) {
        UUID playerId = player.getUniqueId();

        
        if (cooldownManager.isOnCooldown(playerId, CooldownManager.CooldownType.NIGHTMARE)) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Manual nightmare trigger skipped for " + player.getName() + " - still on cooldown");
            }
            return;
        }

        
        nightmareEffect.triggerNightmareEvent(player);

        
        cooldownManager.setCooldown(playerId, CooldownManager.CooldownType.NIGHTMARE);
    }

    /**
     * Helper method to clean up resources when plugin disables
     */
    public void cleanup() {
        ghostEntityEffect.cleanup();
        nightmareEffect.cleanup();
    }

    /**
     * Get the remaining cooldown time for a specific effect
     *
     * @param playerId The player's UUID
     * @param type The cooldown type to check
     * @return The remaining cooldown time in milliseconds
     */
    public long getRemainingCooldown(UUID playerId, CooldownManager.CooldownType type) {
        return cooldownManager.getRemainingCooldown(playerId, type);
    }

    /**
     * Check if a player is on cooldown for a specific effect
     *
     * @param playerId The player's UUID
     * @param type The cooldown type to check
     * @return true if on cooldown, false otherwise
     */
    public boolean isOnCooldown(UUID playerId, CooldownManager.CooldownType type) {
        return cooldownManager.isOnCooldown(playerId, type);
    }
}