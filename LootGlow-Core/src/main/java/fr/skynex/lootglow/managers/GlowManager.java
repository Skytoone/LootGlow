package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Color;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Scoreboard team glow colors, player-specific glow color overrides, and glow refreshing.
 */
public class GlowManager {

    private final LootGlow plugin;
    private final Map<UUID, Color> customGlowColors = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Color>> playerSpecificGlowColors = new ConcurrentHashMap<>();

    public GlowManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, Color> getCustomGlowColors() {
        return customGlowColors;
    }

    public Map<UUID, Map<UUID, Color>> getPlayerSpecificGlowColors() {
        return playerSpecificGlowColors;
    }

    public void setGlowColor(Item item, Color color) {
        if (item == null || color == null) return;
        customGlowColors.put(item.getUniqueId(), color);
    }

    public void setGlowColor(Item item, Color color, Player player) {
        if (item == null || color == null || player == null) return;
        playerSpecificGlowColors.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(item.getUniqueId(), color);
    }

    public void resetGlowColor(Item item) {
        if (item == null) return;
        customGlowColors.remove(item.getUniqueId());
    }

    public void resetGlowColor(Item item, Player player) {
        if (item == null || player == null) return;
        Map<UUID, Color> pMap = playerSpecificGlowColors.get(player.getUniqueId());
        if (pMap != null) {
            pMap.remove(item.getUniqueId());
        }
    }

    public void clearAll() {
        customGlowColors.clear();
        playerSpecificGlowColors.clear();
    }
}
