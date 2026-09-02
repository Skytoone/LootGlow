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
        org.bukkit.World pWorld = player.getWorld();
        org.bukkit.World iWorld = item.getWorld();
        if (!pWorld.equals(iWorld)) return false;

        double px = player.getX();
        double py = player.getEyeHeight() + player.getY();
        double pz = player.getZ();

        double ix = item.getX();
        double iy = item.getY() + 0.25;
        double iz = item.getZ();

        double dx = ix - px;
        double dy = iy - py;
        double dz = iz - pz;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq > maxDistance * maxDistance) return false;

        double distance = Math.sqrt(distSq);
        if (distance < 0.1) return true;

        Vector direction = new Vector(dx / distance, dy / distance, dz / distance);
        Location eyeLoc = new Location(pWorld, px, py, pz);
        org.bukkit.util.RayTraceResult result = pWorld.rayTraceBlocks(eyeLoc, direction, distance,
                org.bukkit.FluidCollisionMode.NEVER, true);

        return result == null || result.getHitBlock() == null;
    }

    public boolean updateOcclusionVisibility(Player player, Item item, double maxDistance) {
        return hasLineOfSight(player, item, maxDistance);
    }
}
