package org.myplugin.deepGuardXray.punishments.handlers.Paranoia.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for location-based operations
 */
public class LocationUtils {

    /**
     * Simple holder class for Location objects to allow modification when used in anonymous inner classes
     */
    public static class LocationHolder {
        public Location location;
    }

    /**
     * Find a safe location nearby for spawning entities
     *
     * @param baseLoc The base location to start from
     * @return A safe location or null if none found
     */
    public static Location findSafeLocationNearby(Location baseLoc) {
        
        if (isSafeLocation(baseLoc)) {
            return baseLoc;
        }

        
        for (int attempt = 0; attempt < 10; attempt++) {
            
            double offsetX = ThreadLocalRandom.current().nextDouble(-3, 3);
            double offsetY = ThreadLocalRandom.current().nextDouble(-2, 2);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-3, 3);

            Location testLoc = baseLoc.clone().add(offsetX, offsetY, offsetZ);
            if (isSafeLocation(testLoc)) {
                return testLoc;
            }
        }

        
        return null;
    }

    /**
     * Check if a location is safe for entities (has air and solid ground below)
     *
     * @param loc The location to check
     * @return true if the location is safe, false otherwise
     */
    public static boolean isSafeLocation(Location loc) {
        Block block = loc.getBlock();
        Block above = block.getRelative(0, 1, 0);
        Block below = block.getRelative(0, -1, 0);

        
        return block.getType() == Material.AIR &&
                above.getType() == Material.AIR &&
                below.getType().isSolid();
    }

    /**
     * Get a random nearby location
     *
     * @param base The base location
     * @param radius The maximum radius
     * @return A random location within the radius
     */
    public static Location getRandomNearbyLocation(Location base, double radius) {
        double x = base.getX() + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * radius;
        double y = base.getY() + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * radius;
        double z = base.getZ() + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * radius;
        return new Location(base.getWorld(), x, y, z);
    }

    public static Location findValidSpawnLocationInSight(Location playerLoc, World world, double minDistance, double maxDistance) {
        
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = attempt * (Math.PI / 6); 

            
            for (double distance = minDistance; distance <= maxDistance; distance += 2) {
                double x = playerLoc.getX() + Math.cos(angle) * distance;
                double z = playerLoc.getZ() + Math.sin(angle) * distance;

                
                for (int yOffset = -2; yOffset <= 2; yOffset++) {
                    Location testLoc = new Location(world, x, playerLoc.getY() + yOffset, z);

                    if (isSafeLocation(testLoc)) {
                        return testLoc;
                    }
                }
            }
        }

        
        return null;
    }
}