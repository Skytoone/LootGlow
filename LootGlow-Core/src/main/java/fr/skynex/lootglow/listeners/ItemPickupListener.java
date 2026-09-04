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
        if (event.getEntity() instanceof Player player) {
            if (plugin.isRmbPickupEnabled() && plugin.isRmbPickupForce()) {
                event.setCancelled(true);
                return;
            }

            if (plugin.isContainerEnabled() && plugin.isContainerRequireClick()) {
                if (plugin.getGroupMembers().containsKey(event.getItem().getUniqueId()) ||
                    plugin.getGroupedItems().contains(event.getItem().getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (plugin.isHardLockEnabled() && event.getItem().getThrower() != null) {
                if (!event.getItem().getThrower().equals(player.getUniqueId()) && !player.hasPermission(plugin.getBypassPermission())) {
                    long spawnTime = plugin.getItemSpawnTimes().getOrDefault(event.getItem().getUniqueId(), 0L);
                    int duration = plugin.getProtectionDuration();
                    long elapsed = (System.currentTimeMillis() - spawnTime) / 1000L;

                    if (elapsed < duration) {
                        event.setCancelled(true);

                        Player owner = Bukkit.getPlayer(event.getItem().getThrower());
                        String ownerName = owner != null ? owner.getName() : "Inconnu";
                        long remaining = duration - elapsed;

                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("owner", ownerName);
                        placeholders.put("time", String.valueOf(remaining));

                        plugin.sendMessage(player, "cannot-pickup", placeholders);
                        return;
                    }
                }
            }

            String category = plugin.getTrackedItemManager() != null ? plugin.getTrackedItemManager().getItemCategoriesCache().get(event.getItem().getUniqueId()) : null;
            if (plugin.getLootEventDispatcher() != null) {
                boolean allowed = plugin.getLootEventDispatcher().handleItemPickup(player, event.getItem(), event.getItem().getItemStack(), category);
                if (!allowed) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (event.getRemaining() == 0) {
                plugin.playAspirationAnimation(event.getItem(), player);
                plugin.getLootRenderPipeline().unrender(event.getItem());
            } else {
                plugin.refreshHologram(event.getItem());
            }
        } else {
            if (event.getRemaining() == 0) {
                plugin.getLootRenderPipeline().unrender(event.getItem());
            } else {
                plugin.refreshHologram(event.getItem());
            }
        }
    }
}
