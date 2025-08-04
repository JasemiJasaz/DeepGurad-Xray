package org.myplugin.deepGuardXray.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.myplugin.deepGuardXray.deepGuardXray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandHider implements Listener, CommandExecutor, TabCompleter {

    private final deepGuardXray plugin;
    private final List<String> hiddenCommands;
    private final AntiXrayCommand realCommandExecutor;

    public CommandHider(deepGuardXray plugin) {
        this.plugin = plugin;
        this.hiddenCommands = Arrays.asList("deepguard", "dgx", "deepguardx", "deepguard-xray:deepguard", "deepguard-xray:dgx");
        this.realCommandExecutor = new AntiXrayCommand(plugin, plugin.getUpdateChecker());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("deepguardx.use")) {
            String errorLine1 = plugin.getConfigManager().getCommandHidingErrorLine1();
            String errorLine2 = plugin.getConfigManager().getCommandHidingErrorLine2().replace("{command}", label);
            sender.sendMessage(errorLine1);
            sender.sendMessage(errorLine2);
            return true;
        }

        return realCommandExecutor.onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("deepguardx.use")) {
            return new ArrayList<>();
        }

        if (realCommandExecutor instanceof TabCompleter) {
            return realCommandExecutor.onTabComplete(sender, command, alias, args);
        }

        return null;
    }

    /**
     * This event is for Paper/Spigot 1.13+ and intercepts the command list sent to players
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("deepguardx.use")) {
            event.getCommands().removeAll(hiddenCommands);
        }
    }

    /**
     * Backup method for tab completion if PlayerCommandSendEvent doesn't work
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) return;

        if (!player.hasPermission("deepguardx.use")) {
            String buffer = event.getBuffer().toLowerCase();

            for (String cmd : hiddenCommands) {
                if (buffer.startsWith("/" + cmd)) {
                    event.setCancelled(true);
                    event.setCompletions(new ArrayList<>());
                    return;
                }

                String cmdWithSlash = "/" + cmd;
                if (cmdWithSlash.startsWith(buffer)) {
                    event.setCancelled(true);
                    event.setCompletions(new ArrayList<>());
                    return;
                }
            }
        }
    }
}