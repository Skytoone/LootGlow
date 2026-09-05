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
        var db = plugin.getService(fr.skynex.lootglow.database.DatabaseManager.class);
        if (db != null) {
            db.loadPlayerData(event.getPlayer(), plugin.getStateRepository().getHiddenVisuals(), plugin.getStateRepository().getDisabledMagnets());
        }

        FoliaScheduler.runLater(plugin, () -> {
            Player p = event.getPlayer();
            var cfgMgr = plugin.getConfigManager();
            if (!p.isOnline() || cfgMgr == null || !cfgMgr.isEnabled()) return;
            var visSvc = plugin.getService(fr.skynex.lootglow.service.EntityVisibilityService.class);
            var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
            var activeItems = trackedMgr != null ? trackedMgr.getActiveItems() : plugin.getStateRepository().getActiveItems();
            if (visSvc != null) {
                visSvc.refreshGlowForPlayer(p, !plugin.getStateRepository().getHiddenVisuals().contains(p.getUniqueId()), plugin.getStateRepository().getHiddenVanillaItems(), plugin.getStateRepository().getEntityIdMap(), plugin.getStateRepository().getVisibleEntities(), cfgMgr.getFarmingViewDistance(), activeItems, plugin.getStateRepository().getGroupedItems(), cfgMgr.getLodHoloDistSq(), cfgMgr.getLodBeamDistSq(), plugin.getStateRepository().getActiveCropSymbols());
            }
            var groupedItems = plugin.getStateRepository().getGroupedItems();
            for (org.bukkit.entity.Item item : activeItems.values()) {
                if (item.getWorld().equals(p.getWorld())) {
                    if (groupedItems.contains(item.getUniqueId())) {
                        p.hideEntity(plugin, item);
                    } else if (!plugin.getStateRepository().getHiddenVanillaItems().contains(item.getEntityId())) {
                        p.hideEntity(plugin, item);
                        p.showEntity(plugin, item);
                    }
                }
            }
        }, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        var dispMgr = plugin.getService(fr.skynex.lootglow.managers.VisualDisplayManager.class);
        if (dispMgr != null) dispMgr.clearVisualsForPlayer(player, plugin.getStateRepository().getTrackedItems());
        FoliaScheduler.runLater(plugin, () -> {
            var cfgMgr = plugin.getConfigManager();
            if (player.isOnline() && cfgMgr != null && cfgMgr.isEnabled()) {
                var visSvc = plugin.getService(fr.skynex.lootglow.service.EntityVisibilityService.class);
                var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
                var activeItems = trackedMgr != null ? trackedMgr.getActiveItems() : plugin.getStateRepository().getActiveItems();
                if (visSvc != null) {
                    visSvc.refreshGlowForPlayer(player, !plugin.getStateRepository().getHiddenVisuals().contains(player.getUniqueId()), plugin.getStateRepository().getHiddenVanillaItems(), plugin.getStateRepository().getEntityIdMap(), plugin.getStateRepository().getVisibleEntities(), cfgMgr.getFarmingViewDistance(), activeItems, plugin.getStateRepository().getGroupedItems(), cfgMgr.getLodHoloDistSq(), cfgMgr.getLodBeamDistSq(), plugin.getStateRepository().getActiveCropSymbols());
                }
                var groupedItems = plugin.getStateRepository().getGroupedItems();
                for (org.bukkit.entity.Item item : activeItems.values()) {
                    if (item.getWorld().equals(player.getWorld())) {
                        if (groupedItems.contains(item.getUniqueId())) {
                            player.hideEntity(plugin, item);
                        } else if (!plugin.getStateRepository().getHiddenVanillaItems().contains(item.getEntityId())) {
                            player.hideEntity(plugin, item);
                            player.showEntity(plugin, item);
                        }
                    }
                }
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportPlayer(PlayerTeleportEvent event) {
        if (event.getTo() != null && !event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            var dispMgr = plugin.getService(fr.skynex.lootglow.managers.VisualDisplayManager.class);
            if (dispMgr != null) dispMgr.clearVisualsForPlayer(event.getPlayer(), plugin.getStateRepository().getTrackedItems());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getStateRepository().getVisibleEntities().remove(uuid);
        plugin.getStateRepository().getHiddenVisuals().remove(uuid);
        plugin.getStateRepository().getDisabledMagnets().remove(uuid);
        plugin.getStateRepository().getLastFarmingScanLocations().remove(uuid);
        var pSettings = plugin.getService(fr.skynex.lootglow.managers.PlayerSettingsManager.class);
        if (pSettings != null) {
            pSettings.getDisabledPlayers().remove(uuid);
        }
        var pktMgr = plugin.getService(fr.skynex.lootglow.managers.VisibilityPacketManager.class);
        if (pktMgr != null) {
            pktMgr.removePlayer(uuid);
        }
        var grpMgr = plugin.getService(fr.skynex.lootglow.managers.GroupContainerManager.class);
        if (grpMgr != null) {
            grpMgr.getOpenContainers().remove(uuid);
        }
    }
}
