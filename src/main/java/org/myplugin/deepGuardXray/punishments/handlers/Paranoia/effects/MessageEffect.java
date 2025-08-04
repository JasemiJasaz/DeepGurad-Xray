package org.myplugin.deepGuardXray.punishments.handlers.Paranoia.effects;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.myplugin.deepGuardXray.config.ConfigManager;
import org.myplugin.deepGuardXray.deepGuardXray;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles fake message effects for paranoia
 */
public class MessageEffect {

    private final deepGuardXray plugin;
    private final ConfigManager configManager;
    private final String[] fakeMessages = {"I can see you mining down there...", "I've been watching you for a while now...", "Your breathing... I can hear it...", "The way you mine... so predictable...", "Don't turn around... I'm right behind you...", "I know all your secret tunnels...", "Your pickaxe makes such a beautiful sound...", "Do you feel me... getting closer?", "You're never truly alone in these caves...", "I've been following your tracks...", "Your fear... it smells so sweet...", "I know where you hide your treasures...", "Your mining pattern reveals your thoughts...", "Tap... tap... tap... I hear your movements...", "When you look away... that's when I move closer...", "I'm always watching... always listening...", "Every ore you break... I feel it...", "Don't you see the eyes in the darkness?", "The deeper you go... the closer I am...", "I've been in these caves far longer than you...", "Your heartbeat... it's getting faster...", "These walls have eyes... my eyes...", "The sounds of mining... they call to me...", "You can run... but these caves are mine...", "I see you when you think you're hidden...", "Keep mining... I'm getting hungrier..."};
    private final String[] adminModMessages = {"Your mining pattern seems suspicious...", "We're monitoring this area for X-ray usage.", "Unusual behavior detected in your session.", "This mining activity has been flagged for review.", "Anti-cheat system has logged your recent activity.", "Warning: Your actions are being recorded.", "Server logs indicate potential rule violations.", "A staff member is spectating this area.", "Your behavior triggered our anti-cheat system.", "Mining straight to resources is suspicious.", "Please explain your mining pattern in chat.", "Anti-X-ray measures have been activated in your area.", "We've received reports about your mining behavior.", "Unusual ore detection rate in your mining session.", "Your account is currently under review.",};

    public MessageEffect(deepGuardXray plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Sends a fake system message to create paranoia
     */
    public void sendFakeMessage(Player player) {

        int messageType = ThreadLocalRandom.current().nextInt(4);
        String message;


        String rawMessage;
        if (messageType == 3) {
            rawMessage = adminModMessages[ThreadLocalRandom.current().nextInt(adminModMessages.length)];
        } else {
            rawMessage = fakeMessages[ThreadLocalRandom.current().nextInt(fakeMessages.length)];
        }

        switch (messageType) {
            case 0:

                message = ChatColor.GRAY + "" + ChatColor.ITALIC + "... " + rawMessage + " ...";

                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 0.5f);
                break;

            case 1:

                String[] mysteriousNames = {"Herobrine", "NULL", "????", "&^!@+#",};
                String name = mysteriousNames[ThreadLocalRandom.current().nextInt(mysteriousNames.length)];
                message = ChatColor.DARK_PURPLE + "<" + name + "> " + ChatColor.WHITE + rawMessage;

                player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_LISTENING, 0.2f, 0.7f);
                break;

            case 2:

                message = ChatColor.DARK_RED + "[" + ChatColor.MAGIC + "ERROR" + ChatColor.RESET + ChatColor.DARK_RED + "] " + ChatColor.RED + rawMessage;

                player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.6f);
                break;

            case 3:

                String[] staffNames = {"Admin", "Moderator", "Staff",};
                String staffName = staffNames[ThreadLocalRandom.current().nextInt(staffNames.length)];


                message = ChatColor.RED + "[" + ChatColor.GOLD + staffName + ChatColor.RED + "] " + ChatColor.YELLOW + rawMessage;


                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);


                player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 1.0f);
                break;

            default:
                message = ChatColor.RED + rawMessage;
        }


        player.sendMessage(message);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Sent fake message to " + player.getName() + ": " + message);
        }
    }
}