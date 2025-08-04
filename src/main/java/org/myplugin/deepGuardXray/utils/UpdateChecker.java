package org.myplugin.deepGuardXray.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final int resourceId;
    private String latestVersion = "";
    private boolean updateAvailable = false;
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
    private File downloadedUpdateFile = null;
    private static final long DEFAULT_CHECK_INTERVAL = 20 * 60 * 60 * 24;

    public UpdateChecker(JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    /**
     * Sets up automatic periodic update checks
     * @param intervalHours How often to check for updates (in hours)
     */
    public void setupPeriodicChecks(int intervalHours) {

        long intervalTicks = 20 * 60 * 60 * intervalHours;


        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {

            checkForUpdates();
        }, intervalTicks, intervalTicks);
    }

    /**
     * Sets up automatic periodic update checks with default interval (24 hours)
     */
    public void setupPeriodicChecks() {
        setupPeriodicChecks(24);
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openStream();
                 Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    String version = scanner.next();
                    this.latestVersion = version;

                    String currentVersion = plugin.getDescription().getVersion();
                    plugin.getLogger().info("Checking version: Current=" + currentVersion + ", Latest=" + version);


                    if (isNewerVersion(version, currentVersion)) {
                        this.updateAvailable = true;
                    } else {
                        this.updateAvailable = false;
                    }

                    consumer.accept(version);
                }
            } catch (IOException exception) {
                plugin.getLogger().info("Unable to check for updates: " + exception.getMessage());
            }
        });
    }

    /**
     * Checks if an update is available and logs a message
     */
    public void checkForUpdates() {
        this.getVersion(version -> {
            String currentVersion = plugin.getDescription().getVersion();


            if (isNewerVersion(version, currentVersion)) {
                plugin.getLogger().info("§e=================================================");
                plugin.getLogger().info("§e DeepGuard-XRay: New update available!");
                plugin.getLogger().info("§e Current version: §c" + currentVersion);
                plugin.getLogger().info("§e New version: §a" + version);
                plugin.getLogger().info("§e Download: §ahttps://www.spigotmc.org/resources/" + resourceId);
                plugin.getLogger().info("§e=================================================");
            } else {
                plugin.getLogger().info("DeepGuard-XRay is up to date!");
            }
        });
    }
    /**
     * Compares two version strings to see if one is newer than the other
     * @param versionA The first version (typically remote version)
     * @param versionB The second version (typically current version)
     * @return true if versionA is newer than versionB
     */
    private boolean isNewerVersion(String versionA, String versionB) {

        Matcher matcherA = VERSION_PATTERN.matcher(versionA);
        if (!matcherA.matches()) {
            plugin.getLogger().warning("Invalid version format: " + versionA);
            return false;
        }

        int majorA = Integer.parseInt(matcherA.group(1));
        int minorA = Integer.parseInt(matcherA.group(2));
        int patchA = Integer.parseInt(matcherA.group(3));


        Matcher matcherB = VERSION_PATTERN.matcher(versionB);
        if (!matcherB.matches()) {
            plugin.getLogger().warning("Invalid version format: " + versionB);
            return false;
        }

        int majorB = Integer.parseInt(matcherB.group(1));
        int minorB = Integer.parseInt(matcherB.group(2));
        int patchB = Integer.parseInt(matcherB.group(3));


        if (majorA > majorB) return true;
        if (majorA < majorB) return false;


        if (minorA > minorB) return true;
        if (minorA < minorB) return false;


        return patchA > patchB;
    }

    /**
     * Downloads the latest version of the plugin directly to the plugins directory
     * @param callback Consumer that accepts a boolean indicating success or failure
     */
    public void downloadUpdate(final Consumer<Boolean> callback) {
        if (!updateAvailable) {
            callback.accept(false);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {

                File pluginsDirectory = plugin.getServer().getUpdateFolderFile().getParentFile();
                plugin.getLogger().info("Using plugins directory: " + pluginsDirectory.getAbsolutePath());


                String pluginName = "DeepGuard-XRay";
                String currentVersion = plugin.getDescription().getVersion();


                File currentPluginFile = new File(pluginsDirectory, pluginName + "-" + currentVersion + ".jar");


                if (!currentPluginFile.exists()) {
                    plugin.getLogger().info("Current plugin JAR not found with expected name, searching for alternatives...");

                    File[] jarFiles = pluginsDirectory.listFiles((dir, name) ->
                            name.toLowerCase().startsWith(pluginName.toLowerCase()) &&
                                    name.toLowerCase().endsWith(".jar"));

                    if (jarFiles != null && jarFiles.length > 0) {

                        currentPluginFile = jarFiles[0];
                        plugin.getLogger().info("Found alternative plugin JAR: " + currentPluginFile.getName());
                    } else {
                        plugin.getLogger().warning("Could not find any matching plugin JAR files!");
                    }
                }


                String newFilename = pluginName + "-" + latestVersion + ".jar";


                File outputFile = new File(pluginsDirectory, newFilename);

                plugin.getLogger().info("Downloading update to: " + outputFile.getAbsolutePath());


                String downloadUrl = "https://api.spiget.org/v2/resources/" + resourceId + "/download";


                URL url = new URL(downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "DeepGuardX-AutoUpdater");


                try (InputStream in = connection.getInputStream();
                     ReadableByteChannel rbc = Channels.newChannel(in);
                     FileOutputStream fos = new FileOutputStream(outputFile)) {

                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);


                    downloadedUpdateFile = outputFile;



                    boolean isPaperServer = false;
                    try {

                        Class.forName("io.papermc.paper.PaperConfig");
                        isPaperServer = true;
                    } catch (ClassNotFoundException e) {

                    }

                    boolean deleteSuccess = false;
                    if (isPaperServer && currentPluginFile.exists()) {
                        plugin.getLogger().info("Paper server detected - attempting immediate cleanup of old JAR");
                        deleteSuccess = currentPluginFile.delete();
                        plugin.getLogger().info(deleteSuccess ?
                                "Successfully deleted old plugin JAR: " + currentPluginFile.getName() :
                                "Could not delete old plugin JAR (it will be cleaned up on shutdown): " + currentPluginFile.getName());
                    }


                    File updateMarker = new File(plugin.getDataFolder(), "pending_update.txt");
                    try (FileOutputStream markerOut = new FileOutputStream(updateMarker)) {
                        String markerContent = "current_plugin=" + currentPluginFile.getAbsolutePath() + "\n" +
                                "new_plugin=" + outputFile.getAbsolutePath() + "\n" +
                                "version=" + latestVersion + "\n" +
                                "already_deleted=" + deleteSuccess + "\n" +
                                "timestamp=" + System.currentTimeMillis();
                        markerOut.write(markerContent.getBytes());
                    }

                    plugin.getLogger().info("Update downloaded successfully! The new version will be used after server restart.");
                    plugin.getLogger().info("New plugin JAR: " + outputFile.getName());


                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(true));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to download update: " + e.getMessage());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public File getDownloadedUpdateFile() {
        return downloadedUpdateFile;
    }
}