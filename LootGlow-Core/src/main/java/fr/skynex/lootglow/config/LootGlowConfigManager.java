package fr.skynex.lootglow.config;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.type.Light;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Centralized manager holding all plugin configuration values, category mappings, and parsing logic.
 */
public class LootGlowConfigManager {

    private final LootGlow plugin;

    // Core Settings
    private boolean isEnabled = true;
    private boolean onlyPlayerDrops = false;
    private boolean isWorldWhitelist = false;
    private final Set<String> filteredWorlds = new HashSet<>();
    private int despawnTime = 300;
    private boolean defaultGlow = true;

    // Grouping & Container
    private boolean groupingEnabled = true;
    private boolean useVisualBag = true;
    private Material bagMaterial = Material.PLAYER_HEAD;
    private String bagHeadTexture = "";
    private boolean useOwnerHead = false;
    private int bagCustomModelData = 0;
    private boolean containerEnabled = true;
    private String containerTitle = "<gradient:gold:white>[Contenu du Butin]</gradient>";
    private boolean containerRequireClick = true;

    // Economy
    private boolean economyEnabled = true;
    private String economyFormat = "<prefix><amount>";
    private String economyPrefix = "&a$&f";
    private NamedTextColor economyColor = NamedTextColor.GOLD;
    private Sound economySound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private final List<NamespacedKey> economyKeys = new ArrayList<>();

    // Visuals & Lighting
    private boolean shadowsEnabled = true;
    private float shadowScale = 0.4f;
    private boolean lightingEnabled = true;
    private final Light[] cachedLightBlockData = new Light[16];

    // Farming
    private boolean farmingEnabled = true;
    private NamedTextColor farmingGlowColor = NamedTextColor.GREEN;
    private Material farmingMaterial = Material.EMERALD_BLOCK;
    private float farmingScale = 0.2f;
    private double farmingOffset = 1.5;
    private boolean farmingAnimation = true;
    private double farmingViewDistance = 24.0;
    private final Set<Material> farmingCrops = new HashSet<>();

    // RPG Drops
    private boolean rpgDropsEnabled = true;
    private List<String> rpgEnabledCategories = new ArrayList<>();
    private float rpgRotation = (float) Math.toRadians(90.0);
    private float rpgItemScale = 0.6f;
    private float rpgBlockScale = 0.8f;
    private final Set<Material> rpgForceFlatMaterials = new HashSet<>();
    private final Set<Material> rpgForceUprightMaterials = new HashSet<>();

    // Holograms
    private boolean holoEnabled = true;
    private double holoOffset = 0.7;
    private boolean holoSeeThrough = false;
    private boolean holoBackground = false;
    private float holoViewDistance = 15.0f;
    private boolean holoShowAmount = true;
    private boolean holoShowTimer = true;
    private boolean holoTimerNewLine = true;
    private boolean holoHideUncategorized = false;

    // WorldGuard
    private boolean wgEnabled = true;
    private List<String> wgBlockedRegions = new ArrayList<>();

    // Performance & LOD
    private boolean lodEnabled = true;
    private double lodHoloDistSq = 576.0;
    private double lodBeamDistSq = 2304.0;
    private double lodPartDistSq = 1024.0;
    private int lodInterval = 20;

    // Loot Protection
    private boolean protectionEnabled = true;
    private int protectionDuration = 10;
    private boolean hardLockEnabled = true;
    private String bypassPermission = "lootglow.bypass.lock";

    // Beams
    private boolean beamsEnabled = true;
    private float beamHeight = 10.0f;
    private float beamWidth = 0.05f;
    private List<String> beamCategories = new ArrayList<>();
    private boolean beamsAnimate = true;
    private boolean beamsUseCategoryColor = true;

    // Particles
    private boolean particlesEnabled = true;
    private int particlesFrequency = 10;
    private String particleAnimType = "STILL";
    private double particleSize = 1.0;

    // Animations
    private boolean spawnAnimEnabled = true;
    private double jumpForce = 0.25;
    private int burstAmount = 15;

