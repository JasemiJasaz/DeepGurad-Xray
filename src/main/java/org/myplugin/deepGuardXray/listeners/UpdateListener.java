package org.myplugin.deepGuardXray.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.utils.UpdateChecker;

import java.io.File;

public class UpdateListener implements Listener {

    private final deepGuardXray plugin;
    private final UpdateChecker updateChecker;

    public UpdateListener(deepGuardXray plugin, UpdateChecker updateChecker) {
        this.plugin = plugin;
        this.updateChecker = updateChecker;


        updateChecker.checkForUpdates();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();


        if (player.hasPermission("deepguardx.staff") && updateChecker.isUpdateAvailable()) {

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

                Component divider = Component.text("=================================================").color(NamedTextColor.GREEN);


                player.sendMessage(divider);
                player.sendMessage(Component.text(" DeepGuard-XRay: New update available!").color(NamedTextColor.GREEN));


                player.sendMessage(Component.text(" Current version: ").color(NamedTextColor.GREEN).append(Component.text(plugin.getPluginMeta().getVersion()).color(NamedTextColor.RED)));


                player.sendMessage(Component.text(" New version: ").color(NamedTextColor.GREEN).append(Component.text(updateChecker.getLatestVersion()).color(NamedTextColor.GREEN)));


                boolean updateDownloaded = isUpdateDownloaded();


                if (player.hasPermission("deepguardx.autoupdate")) {
                    if (!updateDownloaded) {

                        Component updateMessage = Component.text(" Click here to auto-update the plugin ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).clickEvent(ClickEvent.runCommand("/deepguard update auto")).hoverEvent(HoverEvent.showText(Component.text("Click to download and prepare the update for next restart")));

                        player.sendMessage(updateMessage);
                    } else {

                        Component applyMessage = Component.text(" Click here to apply the downloaded update ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).clickEvent(ClickEvent.runCommand("/deepguard update apply")).hoverEvent(HoverEvent.showText(Component.text("Click to apply the update (server restart still required)")));

                        player.sendMessage(applyMessage);
                    }
                }


                Component downloadMessage = Component.text(" Download manually: ").color(NamedTextColor.YELLOW);

                Component downloadLink = Component.text("Click here").color(NamedTextColor.AQUA).decorate(TextDecoration.UNDERLINED).clickEvent(ClickEvent.openUrl("https://www.spigotmc.org/resources/122967")).hoverEvent(HoverEvent.showText(Component.text("Open download page in your browser")));

                player.sendMessage(downloadMessage.append(downloadLink));
                player.sendMessage(divider);
            }, 60L);
        }
    }

    /**
     * Checks if an update has been downloaded but not yet applied
     */
    private boolean isUpdateDownloaded() {
        File updateMarker = new File(plugin.getDataFolder(), "pending_update.txt");
        return updateMarker.exists();
    }
}