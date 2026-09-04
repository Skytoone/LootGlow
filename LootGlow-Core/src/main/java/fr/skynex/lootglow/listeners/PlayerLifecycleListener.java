package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public class PlayerLifecycleListener implements Listener {

    private final LootGlow plugin;

    public PlayerLifecycleListener(LootGlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.loadPlayerData(event.getPlayer());

        FoliaScheduler.runLater(plugin, () -> {
            Player p = event.getPlayer();
            if (!p.isOnline() || !plugin.isPluginEnabled()) return;
            plugin.refreshGlowForPlayer(p, !plugin.getHiddenVisuals().contains(p.getUniqueId()));
        }, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        plugin.clearVisualsForPlayer(player);
        FoliaScheduler.runLater(plugin, () -> {
            if (player.isOnline() && plugin.isPluginEnabled()) {
                plugin.refreshGlowForPlayer(player, !plugin.getHiddenVisuals().contains(player.getUniqueId()));
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportPlayer(PlayerTeleportEvent event) {
        if (event.getTo() != null && !event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            plugin.clearVisualsForPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getVisibleEntities().remove(uuid);
        plugin.getHiddenVisuals().remove(uuid);
        plugin.getDisabledMagnets().remove(uuid);
        plugin.getLastFarmingScanLocations().remove(uuid);
        if (plugin.getPlayerSettingsManager() != null) {
            plugin.getPlayerSettingsManager().getDisabledPlayers().remove(uuid);
        }
        if (plugin.getVisibilityPacketManager() != null) {
            plugin.getVisibilityPacketManager().removePlayer(uuid);
        }
        if (plugin.getGroupContainerManager() != null) {
            plugin.getGroupContainerManager().getOpenContainers().remove(uuid);
        }
    }
}