    // Magnet
    private boolean magnetEnabled = true;
    private boolean magnetEnableForGroups = false;
    private double magnetDistance = 5.0;
    private String magnetPermission = "lootglow.magnet";
    private List<String> magnetCategories = new ArrayList<>();

    // Interaction (RMB)
    private boolean rmbPickupEnabled = false;
    private boolean rmbPickupForce = false;
    private double rmbPickupRange = 3.0;
    private boolean rmbPickupEnableForGroups = false;

    // Bouncing
    private boolean bouncingEnabled = true;
    private int maxBounces = 3;
    private double bounceDamping = 0.6;
    private boolean bouncingOnlyOnSnow = false;
    private final Set<Material> bouncingBlockedBlocks = new HashSet<>();

    // Aspiration
    private boolean aspirationEnabled = true;
    private double aspirationSpeed = 0.15;

    // Default Colors & Categories Maps
    private NamedTextColor defaultColor = NamedTextColor.WHITE;
    private Particle.DustOptions defaultDustOptions;

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

    public LootGlowConfigManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void loadAll(FileConfiguration config, MiniMessage miniMessage, Map<String, Component> displayNameOverridesCache) {
        this.isEnabled = config.getBoolean("settings.enabled", true);
        this.onlyPlayerDrops = config.getBoolean("settings.only-player-drops", false);
        
        loadWorldFiltering(config, filteredWorlds);
        this.despawnTime = config.getInt("settings.despawn-time", 300);
        this.defaultGlow = config.getBoolean("settings.default-glow", true);

        // Grouping & Container
        this.groupingEnabled = config.getBoolean("settings.grouping.enabled", true);
        this.useVisualBag = config.getBoolean("settings.grouping.visual.enabled", true);
        String bagMatStr = config.getString("settings.grouping.visual.material", "PLAYER_HEAD");
        this.bagMaterial = Material.matchMaterial(bagMatStr);
        if (this.bagMaterial == null) this.bagMaterial = Material.PLAYER_HEAD;
        this.bagHeadTexture = config.getString("settings.grouping.visual.head-texture", "");
        this.useOwnerHead = config.getBoolean("settings.grouping.visual.use-owner-head", false);
        this.bagCustomModelData = config.getInt("settings.grouping.visual.custom-model-data", 0);
        this.containerEnabled = config.getBoolean("settings.grouping.container.enabled", true);
        this.containerTitle = config.getString("settings.grouping.container.title", "<gradient:gold:white>[Contenu du Butin]</gradient>");
        this.containerRequireClick = config.getBoolean("settings.grouping.container.require-click", true);

        // Economy
        this.economyEnabled = config.getBoolean("settings.economy.enabled", true);
        this.economyFormat = config.getString("settings.economy.format", "<prefix><amount>");
        this.economyPrefix = config.getString("settings.economy.prefix", "&a$&f");
        this.economyColor = plugin.parseNamedColor(config.getString("settings.economy.color", "GOLD"));
        String ecoSoundStr = config.getString("settings.economy.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        this.economySound = plugin.parseSound(ecoSoundStr);
        if (this.economySound == null) this.economySound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;

        this.shadowsEnabled = config.getBoolean("settings.rpg-drops.shadows.enabled", true);
        this.shadowScale = (float) config.getDouble("settings.rpg-drops.shadows.scale", 0.4);
        this.lightingEnabled = config.getBoolean("settings.lighting.enabled", true);
        for (int i = 0; i <= 15; i++) {
            try {
                Light lightData = (Light) Material.LIGHT.createBlockData();
                lightData.setLevel(i);
                cachedLightBlockData[i] = lightData;
            } catch (Exception ignored) {}
        }
        loadEconomyKeys(config, economyKeys);

        // Farming
        this.farmingEnabled = config.getBoolean("settings.farming.enabled", true);
        this.farmingGlowColor = plugin.parseNamedColor(config.getString("settings.farming.glow-color", "GREEN"));
        String symbolMatStr = config.getString("settings.farming.symbol-material", "EMERALD_BLOCK");
        this.farmingMaterial = Material.matchMaterial(symbolMatStr);
        if (this.farmingMaterial == null || !this.farmingMaterial.isBlock()) {
            if (symbolMatStr != null && !symbolMatStr.isEmpty()) {
                plugin.getLogger().warning("[LootGlow] Farming symbol-material '" + symbolMatStr + "' is invalid or not a block! Falling back to EMERALD_BLOCK.");
            }
            this.farmingMaterial = Material.EMERALD_BLOCK;
        }
        this.farmingScale = (float) config.getDouble("settings.farming.symbol-scale", 0.2);
        this.farmingOffset = config.getDouble("settings.farming.height-offset", 1.5);
        this.farmingAnimation = config.getBoolean("settings.farming.animation", true);
        this.farmingViewDistance = config.getDouble("settings.farming.view-distance", 24.0);
        loadFarmingCrops(config, farmingCrops);

        // RPG Drops
        this.rpgDropsEnabled = config.getBoolean("settings.rpg-drops.enabled", true);
        this.rpgEnabledCategories = config.getStringList("settings.rpg-drops.enabled-categories");
        this.rpgRotation = (float) Math.toRadians(config.getDouble("settings.rpg-drops.rotation-angle", 90.0));
        this.rpgItemScale = (float) config.getDouble("settings.rpg-drops.item-scale", 0.6);
        this.rpgBlockScale = (float) config.getDouble("settings.rpg-drops.block-scale", 0.8);
        loadRpgSettings(config, rpgForceFlatMaterials, rpgForceUprightMaterials);

        if (config.contains("settings.rpg-drops.scale")) {
            float oldScale = (float) config.getDouble("settings.rpg-drops.scale");
            if (!config.contains("settings.rpg-drops.item-scale")) this.rpgItemScale = oldScale;
            if (!config.contains("settings.rpg-drops.block-scale")) this.rpgBlockScale = oldScale;
        }

        // Holograms
        this.holoEnabled = config.getBoolean("settings.holograms.enabled", true);
        this.holoOffset = config.getDouble("settings.holograms.height-offset", 0.7);
        this.holoSeeThrough = config.getBoolean("settings.holograms.see-through", false);
        this.holoBackground = config.getBoolean("settings.holograms.background", false);
        this.holoViewDistance = (float) config.getDouble("settings.holograms.view-distance", 15.0);
        this.holoShowAmount = config.getBoolean("settings.holograms.show-amount", true);
        this.holoShowTimer = config.getBoolean("settings.holograms.show-timer", true);
        this.holoTimerNewLine = config.getBoolean("settings.holograms.timer-on-new-line", true);
        this.holoHideUncategorized = config.getBoolean("settings.holograms.hide-uncategorized", false);

        // WorldGuard
        this.wgEnabled = config.getBoolean("settings.worldguard.enabled", true);
        this.wgBlockedRegions = config.getStringList("settings.worldguard.blocked-regions");

        // LOD
        this.lodEnabled = config.getBoolean("settings.performance.lod.enabled", true);
        this.lodHoloDistSq = Math.pow(config.getDouble("settings.performance.lod.hologram-distance", 24.0), 2);
        this.lodBeamDistSq = Math.pow(config.getDouble("settings.performance.lod.beam-distance", 48.0), 2);
        this.lodPartDistSq = Math.pow(config.getDouble("settings.performance.lod.particle-distance", 32.0), 2);
        this.lodInterval = config.getInt("settings.performance.update-interval", 20);

        // Protection
        this.protectionEnabled = config.getBoolean("settings.loot-protection.enabled", true);
        this.protectionDuration = config.getInt("settings.loot-protection.display-duration", 10);
        this.hardLockEnabled = config.getBoolean("settings.loot-protection.hard-lock", true);
        this.bypassPermission = config.getString("settings.loot-protection.bypass-permission", "lootglow.bypass.lock");

        // Beams
        this.beamsEnabled = config.getBoolean("settings.beams.enabled", true);
        this.beamHeight = (float) config.getDouble("settings.beams.height", 10.0);
        this.beamWidth = (float) config.getDouble("settings.beams.width", 0.05);
        this.beamCategories = config.getStringList("settings.beams.enabled-categories");
        this.beamsAnimate = config.getBoolean("settings.beams.animate", true);
        this.beamsUseCategoryColor = config.getBoolean("settings.beams.use-category-color", true);

        // Particles
        this.particlesEnabled = config.getBoolean("settings.particles.enabled", true);
        this.particlesFrequency = config.getInt("settings.particles.frequency", 10);
        this.particleAnimType = config.getString("settings.particles.animation-type", "STILL");
        this.particleSize = config.getDouble("settings.particles.size", 1.0);

        // Spawn Animation
        this.spawnAnimEnabled = config.getBoolean("settings.spawn-animation.enabled", true);
        this.jumpForce = config.getDouble("settings.spawn-animation.jump-force", 0.25);
        this.burstAmount = config.getInt("settings.spawn-animation.burst-amount", 15);

        // Magnet
        this.magnetEnabled = config.getBoolean("settings.magnet.enabled", true);
        this.magnetEnableForGroups = config.getBoolean("settings.magnet.enable-for-groups", false);
        this.magnetDistance = config.getDouble("settings.magnet.distance", 5.0);
        this.magnetPermission = config.getString("settings.magnet.permission", "lootglow.magnet");
        this.magnetCategories = config.getStringList("settings.magnet.categories-enabled");

        // RMB Pickup
        this.rmbPickupEnabled = config.getBoolean("settings.interaction.rmb-pickup.enabled", false);
        this.rmbPickupForce = config.getBoolean("settings.interaction.rmb-pickup.force", false);
        this.rmbPickupRange = config.getDouble("settings.interaction.rmb-pickup.range", 3.0);
        this.rmbPickupEnableForGroups = config.getBoolean("settings.interaction.rmb-pickup.enable-for-groups", false);

        // Bouncing
        this.bouncingEnabled = config.getBoolean("settings.spawn-animation.bouncing.enabled", true);
        this.maxBounces = config.getInt("settings.spawn-animation.bouncing.max-bounces", 3);
        this.bounceDamping = config.getDouble("settings.spawn-animation.bouncing.damping", 0.6);
        this.bouncingOnlyOnSnow = config.getBoolean("settings.spawn-animation.bouncing.only-on-snow", false);
        loadBouncingSettings(config, bouncingBlockedBlocks);

        // Aspiration
        this.aspirationEnabled = config.getBoolean("settings.aspiration.enabled", true);
        this.aspirationSpeed = config.getDouble("settings.aspiration.speed", 0.15);

        String defColorStr = config.getString("default-color", "WHITE");
        this.defaultColor = plugin.parseNamedColor(defColorStr);
        this.defaultDustOptions = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(defaultColor.red(), defaultColor.green(), defaultColor.blue()),
                (float) particleSize);

        loadCategories(config, categoryColors, categoryDustOptions, particleSize, categoryLights, categoryGlow, displayNameOverridesCache, miniMessage, itemCategories, categoryNames, categoryParticles, categoryAnimTypes, particleAnimType, categorySounds);
    }

