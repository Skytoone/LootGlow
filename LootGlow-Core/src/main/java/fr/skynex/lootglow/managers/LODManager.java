package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages Level of Detail (LOD) distance calculations and rendering optimization tasks.
 */
public class LODManager {

    private final LootGlow plugin;

    public LODManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public boolean isLodEnabled() {
        return plugin.getConfig().getBoolean("settings.lod.enabled", false);
    }

    public double getLodHoloDistanceSquared() {
        double dist = plugin.getConfig().getDouble("settings.lod.hologram-distance", 32.0);
        return dist * dist;
    }

    public double getLodBeamDistanceSquared() {
        double dist = plugin.getConfig().getDouble("settings.lod.beam-distance", 64.0);
        return dist * dist;
    }

    private org.bukkit.scheduler.BukkitTask lodTask;

    public void startLODTask(boolean isEnabled,
                             boolean lodEnabled,
                             double lodBeamDistSq,
                             double lodHoloDistSq,
                             double farmingViewDistance,
                             Map<UUID, Set<UUID>> visibleEntities,
                             Set<UUID> hiddenVisuals,
                             Map<UUID, Item> activeItems,
                             Set<UUID> groupedItems,
                             Map<UUID, TextDisplay> activeLabels,
                             Map<UUID, BlockDisplay> activeBeams,
                             Map<UUID, ItemDisplay> activeItemVisuals,
                             Map<UUID, Display> activeShadows,
                             Map<String, Set<UUID>> itemsByWorld,
                             boolean farmingEnabled,
                             Map<org.bukkit.block.Block, ? extends List<BlockDisplay>> activeCropSymbols,
                             int lodInterval,
                             Set<UUID> globallyVisibleEntities) {

        if (lodTask != null) {
            lodTask.cancel();
            lodTask = null;
        }

        lodTask = FoliaScheduler.runTimer(plugin, () -> {
            if (!isEnabled || !lodEnabled) return;

            Set<UUID> newGloballyVisible = new HashSet<>();
            double farmDistSq = farmingViewDistance * farmingViewDistance;

            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID pUuid = p.getUniqueId();
                World pWorld = p.getWorld();
                String worldName = pWorld.getName();
                double px = p.getX(), py = p.getY(), pz = p.getZ();
                Set<UUID> visibleSet = visibleEntities.computeIfAbsent(pUuid, k -> java.util.concurrent.ConcurrentHashMap.newKeySet());
                boolean isHiddenToggle = hiddenVisuals.contains(pUuid);

                double maxDist = Math.sqrt(Math.max(lodBeamDistSq, lodHoloDistSq));
                int chunkRadius = (int) Math.ceil(maxDist / 16.0);
                Set<UUID> nearbyItemUuids = plugin.getTrackedItemManager() != null
                        ? plugin.getTrackedItemManager().getItemsInChunkRadius(pWorld, ((int) px) >> 4, ((int) pz) >> 4, chunkRadius)
                        : itemsByWorld.get(worldName);

                if (nearbyItemUuids != null && !nearbyItemUuids.isEmpty()) {
                    for (UUID uuid : nearbyItemUuids) {
                        Item item = activeItems.get(uuid);
                        if (item == null || !item.isValid()) continue;

                        double ix = item.getX(), iy = item.getY(), iz = item.getZ();
                        double dx = px - ix, dy = py - iy, dz = pz - iz;
                        double dSq = dx * dx + dy * dy + dz * dz;
                        boolean isGrouped = groupedItems.contains(uuid);

                        TextDisplay label = activeLabels.get(uuid);
                        if (label != null && label.isValid()) {
                            boolean shouldSee = !isHiddenToggle && !isGrouped && dSq <= lodHoloDistSq;
                            plugin.updateEntityVisibility(p, label, shouldSee, visibleSet);
                        }

                        BlockDisplay beam = activeBeams.get(uuid);
                        if (beam != null && beam.isValid()) {
                            boolean shouldSee = !isHiddenToggle && !isGrouped && dSq <= lodBeamDistSq;
                            plugin.updateEntityVisibility(p, beam, shouldSee, visibleSet);
                            if (shouldSee) newGloballyVisible.add(uuid);
                        }

                        ItemDisplay visual = activeItemVisuals.get(uuid);
                        if (visual != null && visual.isValid()) {
                            boolean shouldSee = !isHiddenToggle && !isGrouped && dSq <= lodHoloDistSq;
                            plugin.updateEntityVisibility(p, visual, shouldSee, visibleSet);
                        }

                        Display shadow = activeShadows.get(uuid);
                        if (shadow != null && shadow.isValid()) {
                            boolean shouldSee = !isHiddenToggle && !isGrouped && dSq <= lodHoloDistSq;
                            plugin.updateEntityVisibility(p, shadow, shouldSee, visibleSet);
                        }
                    }
                }

                if (farmingEnabled) {
                    for (Map.Entry<org.bukkit.block.Block, ? extends List<BlockDisplay>> entry : activeCropSymbols.entrySet()) {
                        if (!entry.getKey().getWorld().equals(pWorld)) continue;

                        BlockDisplay first = entry.getValue().isEmpty() ? null : entry.getValue().get(0);
                        Location csLoc = first != null ? first.getLocation() : null;
                        if (csLoc == null) continue;

                        double cdx = px - csLoc.getX(), cdy = py - csLoc.getY(), cdz = pz - csLoc.getZ();
                        boolean shouldSee = !isHiddenToggle && (cdx * cdx + cdy * cdy + cdz * cdz) <= farmDistSq;

                        for (BlockDisplay bd : entry.getValue()) {
                            if (bd.isValid()) {
                                plugin.updateEntityVisibility(p, bd, shouldSee, visibleSet);
                                if (shouldSee) newGloballyVisible.add(bd.getUniqueId());
                            }
                        }
                    }
                }
            }
            globallyVisibleEntities.clear();
            globallyVisibleEntities.addAll(newGloballyVisible);
        }, 200L, (long) lodInterval);
    }
}
