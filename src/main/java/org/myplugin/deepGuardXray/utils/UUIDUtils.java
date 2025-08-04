package org.myplugin.deepGuardXray.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class UUIDUtils {

    /**
     * Retrieves the UUID for the given player name.
     * In online mode, this returns the real UUID.
     * In offline mode, Bukkit generates a name-based UUID.
     *
     * @param playerName the player's name
     * @return the player's UUID
     */
    public static UUID getUUID(String playerName) {

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        return offlinePlayer.getUniqueId();
    }

    /**
     * Alternatively, if you want a custom implementation for offline mode,
     * you can use a name-based UUID generation:
     */
    public static UUID getOfflineUUID(String playerName) {
        return UUID.nameUUIDFromBytes(playerName.getBytes());
    }
}
