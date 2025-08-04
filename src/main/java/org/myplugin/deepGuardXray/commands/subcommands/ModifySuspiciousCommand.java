package org.myplugin.deepGuardXray.commands.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.managers.SuspiciousManager;

public class ModifySuspiciousCommand implements CommandExecutor {

    private final deepGuardXray plugin;

    public ModifySuspiciousCommand(deepGuardXray plugin) {
        this.plugin = plugin;
    }

    /**
     * Command usage:
     * /deepguard points add <player> <amount>
     * /deepguard points remove <player> <amount>
     * /deepguard points set <player> <amount>
     * /deepguard points check <player>
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("deepguardx.modify")) {
            sender.sendMessage(Component.text("You do not have permission to modify player points.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            showUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("check") && args.length >= 2) {
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                sender.sendMessage(Component.text("Player not found: " + targetName).color(NamedTextColor.RED));
                return true;
            }

            int currentPoints = SuspiciousManager.getSuspiciousCounts().getOrDefault(target.getUniqueId(), 0);
            sender.sendMessage(Component.text(target.getName() + "'s suspicious points: ").color(NamedTextColor.YELLOW).append(Component.text(currentPoints).color(NamedTextColor.WHITE)));
            if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                String adminName = sender.getName();
                if (!(sender instanceof Player)) {
                    adminName = "Console";
                }

                plugin.getWebhookManager().sendStaffActionLog(adminName, "Checked suspicious points for " + target.getName(), "Current points: " + currentPoints);
            }

            return true;
        }

        if (args.length < 3) {
            showUsage(sender);
            return true;
        }

        String targetName = args[1];
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: " + args[2]).color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            sender.sendMessage(Component.text("Player not found: " + targetName).color(NamedTextColor.RED));
            return true;
        }

        int currentCount = SuspiciousManager.getSuspiciousCounts().getOrDefault(target.getUniqueId(), 0);
        int newCount = currentCount;

        switch (action) {
            case "add":
                newCount = currentCount + amount;
                break;
            case "remove":
                newCount = currentCount - amount;
                if (newCount < 0) newCount = 0;
                break;
            case "set":
                newCount = amount;
                break;
            default:
                showUsage(sender);
                return true;
        }

        SuspiciousManager.getSuspiciousCounts().put(target.getUniqueId(), newCount);

        if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
            String adminName = sender.getName();
            if (!(sender instanceof Player)) {
                adminName = "Console";
            }

            String actionDescription;
            switch (action) {
                case "add":
                    actionDescription = "Added " + amount + " suspicious points to " + target.getName();
                    break;
                case "remove":
                    actionDescription = "Removed " + amount + " suspicious points from " + target.getName();
                    break;
                case "set":
                    actionDescription = "Set " + target.getName() + "'s suspicious points to " + amount;
                    break;
                default:
                    actionDescription = "Modified " + target.getName() + "'s suspicious points";
            }

            String details = "Previous: " + currentCount + " → New: " + newCount;

            plugin.getWebhookManager().sendStaffActionLog(adminName, actionDescription, details);
        }

        sender.sendMessage(Component.text("Updated " + target.getName() + "'s suspicious points from " + currentCount + " to " + newCount + ".").color(NamedTextColor.GREEN));
        return true;
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:").color(NamedTextColor.RED));
        sender.sendMessage(Component.text("/deepguard points add <player> <amount>").color(NamedTextColor.RED));
        sender.sendMessage(Component.text("/deepguard points remove <player> <amount>").color(NamedTextColor.RED));
        sender.sendMessage(Component.text("/deepguard points set <player> <amount>").color(NamedTextColor.RED));
        sender.sendMessage(Component.text("/deepguard points check <player>").color(NamedTextColor.RED));
    }
}