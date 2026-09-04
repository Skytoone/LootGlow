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

    private final fr.skynex.lootglow.state.LootStateRepository stateRepository = new fr.skynex.lootglow.state.LootStateRepository();
    private fr.skynex.lootglow.registry.ServiceRegistry serviceRegistry = new fr.skynex.lootglow.registry.ServiceRegistry();

    public <T> T getService(Class<T> clazz) {
        return serviceRegistry.get(clazz);
    }

    public fr.skynex.lootglow.registry.ServiceRegistry getServiceRegistry() { return serviceRegistry; }
    public fr.skynex.lootglow.state.LootStateRepository getStateRepository() { return stateRepository; }
    public fr.skynex.lootglow.spatial.LootSpatialIndexService getSpatialIndexService() { return getService(fr.skynex.lootglow.spatial.LootSpatialIndexService.class); }
    public fr.skynex.lootglow.event.LootEventDispatcher getLootEventDispatcher() { return getService(fr.skynex.lootglow.event.LootEventDispatcher.class); }
    public fr.skynex.lootglow.pipeline.LootRenderPipeline getLootRenderPipeline() { return getService(fr.skynex.lootglow.pipeline.LootRenderPipeline.class); }
    public fr.skynex.lootglow.managers.RarityManager getRarityManager() { return getService(fr.skynex.lootglow.managers.RarityManager.class); }
    public fr.skynex.lootglow.managers.PluginLifecycleManager getPluginLifecycleManager() { return getService(fr.skynex.lootglow.managers.PluginLifecycleManager.class); }

    public DatabaseManager getDatabaseManager() { return getService(DatabaseManager.class); }
    public TrackedItemManager getTrackedItemManager() { return getService(TrackedItemManager.class); }
    public BeamManager getBeamManager() { return getService(BeamManager.class); }
    public HologramManager getHologramManager() { return getService(HologramManager.class); }
    public LootGlowConfigManager getConfigManager() { return getService(LootGlowConfigManager.class); }
    public LootGlowCommandManager getCommandManager() { return getService(LootGlowCommandManager.class); }
    public fr.skynex.lootglow.managers.FarmingManager getFarmingManager() { return getService(fr.skynex.lootglow.managers.FarmingManager.class); }
    public fr.skynex.lootglow.managers.RPGDropManager getRpgDropManager() { return getService(fr.skynex.lootglow.managers.RPGDropManager.class); }
    public fr.skynex.lootglow.managers.ParticleAnimationManager getParticleAnimationManager() { return getService(fr.skynex.lootglow.managers.ParticleAnimationManager.class); }
    public fr.skynex.lootglow.managers.GroupContainerManager getGroupContainerManager() { return getService(fr.skynex.lootglow.managers.GroupContainerManager.class); }
    public fr.skynex.lootglow.managers.LootProtectionManager getLootProtectionManager() { return getService(fr.skynex.lootglow.managers.LootProtectionManager.class); }
    public fr.skynex.lootglow.managers.ItemMergeManager getItemMergeManager() { return getService(fr.skynex.lootglow.managers.ItemMergeManager.class); }
    public fr.skynex.lootglow.managers.OcclusionManager getOcclusionManager() { return getService(fr.skynex.lootglow.managers.OcclusionManager.class); }
    public fr.skynex.lootglow.managers.GlowManager getGlowManager() { return getService(fr.skynex.lootglow.managers.GlowManager.class); }
    public fr.skynex.lootglow.managers.ItemMagnetManager getItemMagnetManager() { return getService(fr.skynex.lootglow.managers.ItemMagnetManager.class); }
    public fr.skynex.lootglow.managers.EconomyDropManager getEconomyDropManager() { return getService(fr.skynex.lootglow.managers.EconomyDropManager.class); }
    public fr.skynex.lootglow.managers.HologramRenderer getHologramRenderer() { return getService(fr.skynex.lootglow.managers.HologramRenderer.class); }
    public fr.skynex.lootglow.managers.SurfaceAlignmentManager getSurfaceAlignmentManager() { return getService(fr.skynex.lootglow.managers.SurfaceAlignmentManager.class); }
    public fr.skynex.lootglow.managers.GlowTeamManager getGlowTeamManager() { return getService(fr.skynex.lootglow.managers.GlowTeamManager.class); }
    public fr.skynex.lootglow.managers.VisualDisplayManager getVisualDisplayManager() { return getService(fr.skynex.lootglow.managers.VisualDisplayManager.class); }
    public fr.skynex.lootglow.managers.PluginTickManager getPluginTickManager() { return getService(fr.skynex.lootglow.managers.PluginTickManager.class); }
    public fr.skynex.lootglow.managers.VisualSpawner getVisualSpawner() { return getService(fr.skynex.lootglow.managers.VisualSpawner.class); }
    public fr.skynex.lootglow.config.ConfigParser getConfigParser() { return getService(fr.skynex.lootglow.config.ConfigParser.class); }
    public fr.skynex.lootglow.integration.IntegrationManager getIntegrationManager() { return getService(fr.skynex.lootglow.integration.IntegrationManager.class); }
    public fr.skynex.lootglow.managers.PlayerSettingsManager getPlayerSettingsManager() { return getService(fr.skynex.lootglow.managers.PlayerSettingsManager.class); }
    public fr.skynex.lootglow.managers.VisibilityPacketManager getVisibilityPacketManager() { return getService(fr.skynex.lootglow.managers.VisibilityPacketManager.class); }
    public fr.skynex.lootglow.managers.LODManager getLodManager() { return getService(fr.skynex.lootglow.managers.LODManager.class); }
    public fr.skynex.lootglow.util.ItemNameFormatter getItemNameFormatter() { return getService(fr.skynex.lootglow.util.ItemNameFormatter.class); }
    public fr.skynex.lootglow.managers.LootWorldManager getLootWorldManager() { return getService(fr.skynex.lootglow.managers.LootWorldManager.class); }
    public fr.skynex.lootglow.managers.VanillaItemVisibilityManager getVanillaItemVisibilityManager() { return getService(fr.skynex.lootglow.managers.VanillaItemVisibilityManager.class); }
    public fr.skynex.lootglow.managers.GroundAuraManager getGroundAuraManager() { return getService(fr.skynex.lootglow.managers.GroundAuraManager.class); }
    public fr.skynex.lootglow.service.HologramTickService getHologramTickService() { return getService(fr.skynex.lootglow.service.HologramTickService.class); }
    public fr.skynex.lootglow.service.BeamTickService getBeamTickService() { return getService(fr.skynex.lootglow.service.BeamTickService.class); }
    public fr.skynex.lootglow.service.ItemRotationService getItemRotationService() { return getService(fr.skynex.lootglow.service.ItemRotationService.class); }
    public fr.skynex.lootglow.service.EntityVisibilityService getEntityVisibilityService() { return getService(fr.skynex.lootglow.service.EntityVisibilityService.class); }
    public fr.skynex.lootglow.service.ItemVisualSpawnService getItemVisualSpawnService() { return getService(fr.skynex.lootglow.service.ItemVisualSpawnService.class); }
    public fr.skynex.lootglow.service.ItemGroupingService getItemGroupingService() { return getService(fr.skynex.lootglow.service.ItemGroupingService.class); }
    public fr.skynex.lootglow.service.HologramService getHologramService() { return getService(fr.skynex.lootglow.service.HologramService.class); }
    public fr.skynex.lootglow.service.PluginDisableService getPluginDisableService() { return getService(fr.skynex.lootglow.service.PluginDisableService.class); }
    public fr.skynex.lootglow.service.MessageService getMessageService() { return getService(fr.skynex.lootglow.service.MessageService.class); }
    public fr.skynex.lootglow.service.LightService getLightService() { return getService(fr.skynex.lootglow.service.LightService.class); }
    public fr.skynex.lootglow.service.ItemGlowApplyService getItemGlowApplyService() { return getService(fr.skynex.lootglow.service.ItemGlowApplyService.class); }
    public fr.skynex.lootglow.service.ItemPhysicsService getItemPhysicsService() { return getService(fr.skynex.lootglow.service.ItemPhysicsService.class); }

    public Map<String, Component> getDisplayNameOverridesCache() { return stateRepository.getDisplayNameOverridesCache(); }
    public String getEconomyFormat() { return getConfigManager() != null ? getConfigManager().getEconomyFormat() : ""; }
    public String getEconomyPrefix() { return getConfigManager() != null ? getConfigManager().getEconomyPrefix() : ""; }
    public Map<UUID, Particle> getItemParticlesCache() { return stateRepository.getItemParticlesCache(); }
    public Set<UUID> getRecentlyBounced() { return getRpgDropManager() != null ? getRpgDropManager().getRecentlyBounced() : Collections.emptySet(); }
    public double getFarmingViewDistance() { return getConfigManager() != null ? getConfigManager().getFarmingViewDistance() : 0; }
    public double getLodBeamDistSq() { return getConfigManager() != null ? getConfigManager().getLodBeamDistSq() : 0; }
    public Map<org.bukkit.block.Block, CropSymbol> getActiveCropSymbols() { return stateRepository.getActiveCropSymbols(); }
    public Map<UUID, TrackedItem> getTrackedItems() { return stateRepository.getTrackedItems(); }
    public Map<String, Set<UUID>> getItemsByWorld() { return getTrackedItemManager() != null ? getTrackedItemManager().getItemsByWorld() : Collections.emptyMap(); }
    public Map<String, NamedTextColor> getItemCategories() { return stateRepository.getItemCategories(); }
    public Map<String, Particle> getCategoryParticles() { return stateRepository.getCategoryParticles(); }
    public Map<String, Sound> getCategorySounds() { return stateRepository.getCategorySounds(); }
    public Map<String, String> getCategoryNames() { return stateRepository.getCategoryNames(); }
    public Map<String, NamedTextColor> getCategoryColors() { return stateRepository.getCategoryColors(); }
    public Map<String, Integer> getCategoryLights() { return stateRepository.getCategoryLights(); }
    public Map<UUID, Location> getActiveLights() { return stateRepository.getActiveLights(); }
    public Map<String, org.bukkit.Particle.DustOptions> getCategoryDustOptions() { return stateRepository.getCategoryDustOptions(); }
    public Set<UUID> getGloballyVisibleEntities() { return stateRepository.getGloballyVisibleEntities(); }
    public Map<UUID, Integer> getBounceCounts() { return getRpgDropManager() != null ? getRpgDropManager().getBounceCounts() : Collections.emptyMap(); }
    public Map<UUID, String> getItemCategoriesCache() { return stateRepository.getItemCategoriesCache(); }

    private boolean usePapi;
    private boolean useWorldGuard;
    private boolean usePacketProvider = false;
    private fr.skynex.lootglow.packets.PacketProvider packetProvider;
    private NamespacedKey farmingKey;
    private NamespacedKey sourceMobKey;
    private boolean useMythic;

    public boolean isFarmingEnabled() {
        return getConfigManager() != null ? getConfigManager().isFarmingEnabled() : false;
    }

    public boolean isGroupingEnabled() {
        return getConfigManager() != null ? getConfigManager().isGroupingEnabled() : false;
    }

    public boolean isBobbingEnabled() {
        return getConfigManager() != null ? getConfigManager().isBobbingEnabled() : true;
    }

    public double getBobbingAmplitude() {
        return getConfigManager() != null ? getConfigManager().getBobbingAmplitude() : 0.05;
    }

    public double getBobbingSpeed() {
        return getConfigManager() != null ? getConfigManager().getBobbingSpeed() : 0.08;
    }

    public int getLightColumnHeight() {
        return getConfigManager() != null ? getConfigManager().getLightColumnHeight() : 3;
    }

    public NamespacedKey getSourceMobKey() {
        return sourceMobKey;
    }

    public boolean isWorldAllowed(String worldName) {
        return getConfigManager() != null ? getConfigManager().isWorldAllowed(worldName) : true;
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
        closeDatabase();
        if (getPluginDisableService() != null) {
            getPluginDisableService().onDisable(
                    stateRepository.getActiveLabels(), stateRepository.getActiveBeams(), stateRepository.getActiveItemVisuals(),
                    stateRepository.getActiveShadows(), stateRepository.getActiveCropSymbols(), stateRepository.getHiddenVanillaItems(),
                    stateRepository.getEntityIdMap(), stateRepository.getTrackedItems(), stateRepository.getActiveLights(),
                    stateRepository.getActiveItems(), getItemsByWorld(), stateRepository.getTimerComponentCache(),
                    getBounceCounts(), getRecentlyBounced(), stateRepository.getLastFarmingScanLocations()
            );
        }
    }

    public void debugLog(String message) {
        if (getConfig().getBoolean("settings.debug", false)) {
            getLogger().info("[Debug] " + message);
        }
    }

    public void loadConfiguration() {
        if (getGlowTeamManager() != null) {
            getGlowTeamManager().clearScoreboardTeams();
        }

        reloadConfig();
        loadMessages();
        resetStateOnReload();

        if (getConfigManager() != null) {
            getConfigManager().loadAll(getConfig(), miniMessage, stateRepository.getDisplayNameOverridesCache());
        }

        setupTeams();

        debugLog("Configuration loaded. Debug mode enabled.");
        debugLog("RPG Drops Enabled: " + isRpgDropsEnabled() + ", Enabled Categories: " + (getConfigManager() != null ? getConfigManager().getRpgEnabledCategories() : "[]"));

        if (getItemMergeManager() != null) {
            getItemMergeManager().loadConfig();
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
        if (getParticleAnimationManager() != null && getConfigManager() != null) {
            getParticleAnimationManager().startParticleTask(isPluginEnabled(), getConfigManager().isParticlesEnabled(), getConfigManager().getLodPartDistSq(), stateRepository.getActiveItems(), stateRepository.getItemParticlesCache(), stateRepository.getItemCategoriesCache(), stateRepository.getHiddenVisuals(), stateRepository.getCategoryDustOptions(), stateRepository.getDefaultDustOptions(), stateRepository.getCategoryAnimTypes(), getConfigManager().getParticleAnimType(), getConfigManager().getParticlesFrequency());
        }
    }

    private void startLightingTask() {
        int interval = getConfig().getInt("settings.lighting.update-interval", 5);
        if (getLightService() != null && getConfigManager() != null) {
            getLightService().startLightingTask(isPluginEnabled(), getConfigManager().isLightingEnabled(), stateRepository.getActiveLights(), stateRepository.getActiveItems(), stateRepository.getItemCategoriesCache(), stateRepository.getCategoryLights(), getConfigManager().getCachedLightBlockData(), interval);
        }
    }

    private void loadMessages() {
        if (getMessageService() != null) {
            getMessageService().loadMessages(stateRepository.getTimerComponentCache());
            stateRepository.setRawAmountFormat(getMessageService().getRawAmountFormat());
            stateRepository.setRawOwnerFormat(getMessageService().getRawOwnerFormat());
            stateRepository.setRawBundleFormat(getMessageService().getRawBundleFormat());
        }
    }

    public void sendMessage(CommandSender sender, String key) {
        if (getMessageService() != null) {
            getMessageService().sendMessage(sender, key);
        }
    }

    public void sendMessage(CommandSender sender, String key, @Nullable Map<String, String> placeholders) {
        if (getMessageService() != null) {
            getMessageService().sendMessage(sender, key, placeholders);
        }
    }

    private void setupTeams() {
        if (getGlowTeamManager() != null) {
            getGlowTeamManager().setupTeams();
        }
    }

    public boolean isInBlockedRegion(Location loc) {
        if (!useWorldGuard || getConfigManager() == null || !getConfigManager().isWgEnabled())
            return false;
        return fr.skynex.lootglow.integration.WorldGuardHook.isInBlockedRegion(loc, getConfigManager().getWgBlockedRegions());
    }

    public void applyGlow(Item item) {
        applyGlow(item, true);
    }

    public void applyGlow(Item item, boolean playAnimation) {
        if (getItemGlowApplyService() != null && getConfigManager() != null) {
            fr.skynex.lootglow.model.ItemGlowContext ctx = new fr.skynex.lootglow.model.ItemGlowContext(
                    isPluginEnabled(), getConfigManager().isEconomyEnabled(), getConfigManager().getEconomyKeys(), getConfigManager().getEconomyColor(), getConfigManager().getEconomySound(),
                    stateRepository.getItemMoneyAmounts(), stateRepository.getItemCategories(), stateRepository.getCategoryNames(), getConfigManager().getDefaultColor(), stateRepository.getCategoryParticles(),
                    stateRepository.getItemParticlesCache(), stateRepository.getItemCategoriesCache(), getConfigManager().getDespawnTime(), stateRepository.getEntityIdMap(), getActiveItems(), getItemsByWorld(),
                    getConfigManager().isRpgDropsEnabled(), getConfigManager().getRpgEnabledCategories(), getConfigManager().getCategoryGlow(), getConfigManager().isDefaultGlow(), stateRepository.getHiddenVanillaItems(),
                    stateRepository.getCategorySounds(), getConfigManager().isHoloEnabled(), getConfigManager().isHoloHideUncategorized(), stateRepository.getItemSpawnTimes(), stateRepository.getBaseNameCache(),
                    getConfigManager().isProtectionEnabled(), getConfigManager().getProtectionDuration(), getConfigManager().isShadowsEnabled(), getConfigManager().isBeamsEnabled(), getConfigManager().getBeamCategories()
            );
            getItemGlowApplyService().applyGlow(item, playAnimation, ctx);
        }
    }

    public String getInternalId(ItemStack item) {
        return fr.skynex.lootglow.util.CustomItemIdentifier.getInternalId(item, getConfig().getBoolean("settings.debug", false), getLogger());
    }

    public void playSpawnAnimation(Item item, String id) {
        if (getParticleAnimationManager() != null && getConfigManager() != null) {
            getParticleAnimationManager().playSpawnAnimation(item, id, sourceMobKey, stateRepository.getCategoryParticles(), getConfigManager().getJumpForce(), getConfigManager().getBurstAmount());
        }
    }

    public void updateHologram(Item item, NamedTextColor color) {
        if (getHologramService() != null && getConfigManager() != null) {
            fr.skynex.lootglow.model.HologramContext ctx = new fr.skynex.lootglow.model.HologramContext(
                    getConfigManager().isHoloEnabled(), stateRepository.getItemCategoriesCache(), getConfigManager().isHoloHideUncategorized(),
                    stateRepository.getActiveLabels(), stateRepository.getGroupLeaders(), stateRepository.getLastHoloState(), stateRepository.getBaseNameCache(), stateRepository.getDisplayNameOverridesCache(),
                    stateRepository.getItemMoneyAmounts(), getConfigManager().getEconomyFormat(), getConfigManager().getEconomyPrefix(),
                    getConfigManager().isHoloShowAmount(), stateRepository.getRawAmountFormat(), getConfigManager().isProtectionEnabled(),
                    getConfigManager().getProtectionDuration(), stateRepository.getItemSpawnTimes(), stateRepository.getRawOwnerFormat(), usePapi,
                    getConfigManager().isHoloShowTimer(), stateRepository.getTimerComponentCache(), getConfigManager().isHoloTimerNewLine()
            );
            getHologramService().updateHologram(item, color, ctx);
        }
    }

    private void startGarbageCollectorTask() {
        if (getTrackedItemManager() != null) {
            getTrackedItemManager().startGarbageCollectorTask(isPluginEnabled(), getActiveItems());
        }
    }

    public void removeGlow(UUID uuid) {
        if (getVisualSpawner() != null) {
            getVisualSpawner().removeGlow(uuid);
        }
    }

    private void startLODTask() {
        if (getLodManager() != null && getConfigManager() != null) {
            getLodManager().startLODTask(isPluginEnabled(), getConfigManager().isLodEnabled(), getConfigManager().getLodBeamDistSq(), getConfigManager().getLodHoloDistSq(), getConfigManager().getFarmingViewDistance(),
                    stateRepository.getVisibleEntities(), stateRepository.getHiddenVisuals(), getActiveItems(), stateRepository.getGroupedItems(), stateRepository.getActiveLabels(), stateRepository.getActiveBeams(),
                    stateRepository.getActiveItemVisuals(), stateRepository.getActiveShadows(), getItemsByWorld(), getConfigManager().isFarmingEnabled(), stateRepository.getActiveCropSymbols(), getConfigManager().getLodInterval(), stateRepository.getGloballyVisibleEntities());
        }
    }

    public void updateEntityVisibility(Player p, Entity entity, boolean shouldSee, Set<UUID> visibleSet) {
        if (getEntityVisibilityService() != null) {
            getEntityVisibilityService().updateEntityVisibility(p, entity, shouldSee, visibleSet);
        }
    }

    private boolean isHiddenToggleFor(Player p) {
        return stateRepository.getHiddenVisuals().contains(p.getUniqueId());
    }

    private void startUnifiedTickTask() {
        if (getPluginTickManager() != null) {
            getPluginTickManager().startUnifiedTickTask(
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
        if (getItemMagnetManager() != null && getConfigManager() != null) {
            getItemMagnetManager().tickMagnet(getConfigManager().isMagnetEnabled(), getConfigManager().getMagnetDistance(), getConfigManager().getMagnetPermission(), getConfigManager().getMagnetCategories(),
                    getConfigManager().isMagnetEnableForGroups(), stateRepository.getGroupLeaders(), stateRepository.getGroupMembers(), stateRepository.getGroupedItems(), stateRepository.getItemCategoriesCache(),
                    getConfigManager().isProtectionEnabled(), getConfigManager().getProtectionDuration(), stateRepository.getItemSpawnTimes());
        }
    }

    private void tickBeamAnimation(float angle) {
        if (getBeamTickService() != null && getBeamManager() != null && getConfigManager() != null) {
            getBeamTickService().tickBeamAnimation(angle, getConfigManager().isBeamsEnabled(), getConfigManager().isBeamsAnimate(), stateRepository.getActiveBeams(), stateRepository.getGloballyVisibleEntities(),
                    getBeamManager().getActiveBeamConfigs(), getConfigManager().getBeamHeight(), getConfigManager().getBeamWidth(), stateRepository.getItemParticlesCache());
        }
    }

    private void tickFarmingAnimation(float angle) {
        if (getFarmingManager() != null && getConfigManager() != null) {
            getFarmingManager().tickFarmingAnimation(angle, getConfigManager().isFarmingEnabled(), getConfigManager().isFarmingAnimation(), stateRepository.getGloballyVisibleEntities());
        }
    }

    private void startFarmingTask() {
        if (getFarmingManager() != null && getConfigManager() != null) {
            getFarmingManager().startFarmingTask(isPluginEnabled(), getConfigManager().isFarmingEnabled(), getConfigManager().getFarmingCrops(), getConfigManager().getFarmingViewDistance(), stateRepository.getLastFarmingScanLocations());
        }
    }

    private void startGroupingTask() {
        if (getItemGroupingService() != null && getConfigManager() != null) {
            fr.skynex.lootglow.model.ItemGroupingContext ctx = new fr.skynex.lootglow.model.ItemGroupingContext(
                    isPluginEnabled(), getConfigManager().isGroupingEnabled(), stateRepository.getTrackedItems(), stateRepository.getActiveItems(),
                    stateRepository.getItemCategoriesCache(), stateRepository.getGroupedItems(), stateRepository.getGroupLeaders(), stateRepository.getGroupMembers(), stateRepository.getActiveItemVisuals(),
                    getConfigManager().isUseVisualBag(), getConfigManager().getBagMaterial(), getConfigManager().getBagHeadTexture(),
                    getConfigManager().isUseOwnerHead(), getConfigManager().getBagCustomModelData(), getConfigManager().getRpgRotation(),
                    getConfigManager().isHoloShowTimer(), stateRepository.getRawBundleFormat(), stateRepository.getItemCategories(), getConfigManager().getDefaultColor(), miniMessage
            );
            getItemGroupingService().startGroupingTask(ctx);
        }
    }

    public void spawnHologram(Item item, NamedTextColor color) {
        if (getHologramService() != null && getConfigManager() != null) {
            getHologramService().spawnHologram(item, color, getConfigManager().isHoloEnabled(), stateRepository.getItemCategoriesCache(), getConfigManager().isHoloHideUncategorized(),
                    stateRepository.getActiveLabels(), getConfigManager().isHoloSeeThrough(), getConfigManager().getHoloViewDistance(), getConfigManager().isHoloBackground(), getConfigManager().getHoloOffset(),
                    stateRepository.getBaseNameCache(), stateRepository.getDisplayNameOverridesCache(), stateRepository.getItemMoneyAmounts(), getConfigManager().getEconomyFormat(), getConfigManager().getEconomyPrefix(),
                    getConfigManager().isHoloShowAmount(), stateRepository.getRawAmountFormat(), getConfigManager().isProtectionEnabled(), getConfigManager().getProtectionDuration(), stateRepository.getItemSpawnTimes(),
                    stateRepository.getRawOwnerFormat(), usePapi, getConfigManager().isHoloShowTimer(), stateRepository.getTimerComponentCache(), getConfigManager().isHoloTimerNewLine(),
                    getConfigManager().getLodHoloDistSq(), stateRepository.getHiddenVisuals(), stateRepository.getVisibleEntities());
        }
    }

    public Component calculateBaseName(Item item, NamedTextColor color) {
        return getHologramService() != null && getConfigManager() != null ? getHologramService().calculateBaseName(item, color, stateRepository.getDisplayNameOverridesCache(), stateRepository.getItemMoneyAmounts(), getConfigManager().getEconomyFormat(), getConfigManager().getEconomyPrefix()) : Component.empty();
    }

    public Component buildFinalName(Item item, Component baseName) {
        return getHologramService() != null && getConfigManager() != null ? getHologramService().buildFinalName(item, baseName, getConfigManager().isHoloShowAmount(), stateRepository.getRawAmountFormat(), getConfigManager().isProtectionEnabled(), getConfigManager().getProtectionDuration(), stateRepository.getItemSpawnTimes(), stateRepository.getRawOwnerFormat(), usePapi, getConfigManager().isHoloShowTimer(), stateRepository.getTimerComponentCache(), getConfigManager().isHoloTimerNewLine()) : baseName;
    }

    public void spawnBeam(Item item, String category, NamedTextColor color) {
        if (getBeamManager() != null && getConfigManager() != null) {
            getBeamManager().spawnBeam(item, category, color, stateRepository.getActiveBeams(), getConfigManager().getBeamHeight(), getConfigManager().getBeamWidth(), getConfigManager().isBeamsAnimate(), getConfigManager().isBeamsUseCategoryColor(), getConfigManager().getLodBeamDistSq(), stateRepository.getHiddenVisuals(), stateRepository.getVisibleEntities());
        }
    }

    public Material getColorStainedGlass(NamedTextColor color) {
        return getBeamManager() != null ? getBeamManager().getColorStainedGlass(color) : Material.WHITE_STAINED_GLASS;
    }

    public void removeGlow(Item item) {
        if (item == null)
            return;
        removeGlow(item.getUniqueId());
    }

    public void removeGlowKeepDisplays(UUID uuid) {
        if (getVisualSpawner() != null) {
            getVisualSpawner().removeGlowKeepDisplays(uuid);
        }
        stateRepository.getGroupedItems().remove(uuid);
    }

    public void refreshHologram(Item item) {
        if (getHologramService() != null && getConfigManager() != null) {
            getHologramService().refreshHologram(item, getConfigManager().isHoloEnabled(), getConfigManager().isHoloHideUncategorized(), stateRepository.getItemCategoriesCache(), stateRepository.getItemCategories(), getConfigManager().getDefaultColor(), stateRepository.getLastHoloState());
        }
    }

    public void clearVisualsForPlayer(Player player) {
        if (getVisualDisplayManager() != null) {
            getVisualDisplayManager().clearVisualsForPlayer(player, stateRepository.getTrackedItems());
        }
    }

    public void spawnShadow(Item item) {
        if (getRpgDropManager() != null) {
            getRpgDropManager().spawnShadow(item);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        return getCommandManager() != null && getCommandManager().onCommand(sender, command, label, args);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        return getCommandManager() != null ? getCommandManager().onTabComplete(sender, command, alias, args) : Collections.emptyList();
    }

    public boolean isOnlyPlayerDrops() {
        return getConfigManager() != null && getConfigManager().isOnlyPlayerDrops();
    }

    public void preHideItem(Item item) {
        if (getItemGlowApplyService() != null && getConfigManager() != null) {
            getItemGlowApplyService().preHideItem(item, isPluginEnabled(), isRpgDropsEnabled(), sourceMobKey, stateRepository.getItemCategories(), stateRepository.getCategoryNames(), getConfigManager().getRpgEnabledCategories(), stateRepository.getEntityIdMap(), stateRepository.getHiddenVanillaItems());
        }
    }
    public boolean isHoloEnabled() { return getConfigManager() != null && getConfigManager().isHoloEnabled(); }
    public boolean isRpgDropsEnabled() { return getConfigManager() != null && getConfigManager().isRpgDropsEnabled(); }
    public boolean isHardLockEnabled() { return getConfigManager() != null && getConfigManager().isHardLockEnabled(); }
    public int getProtectionDuration() { return getConfigManager() != null ? getConfigManager().getProtectionDuration() : 10; }
    public String getBypassPermission() { return getConfigManager() != null ? getConfigManager().getBypassPermission() : "lootglow.bypass.lock"; }
    public boolean isProtocolLibEnabled() { return usePacketProvider; }
    public Map<Integer, UUID> getEntityIdMap() { return stateRepository.getEntityIdMap(); }
    public boolean isRmbPickupEnabled() { return getConfigManager() != null && getConfigManager().isRmbPickupEnabled(); }
    public boolean isRmbPickupForce() { return getConfigManager() != null && getConfigManager().isRmbPickupForce(); }
    public double getRmbPickupRange() { return getConfigManager() != null ? getConfigManager().getRmbPickupRange() : 3.0; }
    public boolean isRmbPickupEnableForGroups() { return getConfigManager() != null && getConfigManager().isRmbPickupEnableForGroups(); }
    public Map<UUID, Long> getItemSpawnTimes() { return stateRepository.getItemSpawnTimes(); }
    public Map<UUID, Component> getBaseNameCache() { return stateRepository.getBaseNameCache(); }
    public Map<UUID, Long> getLastHoloState() { return stateRepository.getLastHoloState(); }
    public Map<UUID, ItemDisplay> getActiveItemVisuals() { return stateRepository.getActiveItemVisuals(); }
    public Map<UUID, TextDisplay> getActiveLabels() { return stateRepository.getActiveLabels(); }
    public Map<UUID, BlockDisplay> getActiveBeams() { return stateRepository.getActiveBeams(); }
    public Map<UUID, org.bukkit.entity.Display> getActiveShadows() { return stateRepository.getActiveShadows(); }
    public Set<Integer> getHiddenVanillaItems() { return stateRepository.getHiddenVanillaItems(); }
    public Set<UUID> getHiddenVisuals() { return stateRepository.getHiddenVisuals(); }
    public boolean isPluginEnabled() { return getConfigManager() != null ? getConfigManager().isEnabled() : true; }

    private void setupPacketProvider() {
        if (getVisibilityPacketManager() != null) {
            this.packetProvider = getVisibilityPacketManager().setupPacketProvider();
            this.usePacketProvider = (this.packetProvider != null);
        }
    }

    public void refreshGlowForPlayer(Player player, boolean showVisuals) {
        if (getEntityVisibilityService() != null && getConfigManager() != null) {
            getEntityVisibilityService().refreshGlowForPlayer(player, showVisuals, stateRepository.getHiddenVanillaItems(), stateRepository.getEntityIdMap(), stateRepository.getVisibleEntities(), getConfigManager().getFarmingViewDistance(), getActiveItems(), stateRepository.getGroupedItems(), getConfigManager().getLodHoloDistSq(), getConfigManager().getLodBeamDistSq(), stateRepository.getActiveCropSymbols());
        }
        for (Item item : getActiveItems().values()) {
            if (item.getWorld().equals(player.getWorld()) && !stateRepository.getHiddenVanillaItems().contains(item.getEntityId())) {
                player.hideEntity(this, item);
                player.showEntity(this, item);
            }
        }
    }

    private void initDatabase() {
        if (getDatabaseManager() != null) {
            getDatabaseManager().initDatabase();
        }
    }

    private void closeDatabase() {
        if (getDatabaseManager() != null) {
            getDatabaseManager().closeDatabase();
        }
    }

    public void loadPlayerData(Player player) {
        if (getDatabaseManager() != null) {
            getDatabaseManager().loadPlayerData(player, stateRepository.getHiddenVisuals(), stateRepository.getDisabledMagnets());
        }
    }

    public Set<UUID> getDisabledMagnets() {
        return stateRepository.getDisabledMagnets();
    }

    public void savePlayerData(UUID uuid) {
        if (getDatabaseManager() != null) {
            boolean hidden = stateRepository.getHiddenVisuals().contains(uuid);
            boolean magDisabled = stateRepository.getDisabledMagnets().contains(uuid);
            getDatabaseManager().savePlayerData(uuid, hidden, magDisabled);
        }
    }

    public boolean isFarmingAllowed(Location loc) {
        return getFarmingManager() != null ? getFarmingManager().isFarmingAllowed(loc) : true;
    }

    public void spawnCropSymbol(org.bukkit.block.Block block) {
        if (getFarmingManager() != null) {
            getFarmingManager().spawnCropSymbol(block);
        }
    }

    public void removeCropSymbol(org.bukkit.block.Block block) {
        if (getFarmingManager() != null) {
            getFarmingManager().removeCropSymbol(block);
        }
    }

    public void relinkCropSymbol(org.bukkit.block.Block block, BlockDisplay bd) {
        bd.setVisibleByDefault(false);
        CropSymbol parts = stateRepository.getActiveCropSymbols().computeIfAbsent(block, k -> new CropSymbol(block.getLocation().add(0.5, getConfigManager() != null ? getConfigManager().getFarmingOffset() : 0.0, 0.5)));
        if (!parts.contains(bd)) {
            parts.add(bd);
        }
        updateCropSymbolVisibilityForWorld(parts);
    }

    public void updateCropSymbolVisibilityForWorld(CropSymbol cs) {
        if (cs == null || cs.isEmpty() || getConfigManager() == null) return;
        double farmDistSq = getConfigManager().getFarmingViewDistance() * getConfigManager().getFarmingViewDistance();
        Location loc = cs.location;
        World world = loc.getWorld();
        if (world == null) return;

        for (Player p : world.getPlayers()) {
            UUID pUuid = p.getUniqueId();
            boolean isHiddenToggle = isHiddenToggleFor(p);
            boolean shouldSee = !isHiddenToggle && p.getLocation().distanceSquared(loc) <= farmDistSq;
            Set<UUID> visibleSet = stateRepository.getVisibleEntities().computeIfAbsent(pUuid, k -> java.util.concurrent.ConcurrentHashMap.newKeySet());

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
    public boolean isWgEnabled() { return getConfigManager() != null && getConfigManager().isWgEnabled(); }
    public Material getFarmingMaterial() { return getConfigManager() != null ? getConfigManager().getFarmingMaterial() : Material.AIR; }
    public double getFarmingOffset() { return getConfigManager() != null ? getConfigManager().getFarmingOffset() : 0.0; }
    public float getFarmingScale() { return getConfigManager() != null ? getConfigManager().getFarmingScale() : 1.0f; }
    public NamedTextColor getFarmingGlowColor() { return getConfigManager() != null ? getConfigManager().getFarmingGlowColor() : NamedTextColor.GREEN; }
    public float getShadowScale() { return getConfigManager() != null ? getConfigManager().getShadowScale() : 1.0f; }
    public double getLodHoloDistSq() { return getConfigManager() != null ? getConfigManager().getLodHoloDistSq() : 0.0; }
    public Map<UUID, Set<UUID>> getVisibleEntities() { return stateRepository.getVisibleEntities(); }
    public void setGloballyVisibleEntities(Set<UUID> set) { stateRepository.setGloballyVisibleEntities(set); }

    public Set<Material> getFarmingCrops() {
        return getConfigManager() != null ? getConfigManager().getFarmingCrops() : Collections.emptySet();
    }

    public NamespacedKey getFarmingKey() {
        return farmingKey;
    }

    public void broadcastRpgDropVisibility(Item item) {
        if (getEntityVisibilityService() != null) {
            getEntityVisibilityService().broadcastRpgDropVisibility(item, stateRepository.getActiveItemVisuals(), stateRepository.getHiddenVisuals(), stateRepository.getGroupedItems());
        }
    }

    public void spawnItemVisual(Item item, String category, NamedTextColor color) {
        if (getItemVisualSpawnService() != null && getConfigManager() != null) {
            fr.skynex.lootglow.model.ItemVisualContext ctx = new fr.skynex.lootglow.model.ItemVisualContext(
                    getConfigManager().isUseVisualBag(), getConfigManager().isRpgDropsEnabled(), stateRepository.getGroupLeaders(),
                    stateRepository.getActiveItemVisuals(), stateRepository.getEntityIdMap(), new java.util.HashSet<>(getConfigManager().getRpgEnabledCategories()),
                    stateRepository.getHiddenVisuals(), stateRepository.getVisibleEntities(), getConfigManager().getCategoryGlow(), getConfigManager().isDefaultGlow(),
                    getConfigManager().getBagMaterial(), getConfigManager().getBagHeadTexture(), getConfigManager().isUseOwnerHead(), getConfigManager().getBagCustomModelData(),
                    getConfigManager().getRpgItemScale(), getConfigManager().getRpgBlockScale(), getConfigManager().getRpgRotation()
            );
            getItemVisualSpawnService().spawnItemVisual(item, category, color, ctx);
        }
    }

    private void tickGlobalSync() {
        if (getLootRenderPipeline() != null) {
            getLootRenderPipeline().tickSync();
        } else if (getItemPhysicsService() != null && getConfigManager() != null) {
            getItemPhysicsService().tickGlobalSync(isPluginEnabled(), getActiveItems(), stateRepository.getTrackedItems(), getConfigManager().getRpgBlockScale(), getConfigManager().getRpgItemScale(), getConfigManager().getBagMaterial(), stateRepository.getGroupLeaders(), getConfigManager().getHoloOffset(), getConfigManager().getShadowScale(), getConfigManager().getRpgRotation());
        }
    }

    private void tickBouncing() {
        if (getRpgDropManager() != null && getConfigManager() != null) {
            getRpgDropManager().tickBouncing(getConfigManager().isBouncingEnabled(), getActiveItems(), getConfigManager().getBouncingBlockedBlocks(), getConfigManager().isBouncingOnlyOnSnow(), getConfigManager().getMaxBounces(), getConfigManager().getJumpForce(), getConfigManager().getBounceDamping());
        }
    }

    public void playAspirationAnimation(Item item, Player player) {
        if (getRpgDropManager() != null && getConfigManager() != null) {
            getRpgDropManager().playAspirationAnimation(item, player, stateRepository.getActiveItemVisuals(), getConfigManager().isAspirationEnabled());
        }
    }

    private void tickAspiration() {
        if (getRpgDropManager() != null && getConfigManager() != null) {
            getRpgDropManager().tickAspiration(getConfigManager().isAspirationEnabled(), getConfigManager().getAspirationSpeed());
        }
    }

    public void openLootContainer(Player player, UUID leaderUuid) {
        if (getGroupContainerManager() != null && getConfigManager() != null) {
            getGroupContainerManager().openLootContainer(player, leaderUuid, getConfigManager().isContainerEnabled(), getConfigManager().getContainerTitle(), stateRepository.getActiveItemVisuals(), getConfigManager().getRpgBlockScale(), miniMessage);
        }
    }

    public Map<UUID, UUID> getOpenContainers() {
        return getGroupContainerManager() != null ? getGroupContainerManager().getOpenContainers() : stateRepository.getOpenContainers();
    }

    public Map<UUID, List<UUID>> getGroupMembers() {
        return getGroupContainerManager() != null ? getGroupContainerManager().getGroupMembers() : stateRepository.getGroupMembers();
    }

    public Map<UUID, Integer> getGroupLeaders() {
        return stateRepository.getGroupLeaders();
    }

    public Map<UUID, Item> getActiveItems() {
        return getTrackedItemManager() != null ? getTrackedItemManager().getActiveItems() : stateRepository.getActiveItems();
    }

    public Set<UUID> getGroupedItems() {
        return getGroupContainerManager() != null ? getGroupContainerManager().getGroupedItems() : stateRepository.getGroupedItems();
    }

    public boolean isContainerEnabled() {
        return getConfigManager() != null && getConfigManager().isContainerEnabled();
    }

    public boolean isContainerRequireClick() {
        return getConfigManager() != null && getConfigManager().isContainerRequireClick();
    }

    public UUID getGroupLeader(UUID itemUuid) {
        return getGroupContainerManager() != null ? getGroupContainerManager().getGroupLeader(itemUuid) : null;
    }

    public ItemStack createTexturedHead(String textureInput) {
        return getVisualDisplayManager() != null ? getVisualDisplayManager().createTexturedHead(textureInput) : new ItemStack(Material.PLAYER_HEAD);
    }

    public String getBase64Texture(String input) {
        return getVisualDisplayManager() != null ? getVisualDisplayManager().getBase64Texture(input) : null;
    }

    public Item getItemForDisplay(ItemDisplay display) {
        return getTrackedItemManager() != null ? getTrackedItemManager().getItemForDisplay(display) : null;
    }

    public Item getItemForLabel(TextDisplay label) {
        return getTrackedItemManager() != null ? getTrackedItemManager().getItemForLabel(label) : null;
    }

    public void transferLeaderVisuals(UUID oldLeader, UUID newLeader) {
        if (getGroupContainerManager() != null) {
            getGroupContainerManager().transferLeaderVisuals(oldLeader, newLeader);
        }
    }

    public ItemStack getOwnerHead(UUID owner) {
        return getVisualDisplayManager() != null ? getVisualDisplayManager().getOwnerHead(owner) : new ItemStack(Material.PLAYER_HEAD);
    }

    public Map<UUID, Location> getLastFarmingScanLocations() {
        return stateRepository.getLastFarmingScanLocations();
    }

    public boolean isFlatItemOrBlock(Material mat) {
        return fr.skynex.lootglow.util.ItemTypeClassifier.isFlatItemOrBlock(mat, getConfigManager() != null ? getConfigManager().getRpgForceFlatMaterials() : Collections.emptySet(), getConfigManager() != null ? getConfigManager().getRpgForceUprightMaterials() : Collections.emptySet());
    }

    public boolean isUprightItem(Material mat) {
        return fr.skynex.lootglow.util.ItemTypeClassifier.isUprightItem(mat, getConfigManager() != null ? getConfigManager().getRpgForceFlatMaterials() : Collections.emptySet(), getConfigManager() != null ? getConfigManager().getRpgForceUprightMaterials() : Collections.emptySet());
    }

    public boolean isFishItem(Material mat) {
        return fr.skynex.lootglow.util.ItemTypeClassifier.isFishItem(mat);
    }

    public boolean isCustomItem(ItemStack stack) {
        return fr.skynex.lootglow.util.ItemTypeClassifier.isCustomItem(stack);
    }

    public Double getMoneyAmount(ItemStack stack) {
        return fr.skynex.lootglow.util.MoneyAmountParser.getMoneyAmount(stack, getConfigManager() != null && getConfigManager().isEconomyEnabled(), getConfigManager() != null ? getConfigManager().getEconomyKeys() : Collections.emptyList());
    }

    public NamedTextColor parseNamedColor(String input) {
        return getConfigParser() != null ? getConfigParser().parseNamedColor(input) : NamedTextColor.WHITE;
    }

    public Sound parseSound(String input) {
        return getConfigParser() != null ? getConfigParser().parseSound(input) : null;
    }

    public void updateSurfaceAlignment(Item item) {
        if (getSurfaceAlignmentManager() != null) {
            getSurfaceAlignmentManager().updateSurfaceAlignment(item, getRecentlyBounced());
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
        fr.skynex.lootglow.managers.PluginLifecycleManager lifecycleManager = new fr.skynex.lootglow.managers.PluginLifecycleManager(this);
        this.serviceRegistry = lifecycleManager.initializeServicesAndManagers(
                getApiImpl(),
                stateRepository.getTrackedItems(),
                stateRepository.getActiveItems(),
                stateRepository.getEntityIdMap(),
                stateRepository.getGloballyVisibleEntities()
        );
        this.stateRepository.setTrackedItemManagerSupplier(this::getTrackedItemManager);
    }

    private void resetStateOnReload() {
        if (getPluginLifecycleManager() != null) {
            getPluginLifecycleManager().resetStateOnReload();
        }
    }

    private void registerListeners() {
        if (getPluginLifecycleManager() != null) {
            getPluginLifecycleManager().registerListeners(useMythic);
        }
    }

    private void registerCommands() {
        if (getPluginLifecycleManager() != null) {
            getPluginLifecycleManager().registerCommands();
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
