package fr.skynex.lootglow;

import fr.skynex.lootglow.commands.LootGlowCommandManager;
import fr.skynex.lootglow.config.LootGlowConfigManager;
import fr.skynex.lootglow.database.DatabaseManager;
import fr.skynex.lootglow.managers.TrackedItemManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LootGlow extends JavaPlugin implements fr.skynex.lootglow.api.LootGlowAPI {

    private final fr.skynex.lootglow.state.LootStateRepository stateRepository = new fr.skynex.lootglow.state.LootStateRepository();
    private fr.skynex.lootglow.registry.ServiceRegistry serviceRegistry = new fr.skynex.lootglow.registry.ServiceRegistry();

    private boolean usePapi;
    private boolean useWorldGuard;
    private boolean usePacketProvider = false;
    private NamespacedKey farmingKey;
    private NamespacedKey sourceMobKey;
    private boolean useMythic;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final fr.skynex.lootglow.api.impl.LootGlowAPIImpl apiImpl = new fr.skynex.lootglow.api.impl.LootGlowAPIImpl(this);

    public <T> T getService(Class<T> clazz) {
        return serviceRegistry.get(clazz);
    }

    public fr.skynex.lootglow.registry.ServiceRegistry getServiceRegistry() { return serviceRegistry; }
    public fr.skynex.lootglow.state.LootStateRepository getStateRepository() { return stateRepository; }
    public LootGlowConfigManager getConfigManager() { return getService(LootGlowConfigManager.class); }
    public fr.skynex.lootglow.api.impl.LootGlowAPIImpl getApiImpl() { return apiImpl; }
    public LootGlowCommandManager getCommandManager() { return getService(LootGlowCommandManager.class); }

    public NamespacedKey getSourceMobKey() { return sourceMobKey; }
    public NamespacedKey getFarmingKey() { return farmingKey; }
    public boolean isUsePapi() { return usePapi; }
    public boolean isUseWorldGuard() { return useWorldGuard; }
    public boolean isProtocolLibEnabled() { return usePacketProvider; }
    public boolean isPluginEnabled() { var cfg = getConfigManager(); return cfg == null || cfg.isEnabled(); }

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
        reloadConfig();
        File configFile = new File(getDataFolder(), "config.yml");
        if (configFile.exists()) {
            try {
                if (getConfig().getBoolean("settings.auto-update-config", true)) {
                    ConfigUpdater.update(this, "config.yml", configFile);
                }
            } catch (Exception ignored) {}
        }

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
        var disableSvc = getService(fr.skynex.lootglow.service.PluginDisableService.class);
        var rpgMgr = getService(fr.skynex.lootglow.managers.RPGDropManager.class);
        var trackedMgr = getService(TrackedItemManager.class);
        if (disableSvc != null) {
            disableSvc.onDisable(
                    stateRepository.getActiveLabels(), stateRepository.getActiveBeams(), stateRepository.getActiveItemVisuals(),
                    stateRepository.getActiveShadows(), stateRepository.getActiveCropSymbols(), stateRepository.getHiddenVanillaItems(),
                    stateRepository.getEntityIdMap(), stateRepository.getTrackedItems(), stateRepository.getActiveLights(),
                    stateRepository.getActiveItems(), trackedMgr != null ? trackedMgr.getItemsByWorld() : Collections.emptyMap(), stateRepository.getTimerComponentCache(),
                    rpgMgr != null ? rpgMgr.getBounceCounts() : Collections.emptyMap(), rpgMgr != null ? rpgMgr.getRecentlyBounced() : Collections.emptySet(), stateRepository.getLastFarmingScanLocations()
            );
        }
    }

    public void debugLog(String message) {
        if (getConfig().getBoolean("settings.debug", false)) {
            getLogger().info("[Debug] " + message);
        }
    }

    public void loadConfiguration() {
        var teamMgr = getService(fr.skynex.lootglow.managers.GlowTeamManager.class);
        if (teamMgr != null) {
            teamMgr.clearScoreboardTeams();
        }

        reloadConfig();
        var lifecycle = getService(fr.skynex.lootglow.managers.PluginLifecycleManager.class);
        if (lifecycle != null) {
            lifecycle.loadMessages();
        }
        resetStateOnReload();

        var cfgMgr = getService(LootGlowConfigManager.class);
        if (cfgMgr != null) {
            cfgMgr.loadAll(getConfig(), miniMessage, stateRepository.getDisplayNameOverridesCache());
        }

        if (lifecycle != null) {
            lifecycle.setupTeams();
            lifecycle.startBackgroundTasks();
        }

        debugLog("Configuration loaded. Debug mode enabled.");

        var mergeMgr = getService(fr.skynex.lootglow.managers.ItemMergeManager.class);
        if (mergeMgr != null) {
            mergeMgr.loadConfig();
        }

        startBackgroundTasks();

        if (cfgMgr != null && !cfgMgr.isOnlyPlayerDrops()) {
            var applySvc = getService(fr.skynex.lootglow.service.ItemGlowApplyService.class);
            if (applySvc != null) {
                for (World world : Bukkit.getWorlds()) {
                    for (Item item : world.getEntitiesByClass(Item.class)) {
                        if (item.isValid()) {
                            fr.skynex.lootglow.util.FoliaScheduler.runAtEntity(this, item, () ->
                                    applySvc.applyGlow(item, false, fr.skynex.lootglow.model.ItemGlowContext.from(this))
                            );
                        }
                    }
                }
            }
        }

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        var cmdMgr = getService(LootGlowCommandManager.class);
        return cmdMgr != null && cmdMgr.onCommand(sender, command, label, args);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        var cmdMgr = getService(LootGlowCommandManager.class);
        return cmdMgr != null ? cmdMgr.onTabComplete(sender, command, alias, args) : Collections.emptyList();
    }

    private void setupPacketProvider() {
        var pktMgr = getService(fr.skynex.lootglow.managers.VisibilityPacketManager.class);
        if (pktMgr != null) {
            this.usePacketProvider = (pktMgr.setupPacketProvider() != null);
        }
    }

    private void initDatabase() {
        var db = getService(DatabaseManager.class);
        if (db != null) {
            db.initDatabase();
        }
    }

    private void closeDatabase() {
        var db = getService(DatabaseManager.class);
        if (db != null) {
            db.closeDatabase();
        }
    }

    private void initManagersAndServices() {
        fr.skynex.lootglow.managers.PluginLifecycleManager lifecycleManager = new fr.skynex.lootglow.managers.PluginLifecycleManager(this);
        this.serviceRegistry = lifecycleManager.initializeServicesAndManagers(
                getApiImpl(),
                stateRepository.getTrackedItems(),
                stateRepository.getActiveItems(),
                stateRepository.getEntityIdMap(),
                stateRepository.getGloballyVisibleEntities()
        );
        this.stateRepository.setTrackedItemManagerSupplier(() -> getService(TrackedItemManager.class));
    }

    private void resetStateOnReload() {
        var lifecycle = getService(fr.skynex.lootglow.managers.PluginLifecycleManager.class);
        if (lifecycle != null) {
            lifecycle.resetStateOnReload();
        }
    }

    private void registerListeners() {
        var lifecycle = getService(fr.skynex.lootglow.managers.PluginLifecycleManager.class);
        if (lifecycle != null) {
            lifecycle.registerListeners(useMythic);
        }
    }

    private void registerCommands() {
        var lifecycle = getService(fr.skynex.lootglow.managers.PluginLifecycleManager.class);
        if (lifecycle != null) {
            lifecycle.registerCommands();
        }
    }

    private void startBackgroundTasks() {
        var lifecycle = getService(fr.skynex.lootglow.managers.PluginLifecycleManager.class);
        if (lifecycle != null) {
            lifecycle.startBackgroundTasks();
        }
    }

    // ==========================================
    //            LootGlowAPI Implementation
    // ==========================================

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
    @Override public boolean isGrouped(@NotNull Item item) { return apiImpl.isGrouped(item); }
    @Nullable @Override public Item getLootBagLeader(@NotNull Item item) { return apiImpl.getLootBagLeader(item); }
    @NotNull @Override public List<Item> getGroupedMembers(@NotNull Item bagItem) { return apiImpl.getGroupedMembers(bagItem); }
    @Override public void setParticleAnimationType(@NotNull Item item, @Nullable String animationType) { apiImpl.setParticleAnimationType(item, animationType); }
    @Override public void setCustomLightLevel(@NotNull Item item, int lightLevel) { apiImpl.setCustomLightLevel(item, lightLevel); }
    @Override public void pullItemsToPlayer(@NotNull Player player, double radius, @Nullable java.util.function.Predicate<Item> filter) { apiImpl.pullItemsToPlayer(player, radius, filter); }
}
