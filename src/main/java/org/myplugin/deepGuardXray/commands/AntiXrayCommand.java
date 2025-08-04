package org.myplugin.deepGuardXray.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.commands.subcommands.*;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.utils.UpdateChecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AntiXrayCommand implements CommandExecutor, TabCompleter {

    private final deepGuardXray plugin;
    private final TeleportCommand teleportCommand;
    private final AlertCommand alertCommand;
    private final ReloadCommand reloadCommand;
    private final DebugCommand debugCommand;
    private final StaffGuiCommand staffguiCommand;
    private final ModifySuspiciousCommand pointsCommand;
    private final HighlightDecoyCommand highlightCommand;
    private final PunishCommand punishCommand;
    private final UpdateCommand updateCommand;
    private final AppealCommand appealCommand;
    private final MLCommand mlCommand;
    private final CommandHidingCommand commandHidingCommand;

    public AntiXrayCommand(deepGuardXray plugin, UpdateChecker updateChecker) {
        this.plugin = plugin;
        this.teleportCommand = new TeleportCommand();
        this.alertCommand = new AlertCommand(plugin);
        this.reloadCommand = new ReloadCommand(plugin);
        this.debugCommand = new DebugCommand(plugin);
        this.staffguiCommand = new StaffGuiCommand();
        this.pointsCommand = new ModifySuspiciousCommand(plugin);
        this.highlightCommand = new HighlightDecoyCommand(plugin);
        this.punishCommand = new PunishCommand(plugin);
        this.updateCommand = new UpdateCommand(plugin, updateChecker);
        this.appealCommand = new AppealCommand(plugin, plugin.getAppealManager());
        this.mlCommand = new MLCommand(plugin, plugin.getMLManager());
        this.commandHidingCommand = new CommandHidingCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            List<String> available = new ArrayList<>();
            if (player.hasPermission("deepguardx.teleport")) available.add("teleport <x> <y> <z>");
            if (player.hasPermission("deepguardx.togglealert")) available.add("togglealert");
            if (player.hasPermission("deepguardx.reload")) available.add("reload");
            if (player.hasPermission("deepguardx.debug")) available.add("debug <true/false>");
            if (player.hasPermission("deepguardx.gui")) available.add("staff GUI menu");
            if (player.hasPermission("deepguardx.punish")) available.add("punish <set|remove|check> <player> [level]");
            if (player.hasPermission("deepguardx.modify"))
                available.add("points <add|remove|set|check> <player> [amount]");
            if (player.hasPermission("deepguardx.highlight")) available.add("highlight [radius|off|nearest]");
            if (player.hasPermission("deepguardx.autoupdate")) available.add("update <check|auto>");
            if (player.hasPermission("deepguardx.ml.train") || player.hasPermission("deepguardx.ml.analyze") || player.hasPermission("deepguardx.ml.toggle") || player.hasPermission("deepguardx.ml.status")) {
                available.add("ml <train|analyze|enable|disable|status>");
            }

            if (player.hasPermission("deepguardx.staff")) {
                available.add("commandhiding <enable|disable>");
            }

            available.add("appeal");

            if (available.isEmpty()) {
                String version = plugin.getDescription().getVersion();
                player.sendMessage(Component.text("DeepGuard-XRay Plugin ", NamedTextColor.GREEN, TextDecoration.BOLD).append(Component.text("v" + version, NamedTextColor.AQUA, TextDecoration.BOLD)).hoverEvent(HoverEvent.showText(Component.text("Running version " + version))));
            } else {
                player.sendMessage(Component.text("▶ ", NamedTextColor.GOLD).append(Component.text("DeepGuard-XRay ", NamedTextColor.GREEN, TextDecoration.BOLD)).append(Component.text("Commands", NamedTextColor.YELLOW)).append(Component.text(" ◀", NamedTextColor.GOLD)));

                for (String cmd : available) {
                    String commandText = "/" + label + " " + cmd;
                    String[] parts = cmd.split(" ", 2);
                    String baseCmd = parts[0];
                    String cmdArgs = parts.length > 1 ? " " + parts[1] : "";

                    Component message = Component.text("  • ", NamedTextColor.GRAY).append(Component.text("/" + label + " ", NamedTextColor.GRAY)).append(Component.text(baseCmd, NamedTextColor.GREEN, TextDecoration.BOLD)).append(Component.text(cmdArgs, NamedTextColor.YELLOW));

                    if (!cmd.contains("[") && !cmd.contains("<")) {
                        message = message.clickEvent(ClickEvent.runCommand(commandText)).hoverEvent(HoverEvent.showText(Component.text("Click to execute: " + commandText)));
                    } else {
                        message = message.clickEvent(ClickEvent.suggestCommand("/" + label + " " + baseCmd + " ")).hoverEvent(HoverEvent.showText(Component.text("Click to suggest: /" + label + " " + baseCmd)));
                    }

                    player.sendMessage(message);
                }
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "commandhiding":
                if (!player.isOp()) {
                    player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("Only server operators can manage command hiding.", NamedTextColor.RED)));
                    return true;
                }
                return commandHidingCommand.onCommand(sender, command, label, subArgs);
            case "teleport":
                if (!player.hasPermission("deepguardx.teleport")) {
                    player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to use teleport.", NamedTextColor.RED)));
                    return true;
                }
                return teleportCommand.onCommand(sender, command, label, subArgs);
            case "togglealert":
                if (!player.hasPermission("deepguardx.togglealert")) {
                    player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to toggle alerts.", NamedTextColor.RED)));
                    return true;
                }
                return alertCommand.onCommand(sender, command, label, subArgs);
            case "reload":
                if (!player.hasPermission("deepguardx.reload")) {
                    player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to reload the plugin.", NamedTextColor.RED)));
                    return true;
                }
                return reloadCommand.onCommand(sender, command, label, subArgs);
            case "debug":
                if (!player.hasPermission("deepguardx.debug")) {
                    player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to change debug settings.", NamedTextColor.RED)));
                    return true;
                }
                return debugCommand.onCommand(sender, command, label, subArgs);
            case "staff":
                if (!player.hasPermission("deepguardx.gui")) {
                    player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to access Staff GUI settings.", NamedTextColor.RED)));
                    return true;
                }
                return staffguiCommand.onCommand(sender, command, label, subArgs);
            case "punish":
                if (!player.hasPermission("deepguardx.punish")) {
                    player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to manage punishments.", NamedTextColor.RED)));
                    return true;
                }
                return punishCommand.onCommand(sender, command, label, subArgs);
            case "points":
                if (!player.hasPermission("deepguardx.modify")) {
                    player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to modify player points.", NamedTextColor.RED)));
                    return true;
                }
                return pointsCommand.onCommand(sender, command, label, subArgs);
            case "highlight":
                if (!player.hasPermission("deepguardx.highlight")) {
                    player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to highlight decoy veins.", NamedTextColor.RED)));
                    return true;
                }
                return highlightCommand.onCommand(sender, command, label, subArgs);
            case "update":
                if (!player.hasPermission("deepguardx.autoupdate")) {
                    player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to auto-update the plugin.", NamedTextColor.RED)));
                    return true;
                }
                return updateCommand.onCommand(sender, command, label, subArgs);
            case "appeal":
                return appealCommand.onCommand(sender, command, label, subArgs);
            case "ml":
                if (!player.hasPermission("deepguardx.ml.train") && !player.hasPermission("deepguardx.ml.analyze") && !player.hasPermission("deepguardx.ml.toggle") && !player.hasPermission("deepguardx.ml.status")) {
                    player.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to use ML commands.", NamedTextColor.RED)));
                    return true;
                }
                return mlCommand.onCommand(sender, command, label, subArgs);
            default:
                player.sendMessage(Component.text("❌ ", NamedTextColor.RED).append(Component.text("Unknown command: ", NamedTextColor.RED)).append(Component.text(subCommand, NamedTextColor.GOLD)).append(Component.text(". Use ", NamedTextColor.RED)).append(Component.text("/" + label, NamedTextColor.YELLOW, TextDecoration.BOLD).clickEvent(ClickEvent.runCommand("/" + label)).hoverEvent(HoverEvent.showText(Component.text("Click to run: /" + label)))).append(Component.text(" for help.", NamedTextColor.RED)));
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return suggestions;
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if (sender.isOp() && "commandhiding".startsWith(partial)) suggestions.add("commandhiding");
            if (sender.hasPermission("deepguardx.teleport") && "teleport".startsWith(partial))
                suggestions.add("teleport");
            if (sender.hasPermission("deepguardx.togglealert") && "togglealert".startsWith(partial))
                suggestions.add("togglealert");
            if (sender.hasPermission("deepguardx.reload") && "reload".startsWith(partial)) suggestions.add("reload");
            if (sender.hasPermission("deepguardx.debug") && "debug".startsWith(partial)) suggestions.add("debug");
            if (sender.hasPermission("deepguardx.gui") && "staff".startsWith(partial)) suggestions.add("staff");
            if (sender.hasPermission("deepguardx.punish") && "punish".startsWith(partial)) suggestions.add("punish");
            if (sender.hasPermission("deepguardx.modify") && "points".startsWith(partial)) suggestions.add("points");
            if (sender.hasPermission("deepguardx.highlight") && "highlight".startsWith(partial))
                suggestions.add("highlight");
            if (sender.hasPermission("deepguardx.autoupdate") && "update".startsWith(partial))
                suggestions.add("update");
            if ((sender.hasPermission("deepguardx.ml.train") || sender.hasPermission("deepguardx.ml.analyze") || sender.hasPermission("deepguardx.ml.toggle") || sender.hasPermission("deepguardx.ml.status")) && "ml".startsWith(partial)) {
                suggestions.add("ml");
            }

            if ("appeal".startsWith(partial)) suggestions.add("appeal");
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("ml")) {
            return mlCommand.getTabCompletions(sender, Arrays.copyOfRange(args, 1, args.length));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("update")) {
            if (sender.hasPermission("deepguardx.autoupdate")) {
                String partial = args[1].toLowerCase();
                if ("check".startsWith(partial)) suggestions.add("check");
                if ("auto".startsWith(partial)) suggestions.add("auto");
                if ("apply".startsWith(partial)) suggestions.add("apply");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("punish")) {
            if (sender.hasPermission("deepguardx.punish")) {
                String partial = args[1].toLowerCase();
                if ("set".startsWith(partial)) suggestions.add("set");
                if ("remove".startsWith(partial)) suggestions.add("remove");
                if ("check".startsWith(partial)) suggestions.add("check");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("punish")) {
            if (sender.hasPermission("deepguardx.punish")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        suggestions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("punish") && args[1].equalsIgnoreCase("set")) {
            if (sender.hasPermission("deepguardx.punish")) {
                String partial = args[3].toLowerCase();
                for (int i = 0; i <= 6; i++) {
                    if (String.valueOf(i).startsWith(partial)) {
                        suggestions.add(String.valueOf(i));
                    }
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("highlight")) {
            if (sender.hasPermission("deepguardx.highlight")) {
                suggestions.add("off");
                suggestions.add("nearest");
                suggestions.add("return");
                suggestions.add("20");
                suggestions.add("30");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("points")) {
            if (sender.hasPermission("deepguardx.modify")) {
                suggestions.add("add");
                suggestions.add("remove");
                suggestions.add("set");
                suggestions.add("check");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("points")) {
            if (sender.hasPermission("deepguardx.modify")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        suggestions.add(player.getName());
                    }
                }
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("commandhiding")) {
            if (sender.isOp()) {
                return commandHidingCommand.getTabCompletions(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return suggestions;
    }
}