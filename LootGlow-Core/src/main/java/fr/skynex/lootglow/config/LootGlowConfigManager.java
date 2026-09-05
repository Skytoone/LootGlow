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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final CategoryConfig categoryConfig = new CategoryConfig();

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

    // Default Colors
    private NamedTextColor defaultColor = NamedTextColor.WHITE;
    private Particle.DustOptions defaultDustOptions;

    public LootGlowConfigManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    private final ConfigParser configParser = new ConfigParser();
    public ConfigParser getConfigParser() { return configParser; }
    public boolean isUsePapi() { return plugin.isUsePapi(); }

    public void loadConfiguration() {
        plugin.reloadConfig();
        loadAll(plugin.getConfig(), MiniMessage.miniMessage(), plugin.getStateRepository().getDisplayNameOverridesCache());
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
        this.defaultColor = configParser.parseNamedColor(defColorStr);
        if (this.defaultColor == null) this.defaultColor = NamedTextColor.WHITE;
        this.defaultDustOptions = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(defaultColor.red(), defaultColor.green(), defaultColor.blue()),
                (float) particleConfig.getSize());

        categoryConfig.load(config, configParser, particleConfig.getSize(), particleConfig.getAnimType(), displayNameOverridesCache);
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

    public CategoryConfig getCategoryConfig() { return categoryConfig; }

    public Map<String, NamedTextColor> getCategoryColors() { return categoryConfig.getCategoryColors(); }
    public Map<String, Particle> getCategoryParticles() { return categoryConfig.getCategoryParticles(); }
    public Map<String, Sound> getCategorySounds() { return categoryConfig.getCategorySounds(); }
    public Map<String, String> getCategoryNames() { return categoryConfig.getCategoryNames(); }
    public Map<String, Boolean> getCategoryGlow() { return categoryConfig.getCategoryGlow(); }
    public Map<String, List<String>> getCategoryLorePatterns() { return categoryConfig.getCategoryLorePatterns(); }
    public Map<String, List<String>> getCategoryNbtPatterns() { return categoryConfig.getCategoryNbtPatterns(); }
    public Map<String, String> getCategoryTitles() { return categoryConfig.getCategoryTitles(); }
    public Map<String, String> getCategorySubtitles() { return categoryConfig.getCategorySubtitles(); }
    public Map<String, Double> getCategoryNotificationRadius() { return categoryConfig.getCategoryNotificationRadius(); }
    public Map<String, NamedTextColor> getItemCategories() { return categoryConfig.getItemCategories(); }
    public Map<String, Integer> getCategoryLights() { return categoryConfig.getCategoryLights(); }
    public Map<String, Particle.DustOptions> getCategoryDustOptions() { return categoryConfig.getCategoryDustOptions(); }
    public Map<String, String> getCategoryAnimTypes() { return categoryConfig.getCategoryAnimTypes(); }

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
        categoryConfig.load(config, configParser, particleSize, particleAnimType, displayNameOverridesCache);
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
