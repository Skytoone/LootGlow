package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages item tracking maps, spatial indexing per world, and display entity
 * attachments.
 */
public class TrackedItemManager {

    private final LootGlow plugin;

    public static class TrackedItem extends fr.skynex.lootglow.model.TrackedItem {
    }

    private final Map<UUID, TrackedItem> trackedItems = new ConcurrentHashMap<>();
    private final Map<UUID, Item> activeItems = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> itemsByWorld = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> entityIdMap = new ConcurrentHashMap<>();
    private final Set<UUID> globallyVisibleEntities = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> itemCategoriesCache = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> displayToItemMap = new ConcurrentHashMap<>();

    public TrackedItemManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, TrackedItem> getTrackedItems() {
        return trackedItems;
    }

    public Map<UUID, Item> getActiveItems() {
        return activeItems;
    }

    public Map<String, Set<UUID>> getItemsByWorld() {
        return itemsByWorld;
    }

    public Map<Integer, UUID> getEntityIdMap() {
        return entityIdMap;
    }

    public Set<UUID> getGloballyVisibleEntities() {
        return globallyVisibleEntities;
    }

    public Map<UUID, String> getItemCategoriesCache() {
        return itemCategoriesCache;
    }

    public Map<UUID, UUID> getDisplayToItemMap() {
        return displayToItemMap;
    }

    public void registerDisplayEntity(UUID displayUuid, UUID itemUuid) {
        if (displayUuid != null && itemUuid != null) {
            displayToItemMap.put(displayUuid, itemUuid);
        }
    }

    public void unregisterDisplayEntity(UUID displayUuid) {
        if (displayUuid != null) {
            displayToItemMap.remove(displayUuid);
        }
    }

    public TrackedItem getTrackedItem(UUID uuid) {
        return trackedItems.get(uuid);
    }

    public TrackedItem getOrCreateTrackedItem(UUID uuid) {
        return trackedItems.computeIfAbsent(uuid, k -> new TrackedItem());
    }

    public void setItemCategory(UUID uuid, String category) {
        if (uuid == null || category == null)
            return;
        itemCategoriesCache.put(uuid, category);
        TrackedItem ti = trackedItems.get(uuid);
        if (ti != null) {
            ti.category = category;
        }
    }

    public String getItemCategory(UUID uuid) {
        if (uuid == null)
            return null;
        String cat = itemCategoriesCache.get(uuid);
        if (cat != null)
            return cat;
        TrackedItem ti = trackedItems.get(uuid);
        return ti != null ? ti.category : null;
    }

    public Item getItemForDisplay(ItemDisplay display) {
        if (display == null)
            return null;
        UUID displayUuid = display.getUniqueId();
        UUID itemUuid = displayToItemMap.get(displayUuid);
        return itemUuid != null ? activeItems.get(itemUuid) : null;
    }

    public Item getItemForLabel(TextDisplay label) {
        if (label == null)
            return null;
        UUID labelUuid = label.getUniqueId();
        UUID itemUuid = displayToItemMap.get(labelUuid);
        return itemUuid != null ? activeItems.get(itemUuid) : null;
    }

    public void registerItem(Item item) {
        UUID uuid = item.getUniqueId();
        activeItems.put(uuid, item);
        entityIdMap.put(item.getEntityId(), uuid);
        itemsByWorld.computeIfAbsent(item.getWorld().getName(), k -> ConcurrentHashMap.newKeySet()).add(uuid);
    }

