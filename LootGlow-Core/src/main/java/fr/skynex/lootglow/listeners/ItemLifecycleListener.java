package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ItemLifecycleListener implements Listener {

    private final LootGlow plugin;

    public ItemLifecycleListener(LootGlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropPreRegister(PlayerDropItemEvent event) {
        plugin.preHideItem(event.getItemDrop());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (plugin.isOnlyPlayerDrops()) return;
        Item item = event.getEntity();
        if (plugin.getActiveItems().containsKey(item.getUniqueId())) return;
        plugin.getLootRenderPipeline().render(item);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        plugin.getLootRenderPipeline().render(event.getItemDrop());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        plugin.removeGlow(event.getEntity().getUniqueId());
        
        if (plugin.getActiveItems().containsKey(event.getTarget().getUniqueId())) {
            if (plugin.isHoloEnabled()) {
                FoliaScheduler.runSync(plugin, () -> {
                    if (event.getTarget().isValid()) {
                        plugin.refreshHologram(event.getTarget());
                    }
                });
            }
        } else if (!plugin.isOnlyPlayerDrops()) {
            FoliaScheduler.runSync(plugin, () -> {
                if (event.getTarget().isValid()) {
                    plugin.getLootRenderPipeline().render(event.getTarget(), false);
                }
            });
        }
    }
}
