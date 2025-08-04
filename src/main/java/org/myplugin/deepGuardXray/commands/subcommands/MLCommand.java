package org.myplugin.deepGuardXray.commands.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.ml.MLManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Handles all machine learning related subcommands for the anti-xray system
 */
public class MLCommand implements CommandExecutor {
    private final deepGuardXray plugin;
    private final MLManager mlManager;

    public MLCommand(deepGuardXray plugin, MLManager mlManager) {
        this.plugin = plugin;
        this.mlManager = mlManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "train":
                return handleTrainCommand(player, subArgs);

            case "analyze":
                return handleAnalyzeCommand(player, subArgs);

            case "report":
                return handleReportCommand(player, subArgs);

            case "enable":
                return handleEnableCommand(player, true);

            case "disable":
                return handleEnableCommand(player, false);

            case "status":
                return handleStatusCommand(player);

            default:
                sendHelp(player);
                return true;
        }
    }

    /**
     * Send help message for ML commands
     */
    private void sendHelp(Player player) {
        player.sendMessage(Component.text("▶ ", NamedTextColor.GOLD).append(Component.text("DeepGuard-XRay ML ", NamedTextColor.GREEN, TextDecoration.BOLD)).append(Component.text("Commands", NamedTextColor.YELLOW)).append(Component.text(" ◀", NamedTextColor.GOLD)));

        if (player.hasPermission("deepguardx.ml.train")) {
            player.sendMessage(Component.text("  • ", NamedTextColor.GRAY).append(Component.text("/deepguardx ml train ", NamedTextColor.GRAY)).append(Component.text("train", NamedTextColor.GREEN, TextDecoration.BOLD)).append(Component.text(" <player> <cheater|normal>", NamedTextColor.YELLOW)).clickEvent(ClickEvent.suggestCommand("/deepguardx ml train ")).hoverEvent(HoverEvent.showText(Component.text("Click to suggest command"))));
        }

        if (player.hasPermission("deepguardx.ml.analyze")) {
            player.sendMessage(Component.text("  • ", NamedTextColor.GRAY).append(Component.text("/deepguardx ml ", NamedTextColor.GRAY)).append(Component.text("analyze", NamedTextColor.GREEN, TextDecoration.BOLD)).append(Component.text(" <player>", NamedTextColor.YELLOW)).clickEvent(ClickEvent.suggestCommand("/deepguardx ml analyze ")).hoverEvent(HoverEvent.showText(Component.text("Click to suggest command"))));
        }

        if (player.hasPermission("deepguardx.ml.report")) {
            player.sendMessage(Component.text("  • ", NamedTextColor.GRAY).append(Component.text("/deepguardx ml ", NamedTextColor.GRAY)).append(Component.text("report", NamedTextColor.GREEN, TextDecoration.BOLD)).append(Component.text(" <player>", NamedTextColor.YELLOW)).clickEvent(ClickEvent.suggestCommand("/deepguardx ml report ")).hoverEvent(HoverEvent.showText(Component.text("View detailed analysis report for a player"))));
        }

        if (player.hasPermission("deepguardx.ml.toggle")) {
            player.sendMessage(Component.text("  • ", NamedTextColor.GRAY).append(Component.text("/deepguardx ml ", NamedTextColor.GRAY)).append(Component.text("enable", NamedTextColor.GREEN, TextDecoration.BOLD)).clickEvent(ClickEvent.runCommand("/deepguardx ml enable")).hoverEvent(HoverEvent.showText(Component.text("Click to enable ML system"))));

            player.sendMessage(Component.text("  • ", NamedTextColor.GRAY).append(Component.text("/deepguardx ml ", NamedTextColor.GRAY)).append(Component.text("disable", NamedTextColor.GREEN, TextDecoration.BOLD)).clickEvent(ClickEvent.runCommand("/deepguardx ml disable")).hoverEvent(HoverEvent.showText(Component.text("Click to disable ML system"))));
        }

        if (player.hasPermission("deepguardx.ml.status")) {
            player.sendMessage(Component.text("  • ", NamedTextColor.GRAY).append(Component.text("/deepguardx ml ", NamedTextColor.GRAY)).append(Component.text("status", NamedTextColor.GREEN, TextDecoration.BOLD)).clickEvent(ClickEvent.runCommand("/deepguardx ml status")).hoverEvent(HoverEvent.showText(Component.text("Click to check ML system status"))));
        }
    }

    /**
     * Handle train command - collect training data from a player
     */
    private boolean handleTrainCommand(Player sender, String[] args) {
        if (!sender.hasPermission("deepguardx.ml.train")) {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to train the ML system.", NamedTextColor.RED)));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Usage: ", NamedTextColor.RED)).append(Component.text("/deepguardx ml train <player> <cheater|normal>", NamedTextColor.YELLOW)));
            return true;
        }

        String playerName = args[0];
        String label = args[1].toLowerCase();

        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Player not found: ", NamedTextColor.RED)).append(Component.text(playerName, NamedTextColor.YELLOW)).append(Component.text(" is not online.", NamedTextColor.RED)));
            return true;
        }

        if (!label.equals("cheater") && !label.equals("normal")) {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Invalid label. ", NamedTextColor.RED)).append(Component.text("Use 'cheater' or 'normal'", NamedTextColor.YELLOW)));
            return true;
        }

        boolean isCheater = label.equals("cheater");
        mlManager.startTraining(targetPlayer, isCheater);

        sender.sendMessage(Component.text("✓ ", NamedTextColor.GREEN).append(Component.text("Started collecting training data from ", NamedTextColor.GREEN)).append(Component.text(playerName, NamedTextColor.YELLOW, TextDecoration.BOLD)).append(Component.text(" as ", NamedTextColor.GREEN)).append(Component.text(label, isCheater ? NamedTextColor.RED : NamedTextColor.GREEN, TextDecoration.BOLD)));

        return true;
    }

    /**
     * Handle analyze command - analyze a player's behavior
     */
    private boolean handleAnalyzeCommand(Player sender, String[] args) {
        if (!sender.hasPermission("deepguardx.ml.analyze")) {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to analyze players.", NamedTextColor.RED)));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Usage: ", NamedTextColor.RED)).append(Component.text("/deepguardx ml analyze <player>", NamedTextColor.YELLOW)));
            return true;
        }

        String playerName = args[0];
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer == null) {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Player not found: ", NamedTextColor.RED)).append(Component.text(playerName, NamedTextColor.YELLOW)).append(Component.text(" is not online.", NamedTextColor.RED)));
            return true;
        }

        mlManager.startAnalysis(targetPlayer);

        sender.sendMessage(Component.text("🔍 ", NamedTextColor.AQUA).append(Component.text("Started analyzing ", NamedTextColor.AQUA)).append(Component.text(playerName, NamedTextColor.YELLOW, TextDecoration.BOLD)).append(Component.text(" for X-ray behavior. Results will be shown in a few minutes.", NamedTextColor.AQUA)));

        return true;
    }

    /**
     * Handle report command - show detailed analysis results for a player
     */
    private boolean handleReportCommand(Player sender, String[] args) {
        if (!sender.hasPermission("deepguardx.ml.report")) {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to view analysis reports.", NamedTextColor.RED)));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Usage: ", NamedTextColor.RED)).append(Component.text("/deepguardx ml report <player>", NamedTextColor.YELLOW)));
            return true;
        }

        String playerName = args[0];
        Player targetPlayer = Bukkit.getPlayer(playerName);
        UUID targetId = null;

        if (targetPlayer != null) {
            targetId = targetPlayer.getUniqueId();
        } else {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Player not found or no analysis data available: ", NamedTextColor.RED)).append(Component.text(playerName, NamedTextColor.YELLOW)));
            return true;
        }

        List<String> reportLines = mlManager.getSimplifiedReport(targetId);

        sender.sendMessage(Component.text("▶ ", NamedTextColor.GOLD).append(Component.text("X-Ray Detection Report: ", NamedTextColor.AQUA, TextDecoration.BOLD)).append(Component.text(playerName, NamedTextColor.YELLOW, TextDecoration.BOLD)).append(Component.text(" ◀", NamedTextColor.GOLD)));

        for (String line : reportLines) {
            sender.sendMessage(line);
        }

        return true;
    }

    /**
     * Handle enable/disable commands - toggle ML system
     */
    private boolean handleEnableCommand(Player sender, boolean enable) {
        if (!sender.hasPermission("deepguardx.ml.toggle")) {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to toggle the ML system.", NamedTextColor.RED)));
            return true;
        }

        mlManager.setEnabled(enable);

        if (enable) {
            sender.sendMessage(Component.text("✓ ", NamedTextColor.GREEN).append(Component.text("ML detection system ", NamedTextColor.GREEN)).append(Component.text("ENABLED", NamedTextColor.GREEN, TextDecoration.BOLD)));
        } else {
            sender.sendMessage(Component.text("✓ ", NamedTextColor.GREEN).append(Component.text("ML detection system ", NamedTextColor.GREEN)).append(Component.text("DISABLED", NamedTextColor.RED, TextDecoration.BOLD)));
        }

        return true;
    }

    /**
     * Handle status command - display ML system status
     */
    private boolean handleStatusCommand(Player sender) {
        if (!sender.hasPermission("deepguardx.ml.status")) {
            sender.sendMessage(Component.text("⚠ ", NamedTextColor.GOLD).append(Component.text("Permission Denied: ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("You don't have permission to check ML system status.", NamedTextColor.RED)));
            return true;
        }

        boolean enabled = mlManager.isEnabled();

        sender.sendMessage(Component.text("▶ ", NamedTextColor.GOLD).append(Component.text("DeepGuard-XRay ML Status", NamedTextColor.AQUA, TextDecoration.BOLD)).append(Component.text(" ◀", NamedTextColor.GOLD)));

        sender.sendMessage(Component.text("  System: ", NamedTextColor.GRAY).append(Component.text(enabled ? "ENABLED" : "DISABLED", enabled ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD)));

        if (mlManager.isTrained()) {
            sender.sendMessage(Component.text("  Model: ", NamedTextColor.GRAY).append(Component.text("TRAINED", NamedTextColor.GREEN, TextDecoration.BOLD)));
        } else {
            sender.sendMessage(Component.text("  Model: ", NamedTextColor.GRAY).append(Component.text("NOT TRAINED", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text(" - Use ", NamedTextColor.YELLOW)).append(Component.text("/deepguardx ml train", NamedTextColor.GREEN).clickEvent(ClickEvent.suggestCommand("/deepguardx ml train ")).hoverEvent(HoverEvent.showText(Component.text("Click to suggest command")))));
        }

        return true;
    }

    /**
     * Get tab completions for ML subcommands
     */
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        if (args.length == 1) {
            if (sender.hasPermission("deepguardx.ml.train") && "train".startsWith(args[0].toLowerCase())) {
                completions.add("train");
            }
            if (sender.hasPermission("deepguardx.ml.analyze") && "analyze".startsWith(args[0].toLowerCase())) {
                completions.add("analyze");
            }
            if (sender.hasPermission("deepguardx.ml.report") && "report".startsWith(args[0].toLowerCase())) {
                completions.add("report");
            }
            if (sender.hasPermission("deepguardx.ml.toggle")) {
                if ("enable".startsWith(args[0].toLowerCase())) completions.add("enable");
                if ("disable".startsWith(args[0].toLowerCase())) completions.add("disable");
            }
            if (sender.hasPermission("deepguardx.ml.status") && "status".startsWith(args[0].toLowerCase())) {
                completions.add("status");
            }
        } else if (args.length == 2) {
            if ((args[0].equalsIgnoreCase("train") && sender.hasPermission("deepguardx.ml.train")) || (args[0].equalsIgnoreCase("analyze") && sender.hasPermission("deepguardx.ml.analyze")) || (args[0].equalsIgnoreCase("report") && sender.hasPermission("deepguardx.ml.report"))) {

                String partial = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("train")) {
            if (sender.hasPermission("deepguardx.ml.train")) {
                String partial = args[2].toLowerCase();
                if ("cheater".startsWith(partial)) completions.add("cheater");
                if ("normal".startsWith(partial)) completions.add("normal");
            }
        }

        return completions;
    }
}