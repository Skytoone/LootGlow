package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.managers.TrackedItemManager.TrackedItem;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;

import java.util.Map;
import java.util.UUID;

/**
 * Manages periodic hologram distance checks and text updates.
 */
public class HologramTickService {

    private final LootGlow plugin;

    public HologramTickService(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void tickHolograms(int numPlayers, World[] pWorlds, double[] px, double[] py, double[] pz, double holoDistSq) {
        Map<UUID, TrackedItem> trackedItems = plugin.getTrackedItemManager().getTrackedItems();
        if (trackedItems.isEmpty()) return;

        for (Map.Entry<UUID, TrackedItem> entry : trackedItems.entrySet()) {
            UUID uuid = entry.getKey();
            TrackedItem ti = entry.getValue();
            TextDisplay display = ti.label;
            if (display == null || !display.isValid()) continue;

            boolean hasPlayerNearby = false;
            final double lx = display.getX();
            final double ly = display.getY();
            final double lz = display.getZ();
            final World lWorld = display.getWorld();

            for (int i = 0; i < numPlayers; i++) {
                if (pWorlds[i].equals(lWorld)) {
                    double pdx = px[i] - lx;
                    double pdy = py[i] - ly;
                    double pdz = pz[i] - lz;
                    if ((pdx * pdx + pdy * pdy + pdz * pdz) < holoDistSq) {
                        hasPlayerNearby = true;
                        break;
                    }
                }
            }

            boolean isGroupLeader = plugin.getGroupContainerManager().getGroupLeaders().contains(uuid);
            boolean isGrouped = plugin.getGroupContainerManager().getGroupedItems().contains(uuid);

            if (!hasPlayerNearby && !isGroupLeader && !isGrouped) {
                continue;
            }
        }
    }
}
