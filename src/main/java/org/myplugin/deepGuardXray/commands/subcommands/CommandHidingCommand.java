package org.myplugin.deepGuardXray.commands.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;

import java.util.ArrayList;
import java.util.List;

public class CommandHidingCommand {

    private final deepGuardXray plugin;
    private final ConfigManager configManager;

    public CommandHidingCommand(deepGuardXray plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isConsole = !(sender instanceof Player);

        if (!isConsole) {
            Player player = (Player) sender;
            if (!player.isOp()) {
                player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("Only server operators can manage command hiding.", NamedTextColor.RED)));
                return true;
            }
        }

        if (args.length == 0) {
            boolean isEnabled = configManager.isCommandHidingEnabled();

            if (isConsole) {
                sender.sendMessage("Command Hiding is currently " + (isEnabled ? "ENABLED" : "DISABLED"));
                sender.sendMessage("Use /deepguardx commandhiding <enable/disable> to change this setting.");
                sender.sendMessage("Note: Changes require a server restart to take full effect!");

                if (isEnabled) {
                    sender.sendMessage("IMPORTANT: With command hiding enabled, players require the 'deepguardx.use' permission");
                    sender.sendMessage("to use any DeepGuardX commands, otherwise they'll see 'Unknown command' errors.");
                    sender.sendMessage("");
                    sender.sendMessage("LIMITATION: This only hides commands from direct use. Commands will still");
                    sender.sendMessage("be visible in tab completion, '/help', '/pl', '/plugins', etc.");
                }
            } else {
                Player player = (Player) sender;
                player.sendMessage(Component.text("Command Hiding is currently ", NamedTextColor.YELLOW).append(Component.text(isEnabled ? "ENABLED" : "DISABLED", isEnabled ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD)));
                player.sendMessage(Component.text("Use ", NamedTextColor.GRAY).append(Component.text("/deepguardx commandhiding <enable/disable>", NamedTextColor.YELLOW)).append(Component.text(" to change this setting.", NamedTextColor.GRAY)));
                player.sendMessage(Component.text("Note: ", NamedTextColor.GOLD, TextDecoration.BOLD).append(Component.text("Changes require a server restart to take full effect!", NamedTextColor.RED)));

                if (isEnabled) {
                    player.sendMessage(Component.text("⚠ IMPORTANT: ", NamedTextColor.GOLD, TextDecoration.BOLD).append(Component.text("With command hiding enabled, players require the ", NamedTextColor.WHITE)).append(Component.text("deepguardx.use", NamedTextColor.AQUA, TextDecoration.ITALIC)).append(Component.text(" permission to use any DeepGuardX commands, otherwise they'll see 'Unknown command' errors.", NamedTextColor.WHITE)));

                    player.sendMessage(Component.text("⚠ LIMITATION: ", NamedTextColor.YELLOW, TextDecoration.BOLD).append(Component.text("This only hides commands from direct use. Commands will still be visible in tab completion, '/help', '/pl', '/plugins', etc.", NamedTextColor.WHITE)));
                }
            }
            return true;
        }

        String action = args[0].toLowerCase();
        if (action.equals("enable")) {
            if (configManager.isCommandHidingEnabled()) {
                if (isConsole) {
                    sender.sendMessage("Command Hiding is already enabled.");
                } else {
                    Player player = (Player) sender;
                    player.sendMessage(Component.text("Command Hiding is already enabled.", NamedTextColor.YELLOW));
                }
                return true;
            }

            setCommandHidingEnabled(true);

            if (isConsole) {
                sender.sendMessage("Command Hiding has been ENABLED");
                sender.sendMessage("Note: A server restart is required for this change to take full effect!");
                sender.sendMessage("---------------------------------------------------");
                sender.sendMessage("IMPORTANT PERMISSION NOTICE:");
                sender.sendMessage("You MUST grant players the 'deepguardx.use' permission");
                sender.sendMessage("to allow them to use any DeepGuardX commands.");
                sender.sendMessage("Without this permission, players will see the");
                sender.sendMessage("'Unknown command' error when trying to use commands.");
                sender.sendMessage("---------------------------------------------------");
                sender.sendMessage("LIMITATION: This only hides commands from direct use.");
                sender.sendMessage("Commands will still be visible in tab completion and '/help'.");
                sender.sendMessage("To hide from those too, you need a separate permissions");
                sender.sendMessage("or command hiding plugin (like CommandFilter or TabTPS).");
                sender.sendMessage("---------------------------------------------------");
            } else {
                Player player = (Player) sender;
                player.sendMessage(Component.text("Command Hiding has been ", NamedTextColor.YELLOW).append(Component.text("ENABLED", NamedTextColor.GREEN, TextDecoration.BOLD)));
                player.sendMessage(Component.text("Note: ", NamedTextColor.GOLD, TextDecoration.BOLD).append(Component.text("A server restart is required for this change to take full effect!", NamedTextColor.RED)));

                player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
                player.sendMessage(Component.text("⚠ IMPORTANT PERMISSION NOTICE:", NamedTextColor.GOLD, TextDecoration.BOLD));
                player.sendMessage(Component.text("You ", NamedTextColor.WHITE).append(Component.text("MUST", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text(" grant players the ", NamedTextColor.WHITE)).append(Component.text("deepguardx.use", NamedTextColor.AQUA, TextDecoration.ITALIC)).append(Component.text(" permission", NamedTextColor.WHITE)));
                player.sendMessage(Component.text("to allow them to use any DeepGuardX commands.", NamedTextColor.WHITE));
                player.sendMessage(Component.text("Without this permission, players will see the", NamedTextColor.WHITE));
                player.sendMessage(Component.text("'Unknown command' error when trying to use commands.", NamedTextColor.WHITE));
                player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

                player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("⚠ LIMITATION:", NamedTextColor.YELLOW, TextDecoration.BOLD));
                player.sendMessage(Component.text("This only hides commands from direct use.", NamedTextColor.WHITE));
                player.sendMessage(Component.text("Commands will still be visible in tab completion, '/help',", NamedTextColor.WHITE));
                player.sendMessage(Component.text("'/bukkit:help', '/pl', '/plugins', etc.", NamedTextColor.WHITE));
                player.sendMessage(Component.text("To hide from those sources, you need a separate permissions", NamedTextColor.WHITE));
                player.sendMessage(Component.text("or command hiding plugin (like LuckPerms).", NamedTextColor.WHITE));
                player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.YELLOW));
            }
            return true;
        } else if (action.equals("disable")) {
            if (!configManager.isCommandHidingEnabled()) {
                if (isConsole) {
                    sender.sendMessage("Command Hiding is already disabled.");
                } else {
                    Player player = (Player) sender;
                    player.sendMessage(Component.text("Command Hiding is already disabled.", NamedTextColor.YELLOW));
                }
                return true;
            }
            setCommandHidingEnabled(false);

            if (isConsole) {
                sender.sendMessage("Command Hiding has been DISABLED");
                sender.sendMessage("Note: A server restart is required for this change to take full effect!");
            } else {
                Player player = (Player) sender;
                player.sendMessage(Component.text("Command Hiding has been ", NamedTextColor.YELLOW).append(Component.text("DISABLED", NamedTextColor.RED, TextDecoration.BOLD)));
                player.sendMessage(Component.text("Note: ", NamedTextColor.GOLD, TextDecoration.BOLD).append(Component.text("A server restart is required for this change to take full effect!", NamedTextColor.RED)));
            }
            return true;
        } else {
            if (isConsole) {
                sender.sendMessage("Invalid argument: " + action + ". Use 'enable' or 'disable'.");
            } else {
                Player player = (Player) sender;
                player.sendMessage(Component.text("Invalid argument: ", NamedTextColor.RED).append(Component.text(action, NamedTextColor.GOLD)).append(Component.text(". Use ", NamedTextColor.RED)).append(Component.text("enable", NamedTextColor.YELLOW)).append(Component.text(" or ", NamedTextColor.RED)).append(Component.text("disable", NamedTextColor.YELLOW)).append(Component.text(".", NamedTextColor.RED)));
            }
            return true;
        }
    }

    private void setCommandHidingEnabled(boolean enabled) {
        configManager.setCommandHidingEnabled(enabled);
    }

    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("enable".startsWith(partial)) completions.add("enable");
            if ("disable".startsWith(partial)) completions.add("disable");
        }

        return completions;
    }
}