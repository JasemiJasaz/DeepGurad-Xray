package org.myplugin.deepGuardXray.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.myplugin.deepGuardXray.config.ConfigManager;

import java.util.*;

import static org.myplugin.deepGuardXray.utils.LocationUtils.formatLocation;

public class DecoyManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;


    private final Map<Location, Material> decoyMap = new HashMap<>();
    private final Map<UUID, OreTracker> trackerMap = new HashMap<>();
    private final Set<LocationWrapper> playerPlacedOre = new HashSet<>();


    private final Map<Location, Set<Location>> veinMap = new HashMap<>();
    private final Map<Location, Map<Location, Material>> originalBlockTypes = new HashMap<>();


    private final Map<Location, UUID> decoyOwners = new HashMap<>();


    private final BlockFace[] ADJACENT_FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};


    private final Set<Material> replaceableBlocks = new HashSet<>(Arrays.asList(Material.STONE, Material.DEEPSLATE, Material.COBBLESTONE, Material.DIRT, Material.GRAVEL, Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.TUFF, Material.CALCITE, Material.DRIPSTONE_BLOCK));

    public DecoyManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;


        scheduleOreTrackerCleanup();
    }

    public Map<Location, Set<Location>> getAllDecoyVeins() {

        return new HashMap<>(veinMap);
    }

    /**
     * Schedule a task to clear expired ore trackers based on the timeWindowTicks config
     */
    private void scheduleOreTrackerCleanup() {
        final long cleanupInterval = Math.max(1200, configManager.getTimeWindowTicks() / 2);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();


            for (Iterator<Map.Entry<UUID, OreTracker>> it = trackerMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, OreTracker> entry = it.next();
                OreTracker tracker = entry.getValue();

                if (tracker.isExpired(currentTime, configManager.getTimeWindowTicks())) {
                    it.remove();
                }
            }
        }, cleanupInterval, cleanupInterval);
    }

    public void addPlayerPlacedOre(Location loc) {
        playerPlacedOre.add(new LocationWrapper(loc));
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Added player-placed ore at " + formatLocation(loc));
        }
    }

    public boolean isPlayerPlacedOre(Location loc) {
        boolean result = playerPlacedOre.contains(new LocationWrapper(loc));
        if (configManager.isDebugEnabled() && result) {
            plugin.getLogger().info("Found player-placed ore at " + formatLocation(loc));
        }
        return result;
    }

    public void removePlayerPlacedOre(Location loc) {
        boolean removed = playerPlacedOre.remove(new LocationWrapper(loc));
        if (configManager.isDebugEnabled() && removed) {
            plugin.getLogger().info("Removed player-placed ore at " + formatLocation(loc));
        }
    }

    public boolean isDecoy(Location loc) {
        if (!decoyMap.containsKey(loc)) {
            return false;
        }


        Material expectedType = decoyMap.get(loc);
        Block block = loc.getBlock();

        if (block.getType() != expectedType) {


            removeDecoy(loc);
            return false;
        }

        return true;
    }

    public void removeDecoy(Location loc) {

        Material expectedMaterial = decoyMap.remove(loc);
        decoyOwners.remove(loc);


        for (Map.Entry<Location, Set<Location>> entry : new HashMap<>(veinMap).entrySet()) {
            if (entry.getValue().contains(loc)) {

                entry.getValue().remove(loc);


                if (entry.getValue().isEmpty()) {
                    veinMap.remove(entry.getKey());
                    originalBlockTypes.remove(entry.getKey());
                }


                break;
            }
        }


        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Removed decoy ore at " + formatLocation(loc));
        }
    }

    public void trackOreBreak(Player player, Block block, Material ore) {

        if (!configManager.getNaturalOres().contains(ore)) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        OreTracker tracker = trackerMap.computeIfAbsent(playerUUID, k -> new OreTracker());
        tracker.increment(System.currentTimeMillis());

        if (tracker.getCount() > configManager.getOreThreshold() && configManager.isDecoyEnabled()) {

            placeDecoy(player, block, ore);
            tracker.reset();
        }
    }

    private void placeDecoy(Player player, Block originalBlock, Material oreType) {
        Location initialCandidateLoc = calculateDecoyLocation(player, originalBlock.getLocation());
        int attempts = 0;
        int maxAttempts = 5;

        while (attempts < maxAttempts) {
            attempts++;

            if (initialCandidateLoc != null && isBuried(initialCandidateLoc)) {
                if (placeDecoyVeinAt(player, initialCandidateLoc, oreType)) {
                    return;
                }
            }


            if (initialCandidateLoc != null) {
                Random random = new Random();
                initialCandidateLoc = initialCandidateLoc.clone().add(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            }
        }


        Location originalLoc = originalBlock.getLocation();
        int radius = configManager.getDecoySearchRadius();


        for (int yOffset = -radius; yOffset <= radius; yOffset++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    if (xOffset == 0 && yOffset == 0 && zOffset == 0) continue;

                    Location candidateLoc = originalLoc.clone().add(xOffset, yOffset, zOffset);
                    if (candidateLoc != null && candidateLoc.getWorld() != null && candidateLoc.getWorld().equals(originalLoc.getWorld()) && isBuried(candidateLoc)) {
                        if (placeDecoyVeinAt(player, candidateLoc, oreType)) return;
                    }
                }
            }
        }


        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Failed to place decoy ore vein for " + player.getName() + " near " + formatLocation(originalLoc));
        }
    }


    private boolean placeDecoyVeinAt(Player player, Location startLoc, Material oreType) {

        int veinSize = getVeinSizeForOre(oreType);


        Set<Location> veinBlocks = generateVein(startLoc, oreType, veinSize);

        if (veinBlocks.isEmpty() || veinBlocks.size() < Math.max(2, veinSize / 2)) {
            return false;
        }


        Map<Location, Material> originalTypes = new HashMap<>();
        UUID playerUUID = player.getUniqueId();


        int actualBlocksPlaced = 0;
        for (Location loc : veinBlocks) {
            Block block = loc.getBlock();


            if (!replaceableBlocks.contains(block.getType()) || playerPlacedOre.contains(new LocationWrapper(loc)) || decoyMap.containsKey(loc)) {
                continue;
            }


            originalTypes.put(loc, block.getType());


            block.setType(oreType);
            decoyMap.put(loc, oreType);
            decoyOwners.put(loc, playerUUID);
            actualBlocksPlaced++;
        }


        if (actualBlocksPlaced == 0) {
            return false;
        }


        Set<Location> placedBlocks = new HashSet<>();
        for (Location loc : veinBlocks) {
            if (decoyMap.containsKey(loc)) {
                placedBlocks.add(loc);
            }
        }


        veinMap.put(startLoc, placedBlocks);
        originalBlockTypes.put(startLoc, originalTypes);


        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Decoy ore vein placed for " + player.getName() + " at " + formatLocation(startLoc) + " with " + actualBlocksPlaced + " blocks (originally planned " + veinBlocks.size() + ")");
        }


        scheduleDecoyVeinRevert(player, startLoc);

        return true;
    }

    /**
     * Generate a realistic vein size based on ore type
     * This mimics Minecraft's natural generation patterns
     */
    private int getVeinSizeForOre(Material oreType) {
        Random random = new Random();


        if (oreType == Material.DIAMOND_ORE || oreType == Material.DEEPSLATE_DIAMOND_ORE) {
            return 3 + random.nextInt(3);
        } else if (oreType == Material.EMERALD_ORE || oreType == Material.DEEPSLATE_EMERALD_ORE) {
            return 1 + random.nextInt(2);
        } else if (oreType == Material.IRON_ORE || oreType == Material.DEEPSLATE_IRON_ORE || oreType == Material.GOLD_ORE || oreType == Material.DEEPSLATE_GOLD_ORE) {
            return 4 + random.nextInt(4);
        } else if (oreType == Material.COAL_ORE || oreType == Material.DEEPSLATE_COAL_ORE) {
            return 5 + random.nextInt(11);
        } else if (oreType == Material.REDSTONE_ORE || oreType == Material.DEEPSLATE_REDSTONE_ORE || oreType == Material.LAPIS_ORE || oreType == Material.DEEPSLATE_LAPIS_ORE) {
            return 4 + random.nextInt(5);
        } else {
            return 3 + random.nextInt(3);
        }
    }

    /**
     * Generate a connected vein of ore blocks
     * This creates realistic-looking veins rather than isolated blocks
     */
    private Set<Location> generateVein(Location startLoc, Material oreType, int size) {
        Set<Location> veinBlocks = new HashSet<>();
        Set<Location> candidates = new HashSet<>();

        Block startBlock = startLoc.getBlock();
        if (!replaceableBlocks.contains(startBlock.getType()) || playerPlacedOre.contains(new LocationWrapper(startLoc)) || !isBuried(startLoc)) {
            return veinBlocks;
        }

        veinBlocks.add(startLoc);


        for (BlockFace face : ADJACENT_FACES) {
            candidates.add(startBlock.getRelative(face).getLocation());
        }


        Random random = new Random();
        int attempts = 0;
        int maxAttempts = size * 5;

        while (veinBlocks.size() < size && !candidates.isEmpty() && attempts < maxAttempts) {
            attempts++;


            List<Location> candidateList = new ArrayList<>(candidates);
            Location nextLoc = candidateList.get(random.nextInt(candidateList.size()));
            candidates.remove(nextLoc);


            Block block = nextLoc.getBlock();
            if (replaceableBlocks.contains(block.getType()) && !playerPlacedOre.contains(new LocationWrapper(nextLoc)) && !decoyMap.containsKey(nextLoc) && isBuried(nextLoc)) {

                veinBlocks.add(nextLoc);


                for (BlockFace face : ADJACENT_FACES) {
                    Location adjLoc = block.getRelative(face).getLocation();
                    if (!veinBlocks.contains(adjLoc)) {
                        candidates.add(adjLoc);
                    }
                }
            }
        }


        if (veinBlocks.size() < Math.max(2, size / 2)) {
            return new HashSet<>();
        }

        return veinBlocks;
    }

    /**
     * Schedule the reversion of an entire ore vein
     */
    private void scheduleDecoyVeinRevert(final Player player, final Location primaryLoc) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Set<Location> veinBlocks = veinMap.get(primaryLoc);
            Map<Location, Material> originalTypes = originalBlockTypes.get(primaryLoc);

            if (veinBlocks == null || originalTypes == null) return;

            boolean shouldRevert = !player.isOnline() || player.getLocation().distance(primaryLoc) > configManager.getMaxDecayDistance();

            if (shouldRevert) {

                for (Location loc : veinBlocks) {
                    Block block = loc.getBlock();
                    Material originalType = originalTypes.get(loc);

                    if (originalType != null) {

                        if (decoyMap.containsKey(loc) && block.getType() == decoyMap.get(loc)) {
                            block.setType(originalType);
                            decoyMap.remove(loc);
                            decoyOwners.remove(loc);
                        }
                    }
                }


                veinMap.remove(primaryLoc);
                originalBlockTypes.remove(primaryLoc);

                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("Reverted decoy ore vein at " + formatLocation(primaryLoc) + (player.isOnline() ? " due to player being too far away" : " because player went offline"));
                }
            }
        }, configManager.getDecoyRevertDelay());
    }


    private Location calculateDecoyLocation(Player player, Location original) {
        Location eyeLoc = player.getEyeLocation();
        Vector viewDir = eyeLoc.getDirection().normalize();
        Vector decoyDir = viewDir.clone().multiply(-1);
        Vector perpendicular = decoyDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        decoyDir.add(perpendicular.multiply(configManager.getDecoyFieldOffset()));
        return original.clone().add(decoyDir.multiply(configManager.getDecoyDistance()));
    }


    private boolean isBuried(Location loc) {
        Block block = loc.getBlock();
        int buriedCount = 0;
        int exposedCount = 0;


        BlockFace[] criticalFaces = {BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : criticalFaces) {
            Block adjacentBlock = block.getRelative(face);
            Material adj = adjacentBlock.getType();

            if (adj == Material.AIR || adj == Material.WATER || adj == Material.LAVA || adj == Material.CAVE_AIR || isTransparent(adj)) {
                exposedCount++;


                if (exposedCount >= 2) {
                    return false;
                }
            }
        }


        for (BlockFace face : BlockFace.values()) {
            if (face == BlockFace.SELF) continue;
            Block adjacentBlock = block.getRelative(face);
            Material adj = adjacentBlock.getType();

            if (adj != Material.AIR && adj != Material.WATER && adj != Material.LAVA && adj != Material.CAVE_AIR && !isTransparent(adj)) {
                buriedCount++;
            }
        }


        int buriedThreshold = configManager.getBuriedThreshold();
        return buriedCount >= buriedThreshold;
    }

    /**
     * Helper method to check if a material is transparent
     */
    private boolean isTransparent(Material material) {
        return material.isTransparent() || material == Material.GLASS || material == Material.TINTED_GLASS || material.name().contains("GLASS_PANE") || material.name().contains("LEAVES") || material == Material.ICE || material == Material.SLIME_BLOCK;
    }

    public void validateDecoys() {
        int removedCount = 0;
        Set<Location> veinsToRemove = new HashSet<>();


        for (Iterator<Map.Entry<Location, Material>> it = decoyMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Location, Material> entry = it.next();
            Location loc = entry.getKey();
            Material expectedType = entry.getValue();


            Block block = loc.getBlock();
            if (block.getType() != expectedType) {

                it.remove();
                decoyOwners.remove(loc);
                removedCount++;
            }
        }


        if (removedCount > 0) {
            for (Map.Entry<Location, Set<Location>> entry : veinMap.entrySet()) {
                boolean validVein = false;


                for (Location blockLoc : entry.getValue()) {
                    if (decoyMap.containsKey(blockLoc)) {
                        validVein = true;
                        break;
                    }
                }


                if (!validVein) {
                    veinsToRemove.add(entry.getKey());
                }
            }


            for (Location veinLoc : veinsToRemove) {
                veinMap.remove(veinLoc);
                originalBlockTypes.remove(veinLoc);

                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("Removed invalid vein at " + formatLocation(veinLoc));
                }
            }
        }


        if (configManager.isDebugEnabled() && removedCount > 0) {
            plugin.getLogger().info("Validated decoys: removed " + removedCount + " invalid entries and " + veinsToRemove.size() + " veins");
        }
    }

    private void scheduleDecoyValidation() {


        Bukkit.getScheduler().runTaskTimer(plugin, this::validateDecoys, 12000L, 12000L);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Scheduled decoy validation every 10 minutes");
        }
    }

    private static class OreTracker {
        private int count = 0;
        private long lastBreakTime = 0;

        public void increment(long currentTime) {
            count++;
            lastBreakTime = currentTime;
        }

        public int getCount() {
            return count;
        }

        public void reset() {
            count = 0;
        }

        /**
         * Check if this tracker has expired based on the time window
         *
         * @param currentTime     The current time in milliseconds
         * @param timeWindowTicks The config time window in ticks
         * @return True if expired, false otherwise
         */
        public boolean isExpired(long currentTime, long timeWindowTicks) {

            long timeWindowMs = timeWindowTicks * 50;
            return (currentTime - lastBreakTime) > timeWindowMs;
        }
    }

    private static class LocationWrapper {
        private final String world;
        private final int x, y, z;

        public LocationWrapper(Location loc) {
            this.world = loc.getWorld().getName();
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof LocationWrapper other)) return false;
            return this.world.equals(other.world) && this.x == other.x && this.y == other.y && this.z == other.z;
        }

        @Override
        public int hashCode() {
            return world.hashCode() ^ x ^ (y << 8) ^ (z << 16);
        }
    }

}