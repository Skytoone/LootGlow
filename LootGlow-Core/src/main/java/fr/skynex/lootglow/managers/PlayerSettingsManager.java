package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages in-memory player toggles and settings.
 */
public class PlayerSettingsManager {

    private final LootGlow plugin;
    private final Map<UUID, Boolean> disabledPlayers = new ConcurrentHashMap<>();

    public PlayerSettingsManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, Boolean> getDisabledPlayers() {
        return disabledPlayers;
    }

    public boolean isLootGlowDisabled(Player player) {
        if (player == null) return false;
        return disabledPlayers.getOrDefault(player.getUniqueId(), false);
    }

    public void setLootGlowDisabled(Player player, boolean disabled) {
        if (player == null) return;
        if (disabled) {
            disabledPlayers.put(player.getUniqueId(), true);
        } else {
            disabledPlayers.remove(player.getUniqueId());
        }
    }

    public void clearAll() {
        disabledPlayers.clear();
    }
}