    public boolean isEnabled() { return isEnabled; }
    public boolean isOnlyPlayerDrops() { return onlyPlayerDrops; }
    public boolean isWorldWhitelist() { return isWorldWhitelist; }
    public Set<String> getFilteredWorlds() { return filteredWorlds; }
    public int getDespawnTime() { return despawnTime; }
    public boolean isDefaultGlow() { return defaultGlow; }

    public boolean isGroupingEnabled() { return groupingEnabled; }
    public boolean isUseVisualBag() { return useVisualBag; }
    public Material getBagMaterial() { return bagMaterial; }
    public String getBagHeadTexture() { return bagHeadTexture; }
    public boolean isUseOwnerHead() { return useOwnerHead; }
    public int getBagCustomModelData() { return bagCustomModelData; }
    public boolean isContainerEnabled() { return containerEnabled; }
    public String getContainerTitle() { return containerTitle; }
    public boolean isContainerRequireClick() { return containerRequireClick; }

    public boolean isEconomyEnabled() { return economyEnabled; }
    public String getEconomyFormat() { return economyFormat; }
    public String getEconomyPrefix() { return economyPrefix; }
    public NamedTextColor getEconomyColor() { return economyColor; }
    public Sound getEconomySound() { return economySound; }
    public List<NamespacedKey> getEconomyKeys() { return economyKeys; }

