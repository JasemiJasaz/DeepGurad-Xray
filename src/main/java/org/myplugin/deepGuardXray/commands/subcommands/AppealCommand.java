package org.myplugin.deepGuardXray.commands.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.managers.AppealManager;
import org.myplugin.deepGuardXray.managers.AppealManager.Appeal;
import org.myplugin.deepGuardXray.managers.AppealManager.AppealStatus;

import java.util.*;

public class AppealCommand implements CommandExecutor, Listener {
    private final deepGuardXray plugin;
    private final AppealManager appealManager;
    private final Map<UUID, Integer> awaitingReason = new HashMap<>();

    public AppealCommand(deepGuardXray plugin, AppealManager appealManager) {
        this.plugin = plugin;
        this.appealManager = appealManager;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        UUID playerId = player.getUniqueId();

        int punishmentLevel = plugin.getPunishmentManager().getPlayerPunishmentLevel(playerId);
        if (punishmentLevel <= 0) {
            player.sendMessage(Component.text("You don't have any active punishments to appeal.").color(NamedTextColor.RED));
            return true;
        }

        Appeal existingAppeal = appealManager.getActiveAppealForPlayer(playerId);
        if (existingAppeal != null && existingAppeal.getPunishmentLevel() == punishmentLevel && (existingAppeal.getStatus() == AppealStatus.PENDING || existingAppeal.getStatus() == AppealStatus.UNDER_REVIEW)) {
            showAppealStatus(player, existingAppeal);
            return true;
        }

        List<Appeal> allAppeals = appealManager.getAppealsForPlayer(playerId);
        if (!allAppeals.isEmpty()) {
            allAppeals.sort(Comparator.comparing(Appeal::getTimestamp).reversed());
            Appeal mostRecentAppeal = allAppeals.get(0);

            if (mostRecentAppeal.getPunishmentLevel() != punishmentLevel) {
                startAppealProcess(player, punishmentLevel);
                return true;
            }

            if (mostRecentAppeal.getStatus() == AppealStatus.DENIED || mostRecentAppeal.getStatus() == AppealStatus.APPROVED) {
                startAppealProcess(player, punishmentLevel);
                return true;
            }

            showAppealStatus(player, mostRecentAppeal);
            return true;
        }

        startAppealProcess(player, punishmentLevel);
        return true;
    }

    /**
     * Start the appeal process for a player
     */
    private void startAppealProcess(Player player, int punishmentLevel) {
        UUID playerId = player.getUniqueId();

        awaitingReason.put(playerId, punishmentLevel);

        player.sendMessage(Component.text("╔═════════════════════════════╗").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("║ ").color(NamedTextColor.GOLD).append(Component.text("Please type your appeal reason ").color(NamedTextColor.YELLOW)).append(Component.text("║").color(NamedTextColor.GOLD)));
        player.sendMessage(Component.text("║ ").color(NamedTextColor.GOLD).append(Component.text("Explain why you believe the ").color(NamedTextColor.YELLOW)).append(Component.text("║").color(NamedTextColor.GOLD)));
        player.sendMessage(Component.text("║ ").color(NamedTextColor.GOLD).append(Component.text("detection was incorrect. ").color(NamedTextColor.YELLOW)).append(Component.text("║").color(NamedTextColor.GOLD)));
        player.sendMessage(Component.text("║ ").color(NamedTextColor.GOLD).append(Component.text("Your next message will be your ").color(NamedTextColor.YELLOW)).append(Component.text("║").color(NamedTextColor.GOLD)));
        player.sendMessage(Component.text("║ ").color(NamedTextColor.GOLD).append(Component.text("appeal reason (not visible to ").color(NamedTextColor.YELLOW)).append(Component.text("║").color(NamedTextColor.GOLD)));
        player.sendMessage(Component.text("║ ").color(NamedTextColor.GOLD).append(Component.text("other players). ").color(NamedTextColor.YELLOW)).append(Component.text("║").color(NamedTextColor.GOLD)));
        player.sendMessage(Component.text("╚═════════════════════════════╝").color(NamedTextColor.GOLD));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
    }

    /**
     * Handle chat messages from players who are in the appeal process
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (awaitingReason.containsKey(playerId)) {
            event.setCancelled(true);
            int punishmentLevel = awaitingReason.get(playerId);
            awaitingReason.remove(playerId);
            final String appealReason = event.getMessage();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Appeal appeal = appealManager.createAppeal(player, punishmentLevel, appealReason);

                player.sendMessage(Component.text("Your appeal has been submitted successfully!").color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("Staff will review your case as soon as possible.").color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Use ").color(NamedTextColor.GRAY).append(Component.text("/dgx appeal").color(NamedTextColor.GOLD)).append(Component.text(" to check your appeal status.").color(NamedTextColor.GRAY)));

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
            });
        }
    }

    private void showAppealStatus(Player player, Appeal appeal) {
        UUID playerId = player.getUniqueId();
        int currentPunishmentLevel = plugin.getPunishmentManager().getPlayerPunishmentLevel(playerId);

        Component statusComponent = Component.text("Status: ").color(NamedTextColor.GRAY).append(Component.text(appeal.getStatus().getDisplayName()).color(appeal.getStatus().getColor()));

        player.sendMessage(Component.text("╔═════════ APPEAL STATUS ═════════╗").color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("Appeal #" + appeal.getId()).color(NamedTextColor.YELLOW)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
        player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(statusComponent).append(Component.text(" ║").color(NamedTextColor.AQUA)));

        if (appeal.getStatus() != AppealStatus.PENDING && !appeal.getStaffResponse().isEmpty()) {
            player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("Staff Response:").color(NamedTextColor.GRAY)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
            player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text(appeal.getStaffResponse()).color(NamedTextColor.WHITE)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
        }

        if (appeal.getStatus() == AppealStatus.PENDING || appeal.getStatus() == AppealStatus.UNDER_REVIEW) {
            player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("Your appeal is being reviewed.").color(NamedTextColor.YELLOW)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
        } else if (appeal.getStatus() == AppealStatus.APPROVED) {
            if (currentPunishmentLevel > 0 && appeal.getPunishmentLevel() != currentPunishmentLevel) {
                player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("You have a new punishment.").color(NamedTextColor.RED)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
                player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("Type /dgx appeal to appeal it.").color(NamedTextColor.YELLOW)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
            } else {
                player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("Your punishment has been removed!").color(NamedTextColor.GREEN)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
            }
        } else if (appeal.getStatus() == AppealStatus.DENIED) {
            player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("Your appeal was denied.").color(NamedTextColor.RED)).append(Component.text(" ║").color(NamedTextColor.AQUA)));

            if (currentPunishmentLevel > 0) {
                player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("You can submit a new appeal.").color(NamedTextColor.GOLD)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
                player.sendMessage(Component.text("║ ").color(NamedTextColor.AQUA).append(Component.text("Type /dgx appeal to try again.").color(NamedTextColor.YELLOW)).append(Component.text(" ║").color(NamedTextColor.AQUA)));
            }
        }

        player.sendMessage(Component.text("╚═════════════════════════════╝").color(NamedTextColor.AQUA));
    }

    /**
     * Show appeal status to players when they join
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Appeal appeal = appealManager.getActiveAppealForPlayer(playerId);
        if (appeal != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                showAppealStatus(player, appeal);
            }, 40L);
        }
    }
}