    public void untrackItem(UUID uuid) {
        itemCategoriesCache.remove(uuid);
        TrackedItem ti = trackedItems.remove(uuid);
        if (ti != null) {
            cleanDisplayVisibility(ti.label);
            cleanDisplayVisibility(ti.beam);
            if (ti.beam != null && ti.beam.isValid()) {
                ti.beam.getPassengers().forEach(this::cleanDisplayVisibility);
            }
            cleanDisplayVisibility(ti.visual);
            cleanDisplayVisibility(ti.shadow);

            if (ti.label != null) {
                displayToItemMap.remove(ti.label.getUniqueId());
                if (ti.label.isValid())
                    ti.label.remove();
            }
            if (ti.beam != null) {
                displayToItemMap.remove(ti.beam.getUniqueId());
                if (ti.beam.isValid()) {
                    ti.beam.getPassengers().forEach(e -> {
                        if (e != null) {
                            displayToItemMap.remove(e.getUniqueId());
                            e.remove();
                        }
                    });
                    ti.beam.remove();
                }
            }
            if (ti.visual != null) {
                displayToItemMap.remove(ti.visual.getUniqueId());
                if (ti.visual.isValid())
                    ti.visual.remove();
            }
            if (ti.shadow != null) {
                displayToItemMap.remove(ti.shadow.getUniqueId());
                if (ti.shadow.isValid())
                    ti.shadow.remove();
            }
        }
        if (plugin.getParticleAnimationManager() != null) {
            plugin.getParticleAnimationManager().getCustomParticles().remove(uuid);
        }
        if (plugin.getHologramRenderer() != null) {
            plugin.getHologramRenderer().getCustomHolograms().remove(uuid);
        }
        if (plugin.getSurfaceAlignmentManager() != null) {
            plugin.getSurfaceAlignmentManager().getSurfaceStates().remove(uuid);
            plugin.getSurfaceAlignmentManager().getWaterLogCache().remove(uuid);
        }
        Item item = activeItems.remove(uuid);
        if (item != null) {
            entityIdMap.remove(item.getEntityId());
            Set<UUID> worldItems = itemsByWorld.get(item.getWorld().getName());
            if (worldItems != null) {
                worldItems.remove(uuid);
            }
        }
    }

    private void cleanDisplayVisibility(org.bukkit.entity.Entity entity) {
        if (entity == null) return;
        UUID eUuid = entity.getUniqueId();
        for (Set<UUID> set : plugin.getVisibleEntities().values()) {
            if (set != null) set.remove(eUuid);
        }
    }

    private org.bukkit.scheduler.BukkitTask gcTask;

    public void startGarbageCollectorTask(boolean isEnabled, Map<UUID, Item> activeItems) {
        if (gcTask != null) {
            gcTask.cancel();
            gcTask = null;
        }

        gcTask = fr.skynex.lootglow.util.FoliaScheduler.runTimer(plugin, () -> {
            if (!isEnabled)
                return;

            List<UUID> toRemove = new ArrayList<>();
            for (Map.Entry<UUID, Item> entry : activeItems.entrySet()) {
                if (entry.getValue() == null || !entry.getValue().isValid() || entry.getValue().isDead()) {
                    toRemove.add(entry.getKey());
                }
            }

            for (UUID uuid : toRemove) {
                plugin.removeGlow(uuid);
            }
        }, 600L, 600L);
    }

    public List<Item> getNearbyGlowingItems(org.bukkit.Location location, double radius) {
        if (location == null || location.getWorld() == null)
            return List.of();
        double radiusSq = radius * radius;
        List<Item> result = new ArrayList<>();
        for (org.bukkit.entity.Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius,
                e -> e instanceof Item)) {
            Item item = (Item) entity;
            if (item.isValid() && item.isGlowing() && item.getLocation().distanceSquared(location) <= radiusSq) {
                result.add(item);
            }
        }
        return result;
    }

    public void clearAll() {
        if (gcTask != null) {
            gcTask.cancel();
            gcTask = null;
        }
        for (TrackedItem ti : trackedItems.values()) {
            if (ti.label != null && ti.label.isValid())
                ti.label.remove();
            if (ti.beam != null && ti.beam.isValid()) {
                ti.beam.getPassengers().forEach(e -> {
                    if (e != null)
                        e.remove();
                });
                ti.beam.remove();
            }
            if (ti.visual != null && ti.visual.isValid())
                ti.visual.remove();
            if (ti.shadow != null && ti.shadow.isValid())
                ti.shadow.remove();
        }
        trackedItems.clear();
        activeItems.clear();
        itemsByWorld.clear();
        entityIdMap.clear();
        globallyVisibleEntities.clear();
        itemCategoriesCache.clear();
        displayToItemMap.clear();
    }
}
