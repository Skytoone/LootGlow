package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages economy item drop metadata and money amounts.
 */
public class EconomyDropManager {

    private final LootGlow plugin;
    private final List<NamespacedKey> economyKeys = new ArrayList<>();
    private final Map<UUID, Double> itemMoneyAmounts = new ConcurrentHashMap<>();

    public EconomyDropManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public List<NamespacedKey> getEconomyKeys() {
        return economyKeys;
    }

    public Map<UUID, Double> getItemMoneyAmounts() {
        return itemMoneyAmounts;
    }

    public void clearAll() {
        economyKeys.clear();
        itemMoneyAmounts.clear();
    }
}
