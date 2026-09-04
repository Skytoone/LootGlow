package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Location;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Item;
import org.bukkit.FluidCollisionMode;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages item surface raycasting, water detection, and display alignment on blocks.
 */
public class SurfaceAlignmentManager {

    public static class SurfaceState {
        public final double y;
        public final Float yaw;
        public final Float pitch;
        public final double lastItemX;
        public final double lastItemY;
        public final double lastItemZ;

        public SurfaceState(double y, Float yaw, Float pitch, double lastItemX, double lastItemY, double lastItemZ) {
            this.y = y;
            this.yaw = yaw;
            this.pitch = pitch;
            this.lastItemX = lastItemX;
            this.lastItemY = lastItemY;
            this.lastItemZ = lastItemZ;
        }
    }

    private final LootGlow plugin;
    private final Map<UUID, SurfaceState> surfaceStates = new ConcurrentHashMap<>();
    private final Set<UUID> waterLogCache = ConcurrentHashMap.newKeySet();

    public SurfaceAlignmentManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public LootGlow getPlugin() {
        return plugin;
    }

    public Map<UUID, SurfaceState> getSurfaceStates() {
        return surfaceStates;
    }

    public Set<UUID> getWaterLogCache() {
        return waterLogCache;
    }

    public void updateSurfaceAlignment(Item item, Set<UUID> recentlyBounced) {
        if (item == null || item.isDead()) return;
        UUID uuid = item.getUniqueId();
        if (!item.isOnGround() || item.isInWater() || (recentlyBounced != null && recentlyBounced.contains(uuid))) {
            surfaceStates.remove(uuid);
            return;
        }
        if (surfaceStates.containsKey(uuid)) return;

        Location loc = item.getLocation();
        RayTraceResult result = loc.getWorld().rayTraceBlocks(
                loc.clone().add(0, 0.4, 0),
                new Vector(0, -1, 0),
                0.8,
                FluidCollisionMode.NEVER,
                false);

        double targetY = loc.getY();
        Float forcedYaw = null;
        Float forcedPitch = null;

        if (result != null && result.getHitPosition() != null) {
            targetY = result.getHitPosition().getY();
            if (result.getHitBlock() != null) {
                BlockData data = result.getHitBlock().getBlockData();
                if (data instanceof Stairs stairs) {
                    if (stairs.getHalf() == Bisected.Half.BOTTOM) {
                        switch (stairs.getFacing()) {
                            case NORTH: forcedYaw = 180f; break;
                            case SOUTH: forcedYaw = 0f; break;
                            case WEST: forcedYaw = 90f; break;
                            case EAST: forcedYaw = 270f; break;
                            default: break;
                        }
                        forcedPitch = -30f;
                    }
                }
            }
        }
        surfaceStates.put(uuid, new SurfaceState(targetY, forcedYaw, forcedPitch, loc.getX(), loc.getY(), loc.getZ()));
    }

    public void clearAll() {
        surfaceStates.clear();
        waterLogCache.clear();
    }
}
