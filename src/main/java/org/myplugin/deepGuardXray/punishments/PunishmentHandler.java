package org.myplugin.deepGuardXray.punishments;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Interface for punishment handlers.
 * Each type of punishment should implement this interface.
 */
public interface PunishmentHandler {

    /**
     * Process a block break event for the specific punishment.
     *
     * @param player The player breaking the block
     * @param block The block being broken
     * @return true if the event should be stopped (cancelled) after this handler, false otherwise
     */
    boolean processBlockBreak(Player player, Block block);

    /**
     * Check if this punishment is active for a player.
     *
     * @param player The player to check
     * @return true if the punishment is active for this player
     */
    boolean isActive(Player player);
}