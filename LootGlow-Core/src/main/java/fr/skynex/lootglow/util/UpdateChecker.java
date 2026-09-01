package fr.skynex.lootglow.util;

import fr.skynex.lootglow.LootGlow;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private final LootGlow plugin;
    private final int resourceId;

    public UpdateChecker(LootGlow plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        FoliaScheduler.runAsync(this.plugin, () -> {
            try (InputStream inputStream = URI
                    .create("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).toURL()
                    .openStream();
                    Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    consumer.accept(scanner.next());
                }
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not check for updates: " + exception.getMessage());
            }
        });
    }

    public void checkUpdateOnStartup() {
        if (plugin.getConfig().getBoolean("settings.check-updates", true)) {
            getVersion(version -> {
                if (isNewerVersion(plugin.getPluginMeta().getVersion(), version)) {
                    plugin.getLogger().warning("A new update is available (" + version
                            + ")! Download it here: https://www.spigotmc.org/resources/134648");
                } else if (plugin.getPluginMeta().getVersion().equals(version)) {
                    plugin.getLogger().info("The plugin is up to date.");
                }
            });
        }
    }

    public static boolean isNewerVersion(String current, String online) {
        try {
            String[] currentParts = current.split("\\.");
            String[] onlineParts = online.split("\\.");
            int length = Math.max(currentParts.length, onlineParts.length);
            for (int i = 0; i < length; i++) {
                int c = (i < currentParts.length) ? Integer.parseInt(currentParts[i].replaceAll("[^0-9]", "")) : 0;
                int o = (i < onlineParts.length) ? Integer.parseInt(onlineParts[i].replaceAll("[^0-9]", "")) : 0;
                if (o > c)
                    return true;
                if (c > o)
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}