package fr.skynex.lootglow.managers;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Manages line-of-sight raycasting and occlusion visibility calculations for items and displays.
 */
public class OcclusionManager {

    public OcclusionManager() {
    }

    public boolean hasLineOfSight(Player player, Item item, double maxDistance) {
        if (player == null || item == null || !player.isOnline() || !item.isValid()) return false;
        Location eyeLoc = player.getEyeLocation();
        Location itemLoc = item.getLocation().add(0, 0.25, 0);

        if (!eyeLoc.getWorld().equals(itemLoc.getWorld())) return false;
        double distSq = eyeLoc.distanceSquared(itemLoc);
        if (distSq > maxDistance * maxDistance) return false;

        Vector direction = itemLoc.toVector().subtract(eyeLoc.toVector());
        double distance = Math.sqrt(distSq);
        if (distance < 0.1) return true;

        direction.normalize();
        org.bukkit.World world = eyeLoc.getWorld();
        org.bukkit.util.RayTraceResult result = world.rayTraceBlocks(eyeLoc, direction, distance,
                org.bukkit.FluidCollisionMode.NEVER, true);

        return result == null || result.getHitBlock() == null;
    }

    public boolean updateOcclusionVisibility(Player player, Item item, double maxDistance) {
        return hasLineOfSight(player, item, maxDistance);
    }
}
