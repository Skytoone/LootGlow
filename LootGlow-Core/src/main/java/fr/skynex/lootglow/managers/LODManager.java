package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
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

        FoliaScheduler.runTimer(plugin, () -> {
            if (!isEnabled || !lodEnabled) return;

            Set<UUID> newGloballyVisible = new HashSet<>();
            double maxLodRadius = Math.sqrt(Math.max(lodBeamDistSq, lodHoloDistSq));
            double farmDistSq = farmingViewDistance * farmingViewDistance;

            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID pUuid = p.getUniqueId();
                World pWorld = p.getWorld();
                String worldName = pWorld.getName();
                double px = p.getX(), py = p.getY(), pz = p.getZ();
                Set<UUID> visibleSet = visibleEntities.computeIfAbsent(pUuid, k -> new HashSet<>());
                boolean isHiddenToggle = hiddenVisuals.contains(pUuid);

                Set<UUID> inRangeItemUuids = new HashSet<>();

                for (Entity ent : p.getNearbyEntities(maxLodRadius, maxLodRadius, maxLodRadius)) {
                    if (!(ent instanceof Item item)) continue;
                    UUID uuid = item.getUniqueId();
                    if (!activeItems.containsKey(uuid)) continue;
                    inRangeItemUuids.add(uuid);

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

                Set<UUID> worldItems = itemsByWorld.get(worldName);
                if (worldItems != null) {
                    for (UUID uuid : worldItems) {
                        if (inRangeItemUuids.contains(uuid)) continue;
                        TextDisplay label = activeLabels.get(uuid);
                        if (label != null && label.isValid()) plugin.updateEntityVisibility(p, label, false, visibleSet);
                        BlockDisplay beam = activeBeams.get(uuid);
                        if (beam != null && beam.isValid()) plugin.updateEntityVisibility(p, beam, false, visibleSet);
                        ItemDisplay visual = activeItemVisuals.get(uuid);
                        if (visual != null && visual.isValid()) plugin.updateEntityVisibility(p, visual, false, visibleSet);
                        Display shadow = activeShadows.get(uuid);
                        if (shadow != null && shadow.isValid()) plugin.updateEntityVisibility(p, shadow, false, visibleSet);
                    }
                }

                if (farmingEnabled) {
                    for (Map.Entry<org.bukkit.block.Block, ? extends List<BlockDisplay>> entry : activeCropSymbols.entrySet()) {
                        if (!entry.getKey().getWorld().getName().equals(worldName)) continue;

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
            plugin.setGloballyVisibleEntities(newGloballyVisible);
        }, 200L, (long) lodInterval);
    }
}
