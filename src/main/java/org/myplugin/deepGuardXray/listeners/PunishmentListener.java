package org.myplugin.deepGuardXray.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.managers.DecoyManager;
import org.myplugin.deepGuardXray.managers.PunishmentManager;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PunishmentListener implements Listener {

    private final PunishmentManager punishmentManager;
    private final deepGuardXray plugin;
    private final DecoyManager decoyManager;
    private final ConfigManager configManager;

    public PunishmentListener(PunishmentManager punishmentManager, DecoyManager decoyManager, ConfigManager configManager, deepGuardXray plugin) {
        this.punishmentManager = punishmentManager;
        this.decoyManager = decoyManager;
        this.configManager = configManager;
        this.plugin = plugin;
    }

    /**
     * Handle stone vision effect by sending fake block data packets
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();


        if (event.getClickedBlock() == null) return;


        if (punishmentManager.hasStoneVision(playerId)) {
            org.bukkit.block.Block block = event.getClickedBlock();


            if (isOre(block.getType())) {

                event.setCancelled(true);


                player.sendMessage(Component.text("This block appears to be stone, despite your suspicions...", NamedTextColor.YELLOW));
            }
        }
    }

    /**
     * Handles when a player drinks milk to prevent bypassing potion effect punishments
     * Now with funny messages!
     */
    @EventHandler
    public void onPlayerConsumeMilk(PlayerItemConsumeEvent event) {

        if (event.getItem().getType() != Material.MILK_BUCKET) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();


        int punishmentLevel = punishmentManager.getPlayerPunishmentLevel(playerId);


        if (punishmentLevel <= 0) {
            return;
        }


        String funnyMessage = getFunnyMilkMessage(punishmentLevel);


        new BukkitRunnable() {
            @Override
            public void run() {

                if (!player.isOnline()) {
                    return;
                }


                player.sendMessage(Component.text(funnyMessage, NamedTextColor.GOLD));


                switch (punishmentLevel) {
                    case 1:
                        if (configManager.isPunishmentOptionEnabled(1, "mining_fatigue")) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 6000, 0));
                        }

                        if (configManager.isPunishmentOptionEnabled(1, "heavy_pickaxe")) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 12000, 1));
                        }
                        break;

                    case 2:
                        if (configManager.isPunishmentOptionEnabled(2, "xray_vision_blur")) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 6000, 0));
                        }
                        break;


                }


                if (punishmentManager.hasPermanentMiningDebuff(playerId)) {

                    if (player.getLocation().getY() < 0) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 400, 1));
                        player.sendMessage(Component.text("The mining debuff remains active below Y-level 0!", NamedTextColor.RED));
                    }
                }


                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.7f);
            }
        }.runTaskLater(plugin, 2L);
    }

    /**
     * Handle player join events for returning players
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();


        if (player.hasMetadata("public_notification")) {

            player.removeMetadata("public_notification", plugin);


            Component message = Component.text("[DeepGuardX] ", NamedTextColor.RED).append(Component.text(player.getName(), NamedTextColor.YELLOW)).append(Component.text(" has returned after being punished for suspicious mining behavior.", NamedTextColor.YELLOW));


            plugin.getServer().sendMessage(message);
        }


        UUID playerId = player.getUniqueId();
        Component reputation = punishmentManager.getMiningReputation(playerId);

        if (reputation != null && !reputation.toString().contains("Trusted Miner")) {


            player.sendMessage(Component.text("You are currently flagged as: ", NamedTextColor.RED).append(reputation));
        }


        if (punishmentManager.hasPermanentMiningDebuff(playerId)) {
            player.sendMessage(Component.text("You have a permanent mining debuff for ores when mining below Y-level 0.", NamedTextColor.RED));
        }


        if (player.hasMetadata("requires_staff_review")) {
            player.sendMessage(Component.text("You require staff approval to mine ores below Y-level 25!", NamedTextColor.RED));
        }
    }

    /**
     * Gets a random funny message for milk drinking attempts based on punishment level
     */
    private String getFunnyMilkMessage(int level) {
        String[] commonMessages = {"Nice try! This milk is suspiciously chunky... your punishment remains.", "The DeepGuardX system is lactose intolerant. Try again?", "Milk: It does a body good. Your punishment: Still in effect!", "Got Milk? Got Punishment too!", "Milk mustache detected! Punishment effects reinstated.", "That's not how you use the cow tools...", "The milk curdles as it touches your lips. Magic? No, just DeepGuardX technology!", "The server admins anticipated your dairy-based escape plan!"};

        String[] levelSpecificMessages;


        switch (level) {
            case 1:
                levelSpecificMessages = new String[]{"Your pickaxe somehow feels HEAVIER after drinking that milk...", "The milk makes your arms feel weaker. Strange!", "The warnings stick to you better than that milk mustache."};
                break;

            case 2:
                levelSpecificMessages = new String[]{"Your vision blurs even MORE after that drink. Was that really milk?", "The milk curdles in your stomach. Your paranoia intensifies!", "Did you hear that hissing sound? Oh, it's just the milk... or is it?"};
                break;

            case 3:
                levelSpecificMessages = new String[]{"Your mining license is still suspended. Milk can't wash away bureaucracy!", "The Mining Authority does not accept milk as payment for your tax debt.", "The milk carton has your picture on it: 'WANTED FOR X-RAY VIOLATIONS'"};
                break;

            case 4:
                levelSpecificMessages = new String[]{"Your cursed pickaxe laughs at your milk-drinking attempt.", "The milk turns black as you drink it. Your pickaxe glows ominously.", "The restricted areas remain restricted. Milk isn't an access key!"};
                break;

            case 5:
                levelSpecificMessages = new String[]{"This milk has been fortified with extra mining fatigue!", "Your permanent debuff laughs at your dairy-based solution.", "The stone you see is still stone. Stone-flavored milk, maybe?"};
                break;

            case 6:
                levelSpecificMessages = new String[]{"Even this milk knows you're on your last chance.", "The milk explodes into TNT particles. That can't be good.", "The server is tracking your milk consumption habits now too!"};
                break;

            default:
                levelSpecificMessages = new String[0];
                break;
        }


        if (levelSpecificMessages.length > 0 && ThreadLocalRandom.current().nextInt(10) < 3) {
            return levelSpecificMessages[ThreadLocalRandom.current().nextInt(levelSpecificMessages.length)];
        } else {
            return commonMessages[ThreadLocalRandom.current().nextInt(commonMessages.length)];
        }
    }

    /**
     * Helper method to check if a material is any ore
     */
    boolean isOre(Material material) {
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
}