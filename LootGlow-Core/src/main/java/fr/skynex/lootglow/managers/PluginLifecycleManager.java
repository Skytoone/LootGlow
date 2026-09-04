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
            LootGlowCommandManager cmdMgr = plugin.getCommandManager();
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
        var registry = plugin.getServiceRegistry();
        TrackedItemManager trackedItemMgr = registry != null ? registry.getService(TrackedItemManager.class) : null;
        if (trackedItemMgr != null) {
            trackedItemMgr.clearAll();
        } else {
            for (TrackedItem ti : plugin.getTrackedItems().values()) {
                if (ti.label != null && ti.label.isValid()) ti.label.remove();
                if (ti.beam != null && ti.beam.isValid()) {
                    ti.beam.getPassengers().forEach(e -> { if (e != null) e.remove(); });
                    ti.beam.remove();
                }
                if (ti.visual != null && ti.visual.isValid()) ti.visual.remove();
                if (ti.shadow != null && ti.shadow.isValid()) ti.shadow.remove();
            }
            plugin.getTrackedItems().clear();
            plugin.getActiveItems().clear();
            plugin.getItemsByWorld().clear();
            plugin.getEntityIdMap().clear();
        }

        plugin.getHiddenVanillaItems().clear();
        plugin.getItemSpawnTimes().clear();
        plugin.getItemCategories().clear();
        plugin.getCategoryParticles().clear();
        plugin.getCategorySounds().clear();
        plugin.getCategoryNames().clear();

        LootGlowConfigManager cfgMgr = registry != null ? registry.getService(LootGlowConfigManager.class) : null;
        if (cfgMgr != null) {
            cfgMgr.getCategoryGlow().clear();
            cfgMgr.getFilteredWorlds().clear();
        }
        plugin.getCategoryColors().clear();
        plugin.getDisplayNameOverridesCache().clear();
        plugin.getCategoryLights().clear();

        plugin.getActiveLights().forEach((uuid, loc) -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(loc.getWorld())) {
                    p.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }
        });
        plugin.getActiveLights().clear();

        plugin.getActiveCropSymbols().values().forEach(list -> list.forEach(d -> {
            if (d != null && d.isValid()) d.remove();
        }));
        plugin.getActiveCropSymbols().clear();

        plugin.getVisibleEntities().clear();
        plugin.getHiddenVisuals().clear();
        plugin.getDisabledMagnets().clear();
        plugin.getCategoryDustOptions().clear();

        SurfaceAlignmentManager surfaceAlignMgr = registry != null ? registry.getService(SurfaceAlignmentManager.class) : null;
        if (surfaceAlignMgr != null) {
            surfaceAlignMgr.clearAll();
        }
        plugin.getLastFarmingScanLocations().clear();

        plugin.getGloballyVisibleEntities().clear();

        GroupContainerManager groupContainerMgr = registry != null ? registry.getService(GroupContainerManager.class) : null;
        if (groupContainerMgr != null) {
            groupContainerMgr.clearAll();
        }
        plugin.getGroupMembers().clear();
        plugin.getGroupedItems().clear();
        plugin.getOpenContainers().clear();

        BeamManager beamMgr = registry != null ? registry.getService(BeamManager.class) : null;
        if (beamMgr != null) {
            beamMgr.clearAll();
        }
        ParticleAnimationManager particleAnimMgr = registry != null ? registry.getService(ParticleAnimationManager.class) : null;
        if (particleAnimMgr != null) {
            particleAnimMgr.getCustomParticles().clear();
        }
        HologramRenderer holoRenderer = registry != null ? registry.getService(HologramRenderer.class) : null;
        if (holoRenderer != null) {
            holoRenderer.getCustomHolograms().clear();
        }

        fr.skynex.lootglow.spatial.LootSpatialIndexService spatialIndexService = registry != null ? registry.getService(fr.skynex.lootglow.spatial.LootSpatialIndexService.class) : null;
        if (spatialIndexService != null) {
            spatialIndexService.clearAll();
        }
        plugin.getRecentlyBounced().clear();
        plugin.getBounceCounts().clear();
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
        var pluginTickManager = new fr.skynex.lootglow.managers.PluginTickManager(plugin);
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
}
