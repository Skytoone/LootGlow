package fr.skynex.lootglow.config.modules;

import fr.skynex.lootglow.config.ConfigParser;
import fr.skynex.lootglow.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryConfig {

    private final Map<String, NamedTextColor> categoryColors = new HashMap<>();
    private final Map<String, Particle> categoryParticles = new HashMap<>();
    private final Map<String, Sound> categorySounds = new HashMap<>();
    private final Map<String, String> categoryNames = new HashMap<>();
    private final Map<String, Boolean> categoryGlow = new HashMap<>();
    private final Map<String, List<String>> categoryLorePatterns = new HashMap<>();
    private final Map<String, List<String>> categoryNbtPatterns = new HashMap<>();
    private final Map<String, String> categoryTitles = new HashMap<>();
    private final Map<String, String> categorySubtitles = new HashMap<>();
    private final Map<String, Double> categoryNotificationRadius = new HashMap<>();
    private final Map<String, NamedTextColor> itemCategories = new HashMap<>();
    private final Map<String, Integer> categoryLights = new HashMap<>();
    private final Map<String, Particle.DustOptions> categoryDustOptions = new HashMap<>();
    private final Map<String, String> categoryAnimTypes = new HashMap<>();

    public void load(FileConfiguration config,
                     ConfigParser configParser,
                     double particleSize,
                     String particleAnimType,
                     Map<String, Component> displayNameOverridesCache) {
        categoryColors.clear();
        categoryParticles.clear();
        categorySounds.clear();
        categoryNames.clear();
        categoryGlow.clear();
        categoryLorePatterns.clear();
        categoryNbtPatterns.clear();
        categoryTitles.clear();
        categorySubtitles.clear();
        categoryNotificationRadius.clear();
        itemCategories.clear();
        categoryLights.clear();
        categoryDustOptions.clear();
        categoryAnimTypes.clear();

        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) {
            return;
        }

        for (String key : categoriesSection.getKeys(false)) {
            String colorStr = config.getString("categories." + key + ".color", "WHITE");
            NamedTextColor color = configParser.parseNamedColor(colorStr);
            categoryColors.put(key, color);
            if (color != null) {
                categoryDustOptions.put(key, new Particle.DustOptions(
                        Color.fromRGB(color.red(), color.green(), color.blue()), (float) particleSize));
            }

            String partStr = config.getString("categories." + key + ".particle");
            Particle particle = configParser.parseParticle(partStr);
            if (particle != null) {
                categoryParticles.put(key, particle);
                categoryParticles.put(key.toLowerCase(), particle);
            }

            String soundStr = config.getString("categories." + key + ".sound");
            Sound sound = null;
            if (soundStr != null) {
                sound = configParser.parseSound(soundStr);
            }

            int lightLevel = config.getInt("categories." + key + ".light-level", 0);
            categoryLights.put(key, lightLevel);

            boolean glowEnabled = config.getBoolean("categories." + key + ".glow", true);
            categoryGlow.put(key, glowEnabled);

            List<String> lorePats = config.getStringList("categories." + key + ".lore-patterns");
            if (!lorePats.isEmpty()) {
                List<String> lowerLorePats = new ArrayList<>();
                for (String lp : lorePats) lowerLorePats.add(lp.toLowerCase());
                categoryLorePatterns.put(key, lowerLorePats);
            }

            List<String> nbtPats = config.getStringList("categories." + key + ".nbt-patterns");
            if (!nbtPats.isEmpty()) {
                List<String> lowerNbtPats = new ArrayList<>();
                for (String np : nbtPats) lowerNbtPats.add(np.toLowerCase());
                categoryNbtPatterns.put(key, lowerNbtPats);
            }

            if (config.contains("categories." + key + ".title")) {
                categoryTitles.put(key, config.getString("categories." + key + ".title"));
            }
            if (config.contains("categories." + key + ".subtitle")) {
                categorySubtitles.put(key, config.getString("categories." + key + ".subtitle"));
            }
            categoryNotificationRadius.put(key, config.getDouble("categories." + key + ".notification-radius", 15.0));

            ConfigurationSection displayNamesSection = config.getConfigurationSection("categories." + key + ".display-names");
            if (displayNamesSection != null) {
                for (String itemKey : displayNamesSection.getKeys(false)) {
                    String raw = config.getString("categories." + key + ".display-names." + itemKey);
                    if (raw != null) {
                        displayNameOverridesCache.put(itemKey.toUpperCase(), ColorUtil.parse(raw));
                    }
                }
            }

            for (String material : config.getStringList("categories." + key + ".items")) {
                String mat = material.toUpperCase();
                itemCategories.put(mat, color);
                categoryNames.put(mat, key);
                if (particle != null) {
                    categoryParticles.put(mat, particle);
                }

                String catAnim = config.getString("categories." + key + ".particle-animation", particleAnimType);
                categoryAnimTypes.put(key, catAnim);

                if (sound != null) {
                    categorySounds.put(mat, sound);
                }
            }
        }
    }

    public Map<String, NamedTextColor> getCategoryColors() { return categoryColors; }
    public Map<String, Particle> getCategoryParticles() { return categoryParticles; }
    public Map<String, Sound> getCategorySounds() { return categorySounds; }
    public Map<String, String> getCategoryNames() { return categoryNames; }
    public Map<String, Boolean> getCategoryGlow() { return categoryGlow; }
    public Map<String, List<String>> getCategoryLorePatterns() { return categoryLorePatterns; }
    public Map<String, List<String>> getCategoryNbtPatterns() { return categoryNbtPatterns; }
    public Map<String, String> getCategoryTitles() { return categoryTitles; }
    public Map<String, String> getCategorySubtitles() { return categorySubtitles; }
    public Map<String, Double> getCategoryNotificationRadius() { return categoryNotificationRadius; }
    public Map<String, NamedTextColor> getItemCategories() { return itemCategories; }
    public Map<String, Integer> getCategoryLights() { return categoryLights; }
    public Map<String, Particle.DustOptions> getCategoryDustOptions() { return categoryDustOptions; }
    public Map<String, String> getCategoryAnimTypes() { return categoryAnimTypes; }
}
