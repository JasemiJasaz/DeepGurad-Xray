package org.myplugin.deepGuardXray.punishments.handlers.Paranoia.cooldown;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages cooldowns for various paranoia effects
 */
public class CooldownManager {

    private final Map<UUID, Map<CooldownType, Long>> cooldowns = new HashMap<>();

    /**
     * Check if a player is on cooldown for a specific effect
     *
     * @param playerId The player's UUID
     * @param type     The cooldown type to check
     * @return true if the player is on cooldown, false otherwise
     */
    public boolean isOnCooldown(UUID playerId, CooldownType type) {
        if (!cooldowns.containsKey(playerId)) {
            return false;
        }

        Map<CooldownType, Long> playerCooldowns = cooldowns.get(playerId);
        if (!playerCooldowns.containsKey(type)) {
            return false;
        }

        return System.currentTimeMillis() - playerCooldowns.get(type) < type.getDuration();
    }

    /**
     * Set a cooldown for a player and effect type
     *
     * @param playerId The player's UUID
     * @param type     The cooldown type to set
     */
    public void setCooldown(UUID playerId, CooldownType type) {
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>()).put(type, System.currentTimeMillis());
    }

    /**
     * Get the remaining cooldown time in milliseconds
     *
     * @param playerId The player's UUID
     * @param type     The cooldown type to check
     * @return The remaining cooldown time in milliseconds, or 0 if not on cooldown
     */
    public long getRemainingCooldown(UUID playerId, CooldownType type) {
        if (!isOnCooldown(playerId, type)) {
            return 0;
        }

        long elapsedTime = System.currentTimeMillis() - cooldowns.get(playerId).get(type);
        return Math.max(0, type.getDuration() - elapsedTime);
    }

    /**
     * Remove a cooldown for a player and effect type
     *
     * @param playerId The player's UUID
     * @param type     The cooldown type to remove
     */
    public void removeCooldown(UUID playerId, CooldownType type) {
        if (cooldowns.containsKey(playerId)) {
            cooldowns.get(playerId).remove(type);
        }
    }

    /**
     * Clear all cooldowns for a player
     *
     * @param playerId The player's UUID
     */
    public void clearPlayerCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
    }

    /**
     * Types of cooldowns managed by this class
     */
    public enum CooldownType {
        GHOST_MOB(15000), VISUAL(8000), FAKE_DAMAGE(30000), MESSAGE(20000), SOUND(3000), NIGHTMARE(30 * 60000);

        private final long duration;

        CooldownType(long duration) {
            this.duration = duration;
        }

        public long getDuration() {
            return duration;
        }
    }
}