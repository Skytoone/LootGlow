package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.commands.LootGlowCommandManager;
import fr.skynex.lootglow.config.LootGlowConfigManager;
import fr.skynex.lootglow.listeners.FarmingListener;
import fr.skynex.lootglow.listeners.FishingListener;
import fr.skynex.lootglow.listeners.LootContainerListener;
import fr.skynex.lootglow.model.TrackedItem;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import fr.skynex.lootglow.database.DatabaseManager;

import java.util.List;

/**
 * Handles plugin startup lifecycle steps: listener registration, command registration, and state resets.
 */
public class PluginLifecycleManager {

    private final LootGlow plugin;

    public PluginLifecycleManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void registerListeners(boolean useMythic) {
        plugin.getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.ItemLifecycleListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.ItemPickupListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.ItemInteractionListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.ChunkLifecycleListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.PlayerLifecycleListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new FarmingListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new LootContainerListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new FishingListener(plugin), plugin);
        if (useMythic) {
            try {
                plugin.getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.MythicListener(plugin), plugin);
            } catch (NoClassDefFoundError ignored) {}
        }
    }

    public void registerCommands() {
        // 1. Try Paper 1.21+ Lifecycle API
        try {
            plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                final var registrar = event.registrar();
                registrar.register("lootglow", "Main command for LootGlow", List.of("lg", "glow", "loot"),
                        new BasicCommand() {
                            @Override
                            public void execute(CommandSourceStack stack, String[] args) {
                                plugin.onCommand(stack.getSender(), null, "lootglow", args);
                            }

                            @Override
                            public java.util.Collection<String> suggest(CommandSourceStack stack, String[] args) {
                                return plugin.onTabComplete(stack.getSender(), null, "lootglow", args);
                            }
                        });
            });
        } catch (Throwable ignored) {}

        // 2. Standard Bukkit plugin.yml command binding or dynamic fallback
        try {
            org.bukkit.command.PluginCommand cmd = plugin.getCommand("lootglow");
            LootGlowCommandManager cmdMgr = plugin.getService(LootGlowCommandManager.class);
            if (cmd != null && cmdMgr != null) {
                cmd.setExecutor(cmdMgr);
                cmd.setTabCompleter(cmdMgr);
            } else {
                registerDynamicCommand("lootglow", List.of("lg", "glow", "loot"));
            }
        } catch (Throwable t) {
            // Paper plugin loader throws UnsupportedOperationException for JavaPlugin#getCommand
            registerDynamicCommand("lootglow", List.of("lg", "glow", "loot"));
        }
    }

    private void registerDynamicCommand(String name, List<String> aliases) {
        try {
            java.lang.reflect.Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) field.get(Bukkit.getServer());

            if (commandMap != null) {
                org.bukkit.command.Command dynCmd = new org.bukkit.command.Command(name, "Main command for LootGlow", "/" + name, aliases) {
                    @Override
                    public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                        return plugin.onCommand(sender, this, commandLabel, args);
                    }

                    @Override
                    public List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                        List<String> completions = plugin.onTabComplete(sender, this, alias, args);
                        return completions != null ? completions : super.tabComplete(sender, alias, args);
                    }
                };
                commandMap.register("lootglow", dynCmd);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to dynamically register /" + name + " command: " + t.getMessage());
        }
    }

    public void resetStateOnReload() {
        TrackedItemManager trackedItemMgr = plugin.getService(TrackedItemManager.class);
        if (trackedItemMgr != null) {
            trackedItemMgr.clearAll();
        } else {
            for (TrackedItem ti : plugin.getStateRepository().getTrackedItems().values()) {
                if (ti.label != null && ti.label.isValid()) ti.label.remove();
                if (ti.beam != null && ti.beam.isValid()) {
                    ti.beam.getPassengers().forEach(e -> { if (e != null) e.remove(); });
                    ti.beam.remove();
                }
                if (ti.visual != null && ti.visual.isValid()) ti.visual.remove();
                if (ti.shadow != null && ti.shadow.isValid()) ti.shadow.remove();
            }
            plugin.getStateRepository().getTrackedItems().clear();
            plugin.getStateRepository().getActiveItems().clear();
            plugin.getStateRepository().getEntityIdMap().clear();
        }

        plugin.getStateRepository().getHiddenVanillaItems().clear();
        plugin.getStateRepository().getItemSpawnTimes().clear();
        plugin.getStateRepository().getItemCategories().clear();
        plugin.getStateRepository().getCategoryParticles().clear();
        plugin.getStateRepository().getCategorySounds().clear();
        plugin.getStateRepository().getCategoryNames().clear();

        LootGlowConfigManager cfgMgr = plugin.getConfigManager();
        if (cfgMgr != null) {
            cfgMgr.getCategoryGlow().clear();
            cfgMgr.getFilteredWorlds().clear();
        }
        plugin.getStateRepository().getCategoryColors().clear();
        plugin.getStateRepository().getDisplayNameOverridesCache().clear();
        plugin.getStateRepository().getCategoryLights().clear();

        plugin.getStateRepository().getActiveLights().forEach((uuid, loc) -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(loc.getWorld())) {
                    p.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }
        });
        plugin.getStateRepository().getActiveLights().clear();

        plugin.getStateRepository().getActiveCropSymbols().values().forEach(list -> list.forEach(d -> {
            if (d != null && d.isValid()) d.remove();
        }));
        plugin.getStateRepository().getActiveCropSymbols().clear();

        plugin.getStateRepository().getVisibleEntities().clear();
        plugin.getStateRepository().getHiddenVisuals().clear();
        plugin.getStateRepository().getDisabledMagnets().clear();
        plugin.getStateRepository().getCategoryDustOptions().clear();
        plugin.getStateRepository().getRecentlyBounced().clear();

        SurfaceAlignmentManager surfaceAlignMgr = plugin.getService(SurfaceAlignmentManager.class);
        if (surfaceAlignMgr != null) {
            surfaceAlignMgr.clearAll();
        }
        plugin.getStateRepository().getLastFarmingScanLocations().clear();
        plugin.getStateRepository().getGloballyVisibleEntities().clear();

        GroupContainerManager groupContainerMgr = plugin.getService(GroupContainerManager.class);
        if (groupContainerMgr != null) {
            groupContainerMgr.clearAll();
        }
        plugin.getStateRepository().getGroupMembers().clear();
        plugin.getStateRepository().getGroupedItems().clear();
        plugin.getStateRepository().getOpenContainers().clear();

        BeamManager beamMgr = plugin.getService(BeamManager.class);
        if (beamMgr != null) {
            beamMgr.clearAll();
        }
        ParticleAnimationManager particleAnimMgr = plugin.getService(ParticleAnimationManager.class);
        if (particleAnimMgr != null) {
            particleAnimMgr.getCustomParticles().clear();
        }
        HologramRenderer holoRenderer = plugin.getService(HologramRenderer.class);
        if (holoRenderer != null) {
            holoRenderer.getCustomHolograms().clear();
        }

        fr.skynex.lootglow.spatial.LootSpatialIndexService spatialIndexService = plugin.getService(fr.skynex.lootglow.spatial.LootSpatialIndexService.class);
        if (spatialIndexService != null) {
            spatialIndexService.clearAll();
        }
        RPGDropManager rpgMgr = plugin.getService(RPGDropManager.class);
        if (rpgMgr != null) {
            rpgMgr.getRecentlyBounced().clear();
            rpgMgr.getBounceCounts().clear();
        }
    }

    public fr.skynex.lootglow.registry.ServiceRegistry initializeServicesAndManagers(
            fr.skynex.lootglow.api.LootGlowAPI apiImpl,
            java.util.Map<java.util.UUID, fr.skynex.lootglow.model.TrackedItem> trackedItems,
            java.util.Map<java.util.UUID, org.bukkit.entity.Item> activeItems,
            java.util.Map<Integer, java.util.UUID> entityIdMap,
            java.util.Set<java.util.UUID> globallyVisibleEntities
    ) {
        fr.skynex.lootglow.registry.ServiceRegistry serviceRegistry = new fr.skynex.lootglow.registry.ServiceRegistry();

        var spatialIndexService = new fr.skynex.lootglow.spatial.LootSpatialIndexService(plugin);
        var lootEventDispatcher = new fr.skynex.lootglow.event.LootEventDispatcher(plugin);
        var databaseManager = new DatabaseManager(plugin);
        var trackedItemManager = new TrackedItemManager(plugin, trackedItems, activeItems, entityIdMap, globallyVisibleEntities);
        var beamManager = new BeamManager(plugin);
        var hologramManager = new HologramManager(plugin);
        var configManager = new LootGlowConfigManager(plugin);
        var commandManager = new LootGlowCommandManager(plugin);
        var farmingManager = new fr.skynex.lootglow.managers.FarmingManager(plugin);
        var rpgDropManager = new fr.skynex.lootglow.managers.RPGDropManager(plugin);
        var particleAnimationManager = new fr.skynex.lootglow.managers.ParticleAnimationManager(plugin);
        var groupContainerManager = new fr.skynex.lootglow.managers.GroupContainerManager(plugin);
        var lootProtectionManager = new fr.skynex.lootglow.managers.LootProtectionManager(plugin);
        var itemMergeManager = new fr.skynex.lootglow.managers.ItemMergeManager(plugin);
        itemMergeManager.loadConfig();
        var occlusionManager = new fr.skynex.lootglow.managers.OcclusionManager();
        var glowManager = new fr.skynex.lootglow.managers.GlowManager();
        var itemMagnetManager = new fr.skynex.lootglow.managers.ItemMagnetManager(plugin);
        var economyDropManager = new fr.skynex.lootglow.managers.EconomyDropManager(plugin);
        var hologramRenderer = new fr.skynex.lootglow.managers.HologramRenderer(plugin);
        var surfaceAlignmentManager = new fr.skynex.lootglow.managers.SurfaceAlignmentManager(plugin);
        var glowTeamManager = new fr.skynex.lootglow.managers.GlowTeamManager(plugin);
        var visualDisplayManager = new fr.skynex.lootglow.managers.VisualDisplayManager(plugin);
        var pluginTickManager = new fr.skynex.lootglow.managers.PluginTickManager(plugin, serviceRegistry, plugin.getStateRepository());
        var visualSpawner = new fr.skynex.lootglow.managers.VisualSpawner(plugin);
        var configParser = new fr.skynex.lootglow.config.ConfigParser(plugin);
        var integrationManager = new fr.skynex.lootglow.integration.IntegrationManager(plugin);
        var playerSettingsManager = new fr.skynex.lootglow.managers.PlayerSettingsManager(plugin);
        var visibilityPacketManager = new fr.skynex.lootglow.managers.VisibilityPacketManager(plugin);
        var lodManager = new fr.skynex.lootglow.managers.LODManager(plugin);
        var itemNameFormatter = new fr.skynex.lootglow.util.ItemNameFormatter();
        var lootWorldManager = new fr.skynex.lootglow.managers.LootWorldManager(plugin);
        var vanillaItemVisibilityManager = new fr.skynex.lootglow.managers.VanillaItemVisibilityManager(plugin);
        var rarityManager = new fr.skynex.lootglow.managers.RarityManager(plugin);
        var groundAuraManager = new fr.skynex.lootglow.managers.GroundAuraManager(plugin);
        var hologramTickService = new fr.skynex.lootglow.service.HologramTickService(plugin);
        var beamTickService = new fr.skynex.lootglow.service.BeamTickService(plugin);
        var itemRotationService = new fr.skynex.lootglow.service.ItemRotationService(plugin);
        var entityVisibilityService = new fr.skynex.lootglow.service.EntityVisibilityService(plugin);
        var itemVisualSpawnService = new fr.skynex.lootglow.service.ItemVisualSpawnService(plugin);
        var itemGroupingService = new fr.skynex.lootglow.service.ItemGroupingService(plugin);
        var hologramService = new fr.skynex.lootglow.service.HologramService(plugin);
        var pluginDisableService = new fr.skynex.lootglow.service.PluginDisableService(plugin);
        var messageService = new fr.skynex.lootglow.service.MessageService(plugin);
        var lightService = new fr.skynex.lootglow.service.LightService(plugin);
        var itemGlowApplyService = new fr.skynex.lootglow.service.ItemGlowApplyService(plugin);
        var itemPhysicsService = new fr.skynex.lootglow.service.ItemPhysicsService(plugin);
        var lootRenderPipeline = new fr.skynex.lootglow.pipeline.LootRenderPipeline(plugin);

        serviceRegistry
                .registerService(fr.skynex.lootglow.spatial.LootSpatialIndexService.class, spatialIndexService)
                .registerService(fr.skynex.lootglow.event.LootEventDispatcher.class, lootEventDispatcher)
                .registerService(fr.skynex.lootglow.pipeline.LootRenderPipeline.class, lootRenderPipeline)
                .registerService(DatabaseManager.class, databaseManager)
                .registerService(TrackedItemManager.class, trackedItemManager)
                .registerService(BeamManager.class, beamManager)
                .registerService(HologramManager.class, hologramManager)
                .registerService(LootGlowConfigManager.class, configManager)
                .registerService(LootGlowCommandManager.class, commandManager)
                .registerService(fr.skynex.lootglow.managers.FarmingManager.class, farmingManager)
                .registerService(fr.skynex.lootglow.managers.RPGDropManager.class, rpgDropManager)
                .registerService(fr.skynex.lootglow.managers.ParticleAnimationManager.class, particleAnimationManager)
                .registerService(fr.skynex.lootglow.managers.GroupContainerManager.class, groupContainerManager)
                .registerService(fr.skynex.lootglow.managers.LootProtectionManager.class, lootProtectionManager)
                .registerService(fr.skynex.lootglow.managers.ItemMergeManager.class, itemMergeManager)
                .registerService(fr.skynex.lootglow.managers.OcclusionManager.class, occlusionManager)
                .registerService(fr.skynex.lootglow.managers.GlowManager.class, glowManager)
                .registerService(fr.skynex.lootglow.managers.ItemMagnetManager.class, itemMagnetManager)
                .registerService(fr.skynex.lootglow.managers.EconomyDropManager.class, economyDropManager)
                .registerService(fr.skynex.lootglow.managers.HologramRenderer.class, hologramRenderer)
                .registerService(fr.skynex.lootglow.managers.SurfaceAlignmentManager.class, surfaceAlignmentManager)
                .registerService(fr.skynex.lootglow.managers.GlowTeamManager.class, glowTeamManager)
                .registerService(fr.skynex.lootglow.managers.VisualDisplayManager.class, visualDisplayManager)
                .registerService(fr.skynex.lootglow.managers.PluginTickManager.class, pluginTickManager)
                .registerService(fr.skynex.lootglow.managers.VisualSpawner.class, visualSpawner)
                .registerService(fr.skynex.lootglow.config.ConfigParser.class, configParser)
                .registerService(fr.skynex.lootglow.integration.IntegrationManager.class, integrationManager)
                .registerService(fr.skynex.lootglow.managers.PlayerSettingsManager.class, playerSettingsManager)
                .registerService(fr.skynex.lootglow.managers.VisibilityPacketManager.class, visibilityPacketManager)
                .registerService(fr.skynex.lootglow.managers.LODManager.class, lodManager)
                .registerService(fr.skynex.lootglow.util.ItemNameFormatter.class, itemNameFormatter)
                .registerService(fr.skynex.lootglow.managers.LootWorldManager.class, lootWorldManager)
                .registerService(fr.skynex.lootglow.managers.VanillaItemVisibilityManager.class, vanillaItemVisibilityManager)
                .registerService(fr.skynex.lootglow.managers.RarityManager.class, rarityManager)
                .registerService(fr.skynex.lootglow.managers.GroundAuraManager.class, groundAuraManager)
                .registerService(fr.skynex.lootglow.service.HologramTickService.class, hologramTickService)
                .registerService(fr.skynex.lootglow.service.BeamTickService.class, beamTickService)
                .registerService(fr.skynex.lootglow.service.ItemRotationService.class, itemRotationService)
                .registerService(fr.skynex.lootglow.service.EntityVisibilityService.class, entityVisibilityService)
                .registerService(fr.skynex.lootglow.service.ItemVisualSpawnService.class, itemVisualSpawnService)
                .registerService(fr.skynex.lootglow.service.ItemGroupingService.class, itemGroupingService)
                .registerService(fr.skynex.lootglow.service.HologramService.class, hologramService)
                .registerService(fr.skynex.lootglow.service.PluginDisableService.class, pluginDisableService)
                .registerService(fr.skynex.lootglow.service.MessageService.class, messageService)
                .registerService(fr.skynex.lootglow.service.LightService.class, lightService)
                .registerService(fr.skynex.lootglow.service.ItemGlowApplyService.class, itemGlowApplyService)
                .registerService(fr.skynex.lootglow.service.ItemPhysicsService.class, itemPhysicsService)
                .registerService(PluginLifecycleManager.class, this)
                .registerService(fr.skynex.lootglow.api.LootGlowAPI.class, apiImpl);

        return serviceRegistry;
    }

    public void startBackgroundTasks() {
        startParticleTask();
        startLightingTask();
        startLODTask();
        startGarbageCollectorTask();
        startGroupingTask();
        startFarmingTask();

        var tickMgr = plugin.getService(PluginTickManager.class);
        if (tickMgr != null) {
            tickMgr.startUnifiedTickTask();
        }
    }

    public void loadMessages() {
        var msgSvc = plugin.getService(fr.skynex.lootglow.service.MessageService.class);
        var stateRepo = plugin.getStateRepository();
        if (msgSvc != null) {
            msgSvc.loadMessages(stateRepo.getTimerComponentCache());
            stateRepo.setRawAmountFormat(msgSvc.getRawAmountFormat());
            stateRepo.setRawOwnerFormat(msgSvc.getRawOwnerFormat());
            stateRepo.setRawBundleFormat(msgSvc.getRawBundleFormat());
        }
    }

    public void setupTeams() {
        var teamMgr = plugin.getService(GlowTeamManager.class);
        if (teamMgr != null) {
            teamMgr.setupTeams();
        }
    }

    private void startParticleTask() {
        var animMgr = plugin.getService(ParticleAnimationManager.class);
        var cfgMgr = plugin.getService(LootGlowConfigManager.class);
        var stateRepo = plugin.getStateRepository();
        if (animMgr != null && cfgMgr != null) {
            animMgr.startParticleTask(plugin.isPluginEnabled(), cfgMgr.isParticlesEnabled(), cfgMgr.getLodPartDistSq(), stateRepo.getActiveItems(), stateRepo.getItemParticlesCache(), stateRepo.getItemCategoriesCache(), stateRepo.getHiddenVisuals(), stateRepo.getCategoryDustOptions(), stateRepo.getDefaultDustOptions(), stateRepo.getCategoryAnimTypes(), cfgMgr.getParticleAnimType(), cfgMgr.getParticlesFrequency());
        }
    }

    private void startLightingTask() {
        int interval = plugin.getConfig().getInt("settings.lighting.update-interval", 5);
        var lightSvc = plugin.getService(fr.skynex.lootglow.service.LightService.class);
        var cfgMgr = plugin.getService(LootGlowConfigManager.class);
        var stateRepo = plugin.getStateRepository();
        if (lightSvc != null && cfgMgr != null) {
            lightSvc.startLightingTask(plugin.isPluginEnabled(), cfgMgr.isLightingEnabled(), stateRepo.getActiveLights(), stateRepo.getActiveItems(), stateRepo.getItemCategoriesCache(), stateRepo.getCategoryLights(), cfgMgr.getCachedLightBlockData(), interval);
        }
    }

    private void startGarbageCollectorTask() {
        var trackedMgr = plugin.getService(TrackedItemManager.class);
        if (trackedMgr != null) {
            trackedMgr.startGarbageCollectorTask(plugin.isPluginEnabled(), plugin.getStateRepository().getActiveItems());
        }
    }

    private void startLODTask() {
        var lodMgr = plugin.getService(LODManager.class);
        var cfgMgr = plugin.getService(LootGlowConfigManager.class);
        var trackedMgr = plugin.getService(TrackedItemManager.class);
        var stateRepo = plugin.getStateRepository();
        if (lodMgr != null && cfgMgr != null) {
            lodMgr.startLODTask(plugin.isPluginEnabled(), cfgMgr.isLodEnabled(), cfgMgr.getLodBeamDistSq(), cfgMgr.getLodHoloDistSq(), cfgMgr.getFarmingViewDistance(),
                    stateRepo.getVisibleEntities(), stateRepo.getHiddenVisuals(), stateRepo.getActiveItems(), stateRepo.getGroupedItems(), stateRepo.getActiveLabels(), stateRepo.getActiveBeams(),
                    stateRepo.getActiveItemVisuals(), stateRepo.getActiveShadows(), trackedMgr != null ? trackedMgr.getItemsByWorld() : java.util.Collections.emptyMap(), cfgMgr.isFarmingEnabled(), stateRepo.getActiveCropSymbols(), cfgMgr.getLodInterval(), stateRepo.getGloballyVisibleEntities());
        }
    }

    private void startFarmingTask() {
        var farmMgr = plugin.getService(FarmingManager.class);
        var cfgMgr = plugin.getService(LootGlowConfigManager.class);
        var stateRepo = plugin.getStateRepository();
        if (farmMgr != null && cfgMgr != null) {
            farmMgr.startFarmingTask(plugin.isPluginEnabled(), cfgMgr.isFarmingEnabled(), cfgMgr.getFarmingCrops(), cfgMgr.getFarmingViewDistance(), stateRepo.getLastFarmingScanLocations());
        }
    }

    private void startGroupingTask() {
        var groupSvc = plugin.getService(fr.skynex.lootglow.service.ItemGroupingService.class);
        var cfgMgr = plugin.getService(LootGlowConfigManager.class);
        var stateRepo = plugin.getStateRepository();
        if (groupSvc != null && cfgMgr != null) {
            fr.skynex.lootglow.model.ItemGroupingContext ctx = new fr.skynex.lootglow.model.ItemGroupingContext(
                    plugin.isPluginEnabled(), cfgMgr.isGroupingEnabled(), stateRepo.getTrackedItems(), stateRepo.getActiveItems(),
                    stateRepo.getItemCategoriesCache(), stateRepo.getGroupedItems(), stateRepo.getGroupLeaders(), stateRepo.getGroupMembers(), stateRepo.getActiveItemVisuals(),
                    cfgMgr.isUseVisualBag(), cfgMgr.getBagMaterial(), cfgMgr.getBagHeadTexture(),
                    cfgMgr.isUseOwnerHead(), cfgMgr.getBagCustomModelData(), cfgMgr.getRpgRotation(),
                    cfgMgr.isHoloShowTimer(), stateRepo.getRawBundleFormat(), stateRepo.getItemCategories(), cfgMgr.getDefaultColor(), net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
            );
            groupSvc.startGroupingTask(ctx);
        }
    }
}
