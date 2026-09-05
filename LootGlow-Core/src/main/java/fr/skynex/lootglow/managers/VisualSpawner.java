package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.entity.Item;

import java.util.UUID;

/**
 * Manages spatial spawning and despawning operations for item visual displays.
 */
public class VisualSpawner {

    private final LootGlow plugin;

    public VisualSpawner(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void removeGlow(UUID itemUuid) {
        if (itemUuid == null) return;
        var trackedMgr = plugin.getService(TrackedItemManager.class);
        if (trackedMgr != null) trackedMgr.untrackItem(itemUuid);
        var visDispMgr = plugin.getService(VisualDisplayManager.class);
        if (visDispMgr != null) visDispMgr.removeVisual(itemUuid);
        var holoMgr = plugin.getService(HologramManager.class);
        if (holoMgr != null) holoMgr.removeHologram(itemUuid);
        var beamMgr = plugin.getService(BeamManager.class);
        if (beamMgr != null) beamMgr.removeBeam(itemUuid);
        var rpgMgr = plugin.getService(RPGDropManager.class);
        if (rpgMgr != null) rpgMgr.removeShadow(itemUuid);
        var protMgr = plugin.getService(LootProtectionManager.class);
        if (protMgr != null) protMgr.removeProtection(itemUuid);
    }

    public void removeGlowKeepDisplays(UUID uuid) {
        if (uuid == null) return;

        try {
            org.bukkit.scoreboard.Scoreboard scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
            String itemEntry = uuid.toString();
            org.bukkit.scoreboard.Team itemTeam = scoreboard.getEntryTeam(itemEntry);
            if (itemTeam != null && itemTeam.getName().startsWith("LG_"))
                itemTeam.removeEntry(itemEntry);
        } catch (Exception ignored) {}

        var beamMgr = plugin.getService(BeamManager.class);
        if (beamMgr != null) beamMgr.getActiveBeamConfigs().remove(uuid);
        var trackedMgr = plugin.getService(TrackedItemManager.class);
        if (trackedMgr != null) {
            trackedMgr.getTrackedItems().remove(uuid);
        }
        var surfMgr = plugin.getService(SurfaceAlignmentManager.class);
        if (surfMgr != null) {
            surfMgr.getWaterLogCache().remove(uuid);
            surfMgr.getSurfaceStates().remove(uuid);
        }

        Item item = plugin.getStateRepository().getActiveItems().remove(uuid);
        if (item != null) {
            int entityId = item.getEntityId();
            plugin.getStateRepository().getEntityIdMap().remove(entityId);
            plugin.getStateRepository().getHiddenVanillaItems().remove(entityId);
        }
    }
}
