package org.myplugin.deepGuardXray.commands.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.myplugin.deepGuardXray.deepGuardXray;

public class ReloadCommand implements CommandExecutor {

    private final deepGuardXray plugin;

    public ReloadCommand(deepGuardXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("deepguardx.reload")) {
            sender.sendMessage(Component.text("You do not have permission to reload the plugin.").color(NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Reloading DeepGuard-XRay plugin and configuration...").color(NamedTextColor.GREEN));

        plugin.reloadConfig();
        plugin.getLogger().info("Plugin reloaded by " + sender.getName());

        sender.sendMessage(Component.text("Plugin reloaded successfully!").color(NamedTextColor.GREEN));

        return true;
    }
}