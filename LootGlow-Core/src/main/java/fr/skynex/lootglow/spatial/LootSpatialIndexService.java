package fr.skynex.lootglow.spatial;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spatial Chunk Grid Indexing Service for LootGlow.
 * Fast O(1) spatial partitioning based on Minecraft chunk keys (chunkX, chunkZ).
 * Eliminates O(N) linear scans across all active items for magnet, LOD, and proximity queries.
 */
public class LootSpatialIndexService {

    private final LootGlow plugin;
    // Map<WorldName, Map<ChunkKey, Set<ItemUUID>>>
    private final Map<String, Map<Long, Set<UUID>>> worldSpatialIndex = new ConcurrentHashMap<>();
    private final Map<UUID, Long> itemChunkCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> itemWorldCache = new ConcurrentHashMap<>();

    public LootSpatialIndexService(LootGlow plugin) {
        this.plugin = plugin;
    }

    /**
     * Compute a 64-bit unique chunk key from 32-bit chunk coordinates.
     */
    public static long toChunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Compute chunk key from Location coordinates.
     */
    public static long toChunkKey(Location loc) {
        return toChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    /**
     * Registers an item into the spatial grid index.
     */
    public void register(Location loc, UUID itemUuid) {
        if (loc == null || loc.getWorld() == null || itemUuid == null) return;
        String worldName = loc.getWorld().getName();
        long chunkKey = toChunkKey(loc);

        worldSpatialIndex
                .computeIfAbsent(worldName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet())
                .add(itemUuid);

        itemChunkCache.put(itemUuid, chunkKey);
        itemWorldCache.put(itemUuid, worldName);
    }

    /**
     * Updates an item's position in the spatial index if it moved to a new chunk.
     */
    public void updatePosition(Location oldLoc, Location newLoc, UUID itemUuid) {
        if (itemUuid == null || newLoc == null || newLoc.getWorld() == null) return;

        long newChunkKey = toChunkKey(newLoc);
        String newWorldName = newLoc.getWorld().getName();

        Long oldChunkKey = itemChunkCache.get(itemUuid);
        String oldWorldName = itemWorldCache.get(itemUuid);

        if (oldChunkKey != null && oldChunkKey == newChunkKey && newWorldName.equals(oldWorldName)) {
            return; // Remained in the same chunk
        }

        if (oldWorldName != null && oldChunkKey != null) {
            Map<Long, Set<UUID>> chunkMap = worldSpatialIndex.get(oldWorldName);
            if (chunkMap != null) {
                Set<UUID> set = chunkMap.get(oldChunkKey);
                if (set != null) {
                    set.remove(itemUuid);
                }
            }
        }

        register(newLoc, itemUuid);
    }

    /**
     * Removes an item from the spatial index.
     */
    public void unregister(UUID itemUuid) {
        if (itemUuid == null) return;
        String worldName = itemWorldCache.remove(itemUuid);
        Long chunkKey = itemChunkCache.remove(itemUuid);

        if (worldName != null && chunkKey != null) {
            Map<Long, Set<UUID>> chunkMap = worldSpatialIndex.get(worldName);
            if (chunkMap != null) {
                Set<UUID> set = chunkMap.get(chunkKey);
                if (set != null) {
                    set.remove(itemUuid);
                }
            }
        }
    }

    /**
     * Queries item UUIDs within a given radius using chunk-based spatial lookup.
     * Iterates only over surrounding chunks in the radius.
     */
    public List<UUID> getNearbyItemUuids(Location center, double radius) {
        if (center == null || center.getWorld() == null || radius <= 0) return Collections.emptyList();

        String worldName = center.getWorld().getName();
        Map<Long, Set<UUID>> chunkMap = worldSpatialIndex.get(worldName);
        if (chunkMap == null || chunkMap.isEmpty()) return Collections.emptyList();

        int minChunkX = (center.getBlockX() - (int) Math.ceil(radius)) >> 4;
        int maxChunkX = (center.getBlockX() + (int) Math.ceil(radius)) >> 4;
        int minChunkZ = (center.getBlockZ() - (int) Math.ceil(radius)) >> 4;
        int maxChunkZ = (center.getBlockZ() + (int) Math.ceil(radius)) >> 4;

        List<UUID> result = new ArrayList<>();

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                long key = toChunkKey(cx, cz);
                Set<UUID> itemsInChunk = chunkMap.get(key);
                if (itemsInChunk != null && !itemsInChunk.isEmpty()) {
                    result.addAll(itemsInChunk);
                }
            }
        }

        return result;
    }

    /**
     * Clears all indexed entries for a world when unloaded.
     */
    public void clearWorld(String worldName) {
        if (worldName == null) return;
        Map<Long, Set<UUID>> chunkMap = worldSpatialIndex.remove(worldName);
        if (chunkMap != null) {
            for (Set<UUID> set : chunkMap.values()) {
                for (UUID uuid : set) {
                    itemChunkCache.remove(uuid);
                    itemWorldCache.remove(uuid);
                }
            }
        }
    }

    /**
     * Clears the entire spatial index.
     */
    public void clearAll() {
        worldSpatialIndex.clear();
        itemChunkCache.clear();
        itemWorldCache.clear();
    }
}
