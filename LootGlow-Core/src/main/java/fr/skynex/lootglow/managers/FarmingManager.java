package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages farming crop highlight markers and symbols.
 */
public class FarmingManager {

    public static final class CropSymbol extends ArrayList<BlockDisplay> {
        public final Location location;
        public CropSymbol(Location location) {
            this.location = location;
        }
    }

    private final LootGlow plugin;
    private final Map<Block, CropSymbol> activeCropSymbols = new HashMap<>();
    private final org.joml.Quaternionf reusableRot = new org.joml.Quaternionf();

    public FarmingManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<Block, CropSymbol> getActiveCropSymbols() {
        return activeCropSymbols;
    }

    public boolean isCropHighlighted(Block cropBlock) {
        return cropBlock != null && activeCropSymbols.containsKey(cropBlock);
    }

    public boolean isFarmingAllowed(Location loc) {
        if (!plugin.isUseWorldGuard() || !plugin.isWgEnabled()) return true;
        if (plugin.isInBlockedRegion(loc)) return false;
        return fr.skynex.lootglow.integration.WorldGuardHook.isFarmingAllowed(loc);
    }

    public void spawnCropSymbol(Block block) {
        if (!plugin.isFarmingEnabled() || activeCropSymbols.containsKey(block)) return;
        if (!isFarmingAllowed(block.getLocation())) return;

        Material farmingMat = plugin.getFarmingMaterial();
        if (farmingMat == null || !farmingMat.isBlock()) {
            farmingMat = Material.EMERALD_BLOCK;
        }

        Location loc = block.getLocation().add(0.5, plugin.getFarmingOffset(), 0.5);
        CropSymbol cs = new CropSymbol(loc.clone());

        float scale = plugin.getFarmingScale();
        final Material finalMat = farmingMat;

        BlockDisplay bar = block.getWorld().spawn(loc, BlockDisplay.class, bd -> {
            bd.setBlock(finalMat.createBlockData());
            bd.setPersistent(false);
            Transformation t = bd.getTransformation();
            t.getScale().set(scale, scale * 3.0f, scale);
            t.getTranslation().set(-scale / 2.0f, 0, -scale / 2.0f);
            bd.setTransformation(t);
        });

        BlockDisplay dot = block.getWorld().spawn(loc.clone().add(0, scale * 3.5, 0), BlockDisplay.class, bd -> {
            bd.setBlock(finalMat.createBlockData());
            bd.setPersistent(false);
            Transformation t = bd.getTransformation();
            t.getScale().set(scale * 1.2f, scale * 1.2f, scale * 1.2f);
            t.getTranslation().set(-scale * 0.6f, 0, -scale * 0.6f);
            bd.setTransformation(t);
        });

        cs.add(bar);
        cs.add(dot);
        activeCropSymbols.put(block, cs);
    }

    public void removeCropSymbol(Block block) {
        CropSymbol cs = activeCropSymbols.remove(block);
        if (cs != null) {
            cs.forEach(bd -> {
                if (bd.isValid()) bd.remove();
            });
        }
    }

    public void tickFarmingAnimation(float angle, boolean farmingEnabled, boolean farmingAnimation, Set<UUID> globallyVisibleEntities) {
        if (!farmingEnabled || !farmingAnimation) return;

        reusableRot.rotationY(angle);

        for (List<BlockDisplay> parts : activeCropSymbols.values()) {
            if (parts.size() < 2) continue;
            BlockDisplay bar = parts.get(0);
            BlockDisplay dot = parts.get(1);
            if (!bar.isValid()) continue;
            if (!globallyVisibleEntities.contains(bar.getUniqueId())) continue;

            FoliaScheduler.runAtEntity(plugin, bar, () -> {
                if (!bar.isValid()) return;
                Transformation bT = bar.getTransformation();
                bT.getLeftRotation().set(reusableRot);
                bar.setTransformation(bT);
                bar.setInterpolationDuration(2);
                bar.setInterpolationDelay(0);

                if (dot != null && dot.isValid()) {
                    Transformation dT = dot.getTransformation();
                    dT.getLeftRotation().set(reusableRot);
                    dot.setTransformation(dT);
                    dot.setInterpolationDuration(2);
                    dot.setInterpolationDelay(0);
                }
            });
        }
    }

    private org.bukkit.scheduler.BukkitTask farmingTask;

    public void startFarmingTask(boolean isEnabled, boolean farmingEnabled, Set<Material> farmingCrops, double farmingViewDistance, Map<UUID, Location> lastFarmingScanLocations) {
        if (farmingTask != null) {
            farmingTask.cancel();
            farmingTask = null;
        }

        farmingTask = FoliaScheduler.runTimer(plugin, () -> {
            if (!isEnabled || !farmingEnabled)
                return;

            // Active validation & cleanup of spawned farming symbols (Zero-Allocation Iterator)
            if (!activeCropSymbols.isEmpty()) {
                java.util.Iterator<Map.Entry<Block, CropSymbol>> it = activeCropSymbols.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Block, CropSymbol> entry = it.next();
                    Block b = entry.getKey();
                    boolean valid = b.getType() != Material.AIR && farmingCrops.contains(b.getType());
                    if (valid) {
                        if (b.getBlockData() instanceof org.bukkit.block.data.Ageable age) {
                            if (age.getAge() != age.getMaximumAge()) {
                                valid = false;
                            }
                        } else {
                            valid = false;
                        }
                    }
                    if (valid && !isFarmingAllowed(b.getLocation())) {
                        valid = false;
                    }
                    if (!valid) {
                        entry.getValue().forEach(e -> e.remove());
                        it.remove();
                    }
                }
            }

            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (!plugin.isWorldAllowed(p.getWorld().getName()))
                    continue;

                Location loc = p.getLocation();
                Location lastLoc = lastFarmingScanLocations.get(p.getUniqueId());
                if (lastLoc != null && lastLoc.getWorld().equals(loc.getWorld()) && lastLoc.distanceSquared(loc) < 64.0) {
                    continue;
                }
                lastFarmingScanLocations.put(p.getUniqueId(), loc.clone());

                Block center = loc.getBlock();
                int r = (int) Math.min(16.0, Math.ceil(farmingViewDistance));
                for (int x = -r; x <= r; x += 2) {
                    for (int z = -r; z <= r; z += 2) {
                        for (int y = -2; y <= 2; y++) {
                            Block b = center.getRelative(x, y, z);
                            if (farmingCrops.contains(b.getType())) {
                                if (b.getBlockData() instanceof org.bukkit.block.data.Ageable age) {
                                    if (age.getAge() == age.getMaximumAge()) {
                                        spawnCropSymbol(b);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }, 100L, 100L);
    }

    public void setCropHighlight(Block cropBlock, boolean highlight) {
        if (cropBlock == null) return;
        if (highlight) {
            spawnCropSymbol(cropBlock);
        } else {
            removeCropSymbol(cropBlock);
        }
    }

    public void clearAll() {
        activeCropSymbols.values().forEach(cs -> cs.forEach(bd -> {
            if (bd.isValid()) bd.remove();
        }));
        activeCropSymbols.clear();
    }
}
