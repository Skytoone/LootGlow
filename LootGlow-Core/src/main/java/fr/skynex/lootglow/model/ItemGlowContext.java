package fr.skynex.lootglow.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsulates resolution and rendering configuration for item glow application.
 * Eliminates long-parameter methods and ensures a clean, extensible architectural context.
 */
public record ItemGlowContext(
        boolean isEnabled,
        boolean economyEnabled,
        List<NamespacedKey> economyKeys,
        NamedTextColor economyColor,
        Sound economySound,
        Map<UUID, Double> itemMoneyAmounts,
        Map<String, NamedTextColor> itemCategories,
        Map<String, String> categoryNames,
        NamedTextColor defaultColor,
        Map<String, Particle> categoryParticles,
        Map<UUID, Particle> itemParticlesCache,
        Map<UUID, String> itemCategoriesCache,
        int despawnTime,
        Map<Integer, UUID> entityIdMap,
        Map<UUID, Item> activeItems,
        Map<String, Set<UUID>> itemsByWorld,
        boolean rpgDropsEnabled,
        List<String> rpgEnabledCategories,
        Map<String, Boolean> categoryGlow,
        boolean defaultGlow,
        Set<Integer> hiddenVanillaItems,
        Map<String, Sound> categorySounds,
        boolean holoEnabled,
        boolean holoHideUncategorized,
        Map<UUID, Long> itemSpawnTimes,
        Map<UUID, Component> baseNameCache,
        boolean protectionEnabled,
        int protectionDuration,
        boolean shadowsEnabled,
        boolean beamsEnabled,
        List<String> beamCategories
) {}
