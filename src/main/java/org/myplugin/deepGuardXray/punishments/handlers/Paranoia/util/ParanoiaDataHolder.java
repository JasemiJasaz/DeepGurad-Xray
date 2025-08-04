package org.myplugin.deepGuardXray.punishments.handlers.Paranoia.util;

import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Central data holder for various paranoia effect data
 */
public class ParanoiaDataHolder {

    
    private final Map<UUID, List<Entity>> activeGhostEntities = new HashMap<>();

    /**
     * Adds a ghost entity to a player's tracked entities
     *
     * @param playerId The player's UUID
     * @param entity The entity to track
     */
    public void addGhostEntity(UUID playerId, Entity entity) {
        if (!activeGhostEntities.containsKey(playerId)) {
            activeGhostEntities.put(playerId, new ArrayList<>());
        }
        activeGhostEntities.get(playerId).add(entity);
    }

    /**
     * Removes a ghost entity from tracking
     *
     * @param playerId The player's UUID
     * @param entity The entity to remove
     */
    public void removeGhostEntity(UUID playerId, Entity entity) {
        if (activeGhostEntities.containsKey(playerId)) {
            activeGhostEntities.get(playerId).remove(entity);
        }
    }

    /**
     * Gets all active ghost entities for a player
     *
     * @param playerId The player's UUID
     * @return List of entities or empty list if none
     */
    public List<Entity> getPlayerGhostEntities(UUID playerId) {
        return activeGhostEntities.getOrDefault(playerId, new ArrayList<>());
    }

    /**
     * Gets all players who have ghost entities
     *
     * @return Map of player UUIDs to their ghost entities
     */
    public Map<UUID, List<Entity>> getAllGhostEntities() {
        return activeGhostEntities;
    }

    /**
     * Clean up ghost entities for a player
     *
     * @param playerId The player's UUID
     */
    public void cleanupPlayerGhostEntities(UUID playerId) {
        if (activeGhostEntities.containsKey(playerId)) {
            List<Entity> entities = activeGhostEntities.get(playerId);
            for (Entity entity : entities) {
                if (entity != null && !entity.isDead()) {
                    entity.remove();
                }
            }
            activeGhostEntities.remove(playerId);
        }
    }

    /**
     * Clean up all ghost entities
     */
    public void cleanupAllGhostEntities() {
        for (List<Entity> entities : activeGhostEntities.values()) {
            for (Entity entity : entities) {
                if (entity != null && !entity.isDead()) {
                    entity.remove();
                }
            }
        }
        activeGhostEntities.clear();
    }

    /**
     * Remove dead entities from tracking
     */
    public void removeDeadEntities() {
        for (UUID playerId : new ArrayList<>(activeGhostEntities.keySet())) {
            List<Entity> entities = activeGhostEntities.get(playerId);
            entities.removeIf(entity -> entity == null || entity.isDead());

            
            if (entities.isEmpty()) {
                activeGhostEntities.remove(playerId);
            }
        }
    }
}