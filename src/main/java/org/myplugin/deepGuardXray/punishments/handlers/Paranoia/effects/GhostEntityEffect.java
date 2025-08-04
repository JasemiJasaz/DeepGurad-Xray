package org.myplugin.deepGuardXray.punishments.handlers.Paranoia.effects;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.punishments.handlers.Paranoia.util.LocationUtils;
import org.myplugin.deepGuardXray.punishments.handlers.Paranoia.util.ParanoiaDataHolder;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles all effects related to ghost entities (mobs, fake players, etc.)
 */
public class GhostEntityEffect {

    private final deepGuardXray plugin;
    private final ConfigManager configManager;
    private final ParanoiaDataHolder dataHolder;

    public GhostEntityEffect(deepGuardXray plugin, ConfigManager configManager, ParanoiaDataHolder dataHolder) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataHolder = dataHolder;
    }

    /**
     * Spawns an illusion of a hostile mob that disappears after a few seconds
     */
    public void spawnGhostMob(Player player, Location location) {
        UUID playerId = player.getUniqueId();


        EntityType[] hostileMobs = {EntityType.CREEPER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.ENDERMAN, EntityType.WITCH, EntityType.WARDEN};

        EntityType mobType = hostileMobs[ThreadLocalRandom.current().nextInt(hostileMobs.length)];


        Location spawnLoc = LocationUtils.findSafeLocationNearby(location);
        if (spawnLoc == null) return;


        Entity ghostMob = spawnLoc.getWorld().spawnEntity(spawnLoc, mobType);


        ghostMob.setMetadata("ghost_mob", new FixedMetadataValue(plugin, true));


        if (ghostMob instanceof LivingEntity livingMob) {


            boolean willCharge = ThreadLocalRandom.current().nextInt(100) < 40;

            if (willCharge) {

                livingMob.setAI(true);


                if (livingMob instanceof Monster) {
                    ((Monster) livingMob).setTarget(player);
                }


                if (mobType == EntityType.CREEPER) {
                    Creeper creeper = (Creeper) livingMob;
                    creeper.setPowered(ThreadLocalRandom.current().nextBoolean());

                    if (ThreadLocalRandom.current().nextInt(100) < 30) {

                        player.playSound(creeper.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.0f);
                    }
                } else if (mobType == EntityType.ENDERMAN) {
                    Enderman enderman = (Enderman) livingMob;
                    enderman.setTarget(player);
                } else if (mobType == EntityType.SKELETON) {

                    player.playSound(livingMob.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 0.5f, 0.8f);
                }
            } else {

                livingMob.setAI(false);


                Vector direction = player.getLocation().toVector().subtract(livingMob.getLocation().toVector()).normalize();
                Location lookLocation = livingMob.getLocation().setDirection(direction);
                livingMob.teleport(lookLocation);
            }


            if (mobType == EntityType.ENDERMAN) {
                Enderman enderman = (Enderman) livingMob;

                enderman.setTarget(player);
            }
        }


        dataHolder.addGhostEntity(playerId, ghostMob);


        Sound mobSound;
        switch (mobType.name()) {
            case "CREEPER":
                mobSound = Sound.ENTITY_CREEPER_PRIMED;
                break;
            case "ZOMBIE":
                mobSound = Sound.ENTITY_ZOMBIE_AMBIENT;
                break;
            case "SKELETON":
                mobSound = Sound.ENTITY_SKELETON_AMBIENT;
                break;
            case "SPIDER":
                mobSound = Sound.ENTITY_SPIDER_AMBIENT;
                break;
            case "ENDERMAN":
                mobSound = Sound.ENTITY_ENDERMAN_STARE;
                break;
            case "WITCH":
                mobSound = Sound.ENTITY_WITCH_AMBIENT;
                break;
            case "WARDEN":
                mobSound = Sound.ENTITY_WARDEN_ANGRY;
                break;
            default:
                mobSound = Sound.ENTITY_GENERIC_EXPLODE;
        }
        player.playSound(ghostMob.getLocation(), mobSound, 0.8f, 0.7f);


        int disappearTime = 80 + ThreadLocalRandom.current().nextInt(60);

        new BukkitRunnable() {
            @Override
            public void run() {

                if (!ghostMob.isDead()) {
                    ghostMob.getWorld().spawnParticle(Particle.SMOKE, ghostMob.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);


                    ghostMob.getWorld().spawnParticle(Particle.PORTAL, ghostMob.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);


                    player.playSound(ghostMob.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.5f);


                    ghostMob.remove();
                }


                dataHolder.removeGhostEntity(playerId, ghostMob);
            }
        }.runTaskLater(plugin, disappearTime);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Spawned ghost " + mobType.name() + " for " + player.getName());
        }
    }

    /**
     * Spawns a ghost mob in the player's peripheral vision
     */
    public void spawnPeripheralGhostMob(Player player) {

        Location playerLoc = player.getLocation();
        Vector playerDir = playerLoc.getDirection();


        boolean leftSide = ThreadLocalRandom.current().nextBoolean();
        double angle = Math.toRadians(90 + ThreadLocalRandom.current().nextInt(30));
        if (leftSide) angle = -angle;


        double x = playerDir.getX();
        double z = playerDir.getZ();

        double newX = x * Math.cos(angle) - z * Math.sin(angle);
        double newZ = x * Math.sin(angle) + z * Math.cos(angle);

        Vector direction = new Vector(newX, 0, newZ).normalize();


        double distance = 8 + ThreadLocalRandom.current().nextDouble() * 4;
        Location spawnLoc = playerLoc.clone().add(direction.multiply(distance));


        spawnLoc.setY(playerLoc.getY());


        spawnLoc = LocationUtils.findSafeLocationNearby(spawnLoc);
        if (spawnLoc == null) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Peripheral ghost mob cancelled for " + player.getName() + " - no valid location found");
            }
            return;
        }


        EntityType[] hostileMobs = {EntityType.CREEPER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.ENDERMAN};

        EntityType mobType = hostileMobs[ThreadLocalRandom.current().nextInt(hostileMobs.length)];

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Spawning peripheral ghost " + mobType.name() + " for " + player.getName() + " at distance " + player.getLocation().distance(spawnLoc));
        }


        Entity ghostMob = spawnLoc.getWorld().spawnEntity(spawnLoc, mobType);


        ghostMob.setMetadata("ghost_mob", new FixedMetadataValue(plugin, true));

        UUID playerId = player.getUniqueId();


        dataHolder.addGhostEntity(playerId, ghostMob);


        if (ghostMob instanceof LivingEntity livingMob) {
            livingMob.setAI(false);


            if (mobType == EntityType.ENDERMAN) {
                Enderman enderman = (Enderman) livingMob;
                enderman.setTarget(player);
            }
        }


        int disappearTime = 40 + ThreadLocalRandom.current().nextInt(40);

        new BukkitRunnable() {
            @Override
            public void run() {

                if (!ghostMob.isDead() && ghostMob.isValid()) {

                    ghostMob.getWorld().spawnParticle(Particle.SMOKE, ghostMob.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);


                    ghostMob.remove();
                }


                dataHolder.removeGhostEntity(playerId, ghostMob);
            }
        }.runTaskLater(plugin, disappearTime);
    }

    /**
     * Creates an illusion of another player mining nearby
     */
    public void createFakePlayerSighting(Player player) {

        Location playerLoc = player.getLocation();
        Vector direction = playerLoc.getDirection().normalize();


        direction.add(new Vector((ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3, (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.1, (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3)).normalize();

        double distance = 15 + ThreadLocalRandom.current().nextDouble() * 10;
        Location spawnLoc = playerLoc.clone().add(direction.multiply(distance));


        spawnLoc = LocationUtils.findSafeLocationNearby(spawnLoc);
        if (spawnLoc == null) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Fake player sighting cancelled for " + player.getName() + " - no valid location found");
            }
            return;
        }

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Creating fake player sighting for " + player.getName() + " at distance " + Math.round(player.getLocation().distance(spawnLoc)));
        }


        ArmorStand stand = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        stand.setVisible(false);


        UUID playerId = player.getUniqueId();
        dataHolder.addGhostEntity(playerId, stand);


        for (int i = 0; i < 3; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && !stand.isDead()) {

                        player.playSound(stand.getLocation(), Sound.BLOCK_STONE_HIT, 0.4f, 1.0f);


                        player.getWorld().spawnParticle(Particle.FALLING_DUST, stand.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05, Bukkit.createBlockData(Material.STONE));
                    }
                }
            }.runTaskLater(plugin, i * 10L);
        }


        new BukkitRunnable() {
            @Override
            public void run() {
                if (!stand.isDead()) {
                    stand.remove();
                }
                dataHolder.removeGhostEntity(playerId, stand);
            }
        }.runTaskLater(plugin, 40L);
    }

    /**
     * Improved fake mob tracking system that makes mobs more interactive
     */
    public void trackAndUpdateGhostMobs() {

        int totalEntities = 0;
        int facingUpdates = 0;
        int pathUpdates = 0;
        Map<EntityType, Integer> entityTypeStats = new HashMap<>();


        for (List<Entity> entities : dataHolder.getAllGhostEntities().values()) {
            totalEntities += entities.size();
        }


        if (totalEntities == 0) {
            return;
        }


        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("trackAndUpdateGhostMobs running - tracking " + totalEntities + " ghost entities for " + dataHolder.getAllGhostEntities().size() + " players");
        }

        int updatedEntities = 0;
        int activatedEntities = 0;


        for (UUID playerId : new ArrayList<>(dataHolder.getAllGhostEntities().keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("Skipping ghost update for offline player UUID: " + playerId);
                }
                continue;
            }

            List<Entity> playerEntities = dataHolder.getPlayerGhostEntities(playerId);

            if (configManager.isDebugEnabled() && !playerEntities.isEmpty()) {
                plugin.getLogger().info("Processing " + playerEntities.size() + " ghost entities for player " + player.getName());
            }

            for (Entity entity : new ArrayList<>(playerEntities)) {
                if (entity == null || entity.isDead()) {
                    dataHolder.removeGhostEntity(playerId, entity);
                    continue;
                }


                if (configManager.isDebugEnabled()) {
                    entityTypeStats.put(entity.getType(), entityTypeStats.getOrDefault(entity.getType(), 0) + 1);
                }


                if (ThreadLocalRandom.current().nextBoolean()) {
                    updatedEntities++;

                    if (entity instanceof LivingEntity livingEntity) {


                        if (livingEntity.hasAI()) {


                            if (livingEntity instanceof Monster) {
                                ((Monster) livingEntity).setTarget(player);
                                pathUpdates++;

                                if (configManager.isDebugEnabled()) {
                                    plugin.getLogger().info("Updated target path for " + entity.getType() + " ghost entity chasing " + player.getName() + ", distance: " + String.format("%.1f", entity.getLocation().distance(player.getLocation())));
                                }
                            }
                        } else {

                            Vector direction = player.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
                            Location lookLoc = entity.getLocation().setDirection(direction);
                            entity.teleport(lookLoc);
                            facingUpdates++;

                            if (configManager.isDebugEnabled() && ThreadLocalRandom.current().nextInt(5) == 0) {

                                plugin.getLogger().info("Updated facing direction for " + entity.getType() + " ghost entity to look at " + player.getName());
                            }


                            if (ThreadLocalRandom.current().nextInt(100) < 10 && entity instanceof Monster && !livingEntity.hasAI()) {

                                livingEntity.setAI(true);
                                ((Monster) livingEntity).setTarget(player);
                                activatedEntities++;

                                if (configManager.isDebugEnabled()) {
                                    plugin.getLogger().info("★ JUMP-SCARE: Activated " + entity.getType() + " ghost entity to charge at " + player.getName() + " from distance " + String.format("%.1f", entity.getLocation().distance(player.getLocation())));
                                }


                                if (entity instanceof Creeper) {
                                    player.playSound(entity.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.0f);
                                } else if (entity instanceof Zombie) {
                                    player.playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.7f);
                                } else {
                                    player.playSound(entity.getLocation(), Sound.ENTITY_HOSTILE_HURT, 0.8f, 0.6f);
                                }


                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (!entity.isDead()) {
                                            if (configManager.isDebugEnabled()) {
                                                plugin.getLogger().info("Removing charging " + entity.getType() + " ghost entity after jump-scare for " + player.getName());
                                            }

                                            entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
                                            entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                                            entity.remove();
                                            dataHolder.removeGhostEntity(playerId, entity);
                                        }
                                    }
                                }.runTaskLater(plugin, 40L);
                            }
                        }
                    }
                }
            }
        }


        dataHolder.removeDeadEntities();


        if (configManager.isDebugEnabled() && (updatedEntities > 0 || activatedEntities > 0)) {
            StringBuilder statsLog = new StringBuilder();
            statsLog.append("Ghost tracking stats: ");
            statsLog.append(updatedEntities).append(" entities updated (");
            statsLog.append(pathUpdates).append(" paths, ");
            statsLog.append(facingUpdates).append(" facings), ");
            statsLog.append(activatedEntities).append(" jump-scares activated");


            if (!entityTypeStats.isEmpty()) {
                statsLog.append(" - Types: ");
                for (Map.Entry<EntityType, Integer> entry : entityTypeStats.entrySet()) {
                    statsLog.append(entry.getKey().name()).append("(").append(entry.getValue()).append(") ");
                }
            }

            plugin.getLogger().info(statsLog.toString());
        }
    }

    /**
     * Clean up all ghost entities
     */
    public void cleanup() {
        int removedEntities = 0;


        dataHolder.cleanupAllGhostEntities();


        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.hasMetadata("nightmare_ghost") || entity.hasMetadata("ghost_mob")) {
                    entity.remove();
                    removedEntities++;
                }
            }
        }

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("GhostEntityEffect cleanup: removed " + removedEntities + " remaining entities");
        }
    }
}