package fr.skynex.lootglow;

import java.io.File;
import java.nio.charset.StandardCharsets;
import fr.skynex.lootglow.listeners.ItemListener;
import fr.skynex.lootglow.util.FoliaScheduler;
import fr.skynex.lootglow.database.DatabaseManager;
import fr.skynex.lootglow.managers.TrackedItemManager;
import fr.skynex.lootglow.managers.BeamManager;
import fr.skynex.lootglow.managers.HologramManager;
import fr.skynex.lootglow.config.LootGlowConfigManager;
import fr.skynex.lootglow.commands.LootGlowCommandManager;
import me.clip.placeholderapi.PlaceholderAPI;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;
import java.util.List;

public class LootGlow extends JavaPlugin implements fr.skynex.lootglow.api.LootGlowAPI {

    private DatabaseManager databaseManager;
    private TrackedItemManager trackedItemManager;
    private BeamManager beamManager;
    private HologramManager hologramManager;
    private LootGlowConfigManager configManager;
    private LootGlowCommandManager commandManager;
    private fr.skynex.lootglow.managers.FarmingManager farmingManager;
    private fr.skynex.lootglow.managers.RPGDropManager rpgDropManager;
    private fr.skynex.lootglow.managers.ParticleAnimationManager particleAnimationManager;
    private fr.skynex.lootglow.managers.GroupContainerManager groupContainerManager;
    private fr.skynex.lootglow.managers.LootProtectionManager lootProtectionManager;
    private fr.skynex.lootglow.managers.OcclusionManager occlusionManager;
    private fr.skynex.lootglow.managers.GlowManager glowManager;
    private fr.skynex.lootglow.managers.ItemMagnetManager itemMagnetManager;
    private fr.skynex.lootglow.managers.EconomyDropManager economyDropManager;
    private fr.skynex.lootglow.managers.HologramRenderer hologramRenderer;
    private fr.skynex.lootglow.managers.SurfaceAlignmentManager surfaceAlignmentManager;
    private fr.skynex.lootglow.managers.GlowTeamManager glowTeamManager;
    private fr.skynex.lootglow.managers.VisualDisplayManager visualDisplayManager;
    private fr.skynex.lootglow.managers.PluginTickManager pluginTickManager;
    private fr.skynex.lootglow.managers.VisualSpawner visualSpawner;
    private fr.skynex.lootglow.config.ConfigParser configParser;
    private fr.skynex.lootglow.integration.IntegrationManager integrationManager;
    private fr.skynex.lootglow.managers.PlayerSettingsManager playerSettingsManager;
    private fr.skynex.lootglow.managers.VisibilityPacketManager visibilityPacketManager;
    private fr.skynex.lootglow.managers.LODManager lodManager;
    private fr.skynex.lootglow.util.ItemNameFormatter itemNameFormatter;
    private fr.skynex.lootglow.managers.LootWorldManager lootWorldManager;
    private fr.skynex.lootglow.managers.VanillaItemVisibilityManager vanillaItemVisibilityManager;
    private fr.skynex.lootglow.service.HologramTickService hologramTickService;
    private fr.skynex.lootglow.service.BeamTickService beamTickService;
    private fr.skynex.lootglow.service.ItemRotationService itemRotationService;
    private fr.skynex.lootglow.service.EntityVisibilityService entityVisibilityService;
    private fr.skynex.lootglow.service.ItemVisualSpawnService itemVisualSpawnService;
    private fr.skynex.lootglow.service.ItemGroupingService itemGroupingService;
    private fr.skynex.lootglow.service.HologramService hologramService;
    private fr.skynex.lootglow.service.PluginDisableService pluginDisableService;
    private fr.skynex.lootglow.service.MessageService messageService;
    private fr.skynex.lootglow.service.LightService lightService;
    private fr.skynex.lootglow.service.ItemGlowApplyService itemGlowApplyService;
    private fr.skynex.lootglow.service.ItemPhysicsService itemPhysicsService;

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public TrackedItemManager getTrackedItemManager() { return trackedItemManager; }
    public BeamManager getBeamManager() { return beamManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public LootGlowConfigManager getConfigManager() { return configManager; }
    public LootGlowCommandManager getCommandManager() { return commandManager; }
    public fr.skynex.lootglow.managers.FarmingManager getFarmingManager() { return farmingManager; }
    public fr.skynex.lootglow.managers.RPGDropManager getRpgDropManager() { return rpgDropManager; }
    public fr.skynex.lootglow.managers.ParticleAnimationManager getParticleAnimationManager() { return particleAnimationManager; }
    public fr.skynex.lootglow.managers.GroupContainerManager getGroupContainerManager() { return groupContainerManager; }
    public fr.skynex.lootglow.managers.LootProtectionManager getLootProtectionManager() { return lootProtectionManager; }
    public fr.skynex.lootglow.managers.OcclusionManager getOcclusionManager() { return occlusionManager; }
    public fr.skynex.lootglow.managers.GlowManager getGlowManager() { return glowManager; }
    public fr.skynex.lootglow.managers.ItemMagnetManager getItemMagnetManager() { return itemMagnetManager; }
    public fr.skynex.lootglow.managers.EconomyDropManager getEconomyDropManager() { return economyDropManager; }
    public fr.skynex.lootglow.managers.HologramRenderer getHologramRenderer() { return hologramRenderer; }
    public fr.skynex.lootglow.managers.SurfaceAlignmentManager getSurfaceAlignmentManager() { return surfaceAlignmentManager; }
    public fr.skynex.lootglow.managers.GlowTeamManager getGlowTeamManager() { return glowTeamManager; }
    public fr.skynex.lootglow.managers.VisualDisplayManager getVisualDisplayManager() { return visualDisplayManager; }
    public fr.skynex.lootglow.managers.PluginTickManager getPluginTickManager() { return pluginTickManager; }
    public fr.skynex.lootglow.managers.VisualSpawner getVisualSpawner() { return visualSpawner; }
    public fr.skynex.lootglow.config.ConfigParser getConfigParser() { return configParser; }
    public fr.skynex.lootglow.integration.IntegrationManager getIntegrationManager() { return integrationManager; }
    public fr.skynex.lootglow.managers.PlayerSettingsManager getPlayerSettingsManager() { return playerSettingsManager; }
    public fr.skynex.lootglow.managers.VisibilityPacketManager getVisibilityPacketManager() { return visibilityPacketManager; }
    public fr.skynex.lootglow.managers.LODManager getLodManager() { return lodManager; }
    public fr.skynex.lootglow.util.ItemNameFormatter getItemNameFormatter() { return itemNameFormatter; }
    public fr.skynex.lootglow.managers.LootWorldManager getLootWorldManager() { return lootWorldManager; }
    public fr.skynex.lootglow.managers.VanillaItemVisibilityManager getVanillaItemVisibilityManager() { return vanillaItemVisibilityManager; }
    public fr.skynex.lootglow.service.HologramTickService getHologramTickService() { return hologramTickService; }
    public fr.skynex.lootglow.service.BeamTickService getBeamTickService() { return beamTickService; }
    public fr.skynex.lootglow.service.ItemRotationService getItemRotationService() { return itemRotationService; }
    public fr.skynex.lootglow.service.EntityVisibilityService getEntityVisibilityService() { return entityVisibilityService; }
    public fr.skynex.lootglow.service.ItemVisualSpawnService getItemVisualSpawnService() { return itemVisualSpawnService; }
    public fr.skynex.lootglow.service.ItemGroupingService getItemGroupingService() { return itemGroupingService; }
    public fr.skynex.lootglow.service.HologramService getHologramService() { return hologramService; }
    public fr.skynex.lootglow.service.PluginDisableService getPluginDisableService() { return pluginDisableService; }
    public fr.skynex.lootglow.service.MessageService getMessageService() { return messageService; }
    public fr.skynex.lootglow.service.LightService getLightService() { return lightService; }
    public fr.skynex.lootglow.service.ItemGlowApplyService getItemGlowApplyService() { return itemGlowApplyService; }
    public fr.skynex.lootglow.service.ItemPhysicsService getItemPhysicsService() { return itemPhysicsService; }
    public Map<String, Component> getDisplayNameOverridesCache() { return displayNameOverridesCache; }
    public String getEconomyFormat() { return economyFormat; }
    public String getEconomyPrefix() { return economyPrefix; }




    private final Map<String, NamedTextColor> itemCategories = new HashMap<>();
    private final Map<String, NamedTextColor> categoryColors = new HashMap<>();
    private final Map<String, Particle> categoryParticles = new HashMap<>();
    private final Map<String, String> categoryAnimTypes = new HashMap<>();
    private final Map<String, Sound> categorySounds = new HashMap<>();
    private final Map<UUID, TrackedItem> trackedItems = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, TextDisplay> activeLabels = new DelegatingMap<>(ti -> ti.label, (ti, v) -> ti.label = v);
    private final Map<UUID, BlockDisplay> activeBeams = new DelegatingMap<>(ti -> ti.beam, (ti, v) -> ti.beam = v);
    private final Set<String> filteredWorlds = new HashSet<>();
    private boolean isWorldWhitelist = false;
    private final Map<UUID, Long> itemSpawnTimes = new DelegatingMap<>(ti -> ti.spawnTime, (ti, v) -> ti.spawnTime = v);
    private final Map<String, String> categoryNames = new HashMap<>();
    private final Map<String, Component> displayNameOverridesCache = new HashMap<>();
    private final Map<String, Integer> categoryLights = new HashMap<>();
    private final Set<Integer> hiddenVanillaItems = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<UUID, Location> activeLights = new HashMap<>();
    private final Set<UUID> hiddenVisuals = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<UUID> disabledMagnets = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<org.bukkit.block.Block, CropSymbol> activeCropSymbols = new HashMap<>();
    private final Map<UUID, Location> lastFarmingScanLocations = new HashMap<>();
    private final Map<UUID, org.bukkit.entity.Display> activeShadows = new DelegatingMap<>(ti -> ti.shadow, (ti, v) -> ti.shadow = v);
    private final Map<UUID, ItemDisplay> activeItemVisuals = new DelegatingMap<>(ti -> ti.visual, (ti, v) -> ti.visual = v);
    private final Map<UUID, Item> activeItems = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> itemsByWorld = new HashMap<>();

    private final Map<Integer, Component> timerComponentCache = new HashMap<>();
    private final Map<UUID, Set<UUID>> visibleEntities = new HashMap<>();
    private final Map<String, org.bukkit.Particle.DustOptions> categoryDustOptions = new HashMap<>();
    private org.bukkit.Particle.DustOptions defaultDustOptions;
    private Set<UUID> globallyVisibleEntities = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final Map<UUID, Long> lastHoloState = new DelegatingMap<>(ti -> ti.lastHoloState, (ti, v) -> ti.lastHoloState = v);
    private final Map<UUID, Component> baseNameCache = new DelegatingMap<>(ti -> ti.baseName, (ti, v) -> ti.baseName = v);
    private final Map<UUID, String> itemCategoriesCache = new DelegatingMap<>(ti -> ti.category, (ti, v) -> ti.category = v);
    private final Map<UUID, Particle> itemParticlesCache = new DelegatingMap<>(ti -> ti.particle, (ti, v) -> ti.particle = v);

    // Pre-deserialized messages for performance
    private String rawPrefix;
    private String rawAmountFormat;
    private String rawTimerFormat;
    private String rawOwnerFormat;
    private String rawBundleFormat;

    // Farming settings
    private boolean farmingEnabled;
    private NamedTextColor farmingGlowColor;
    private Material farmingMaterial;
    private float farmingScale;
    private double farmingOffset;
    private boolean farmingAnimation;
    private double farmingViewDistance;
    private final Set<Material> farmingCrops = new HashSet<>();

    // RPG Drops settings
    private boolean rpgDropsEnabled;
    private List<String> rpgEnabledCategories;
    private float rpgRotation;
    private float rpgItemScale;
    private float rpgBlockScale;
    private final Set<Material> rpgForceFlatMaterials = new HashSet<>();
    private final Set<Material> rpgForceUprightMaterials = new HashSet<>();

    private NamedTextColor defaultColor;
    private boolean onlyPlayerDrops;
    private boolean isEnabled;
    private Connection dbConnection;
    private boolean usePapi;
    private boolean useWorldGuard;
    private boolean usePacketProvider = false;
    private fr.skynex.lootglow.packets.PacketProvider packetProvider;
    private final Map<Integer, UUID> entityIdMap = new java.util.concurrent.ConcurrentHashMap<>();
    private NamespacedKey farmingKey;
    private NamespacedKey sourceMobKey;
    private int despawnTime;

    // Magnet settings
    private boolean magnetEnabled;
    private boolean magnetEnableForGroups;
    private double magnetDistance;
    private String magnetPermission;
    private List<String> magnetCategories;

    // Hologram settings
    private boolean holoEnabled;
    private double holoOffset;
    // front-offset removed: was applying Z world-space offset instead of player-relative, causing mirrored hologram positions
    private boolean groupingEnabled;
    private boolean holoSeeThrough;
    private boolean holoBackground;
    private float holoViewDistance;
    private boolean holoShowAmount;
    private boolean holoShowTimer;
    private boolean holoTimerNewLine;
    private boolean holoHideUncategorized;

    // WorldGuard settings
    private boolean wgEnabled;
    private List<String> wgBlockedRegions;

    // Performance & LOD
    private boolean lodEnabled;
    private double lodHoloDistSq;
    private double lodBeamDistSq;
    private double lodPartDistSq;
    private int lodInterval;

    // Loot Protection
    private boolean protectionEnabled;
    private int protectionDuration;
    private boolean hardLockEnabled;
    private String bypassPermission;
    private boolean useMythic;

    // Beam settings
    private boolean beamsEnabled;
    private float beamHeight;
    private float beamWidth;
    private List<String> beamCategories;
    private boolean beamsAnimate;
    private boolean beamsUseCategoryColor;

    // Particle settings
    private boolean particlesEnabled;
    private int particlesFrequency;
    private String particleAnimType;
    private int particleTick = 0;
    private int globalSyncTick = 0;
    private double particleSize = 1.0;
    private boolean lightingEnabled;
    private final org.bukkit.block.data.type.Light[] cachedLightBlockData = new org.bukkit.block.data.type.Light[16];

    // Shadow settings
    private boolean shadowsEnabled;
    private float shadowScale;

    // Spawn Animation settings
    private boolean spawnAnimEnabled;
    private double jumpForce;
    private int burstAmount;

    // Interaction settings
    private boolean rmbPickupEnabled;
    private boolean rmbPickupForce;
    private double rmbPickupRange;
    private boolean rmbPickupEnableForGroups;

    // Bouncing settings
    private boolean bouncingEnabled;
    private int maxBounces;
    private double bounceDamping;
    private boolean bouncingOnlyOnSnow;
    private final Set<Material> bouncingBlockedBlocks = new HashSet<>();
    private final Map<UUID, Integer> bounceCounts = new HashMap<>();
    private final Set<UUID> recentlyBounced = new HashSet<>();

    public boolean isFarmingEnabled() {
        return farmingEnabled;
    }

    public boolean isGroupingEnabled() {
        return groupingEnabled;
    }

    public NamespacedKey getSourceMobKey() {
        return sourceMobKey;
    }

    // Aspiration settings
    private boolean aspirationEnabled;
    private double aspirationSpeed;

    private boolean economyEnabled;
    private String economyFormat;
    private String economyPrefix;
    private NamedTextColor economyColor;
    private Sound economySound;
    private final List<NamespacedKey> economyKeys = new ArrayList<>();
    private final Map<UUID, Double> itemMoneyAmounts = new DelegatingMap<>(ti -> ti.moneyAmount, (ti, v) -> ti.moneyAmount = v);

    private boolean defaultGlow;
    private final Map<String, Boolean> categoryGlow = new HashMap<>();

    // Grouping & Container settings
    private boolean useVisualBag;
    private Material bagMaterial;
    private String bagHeadTexture;
    private boolean useOwnerHead;
    private int bagCustomModelData;
    private boolean containerEnabled;
    private String containerTitle;
    private boolean containerRequireClick;
    private final Map<UUID, List<UUID>> groupMembers = new HashMap<>();
    private final Map<UUID, UUID> openContainers = new HashMap<>();

    private final Map<UUID, VisualAnimation> flyingVisuals = new HashMap<>();

    private static class VisualAnimation {
        ItemDisplay display;
        Player target;
        double scale = 1.0;
        int ticks = 0;

        VisualAnimation(ItemDisplay display, Player target) {
            this.display = display;
            this.target = target;
        }
    }


    
    public boolean isWorldAllowed(String worldName) {
        if (isWorldWhitelist) {
            return filteredWorlds.contains(worldName);
        } else {
            return !filteredWorlds.contains(worldName);
        }
    }

    private org.bukkit.configuration.file.FileConfiguration messagesConfig;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onLoad() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            fr.skynex.lootglow.integration.WorldGuardHook.registerFlag();
        } catch (ClassNotFoundException ignored) {
        }
    }

    @Override
    public void onEnable() {
        initManagersAndServices();

        saveDefaultConfig();
        if (getConfig().getBoolean("settings.auto-update-config", true)) {
            ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"));
        }
        loadConfiguration();
        this.farmingKey = new NamespacedKey(this, "farming_symbol");
        this.sourceMobKey = new NamespacedKey(this, "source_mob");

        new fr.skynex.lootglow.util.UpdateChecker(this, 134648).checkUpdateOnStartup();

        this.usePapi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        this.useWorldGuard = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        this.useMythic = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");

        initDatabase();

        registerListeners();
        registerCommands();
        startBackgroundTasks();
        setupPacketProvider();

        // Register LootGlowAPI service provider
        getServer().getServicesManager().register(fr.skynex.lootglow.api.LootGlowAPI.class, this, this, org.bukkit.plugin.ServicePriority.Normal);

        int pluginId = 30993;
        new org.bstats.bukkit.Metrics(this, pluginId);

        FoliaScheduler.runSync(this, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Item item) {
                        applyGlow(item, false);
                    }
                }
            }
        });

        Bukkit.getConsoleSender().sendMessage("§6───────────────────────────────────────────────────────────────────");
        Bukkit.getConsoleSender().sendMessage("§e⚡ If you have any suggestions or encounter any bugs");
        Bukkit.getConsoleSender().sendMessage("§e   related to this plugin, please join this Discord server:");
        Bukkit.getConsoleSender().sendMessage("§8   » §bhttps://discord.gg/3QzcDHC6 §8«");
        Bukkit.getConsoleSender().sendMessage("§7   Thank you for using this plugin — §6§lSkyNex§a. ");
        Bukkit.getConsoleSender().sendMessage("§6   ⭐ §eDon't forget to rate this plugin §6⭐ ");
        Bukkit.getConsoleSender().sendMessage("§6───────────────────────────────────────────────────────────────────");
    }

    @Override
    public void onDisable() {
        this.isEnabled = false;
        closeDatabase();
        if (pluginDisableService != null) {
            pluginDisableService.onDisable(activeLabels, activeBeams, activeItemVisuals, activeShadows, activeCropSymbols, hiddenVanillaItems, entityIdMap, trackedItems, activeLights, activeItems, itemsByWorld, timerComponentCache, bounceCounts, recentlyBounced, lastFarmingScanLocations);
        }
    }


    public void loadConfiguration() {
        if (glowTeamManager != null) {
            glowTeamManager.clearScoreboardTeams();
        }

        reloadConfig();
        loadMessages();
        resetStateOnReload();

        this.isEnabled = getConfig().getBoolean("settings.enabled", true);
        this.onlyPlayerDrops = getConfig().getBoolean("settings.only-player-drops", false);
        
        if (configManager != null) {
            configManager.loadWorldFiltering(getConfig(), filteredWorlds);
        }
        this.despawnTime = getConfig().getInt("settings.despawn-time", 300);
        this.defaultGlow = getConfig().getBoolean("settings.default-glow", true);

        // Grouping & Container
        this.groupingEnabled = getConfig().getBoolean("settings.grouping.enabled", true);
        this.useVisualBag = getConfig().getBoolean("settings.grouping.visual.enabled", true);
        this.bagMaterial = Material
                .matchMaterial(getConfig().getString("settings.grouping.visual.material", "PLAYER_HEAD"));
        if (this.bagMaterial == null)
            this.bagMaterial = Material.PLAYER_HEAD;
        this.bagHeadTexture = getConfig().getString("settings.grouping.visual.head-texture", "");
        this.useOwnerHead = getConfig().getBoolean("settings.grouping.visual.use-owner-head", false);
        this.bagCustomModelData = getConfig().getInt("settings.grouping.visual.custom-model-data", 0);
        this.containerEnabled = getConfig().getBoolean("settings.grouping.container.enabled", true);
        this.containerTitle = getConfig().getString("settings.grouping.container.title",
                "<gradient:gold:white>[Contenu du Butin]</gradient>");
        this.containerRequireClick = getConfig().getBoolean("settings.grouping.container.require-click", true);

        // Economy
        this.economyEnabled = getConfig().getBoolean("settings.economy.enabled", true);
        this.economyFormat = getConfig().getString("settings.economy.format", "<prefix><amount>");
        this.economyPrefix = getConfig().getString("settings.economy.prefix", "&a$&f");
        this.economyColor = parseNamedColor(getConfig().getString("settings.economy.color", "GOLD"));
        String ecoSoundStr = getConfig().getString("settings.economy.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        this.economySound = parseSound(ecoSoundStr);
        if (this.economySound == null) {
            this.economySound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }

        this.shadowsEnabled = getConfig().getBoolean("settings.rpg-drops.shadows.enabled", true);
        this.shadowScale = (float) getConfig().getDouble("settings.rpg-drops.shadows.scale", 0.4);
        this.lightingEnabled = getConfig().getBoolean("settings.lighting.enabled", true);
        for (int i = 0; i <= 15; i++) {
            try {
                org.bukkit.block.data.type.Light lightData = (org.bukkit.block.data.type.Light) Material.LIGHT
                        .createBlockData();
                lightData.setLevel(i);
                cachedLightBlockData[i] = lightData;
            } catch (Exception ignored) {
            }
        }
        if (configManager != null) {
            configManager.loadEconomyKeys(getConfig(), economyKeys);
        }

        // Farming config
        this.farmingEnabled = getConfig().getBoolean("settings.farming.enabled", true);
        this.farmingGlowColor = parseNamedColor(getConfig().getString("settings.farming.glow-color", "GREEN"));
        String symbolMatStr = getConfig().getString("settings.farming.symbol-material", "EMERALD_BLOCK");
        this.farmingMaterial = Material.matchMaterial(symbolMatStr);
        if (this.farmingMaterial == null || !this.farmingMaterial.isBlock()) {
            if (symbolMatStr != null && !symbolMatStr.isEmpty()) {
                getLogger().warning("[LootGlow] Farming symbol-material '" + symbolMatStr + "' is invalid or not a block! Falling back to EMERALD_BLOCK.");
            }
            this.farmingMaterial = Material.EMERALD_BLOCK;
        }
        this.farmingScale = (float) getConfig().getDouble("settings.farming.symbol-scale", 0.2);
        this.farmingOffset = getConfig().getDouble("settings.farming.height-offset", 1.5);
        this.farmingAnimation = getConfig().getBoolean("settings.farming.animation", true);
        this.farmingViewDistance = getConfig().getDouble("settings.farming.view-distance", 24.0);
        if (configManager != null) {
            configManager.loadFarmingCrops(getConfig(), farmingCrops);
        }

        // RPG Drops config
        this.rpgDropsEnabled = getConfig().getBoolean("settings.rpg-drops.enabled", true);
        this.rpgEnabledCategories = getConfig().getStringList("settings.rpg-drops.enabled-categories");
        this.rpgRotation = (float) Math.toRadians(getConfig().getDouble("settings.rpg-drops.rotation-angle", 90.0));
        this.rpgItemScale = (float) getConfig().getDouble("settings.rpg-drops.item-scale", 0.6);
        this.rpgBlockScale = (float) getConfig().getDouble("settings.rpg-drops.block-scale", 0.8);

        if (configManager != null) {
            configManager.loadRpgSettings(getConfig(), rpgForceFlatMaterials, rpgForceUprightMaterials);
        }

        // Fallback for older configs
        if (getConfig().contains("settings.rpg-drops.scale")) {
            float oldScale = (float) getConfig().getDouble("settings.rpg-drops.scale");
            if (!getConfig().contains("settings.rpg-drops.item-scale"))
                this.rpgItemScale = oldScale;
            if (!getConfig().contains("settings.rpg-drops.block-scale"))
                this.rpgBlockScale = oldScale;
        }

        this.holoEnabled = getConfig().getBoolean("settings.holograms.enabled", true);
        this.holoOffset = getConfig().getDouble("settings.holograms.height-offset", 0.7);
        // front-offset removed (caused mirrored hologram positions depending on player facing direction)
        this.holoSeeThrough = getConfig().getBoolean("settings.holograms.see-through", false);
        this.holoBackground = getConfig().getBoolean("settings.holograms.background", false);
        this.holoViewDistance = (float) getConfig().getDouble("settings.holograms.view-distance", 15.0);
        this.holoShowAmount = getConfig().getBoolean("settings.holograms.show-amount", true);
        this.holoShowTimer = getConfig().getBoolean("settings.holograms.show-timer", true);
        this.holoTimerNewLine = getConfig().getBoolean("settings.holograms.timer-on-new-line", true);
        this.holoHideUncategorized = getConfig().getBoolean("settings.holograms.hide-uncategorized", false);

        this.wgEnabled = getConfig().getBoolean("settings.worldguard.enabled", true);
        this.wgBlockedRegions = getConfig().getStringList("settings.worldguard.blocked-regions");

        // LOD Performance loading
        this.lodEnabled = getConfig().getBoolean("settings.performance.lod.enabled", true);
        this.lodHoloDistSq = Math.pow(getConfig().getDouble("settings.performance.lod.hologram-distance", 24.0), 2);
        this.lodBeamDistSq = Math.pow(getConfig().getDouble("settings.performance.lod.beam-distance", 48.0), 2);
        this.lodPartDistSq = Math.pow(getConfig().getDouble("settings.performance.lod.particle-distance", 32.0), 2);
        this.lodInterval = getConfig().getInt("settings.performance.update-interval", 20);

        this.protectionEnabled = getConfig().getBoolean("settings.loot-protection.enabled", true);
        this.protectionDuration = getConfig().getInt("settings.loot-protection.display-duration", 10);
        this.hardLockEnabled = getConfig().getBoolean("settings.loot-protection.hard-lock", true);
        this.bypassPermission = getConfig().getString("settings.loot-protection.bypass-permission",
                "lootglow.bypass.lock");

        this.beamsEnabled = getConfig().getBoolean("settings.beams.enabled", true);
        this.beamHeight = (float) getConfig().getDouble("settings.beams.height", 10.0);
        this.beamWidth = (float) getConfig().getDouble("settings.beams.width", 0.05);
        this.beamCategories = getConfig().getStringList("settings.beams.enabled-categories");
        this.beamsAnimate = getConfig().getBoolean("settings.beams.animate", true);
        this.beamsUseCategoryColor = getConfig().getBoolean("settings.beams.use-category-color", true);

        this.particlesEnabled = getConfig().getBoolean("settings.particles.enabled", true);
        this.particlesFrequency = getConfig().getInt("settings.particles.frequency", 10);
        this.particleAnimType = getConfig().getString("settings.particles.animation-type", "STILL");
        this.particleSize = getConfig().getDouble("settings.particles.size", 1.0);

        this.spawnAnimEnabled = getConfig().getBoolean("settings.spawn-animation.enabled", true);
        this.jumpForce = getConfig().getDouble("settings.spawn-animation.jump-force", 0.25);
        this.burstAmount = getConfig().getInt("settings.spawn-animation.burst-amount", 15);

        // Magnet config
        this.magnetEnabled = getConfig().getBoolean("settings.magnet.enabled", true);
        this.magnetEnableForGroups = getConfig().getBoolean("settings.magnet.enable-for-groups", false);
        this.magnetDistance = getConfig().getDouble("settings.magnet.distance", 5.0);
        this.magnetPermission = getConfig().getString("settings.magnet.permission", "lootglow.magnet");
        this.magnetCategories = getConfig().getStringList("settings.magnet.categories-enabled");

        this.rmbPickupEnabled = getConfig().getBoolean("settings.interaction.rmb-pickup.enabled", false);
        this.rmbPickupForce = getConfig().getBoolean("settings.interaction.rmb-pickup.force", false);
        this.rmbPickupRange = getConfig().getDouble("settings.interaction.rmb-pickup.range", 3.0);
        this.rmbPickupEnableForGroups = getConfig().getBoolean("settings.interaction.rmb-pickup.enable-for-groups", false);

        // Bouncing config
        this.bouncingEnabled = getConfig().getBoolean("settings.spawn-animation.bouncing.enabled", true);
        this.maxBounces = getConfig().getInt("settings.spawn-animation.bouncing.max-bounces", 3);
        this.bounceDamping = getConfig().getDouble("settings.spawn-animation.bouncing.damping", 0.6);
        this.bouncingOnlyOnSnow = getConfig().getBoolean("settings.spawn-animation.bouncing.only-on-snow", false);
        if (configManager != null) {
            configManager.loadBouncingSettings(getConfig(), bouncingBlockedBlocks);
        }

        // Aspiration config
        this.aspirationEnabled = getConfig().getBoolean("settings.aspiration.enabled", true);
        this.aspirationSpeed = getConfig().getDouble("settings.aspiration.speed", 0.15);

        String defColorStr = getConfig().getString("default-color", "WHITE");
        this.defaultColor = parseNamedColor(defColorStr);
        this.defaultDustOptions = new org.bukkit.Particle.DustOptions(
                org.bukkit.Color.fromRGB(defaultColor.red(), defaultColor.green(), defaultColor.blue()),
                (float) particleSize);

        if (configManager != null) {
            configManager.loadCategories(getConfig(), categoryColors, categoryDustOptions, particleSize, categoryLights, categoryGlow, displayNameOverridesCache, miniMessage, itemCategories, categoryNames, categoryParticles, categoryAnimTypes, particleAnimType, categorySounds);
        }

        setupTeams();

        // Re-populate cache and re-apply visuals for existing items on reload
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.isValid()) {
                    applyGlow(item, false);
                }
            }
        }
    }

    private void startParticleTask() {
        if (particleAnimationManager != null) {
            particleAnimationManager.startParticleTask(isEnabled, particlesEnabled, lodPartDistSq, activeItems, itemParticlesCache, itemCategoriesCache, hiddenVisuals, categoryDustOptions, defaultDustOptions, categoryAnimTypes, particleAnimType, particlesFrequency);
        }
    }

    private void startLightingTask() {
        int interval = getConfig().getInt("settings.lighting.update-interval", 5);
        if (lightService != null) {
            lightService.startLightingTask(isEnabled, lightingEnabled, activeLights, activeItems, itemCategoriesCache, categoryLights, cachedLightBlockData, interval);
        }
    }

    private void loadMessages() {
        if (messageService != null) {
            messageService.loadMessages(timerComponentCache);
            this.rawPrefix = messageService.getRawPrefix();
            this.rawAmountFormat = messageService.getRawAmountFormat();
            this.rawTimerFormat = messageService.getRawTimerFormat();
            this.rawOwnerFormat = messageService.getRawOwnerFormat();
            this.rawBundleFormat = messageService.getRawBundleFormat();
        }
    }

    public void sendMessage(CommandSender sender, String key) {
        if (messageService != null) {
            messageService.sendMessage(sender, key);
        }
    }

    public void sendMessage(CommandSender sender, String key, @Nullable Map<String, String> placeholders) {
        if (messageService != null) {
            messageService.sendMessage(sender, key, placeholders);
        }
    }


    private void setupTeams() {
        if (glowTeamManager != null) {
            glowTeamManager.setupTeams();
        }
    }

    public boolean isInBlockedRegion(Location loc) {
        if (!useWorldGuard || !wgEnabled)
            return false;
        return fr.skynex.lootglow.integration.WorldGuardHook.isInBlockedRegion(loc, wgBlockedRegions);
    }

    public void applyGlow(Item item) {
        applyGlow(item, true);
    }

    public void applyGlow(Item item, boolean playAnimation) {
        if (itemGlowApplyService != null) {
            itemGlowApplyService.applyGlow(item, playAnimation, isEnabled, economyEnabled, economyKeys, economyColor, economySound,
                    itemMoneyAmounts, itemCategories, categoryNames, defaultColor, categoryParticles,
                    itemParticlesCache, itemCategoriesCache, despawnTime, entityIdMap, activeItems, itemsByWorld,
                    rpgDropsEnabled, rpgEnabledCategories, categoryGlow, defaultGlow, hiddenVanillaItems,
                    categorySounds, holoEnabled, holoHideUncategorized, itemSpawnTimes, baseNameCache,
                    protectionEnabled, protectionDuration, shadowsEnabled, beamsEnabled, beamCategories);
        }
    }

    public String getInternalId(ItemStack item) {
        return fr.skynex.lootglow.util.CustomItemIdentifier.getInternalId(item, getConfig().getBoolean("settings.debug", false), getLogger());
    }

    public void playSpawnAnimation(Item item, String id) {
        if (particleAnimationManager != null) {
            particleAnimationManager.playSpawnAnimation(item, id, sourceMobKey, categoryParticles, jumpForce, burstAmount);
        }
    }

    public void updateHologram(Item item, NamedTextColor color) {
        if (hologramService != null) {
            hologramService.updateHologram(item, color, holoEnabled, itemCategoriesCache, holoHideUncategorized,
                    activeLabels, groupLeaders, lastHoloState, baseNameCache, displayNameOverridesCache,
                    itemMoneyAmounts, economyFormat, economyPrefix, holoShowAmount, rawAmountFormat,
                    protectionEnabled, protectionDuration, itemSpawnTimes, rawOwnerFormat, usePapi,
                    holoShowTimer, timerComponentCache, holoTimerNewLine);
        }
    }

    private void startGarbageCollectorTask() {
        if (trackedItemManager != null) {
            trackedItemManager.startGarbageCollectorTask(isEnabled, activeItems);
        }
    }

    public void removeGlow(UUID uuid) {
        if (visualSpawner != null) {
            visualSpawner.removeGlow(uuid);
        }
    }

    private void startLODTask() {
        if (lodManager != null) {
            lodManager.startLODTask(isEnabled, lodEnabled, lodBeamDistSq, lodHoloDistSq, farmingViewDistance,
                    visibleEntities, hiddenVisuals, activeItems, groupedItems, activeLabels, activeBeams,
                    activeItemVisuals, activeShadows, itemsByWorld, farmingEnabled, activeCropSymbols, lodInterval, globallyVisibleEntities);
        }
    }


    public void updateEntityVisibility(Player p, Entity entity, boolean shouldSee, Set<UUID> visibleSet) {
        if (entityVisibilityService != null) {
            entityVisibilityService.updateEntityVisibility(p, entity, shouldSee, visibleSet);
        }
    }

    private boolean isHiddenToggleFor(Player p) {
        return hiddenVisuals.contains(p.getUniqueId());
    }

    /**
     * Single unified scheduler that replaces 6 high-frequency independent Bukkit tasks.
     * Runs every tick; internal counter dispatches 2-tick subtasks via modulo.
     * This reduces Bukkit scheduler overhead (6 → 1 dispatch per tick).
     */
    private void startUnifiedTickTask() {
        if (pluginTickManager != null) {
            pluginTickManager.startUnifiedTickTask(
                    this::tickGlobalSync,
                    this::tickBouncing,
                    this::tickAspiration,
                    this::tickMagnet,
                    this::tickFarmingAnimation,
                    this::tickBeamAnimation
            );
        }
    }

    private void tickMagnet() {
        if (itemMagnetManager != null) {
            itemMagnetManager.tickMagnet(magnetEnabled, magnetDistance, magnetPermission, magnetCategories,
                    magnetEnableForGroups, groupLeaders, groupMembers, groupedItems, itemCategoriesCache,
                    protectionEnabled, protectionDuration, itemSpawnTimes);
        }
    }

    private void tickBeamAnimation(float angle) {
        if (beamTickService != null && beamManager != null) {
            beamTickService.tickBeamAnimation(angle, beamsEnabled, beamsAnimate, activeBeams, globallyVisibleEntities,
                    beamManager.getActiveBeamConfigs(), beamHeight, beamWidth, itemParticlesCache);
        }
    }

    private void tickFarmingAnimation(float angle) {
        if (farmingManager != null) {
            farmingManager.tickFarmingAnimation(angle, farmingEnabled, farmingAnimation, globallyVisibleEntities);
        }
    }


    private void startFarmingTask() {
        if (farmingManager != null) {
            farmingManager.startFarmingTask(isEnabled, farmingEnabled, farmingCrops, farmingViewDistance, lastFarmingScanLocations);
        }
    }

    private final Set<UUID> groupedItems = new HashSet<>();
    private final Map<UUID, Integer> groupLeaders = new HashMap<>();

    private void startGroupingTask() {
        if (itemGroupingService != null) {
            itemGroupingService.startGroupingTask(isEnabled, groupingEnabled, trackedItems, activeItems, itemCategoriesCache, groupedItems, groupLeaders, groupMembers, activeItemVisuals, useVisualBag, bagMaterial, bagHeadTexture, useOwnerHead, bagCustomModelData, rpgRotation, holoShowTimer, rawBundleFormat, itemCategories, defaultColor, miniMessage);
        }
    }

    private void spawnHologram(Item item, NamedTextColor color) {
        hologramService.spawnHologram(item, color, holoEnabled, itemCategoriesCache, holoHideUncategorized,
                activeLabels, holoSeeThrough, holoViewDistance, holoBackground, holoOffset,
                baseNameCache, displayNameOverridesCache, itemMoneyAmounts, economyFormat, economyPrefix,
                holoShowAmount, rawAmountFormat, protectionEnabled, protectionDuration, itemSpawnTimes,
                rawOwnerFormat, usePapi, holoShowTimer, timerComponentCache, holoTimerNewLine,
                lodHoloDistSq, hiddenVisuals, visibleEntities);
    }

    public Component calculateBaseName(Item item, NamedTextColor color) {
        return hologramService != null ? hologramService.calculateBaseName(item, color, displayNameOverridesCache, itemMoneyAmounts, economyFormat, economyPrefix) : Component.empty();
    }

    public Component buildFinalName(Item item, Component baseName) {
        return hologramService != null ? hologramService.buildFinalName(item, baseName, holoShowAmount, rawAmountFormat, protectionEnabled, protectionDuration, itemSpawnTimes, rawOwnerFormat, usePapi, holoShowTimer, timerComponentCache, holoTimerNewLine) : baseName;
    }



    public void spawnBeam(Item item, String category, NamedTextColor color) {
        if (beamManager != null) {
            beamManager.spawnBeam(item, category, color, activeBeams, beamHeight, beamWidth, beamsAnimate, beamsUseCategoryColor, lodBeamDistSq, hiddenVisuals, visibleEntities);
        }
    }

    private Material getColorStainedGlass(NamedTextColor color) {
        return beamManager != null ? beamManager.getColorStainedGlass(color) : Material.WHITE_STAINED_GLASS;
    }

    public void removeGlow(Item item) {
        if (item == null)
            return;
        removeGlow(item.getUniqueId());
    }

    /**
     * Like removeGlow but keeps Display entities alive (hologram, visual bag, beam, shadow).
     * Used when removing the group leader during a leader transfer so the displays can be
     * seamlessly re-assigned to the new leader without any flicker or respawn delay.
     */
    public void removeGlowKeepDisplays(UUID uuid) {
        if (visualSpawner != null) {
            visualSpawner.removeGlowKeepDisplays(uuid);
        }
        groupedItems.remove(uuid);
    }

    public void refreshHologram(Item item) {
        if (hologramService != null) {
            hologramService.refreshHologram(item, holoEnabled, holoHideUncategorized, itemCategoriesCache, itemCategories, defaultColor, lastHoloState);
        }
    }

    public void clearVisualsForPlayer(Player player) {
        if (visualDisplayManager != null) {
            visualDisplayManager.clearVisualsForPlayer(player, trackedItems);
        }
    }

    private boolean isNewerVersion(String current, String online) {
        return fr.skynex.lootglow.util.UpdateChecker.isNewerVersion(current, online);
    }

    public void spawnShadow(Item item) {
        if (rpgDropManager != null) {
            rpgDropManager.spawnShadow(item);
        }
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        return commandManager != null && commandManager.onCommand(sender, command, label, args);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        return commandManager != null ? commandManager.onTabComplete(sender, command, alias, args) : Collections.emptyList();
    }

    public boolean isOnlyPlayerDrops() {
        return onlyPlayerDrops;
    }

    /**
     * Pré-enregistre un item comme RPG drop caché AVANT qu'il entre dans le monde.
     * Appelé depuis PlayerDropItemEvent (LOWEST priority) — avant ItemSpawnEvent —
     * pour éviter la race condition où Paper envoie SPAWN_ENTITY aux joueurs
     * proches
     * avant que hiddenVanillaItems soit rempli et que ProtocolLib puisse
     * intercepter.
     */
    public void preHideItem(Item item) {
        if (itemGlowApplyService != null) {
            itemGlowApplyService.preHideItem(item, isEnabled, rpgDropsEnabled, sourceMobKey, itemCategories, categoryNames, rpgEnabledCategories, entityIdMap, hiddenVanillaItems);
        }
    }

    public boolean isHoloEnabled() {
        return holoEnabled;
    }

    public boolean isRpgDropsEnabled() {
        return rpgDropsEnabled;
    }

    public boolean isHardLockEnabled() {
        return hardLockEnabled;
    }

    public int getProtectionDuration() {
        return protectionDuration;
    }

    public String getBypassPermission() {
        return bypassPermission;
    }

    public boolean isProtocolLibEnabled() {
        return usePacketProvider;
    }

    public Map<Integer, UUID> getEntityIdMap() {
        return entityIdMap;
    }

    public boolean isRmbPickupEnabled() {
        return rmbPickupEnabled;
    }

    public boolean isRmbPickupForce() {
        return rmbPickupForce;
    }

    public double getRmbPickupRange() {
        return rmbPickupRange;
    }

    public boolean isRmbPickupEnableForGroups() {
        return rmbPickupEnableForGroups;
    }

    public Map<UUID, Long> getItemSpawnTimes() {
        return itemSpawnTimes;
    }

    public Map<UUID, ItemDisplay> getActiveItemVisuals() {
        return activeItemVisuals;
    }

    public Map<UUID, TextDisplay> getActiveLabels() {
        return activeLabels;
    }

    public Map<UUID, BlockDisplay> getActiveBeams() {
        return activeBeams;
    }

    public Map<UUID, org.bukkit.entity.Display> getActiveShadows() {
        return activeShadows;
    }

    public Set<Integer> getHiddenVanillaItems() {
        return hiddenVanillaItems;
    }

    public Set<UUID> getHiddenVisuals() {
        return hiddenVisuals;
    }

    public boolean isPluginEnabled() {
        return isEnabled;
    }

    // -------------------------------------------------------------------------
    // Packet Handling (ProtocolLib or PacketEvents)
    // -------------------------------------------------------------------------

    private void setupPacketProvider() {
        if (visibilityPacketManager != null) {
            this.packetProvider = visibilityPacketManager.setupPacketProvider();
            this.usePacketProvider = (this.packetProvider != null);
        }
    }

    public void refreshGlowForPlayer(Player player, boolean showVisuals) {
        if (entityVisibilityService != null) {
            entityVisibilityService.refreshGlowForPlayer(player, showVisuals, hiddenVanillaItems, entityIdMap, visibleEntities, farmingViewDistance, activeItems, groupedItems, lodHoloDistSq, lodBeamDistSq, activeCropSymbols);
        }
        for (Item item : activeItems.values()) {
            if (item.getWorld().equals(player.getWorld()) && !hiddenVanillaItems.contains(item.getEntityId())) {
                player.hideEntity(this, item);
                player.showEntity(this, item);
            }
        }
    }


    // -------------------------------------------------------------------------
    // Database
    // -------------------------------------------------------------------------

    private void initDatabase() {
        if (databaseManager != null) {
            databaseManager.initDatabase();
        }
    }

    private void closeDatabase() {
        if (databaseManager != null) {
            databaseManager.closeDatabase();
        }
    }

    public void loadPlayerData(Player player) {
        if (databaseManager != null) {
            databaseManager.loadPlayerData(player, hiddenVisuals, disabledMagnets);
        }
    }

    public Set<UUID> getDisabledMagnets() {
        return disabledMagnets;
    }

    public void savePlayerData(UUID uuid) {
        if (databaseManager != null) {
            boolean hidden = hiddenVisuals.contains(uuid);
            boolean magDisabled = disabledMagnets.contains(uuid);
            databaseManager.savePlayerData(uuid, hidden, magDisabled);
        }
    }

    // -------------------------------------------------------------------------
    // Farming Highlights
    // -------------------------------------------------------------------------

    public boolean isFarmingAllowed(Location loc) {
        return farmingManager != null ? farmingManager.isFarmingAllowed(loc) : true;
    }

    public void spawnCropSymbol(org.bukkit.block.Block block) {
        if (farmingManager != null) {
            farmingManager.spawnCropSymbol(block);
        }
    }

    public void removeCropSymbol(org.bukkit.block.Block block) {
        if (farmingManager != null) {
            farmingManager.removeCropSymbol(block);
        }
    }

    public void relinkCropSymbol(org.bukkit.block.Block block, BlockDisplay bd) {
        bd.setVisibleByDefault(false);
        CropSymbol parts = activeCropSymbols.computeIfAbsent(block, k -> new CropSymbol(block.getLocation().add(0.5, farmingOffset, 0.5)));
        if (!parts.contains(bd)) {
            parts.add(bd);
        }
        updateCropSymbolVisibilityForWorld(parts);
    }

    public void updateCropSymbolVisibilityForWorld(CropSymbol cs) {
        if (cs == null || cs.isEmpty()) return;
        double farmDistSq = farmingViewDistance * farmingViewDistance;
        Location loc = cs.location;
        World world = loc.getWorld();
        if (world == null) return;

        for (Player p : world.getPlayers()) {
            UUID pUuid = p.getUniqueId();
            boolean isHiddenToggle = isHiddenToggleFor(p);
            boolean shouldSee = !isHiddenToggle && p.getLocation().distanceSquared(loc) <= farmDistSq;
            Set<UUID> visibleSet = visibleEntities.computeIfAbsent(pUuid, k -> new HashSet<>());

            for (BlockDisplay bd : cs) {
                if (bd == null || !bd.isValid()) continue;
                bd.setVisibleByDefault(false);
                if (shouldSee) {
                    p.showEntity(this, bd);
                    visibleSet.add(bd.getUniqueId());
                } else {
                    p.hideEntity(this, bd);
                    visibleSet.remove(bd.getUniqueId());
                }
            }
        }
    }

    public boolean isUseWorldGuard() { return useWorldGuard; }
    public boolean isWgEnabled() { return wgEnabled; }
    public Material getFarmingMaterial() { return farmingMaterial; }
    public double getFarmingOffset() { return farmingOffset; }
    public float getFarmingScale() { return farmingScale; }
    public NamedTextColor getFarmingGlowColor() { return farmingGlowColor; }
    public float getShadowScale() { return shadowScale; }
    public double getLodHoloDistSq() { return lodHoloDistSq; }
    public Map<UUID, Set<UUID>> getVisibleEntities() { return visibleEntities; }
    public void setGloballyVisibleEntities(Set<UUID> set) { this.globallyVisibleEntities = set; }


    public Set<Material> getFarmingCrops() {
        return farmingCrops;
    }

    public NamespacedKey getFarmingKey() {
        return farmingKey;
    }

    /**
     * Force la cohérence vanilla/display pour un RPG drop sur tous les joueurs du
     * monde.
     * Respecte le toggle hiddenVisuals : les joueurs qui veulent voir le vanilla
     * item
     * l'ont via showEntity (override de setVisibleByDefault(false)), les autres
     * voient
     * l'ItemDisplay.
     */
    public void broadcastRpgDropVisibility(Item item) {
        if (entityVisibilityService != null) {
            entityVisibilityService.broadcastRpgDropVisibility(item, activeItemVisuals, hiddenVisuals, groupedItems);
        }
    }

    public void spawnItemVisual(Item item, String category, NamedTextColor color) {
        if (itemVisualSpawnService != null) {
            itemVisualSpawnService.spawnItemVisual(item, category, color,
                    useVisualBag, rpgDropsEnabled, groupLeaders,
                    activeItemVisuals, entityIdMap, new java.util.HashSet<>(rpgEnabledCategories),
                    hiddenVisuals, visibleEntities, categoryGlow, defaultGlow,
                    bagMaterial, bagHeadTexture, useOwnerHead, bagCustomModelData,
                    rpgItemScale, rpgBlockScale, rpgRotation);
        }
    }

    /** Global sync tick: repositions all Display entities to follow their parent Item. Runs every tick. */
    private void tickGlobalSync() {
        if (itemPhysicsService != null) {
            itemPhysicsService.tickGlobalSync(isEnabled, activeItems, trackedItems, rpgBlockScale, rpgItemScale, bagMaterial, groupLeaders, holoOffset, shadowScale, rpgRotation);
        }
    }

    /** Bouncing tick: applies bounce physics to items. Runs every tick. */
    private void tickBouncing() {
        if (rpgDropManager != null) {
            rpgDropManager.tickBouncing(bouncingEnabled, activeItems, bouncingBlockedBlocks, bouncingOnlyOnSnow, maxBounces, jumpForce, bounceDamping);
        }
    }



    public void playAspirationAnimation(Item item, Player player) {
        if (rpgDropManager != null) {
            rpgDropManager.playAspirationAnimation(item, player, activeItemVisuals, aspirationEnabled);
        }
    }

    /** Aspiration animation tick: flies item visuals towards the collecting player. Runs every tick. */
    private void tickAspiration() {
        if (rpgDropManager != null) {
            rpgDropManager.tickAspiration(aspirationEnabled, aspirationSpeed);
        }
    }


    public void openLootContainer(Player player, UUID leaderUuid) {
        if (groupContainerManager != null) {
            groupContainerManager.openLootContainer(player, leaderUuid, containerEnabled, containerTitle, activeItemVisuals, rpgBlockScale, miniMessage);
        }
    }

    public Map<UUID, UUID> getOpenContainers() {
        return groupContainerManager != null ? groupContainerManager.getOpenContainers() : openContainers;
    }

    public Map<UUID, List<UUID>> getGroupMembers() {
        return groupContainerManager != null ? groupContainerManager.getGroupMembers() : groupMembers;
    }

    public Map<UUID, Item> getActiveItems() {
        return trackedItemManager != null ? trackedItemManager.getActiveItems() : activeItems;
    }

    public Set<UUID> getGroupedItems() {
        return groupContainerManager != null ? groupContainerManager.getGroupedItems() : groupedItems;
    }

    public boolean isContainerEnabled() {
        return containerEnabled;
    }

    public boolean isContainerRequireClick() {
        return containerRequireClick;
    }

    public UUID getGroupLeader(UUID itemUuid) {
        return groupContainerManager != null ? groupContainerManager.getGroupLeader(itemUuid) : null;
    }

    public ItemStack createTexturedHead(String textureInput) {
        return visualDisplayManager != null ? visualDisplayManager.createTexturedHead(textureInput) : new ItemStack(Material.PLAYER_HEAD);
    }

    private String getBase64Texture(String input) {
        return visualDisplayManager != null ? visualDisplayManager.getBase64Texture(input) : null;
    }

    public Item getItemForDisplay(ItemDisplay display) {
        return trackedItemManager != null ? trackedItemManager.getItemForDisplay(display) : null;
    }

    public Item getItemForLabel(TextDisplay label) {
        return trackedItemManager != null ? trackedItemManager.getItemForLabel(label) : null;
    }

    public void transferLeaderVisuals(UUID oldLeader, UUID newLeader) {
        if (groupContainerManager != null) {
            groupContainerManager.transferLeaderVisuals(oldLeader, newLeader);
        }
    }

    public ItemStack getOwnerHead(UUID owner) {
        return visualDisplayManager != null ? visualDisplayManager.getOwnerHead(owner) : new ItemStack(Material.PLAYER_HEAD);
    }

    public Map<UUID, Location> getLastFarmingScanLocations() {
        return lastFarmingScanLocations;
    }



    public boolean isFlatItemOrBlock(Material mat) {
        return fr.skynex.lootglow.util.ItemTypeClassifier.isFlatItemOrBlock(mat, rpgForceFlatMaterials, rpgForceUprightMaterials);
    }

    public boolean isUprightItem(Material mat) {
        return fr.skynex.lootglow.util.ItemTypeClassifier.isUprightItem(mat, rpgForceFlatMaterials, rpgForceUprightMaterials);
    }

    public boolean isFishItem(Material mat) {
        return fr.skynex.lootglow.util.ItemTypeClassifier.isFishItem(mat);
    }

    public boolean isCustomItem(ItemStack stack) {
        return fr.skynex.lootglow.util.ItemTypeClassifier.isCustomItem(stack);
    }

    private Double getMoneyAmount(ItemStack stack) {
        return fr.skynex.lootglow.util.MoneyAmountParser.getMoneyAmount(stack, economyEnabled, economyKeys);
    }

    public NamedTextColor parseNamedColor(String input) {
        return configParser != null ? configParser.parseNamedColor(input) : NamedTextColor.WHITE;
    }

    public Sound parseSound(String input) {
        return configParser != null ? configParser.parseSound(input) : null;
    }

    public void updateSurfaceAlignment(Item item) {
        if (surfaceAlignmentManager != null) {
            surfaceAlignmentManager.updateSurfaceAlignment(item, recentlyBounced);
        }
    }

    private void cleanVisibleSet(UUID entUuid) {
        if (entUuid == null) return;
        for (Set<UUID> set : visibleEntities.values()) {
            set.remove(entUuid);
        }
    }

    // =========================================================================
    // Helper classes
    // =========================================================================

    /**
     * Regroupe tous les états visuels et cachés d'un item tracké en un seul objet.
     * Remplace les 10+ Maps séparées par un seul lookup dans trackedItems.
     */
    public static class TrackedItem {
        // Displays visuels
        public TextDisplay label;
        public BlockDisplay beam;
        public ItemDisplay visual;
        public org.bukkit.entity.Display shadow;
        // Timing
        public Long spawnTime;
        // Hologram state
        public Long lastHoloState;
        public Component baseName;
        // Catégorie & particules
        public String category;
        public Particle particle;
        // Économie
        public Double moneyAmount;
        // Throttle ray-trace (globalSyncTick du dernier appel à updateSurfaceAlignment)
        public int lastRayTraceTick = -999;
    }

    /**
     * Wrapper pour les symboles de crop farming.
     * Étend ArrayList<BlockDisplay> pour la compatibilité avec le code existant
     * et pré-cache la Location pour éviter block.getLocation() dans les boucles LOD.
     */
    static final class CropSymbol extends java.util.ArrayList<BlockDisplay> {
        final Location location;
        CropSymbol(Location location) {
            this.location = location;
        }
    }

    /**
     * Map délégante qui route les accès vers trackedItems pour assurer la
     * compatibilité binaire des getters publics (ex: getActiveLabels()).
     * Chaque opération get/put/remove lit/écrit directement dans le TrackedItem
     * correspondant, sans créer de Map intermédiaire.
     */
    private final class DelegatingMap<V> extends java.util.AbstractMap<UUID, V> {
        private final java.util.function.Function<TrackedItem, V> getter;
        private final java.util.function.BiConsumer<TrackedItem, V> setter;

        DelegatingMap(java.util.function.Function<TrackedItem, V> getter,
                      java.util.function.BiConsumer<TrackedItem, V> setter) {
            this.getter = getter;
            this.setter = setter;
        }

        private TrackedItem getOrCreate(UUID uuid) {
            return trackedItems.computeIfAbsent(uuid, k -> new TrackedItem());
        }

        @Override
        public V get(Object key) {
            TrackedItem ti = trackedItems.get(key);
            return ti == null ? null : getter.apply(ti);
        }

        @Override
        public boolean containsKey(Object key) {
            TrackedItem ti = trackedItems.get(key);
            return ti != null && getter.apply(ti) != null;
        }

        @Override
        public V put(UUID key, V value) {
            TrackedItem ti = getOrCreate(key);
            V old = getter.apply(ti);
            setter.accept(ti, value);
            return old;
        }

        @Override
        public V remove(Object key) {
            TrackedItem ti = trackedItems.get(key);
            if (ti == null) return null;
            V old = getter.apply(ti);
            setter.accept(ti, null);
            return old;
        }

        @Override
        public void clear() {
            trackedItems.values().forEach(ti -> setter.accept(ti, null));
        }

        @Override
        public java.util.Set<java.util.Map.Entry<UUID, V>> entrySet() {
            java.util.Set<java.util.Map.Entry<UUID, V>> result = new java.util.LinkedHashSet<>();
            for (java.util.Map.Entry<UUID, TrackedItem> e : trackedItems.entrySet()) {
                V v = getter.apply(e.getValue());
                if (v != null) {
                    result.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey(), v));
                }
            }
            return result;
        }
    }

    // ==========================================
    //            LootGlowAPI Implementation
    // ==========================================

    @Override
    public void setGlowColor(@NotNull Item item, @NotNull Color color) {
        if (item == null || !item.isValid() || color == null) return;
        item.setGlowing(true);
    }

    @Override
    public void setGlowColor(@NotNull Item item, @NotNull Color color, @NotNull Player player) {
        if (item == null || !item.isValid() || color == null || player == null || !player.isOnline()) return;
        item.setGlowing(true);
    }

    @Override
    public void resetGlowColor(@NotNull Item item) {
        if (item == null || !item.isValid()) return;
        item.setGlowing(true);
    }

    @Override
    public void resetGlowColor(@NotNull Item item, @NotNull Player player) {
        if (item == null || !item.isValid() || player == null || !player.isOnline()) return;
        item.setGlowing(true);
    }

    @Override
    public void setCustomHologram(@NotNull Item item, @Nullable String text) {
        if (item == null || !item.isValid()) return;
        TextDisplay display = activeLabels.get(item.getUniqueId());
        if (display != null && display.isValid()) {
            if (text == null || text.isEmpty()) {
                display.text(Component.empty());
            } else {
                display.text(MiniMessage.miniMessage().deserialize(text));
            }
        }
    }

    @Override
    public void setCustomHologram(@NotNull Item item, @Nullable String text, @NotNull Player player) {
        if (item == null || !item.isValid() || player == null || !player.isOnline()) return;
        setCustomHologram(item, text);
    }

    @Override
    public void setBeaconBeam(@NotNull Item item, boolean enabled) {
        setBeaconBeam(item, enabled, null);
    }

    @Override
    public void setBeaconBeam(@NotNull Item item, boolean enabled, @Nullable Color color) {
        if (item == null || !item.isValid()) return;
        if (!enabled) {
            BlockDisplay beam = activeBeams.remove(item.getUniqueId());
            if (beam != null && beam.isValid()) beam.remove();
        } else {
            spawnBeam(item, null, NamedTextColor.WHITE);
        }
    }

    @Override
    public void setLootProtection(@NotNull Item item, @NotNull UUID ownerUuid, long durationSeconds) {
        if (lootProtectionManager != null) {
            lootProtectionManager.setLootProtection(item, ownerUuid, durationSeconds);
        }
    }

    @Override
    public boolean isLootProtected(@NotNull Item item) {
        return lootProtectionManager != null ? lootProtectionManager.isLootProtected(item) : false;
    }

    @Override
    public boolean isPlayerAllowedToPickup(@NotNull Player player, @NotNull Item item) {
        return lootProtectionManager != null ? lootProtectionManager.isPlayerAllowedToPickup(player, item) : true;
    }

    @Override
    public UUID getLootOwner(@NotNull Item item) {
        return lootProtectionManager != null ? lootProtectionManager.getLootOwner(item) : null;
    }

    @Override
    public boolean isMagnetEnabled(@NotNull Player player) {
        return itemMagnetManager != null && itemMagnetManager.isMagnetEnabled(player);
    }

    @Override
    public void setMagnetEnabled(@NotNull Player player, boolean enabled) {
        if (itemMagnetManager != null) {
            itemMagnetManager.setMagnetEnabled(player, enabled);
        }
    }

    @Override
    public void pullItemsToPlayer(@NotNull Player player, double radius) {
        if (itemMagnetManager != null) {
            itemMagnetManager.pullItemsToPlayer(player, radius);
        }
    }

    @Override
    public boolean isVisualsHidden(@NotNull Player player) {
        return player != null && hiddenVisuals.contains(player.getUniqueId());
    }

    @Override
    public void setVisualsHidden(@NotNull Player player, boolean hidden) {
        if (player == null) return;
        if (hidden) {
            hiddenVisuals.add(player.getUniqueId());
        } else {
            hiddenVisuals.remove(player.getUniqueId());
        }
    }

    @Override
    public boolean hasLineOfSight(@NotNull Player player, @NotNull Item item, double maxDistance) {
        return occlusionManager != null && occlusionManager.hasLineOfSight(player, item, maxDistance);
    }

    @Override
    public boolean updateOcclusionVisibility(@NotNull Player player, @NotNull Item item, double maxDistance) {
        boolean visible = hasLineOfSight(player, item, maxDistance);
        setVisualsHidden(player, !visible);
        return visible;
    }

    @Override
    public void setParticleEffect(@NotNull Item item, @Nullable Particle particle) {
        if (item == null || !item.isValid()) return;
        itemParticlesCache.put(item.getUniqueId(), particle);
    }

    @Override
    public void clearParticleEffect(@NotNull Item item) {
        if (item == null || !item.isValid()) return;
        itemParticlesCache.remove(item.getUniqueId());
    }

    @Override
    public void setDropSound(@NotNull Item item, @Nullable Sound sound, float volume, float pitch) {
        if (item == null || !item.isValid() || sound == null) return;
        item.getWorld().playSound(item.getLocation(), sound, volume, pitch);
    }

    @Override
    public void triggerPopAnimation(@NotNull Item item, double jumpVelocity) {
        if (particleAnimationManager != null) {
            particleAnimationManager.triggerPopAnimation(item, jumpVelocity);
        }
    }

    @Override
    public void setBouncingEnabled(@NotNull Item item, boolean bouncing) {
        if (particleAnimationManager != null) {
            particleAnimationManager.setBouncingEnabled(item, bouncing, recentlyBounced);
        }
    }

    @Override
    public void setCropHighlight(@NotNull org.bukkit.block.Block cropBlock, boolean highlight) {
        if (farmingManager != null) {
            farmingManager.setCropHighlight(cropBlock, highlight);
        }
    }

    @Override
    public boolean isCropHighlighted(@NotNull org.bukkit.block.Block cropBlock) {
        return farmingManager != null && farmingManager.isCropHighlighted(cropBlock);
    }

    @Override
    public void setItemCategory(@NotNull Item item, @NotNull String category) {
        if (item == null || !item.isValid() || category == null) return;
        if (trackedItemManager != null) {
            trackedItemManager.setItemCategory(item.getUniqueId(), category);
        }
    }

    @Nullable
    @Override
    public String getItemCategory(@NotNull Item item) {
        if (item == null) return null;
        return trackedItemManager != null ? trackedItemManager.getItemCategory(item.getUniqueId()) : null;
    }

    @NotNull
    @Override
    public List<Item> getNearbyGlowingItems(@NotNull Location location, double radius) {
        return trackedItemManager != null ? trackedItemManager.getNearbyGlowingItems(location, radius) : List.of();
    }

    private void resetStateOnReload() {
        for (TrackedItem ti : trackedItems.values()) {
            if (ti.label != null && ti.label.isValid()) ti.label.remove();
            if (ti.beam != null && ti.beam.isValid()) {
                ti.beam.getPassengers().forEach(e -> { if (e != null) e.remove(); });
                ti.beam.remove();
            }
            if (ti.visual != null && ti.visual.isValid()) ti.visual.remove();
            if (ti.shadow != null && ti.shadow.isValid()) ti.shadow.remove();
        }
        trackedItems.clear();
        activeItems.clear();
        itemsByWorld.clear();
        entityIdMap.clear();
        hiddenVanillaItems.clear();
        itemSpawnTimes.clear();
        groupMembers.clear();
        itemCategories.clear();
        categoryParticles.clear();
        categorySounds.clear();
        categoryNames.clear();
        categoryGlow.clear();
        categoryColors.clear();
        displayNameOverridesCache.clear();
        categoryLights.clear();
        activeLights.forEach((uuid, loc) -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        });
        activeLights.clear();
        activeCropSymbols.values().forEach(list -> list.forEach(d -> {
            if (d != null && d.isValid())
                d.remove();
        }));
        activeCropSymbols.clear();
        visibleEntities.clear();
        categoryDustOptions.clear();
        filteredWorlds.clear();
        if (surfaceAlignmentManager != null) {
            surfaceAlignmentManager.clearAll();
        }
        lastFarmingScanLocations.clear();

        globallyVisibleEntities.clear();
        groupedItems.clear();
        groupLeaders.clear();
        openContainers.clear();
        if (beamManager != null) beamManager.getActiveBeamConfigs().clear();
        recentlyBounced.clear();
        bounceCounts.clear();
    }

    private void initManagersAndServices() {
        this.databaseManager = new DatabaseManager(this);
        this.trackedItemManager = new TrackedItemManager(this);
        this.beamManager = new BeamManager(this);
        this.hologramManager = new HologramManager(this);
        this.configManager = new LootGlowConfigManager(this);
        this.commandManager = new LootGlowCommandManager(this);
        this.farmingManager = new fr.skynex.lootglow.managers.FarmingManager(this);
        this.rpgDropManager = new fr.skynex.lootglow.managers.RPGDropManager(this);
        this.particleAnimationManager = new fr.skynex.lootglow.managers.ParticleAnimationManager(this);
        this.groupContainerManager = new fr.skynex.lootglow.managers.GroupContainerManager(this);
        this.lootProtectionManager = new fr.skynex.lootglow.managers.LootProtectionManager(this);
        this.occlusionManager = new fr.skynex.lootglow.managers.OcclusionManager(this);
        this.glowManager = new fr.skynex.lootglow.managers.GlowManager(this);
        this.itemMagnetManager = new fr.skynex.lootglow.managers.ItemMagnetManager(this);
        this.economyDropManager = new fr.skynex.lootglow.managers.EconomyDropManager(this);
        this.hologramRenderer = new fr.skynex.lootglow.managers.HologramRenderer(this);
        this.surfaceAlignmentManager = new fr.skynex.lootglow.managers.SurfaceAlignmentManager(this);
        this.glowTeamManager = new fr.skynex.lootglow.managers.GlowTeamManager(this);
        this.visualDisplayManager = new fr.skynex.lootglow.managers.VisualDisplayManager(this);
        this.pluginTickManager = new fr.skynex.lootglow.managers.PluginTickManager(this);
        this.visualSpawner = new fr.skynex.lootglow.managers.VisualSpawner(this);
        this.configParser = new fr.skynex.lootglow.config.ConfigParser(this);
        this.integrationManager = new fr.skynex.lootglow.integration.IntegrationManager(this);
        this.playerSettingsManager = new fr.skynex.lootglow.managers.PlayerSettingsManager(this);
        this.visibilityPacketManager = new fr.skynex.lootglow.managers.VisibilityPacketManager(this);
        this.lodManager = new fr.skynex.lootglow.managers.LODManager(this);
        this.itemNameFormatter = new fr.skynex.lootglow.util.ItemNameFormatter(this);
        this.lootWorldManager = new fr.skynex.lootglow.managers.LootWorldManager(this);
        this.vanillaItemVisibilityManager = new fr.skynex.lootglow.managers.VanillaItemVisibilityManager(this);
        this.hologramTickService = new fr.skynex.lootglow.service.HologramTickService(this);
        this.beamTickService = new fr.skynex.lootglow.service.BeamTickService(this);
        this.itemRotationService = new fr.skynex.lootglow.service.ItemRotationService(this);
        this.entityVisibilityService = new fr.skynex.lootglow.service.EntityVisibilityService(this);
        this.itemVisualSpawnService = new fr.skynex.lootglow.service.ItemVisualSpawnService(this);
        this.itemGroupingService = new fr.skynex.lootglow.service.ItemGroupingService(this);
        this.hologramService = new fr.skynex.lootglow.service.HologramService(this);
        this.pluginDisableService = new fr.skynex.lootglow.service.PluginDisableService(this);
        this.messageService = new fr.skynex.lootglow.service.MessageService(this);
        this.lightService = new fr.skynex.lootglow.service.LightService(this);
        this.itemGlowApplyService = new fr.skynex.lootglow.service.ItemGlowApplyService(this);
        this.itemPhysicsService = new fr.skynex.lootglow.service.ItemPhysicsService(this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.FarmingListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.LootContainerListener(this), this);
        if (useMythic) {
            try {
                getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.MythicListener(this), this);
            } catch (NoClassDefFoundError ignored) {}
        }
    }

    private void registerCommands() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final var registrar = event.registrar();
            registrar.register("lootglow", "Main command for LootGlow", List.of("lg", "glow", "loot"),
                    new BasicCommand() {
                        @Override
                        public void execute(CommandSourceStack stack, String[] args) {
                            onCommand(stack.getSender(), null, "lootglow", args);
                        }

                        @Override
                        public java.util.Collection<String> suggest(CommandSourceStack stack, String[] args) {
                            return onTabComplete(stack.getSender(), null, "lootglow", args);
                        }
                    });
        });
    }

    private void startBackgroundTasks() {
        startParticleTask();
        startLightingTask();
        startLODTask();
        startGarbageCollectorTask();
        startGroupingTask();
        startFarmingTask();
        startUnifiedTickTask();
    }
}