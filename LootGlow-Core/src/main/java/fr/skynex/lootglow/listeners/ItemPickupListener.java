package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.HashMap;
import java.util.Map;

public class ItemPickupListener implements Listener {

    private final LootGlow plugin;

    public ItemPickupListener(LootGlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        var cfgMgr = plugin.getConfigManager();
        if (event.getEntity() instanceof Player player) {
            if (cfgMgr != null && cfgMgr.isRmbPickupEnabled() && cfgMgr.isRmbPickupForce()) {
                event.setCancelled(true);
                return;
            }

            var gcMgr = plugin.getService(fr.skynex.lootglow.managers.GroupContainerManager.class);
            var groupMembers = gcMgr != null ? gcMgr.getGroupMembers() : plugin.getStateRepository().getGroupMembers();
            var groupedItems = gcMgr != null ? gcMgr.getGroupedItems() : plugin.getStateRepository().getGroupedItems();

            if (cfgMgr != null && cfgMgr.isContainerEnabled() && cfgMgr.isContainerRequireClick()) {
                if (groupMembers.containsKey(event.getItem().getUniqueId()) ||
                    groupedItems.contains(event.getItem().getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (cfgMgr != null && cfgMgr.isHardLockEnabled() && event.getItem().getThrower() != null) {
                if (!event.getItem().getThrower().equals(player.getUniqueId()) && !player.hasPermission(cfgMgr.getBypassPermission())) {
                    long spawnTime = plugin.getStateRepository().getItemSpawnTimes().getOrDefault(event.getItem().getUniqueId(), 0L);
                    int duration = cfgMgr.getProtectionDuration();
                    long elapsed = (System.currentTimeMillis() - spawnTime) / 1000L;

                    if (elapsed < duration) {
                        event.setCancelled(true);

                        Player owner = Bukkit.getPlayer(event.getItem().getThrower());
                        String ownerName = owner != null ? owner.getName() : "Inconnu";
                        long remaining = duration - elapsed;

                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("owner", ownerName);
                        placeholders.put("time", String.valueOf(remaining));

                        var msgSvc = plugin.getService(fr.skynex.lootglow.service.MessageService.class);
                        if (msgSvc != null) msgSvc.sendMessage(player, "cannot-pickup", placeholders);
                        return;
                    }
                }
            }

            var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
            String category = trackedMgr != null ? trackedMgr.getItemCategoriesCache().get(event.getItem().getUniqueId()) : plugin.getStateRepository().getItemCategoriesCache().get(event.getItem().getUniqueId());
            var dispatcher = plugin.getService(fr.skynex.lootglow.event.LootEventDispatcher.class);
            if (dispatcher != null) {
                boolean allowed = dispatcher.handleItemPickup(player, event.getItem(), event.getItem().getItemStack(), category);
                if (!allowed) {
                    event.setCancelled(true);
                    return;
                }
            }

            var pipeline = plugin.getService(fr.skynex.lootglow.pipeline.LootRenderPipeline.class);
            var holoSvc = plugin.getService(fr.skynex.lootglow.service.HologramService.class);
            var rpgMgr = plugin.getService(fr.skynex.lootglow.managers.RPGDropManager.class);

            if (event.getRemaining() == 0) {
                if (rpgMgr != null && cfgMgr != null) rpgMgr.playAspirationAnimation(event.getItem(), player, plugin.getStateRepository().getActiveItemVisuals(), cfgMgr.isAspirationEnabled());
                if (pipeline != null) pipeline.unrender(event.getItem());
            } else {
                if (holoSvc != null && cfgMgr != null) holoSvc.refreshHologram(event.getItem(), cfgMgr.isHoloEnabled(), cfgMgr.isHoloHideUncategorized(), plugin.getStateRepository().getItemCategoriesCache(), plugin.getStateRepository().getItemCategories(), cfgMgr.getDefaultColor(), plugin.getStateRepository().getLastHoloState());
            }
        } else {
            var pipeline = plugin.getService(fr.skynex.lootglow.pipeline.LootRenderPipeline.class);
            var holoSvc = plugin.getService(fr.skynex.lootglow.service.HologramService.class);
            if (event.getRemaining() == 0) {
                if (pipeline != null) pipeline.unrender(event.getItem());
            } else {
                if (holoSvc != null && cfgMgr != null) holoSvc.refreshHologram(event.getItem(), cfgMgr.isHoloEnabled(), cfgMgr.isHoloHideUncategorized(), plugin.getStateRepository().getItemCategoriesCache(), plugin.getStateRepository().getItemCategories(), cfgMgr.getDefaultColor(), plugin.getStateRepository().getLastHoloState());
            }
        }
    }
}
