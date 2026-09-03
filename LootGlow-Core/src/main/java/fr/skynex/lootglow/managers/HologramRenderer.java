package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages holographic TextDisplay formatting, placeholder replacements, and rendering.
 */
public class HologramRenderer {

    private final LootGlow plugin;
    private final Map<UUID, Component> customHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Component>> playerCustomHolograms = new ConcurrentHashMap<>();

    public HologramRenderer(LootGlow plugin) {
        this.plugin = plugin;
    }

    public LootGlow getPlugin() {
        return plugin;
    }

    public Map<UUID, Component> getCustomHolograms() {
        return customHolograms;
    }

    public Map<UUID, Map<UUID, Component>> getPlayerCustomHolograms() {
        return playerCustomHolograms;
    }

    public void setCustomHologram(Item item, String text, MiniMessage miniMessage) {
        if (item == null || text == null || !item.isValid()) return;
        customHolograms.put(item.getUniqueId(), fr.skynex.lootglow.util.ColorUtil.parse(text));
    }

    public void setCustomHologram(Item item, String text, Player player) {
        if (item == null || text == null || player == null || !item.isValid()) return;
        playerCustomHolograms.computeIfAbsent(item.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(player.getUniqueId(), fr.skynex.lootglow.util.ColorUtil.parse(text));
    }

    public void removeCustomHologram(Item item) {
        if (item == null) return;
        UUID uuid = item.getUniqueId();
        customHolograms.remove(uuid);
        playerCustomHolograms.remove(uuid);
    }

    public void removeCustomHologram(Item item, Player player) {
        if (item == null || player == null) return;
        Map<UUID, Component> map = playerCustomHolograms.get(item.getUniqueId());
        if (map != null) {
            map.remove(player.getUniqueId());
            if (map.isEmpty()) {
                playerCustomHolograms.remove(item.getUniqueId());
            }
        }
    }

    public Component getCustomHologram(Item item, Player player) {
        if (item == null) return null;
        UUID itemUuid = item.getUniqueId();
        if (player != null) {
            Map<UUID, Component> map = playerCustomHolograms.get(itemUuid);
            if (map != null && map.containsKey(player.getUniqueId())) {
                return map.get(player.getUniqueId());
            }
        }
        return customHolograms.get(itemUuid);
    }

    public void clearAll() {
        customHolograms.clear();
        playerCustomHolograms.clear();
    }
}
