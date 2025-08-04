package org.myplugin.deepGuardXray.commands.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.myplugin.deepGuardXray.deepGuardXray;

public class DebugCommand implements CommandExecutor {

    private final deepGuardXray plugin;

    public DebugCommand(deepGuardXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("deepguardx.debug")) {
            sender.sendMessage(Component.text("You do not have permission to change debug settings.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /" + label + " debug <true/false>").color(NamedTextColor.RED));
            return true;
        }

        String value = args[0].toLowerCase();
        boolean debugValue;

        if (value.equals("true")) {
            debugValue = true;
        } else if (value.equals("false")) {
            debugValue = false;
        } else {
            sender.sendMessage(Component.text("Invalid value. Use true or false.").color(NamedTextColor.RED));
            return true;
        }

        plugin.getConfig().set("debug.enabled", debugValue);

        plugin.saveConfig();
        plugin.reloadConfig();

        sender.sendMessage(Component.text("Debug mode set to " + debugValue + ".").color(NamedTextColor.GREEN));

        return true;
    }
}