package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages item tracking maps, spatial indexing per world, and display entity attachments.
 */
public class TrackedItemManager {

    private final LootGlow plugin;

    public static class TrackedItem {
        public TextDisplay label;
        public BlockDisplay beam;
        public ItemDisplay visual;
        public Display shadow;
        public Long spawnTime;
        public Long lastHoloState;
        public Component baseName;
        public String category;
        public Particle particle;
        public Double moneyAmount;
        public int lastRayTraceTick = -999;
    }

    private final Map<UUID, TrackedItem> trackedItems = new ConcurrentHashMap<>();
    private final Map<UUID, Item> activeItems = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> itemsByWorld = new HashMap<>();
    private final Map<Integer, UUID> entityIdMap = new ConcurrentHashMap<>();
    private final Set<UUID> globallyVisibleEntities = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> itemCategoriesCache = new ConcurrentHashMap<>();

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

    public TrackedItem getTrackedItem(UUID uuid) {
        return trackedItems.get(uuid);
    }

    public TrackedItem getOrCreateTrackedItem(UUID uuid) {
        return trackedItems.computeIfAbsent(uuid, k -> new TrackedItem());
    }

    public void setItemCategory(UUID uuid, String category) {
        if (uuid == null || category == null) return;
        itemCategoriesCache.put(uuid, category);
        TrackedItem ti = trackedItems.get(uuid);
        if (ti != null) {
            ti.category = category;
        }
    }

    public String getItemCategory(UUID uuid) {
        if (uuid == null) return null;
        String cat = itemCategoriesCache.get(uuid);
        if (cat != null) return cat;
        TrackedItem ti = trackedItems.get(uuid);
        return ti != null ? ti.category : null;
    }

    public Item getItemForDisplay(ItemDisplay display) {
        if (display == null) return null;
        for (Map.Entry<UUID, TrackedItem> entry : trackedItems.entrySet()) {
            if (entry.getValue() != null && display.equals(entry.getValue().visual)) {
                return activeItems.get(entry.getKey());
            }
        }
        return null;
    }

    public Item getItemForLabel(TextDisplay label) {
        if (label == null) return null;
        for (Map.Entry<UUID, TrackedItem> entry : trackedItems.entrySet()) {
            if (entry.getValue() != null && label.equals(entry.getValue().label)) {
                return activeItems.get(entry.getKey());
            }
        }
        return null;
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
            if (ti.label != null && ti.label.isValid()) ti.label.remove();
            if (ti.beam != null && ti.beam.isValid()) {
                ti.beam.getPassengers().forEach(e -> { if (e != null) e.remove(); });
                ti.beam.remove();
            }
            if (ti.visual != null && ti.visual.isValid()) ti.visual.remove();
            if (ti.shadow != null && ti.shadow.isValid()) ti.shadow.remove();
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

    public void startGarbageCollectorTask(boolean isEnabled, Map<UUID, Item> activeItems) {
        fr.skynex.lootglow.util.FoliaScheduler.runTimer(plugin, () -> {
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
        if (location == null || location.getWorld() == null) return List.of();
        double radiusSq = radius * radius;
        List<Item> result = new ArrayList<>();
        for (Item item : location.getWorld().getEntitiesByClass(Item.class)) {
            if (item.isValid() && item.isGlowing() && item.getLocation().distanceSquared(location) <= radiusSq) {
                result.add(item);
            }
        }
        return result;
    }

    public void clearAll() {
        for (TrackedItem ti : trackedItems.values()) {
            if (ti.label != null && ti.label.isValid()) ti.label.remove();
            if (ti.beam != null && ti.beam.isValid()) {
                ti.beam.getPassengers().forEach(e -> { if (e != null) e.remove(); });
                ti.beam.remove();
            }
            if (ti.visual != null && ti.visual.isValid()) ti.visual.remove();
            if (ti.shadow != null && ti.shadow.isValid()) ti.shadow.remove();
        }
        trackedItems.clear();
        activeItems.clear();
        itemsByWorld.clear();
        entityIdMap.clear();
        globallyVisibleEntities.clear();
        itemCategoriesCache.clear();
    }
}
