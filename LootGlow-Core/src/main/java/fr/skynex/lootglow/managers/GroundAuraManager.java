package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
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

    private boolean enabled;
    private boolean aurasEnabled;
    private java.util.Set<String> enabledCategories = java.util.Collections.emptySet();
    private double radius;
    private int particleCount;

    public GroundAuraManager(LootGlow plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        this.enabled = plugin.getConfig().getBoolean("settings.wow-effects.enabled", true);
        this.aurasEnabled = plugin.getConfig().getBoolean("settings.wow-effects.ground-auras.enabled", true);

        List<String> list = plugin.getConfig().getStringList("settings.wow-effects.ground-auras.categories");
        if (list == null || list.isEmpty()) {
            list = List.of("mythic", "legendary", "epic");
        }
        this.enabledCategories = list.stream().map(s -> s.toLowerCase(java.util.Locale.ROOT)).collect(java.util.stream.Collectors.toSet());

        this.radius = plugin.getConfig().getDouble("settings.wow-effects.ground-auras.radius", 0.7);
        this.particleCount = plugin.getConfig().getInt("settings.wow-effects.ground-auras.particle-count", 12);
    }

    public void tickAuras(Map<UUID, Item> activeItems, Map<UUID, String> itemCategoriesCache) {
        if (!enabled || !aurasEnabled || activeItems.isEmpty()) return;

        java.util.Collection<? extends org.bukkit.entity.Player> onlinePlayers = org.bukkit.Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) return;

        currentAngle = (currentAngle + 0.15) % (2 * Math.PI);
        double maxDistSq = plugin.getConfigManager() != null ? plugin.getConfigManager().getLodPartDistSq() : 1024.0;

        for (Map.Entry<UUID, Item> entry : activeItems.entrySet()) {
            Item item = entry.getValue();
            if (item == null || !item.isValid() || item.isDead()) continue;

            String cat = itemCategoriesCache.get(entry.getKey());
            if (cat == null || !enabledCategories.contains(cat.toLowerCase())) continue;

            double baseX = item.getX();
            double baseY = item.getY() + 0.05;
            double baseZ = item.getZ();
            org.bukkit.World world = item.getWorld();

            // Collect nearby players in LOD range
            java.util.List<org.bukkit.entity.Player> nearbyPlayers = null;
            for (org.bukkit.entity.Player p : onlinePlayers) {
                if (plugin.getHiddenVisuals().contains(p.getUniqueId()) || !p.getWorld().equals(world)) continue;
                double dx = p.getX() - baseX;
                double dy = p.getY() - baseY;
                double dz = p.getZ() - baseZ;
                if ((dx * dx + dy * dy + dz * dz) <= maxDistSq) {
                    if (nearbyPlayers == null) nearbyPlayers = new java.util.ArrayList<>();
                    nearbyPlayers.add(p);
                }
            }

            if (nearbyPlayers == null || nearbyPlayers.isEmpty()) continue;

            Particle.DustOptions dustOptions = plugin.getCategoryDustOptions().get(cat);
            if (dustOptions == null) {
                NamedTextColor color = plugin.getConfigManager().getCategoryColors().get(cat);
                Color dustColor = color != null ? Color.fromRGB(color.red(), color.green(), color.blue()) : Color.WHITE;
                dustOptions = new Particle.DustOptions(dustColor, 0.9f);
            }

            for (int i = 0; i < particleCount; i++) {
                double angle = currentAngle + (2 * Math.PI * i / particleCount);
                double px = baseX + radius * Math.cos(angle);
                double pz = baseZ + radius * Math.sin(angle);

                for (org.bukkit.entity.Player p : nearbyPlayers) {
                    p.spawnParticle(Particle.DUST, px, baseY, pz, 1, 0, 0, 0, 0, dustOptions);
                }
            }
        }
    }
}
