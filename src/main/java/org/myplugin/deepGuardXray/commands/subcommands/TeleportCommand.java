package org.myplugin.deepGuardXray.commands.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.utils.LocationUtils;

public class TeleportCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.").color(NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("deepguardx.teleport")) {
            player.sendMessage(Component.text("You do not have permission to teleport.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 1) {
            String targetPlayerName = args[0];
            Player targetPlayer = player.getServer().getPlayer(targetPlayerName);

            if (targetPlayer == null || !targetPlayer.isOnline()) {
                player.sendMessage(Component.text("Player " + targetPlayerName + " is not online.").color(NamedTextColor.RED));
                return true;
            }

            player.teleport(targetPlayer.getLocation());
            player.sendMessage(Component.text("Teleported to " + targetPlayer.getName() + " at " + LocationUtils.formatLocation(targetPlayer.getLocation())).color(NamedTextColor.GREEN));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /<main> teleport <player> or /<main> teleport <x> <y> <z>").color(NamedTextColor.RED));
            return true;
        }

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            Location loc = player.getWorld().getBlockAt(x, y, z).getLocation();
            player.teleport(loc);
            player.sendMessage(Component.text("Teleported to " + LocationUtils.formatLocation(loc)).color(NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid coordinates.").color(NamedTextColor.RED));
        }

        return true;
    }
}