package fr.skynex.lootglow;

import java.io.File;
import fr.skynex.lootglow.util.FoliaScheduler;
import fr.skynex.lootglow.database.DatabaseManager;
import fr.skynex.lootglow.managers.TrackedItemManager;
import fr.skynex.lootglow.managers.BeamManager;
import fr.skynex.lootglow.managers.HologramManager;
import fr.skynex.lootglow.config.LootGlowConfigManager;
import fr.skynex.lootglow.commands.LootGlowCommandManager;
import fr.skynex.lootglow.model.TrackedItem;
import fr.skynex.lootglow.model.CropSymbol;
import fr.skynex.lootglow.model.DelegatingMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private fr.skynex.lootglow.managers.ItemMergeManager itemMergeManager;
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
    private fr.skynex.lootglow.managers.PluginLifecycleManager pluginLifecycleManager;
    private fr.skynex.lootglow.managers.RarityManager rarityManager;
    private fr.skynex.lootglow.managers.GroundAuraManager groundAuraManager;
    private fr.skynex.lootglow.pipeline.LootRenderPipeline lootRenderPipeline;
    private fr.skynex.lootglow.registry.ServiceRegistry serviceRegistry;
    private fr.skynex.lootglow.spatial.LootSpatialIndexService spatialIndexService;
    private fr.skynex.lootglow.event.LootEventDispatcher lootEventDispatcher;

    public fr.skynex.lootglow.registry.ServiceRegistry getServiceRegistry() { return serviceRegistry; }
    public fr.skynex.lootglow.spatial.LootSpatialIndexService getSpatialIndexService() { return spatialIndexService; }
    public fr.skynex.lootglow.event.LootEventDispatcher getLootEventDispatcher() { return lootEventDispatcher; }
    public fr.skynex.lootglow.pipeline.LootRenderPipeline getLootRenderPipeline() { return lootRenderPipeline; }
    public fr.skynex.lootglow.managers.RarityManager getRarityManager() { return rarityManager; }
    public fr.skynex.lootglow.managers.PluginLifecycleManager getPluginLifecycleManager() { return pluginLifecycleManager; }

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
    public fr.skynex.lootglow.managers.ItemMergeManager getItemMergeManager() { return itemMergeManager; }
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
    public fr.skynex.lootglow.managers.GroundAuraManager getGroundAuraManager() { return groundAuraManager; }
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
    public String getEconomyFormat() { return configManager != null ? configManager.getEconomyFormat() : ""; }
    public String getEconomyPrefix() { return configManager != null ? configManager.getEconomyPrefix() : ""; }
    public Map<UUID, Particle> getItemParticlesCache() { return itemParticlesCache; }
    public Set<UUID> getRecentlyBounced() { return rpgDropManager != null ? rpgDropManager.getRecentlyBounced() : Collections.emptySet(); }
    public double getFarmingViewDistance() { return configManager != null ? configManager.getFarmingViewDistance() : 0; }
    public double getLodBeamDistSq() { return configManager != null ? configManager.getLodBeamDistSq() : 0; }
    public Map<org.bukkit.block.Block, CropSymbol> getActiveCropSymbols() { return activeCropSymbols; }
    public Map<UUID, TrackedItem> getTrackedItems() { return trackedItems; }
    public Map<String, Set<UUID>> getItemsByWorld() { return trackedItemManager != null ? trackedItemManager.getItemsByWorld() : Collections.emptyMap(); }
    public Map<String, NamedTextColor> getItemCategories() { return itemCategories; }
    public Map<String, Particle> getCategoryParticles() { return categoryParticles; }
    public Map<String, Sound> getCategorySounds() { return categorySounds; }
    public Map<String, String> getCategoryNames() { return categoryNames; }
    public Map<String, NamedTextColor> getCategoryColors() { return categoryColors; }
    public Map<String, Integer> getCategoryLights() { return categoryLights; }
    public Map<UUID, Location> getActiveLights() { return activeLights; }
    public Map<String, org.bukkit.Particle.DustOptions> getCategoryDustOptions() { return categoryDustOptions; }
    public Set<UUID> getGloballyVisibleEntities() { return globallyVisibleEntities; }
    public Map<UUID, Integer> getBounceCounts() { return rpgDropManager != null ? rpgDropManager.getBounceCounts() : Collections.emptyMap(); }
    public Map<UUID, String> getItemCategoriesCache() { return itemCategoriesCache; }




    private final Map<String, NamedTextColor> itemCategories = new HashMap<>();
    private final Map<String, NamedTextColor> categoryColors = new HashMap<>();
    private final Map<String, Particle> categoryParticles = new HashMap<>();
    private final Map<String, String> categoryAnimTypes = new HashMap<>();
    private final Map<String, Sound> categorySounds = new HashMap<>();
    private final Map<UUID, TrackedItem> trackedItems = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, TextDisplay> activeLabels = new DelegatingMap<>(trackedItems, ti -> ti.label, (ti, v) -> ti.label = v, (dUuid, iUuid) -> { if (trackedItemManager != null) trackedItemManager.registerDisplayEntity(dUuid, iUuid); }, dUuid -> { if (trackedItemManager != null) trackedItemManager.unregisterDisplayEntity(dUuid); });
    private final Map<UUID, BlockDisplay> activeBeams = new DelegatingMap<>(trackedItems, ti -> ti.beam, (ti, v) -> ti.beam = v, (dUuid, iUuid) -> { if (trackedItemManager != null) trackedItemManager.registerDisplayEntity(dUuid, iUuid); }, dUuid -> { if (trackedItemManager != null) trackedItemManager.unregisterDisplayEntity(dUuid); });
    private final Map<UUID, Long> itemSpawnTimes = new DelegatingMap<>(trackedItems, ti -> ti.spawnTime, (ti, v) -> ti.spawnTime = v);
    private final Map<String, String> categoryNames = new HashMap<>();
    private final Map<String, Component> displayNameOverridesCache = new HashMap<>();
    private final Map<String, Integer> categoryLights = new HashMap<>();
    private final Set<Integer> hiddenVanillaItems = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<UUID, Location> activeLights = new HashMap<>();
    private final Set<UUID> hiddenVisuals = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<UUID> disabledMagnets = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<org.bukkit.block.Block, CropSymbol> activeCropSymbols = new HashMap<>();
    private final Map<UUID, Location> lastFarmingScanLocations = new HashMap<>();
    private final Map<UUID, org.bukkit.entity.Display> activeShadows = new DelegatingMap<>(trackedItems, ti -> ti.shadow, (ti, v) -> ti.shadow = v, (dUuid, iUuid) -> { if (trackedItemManager != null) trackedItemManager.registerDisplayEntity(dUuid, iUuid); }, dUuid -> { if (trackedItemManager != null) trackedItemManager.unregisterDisplayEntity(dUuid); });
    private final Map<UUID, ItemDisplay> activeItemVisuals = new DelegatingMap<>(trackedItems, ti -> ti.visual, (ti, v) -> ti.visual = v, (dUuid, iUuid) -> { if (trackedItemManager != null) trackedItemManager.registerDisplayEntity(dUuid, iUuid); }, dUuid -> { if (trackedItemManager != null) trackedItemManager.unregisterDisplayEntity(dUuid); });
    private final Map<UUID, Item> activeItems = new java.util.concurrent.ConcurrentHashMap<>();

    private final Map<Integer, Component> timerComponentCache = new HashMap<>();
    private final Map<UUID, Set<UUID>> visibleEntities = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, org.bukkit.Particle.DustOptions> categoryDustOptions = new HashMap<>();
    private org.bukkit.Particle.DustOptions defaultDustOptions;
    private Set<UUID> globallyVisibleEntities = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final Map<UUID, Long> lastHoloState = new DelegatingMap<>(trackedItems, ti -> ti.lastHoloState, (ti, v) -> ti.lastHoloState = v);
    private final Map<UUID, Component> baseNameCache = new DelegatingMap<>(trackedItems, ti -> ti.baseName, (ti, v) -> ti.baseName = v);
    private final Map<UUID, String> itemCategoriesCache = new DelegatingMap<>(trackedItems, ti -> ti.category, (ti, v) -> ti.category = v);
    private final Map<UUID, Particle> itemParticlesCache = new DelegatingMap<>(trackedItems, ti -> ti.particle, (ti, v) -> ti.particle = v);
    private final Map<UUID, Double> itemMoneyAmounts = new DelegatingMap<>(trackedItems, ti -> ti.moneyAmount, (ti, v) -> ti.moneyAmount = v);

    // Pre-deserialized messages for performance
    private String rawAmountFormat;
    private String rawOwnerFormat;
    private String rawBundleFormat;

    private boolean isEnabled;
    private boolean usePapi;
    private boolean useWorldGuard;
    private boolean usePacketProvider = false;
    private fr.skynex.lootglow.packets.PacketProvider packetProvider;
    private final Map<Integer, UUID> entityIdMap = new java.util.concurrent.ConcurrentHashMap<>();
    private NamespacedKey farmingKey;
    private NamespacedKey sourceMobKey;
    private boolean useMythic;

    private final Map<UUID, List<UUID>> groupMembers = new HashMap<>();
    private final Map<UUID, UUID> openContainers = new HashMap<>();
    private final Set<UUID> groupedItems = new HashSet<>();
    private final Map<UUID, Integer> groupLeaders = new HashMap<>();

    public boolean isFarmingEnabled() {
        return configManager != null ? configManager.isFarmingEnabled() : false;
    }

    public boolean isGroupingEnabled() {
        return configManager != null ? configManager.isGroupingEnabled() : false;
    }

    public boolean isBobbingEnabled() {
        return configManager != null ? configManager.isBobbingEnabled() : true;
    }

    public double getBobbingAmplitude() {
        return configManager != null ? configManager.getBobbingAmplitude() : 0.05;
    }

    public double getBobbingSpeed() {
        return configManager != null ? configManager.getBobbingSpeed() : 0.08;
    }

    public int getLightColumnHeight() {
        return configManager != null ? configManager.getLightColumnHeight() : 3;
    }

    public NamespacedKey getSourceMobKey() {
        return sourceMobKey;
    }


    public boolean isWorldAllowed(String worldName) {
        return configManager != null ? configManager.isWorldAllowed(worldName) : true;
    }

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
        saveDefaultConfig();
        File configFile = new File(getDataFolder(), "config.yml");
        ConfigUpdater.update(this, "config.yml", configFile);

        initManagersAndServices();
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
        getServer().getServicesManager().register(fr.skynex.lootglow.api.LootGlowAPI.class, apiImpl, this, org.bukkit.plugin.ServicePriority.Normal);

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
            pluginDisableService.onDisable(activeLabels, activeBeams, activeItemVisuals, activeShadows, activeCropSymbols, hiddenVanillaItems, entityIdMap, trackedItems, activeLights, activeItems, getItemsByWorld(), timerComponentCache, getBounceCounts(), getRecentlyBounced(), lastFarmingScanLocations);
        }
    }


    public void debugLog(String message) {
        if (getConfig().getBoolean("settings.debug", false)) {
            getLogger().info("[Debug] " + message);
        }
    }

    public void loadConfiguration() {
        if (glowTeamManager != null) {
            glowTeamManager.clearScoreboardTeams();
        }

        reloadConfig();
        loadMessages();
        resetStateOnReload();

        if (configManager != null) {
            configManager.loadAll(getConfig(), miniMessage, displayNameOverridesCache);
        }
        this.isEnabled = isPluginEnabled();

        setupTeams();

        debugLog("Configuration loaded. Debug mode enabled.");
        debugLog("RPG Drops Enabled: " + isRpgDropsEnabled() + ", Enabled Categories: " + (configManager != null ? configManager.getRpgEnabledCategories() : "[]"));

        if (itemMergeManager != null) {
            itemMergeManager.loadConfig();
        }

        startBackgroundTasks();

        // Re-populate cache and re-apply visuals for existing items on reload if not only-player-drops
        if (!isOnlyPlayerDrops()) {
            for (World world : Bukkit.getWorlds()) {
                for (Item item : world.getEntitiesByClass(Item.class)) {
                    if (item.isValid()) {
                        applyGlow(item, false);
                    }
                }
            }
        }
    }

    private void startParticleTask() {
        if (particleAnimationManager != null) {
            particleAnimationManager.startParticleTask(isPluginEnabled(), configManager.isParticlesEnabled(), configManager.getLodPartDistSq(), activeItems, itemParticlesCache, itemCategoriesCache, hiddenVisuals, categoryDustOptions, defaultDustOptions, categoryAnimTypes, configManager.getParticleAnimType(), configManager.getParticlesFrequency());
        }
    }

    private void startLightingTask() {
        int interval = getConfig().getInt("settings.lighting.update-interval", 5);
        if (lightService != null && configManager != null) {
            lightService.startLightingTask(isPluginEnabled(), configManager.isLightingEnabled(), activeLights, activeItems, itemCategoriesCache, categoryLights, configManager.getCachedLightBlockData(), interval);
        }
    }

    private void loadMessages() {
        if (messageService != null) {
            messageService.loadMessages(timerComponentCache);
            this.rawAmountFormat = messageService.getRawAmountFormat();
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
        if (!useWorldGuard || !configManager.isWgEnabled())
            return false;
        return fr.skynex.lootglow.integration.WorldGuardHook.isInBlockedRegion(loc, configManager.getWgBlockedRegions());
    }

    public void applyGlow(Item item) {
        applyGlow(item, true);
    }

    public void applyGlow(Item item, boolean playAnimation) {
        if (itemGlowApplyService != null && configManager != null) {
            fr.skynex.lootglow.model.ItemGlowContext ctx = new fr.skynex.lootglow.model.ItemGlowContext(
                    isPluginEnabled(), configManager.isEconomyEnabled(), configManager.getEconomyKeys(), configManager.getEconomyColor(), configManager.getEconomySound(),
                    itemMoneyAmounts, itemCategories, categoryNames, configManager.getDefaultColor(), categoryParticles,
                    itemParticlesCache, itemCategoriesCache, configManager.getDespawnTime(), entityIdMap, getActiveItems(), getItemsByWorld(),
                    configManager.isRpgDropsEnabled(), configManager.getRpgEnabledCategories(), configManager.getCategoryGlow(), configManager.isDefaultGlow(), hiddenVanillaItems,
                    categorySounds, configManager.isHoloEnabled(), configManager.isHoloHideUncategorized(), itemSpawnTimes, baseNameCache,
                    configManager.isProtectionEnabled(), configManager.getProtectionDuration(), configManager.isShadowsEnabled(), configManager.isBeamsEnabled(), configManager.getBeamCategories()
            );
            itemGlowApplyService.applyGlow(item, playAnimation, ctx);
        }
    }

    public String getInternalId(ItemStack item) {
        return fr.skynex.lootglow.util.CustomItemIdentifier.getInternalId(item, getConfig().getBoolean("settings.debug", false), getLogger());
    }

    public void playSpawnAnimation(Item item, String id) {
        if (particleAnimationManager != null) {
            particleAnimationManager.playSpawnAnimation(item, id, sourceMobKey, categoryParticles, configManager.getJumpForce(), configManager.getBurstAmount());
        }
    }

    public void updateHologram(Item item, NamedTextColor color) {
        if (hologramService != null && configManager != null) {
            fr.skynex.lootglow.model.HologramContext ctx = new fr.skynex.lootglow.model.HologramContext(
                    configManager.isHoloEnabled(), itemCategoriesCache, configManager.isHoloHideUncategorized(),
                    activeLabels, groupLeaders, lastHoloState, baseNameCache, displayNameOverridesCache,
                    itemMoneyAmounts, configManager.getEconomyFormat(), configManager.getEconomyPrefix(),
                    configManager.isHoloShowAmount(), rawAmountFormat, configManager.isProtectionEnabled(),
                    configManager.getProtectionDuration(), itemSpawnTimes, rawOwnerFormat, usePapi,
                    configManager.isHoloShowTimer(), timerComponentCache, configManager.isHoloTimerNewLine()
            );
            hologramService.updateHologram(item, color, ctx);
        }
    }

    private void startGarbageCollectorTask() {
        if (trackedItemManager != null) {
            trackedItemManager.startGarbageCollectorTask(isPluginEnabled(), getActiveItems());
        }
    }

    public void removeGlow(UUID uuid) {
        if (visualSpawner != null) {
            visualSpawner.removeGlow(uuid);
        }
    }

    private void startLODTask() {
        if (lodManager != null) {
            lodManager.startLODTask(isPluginEnabled(), configManager.isLodEnabled(), configManager.getLodBeamDistSq(), configManager.getLodHoloDistSq(), configManager.getFarmingViewDistance(),
                    visibleEntities, hiddenVisuals, getActiveItems(), groupedItems, activeLabels, activeBeams,
                    activeItemVisuals, activeShadows, getItemsByWorld(), configManager.isFarmingEnabled(), activeCropSymbols, configManager.getLodInterval(), globallyVisibleEntities);
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
        if (itemMagnetManager != null && configManager != null) {
            itemMagnetManager.tickMagnet(configManager.isMagnetEnabled(), configManager.getMagnetDistance(), configManager.getMagnetPermission(), configManager.getMagnetCategories(),
                    configManager.isMagnetEnableForGroups(), groupLeaders, groupMembers, groupedItems, itemCategoriesCache,
                    configManager.isProtectionEnabled(), configManager.getProtectionDuration(), itemSpawnTimes);
        }
    }

    private void tickBeamAnimation(float angle) {
        if (beamTickService != null && beamManager != null && configManager != null) {
            beamTickService.tickBeamAnimation(angle, configManager.isBeamsEnabled(), configManager.isBeamsAnimate(), activeBeams, globallyVisibleEntities,
                    beamManager.getActiveBeamConfigs(), configManager.getBeamHeight(), configManager.getBeamWidth(), itemParticlesCache);
        }
    }

    private void tickFarmingAnimation(float angle) {
        if (farmingManager != null && configManager != null) {
            farmingManager.tickFarmingAnimation(angle, configManager.isFarmingEnabled(), configManager.isFarmingAnimation(), globallyVisibleEntities);
        }
    }


    private void startFarmingTask() {
        if (farmingManager != null) {
            farmingManager.startFarmingTask(isPluginEnabled(), configManager.isFarmingEnabled(), configManager.getFarmingCrops(), configManager.getFarmingViewDistance(), lastFarmingScanLocations);
        }
    }


    private void startGroupingTask() {
        if (itemGroupingService != null && configManager != null) {
            fr.skynex.lootglow.model.ItemGroupingContext ctx = new fr.skynex.lootglow.model.ItemGroupingContext(
                    isPluginEnabled(), configManager.isGroupingEnabled(), trackedItems, activeItems,
                    itemCategoriesCache, groupedItems, groupLeaders, groupMembers, activeItemVisuals,
                    configManager.isUseVisualBag(), configManager.getBagMaterial(), configManager.getBagHeadTexture(),
                    configManager.isUseOwnerHead(), configManager.getBagCustomModelData(), configManager.getRpgRotation(),
                    configManager.isHoloShowTimer(), rawBundleFormat, itemCategories, configManager.getDefaultColor(), miniMessage
            );
            itemGroupingService.startGroupingTask(ctx);
        }
    }

    public void spawnHologram(Item item, NamedTextColor color) {
        if (hologramService != null) {
            hologramService.spawnHologram(item, color, configManager.isHoloEnabled(), itemCategoriesCache, configManager.isHoloHideUncategorized(),
                    activeLabels, configManager.isHoloSeeThrough(), configManager.getHoloViewDistance(), configManager.isHoloBackground(), configManager.getHoloOffset(),
                    baseNameCache, displayNameOverridesCache, itemMoneyAmounts, configManager.getEconomyFormat(), configManager.getEconomyPrefix(),
                    configManager.isHoloShowAmount(), rawAmountFormat, configManager.isProtectionEnabled(), configManager.getProtectionDuration(), itemSpawnTimes,
                    rawOwnerFormat, usePapi, configManager.isHoloShowTimer(), timerComponentCache, configManager.isHoloTimerNewLine(),
                    configManager.getLodHoloDistSq(), hiddenVisuals, visibleEntities);
        }
    }

    public Component calculateBaseName(Item item, NamedTextColor color) {
        return hologramService != null ? hologramService.calculateBaseName(item, color, displayNameOverridesCache, itemMoneyAmounts, configManager.getEconomyFormat(), configManager.getEconomyPrefix()) : Component.empty();
    }

    public Component buildFinalName(Item item, Component baseName) {
        return hologramService != null ? hologramService.buildFinalName(item, baseName, configManager.isHoloShowAmount(), rawAmountFormat, configManager.isProtectionEnabled(), configManager.getProtectionDuration(), itemSpawnTimes, rawOwnerFormat, usePapi, configManager.isHoloShowTimer(), timerComponentCache, configManager.isHoloTimerNewLine()) : baseName;
    }



    public void spawnBeam(Item item, String category, NamedTextColor color) {
        if (beamManager != null) {
            beamManager.spawnBeam(item, category, color, activeBeams, configManager.getBeamHeight(), configManager.getBeamWidth(), configManager.isBeamsAnimate(), configManager.isBeamsUseCategoryColor(), configManager.getLodBeamDistSq(), hiddenVisuals, visibleEntities);
        }
    }

    public Material getColorStainedGlass(NamedTextColor color) {
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
            hologramService.refreshHologram(item, configManager.isHoloEnabled(), configManager.isHoloHideUncategorized(), itemCategoriesCache, itemCategories, configManager.getDefaultColor(), lastHoloState);
        }
    }

    public void clearVisualsForPlayer(Player player) {
        if (visualDisplayManager != null) {
            visualDisplayManager.clearVisualsForPlayer(player, trackedItems);
        }
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
        return configManager != null && configManager.isOnlyPlayerDrops();
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
        if (itemGlowApplyService != null && configManager != null) {
            itemGlowApplyService.preHideItem(item, isPluginEnabled(), isRpgDropsEnabled(), sourceMobKey, itemCategories, categoryNames, configManager.getRpgEnabledCategories(), entityIdMap, hiddenVanillaItems);
        }
    }
    public boolean isHoloEnabled() { return configManager != null && configManager.isHoloEnabled(); }
    public boolean isRpgDropsEnabled() { return configManager != null && configManager.isRpgDropsEnabled(); }
    public boolean isHardLockEnabled() { return configManager != null && configManager.isHardLockEnabled(); }
    public int getProtectionDuration() { return configManager != null ? configManager.getProtectionDuration() : 10; }
    public String getBypassPermission() { return configManager != null ? configManager.getBypassPermission() : "lootglow.bypass.lock"; }
    public boolean isProtocolLibEnabled() { return usePacketProvider; }
    public Map<Integer, UUID> getEntityIdMap() { return entityIdMap; }
    public boolean isRmbPickupEnabled() { return configManager != null && configManager.isRmbPickupEnabled(); }
    public boolean isRmbPickupForce() { return configManager != null && configManager.isRmbPickupForce(); }
    public double getRmbPickupRange() { return configManager != null ? configManager.getRmbPickupRange() : 3.0; }
    public boolean isRmbPickupEnableForGroups() { return configManager != null && configManager.isRmbPickupEnableForGroups(); }
    public Map<UUID, Long> getItemSpawnTimes() { return itemSpawnTimes; }
    public Map<UUID, Component> getBaseNameCache() { return baseNameCache; }
    public Map<UUID, Long> getLastHoloState() { return lastHoloState; }
    public Map<UUID, ItemDisplay> getActiveItemVisuals() { return activeItemVisuals; }
    public Map<UUID, TextDisplay> getActiveLabels() { return activeLabels; }
    public Map<UUID, BlockDisplay> getActiveBeams() { return activeBeams; }
    public Map<UUID, org.bukkit.entity.Display> getActiveShadows() { return activeShadows; }
    public Set<Integer> getHiddenVanillaItems() { return hiddenVanillaItems; }
    public Set<UUID> getHiddenVisuals() { return hiddenVisuals; }
    public boolean isPluginEnabled() { return configManager != null ? configManager.isEnabled() : true; }

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
        if (entityVisibilityService != null && configManager != null) {
            entityVisibilityService.refreshGlowForPlayer(player, showVisuals, hiddenVanillaItems, entityIdMap, visibleEntities, configManager.getFarmingViewDistance(), getActiveItems(), groupedItems, configManager.getLodHoloDistSq(), configManager.getLodBeamDistSq(), activeCropSymbols);
        }
        for (Item item : getActiveItems().values()) {
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
        CropSymbol parts = activeCropSymbols.computeIfAbsent(block, k -> new CropSymbol(block.getLocation().add(0.5, configManager != null ? configManager.getFarmingOffset() : 0.0, 0.5)));
        if (!parts.contains(bd)) {
            parts.add(bd);
        }
        updateCropSymbolVisibilityForWorld(parts);
    }

    public void updateCropSymbolVisibilityForWorld(CropSymbol cs) {
        if (cs == null || cs.isEmpty() || configManager == null) return;
        double farmDistSq = configManager.getFarmingViewDistance() * configManager.getFarmingViewDistance();
        Location loc = cs.location;
        World world = loc.getWorld();
        if (world == null) return;

        for (Player p : world.getPlayers()) {
            UUID pUuid = p.getUniqueId();
            boolean isHiddenToggle = isHiddenToggleFor(p);
            boolean shouldSee = !isHiddenToggle && p.getLocation().distanceSquared(loc) <= farmDistSq;
            Set<UUID> visibleSet = visibleEntities.computeIfAbsent(pUuid, k -> java.util.concurrent.ConcurrentHashMap.newKeySet());

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
    public boolean isWgEnabled() { return configManager != null && configManager.isWgEnabled(); }
    public Material getFarmingMaterial() { return configManager != null ? configManager.getFarmingMaterial() : Material.AIR; }
    public double getFarmingOffset() { return configManager != null ? configManager.getFarmingOffset() : 0.0; }
    public float getFarmingScale() { return configManager != null ? configManager.getFarmingScale() : 1.0f; }
    public NamedTextColor getFarmingGlowColor() { return configManager != null ? configManager.getFarmingGlowColor() : NamedTextColor.GREEN; }
    public float getShadowScale() { return configManager != null ? configManager.getShadowScale() : 1.0f; }
    public double getLodHoloDistSq() { return configManager != null ? configManager.getLodHoloDistSq() : 0.0; }
    public Map<UUID, Set<UUID>> getVisibleEntities() { return visibleEntities; }
    public void setGloballyVisibleEntities(Set<UUID> set) { this.globallyVisibleEntities = set; }


    public Set<Material> getFarmingCrops() {
        return configManager != null ? configManager.getFarmingCrops() : Collections.emptySet();
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
        if (itemVisualSpawnService != null && configManager != null) {
            fr.skynex.lootglow.model.ItemVisualContext ctx = new fr.skynex.lootglow.model.ItemVisualContext(
                    configManager.isUseVisualBag(), configManager.isRpgDropsEnabled(), groupLeaders,
                    activeItemVisuals, entityIdMap, new java.util.HashSet<>(configManager.getRpgEnabledCategories()),
                    hiddenVisuals, visibleEntities, configManager.getCategoryGlow(), configManager.isDefaultGlow(),
                    configManager.getBagMaterial(), configManager.getBagHeadTexture(), configManager.isUseOwnerHead(), configManager.getBagCustomModelData(),
                    configManager.getRpgItemScale(), configManager.getRpgBlockScale(), configManager.getRpgRotation()
            );
            itemVisualSpawnService.spawnItemVisual(item, category, color, ctx);
        }
    }

    /** Global sync tick: repositions all Display entities to follow their parent Item. Runs every tick. */
    private void tickGlobalSync() {
        if (lootRenderPipeline != null) {
            lootRenderPipeline.tickSync();
        } else if (itemPhysicsService != null && configManager != null) {
            itemPhysicsService.tickGlobalSync(isPluginEnabled(), getActiveItems(), trackedItems, configManager.getRpgBlockScale(), configManager.getRpgItemScale(), configManager.getBagMaterial(), groupLeaders, configManager.getHoloOffset(), configManager.getShadowScale(), configManager.getRpgRotation());
        }
    }

    /** Bouncing tick: applies bounce physics to items. Runs every tick. */
    private void tickBouncing() {
        if (rpgDropManager != null && configManager != null) {
            rpgDropManager.tickBouncing(configManager.isBouncingEnabled(), getActiveItems(), configManager.getBouncingBlockedBlocks(), configManager.isBouncingOnlyOnSnow(), configManager.getMaxBounces(), configManager.getJumpForce(), configManager.getBounceDamping());
        }
    }



    public void playAspirationAnimation(Item item, Player player) {
        if (rpgDropManager != null && configManager != null) {
            rpgDropManager.playAspirationAnimation(item, player, activeItemVisuals, configManager.isAspirationEnabled());
        }
    }

    /** Aspiration animation tick: flies item visuals towards the collecting player. Runs every tick. */
    private void tickAspiration() {
        if (rpgDropManager != null && configManager != null) {
            rpgDropManager.tickAspiration(configManager.isAspirationEnabled(), configManager.getAspirationSpeed());
        }
    }


    public void openLootContainer(Player player, UUID leaderUuid) {
        if (groupContainerManager != null && configManager != null) {
            groupContainerManager.openLootContainer(player, leaderUuid, configManager.isContainerEnabled(), configManager.getContainerTitle(), activeItemVisuals, configManager.getRpgBlockScale(), miniMessage);
        }
    }

    public Map<UUID, UUID> getOpenContainers() {
        return groupContainerManager != null ? groupContainerManager.getOpenContainers() : openContainers;
    }

    public Map<UUID, List<UUID>> getGroupMembers() {
        return groupContainerManager != null ? groupContainerManager.getGroupMembers() : groupMembers;
    }

    public Map<UUID, Integer> getGroupLeaders() {
        return groupLeaders;
    }

    public Map<UUID, Item> getActiveItems() {
        return trackedItemManager != null ? trackedItemManager.getActiveItems() : activeItems;
    }

    public Set<UUID> getGroupedItems() {
        return groupContainerManager != null ? groupContainerManager.getGroupedItems() : groupedItems;
    }

    public boolean isContainerEnabled() {
        return configManager != null && configManager.isContainerEnabled();
    }

    public boolean isContainerRequireClick() {
        return configManager != null && configManager.isContainerRequireClick();
    }

    public UUID getGroupLeader(UUID itemUuid) {
        return groupContainerManager != null ? groupContainerManager.getGroupLeader(itemUuid) : null;
    }

    public ItemStack createTexturedHead(String textureInput) {
        return visualDisplayManager != null ? visualDisplayManager.createTexturedHead(textureInput) : new ItemStack(Material.PLAYER_HEAD);
    }

    public String getBase64Texture(String input) {
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
        return fr.skynex.lootglow.util.ItemTypeClassifier.isFlatItemOrBlock(mat, configManager != null ? configManager.getRpgForceFlatMaterials() : Collections.emptySet(), configManager != null ? configManager.getRpgForceUprightMaterials() : Collections.emptySet());
    }

    public boolean isUprightItem(Material mat) {
        return fr.skynex.lootglow.util.ItemTypeClassifier.isUprightItem(mat, configManager != null ? configManager.getRpgForceFlatMaterials() : Collections.emptySet(), configManager != null ? configManager.getRpgForceUprightMaterials() : Collections.emptySet());
    }

    public boolean isFishItem(Material mat) {
        return fr.skynex.lootglow.util.ItemTypeClassifier.isFishItem(mat);
    }

    public boolean isCustomItem(ItemStack stack) {
        return fr.skynex.lootglow.util.ItemTypeClassifier.isCustomItem(stack);
    }

    public Double getMoneyAmount(ItemStack stack) {
        return fr.skynex.lootglow.util.MoneyAmountParser.getMoneyAmount(stack, configManager != null && configManager.isEconomyEnabled(), configManager != null ? configManager.getEconomyKeys() : Collections.emptyList());
    }

    public NamedTextColor parseNamedColor(String input) {
        return configParser != null ? configParser.parseNamedColor(input) : NamedTextColor.WHITE;
    }

    public Sound parseSound(String input) {
        return configParser != null ? configParser.parseSound(input) : null;
    }

    public void updateSurfaceAlignment(Item item) {
        if (surfaceAlignmentManager != null) {
            surfaceAlignmentManager.updateSurfaceAlignment(item, getRecentlyBounced());
        }
    }

    // ==========================================
    //            LootGlowAPI Implementation
    // ==========================================

    private final fr.skynex.lootglow.api.impl.LootGlowAPIImpl apiImpl = new fr.skynex.lootglow.api.impl.LootGlowAPIImpl(this);

    public fr.skynex.lootglow.api.impl.LootGlowAPIImpl getApiImpl() {
        return apiImpl;
    }

    @Override public void setGlowColor(@NotNull Item item, @NotNull Color color) { apiImpl.setGlowColor(item, color); }
    @Override public void setGlowColor(@NotNull Item item, @NotNull Color color, @NotNull Player player) { apiImpl.setGlowColor(item, color, player); }
    @Override public void resetGlowColor(@NotNull Item item) { apiImpl.resetGlowColor(item); }
    @Override public void resetGlowColor(@NotNull Item item, @NotNull Player player) { apiImpl.resetGlowColor(item, player); }
    @Override public void setCustomHologram(@NotNull Item item, @Nullable String text) { apiImpl.setCustomHologram(item, text); }
    @Override public void setCustomHologram(@NotNull Item item, @Nullable String text, @NotNull Player player) { apiImpl.setCustomHologram(item, text, player); }
    @Override public void setBeaconBeam(@NotNull Item item, boolean enabled) { apiImpl.setBeaconBeam(item, enabled); }
    @Override public void setBeaconBeam(@NotNull Item item, boolean enabled, @Nullable Color color) { apiImpl.setBeaconBeam(item, enabled, color); }
    @Override public void setLootProtection(@NotNull Item item, @NotNull UUID ownerUuid, long durationSeconds) { apiImpl.setLootProtection(item, ownerUuid, durationSeconds); }
    @Override public boolean isLootProtected(@NotNull Item item) { return apiImpl.isLootProtected(item); }
    @Override public boolean isPlayerAllowedToPickup(@NotNull Player player, @NotNull Item item) { return apiImpl.isPlayerAllowedToPickup(player, item); }
    @Override public UUID getLootOwner(@NotNull Item item) { return apiImpl.getLootOwner(item); }
    @Override public boolean isMagnetEnabled(@NotNull Player player) { return apiImpl.isMagnetEnabled(player); }
    @Override public void setMagnetEnabled(@NotNull Player player, boolean enabled) { apiImpl.setMagnetEnabled(player, enabled); }
    @Override public void pullItemsToPlayer(@NotNull Player player, double radius) { apiImpl.pullItemsToPlayer(player, radius); }
    @Override public boolean isVisualsHidden(@NotNull Player player) { return apiImpl.isVisualsHidden(player); }
    @Override public void setVisualsHidden(@NotNull Player player, boolean hidden) { apiImpl.setVisualsHidden(player, hidden); }
    @Override public boolean hasLineOfSight(@NotNull Player player, @NotNull Item item, double maxDistance) { return apiImpl.hasLineOfSight(player, item, maxDistance); }
    @Override public boolean updateOcclusionVisibility(@NotNull Player player, @NotNull Item item, double maxDistance) { return apiImpl.updateOcclusionVisibility(player, item, maxDistance); }
    @Override public void setParticleEffect(@NotNull Item item, @Nullable Particle particle) { apiImpl.setParticleEffect(item, particle); }
    @Override public void clearParticleEffect(@NotNull Item item) { apiImpl.clearParticleEffect(item); }
    @Override public void setDropSound(@NotNull Item item, @Nullable Sound sound, float volume, float pitch) { apiImpl.setDropSound(item, sound, volume, pitch); }
    @Override public void triggerPopAnimation(@NotNull Item item, double jumpVelocity) { apiImpl.triggerPopAnimation(item, jumpVelocity); }
    @Override public void setBouncingEnabled(@NotNull Item item, boolean bouncing) { apiImpl.setBouncingEnabled(item, bouncing); }
    @Override public void setCropHighlight(@NotNull Block cropBlock, boolean highlight) { apiImpl.setCropHighlight(cropBlock, highlight); }
    @Override public boolean isCropHighlighted(@NotNull Block cropBlock) { return apiImpl.isCropHighlighted(cropBlock); }
    @Override public void setItemCategory(@NotNull Item item, @NotNull String category) { apiImpl.setItemCategory(item, category); }
    @Nullable @Override public String getItemCategory(@NotNull Item item) { return apiImpl.getItemCategory(item); }
    @NotNull @Override public List<Item> getNearbyGlowingItems(@NotNull Location location, double radius) { return apiImpl.getNearbyGlowingItems(location, radius); }
    @NotNull @Override public Item spawnGlowItem(@NotNull Location location, @NotNull ItemStack itemStack, @Nullable String category) { return apiImpl.spawnGlowItem(location, itemStack, category); }
    @Override public void refreshVisuals(@NotNull Item item, @NotNull Player player) { apiImpl.refreshVisuals(item, player); }
    @Override public boolean isTracked(@NotNull Item item) { return apiImpl.isTracked(item); }
    @NotNull @Override public List<Item> getTrackedItemsInChunk(@NotNull Chunk chunk) { return apiImpl.getTrackedItemsInChunk(chunk); }
    @Override public void addLootSharer(@NotNull Item item, @NotNull UUID playerUuid) { apiImpl.addLootSharer(item, playerUuid); }
    @Override public void removeLootSharer(@NotNull Item item, @NotNull UUID playerUuid) { apiImpl.removeLootSharer(item, playerUuid); }
    @Override public void removeCustomHologram(@NotNull Item item) { apiImpl.removeCustomHologram(item); }
    @Override public void removeCustomHologram(@NotNull Item item, @NotNull Player player) { apiImpl.removeCustomHologram(item, player); }
    @Override public void resetLootProtection(@NotNull Item item) { apiImpl.resetLootProtection(item); }
    @NotNull @Override public Set<UUID> getLootSharers(@NotNull Item item) { return apiImpl.getLootSharers(item); }
    @NotNull @Override public String detectItemRarity(@NotNull ItemStack itemStack) { return apiImpl.detectItemRarity(itemStack); }
    @NotNull @Override public String detectItemRarity(@NotNull Item item) { return apiImpl.detectItemRarity(item); }
    @Override public boolean canMerge(@NotNull Item item1, @NotNull Item item2) { return apiImpl.canMerge(item1, item2); }
    @Override public boolean mergeAmount(@NotNull Item item1, @NotNull Item item2) { return apiImpl.mergeAmount(item1, item2); }
    @Override public boolean unMergeAmount(@NotNull Item item, int amount) { return apiImpl.unMergeAmount(item, amount); }
    @Override public int getMergeAmount(@NotNull Item item) { return apiImpl.getMergeAmount(item); }
    @Override public void setMergeAmount(@NotNull Item item, int amount) { apiImpl.setMergeAmount(item, amount); }
    @Override public void addMergeAmount(@NotNull Item item, int amount) { apiImpl.addMergeAmount(item, amount); }
    @Override public void removeMergeAmount(@NotNull Item item, int amount) { apiImpl.removeMergeAmount(item, amount); }

    private void initManagersAndServices() {
        this.serviceRegistry = new fr.skynex.lootglow.registry.ServiceRegistry();
        this.spatialIndexService = new fr.skynex.lootglow.spatial.LootSpatialIndexService(this);
        this.lootEventDispatcher = new fr.skynex.lootglow.event.LootEventDispatcher(this);

        this.databaseManager = new DatabaseManager(this);
        this.trackedItemManager = new TrackedItemManager(this, trackedItems, activeItems, entityIdMap, globallyVisibleEntities);
        this.beamManager = new BeamManager(this);
        this.hologramManager = new HologramManager(this);
        this.configManager = new LootGlowConfigManager(this);
        this.commandManager = new LootGlowCommandManager(this);
        this.farmingManager = new fr.skynex.lootglow.managers.FarmingManager(this);
        this.rpgDropManager = new fr.skynex.lootglow.managers.RPGDropManager(this);
        this.particleAnimationManager = new fr.skynex.lootglow.managers.ParticleAnimationManager(this);
        this.groupContainerManager = new fr.skynex.lootglow.managers.GroupContainerManager(this);
        this.lootProtectionManager = new fr.skynex.lootglow.managers.LootProtectionManager(this);
        this.itemMergeManager = new fr.skynex.lootglow.managers.ItemMergeManager(this);
        this.itemMergeManager.loadConfig();
        this.occlusionManager = new fr.skynex.lootglow.managers.OcclusionManager();
        this.glowManager = new fr.skynex.lootglow.managers.GlowManager();
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
        this.itemNameFormatter = new fr.skynex.lootglow.util.ItemNameFormatter();
        this.lootWorldManager = new fr.skynex.lootglow.managers.LootWorldManager(this);
        this.vanillaItemVisibilityManager = new fr.skynex.lootglow.managers.VanillaItemVisibilityManager(this);
        this.rarityManager = new fr.skynex.lootglow.managers.RarityManager(this);
        this.groundAuraManager = new fr.skynex.lootglow.managers.GroundAuraManager(this);
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
        this.pluginLifecycleManager = new fr.skynex.lootglow.managers.PluginLifecycleManager(this);
        this.lootRenderPipeline = new fr.skynex.lootglow.pipeline.LootRenderPipeline(this);

        this.serviceRegistry.registerService(fr.skynex.lootglow.spatial.LootSpatialIndexService.class, spatialIndexService);
        this.serviceRegistry.registerService(fr.skynex.lootglow.event.LootEventDispatcher.class, lootEventDispatcher);
        this.serviceRegistry.registerService(fr.skynex.lootglow.pipeline.LootRenderPipeline.class, lootRenderPipeline);
        this.serviceRegistry.registerService(fr.skynex.lootglow.service.ItemGlowApplyService.class, itemGlowApplyService);
        this.serviceRegistry.registerService(fr.skynex.lootglow.service.ItemPhysicsService.class, itemPhysicsService);
        this.serviceRegistry.registerService(fr.skynex.lootglow.service.HologramService.class, hologramService);
        this.serviceRegistry.registerService(fr.skynex.lootglow.service.ItemGroupingService.class, itemGroupingService);
        this.serviceRegistry.registerService(fr.skynex.lootglow.service.EntityVisibilityService.class, entityVisibilityService);
        this.serviceRegistry.registerService(fr.skynex.lootglow.managers.ItemMagnetManager.class, itemMagnetManager);
    }

    private void resetStateOnReload() {
        if (pluginLifecycleManager != null) {
            pluginLifecycleManager.resetStateOnReload();
        }
    }

    private void registerListeners() {
        if (pluginLifecycleManager != null) {
            pluginLifecycleManager.registerListeners(useMythic);
        }
    }

    private void registerCommands() {
        if (pluginLifecycleManager != null) {
            pluginLifecycleManager.registerCommands();
        }
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
