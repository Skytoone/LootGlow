package fr.skynex.lootglow.config;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.*;

/**
 * Manages configuration loading, parsing, category colors, particle definitions, and sounds.
 */
public class LootGlowConfigManager {

    private final LootGlow plugin;

    private boolean isEnabled = true;
    private boolean onlyPlayerDrops = false;
    private boolean isWorldWhitelist = false;
    private final Set<String> filteredWorlds = new HashSet<>();
    private int despawnTime = 300;
    private boolean defaultGlow = true;

    private final Map<String, NamedTextColor> categoryColors = new HashMap<>();
    private final Map<String, Particle> categoryParticles = new HashMap<>();
    private final Map<String, Sound> categorySounds = new HashMap<>();
    private final Map<String, String> categoryNames = new HashMap<>();
    private final Map<String, Boolean> categoryGlow = new HashMap<>();

    public LootGlowConfigManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isOnlyPlayerDrops() {
        return onlyPlayerDrops;
    }

    public boolean isWorldWhitelist() {
        return isWorldWhitelist;
    }

    public Set<String> getFilteredWorlds() {
        return filteredWorlds;
    }

    public int getDespawnTime() {
        return despawnTime;
    }

    public boolean isDefaultGlow() {
        return defaultGlow;
    }

    public Map<String, NamedTextColor> getCategoryColors() {
        return categoryColors;
    }

    public Map<String, Particle> getCategoryParticles() {
        return categoryParticles;
    }

    public Map<String, Sound> getCategorySounds() {
        return categorySounds;
    }

    public Map<String, String> getCategoryNames() {
        return categoryNames;
    }

    public Map<String, Boolean> getCategoryGlow() {
        return categoryGlow;
    }

    public boolean isWorldAllowed(String worldName) {
        if (isWorldWhitelist) {
            return filteredWorlds.contains(worldName);
        } else {
            return !filteredWorlds.contains(worldName);
        }
    }

    public void loadCategories(org.bukkit.configuration.file.FileConfiguration config,
                               Map<String, NamedTextColor> categoryColors,
                               Map<String, org.bukkit.Particle.DustOptions> categoryDustOptions,
                               double particleSize,
                               Map<String, Integer> categoryLights,
                               Map<String, Boolean> categoryGlow,
                               Map<String, net.kyori.adventure.text.Component> displayNameOverridesCache,
                               net.kyori.adventure.text.minimessage.MiniMessage miniMessage,
                               Map<String, NamedTextColor> itemCategories,
                               Map<String, String> categoryNames,
                               Map<String, Particle> categoryParticles,
                               Map<String, String> categoryAnimTypes,
                               String particleAnimType,
                               Map<String, Sound> categorySounds) {
        if (config.getConfigurationSection("categories") != null) {
            for (String key : config.getConfigurationSection("categories").getKeys(false)) {
                String colorStr = config.getString("categories." + key + ".color", "WHITE");
                NamedTextColor color = plugin.parseNamedColor(colorStr);
                categoryColors.put(key, color);
                if (color != null) {
                    categoryDustOptions.put(key, new org.bukkit.Particle.DustOptions(
                            org.bukkit.Color.fromRGB(color.red(), color.green(), color.blue()), (float) particleSize));
                }

                String partStr = config.getString("categories." + key + ".particle");
                Particle particle = null;
                if (partStr != null) {
                    try {
                        org.bukkit.NamespacedKey particleKey = org.bukkit.NamespacedKey.minecraft(partStr.toLowerCase());
                        particle = org.bukkit.Registry.PARTICLE_TYPE.get(particleKey);
                    } catch (Exception ignored) {}
                }

                String soundStr = config.getString("categories." + key + ".sound");
                Sound sound = null;
                if (soundStr != null) {
                    sound = plugin.parseSound(soundStr);
                }

                int lightLevel = config.getInt("categories." + key + ".light-level", 0);
                categoryLights.put(key, lightLevel);

                boolean glowEnabled = config.getBoolean("categories." + key + ".glow", true);
                categoryGlow.put(key, glowEnabled);

                if (config.getConfigurationSection("categories." + key + ".display-names") != null) {
                    for (String itemKey : config.getConfigurationSection("categories." + key + ".display-names").getKeys(false)) {
                        String raw = config.getString("categories." + key + ".display-names." + itemKey);
                        if (raw != null)
                            displayNameOverridesCache.put(itemKey.toUpperCase(), miniMessage.deserialize(raw));
                    }
                }

                for (String material : config.getStringList("categories." + key + ".items")) {
                    String mat = material.toUpperCase();
                    itemCategories.put(mat, color);
                    categoryNames.put(mat, key);
                    if (particle != null)
                        categoryParticles.put(mat, particle);

                    String catAnim = config.getString("categories." + key + ".particle-animation", particleAnimType);
                    categoryAnimTypes.put(key, catAnim);

                    if (sound != null)
                        categorySounds.put(mat, sound);
                }
            }
        }
    }

