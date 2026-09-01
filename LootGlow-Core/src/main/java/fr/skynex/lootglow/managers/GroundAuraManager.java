package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages rotating ground magic aura particle circles beneath high-rarity items.
 */
public class GroundAuraManager {

    private final LootGlow plugin;
    private double currentAngle = 0.0;

    public GroundAuraManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void tickAuras(Map<UUID, Item> activeItems, Map<UUID, String> itemCategoriesCache) {
        if (!plugin.getConfig().getBoolean("settings.wow-effects.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("settings.wow-effects.ground-auras.enabled", true)) return;

        List<String> enabledCategories = plugin.getConfig().getStringList("settings.wow-effects.ground-auras.categories");
        if (enabledCategories.isEmpty()) {
            enabledCategories = List.of("mythic", "legendary", "epic");
        }

        double radius = plugin.getConfig().getDouble("settings.wow-effects.ground-auras.radius", 0.7);
        int particleCount = plugin.getConfig().getInt("settings.wow-effects.ground-auras.particle-count", 12);

        currentAngle = (currentAngle + 0.15) % (2 * Math.PI);

        for (Map.Entry<UUID, Item> entry : activeItems.entrySet()) {
            Item item = entry.getValue();
            if (item == null || !item.isValid() || item.isDead()) continue;

            String cat = itemCategoriesCache.get(item.getUniqueId());
            if (cat == null) continue;

            final String lowerCat = cat.toLowerCase();
            boolean isMatch = enabledCategories.stream().anyMatch(c -> c.equalsIgnoreCase(lowerCat));
            if (!isMatch) continue;

            Location loc = item.getLocation();
            NamedTextColor color = plugin.getConfigManager().getCategoryColors().get(cat);
            Color dustColor = color != null ? Color.fromRGB(color.red(), color.green(), color.blue()) : Color.WHITE;
            Particle.DustOptions dustOptions = new Particle.DustOptions(dustColor, 0.9f);

            for (int i = 0; i < particleCount; i++) {
                double angle = currentAngle + (2 * Math.PI * i / particleCount);
                double x = loc.getX() + radius * Math.cos(angle);
                double z = loc.getZ() + radius * Math.sin(angle);
                double y = loc.getY() + 0.05; // Slightly above ground level

                Location pLoc = new Location(loc.getWorld(), x, y, z);
                loc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, dustOptions);
            }
        }
    }
}
