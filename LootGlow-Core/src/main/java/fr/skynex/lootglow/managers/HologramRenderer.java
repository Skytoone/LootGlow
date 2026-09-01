package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages holographic TextDisplay formatting, placeholder replacements, and rendering.
 */
public class HologramRenderer {

    private final LootGlow plugin;
    private final Map<UUID, Component> customHolograms = new ConcurrentHashMap<>();

    public HologramRenderer(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, Component> getCustomHolograms() {
        return customHolograms;
    }

    public void setCustomHologram(Item item, String text, MiniMessage miniMessage) {
        if (item == null || text == null || !item.isValid()) return;
        customHolograms.put(item.getUniqueId(), miniMessage.deserialize(text));
    }

    public void clearAll() {
        customHolograms.clear();
    }
}
