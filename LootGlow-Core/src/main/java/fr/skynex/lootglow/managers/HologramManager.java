package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.managers.TrackedItemManager.TrackedItem;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Item;
import org.bukkit.entity.TextDisplay;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages TextDisplay hologram creation, text updates, and entity tracking.
 */
public class HologramManager {

    private final LootGlow plugin;
    private final Map<UUID, Component> lastHoloState = new ConcurrentHashMap<>();

    public HologramManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, Component> getLastHoloState() {
        return lastHoloState;
    }

    public void removeHologram(Item item) {
        if (item == null) return;
        removeHologram(item.getUniqueId());
    }

    public void removeHologram(UUID uuid) {
        if (uuid == null) return;
        TrackedItem ti = plugin.getTrackedItemManager().getTrackedItems().get(uuid);
        if (ti != null && ti.label != null && ti.label.isValid()) {
            ti.label.remove();
        }
        lastHoloState.remove(uuid);
    }

    public void clearAll() {
        lastHoloState.clear();
    }
}
