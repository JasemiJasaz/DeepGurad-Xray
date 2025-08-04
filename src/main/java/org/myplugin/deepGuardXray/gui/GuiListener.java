package org.myplugin.deepGuardXray.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.myplugin.deepGuardXray.deepGuardXray;
import org.myplugin.deepGuardXray.gui.subgui.*;

/**
 * Updated GuiListener with appeal system integration
 */
public class GuiListener implements Listener {
    private final deepGuardXray plugin;

    public GuiListener(deepGuardXray plugin) {
        this.plugin = plugin;
        StaffMenuGUI.setPlugin(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Component title = event.getView().title();

        String titlePlainText = PlainTextComponentSerializer.plainText().serialize(title);


        if (titlePlainText.contains("DeepGuard-XRay Control Panel") || titlePlainText.contains("🔍 Suspicious Activity") || titlePlainText.contains("⚖ Punishment System") || titlePlainText.contains("⚙ Plugin Configuration") || titlePlainText.contains("⚙ Decoy Settings") || titlePlainText.contains("⚙ Auto-Save Settings") || titlePlainText.contains("⚙ Staff Settings") || titlePlainText.contains("📊 Player Analytics") || titlePlainText.contains("🤖 ML Analysis") || titlePlainText.contains("📋 ML Analysis Reports") || titlePlainText.contains("⚙ Auto ML Analysis Settings") || titlePlainText.contains("📊 Select Player to Analyze") || titlePlainText.contains("📈 Select Normal Player for Training") || titlePlainText.contains("🔍 Select Cheater for Training") || titlePlainText.contains("Mining Stats") || titlePlainText.contains("Settings for Level") || titlePlainText.contains("⛏ Ore Management") || titlePlainText.contains("🔔 Discord Webhook Settings") || titlePlainText.contains("📋 DeepGuardX Appeal System") || titlePlainText.contains("Appeal #") && titlePlainText.contains("Details")) {

            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

            Component nameComponent = clicked.getItemMeta().displayName();

            String plainName = PlainTextComponentSerializer.plainText().serialize(nameComponent);
            Player player = (Player) event.getWhoClicked();


            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);


            if (titlePlainText.contains("DeepGuard-XRay Control Panel")) {

                if (plainName.contains("Player Analytics")) {
                    new PlayerStatsMainGUI(0).openInventory(player);
                } else if (plainName.contains("Suspicious Activity")) {
                    new SuspiciousPlayersGUI(0).openInventory(player);
                } else if (plainName.contains("Punishment System")) {
                    new PunishmentSettingsGUI(plugin.getConfigManager()).openInventory(player);
                } else if (plainName.contains("Ore Management")) {
                    new OreConfigGUI(plugin.getConfigManager()).openInventory(player);
                } else if (plainName.contains("Plugin Configuration")) {
                    if (player.hasPermission(ConfigSettingsGUI.PERMISSION)) {
                        new ConfigSettingsGUI(plugin.getConfigManager(), plugin).openInventory(player);
                    } else {
                        player.sendMessage(Component.text("You don't have permission to access the config settings.").color(NamedTextColor.RED));
                    }
                } else if (plainName.contains("Discord Webhook")) {
                    if (player.hasPermission(WebhookSettingsGUI.PERMISSION)) {
                        new WebhookSettingsGUI(plugin.getConfigManager(), plugin).openInventory(player);
                    } else {
                        player.sendMessage(Component.text("You don't have permission to access webhook settings.").color(NamedTextColor.RED));
                    }
                } else if (plainName.contains("ML Analysis")) {
                    if (player.hasPermission(MLAnalysisGUI.PERMISSION)) {
                        new MLAnalysisGUI(plugin).openInventory(player);
                    } else {
                        player.sendMessage(Component.text("You don't have permission to access ML Analysis.").color(NamedTextColor.RED));
                    }
                } else if (plainName.contains("Player Appeals")) {
                    if (player.hasPermission("deepguardx.gui_Appeal")) {
                        new AppealGUI(plugin, plugin.getAppealManager(), 0).openInventory(player);
                    } else {
                        player.sendMessage(Component.text("You don't have permission to access the appeals system.").color(NamedTextColor.RED));
                    }
                } else if (clicked.getType() == Material.NETHER_STAR || clicked.getType() == Material.BOOK || clicked.getType() == Material.REDSTONE || clicked.getType() == Material.COMPASS || clicked.getType() == Material.BARRIER || clicked.getType() == Material.OBSERVER || clicked.getType() == Material.LODESTONE) {

                }
            } else if (titlePlainText.contains("🤖 ML Analysis")) {

                if (!player.hasPermission(MLAnalysisGUI.PERMISSION)) {
                    player.sendMessage(Component.text("You don't have permission to access ML Analysis.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }


                MLAnalysisGUI.handleClick(player, event.getRawSlot(), event.getInventory(), plugin);
            } else if (titlePlainText.contains("⚙ Auto ML Analysis Settings")) {

                if (!player.hasPermission(MLAnalysisGUI.PERMISSION)) {
                    player.sendMessage(Component.text("You don't have permission to access ML Analysis settings.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }


                AutoAnalysisGUI.handleClick(player, event.getRawSlot(), event.getInventory(), plugin);
            } else if (titlePlainText.contains("📊 Select Player to Analyze")) {

                if (!player.hasPermission(MLAnalysisGUI.PERMISSION)) {
                    player.sendMessage(Component.text("You don't have permission to access ML Analysis.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }


                PlayerSelectorGUI.handleClick(player, event.getRawSlot(), event.getInventory(), plugin, PlayerSelectorGUI.SelectionType.ANALYZE);
            } else if (titlePlainText.contains("📋 ML Analysis Reports")) {

                if (!player.hasPermission(MLAnalysisGUI.PERMISSION)) {
                    player.sendMessage(Component.text("You don't have permission to access ML Reports.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }


                MLReportsGUI.handleClick(player, event.getRawSlot(), event.getInventory(), plugin);
            } else if (titlePlainText.contains("📈 Select Normal Player for Training")) {

                if (!player.hasPermission(MLAnalysisGUI.PERMISSION)) {
                    player.sendMessage(Component.text("You don't have permission to access ML Analysis.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }


                PlayerSelectorGUI.handleClick(player, event.getRawSlot(), event.getInventory(), plugin, PlayerSelectorGUI.SelectionType.TRAIN_NORMAL);
            } else if (titlePlainText.contains("🔍 Select Cheater for Training")) {

                if (!player.hasPermission(MLAnalysisGUI.PERMISSION)) {
                    player.sendMessage(Component.text("You don't have permission to access ML Analysis.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }


                PlayerSelectorGUI.handleClick(player, event.getRawSlot(), event.getInventory(), plugin, PlayerSelectorGUI.SelectionType.TRAIN_CHEATER);
            } else if (titlePlainText.contains("📋 DeepGuardX Appeal System")) {

                if (!player.hasPermission("deepguardx.gui_Appeal")) {
                    player.sendMessage(Component.text("You don't have permission to access the appeals system.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }


                AppealGUI.handleClick(player, event.getRawSlot(), event.getInventory(), plugin, plugin.getAppealManager());
            } else if (titlePlainText.contains("Appeal #") && titlePlainText.contains("Details")) {

                if (!player.hasPermission("deepguardx.gui_Appeal")) {
                    player.sendMessage(Component.text("You don't have permission to access the appeals system.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }


                AppealDetailsGUI.handleClick(player, event.getRawSlot(), event.getClick(), event.getInventory(), plugin, plugin.getAppealManager());
            } else if (titlePlainText.contains("🔔 Discord Webhook Settings")) {

                if (!player.hasPermission(WebhookSettingsGUI.PERMISSION)) {
                    player.sendMessage(Component.text("You don't have permission to access webhook settings.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }


                WebhookSettingsGUI.handleClick(player, event.getRawSlot(), event.getInventory(), plugin.getConfigManager(), plugin);
            } else if (titlePlainText.contains("⚖ Punishment System")) {

                if (clicked.getType() == Material.GREEN_WOOL || clicked.getType() == Material.RED_WOOL) {

                    int slot = event.getRawSlot();
                    int level = (slot / 9) + 1;


                    boolean currentlyEnabled = plugin.getConfigManager().isPunishmentEnabled(level);


                    plugin.getConfigManager().setPunishmentEnabled(level, !currentlyEnabled);


                    if (currentlyEnabled) {


                        plugin.getPunishmentManager().onPunishmentLevelDisabled(level, player.getName());


                        player.sendMessage(Component.text("Punishment level " + level + " disabled. All affected players have been notified.", NamedTextColor.GREEN));


                        if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                            plugin.getWebhookManager().sendStaffActionLog(player.getName(), "Disabled Punishment Level " + level, "All affected players");
                        }
                    } else {

                        player.sendMessage(Component.text("Punishment level " + level + " enabled.", NamedTextColor.GREEN));


                        plugin.getLogger().info("Admin " + player.getName() + " enabled punishment level " + level);


                        if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                            plugin.getWebhookManager().sendStaffActionLog(player.getName(), "Enabled Punishment Level " + level, "Server-wide setting");
                        }
                    }


                    new PunishmentSettingsGUI(plugin.getConfigManager()).openInventory(player);
                } else if (clicked.getType() == Material.BELL || (clicked.getType() == Material.GRAY_DYE && plainName.contains("Admin Alerts"))) {

                    int slot = event.getRawSlot();
                    int level = (slot / 9) + 1;


                    boolean current = plugin.getConfigManager().isPunishmentOptionEnabled(level, "admin_alert");
                    plugin.getConfigManager().setPunishmentOptionEnabled(level, "admin_alert", !current);


                    plugin.getLogger().info("Admin " + player.getName() + " " + (current ? "disabled" : "enabled") + " admin alerts for punishment level " + level);


                    if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                        plugin.getWebhookManager().sendStaffActionLog(player.getName(), (current ? "Disabled" : "Enabled") + " Admin Alerts for Punishment Level " + level, "Configuration Change");
                    }


                    new PunishmentSettingsGUI(plugin.getConfigManager()).openInventory(player);
                } else if (clicked.getType() == Material.BOOK || (clicked.getType() == Material.GRAY_DYE && plainName.contains("Warning Messages"))) {

                    int slot = event.getRawSlot();
                    int level = (slot / 9) + 1;


                    boolean current = plugin.getConfigManager().isPunishmentOptionEnabled(level, "warning_message");
                    plugin.getConfigManager().setPunishmentOptionEnabled(level, "warning_message", !current);


                    if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                        plugin.getWebhookManager().sendStaffActionLog(player.getName(), (current ? "Disabled" : "Enabled") + " Warning Messages for Punishment Level " + level, "Configuration Change");
                    }


                    new PunishmentSettingsGUI(plugin.getConfigManager()).openInventory(player);
                } else if (plainName.contains("Advanced settings")) {
                    int slot = event.getRawSlot();
                    int level = (slot / 9) + 1;
                    new LevelPunishmentSettingsGUI(plugin.getConfigManager(), level).openInventory(player);
                } else if (plainName.contains("Icon Guide")) {

                } else if (plainName.contains("Back to")) {
                    new StaffMenuGUI().openInventory(player);
                }
            } else if (titlePlainText.contains("Settings for Level")) {

                int level = Integer.parseInt(titlePlainText.substring(titlePlainText.indexOf("Level ") + 6, titlePlainText.indexOf(" Punishment")));


                if (plainName.contains("Back to")) {
                    new PunishmentSettingsGUI(plugin.getConfigManager()).openInventory(player);
                } else {
                    LevelPunishmentSettingsGUI.handleClick(player, event.getRawSlot(), level, plugin.getConfigManager());
                }
            } else if (titlePlainText.contains("📊 Player Analytics")) {

                PlayerStatsMainGUI.handleClick(player, event.getRawSlot(), event.getInventory());
            } else if (titlePlainText.contains("Mining Stats")) {

                PlayerMiningStatsGUI.handleClick(player, event.getRawSlot());
            } else if (titlePlainText.contains("⛏ Ore Management")) {

                if (plainName.contains("Back to")) {
                    new StaffMenuGUI().openInventory(player);
                    return;
                }


                Material oreMaterial = clicked.getType();

                if (plugin.getConfigManager().getNaturalOres().contains(oreMaterial)) {

                    plugin.getConfigManager().getNaturalOres().remove(oreMaterial);
                    plugin.getConfig().set("ores.natural", plugin.getConfigManager().getNaturalOres().stream().map(Material::name).toList());
                    plugin.saveConfig();
                    player.sendMessage(Component.text(oreMaterial.name() + " removed from natural ores.").color(NamedTextColor.YELLOW));


                    if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                        plugin.getWebhookManager().sendStaffActionLog(player.getName(), "Ore Configuration", "Removed " + oreMaterial.name() + " from natural ores");
                    }
                } else {

                    plugin.getConfigManager().getNaturalOres().add(oreMaterial);
                    plugin.getConfig().set("ores.natural", plugin.getConfigManager().getNaturalOres().stream().map(Material::name).toList());
                    plugin.saveConfig();
                    player.sendMessage(Component.text(oreMaterial.name() + " added to natural ores.").color(NamedTextColor.GREEN));


                    if (plugin.getConfigManager().isWebhookAlertEnabled("staff_actions")) {
                        plugin.getWebhookManager().sendStaffActionLog(player.getName(), "Ore Configuration", "Added " + oreMaterial.name() + " to natural ores");
                    }
                }

                new OreConfigGUI(plugin.getConfigManager()).openInventory(player);
            } else if (titlePlainText.contains("🔍 Suspicious Activity")) {

                SuspiciousPlayersGUI.handleClick(player, event.getRawSlot(), event.getInventory());
            } else if (titlePlainText.equals("⚙ Plugin Configuration")) {

                if (!player.hasPermission(ConfigSettingsGUI.PERMISSION)) {
                    player.sendMessage(Component.text("You don't have permission to access config settings.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }


                ConfigSettingsGUI.handleClick(player, event.getRawSlot(), event.getInventory(), plugin.getConfigManager(), plugin);
            } else if (titlePlainText.equals("⚙ Decoy Settings")) {

                if (!player.hasPermission(ConfigSettingsGUI.PERMISSION)) {
                    player.sendMessage(Component.text("You don't have permission to access config settings.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }

                DecoySettingsGUI.handleClick(player, event.getRawSlot(), event.getInventory(), event.getClick(), plugin.getConfigManager(), plugin);
            } else if (titlePlainText.equals("⚙ Auto-Save Settings")) {

                if (!player.hasPermission(ConfigSettingsGUI.PERMISSION)) {
                    player.sendMessage(Component.text("You don't have permission to access config settings.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }

                AutoSaveSettingsGUI.handleClick(player, event.getRawSlot(), event.getInventory(), event.getClick(), plugin.getConfigManager(), plugin);
            } else if (titlePlainText.equals("⚙ Staff Settings")) {

                if (!player.hasPermission(ConfigSettingsGUI.PERMISSION)) {
                    player.sendMessage(Component.text("You don't have permission to access config settings.").color(NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }

                StaffSettingsGUI.handleClick(player, event.getRawSlot(), event.getInventory(), event.getClick(), plugin.getConfigManager(), plugin);
            }

        }
    }
}