    public boolean isShadowsEnabled() { return shadowsEnabled; }
    public float getShadowScale() { return shadowScale; }
    public boolean isLightingEnabled() { return lightingEnabled; }
    public Light[] getCachedLightBlockData() { return cachedLightBlockData; }

    public boolean isFarmingEnabled() { return farmingEnabled; }
    public NamedTextColor getFarmingGlowColor() { return farmingGlowColor; }
    public Material getFarmingMaterial() { return farmingMaterial; }
    public float getFarmingScale() { return farmingScale; }
    public double getFarmingOffset() { return farmingOffset; }
    public boolean isFarmingAnimation() { return farmingAnimation; }
    public double getFarmingViewDistance() { return farmingViewDistance; }
    public Set<Material> getFarmingCrops() { return farmingCrops; }

    public boolean isRpgDropsEnabled() { return rpgDropsEnabled; }
    public List<String> getRpgEnabledCategories() { return rpgEnabledCategories; }
    public float getRpgRotation() { return rpgRotation; }
    public float getRpgItemScale() { return rpgItemScale; }
    public float getRpgBlockScale() { return rpgBlockScale; }
    public Set<Material> getRpgForceFlatMaterials() { return rpgForceFlatMaterials; }
    public Set<Material> getRpgForceUprightMaterials() { return rpgForceUprightMaterials; }

