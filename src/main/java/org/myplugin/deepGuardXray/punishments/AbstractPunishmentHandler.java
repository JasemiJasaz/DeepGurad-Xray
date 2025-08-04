package org.myplugin.deepGuardXray.punishments;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;

/**
 * Abstract base class for punishment handlers with common functionality.
 */
public abstract class AbstractPunishmentHandler implements PunishmentHandler {

    protected final deepGuardXray plugin;
    protected final ConfigManager configManager;
    protected final PunishmentManager punishmentManager;

    public AbstractPunishmentHandler(deepGuardXray plugin, ConfigManager configManager, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.punishmentManager = punishmentManager;
    }

    /**
     * Helper method to check if a material is a valuable ore
     */
    protected boolean isValuableOre(Material material) {
        switch (material) {
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
            case ANCIENT_DEBRIS:
                return true;
            default:
                return false;
        }
    }

    /**
     * Helper method to check if a material is any ore
     */
    protected boolean isOre(Material material) {
        switch (material) {
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
            case NETHER_GOLD_ORE:
            case NETHER_QUARTZ_ORE:
            case ANCIENT_DEBRIS:
                return true;
            default:
                return false;
        }
    }

    /**
     * Helper method to format item names for display
     */
    protected String formatItemName(Material material) {
        String name = material.name();

        
        name = name.replace('_', ' ');

        
        name = name.toLowerCase();

        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}