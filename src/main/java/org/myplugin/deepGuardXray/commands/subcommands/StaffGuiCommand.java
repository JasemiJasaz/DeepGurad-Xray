package org.myplugin.deepGuardXray.commands.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.gui.StaffMenuGUI;

public class StaffGuiCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.").color(NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("deepguardx.gui")) {
            player.sendMessage(Component.text("You do not have permission to access the staff GUI.").color(NamedTextColor.RED));
            return true;
        }

        new StaffMenuGUI().openInventory(player);
        return true;
    }
}