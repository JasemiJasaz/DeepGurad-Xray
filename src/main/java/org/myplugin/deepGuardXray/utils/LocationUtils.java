package org.myplugin.deepGuardXray.utils;

import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtils {

    public static String formatLocation(Location loc) {
        return "(X " + loc.getBlockX() + " Y " + loc.getBlockY() + " Z " + loc.getBlockZ() + ")";
    }

    public static String getFriendlyWorldName(World world) {
        String name = world.getName();
        if (name.equalsIgnoreCase("world")) {
            return "Overworld";
        } else if (name.equalsIgnoreCase("world_nether")) {
            return "Nether";
        } else if (name.equalsIgnoreCase("world_the_end")) {
            return "The End";
        }
        return name;
    }
}