    public boolean isHoloEnabled() { return holoEnabled; }
    public double getHoloOffset() { return holoOffset; }
    public boolean isHoloSeeThrough() { return holoSeeThrough; }
    public boolean isHoloBackground() { return holoBackground; }
    public float getHoloViewDistance() { return holoViewDistance; }
    public boolean isHoloShowAmount() { return holoShowAmount; }
    public boolean isHoloShowTimer() { return holoShowTimer; }
    public boolean isHoloTimerNewLine() { return holoTimerNewLine; }
    public boolean isHoloHideUncategorized() { return holoHideUncategorized; }

    public boolean isWgEnabled() { return wgEnabled; }
    public List<String> getWgBlockedRegions() { return wgBlockedRegions; }

    public boolean isLodEnabled() { return lodEnabled; }
    public double getLodHoloDistSq() { return lodHoloDistSq; }
    public double getLodBeamDistSq() { return lodBeamDistSq; }
    public double getLodPartDistSq() { return lodPartDistSq; }
    public int getLodInterval() { return lodInterval; }

    public boolean isProtectionEnabled() { return protectionEnabled; }
    public int getProtectionDuration() { return protectionDuration; }
    public boolean isHardLockEnabled() { return hardLockEnabled; }
    public String getBypassPermission() { return bypassPermission; }

    public boolean isBeamsEnabled() { return beamsEnabled; }
    public float getBeamHeight() { return beamHeight; }
    public float getBeamWidth() { return beamWidth; }
    public List<String> getBeamCategories() { return beamCategories; }
    public boolean isBeamsAnimate() { return beamsAnimate; }
    public boolean isBeamsUseCategoryColor() { return beamsUseCategoryColor; }

    public boolean isParticlesEnabled() { return particlesEnabled; }
    public int getParticlesFrequency() { return particlesFrequency; }
    public String getParticleAnimType() { return particleAnimType; }
    public double getParticleSize() { return particleSize; }

    public boolean isSpawnAnimEnabled() { return spawnAnimEnabled; }
    public double getJumpForce() { return jumpForce; }
    public int getBurstAmount() { return burstAmount; }

