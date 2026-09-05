package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles graceful plugin shutdown, entity despawning, and map cleanup.
 */
public class PluginDisableService {

    private final LootGlow plugin;

    public PluginDisableService(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void onDisable(Map<UUID, TextDisplay> activeLabels,
                          Map<UUID, BlockDisplay> activeBeams,
                          Map<UUID, ItemDisplay> activeItemVisuals,
                          Map<UUID, Display> activeShadows,
                          Map<?, ? extends List<BlockDisplay>> activeCropSymbols,
                          Set<Integer> hiddenVanillaItems,
                          Map<Integer, UUID> entityIdMap,
                          Map<UUID, ?> trackedItems,
                          Map<UUID, Location> activeLights,
                          Map<UUID, Item> activeItems,
                          Map<String, Set<UUID>> itemsByWorld,
                          Map<Integer, ?> timerComponentCache,
                          Map<UUID, Integer> bounceCounts,
                          Set<UUID> recentlyBounced,
                          Map<UUID, Location> lastFarmingScanLocations) {

        // Remove active visual display entities
        activeLabels.values().forEach(d -> { if (d != null && d.isValid()) d.remove(); });
        activeBeams.values().forEach(d -> { if (d != null && d.isValid()) d.remove(); });
        activeItemVisuals.values().forEach(d -> { if (d != null && d.isValid()) d.remove(); });
        activeShadows.values().forEach(d -> { if (d != null && d.isValid()) d.remove(); });
        activeCropSymbols.values().forEach(list -> list.forEach(d -> { if (d != null && d.isValid()) d.remove(); }));

        // Restore vanilla item visibility for online players before shutdown
        for (Integer entityId : hiddenVanillaItems) {
            UUID uuid = entityIdMap.get(entityId);
            if (uuid == null) continue;
            Entity ent = Bukkit.getEntity(uuid);
            if (ent instanceof Item item && item.isValid()) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(item.getWorld())) {
                        p.showEntity(plugin, item);
                    }
                }
            }
        }

        // Clear all tracking maps and manager states
        var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
        if (trackedMgr != null) {
            trackedMgr.clearAll();
        }
        var groupMgr = plugin.getService(fr.skynex.lootglow.managers.GroupContainerManager.class);
        if (groupMgr != null) {
            groupMgr.clearAll();
        }
        var beamMgr = plugin.getService(fr.skynex.lootglow.managers.BeamManager.class);
        if (beamMgr != null) {
            beamMgr.clearAll();
        }
        var animMgr = plugin.getService(fr.skynex.lootglow.managers.ParticleAnimationManager.class);
        if (animMgr != null) {
            animMgr.getCustomParticles().clear();
        }
        var holoRenderer = plugin.getService(fr.skynex.lootglow.managers.HologramRenderer.class);
        if (holoRenderer != null) {
            holoRenderer.getCustomHolograms().clear();
        }
        var magnetMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMagnetManager.class);
        if (magnetMgr != null) {
            magnetMgr.clearAll();
        }
        var teamMgr = plugin.getService(fr.skynex.lootglow.managers.GlowTeamManager.class);
        if (teamMgr != null) {
            teamMgr.clearScoreboardTeams();
        }
        var tickMgr = plugin.getService(fr.skynex.lootglow.managers.PluginTickManager.class);
        if (tickMgr != null) {
            tickMgr.cancelTasks();
        }

        plugin.getStateRepository().getVisibleEntities().clear();
        plugin.getStateRepository().getHiddenVisuals().clear();
        plugin.getStateRepository().getDisabledMagnets().clear();

        trackedItems.clear();
        activeLabels.clear();
        activeBeams.clear();
        activeItemVisuals.clear();
        activeCropSymbols.clear();
        activeLights.clear();
        entityIdMap.clear();
        hiddenVanillaItems.clear();
        activeItems.clear();
        itemsByWorld.clear();
        timerComponentCache.clear();
        bounceCounts.clear();
        recentlyBounced.clear();
        var alignMgr = plugin.getService(fr.skynex.lootglow.managers.SurfaceAlignmentManager.class);
        if (alignMgr != null) {
            alignMgr.clearAll();
        }
        lastFarmingScanLocations.clear();
    }
}
