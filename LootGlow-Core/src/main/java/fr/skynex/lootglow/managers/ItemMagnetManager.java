package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages VIP magnet item attraction features and inventory capacity checks.
 */
public class ItemMagnetManager {

    private final LootGlow plugin;
    private final Set<UUID> disabledMagnets = ConcurrentHashMap.newKeySet();

    public ItemMagnetManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Set<UUID> getDisabledMagnets() {
        return disabledMagnets;
    }

    public boolean isMagnetEnabled(Player player) {
        return player != null && !disabledMagnets.contains(player.getUniqueId());
    }

    public void setMagnetEnabled(Player player, boolean enabled) {
        if (player == null) return;
        if (enabled) {
            disabledMagnets.remove(player.getUniqueId());
        } else {
            disabledMagnets.add(player.getUniqueId());
        }
    }

    public boolean canFit(Inventory inv, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (inv.firstEmpty() != -1) return true;
        int maxStack = item.getMaxStackSize();
        if (maxStack <= 1) return false;
        for (ItemStack is : inv.getStorageContents()) {
            if (is != null && is.isSimilar(item)) {
                if (is.getAmount() < maxStack) {
                    return true;
                }
            }
        }
        return false;
    }

    public void tickMagnet(boolean magnetEnabled,
                           double magnetDistance,
                           String magnetPermission,
                           List<String> magnetCategories,
                           boolean magnetEnableForGroups,
                           Map<UUID, ?> groupLeaders,
                           Map<UUID, ?> groupMembers,
                           Set<UUID> groupedItems,
                           Map<UUID, String> itemCategoriesCache,
                           boolean protectionEnabled,
                           int protectionDuration,
                           Map<UUID, Long> itemSpawnTimes) {

        if (!magnetEnabled) return;

        double dist = magnetDistance;
        double distSq = dist * dist;
        String perm = magnetPermission;
        List<String> magnetCats = magnetCategories;
        int chunkRadius = (int) Math.ceil(dist / 16.0);

        var trackedMgr = plugin.getService(TrackedItemManager.class);
        var spatialSvc = plugin.getService(fr.skynex.lootglow.spatial.LootSpatialIndexService.class);

        Set<UUID> nearbyUuids = new java.util.HashSet<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (disabledMagnets.contains(p.getUniqueId()) || !p.hasPermission(perm)) continue;

            Location pLoc = p.getLocation();
            double px = pLoc.getX(), py = pLoc.getY() + 1.0, pz = pLoc.getZ();
            nearbyUuids.clear();

            if (trackedMgr != null) {
                trackedMgr.getItemsInChunkRadius(p.getWorld(), pLoc.getBlockX() >> 4, pLoc.getBlockZ() >> 4, chunkRadius, nearbyUuids);
            } else if (spatialSvc != null) {
                spatialSvc.getNearbyItemUuids(pLoc, dist, nearbyUuids);
            }

            if (!nearbyUuids.isEmpty()) {
                Map<UUID, Item> activeItems = trackedMgr != null ? trackedMgr.getActiveItems() : plugin.getStateRepository().getActiveItems();
                for (UUID uuid : nearbyUuids) {
                    Item item = activeItems.get(uuid);
                    if (item == null || !item.isValid() || item.getPickupDelay() > 0) continue;

                    double dx = px - item.getX();
                    double dy = py - item.getY();
                    double dz = pz - item.getZ();
                    double d2 = dx * dx + dy * dy + dz * dz;

                    if (d2 < 0.04 || d2 > distSq) continue;

                    if (!magnetEnableForGroups && (groupLeaders.containsKey(uuid) || groupMembers.containsKey(uuid) || groupedItems.contains(uuid)))
                        continue;

                    String category = itemCategoriesCache.get(uuid);
                    if (!magnetCats.isEmpty() && (category == null || !magnetCats.contains(category.toLowerCase())))
                        continue;

                    UUID owner = item.getThrower();
                    if (owner != null && !owner.equals(p.getUniqueId()) && protectionEnabled) {
                        long spawnTime = itemSpawnTimes.getOrDefault(uuid, System.currentTimeMillis());
                        if (System.currentTimeMillis() - spawnTime < (protectionDuration * 1000L)) {
                            if (!p.hasPermission("lootglow.bypass.lock")) continue;
                        }
                    }

                    if (!canFit(p.getInventory(), item.getItemStack())) continue;

                    double d = Math.sqrt(d2);
                    if (d < 0.01) continue;
                    double speed = 0.4;
                    Vector vel = new Vector((dx / d) * speed, (dy / d) * speed, (dz / d) * speed);
                    FoliaScheduler.runAtEntity(plugin, item, () -> {
                        if (item.isValid()) {
                            item.setVelocity(vel);
                        }
                    });
                }
            } else if (trackedMgr == null && spatialSvc == null) {
                for (Entity ent : p.getWorld().getNearbyEntities(pLoc, dist, dist, dist, e -> e instanceof Item)) {
                    Item item = (Item) ent;
                    if (!item.isValid() || item.getPickupDelay() > 0) continue;

                    double dx = px - item.getX();
                    double dy = py - item.getY();
                    double dz = pz - item.getZ();
                    double d2 = dx * dx + dy * dy + dz * dz;

                    if (d2 < 0.04 || d2 > distSq) continue;

                    UUID itemUuid = item.getUniqueId();
                    if (!magnetEnableForGroups && (groupLeaders.containsKey(itemUuid) || groupMembers.containsKey(itemUuid) || groupedItems.contains(itemUuid)))
                        continue;

                    String category = itemCategoriesCache.get(itemUuid);
                    if (!magnetCats.isEmpty() && (category == null || !magnetCats.contains(category.toLowerCase())))
                        continue;

                    UUID owner = item.getThrower();
                    if (owner != null && !owner.equals(p.getUniqueId()) && protectionEnabled) {
                        long spawnTime = itemSpawnTimes.getOrDefault(itemUuid, System.currentTimeMillis());
                        if (System.currentTimeMillis() - spawnTime < (protectionDuration * 1000L)) {
                            if (!p.hasPermission("lootglow.bypass.lock")) continue;
                        }
                    }

                    if (!canFit(p.getInventory(), item.getItemStack())) continue;

                    double d = Math.sqrt(d2);
                    if (d < 0.01) continue;
                    double speed = 0.4;
                    Vector vel = new Vector((dx / d) * speed, (dy / d) * speed, (dz / d) * speed);
                    FoliaScheduler.runAtEntity(plugin, item, () -> {
                        if (item.isValid()) {
                            item.setVelocity(vel);
                        }
                    });
                }
            }
        }
    }

    public void pullItemsToPlayer(Player player, double radius) {
        if (player == null || !player.isOnline()) return;
        Location loc = player.getLocation();
        double px = loc.getX();
        double py = loc.getY();
        double pz = loc.getZ();
        double radiusSq = radius * radius;
        int chunkRadius = (int) Math.ceil(radius / 16.0);
        var trackedMgr = plugin.getService(TrackedItemManager.class);
        Set<UUID> nearbyItems = trackedMgr != null ? trackedMgr.getItemsInChunkRadius(player.getWorld(), ((int) px) >> 4, ((int) pz) >> 4, chunkRadius) : null;
        if (nearbyItems != null && !nearbyItems.isEmpty()) {
            for (UUID uuid : nearbyItems) {
                Item item = trackedMgr != null ? trackedMgr.getActiveItems().get(uuid) : null;
                if (item != null && item.isValid()) {
                    double dx = px - item.getX();
                    double dy = py - item.getY();
                    double dz = pz - item.getZ();
                    if ((dx * dx + dy * dy + dz * dz) <= radiusSq) {
                        item.teleport(loc);
                    }
                }
            }
        }
    }

    public void clearAll() {
        disabledMagnets.clear();
    }
}
