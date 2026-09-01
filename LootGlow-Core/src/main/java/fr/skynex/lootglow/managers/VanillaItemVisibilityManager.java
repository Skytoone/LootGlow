package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.entity.Item;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages tracking and hiding of vanilla item entities.
 */
public class VanillaItemVisibilityManager {

    private final Set<Integer> hiddenVanillaItems = ConcurrentHashMap.newKeySet();

    public VanillaItemVisibilityManager() {
    }

    public VanillaItemVisibilityManager(LootGlow plugin) {
    }

    public Set<Integer> getHiddenVanillaItems() {
        return hiddenVanillaItems;
    }

    public boolean isVanillaItemHidden(Item item) {
        if (item == null) return false;
        return hiddenVanillaItems.contains(item.getEntityId());
    }

    public void hideVanillaItem(Item item) {
        if (item == null) return;
        hiddenVanillaItems.add(item.getEntityId());
    }

    public void showVanillaItem(Item item) {
        if (item == null) return;
        hiddenVanillaItems.remove(item.getEntityId());
    }

    public void clearAll() {
        hiddenVanillaItems.clear();
    }
}
