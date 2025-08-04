package org.myplugin.deepGuardXray.commands.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.managers.DecoyManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HighlightDecoyCommand implements CommandExecutor {

    private final deepGuardXray plugin;
    private final Map<UUID, BukkitTask> highlightTasks = new HashMap<>();
    private final Map<UUID, Integer> highlightRadius = new HashMap<>();
    private final Map<UUID, GameMode> originalGameModes = new HashMap<>();
    private final Map<UUID, Location> originalLocations = new HashMap<>();
    private final Map<UUID, Boolean> shouldReturnToOriginalLocation = new HashMap<>();
    private final Map<UUID, Boolean> inNearestMode = new HashMap<>();

    public HighlightDecoyCommand(deepGuardXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.").color(NamedTextColor.RED));
            return true;
        }

        UUID playerId = player.getUniqueId();

        if (inNearestMode.getOrDefault(playerId, false)) {
            player.sendMessage(Component.text("You are already using a highlight command. Please wait for it to complete.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            toggleHighlighting(player, false);
            return true;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("disable")) {
                disableHighlighting(player);
                return true;
            } else if (args[0].equalsIgnoreCase("nearest")) {
                if (highlightTasks.containsKey(playerId)) {
                    disableHighlighting(player);
                }
                highlightNearestDecoy(player);
                return true;
            } else if (args[0].equalsIgnoreCase("return")) {
                boolean shouldReturn = !shouldReturnToOriginalLocation.getOrDefault(playerId, false);
                shouldReturnToOriginalLocation.put(playerId, shouldReturn);
                player.sendMessage(Component.text("Return to original location on disable: " + (shouldReturn ? "ENABLED" : "DISABLED")).color(NamedTextColor.GREEN));

                return true;
            } else {
                try {
                    int radius = Integer.parseInt(args[0]);
                    if (radius < 5) radius = 5;
                    if (radius > 50) radius = 50;

                    highlightRadius.put(playerId, radius);
                    player.sendMessage(Component.text("Highlight radius set to " + radius + " blocks.").color(NamedTextColor.GREEN));


                    if (highlightTasks.containsKey(playerId)) {
                        toggleHighlighting(player, false);
                        toggleHighlighting(player, false);
                    }

                    return true;
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid radius. Please enter a number between 5 and 50.").color(NamedTextColor.RED));
                    return true;
                }
            }
        } else {

            player.sendMessage(Component.text("Usage:").color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/" + label + " highlight").color(NamedTextColor.GOLD).append(Component.text(" - Toggle highlighting of decoy veins").color(NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("/" + label + " highlight <radius>").color(NamedTextColor.GOLD).append(Component.text(" - Set highlight radius (5-50 blocks)").color(NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("/" + label + " highlight off").color(NamedTextColor.GOLD).append(Component.text(" - Disable highlighting").color(NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("/" + label + " highlight nearest").color(NamedTextColor.GOLD).append(Component.text(" - Highlight only nearest decoy vein").color(NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("/" + label + " highlight return").color(NamedTextColor.GOLD).append(Component.text(" - Toggle returning to original location").color(NamedTextColor.YELLOW)));
            return true;
        }
    }

    private void toggleHighlighting(Player player, boolean isNearestMode) {
        UUID playerId = player.getUniqueId();

        if (highlightTasks.containsKey(playerId)) {
            disableHighlighting(player);
        } else {
            enableHighlighting(player, isNearestMode);
        }
    }

    private void enableHighlighting(Player player, boolean isNearestMode) {
        UUID playerId = player.getUniqueId();


        disableHighlighting(player);


        GameMode originalMode = player.getGameMode();
        Location originalLoc = player.getLocation().clone();
        originalGameModes.put(playerId, originalMode);
        originalLocations.put(playerId, originalLoc);


        inNearestMode.put(playerId, isNearestMode);

        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(Component.text("Switched to spectator mode. You can now see through blocks.").color(NamedTextColor.YELLOW));


        int radius = highlightRadius.getOrDefault(playerId, 30);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {

                if (!player.isOnline()) {
                    cancel();
                    cleanupPlayer(playerId);
                    return;
                }

                DecoyManager decoyManager = plugin.getDecoyManager();


                Map<Location, Set<Location>> decoyVeins = decoyManager.getAllDecoyVeins();

                if (decoyVeins.isEmpty()) {
                    if (getTaskId() % 40 == 0) {
                        player.sendMessage(Component.text("No decoy veins found in range.").color(NamedTextColor.YELLOW));
                    }
                    return;
                }

                int highlightedVeins = 0;
                int totalBlocks = 0;


                Location playerLoc = player.getLocation();
                for (Map.Entry<Location, Set<Location>> entry : decoyVeins.entrySet()) {
                    Location primaryLoc = entry.getKey();
                    Set<Location> veinBlocks = entry.getValue();


                    if (primaryLoc.getWorld() != playerLoc.getWorld() || primaryLoc.distance(playerLoc) > radius) {
                        continue;
                    }

                    boolean validVein = false;


                    for (Location blockLoc : veinBlocks) {

                        if (decoyManager.isDecoy(blockLoc)) {

                            highlightBlock(player, blockLoc);
                            totalBlocks++;
                            validVein = true;
                        }
                    }

                    if (validVein) {
                        highlightedVeins++;
                    }
                }

                if (highlightedVeins > 0) {

                    if (getTaskId() % 40 == 0) {
                        player.sendMessage(Component.text("Highlighting " + totalBlocks + " decoy blocks in " + highlightedVeins + " veins within " + radius + " blocks.").color(NamedTextColor.GREEN));
                    }
                } else {
                    if (getTaskId() % 40 == 0) {
                        player.sendMessage(Component.text("No decoy veins found within " + radius + " blocks. Move around to find some.").color(NamedTextColor.YELLOW));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 15L);

        highlightTasks.put(playerId, task);
        player.sendMessage(Component.text("Decoy vein highlighting enabled with radius " + highlightRadius.getOrDefault(playerId, 30) + " blocks.").color(NamedTextColor.GREEN));


        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
                if (event.getPlayer().getUniqueId().equals(playerId)) {
                    cleanupPlayer(playerId);
                }
            }
        }, plugin);
    }

    private void disableHighlighting(Player player) {
        UUID playerId = player.getUniqueId();


        BukkitTask task = highlightTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }


        GameMode originalMode = originalGameModes.remove(playerId);
        if (originalMode != null && player.isOnline()) {
            player.setGameMode(originalMode);
            player.sendMessage(Component.text("Restored your original game mode (" + formatGameMode(originalMode) + ").").color(NamedTextColor.YELLOW));
        }


        boolean shouldReturn = shouldReturnToOriginalLocation.getOrDefault(playerId, false) || inNearestMode.getOrDefault(playerId, false);

        if (shouldReturn && player.isOnline()) {
            Location originalLoc = originalLocations.get(playerId);
            if (originalLoc != null) {
                player.teleport(originalLoc);
                player.sendMessage(Component.text("Returned you to your original location.").color(NamedTextColor.YELLOW));
            }
        }


        inNearestMode.remove(playerId);
        originalLocations.remove(playerId);

        if (player.isOnline()) {
            player.sendMessage(Component.text("Decoy vein highlighting disabled.").color(NamedTextColor.YELLOW));
        }
    }

    private void cleanupPlayer(UUID playerId) {

        BukkitTask quitTask = highlightTasks.remove(playerId);
        if (quitTask != null) {
            quitTask.cancel();
        }


        originalGameModes.remove(playerId);
        originalLocations.remove(playerId);
        inNearestMode.remove(playerId);
        highlightRadius.remove(playerId);
    }

    private String formatGameMode(GameMode mode) {
        switch (mode) {
            case CREATIVE:
                return "Creative";
            case SURVIVAL:
                return "Survival";
            case ADVENTURE:
                return "Adventure";
            case SPECTATOR:
                return "Spectator";
            default:
                return mode.toString();
        }
    }

    private void highlightNearestDecoy(Player player) {
        UUID playerId = player.getUniqueId();


        inNearestMode.put(playerId, true);


        GameMode originalMode = player.getGameMode();
        Location originalLocation = player.getLocation().clone();
        originalGameModes.put(playerId, originalMode);
        originalLocations.put(playerId, originalLocation);

        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(Component.text("Switched to spectator mode temporarily.").color(NamedTextColor.YELLOW));

        DecoyManager decoyManager = plugin.getDecoyManager();
        Location playerLoc = player.getLocation();


        Map<Location, Set<Location>> decoyVeins = decoyManager.getAllDecoyVeins();

        if (decoyVeins.isEmpty()) {
            player.sendMessage(Component.text("No decoy veins found.").color(NamedTextColor.YELLOW));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cleanupAndRestore(player);
            }, 40L);
            return;
        }

        Location nearestVeinLoc = null;
        double nearestDistance = Double.MAX_VALUE;
        Set<Location> nearestVeinBlocks = null;


        for (Map.Entry<Location, Set<Location>> entry : decoyVeins.entrySet()) {
            Location veinLoc = entry.getKey();
            Set<Location> veinBlocks = entry.getValue();

            if (veinLoc.getWorld() != playerLoc.getWorld()) {
                continue;
            }


            boolean validVein = false;
            for (Location blockLoc : veinBlocks) {
                if (decoyManager.isDecoy(blockLoc)) {
                    validVein = true;
                    break;
                }
            }

            if (!validVein) {
                continue;
            }

            double distance = veinLoc.distance(playerLoc);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestVeinLoc = veinLoc;
                nearestVeinBlocks = veinBlocks;
            }
        }

        if (nearestVeinLoc == null || nearestVeinBlocks == null) {
            player.sendMessage(Component.text("No valid decoy veins found in your world.").color(NamedTextColor.YELLOW));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cleanupAndRestore(player);
            }, 40L);
            return;
        }


        player.teleport(nearestVeinLoc);


        final Location veinLocation = nearestVeinLoc;
        final Set<Location> finalNearestVeinBlocks = nearestVeinBlocks;
        final DecoyManager finalDecoyManager = decoyManager;


        BukkitTask highlightTask = new BukkitRunnable() {
            private int count = 0;

            @Override
            public void run() {
                if (count >= 10 || !player.isOnline()) {

                    if (player.isOnline()) {
                        cleanupAndRestore(player);
                    } else {
                        cleanupPlayer(playerId);
                    }
                    this.cancel();
                    return;
                }


                if (count == 5) {
                    player.sendMessage(Component.text("You will be returned to your original location in 5 seconds...").color(NamedTextColor.YELLOW));
                }


                int validBlocks = 0;
                for (Location blockLoc : finalNearestVeinBlocks) {
                    if (finalDecoyManager.isDecoy(blockLoc)) {
                        highlightBlock(player, blockLoc);
                        validBlocks++;
                    }
                }


                if (validBlocks == 0 && count > 0) {
                    player.sendMessage(Component.text("No decoy blocks remain in this vein. Returning you now.").color(NamedTextColor.YELLOW));
                    cleanupAndRestore(player);
                    this.cancel();
                    return;
                }

                count++;
            }
        }.runTaskTimer(plugin, 0L, 20L);


        highlightTasks.put(playerId, highlightTask);

        player.sendMessage(Component.text("Teleported to and highlighting nearest decoy vein at " + formatLocation(nearestVeinLoc) + " (" + String.format("%.1f", nearestDistance) + " blocks away)").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("You will be returned to your original location and game mode in 10 seconds.").color(NamedTextColor.YELLOW));
    }

    private void cleanupAndRestore(Player player) {
        UUID playerId = player.getUniqueId();


        GameMode originalMode = originalGameModes.getOrDefault(playerId, GameMode.SURVIVAL);
        Location originalLoc = originalLocations.getOrDefault(playerId, player.getLocation());


        BukkitTask task = highlightTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }

        originalGameModes.remove(playerId);
        originalLocations.remove(playerId);
        inNearestMode.remove(playerId);


        player.setGameMode(originalMode);
        player.teleport(originalLoc);
        player.sendMessage(Component.text("Returned you to your original game mode and location.").color(NamedTextColor.YELLOW));
    }

    private void highlightBlock(Player player, Location location) {
        World world = location.getWorld();
        if (world == null) return;


        world.spawnParticle(Particle.FLAME, location.clone().add(0.5, 0.5, 0.5), 5, 0.4, 0.4, 0.4, 0.01);


        world.spawnParticle(Particle.ASH, location.clone().add(0.5, 0.5, 0.5), 1, 0, 0, 0, 1);
    }

    private String formatLocation(Location loc) {
        return loc.getWorld().getName() + " [" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]";
    }
}