    public void loadWorldFiltering(org.bukkit.configuration.file.FileConfiguration config, Set<String> filteredWorlds) {
        filteredWorlds.clear();
        if (config.contains("settings.worlds.mode")) {
            String mode = config.getString("settings.worlds.mode", "BLACKLIST").toUpperCase();
            this.isWorldWhitelist = mode.equals("WHITELIST");
            filteredWorlds.addAll(config.getStringList("settings.worlds.list"));
        } else {
            this.isWorldWhitelist = false;
            filteredWorlds.addAll(config.getStringList("settings.disabled-worlds"));
        }
    }

    public void loadRpgSettings(org.bukkit.configuration.file.FileConfiguration config,
                               Set<Material> rpgForceFlatMaterials,
                               Set<Material> rpgForceUprightMaterials) {
        rpgForceFlatMaterials.clear();
        List<String> flatMats = config.getStringList("settings.rpg-drops.force-flat-materials");
        if (flatMats != null) {
            for (String matStr : flatMats) {
                if (matStr == null || matStr.trim().isEmpty()) continue;
                String upper = matStr.trim().toUpperCase();
                Material m = Material.matchMaterial(upper);
                if (m != null) {
                    rpgForceFlatMaterials.add(m);
                }
                for (Material mat : Material.values()) {
                    if (mat.name().equals(upper) || (upper.contains("CANDLE") && mat.name().endsWith("_CANDLE"))) {
                        rpgForceFlatMaterials.add(mat);
                    }
                }
            }
        }

        rpgForceUprightMaterials.clear();
        List<String> uprightMats = config.getStringList("settings.rpg-drops.force-upright-materials");
        if (uprightMats != null) {
            for (String matStr : uprightMats) {
                if (matStr == null || matStr.trim().isEmpty()) continue;
                String upper = matStr.trim().toUpperCase();
                Material m = Material.matchMaterial(upper);
                if (m != null) {
                    rpgForceUprightMaterials.add(m);
                }
                for (Material mat : Material.values()) {
                    if (mat.name().equals(upper)) {
                        rpgForceUprightMaterials.add(mat);
                    }
                }
            }
        }
    }

    public void loadBouncingSettings(org.bukkit.configuration.file.FileConfiguration config, Set<Material> bouncingBlockedBlocks) {
        bouncingBlockedBlocks.clear();
        for (String blockName : config.getStringList("settings.spawn-animation.bouncing.blocked-blocks")) {
            Material m = Material.matchMaterial(blockName);
            if (m != null) {
                bouncingBlockedBlocks.add(m);
            }
        }
    }

    public void loadEconomyKeys(org.bukkit.configuration.file.FileConfiguration config, List<org.bukkit.NamespacedKey> economyKeys) {
        economyKeys.clear();
        for (String keyStr : config.getStringList("settings.economy.custom-keys")) {
            if (keyStr.contains(":")) {
                String[] parts = keyStr.split(":");
                economyKeys.add(new org.bukkit.NamespacedKey(parts[0], parts[1]));
            }
        }
    }

    public void loadFarmingCrops(org.bukkit.configuration.file.FileConfiguration config, Set<Material> farmingCrops) {
        farmingCrops.clear();
        List<String> cropsList = config.getStringList("settings.farming.crops");
        for (String crop : cropsList) {
            Material m = Material.matchMaterial(crop);
            if (m != null)
                farmingCrops.add(m);
        }
    }
}
