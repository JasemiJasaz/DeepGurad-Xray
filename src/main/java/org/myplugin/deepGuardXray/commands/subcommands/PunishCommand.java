package org.myplugin.deepGuardXray.commands.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.deepGuardXray;

public class PunishCommand implements CommandExecutor {

    private final deepGuardXray plugin;

    public PunishCommand(deepGuardXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("deepguardx.punish")) {
            sender.sendMessage(Component.text("You don't have permission to manage punishments.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /deepguard punish <set|remove|check> <player> [level]").color(NamedTextColor.RED));
            return true;
        }

        String action = args[0].toLowerCase();
        String playerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Player " + playerName + " not found or not online.").color(NamedTextColor.RED));
            return true;
        }

        switch (action) {
            case "set":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /deepguard punish set <player> <level>").color(NamedTextColor.RED));
                    return true;
                }

                int level;
                try {
                    level = Integer.parseInt(args[2]);
                    if (level < 0 || level > 6) {
                        sender.sendMessage(Component.text("Punishment level must be between 0 and 6.").color(NamedTextColor.RED));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid level format. Must be a number between 0 and 6.").color(NamedTextColor.RED));
                    return true;
                }

                plugin.getPunishmentManager().setPunishmentLevel(targetPlayer, level);
                if (level > 0 && plugin.getConfigManager().isWebhookAlertEnabled("punishment_applied")) {
                    String adminName = sender.getName();
                    if (!(sender instanceof Player)) {
                        adminName = "Console";
                    }

                    String punishmentType;
                    switch (level) {
                        case 1:
                            punishmentType = "Warning Phase";
                            break;
                        case 2:
                            punishmentType = "Minor Consequences";
                            break;
                        case 3:
                            punishmentType = "Moderate Punishment";
                            break;
                        case 4:
                            punishmentType = "Severe Consequences";
                            break;
                        case 5:
                            punishmentType = "Critical Response";
                            break;
                        case 6:
                            punishmentType = "Maximum Enforcement";
                            break;
                        default:
                            punishmentType = "Level " + level + " Punishment";
                    }

                    plugin.getWebhookManager().sendPunishmentAlertWithAdmin(targetPlayer, level, punishmentType, adminName);
                } else if (level == 0 && plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                    String adminName = sender.getName();
                    if (!(sender instanceof Player)) {
                        adminName = "Console";
                    }

                    plugin.getWebhookManager().sendStaffActionLog(adminName, "Removed punishment from " + targetPlayer.getName(), "Set to level 0");
                }

                if (level == 0) {
                    sender.sendMessage(Component.text("Removed all punishments from " + targetPlayer.getName()).color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Applied punishment level " + level + " to " + targetPlayer.getName()).color(NamedTextColor.GREEN));
                }
                break;

            case "remove":
                int currentPunishmentLevel = plugin.getPunishmentManager().getPlayerPunishmentLevel(targetPlayer.getUniqueId());
                plugin.getPunishmentManager().removePunishment(targetPlayer);

                if (currentPunishmentLevel > 0 && plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                    String adminName = sender.getName();
                    if (!(sender instanceof Player)) {
                        adminName = "Console";
                    }

                    plugin.getWebhookManager().sendStaffActionLog(adminName, "Removed punishment from " + targetPlayer.getName(), "Previous level: " + currentPunishmentLevel);
                }

                sender.sendMessage(Component.text("Removed all punishments from " + targetPlayer.getName()).color(NamedTextColor.GREEN));
                break;

            case "check":
                int currentLevel = plugin.getPunishmentManager().getPlayerPunishmentLevel(targetPlayer.getUniqueId());

                if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                    String adminName = sender.getName();
                    if (!(sender instanceof Player)) {
                        adminName = "Console";
                    }

                    plugin.getWebhookManager().sendStaffActionLog(adminName, "Checked punishment status for " + targetPlayer.getName(), "Current level: " + (currentLevel > 0 ? currentLevel : "None"));
                }

                if (currentLevel > 0) {
                    sender.sendMessage(Component.text(targetPlayer.getName() + " is currently at punishment level " + currentLevel).color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(targetPlayer.getName() + " currently has no active punishments").color(NamedTextColor.GREEN));
                }
                break;

            default:
                sender.sendMessage(Component.text("Invalid action. Use 'set', 'remove', or 'check'.").color(NamedTextColor.RED));
                break;
        }

        return true;
    }
}