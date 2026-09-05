package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ItemLifecycleListener implements Listener {

    private final LootGlow plugin;

    public ItemLifecycleListener(LootGlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropPreRegister(PlayerDropItemEvent event) {
        var applySvc = plugin.getService(fr.skynex.lootglow.service.ItemGlowApplyService.class);
        var cfgMgr = plugin.getConfigManager();
        if (applySvc != null && cfgMgr != null) {
            applySvc.preHideItem(event.getItemDrop(), plugin.isPluginEnabled(), cfgMgr.isRpgDropsEnabled(), plugin.getSourceMobKey(), plugin.getStateRepository().getItemCategories(), plugin.getStateRepository().getCategoryNames(), cfgMgr.getRpgEnabledCategories(), plugin.getStateRepository().getEntityIdMap(), plugin.getStateRepository().getHiddenVanillaItems());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        var cfgMgr = plugin.getConfigManager();
        if (cfgMgr != null && cfgMgr.isOnlyPlayerDrops()) return;
        Item item = event.getEntity();
        if (plugin.getStateRepository().getActiveItems().containsKey(item.getUniqueId())) return;
        var pipeline = plugin.getService(fr.skynex.lootglow.pipeline.LootRenderPipeline.class);
        if (pipeline != null) pipeline.render(item);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        var pipeline = plugin.getService(fr.skynex.lootglow.pipeline.LootRenderPipeline.class);
        if (pipeline != null) pipeline.render(event.getItemDrop());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        var spawner = plugin.getService(fr.skynex.lootglow.managers.VisualSpawner.class);
        if (spawner != null) spawner.removeGlow(event.getEntity().getUniqueId());
        
        var cfgMgr = plugin.getConfigManager();
        if (plugin.getStateRepository().getActiveItems().containsKey(event.getTarget().getUniqueId())) {
            if (cfgMgr != null && cfgMgr.isHoloEnabled()) {
                FoliaScheduler.runSync(plugin, () -> {
                    if (event.getTarget().isValid()) {
                        var holoSvc = plugin.getService(fr.skynex.lootglow.service.HologramService.class);
                        if (holoSvc != null) {
                            holoSvc.refreshHologram(event.getTarget(), cfgMgr.isHoloEnabled(), cfgMgr.isHoloHideUncategorized(), plugin.getStateRepository().getItemCategoriesCache(), plugin.getStateRepository().getItemCategories(), cfgMgr.getDefaultColor(), plugin.getStateRepository().getLastHoloState());
                        }
                    }
                });
            }
        } else if (cfgMgr != null && !cfgMgr.isOnlyPlayerDrops()) {
            FoliaScheduler.runSync(plugin, () -> {
                if (event.getTarget().isValid()) {
                    var pipeline = plugin.getService(fr.skynex.lootglow.pipeline.LootRenderPipeline.class);
                    if (pipeline != null) pipeline.render(event.getTarget(), false);
                }
            });
        }
    }
}
