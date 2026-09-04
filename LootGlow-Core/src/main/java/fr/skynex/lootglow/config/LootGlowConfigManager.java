package fr.skynex.lootglow.config;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.config.modules.*;
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
 * Centralized manager holding plugin configuration modules, category mappings, and parsing logic.
 */
public class LootGlowConfigManager {

    private final LootGlow plugin;

    // Sub-config modules
    private final HologramConfig hologramConfig = new HologramConfig();
    private final ParticleConfig particleConfig = new ParticleConfig();
    private final FarmingConfig farmingConfig = new FarmingConfig();
    private final EconomyConfig economyConfig = new EconomyConfig();
    private final RpgConfig rpgConfig = new RpgConfig();
    private final ProtectionConfig protectionConfig = new ProtectionConfig();
    private final LodConfig lodConfig = new LodConfig();
    private final BeamConfig beamConfig = new BeamConfig();

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

    // Lighting
    private boolean lightingEnabled = true;
    private int lightColumnHeight = 3;
    private final Light[] cachedLightBlockData = new Light[16];

    // WorldGuard
    private boolean wgEnabled = true;
    private List<String> wgBlockedRegions = new ArrayList<>();

    // Spawn Animation & Magnet & Interaction & Bouncing & Aspiration
    private boolean spawnAnimEnabled = true;
    private double jumpForce = 0.25;
    private int burstAmount = 15;

    private boolean magnetEnabled = true;
    private boolean magnetEnableForGroups = false;
    private double magnetDistance = 5.0;
    private String magnetPermission = "lootglow.magnet";
    private List<String> magnetCategories = new ArrayList<>();

    private boolean rmbPickupEnabled = false;
    private boolean rmbPickupForce = false;
    private double rmbPickupRange = 3.0;
    private boolean rmbPickupEnableForGroups = false;

    private boolean bouncingEnabled = true;
    private int maxBounces = 3;
    private double bounceDamping = 0.6;
    private boolean bouncingOnlyOnSnow = false;
    private final Set<Material> bouncingBlockedBlocks = new HashSet<>();

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

    public HologramConfig getHologramConfig() { return hologramConfig; }
    public ParticleConfig getParticleConfig() { return particleConfig; }
    public FarmingConfig getFarmingConfig() { return farmingConfig; }
    public EconomyConfig getEconomyConfig() { return economyConfig; }
    public RpgConfig getRpgConfig() { return rpgConfig; }
    public ProtectionConfig getProtectionConfig() { return protectionConfig; }
    public LodConfig getLodConfig() { return lodConfig; }
    public BeamConfig getBeamConfig() { return beamConfig; }

    public void loadAll(FileConfiguration config, MiniMessage miniMessage, Map<String, Component> displayNameOverridesCache) {
        this.isEnabled = config.getBoolean("settings.enabled", true);
        this.onlyPlayerDrops = config.getBoolean("settings.only-player-drops", false);
        
        loadWorldFiltering(config, filteredWorlds);
        this.despawnTime = config.getInt("settings.despawn-time", 300);
        this.defaultGlow = config.getBoolean("settings.default-glow", true);

        // Sub-configs
        this.hologramConfig.load(config);
        this.particleConfig.load(config);
        this.farmingConfig.load(config, plugin);
        this.economyConfig.load(config, plugin);
        this.rpgConfig.load(config);
        this.protectionConfig.load(config);
        this.lodConfig.load(config);
        this.beamConfig.load(config);

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

        this.lightingEnabled = config.getBoolean("settings.lighting.enabled", true);
        this.lightColumnHeight = config.getInt("settings.lighting.column-height", 3);
        for (int i = 0; i <= 15; i++) {
            try {
                Light lightData = (Light) Material.LIGHT.createBlockData();
                lightData.setLevel(i);
                cachedLightBlockData[i] = lightData;
            } catch (Exception ignored) {}
        }

        // WorldGuard
        this.wgEnabled = config.getBoolean("settings.worldguard.enabled", true);
        this.wgBlockedRegions = config.getStringList("settings.worldguard.blocked-regions");

        // Spawn Animation
        this.spawnAnimEnabled = config.getBoolean("settings.spawn-animation.enabled", true);
        this.jumpForce = config.getDouble("settings.spawn-animation.jump-force", 0.25);
        this.burstAmount = config.getInt("settings.spawn-animation.burst-amount", 15);

        // Magnet
        this.magnetEnabled = config.getBoolean("settings.magnet.enabled", true);
        this.magnetEnableForGroups = config.getBoolean("settings.magnet.enable-for-groups", false);
        this.magnetDistance = config.getDouble("settings.magnet.distance", 5.0);
        this.magnetPermission = config.getString("settings.magnet.permission", "lootglow.magnet");
        List<String> rawMagCats = config.getStringList("settings.magnet.categories-enabled");
        this.magnetCategories = new ArrayList<>();
        if (rawMagCats != null) {
            for (String mc : rawMagCats) {
                if (mc != null && !mc.isBlank()) this.magnetCategories.add(mc.toLowerCase(java.util.Locale.ROOT));
            }
        }

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
                (float) particleConfig.getSize());

        loadCategories(config, categoryColors, categoryDustOptions, particleConfig.getSize(), categoryLights, categoryGlow, displayNameOverridesCache, miniMessage, itemCategories, categoryNames, categoryParticles, categoryAnimTypes, particleConfig.getAnimType(), categorySounds);
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

