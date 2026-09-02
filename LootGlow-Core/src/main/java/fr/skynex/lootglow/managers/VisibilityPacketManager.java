package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player entity visibility tracking and packet cleanups.
 */
public class VisibilityPacketManager {

    private final LootGlow plugin;
    private final Map<UUID, Set<UUID>> visibleEntitiesPerPlayer = new ConcurrentHashMap<>();

    public VisibilityPacketManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, Set<UUID>> getVisibleEntitiesPerPlayer() {
        return plugin.getVisibleEntities();
    }

    public void removePlayer(UUID playerUuid) {
        if (playerUuid != null) {
            plugin.getVisibleEntities().remove(playerUuid);
        }
    }

    public void cleanVisibleSet(UUID entityUuid) {
        if (entityUuid == null) return;
        for (Set<UUID> visibleSet : plugin.getVisibleEntities().values()) {
            if (visibleSet != null) {
                visibleSet.remove(entityUuid);
            }
        }
    }

    public fr.skynex.lootglow.packets.PacketProvider setupPacketProvider() {
        fr.skynex.lootglow.packets.PacketProvider provider = null;
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PacketEvents")) {
            provider = new fr.skynex.lootglow.packets.PacketEventsProvider();
            plugin.getLogger().info("Using PacketEvents for packet handling.");
        } else if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            provider = new fr.skynex.lootglow.packets.ProtocolLibProvider();
            plugin.getLogger().info("Using ProtocolLib for packet handling.");
        }

        if (provider != null) {
            provider.register(plugin);
        } else {
            plugin.getLogger().warning(
                    "Neither ProtocolLib nor PacketEvents found! Per-player glow toggle and RPG item hiding will not work.");
        }
        return provider;
    }

    public void clearAll() {
        visibleEntitiesPerPlayer.clear();
    }
}
