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
        plugin.getTrackedItemManager().untrackItem(itemUuid);
        plugin.getVisualDisplayManager().removeVisual(itemUuid);
        plugin.getHologramManager().removeHologram(itemUuid);
        plugin.getBeamManager().removeBeam(itemUuid);
        plugin.getRpgDropManager().removeShadow(itemUuid);
        plugin.getLootProtectionManager().removeProtection(itemUuid);
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

        if (plugin.getBeamManager() != null) plugin.getBeamManager().getActiveBeamConfigs().remove(uuid);
        if (plugin.getTrackedItemManager() != null) {
            plugin.getTrackedItemManager().getTrackedItems().remove(uuid);
        }
        if (plugin.getSurfaceAlignmentManager() != null) {
            plugin.getSurfaceAlignmentManager().getWaterLogCache().remove(uuid);
            plugin.getSurfaceAlignmentManager().getSurfaceStates().remove(uuid);
        }

        Item item = plugin.getActiveItems().remove(uuid);
        if (item != null) {
            int entityId = item.getEntityId();
            plugin.getEntityIdMap().remove(entityId);
            plugin.getHiddenVanillaItems().remove(entityId);
        }
    }
}