    public boolean isEconomyEnabled() { return economyConfig.isEnabled(); }
    public String getEconomyFormat() { return economyConfig.getFormat(); }
    public String getEconomyPrefix() { return economyConfig.getPrefix(); }
    public NamedTextColor getEconomyColor() { return economyConfig.getColor(); }
    public Sound getEconomySound() { return economyConfig.getSound(); }
    public List<NamespacedKey> getEconomyKeys() { return economyConfig.getEconomyKeys(); }

    public boolean isShadowsEnabled() { return rpgConfig.isShadowsEnabled(); }
    public float getShadowScale() { return rpgConfig.getShadowScale(); }
    public boolean isLightingEnabled() { return lightingEnabled; }
    public Light[] getCachedLightBlockData() { return cachedLightBlockData; }

    public boolean isFarmingEnabled() { return farmingConfig.isEnabled(); }
    public NamedTextColor getFarmingGlowColor() { return farmingConfig.getGlowColor(); }
    public Material getFarmingMaterial() { return farmingConfig.getMaterial(); }
    public float getFarmingScale() { return farmingConfig.getScale(); }
    public double getFarmingOffset() { return farmingConfig.getOffset(); }
    public boolean isFarmingAnimation() { return farmingConfig.isAnimation(); }
    public double getFarmingViewDistance() { return farmingConfig.getViewDistance(); }
    public Set<Material> getFarmingCrops() { return farmingConfig.getCrops(); }

    public boolean isRpgDropsEnabled() { return rpgConfig.isEnabled(); }
    public List<String> getRpgEnabledCategories() { return rpgConfig.getEnabledCategories(); }
    public float getRpgRotation() { return rpgConfig.getRotation(); }
    public float getRpgItemScale() { return rpgConfig.getItemScale(); }
    public float getRpgBlockScale() { return rpgConfig.getBlockScale(); }
    public Set<Material> getRpgForceFlatMaterials() { return rpgConfig.getForceFlatMaterials(); }
    public Set<Material> getRpgForceUprightMaterials() { return rpgConfig.getForceUprightMaterials(); }

    public boolean isHoloEnabled() { return hologramConfig.isEnabled(); }
    public double getHoloOffset() { return hologramConfig.getOffset(); }
    public boolean isHoloSeeThrough() { return hologramConfig.isSeeThrough(); }
    public boolean isHoloBackground() { return hologramConfig.isBackground(); }
    public float getHoloViewDistance() { return hologramConfig.getViewDistance(); }
    public boolean isHoloShowAmount() { return hologramConfig.isShowAmount(); }
    public boolean isHoloShowTimer() { return hologramConfig.isShowTimer(); }
    public boolean isHoloTimerNewLine() { return hologramConfig.isTimerNewLine(); }
    public boolean isHoloHideUncategorized() { return hologramConfig.isHideUncategorized(); }

    public boolean isWgEnabled() { return wgEnabled; }
    public List<String> getWgBlockedRegions() { return wgBlockedRegions; }

    public boolean isLodEnabled() { return lodConfig.isEnabled(); }
    public double getLodHoloDistSq() { return lodConfig.getHoloDistSq(); }
    public double getLodBeamDistSq() { return lodConfig.getBeamDistSq(); }
    public double getLodPartDistSq() { return lodConfig.getPartDistSq(); }
    public int getLodInterval() { return lodConfig.getInterval(); }

    public boolean isProtectionEnabled() { return protectionConfig.isEnabled(); }
    public int getProtectionDuration() { return protectionConfig.getDuration(); }
    public boolean isHardLockEnabled() { return protectionConfig.isHardLockEnabled(); }
    public String getBypassPermission() { return protectionConfig.getBypassPermission(); }

    public boolean isBobbingEnabled() { return rpgConfig.isBobbingEnabled(); }
    public double getBobbingAmplitude() { return rpgConfig.getBobbingAmplitude(); }
    public double getBobbingSpeed() { return rpgConfig.getBobbingSpeed(); }
    public int getLightColumnHeight() { return lightColumnHeight; }

    public boolean isBeamsEnabled() { return beamConfig.isEnabled(); }
    public float getBeamHeight() { return beamConfig.getHeight(); }
    public float getBeamWidth() { return beamConfig.getWidth(); }
    public List<String> getBeamCategories() { return beamConfig.getCategories(); }
    public boolean isBeamsAnimate() { return beamConfig.isAnimate(); }
    public boolean isBeamsUseCategoryColor() { return beamConfig.isUseCategoryColor(); }

    public boolean isParticlesEnabled() { return particleConfig.isEnabled(); }
    public int getParticlesFrequency() { return particleConfig.getFrequency(); }
    public String getParticleAnimType() { return particleConfig.getAnimType(); }
    public double getParticleSize() { return particleConfig.getSize(); }

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
                            displayNameOverridesCache.put(itemKey.toUpperCase(), fr.skynex.lootglow.util.ColorUtil.parse(raw));
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

    public void loadBouncingSettings(FileConfiguration config, Set<Material> bouncingBlockedBlocks) {
        bouncingBlockedBlocks.clear();
        for (String blockName : config.getStringList("settings.spawn-animation.bouncing.blocked-blocks")) {
            Material m = Material.matchMaterial(blockName);
            if (m != null) {
                bouncingBlockedBlocks.add(m);
            }
        }
    }
}