    public boolean isMagnetEnabled() { return magnetEnabled; }
    public boolean isMagnetEnableForGroups() { return magnetEnableForGroups; }
    public double getMagnetDistance() { return magnetDistance; }
    public String getMagnetPermission() { return magnetPermission; }
    public List<String> getMagnetCategories() { return magnetCategories; }

    public boolean isRmbPickupEnabled() { return rmbPickupEnabled; }
    public boolean isRmbPickupForce() { return rmbPickupForce; }
    public double getRmbPickupRange() { return rmbPickupRange; }
    public boolean isRmbPickupEnableForGroups() { return rmbPickupEnableForGroups; }

    public boolean isBouncingEnabled() { return bouncingEnabled; }
    public int getMaxBounces() { return maxBounces; }
    public double getBounceDamping() { return bounceDamping; }
    public boolean isBouncingOnlyOnSnow() { return bouncingOnlyOnSnow; }
    public Set<Material> getBouncingBlockedBlocks() { return bouncingBlockedBlocks; }

    public boolean isAspirationEnabled() { return aspirationEnabled; }
    public double getAspirationSpeed() { return aspirationSpeed; }

    public NamedTextColor getDefaultColor() { return defaultColor; }
    public Particle.DustOptions getDefaultDustOptions() { return defaultDustOptions; }

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

    public boolean isWorldAllowed(String worldName) {
        if (isWorldWhitelist) {
            return filteredWorlds.contains(worldName);
        } else {
            return !filteredWorlds.contains(worldName);
        }
    }

    public void loadCategories(FileConfiguration config,
                                Map<String, NamedTextColor> categoryColors,
                                Map<String, Particle.DustOptions> categoryDustOptions,
                                double particleSize,
                                Map<String, Integer> categoryLights,
                                Map<String, Boolean> categoryGlow,
                                Map<String, Component> displayNameOverridesCache,
                                MiniMessage miniMessage,
                                Map<String, NamedTextColor> itemCategories,
                                Map<String, String> categoryNames,
                                Map<String, Particle> categoryParticles,
                                Map<String, String> categoryAnimTypes,
                                String particleAnimType,
                                Map<String, Sound> categorySounds) {
        categoryLorePatterns.clear();
        categoryNbtPatterns.clear();
        categoryTitles.clear();
        categorySubtitles.clear();
        categoryNotificationRadius.clear();

        if (config.getConfigurationSection("categories") != null) {
            for (String key : config.getConfigurationSection("categories").getKeys(false)) {
                String colorStr = config.getString("categories." + key + ".color", "WHITE");
                NamedTextColor color = plugin.parseNamedColor(colorStr);
                categoryColors.put(key, color);
                if (color != null) {
                    categoryDustOptions.put(key, new Particle.DustOptions(
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

    public void loadWorldFiltering(FileConfiguration config, Set<String> filteredWorlds) {
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

    public void loadRpgSettings(FileConfiguration config,
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

    public void loadBouncingSettings(FileConfiguration config, Set<Material> bouncingBlockedBlocks) {
        bouncingBlockedBlocks.clear();
        for (String blockName : config.getStringList("settings.spawn-animation.bouncing.blocked-blocks")) {
            Material m = Material.matchMaterial(blockName);
            if (m != null) {
                bouncingBlockedBlocks.add(m);
            }
        }
    }

    public void loadEconomyKeys(FileConfiguration config, List<NamespacedKey> economyKeys) {
        economyKeys.clear();
        for (String keyStr : config.getStringList("settings.economy.custom-keys")) {
            if (keyStr.contains(":")) {
                String[] parts = keyStr.split(":");
                economyKeys.add(new NamespacedKey(parts[0], parts[1]));
            }
        }
    }

    public void loadFarmingCrops(FileConfiguration config, Set<Material> farmingCrops) {
        farmingCrops.clear();
        List<String> cropsList = config.getStringList("settings.farming.crops");
        for (String crop : cropsList) {
            Material m = Material.matchMaterial(crop);
            if (m != null)
                farmingCrops.add(m);
        }
    }
}
