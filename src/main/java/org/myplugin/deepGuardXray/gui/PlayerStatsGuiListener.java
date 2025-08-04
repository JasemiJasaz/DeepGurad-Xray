package org.myplugin.deepGuardXray.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.myplugin.deepGuardXray.gui.subgui.PlayerMiningStatsGUI;
import org.myplugin.deepGuardXray.utils.UUIDUtils;

import java.util.UUID;

public class PlayerStatsGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        Component title = event.getView().title();
        String titleText = title.toString();

        if (titleText.contains("📊 Player Analytics")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            if (!(clicked.getItemMeta() instanceof SkullMeta meta)) return;


            Player player = (Player) event.getWhoClicked();
            if (!player.hasPermission("deepguardx.gui_playerstats")) {
                player.sendMessage(Component.text("You do not have permission to view player stats.").color(NamedTextColor.RED));
                return;
            }

            Component displayName = meta.displayName();

            if (displayName == null) return;


            String targetPlayerName = PlainTextComponentSerializer.plainText().serialize(displayName);


            UUID targetPlayerId = UUIDUtils.getUUID(targetPlayerName);
            if (targetPlayerId == null) {
                player.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
                return;
            }

            PlayerMiningStatsGUI statsGUI = new PlayerMiningStatsGUI(targetPlayerId, targetPlayerName);
            player.openInventory(statsGUI.getInventory());
        }
    }
}