package fr.skynex.lootglow;

import java.io.File;
import java.nio.charset.StandardCharsets;
import fr.skynex.lootglow.listeners.ItemListener;
import fr.skynex.lootglow.util.FoliaScheduler;
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

public class LootGlow extends JavaPlugin implements org.bukkit.event.Listener, fr.skynex.lootglow.api.LootGlowAPI {

    private static final NamespacedKey ORAXEN_KEY = new NamespacedKey("oraxen", "id");
    private static final NamespacedKey ORAXEN_KEY_ALT = new NamespacedKey("oraxen", "item_id");
    private static final NamespacedKey ITEMSADDER_KEY = new NamespacedKey("itemsadder", "id");
    private static final NamespacedKey ITEMSADDER_KEY_ALT = new NamespacedKey("itemsadder", "item_id");
    private static final NamespacedKey NEXO_KEY = new NamespacedKey("nexo", "id");
    private static final NamespacedKey NEXO_KEY_ALT = new NamespacedKey("nexo", "item_id");
    private static final NamespacedKey ADVANCEDITEMS_KEY = new NamespacedKey("advanceditems", "id");
    private static final NamespacedKey ITEMEDIT_KEY = new NamespacedKey("itemedit", "id");
    private static final NamespacedKey ECO_KEY = new NamespacedKey("ecoitems", "id");
    private static final NamespacedKey ECO_KEY_ALT = new NamespacedKey("eco", "id");
    private static final NamespacedKey MMO_TYPE_KEY = new NamespacedKey("mmoitems", "item_type");
    private static final NamespacedKey MMO_ID_KEY = new NamespacedKey("mmoitems", "item_id");
    private static final NamespacedKey MMO_TYPE_KEY_ALT = new NamespacedKey("mmoitems", "type");
    private static final NamespacedKey MMO_ID_KEY_ALT = new NamespacedKey("mmoitems", "id");
    private static final NamespacedKey ML_TYPE_KEY = new NamespacedKey("mythiclib", "item_type");
    private static final NamespacedKey ML_ID_KEY = new NamespacedKey("mythiclib", "item_id");
    private static final NamespacedKey ML_TYPE_KEY_ALT = new NamespacedKey("mythiclib", "type");
    private static final NamespacedKey ML_ID_KEY_ALT = new NamespacedKey("mythiclib", "id");
    private static final NamespacedKey PUB_TYPE_KEY = new NamespacedKey("public", "mmoitems_item_type");
    private static final NamespacedKey PUB_ID_KEY = new NamespacedKey("public", "mmoitems_item_id");
    private static final NamespacedKey MYTHIC_KEY = new NamespacedKey("mythicmobs", "type");
    private static final NamespacedKey MD_KEY = new NamespacedKey("mythicdrops", "tier");

    private final Map<String, NamedTextColor> itemCategories = new HashMap<>();
    private final Map<String, NamedTextColor> categoryColors = new HashMap<>();
    private final Map<String, Particle> categoryParticles = new HashMap<>();
    private final Map<String, String> categoryAnimTypes = new HashMap<>();
    private final Map<String, Sound> categorySounds = new HashMap<>();
    private final Map<UUID, TrackedItem> trackedItems = new HashMap<>();
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
    private final Map<UUID, Item> activeItems = new HashMap<>();
    private final Map<String, Set<UUID>> itemsByWorld = new HashMap<>();

    private final Map<Integer, Component> timerComponentCache = new HashMap<>();
    private final Map<UUID, Set<UUID>> visibleEntities = new HashMap<>();
    private final Map<String, org.bukkit.Particle.DustOptions> categoryDustOptions = new HashMap<>();
    private org.bukkit.Particle.DustOptions defaultDustOptions;
    private Set<UUID> globallyVisibleEntities = new HashSet<>();

    private static class SurfaceState {
        final double y;
        final Float yaw;
        final Float pitch;
        final double lastItemX;
        final double lastItemY;
        final double lastItemZ;

        SurfaceState(double y, Float yaw, Float pitch, double lastItemX, double lastItemY, double lastItemZ) {
            this.y = y;
            this.yaw = yaw;
            this.pitch = pitch;
            this.lastItemX = lastItemX;
            this.lastItemY = lastItemY;
            this.lastItemZ = lastItemZ;
        }
    }
    private final Map<UUID, SurfaceState> surfaceStates = new HashMap<>();
    private final Set<UUID> waterLogCache = new HashSet<>();
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
    private double holoFrontOffset;
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

    private record BeamConfig(float height, float width, boolean animate, boolean pulse) {
    }

    private final Map<UUID, BeamConfig> activeBeamConfigs = new HashMap<>();
    
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
        saveDefaultConfig();
        if (getConfig().getBoolean("settings.auto-update-config", true)) {
            ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"));
        }
        loadConfiguration();
        this.farmingKey = new NamespacedKey(this, "farming_symbol");
        this.sourceMobKey = new NamespacedKey(this, "source_mob");

        // Update Checker
        if (getConfig().getBoolean("settings.check-updates", true)) {
            new fr.skynex.lootglow.util.UpdateChecker(this, 134648).getVersion(version -> {
                if (isNewerVersion(getPluginMeta().getVersion(), version)) {
                    getLogger().warning("A new update is available (" + version
                            + ")! Download it here: https://www.spigotmc.org/resources/134648");
                } else if (getPluginMeta().getVersion().equals(version)) {
                    getLogger().info("The plugin is up to date.");
                }
            });
        }

        this.usePapi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        this.useWorldGuard = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        this.useMythic = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");

        initDatabase();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.FarmingListener(this), this);
        if (useMythic) {
            try {
                getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.MythicListener(this),
                        this);
            } catch (NoClassDefFoundError ignored) {
            }
        }
        // Enregistrement de la commande via le cycle de vie Paper (Modern Way)
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

        startParticleTask();
        startLightingTask();
        startLODTask();
        startGarbageCollectorTask();
        startGroupingTask();
        startFarmingTask();
        startUnifiedTickTask();
        setupPacketProvider();

        getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.LootContainerListener(this),
                this);

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

        // Cleanup propre des entities visuelles (toutes setPersistent(false), mais on
        // force au cas où)
        activeLabels.values().forEach(d -> {
            if (d != null && d.isValid())
                d.remove();
        });
        activeBeams.values().forEach(d -> {
            if (d != null && d.isValid())
                d.remove();
        });
        activeItemVisuals.values().forEach(d -> {
            if (d != null && d.isValid())
                d.remove();
        });
        activeShadows.values().forEach(d -> {
            if (d != null && d.isValid())
                d.remove();
        });
        activeCropSymbols.values().forEach(list -> list.forEach(d -> {
            if (d != null && d.isValid())
                d.remove();
        }));

        // Pour les RPG drops cachés, ré-afficher l'item vanilla aux joueurs avant
        // shutdown
        // sinon il reste invisible dans le worldsave côté client (résolu au prochain
        // chunk reload mais bon)
        for (Integer entityId : hiddenVanillaItems) {
            UUID uuid = entityIdMap.get(entityId);
            if (uuid == null)
                continue;
            Entity ent = Bukkit.getEntity(uuid);
            if (ent instanceof Item item && item.isValid()) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(item.getWorld())) {
                        p.showEntity(this, item);
                    }
                }
            }
        }

        // Clear de TOUTES les maps (sinon stale state si /reload du plugin)
        trackedItems.clear();
        activeLabels.clear();
        activeBeams.clear();
        activeItemVisuals.clear();
        activeCropSymbols.clear();
        activeLights.clear();
        entityIdMap.clear();
        hiddenVanillaItems.clear();
        itemSpawnTimes.clear();
        activeItems.clear();
        itemsByWorld.clear();
        timerComponentCache.clear();
        bounceCounts.clear();
        recentlyBounced.clear();
        waterLogCache.clear();
        surfaceStates.clear();
        lastFarmingScanLocations.clear();
    }

    public void loadConfiguration() {
        try {
            org.bukkit.scoreboard.Scoreboard scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
            for (org.bukkit.scoreboard.Team team : scoreboard.getTeams()) {
                if (team.getName().startsWith("LG_")) {
                    for (String entry : new java.util.ArrayList<>(team.getEntries())) {
                        team.removeEntry(entry);
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("Failed to clear LootGlow scoreboard teams: " + e.getMessage());
        }

        reloadConfig();
        loadMessages();

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
        waterLogCache.clear();
        surfaceStates.clear();
        lastFarmingScanLocations.clear();

        globallyVisibleEntities.clear();
        groupedItems.clear();
        groupLeaders.clear();
        openContainers.clear();
        flyingVisuals.clear();
        activeBeamConfigs.clear();
        recentlyBounced.clear();
        bounceCounts.clear();

        this.isEnabled = getConfig().getBoolean("settings.enabled", true);
        this.onlyPlayerDrops = getConfig().getBoolean("settings.only-player-drops", false);
        
        // Modern World Filtering System
        if (getConfig().contains("settings.worlds.mode")) {
            String mode = getConfig().getString("settings.worlds.mode", "BLACKLIST").toUpperCase();
            this.isWorldWhitelist = mode.equals("WHITELIST");
            this.filteredWorlds.addAll(getConfig().getStringList("settings.worlds.list"));
        } else {
            // Legacy Fallback
            this.isWorldWhitelist = false;
            this.filteredWorlds.addAll(getConfig().getStringList("settings.disabled-worlds"));
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
        this.economyKeys.clear();
        for (String keyStr : getConfig().getStringList("settings.economy.custom-keys")) {
            if (keyStr.contains(":")) {
                String[] parts = keyStr.split(":");
                this.economyKeys.add(new NamespacedKey(parts[0], parts[1]));
            }
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
        this.farmingCrops.clear();
        List<String> cropsList = getConfig().getStringList("settings.farming.crops");
        for (String crop : cropsList) {
            Material m = Material.matchMaterial(crop);
            if (m != null)
                farmingCrops.add(m);
        }

        // RPG Drops config
        this.rpgDropsEnabled = getConfig().getBoolean("settings.rpg-drops.enabled", true);
        this.rpgEnabledCategories = getConfig().getStringList("settings.rpg-drops.enabled-categories");
        this.rpgRotation = (float) Math.toRadians(getConfig().getDouble("settings.rpg-drops.rotation-angle", 90.0));
        this.rpgItemScale = (float) getConfig().getDouble("settings.rpg-drops.item-scale", 0.6);
        this.rpgBlockScale = (float) getConfig().getDouble("settings.rpg-drops.block-scale", 0.8);

        this.rpgForceFlatMaterials.clear();
        List<String> flatMats = getConfig().getStringList("settings.rpg-drops.force-flat-materials");
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

        this.rpgForceUprightMaterials.clear();
        List<String> uprightMats = getConfig().getStringList("settings.rpg-drops.force-upright-materials");
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
        this.holoFrontOffset = getConfig().getDouble("settings.holograms.front-offset", 0.0);
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
        this.bouncingBlockedBlocks.clear();
        for (String blockName : getConfig().getStringList("settings.spawn-animation.bouncing.blocked-blocks")) {
            Material m = Material.matchMaterial(blockName);
            if (m != null) {
                this.bouncingBlockedBlocks.add(m);
            }
        }

        // Aspiration config
        this.aspirationEnabled = getConfig().getBoolean("settings.aspiration.enabled", true);
        this.aspirationSpeed = getConfig().getDouble("settings.aspiration.speed", 0.15);

        String defColorStr = getConfig().getString("default-color", "WHITE");
        this.defaultColor = parseNamedColor(defColorStr);
        this.defaultDustOptions = new org.bukkit.Particle.DustOptions(
                org.bukkit.Color.fromRGB(defaultColor.red(), defaultColor.green(), defaultColor.blue()),
                (float) particleSize);

        if (getConfig().getConfigurationSection("categories") != null) {
            for (String key : getConfig().getConfigurationSection("categories").getKeys(false)) {
                String colorStr = getConfig().getString("categories." + key + ".color", "WHITE");
                NamedTextColor color = parseNamedColor(colorStr);
                categoryColors.put(key, color);
                if (color != null) {
                    categoryDustOptions.put(key, new org.bukkit.Particle.DustOptions(
                            org.bukkit.Color.fromRGB(color.red(), color.green(), color.blue()), (float) particleSize));
                }

                String partStr = getConfig().getString("categories." + key + ".particle");
                Particle particle = null;
                if (partStr != null) {
                    try {
                        org.bukkit.NamespacedKey particleKey = org.bukkit.NamespacedKey
                                .minecraft(partStr.toLowerCase());
                        particle = org.bukkit.Registry.PARTICLE_TYPE.get(particleKey);
                    } catch (Exception ignored) {
                    }
                }

                String soundStr = getConfig().getString("categories." + key + ".sound");
                Sound sound = null;
                if (soundStr != null) {
                    sound = parseSound(soundStr);
                }

                int lightLevel = getConfig().getInt("categories." + key + ".light-level", 0);
                categoryLights.put(key, lightLevel);

                boolean glowEnabled = getConfig().getBoolean("categories." + key + ".glow", true);
                categoryGlow.put(key, glowEnabled);

                if (getConfig().getConfigurationSection("categories." + key + ".display-names") != null) {
                    for (String itemKey : getConfig().getConfigurationSection("categories." + key + ".display-names")
                            .getKeys(false)) {
                        String raw = getConfig().getString("categories." + key + ".display-names." + itemKey);
                        if (raw != null)
                            displayNameOverridesCache.put(itemKey.toUpperCase(), miniMessage.deserialize(raw));
                    }
                }

                for (String material : getConfig().getStringList("categories." + key + ".items")) {
                    String mat = material.toUpperCase();
                    itemCategories.put(mat, color);
                    categoryNames.put(mat, key);
                    if (particle != null)
                        categoryParticles.put(mat, particle);

                    String catAnim = getConfig().getString("categories." + key + ".particle-animation",
                            particleAnimType);
                    categoryAnimTypes.put(key, catAnim);

                    if (sound != null)
                        categorySounds.put(mat, sound);
                }
            }
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
        FoliaScheduler.runTimer(this, () -> {
            if (!isEnabled || !particlesEnabled)
                return;

            particleTick++;
            double partDistSq = lodPartDistSq;

            // Optimisation : cache les positions des items une seule fois par tick, groupés par monde
            record CachedItemLoc(UUID uuid, double x, double y, double z, World world, Particle particle, String category) {}
            java.util.Map<World, java.util.List<CachedItemLoc>> itemsByWorldMap = new java.util.HashMap<>();
            for (java.util.Map.Entry<UUID, Item> e : activeItems.entrySet()) {
                Item item = e.getValue();
                if (item == null || item.isDead() || !item.isValid()) continue;
                Particle particle = itemParticlesCache.get(e.getKey());
                if (particle == null) continue;
                String category = itemCategoriesCache.get(e.getKey());
                Location loc = item.getLocation();
                CachedItemLoc cached = new CachedItemLoc(e.getKey(), loc.getX(), loc.getY(), loc.getZ(), item.getWorld(), particle, category);
                itemsByWorldMap.computeIfAbsent(item.getWorld(), w -> new java.util.ArrayList<>()).add(cached);
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (hiddenVisuals.contains(p.getUniqueId()))
                    continue;

                double px = p.getX();
                double py = p.getY();
                double pz = p.getZ();
                World pWorld = p.getWorld();

                java.util.List<CachedItemLoc> worldItems = itemsByWorldMap.get(pWorld);
                if (worldItems == null || worldItems.isEmpty()) continue;

                for (CachedItemLoc ci : worldItems) {
                    double dx = px - ci.x();
                    double dy = py - ci.y();
                    double dz = pz - ci.z();
                    if ((dx * dx + dy * dy + dz * dz) >= partDistSq) continue;

                    Particle particle = ci.particle();
                    String category = ci.category();
                    double xCoord = ci.x();
                    double yCoord = ci.y() + 0.2;
                    double zCoord = ci.z();

                    Object data = null;
                    if (particle.getDataType() == org.bukkit.Particle.DustOptions.class) {
                        data = category != null ? categoryDustOptions.getOrDefault(category, defaultDustOptions)
                                : defaultDustOptions;
                    }

                    String animType = (category != null)
                            ? categoryAnimTypes.getOrDefault(category, particleAnimType)
                            : particleAnimType;

                    if (animType.equalsIgnoreCase("CIRCLE")) {
                        double radius = 0.4;
                        double x = Math.cos(particleTick * 0.2) * radius;
                        double z = Math.sin(particleTick * 0.2) * radius;
                        p.spawnParticle(particle, xCoord + x, yCoord, zCoord + z, 1, 0, 0, 0, 0, data);
                    } else if (animType.equalsIgnoreCase("SPIRAL")) {
                        double radius = 0.3;
                        double x = Math.cos(particleTick * 0.3) * radius;
                        double z = Math.sin(particleTick * 0.3) * radius;
                        double yOffset = (particleTick % 20) * 0.05;
                        p.spawnParticle(particle, xCoord + x, yCoord + yOffset, zCoord + z, 1, 0, 0, 0, 0, data);
                    } else {
                        p.spawnParticle(particle, xCoord, yCoord, zCoord, 1, 0.1, 0.1, 0.1, 0.02, data);
                    }
                }
            }
        }, 20L, (long) particlesFrequency);
    }

    private void startLightingTask() {
        int interval = getConfig().getInt("settings.lighting.update-interval", 5);
        FoliaScheduler.runTimer(this, () -> {
            if (!isEnabled || !lightingEnabled)
                return;

            // Remove lights for dead items (Optimisé : utilise activeItems au lieu de Bukkit.getEntity)
            activeLights.keySet().removeIf(uuid -> {
                Item ent = activeItems.get(uuid);
                if (ent == null || ent.isDead() || !ent.isValid()) {
                    Location loc = activeLights.get(uuid);
                    if (loc != null) {
                        org.bukkit.block.data.BlockData blockData = loc.getBlock().getBlockData();
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getWorld().equals(loc.getWorld())) {
                                p.sendBlockChange(loc, blockData);
                            }
                        }
                    }
                    return true;
                }
                return false;
            });

            // Optimisé : utilise activeItems (O(1)) au lieu de Bukkit.getEntity (scan global)
            for (java.util.Map.Entry<UUID, Item> lightEntry : activeItems.entrySet()) {
                UUID uuid = lightEntry.getKey();
                Item item = lightEntry.getValue();
                if (item == null || !item.isValid())
                    continue;

                String category = itemCategoriesCache.get(uuid);
                if (category == null)
                    continue;

                int lightLevel = categoryLights.getOrDefault(category, 0);
                if (lightLevel <= 0)
                    continue;

                org.bukkit.block.Block block = item.getLocation().getBlock();
                Location currentLoc = block.getLocation();
                Location oldLoc = activeLights.get(uuid);

                if (oldLoc != null && oldLoc.equals(currentLoc))
                    continue;

                // Move light
                if (oldLoc != null) {
                    org.bukkit.block.data.BlockData oldBlockData = oldLoc.getBlock().getBlockData();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getWorld().equals(oldLoc.getWorld())) {
                            p.sendBlockChange(oldLoc, oldBlockData);
                        }
                    }
                }

                // Only place light in air or water
                Material blockType = block.getType();
                if (blockType.isAir() || blockType == Material.WATER) {
                    org.bukkit.block.data.type.Light lightData = cachedLightBlockData[Math.max(0,
                            Math.min(lightLevel, 15))];
                    if (lightData != null) {
                        if (blockType == Material.WATER) {
                            org.bukkit.block.data.type.Light waterloggedLight = (org.bukkit.block.data.type.Light) lightData
                                    .clone();
                            waterloggedLight.setWaterlogged(true);
                            lightData = waterloggedLight;
                        }

                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getWorld().equals(currentLoc.getWorld())) {
                                p.sendBlockChange(currentLoc, lightData);
                            }
                        }
                        activeLights.put(uuid, currentLoc);
                    }
                } else {
                    activeLights.remove(uuid);
                }
            }
        }, 20L, interval);
    }

    private void loadMessages() {
        java.io.File messagesFile = new java.io.File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        } else {
            ConfigUpdater.update(this, "messages.yml", messagesFile);
        }
        this.messagesConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(messagesFile);

        // Caching raw strings to avoid YAML lookups in high-frequency tasks
        this.rawPrefix = messagesConfig.getString("prefix", "");
        this.rawAmountFormat = messagesConfig.getString("item-amount-format", " <gray>(x<amount>)</gray>");
        this.rawTimerFormat = messagesConfig.getString("item-timer-format", " <gray>(<time>s)</gray>");
        this.rawOwnerFormat = messagesConfig.getString("owner-format",
                "<newline><gray>Propriété de</gray> <white><owner></white>");
        this.rawBundleFormat = messagesConfig.getString("bundle-format",
                "<gradient:gold:white>[Sac de Butin]</gradient> <gray>(x<count> objets)</gray>");

        // Pre-calculate timer components (0-305 seconds)
        timerComponentCache.clear();
        for (int i = 0; i <= 305; i++) {
            timerComponentCache.put(i, miniMessage.deserialize(rawTimerFormat.replace("<time>", String.valueOf(i))));
        }
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, null);
    }

    public void sendMessage(CommandSender sender, String key, @Nullable Map<String, String> placeholders) {
        if (messagesConfig.isList(key)) {
            for (String line : messagesConfig.getStringList(key)) {
                sendProcessedMessage(sender, line, placeholders);
            }
        } else {
            String msg = messagesConfig.getString(key, "Missing message: " + key);
            sendProcessedMessage(sender, msg, placeholders);
        }
    }

    private void sendProcessedMessage(CommandSender sender, String msg, @Nullable Map<String, String> placeholders) {
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("<" + entry.getKey() + ">", entry.getValue());
            }
        }
        String fullMsg = msg.replace("<prefix>", rawPrefix);
        sender.sendMessage(miniMessage.deserialize(fullMsg));
    }

    private void setupTeams() {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            for (NamedTextColor color : NamedTextColor.NAMES.values()) {
                String teamName = "LG_" + color.toString().toUpperCase();
                Team team = scoreboard.getTeam(teamName);
                if (team == null)
                    team = scoreboard.registerNewTeam(teamName);
                team.color(color);
            }
        } catch (Throwable t) {
            getLogger().warning("Failed to setup LootGlow scoreboard teams (Scoreboard team registration may not be supported on this server software): " + t.getMessage());
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
        if (!isEnabled || item == null)
            return;
        if (!isWorldAllowed(item.getWorld().getName()))
            return;
        if (isInBlockedRegion(item.getLocation()))
            return;

        ItemStack stack = item.getItemStack();
        String customId = getInternalId(stack);
        String matName = stack.getType().name();

        if (getConfig().getBoolean("settings.debug", false)) {
            getLogger().info("[Debug] Processing item " + item.getUniqueId() + " (Material: " + matName + ", CustomID: " + customId + ")");
            if (stack.hasItemMeta()) {
                PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
                if (!pdc.getKeys().isEmpty()) {
                    getLogger().info("[Debug]   PDC Keys:");
                    for (NamespacedKey key : pdc.getKeys()) {
                        getLogger().info("[Debug]     - " + key.toString());
                    }
                }
            }
        }

        String category = null;
        NamedTextColor color = defaultColor;

        // Check for Economy first
        Double moneyAmount = getMoneyAmount(stack);
        if (moneyAmount != null) {
            category = "ECONOMY";
            color = economyColor;
            itemMoneyAmounts.put(item.getUniqueId(), moneyAmount);

            // Play sound for money drop
            if (playAnimation && economySound != null) {
                item.getWorld().playSound(item.getLocation(), economySound, 1.0f, 1.2f);
            }
        }

        if (category == null) {
            // Priority: PDC Custom ID > Material Name
            if (customId != null && itemCategories.containsKey(customId)) {
                category = categoryNames.get(customId);
                color = itemCategories.get(customId);
            } else if (itemCategories.containsKey(matName)) {
                category = categoryNames.get(matName);
                color = itemCategories.get(matName);
            } else if (customId != null && item.getItemStack().hasItemMeta()) {
                // Smart Rarity Detection for MMOItems/Mythic if not explicitly in config
                PersistentDataContainer pdc = item.getItemStack().getItemMeta().getPersistentDataContainer();

                // MMOItems Tier detection
                NamespacedKey tierKey = new NamespacedKey("mmoitems", "tier");
                NamespacedKey tierKeyAlt = new NamespacedKey("mmoitems", "item_tier");
                String tier = null;

                if (pdc.has(tierKey, PersistentDataType.STRING)) {
                    tier = pdc.get(tierKey, PersistentDataType.STRING);
                } else if (pdc.has(tierKeyAlt, PersistentDataType.STRING)) {
                    tier = pdc.get(tierKeyAlt, PersistentDataType.STRING);
                }

                if (tier != null) {
                    tier = tier.toLowerCase();
                    if (getConfig().getBoolean("settings.debug", false)) {
                        getLogger().info("[Debug] Detected MMOItems Tier: " + tier);
                    }
                    // Check if a category with this name exists (e.g. "rare", "epic")
                    if (getConfig().contains("categories." + tier)) {
                        category = tier;
                        String colorStr = getConfig().getString("categories." + tier + ".color", "WHITE");
                        color = parseNamedColor(colorStr);
                    }
                }

                // MythicDrops Tier detection (Smart matching)
                NamespacedKey mdKey = new NamespacedKey("mythicdrops", "tier");
                if (pdc.has(mdKey, PersistentDataType.STRING)) {
                    String mdTier = pdc.get(mdKey, PersistentDataType.STRING).toLowerCase();
                    if (getConfig().contains("categories." + mdTier)) {
                        category = mdTier;
                        String colorStr = getConfig().getString("categories." + mdTier + ".color", "WHITE");
                        color = parseNamedColor(colorStr);
                    }
                }

                // Fallback: Name-Color Auto Matching (The "Magic" part)
                if (category == null && item.getItemStack().hasItemMeta()) {
                    Component displayName = item.getItemStack().getItemMeta().displayName();
                    if (displayName != null) {
                        net.kyori.adventure.text.format.TextColor nameColor = displayName.color();

                        // If component color is null, try to extract it from legacy text
                        if (nameColor == null) {
                            String legacyName = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                                    .legacySection().serialize(displayName);

                            if (legacyName.contains("§")) {
                                int index = legacyName.indexOf("§");
                                if (index != -1 && index + 1 < legacyName.length()) {
                                    char code = legacyName.charAt(index + 1);
                                    // Mapping legacy to adventure
                                    switch (code) {
                                        case '0':
                                            nameColor = NamedTextColor.BLACK;
                                            break;
                                        case '1':
                                            nameColor = NamedTextColor.DARK_BLUE;
                                            break;
                                        case '2':
                                            nameColor = NamedTextColor.DARK_GREEN;
                                            break;
                                        case '3':
                                            nameColor = NamedTextColor.DARK_AQUA;
                                            break;
                                        case '4':
                                            nameColor = NamedTextColor.DARK_RED;
                                            break;
                                        case '5':
                                            nameColor = NamedTextColor.DARK_PURPLE;
                                            break;
                                        case '6':
                                            nameColor = NamedTextColor.GOLD;
                                            break;
                                        case '7':
                                            nameColor = NamedTextColor.GRAY;
                                            break;
                                        case '8':
                                            nameColor = NamedTextColor.DARK_GRAY;
                                            break;
                                        case '9':
                                            nameColor = NamedTextColor.BLUE;
                                            break;
                                        case 'a':
                                            nameColor = NamedTextColor.GREEN;
                                            break;
                                        case 'b':
                                            nameColor = NamedTextColor.AQUA;
                                            break;
                                        case 'c':
                                            nameColor = NamedTextColor.RED;
                                            break;
                                        case 'd':
                                            nameColor = NamedTextColor.LIGHT_PURPLE;
                                            break;
                                        case 'e':
                                            nameColor = NamedTextColor.YELLOW;
                                            break;
                                        case 'f':
                                            nameColor = NamedTextColor.WHITE;
                                            break;
                                    }
                                }
                            }
                        }

                        if (nameColor != null) {
                            NamedTextColor nearest = NamedTextColor.nearestTo(nameColor);
                            if (!nearest.equals(NamedTextColor.WHITE)) {
                                color = nearest;
                            }
                        }
                    }
                }
            }
        }

        if (category != null) {
            itemCategoriesCache.put(item.getUniqueId(), category);

            // Cache particle for this item (Performance optimization)
            Particle part = categoryParticles.get(customId != null ? customId : matName);
            if (part == null) {
                // Try to get particle from category settings if not found by item ID
                String partStr = getConfig().getString("categories." + category + ".particle");
                if (partStr != null) {
                    try {
                        NamespacedKey particleKey = NamespacedKey.minecraft(partStr.toLowerCase());
                        part = org.bukkit.Registry.PARTICLE_TYPE.get(particleKey);
                    } catch (Exception ignored) {
                    }
                }
            }

            if (part != null)
                itemParticlesCache.put(item.getUniqueId(), part);

            if (getConfig().getBoolean("settings.debug", false)) {
                getLogger().info("[Debug] Final category for " + item.getUniqueId() + ": " + category);
            }
        }

        final NamedTextColor finalColor = color;
        final String finalCategory = category;

        // Custom despawn time adjustment
        if (despawnTime > 0 && despawnTime < 300) {
            item.setTicksLived(Math.max(1, 6000 - (despawnTime * 20)));
        }

        entityIdMap.put(item.getEntityId(), item.getUniqueId());
        activeItems.put(item.getUniqueId(), item);
        itemsByWorld.computeIfAbsent(item.getWorld().getName(), k -> new HashSet<>()).add(item.getUniqueId());

        boolean isRpgDrop = rpgDropsEnabled && (rpgEnabledCategories.isEmpty()
                || (finalCategory != null && rpgEnabledCategories.contains(finalCategory.toLowerCase())));
        boolean shouldGlow = categoryGlow.getOrDefault(finalCategory, defaultGlow);
        if (!isRpgDrop) {
            if (shouldGlow) {
                item.setGlowing(true);
            }
        } else {
            // Paper native hiding : empêche l'entity tracker d'envoyer le SPAWN_ENTITY
            // packet à TOUS les joueurs, dès le tick courant. Plus fiable que hideEntity
            // post-spawn car ça court-circuite le tracker AVANT qu'il ne décide quoi
            // envoyer.
            // Évite la race condition où le packet est envoyé avant l'interception
            // ProtocolLib.
            try {
                item.setVisibleByDefault(false);
            } catch (NoSuchMethodError ignored) {
                // Fallback pour versions Paper antérieures
            }
            hiddenVanillaItems.add(item.getEntityId());
        }

        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = "LG_" + finalColor.toString().toUpperCase();
            Team team = scoreboard.getTeam(teamName);
            if (team != null)
                team.addEntry(item.getUniqueId().toString());
        } catch (Throwable ignored) {}

        Sound sound = categorySounds.get(customId);
        if (sound == null)
            sound = categorySounds.get(matName);
        if (sound == null && finalCategory != null) {
            String soundStr = getConfig().getString("categories." + finalCategory + ".sound");
            if (soundStr != null) {
                sound = parseSound(soundStr);
            }
        }

        if (playAnimation && sound != null) {
            item.getWorld().playSound(item.getLocation(), sound, 1.0f, 1.0f);
            if (finalColor.equals(NamedTextColor.GOLD)) {
                item.getWorld().getNearbyPlayers(item.getLocation(), 15)
                        .forEach(p -> sendMessage(p, "legendary-found"));
            }
        }

        if (holoEnabled) {
            // Skip hologram for uncategorized items if the option is enabled
            if (holoHideUncategorized && finalCategory == null) {
                // Still record spawn time so timer works if category is assigned later
            } else {
                if (!itemSpawnTimes.containsKey(item.getUniqueId())) {
                    itemSpawnTimes.put(item.getUniqueId(), System.currentTimeMillis());
                }
                // Optimization: Pre-calculate and cache the base name (expensive
                // PAPI/MiniMessage part)
                baseNameCache.put(item.getUniqueId(), calculateBaseName(item, finalColor));
                updateHologram(item, finalColor);

                if (protectionEnabled) {
                    FoliaScheduler.runLater(this, () -> {
                        if (item.isValid())
                            updateHologram(item, finalColor);
                    }, protectionDuration * 20L);
                }
            }
        }

        // spawnBeam moved into the delayed task below for better sync

        if (isRpgDrop) {
            // Masquer immédiatement l'item vanilla pour tous les joueurs.
            // Le display n'existe pas encore mais l'item est déjà invisible côté serveur
            // via setVisibleByDefault(false) + hideEntity par joueur.
            broadcastRpgDropVisibility(item);

            // Délai d'1 tick avant de créer l'ItemDisplay.
            // Raison principale : lors d'un stack immédiat (ItemMergeEvent dans le même
            // tick),
            // l'item absorbé sera déjà invalide (isValid() == false) → le display n'est
            // jamais créé → plus de flash du display qui apparaît/disparaît au stack.
            // Pour les items qui survivent (~50ms imperceptibles), le display apparaît au
            // tick suivant.
            FoliaScheduler.runLater(this, () -> {
                if (!item.isValid() || !activeItems.containsKey(item.getUniqueId()))
                    return;
                spawnItemVisual(item, finalCategory, finalColor);
                if (shadowsEnabled)
                    spawnShadow(item);
                if (beamsEnabled && finalCategory != null && beamCategories.contains(finalCategory.toLowerCase())) {
                    spawnBeam(item, finalCategory, finalColor);
                }
                broadcastRpgDropVisibility(item);

                FoliaScheduler.runLater(this, () -> {
                    if (!item.isValid())
                        return;
                    try {
                        item.setVisibleByDefault(false);
                    } catch (NoSuchMethodError ignored) {
                    }
                    broadcastRpgDropVisibility(item);
                }, 1L);
            }, 1L);
        }

        if (playAnimation && spawnAnimEnabled) {
            playSpawnAnimation(item, customId != null ? customId : matName);
            if (bouncingEnabled) {
                bounceCounts.put(item.getUniqueId(), 0);
            }
        }
    }

    private static Class<?> nbtItemClass = null;
    private static java.lang.reflect.Method nbtItemResolver = null;
    private static Object nbtItemResolverTarget = null;
    private static java.lang.reflect.Constructor<?> nbtItemConstructor = null;
    private static java.lang.reflect.Method getStringMethod = null;
    private static java.lang.reflect.Method getTypeMethod = null;
    private static boolean reflectionInitialized = false;

    private static void initReflection(boolean debug, java.util.logging.Logger log) {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        // Must use the target plugin's own ClassLoader — LootGlow's classloader
        // cannot see other plugins' classes unless they are in softdepend.
        ClassLoader mlLoader = null;
        ClassLoader mmoLoader = null;

        org.bukkit.plugin.Plugin mlPlugin = Bukkit.getPluginManager().getPlugin("MythicLib");
        if (mlPlugin != null) mlLoader = mlPlugin.getClass().getClassLoader();

        org.bukkit.plugin.Plugin mmoPlugin = Bukkit.getPluginManager().getPlugin("MMOItems");
        if (mmoPlugin != null) mmoLoader = mmoPlugin.getClass().getClassLoader();

        if (mlLoader != null) {
            try {
                nbtItemClass = Class.forName("io.lumine.mythic.lib.api.item.NBTItem", true, mlLoader);
            } catch (ClassNotFoundException ignored) {}
        }
        if (nbtItemClass == null && mmoLoader != null) {
            try {
                nbtItemClass = Class.forName("net.Indyuce.mmoitems.api.util.NBTItem", true, mmoLoader);
            } catch (ClassNotFoundException e2) {
                try {
                    nbtItemClass = Class.forName("net.Indyuce.mmoitems.api.item.NBTItem", true, mmoLoader);
                } catch (ClassNotFoundException ignored) {}
            }
        }

        if (nbtItemClass == null) {
            if (debug) log.warning("[Debug] [Reflection] NBTItem class not found in MythicLib/MMOItems classloaders (mlLoader=" + mlLoader + ", mmoLoader=" + mmoLoader + ").");
            return;
        }

        if (debug) log.info("[Debug] [Reflection] NBTItem class found: " + nbtItemClass.getName());

        try {
            getStringMethod = nbtItemClass.getMethod("getString", String.class);
        } catch (NoSuchMethodException ignored) {}
        try {
            getTypeMethod = nbtItemClass.getMethod("getType");
            if (debug) log.info("[Debug] [Reflection] getType() method found.");
        } catch (NoSuchMethodException ignored) {}

        // Try to resolve how to obtain NBTItem from ItemStack
        try {
            Class<?> mythicLibClass = Class.forName("io.lumine.mythic.lib.MythicLib");
            Object plugin = mythicLibClass.getField("plugin").get(null);
            Object version = plugin.getClass().getMethod("getVersion").invoke(plugin);
            Object wrapper = version.getClass().getMethod("getWrapper").invoke(version);
            nbtItemResolver = wrapper.getClass().getMethod("getNBTItem", ItemStack.class);
            nbtItemResolverTarget = wrapper;
            if (debug) log.info("[Debug] [Reflection] Resolver: MythicLib version wrapper (getNBTItem)");
        } catch (Exception e) {
            if (debug) log.info("[Debug] [Reflection] Wrapper not available (" + e.getMessage() + "), trying static get(ItemStack)");
            try {
                nbtItemResolver = nbtItemClass.getMethod("get", ItemStack.class);
                nbtItemResolverTarget = null;
                if (debug) log.info("[Debug] [Reflection] Resolver: static NBTItem.get(ItemStack)");
            } catch (NoSuchMethodException e2) {
                if (debug) log.info("[Debug] [Reflection] Static get() not found, trying constructor");
                try {
                    nbtItemConstructor = nbtItemClass.getConstructor(ItemStack.class);
                    if (debug) log.info("[Debug] [Reflection] Resolver: new NBTItem(ItemStack) constructor");
                } catch (NoSuchMethodException ignored) {
                    if (debug) log.warning("[Debug] [Reflection] No valid NBTItem resolver found! MMOItems reflection detection disabled.");
                }
            }
        }
    }

    private String getMMOItemsIdFromReflection(ItemStack item) {
        boolean debug = getConfig().getBoolean("settings.debug", false);
        initReflection(debug, getLogger());
        if (nbtItemClass == null) {
            if (debug && Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
                getLogger().warning("[Debug] [Reflection] MMOItems is enabled but NBT class was not found.");
            }
            return null;
        }

        if (nbtItemResolver == null && nbtItemConstructor == null) {
            if (debug) getLogger().warning("[Debug] [Reflection] No NBTItem resolver available, skipping reflection detection.");
            return null;
        }

        try {
            Object nbtItem = null;
            if (nbtItemResolver != null) {
                nbtItem = nbtItemResolver.invoke(nbtItemResolverTarget, item);
            } else {
                nbtItem = nbtItemConstructor.newInstance(item);
            }

            if (nbtItem == null) {
                if (debug) getLogger().info("[Debug] [Reflection] NBTItem resolved to null for this item.");
                return null;
            }

            String type = null;
            if (getTypeMethod != null) {
                try {
                    type = (String) getTypeMethod.invoke(nbtItem);
                    if (debug) getLogger().info("[Debug] [Reflection] getType() returned: " + type);
                } catch (Exception ignored) {}
            }
            if (type == null || type.trim().isEmpty()) {
                type = getTagValue(nbtItem, getStringMethod, "MMOITEMS_ITEM_TYPE", "mmoitems_item_type", "item_type", "type", "TYPE");
                if (debug) getLogger().info("[Debug] [Reflection] getString type fallback returned: " + type);
            }

            String id = getTagValue(nbtItem, getStringMethod, "MMOITEMS_ITEM_ID", "mmoitems_item_id", "item_id", "id", "ID");
            if (debug) getLogger().info("[Debug] [Reflection] getString id returned: " + id);

            if (type != null && !type.isEmpty() && id != null && !id.isEmpty()) {
                if (debug) getLogger().info("[Debug] [Reflection] Matched MMOItem -> type=" + type + ", id=" + id);
                return "MMOITEMS:" + type.toUpperCase() + ":" + id.toUpperCase();
            }

            if (debug) getLogger().info("[Debug] [Reflection] No MMOItems tags found via NBT reflection (type=" + type + ", id=" + id + ").");
        } catch (Exception e) {
            if (debug) {
                getLogger().warning("[Debug] [Reflection] Exception during NBT read: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    private String getTagValue(Object nbtItem, java.lang.reflect.Method getStringMethod, String... keys) {
        if (getStringMethod == null) return null;
        for (String key : keys) {
            try {
                String val = (String) getStringMethod.invoke(nbtItem, key);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String getInternalId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        // Oraxen
        if (pdc.has(ORAXEN_KEY, PersistentDataType.STRING))
            return "ORAXEN:" + pdc.get(ORAXEN_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(ORAXEN_KEY_ALT, PersistentDataType.STRING))
            return "ORAXEN:" + pdc.get(ORAXEN_KEY_ALT, PersistentDataType.STRING).toUpperCase();

        // ItemsAdder
        if (pdc.has(ITEMSADDER_KEY, PersistentDataType.STRING))
            return "ITEMSADDER:" + pdc.get(ITEMSADDER_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(ITEMSADDER_KEY_ALT, PersistentDataType.STRING))
            return "ITEMSADDER:" + pdc.get(ITEMSADDER_KEY_ALT, PersistentDataType.STRING).toUpperCase();

        // Nexo
        if (pdc.has(NEXO_KEY, PersistentDataType.STRING))
            return "NEXO:" + pdc.get(NEXO_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(NEXO_KEY_ALT, PersistentDataType.STRING))
            return "NEXO:" + pdc.get(NEXO_KEY_ALT, PersistentDataType.STRING).toUpperCase();

        // AdvancedItems
        if (pdc.has(ADVANCEDITEMS_KEY, PersistentDataType.STRING))
            return "ADVANCEDITEMS:" + pdc.get(ADVANCEDITEMS_KEY, PersistentDataType.STRING).toUpperCase();

        // ItemEdit
        if (pdc.has(ITEMEDIT_KEY, PersistentDataType.STRING))
            return "ITEMEDIT:" + pdc.get(ITEMEDIT_KEY, PersistentDataType.STRING).toUpperCase();

        // EcoItems & Auxilium
        if (pdc.has(ECO_KEY, PersistentDataType.STRING))
            return "ECOITEMS:" + pdc.get(ECO_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(ECO_KEY_ALT, PersistentDataType.STRING))
            return "ECOITEMS:" + pdc.get(ECO_KEY_ALT, PersistentDataType.STRING).toUpperCase();

        // MMOItems (Type + ID) - Checking multiple variants for compatibility
        boolean debugMmo = getConfig().getBoolean("settings.debug", false);
        if (debugMmo) getLogger().info("[Debug] [MMOItems PDC] Checking PDC for MMOItems keys...");

        if (pdc.has(MMO_TYPE_KEY, PersistentDataType.STRING) && pdc.has(MMO_ID_KEY, PersistentDataType.STRING)) {
            if (debugMmo) getLogger().info("[Debug] [MMOItems PDC] Match on mmoitems:item_type + mmoitems:item_id");
            return "MMOITEMS:" + pdc.get(MMO_TYPE_KEY, PersistentDataType.STRING).toUpperCase() + ":"
                    + pdc.get(MMO_ID_KEY, PersistentDataType.STRING).toUpperCase();
        } else if (pdc.has(MMO_TYPE_KEY_ALT, PersistentDataType.STRING)
                && pdc.has(MMO_ID_KEY_ALT, PersistentDataType.STRING)) {
            if (debugMmo) getLogger().info("[Debug] [MMOItems PDC] Match on mmoitems:type + mmoitems:id");
            return "MMOITEMS:" + pdc.get(MMO_TYPE_KEY_ALT, PersistentDataType.STRING).toUpperCase() + ":"
                    + pdc.get(MMO_ID_KEY_ALT, PersistentDataType.STRING).toUpperCase();
        } else if (debugMmo) {
            getLogger().info("[Debug] [MMOItems PDC] No match in namespace 'mmoitems' (item_type=" + pdc.has(MMO_TYPE_KEY, PersistentDataType.STRING) + ", item_id=" + pdc.has(MMO_ID_KEY, PersistentDataType.STRING) + ")");
        }

        // Variant 2: namespace "mythiclib"
        if (pdc.has(ML_TYPE_KEY, PersistentDataType.STRING) && pdc.has(ML_ID_KEY, PersistentDataType.STRING)) {
            if (debugMmo) getLogger().info("[Debug] [MMOItems PDC] Match on mythiclib:item_type + mythiclib:item_id");
            return "MMOITEMS:" + pdc.get(ML_TYPE_KEY, PersistentDataType.STRING).toUpperCase() + ":"
                    + pdc.get(ML_ID_KEY, PersistentDataType.STRING).toUpperCase();
        } else if (pdc.has(ML_TYPE_KEY_ALT, PersistentDataType.STRING)
                && pdc.has(ML_ID_KEY_ALT, PersistentDataType.STRING)) {
            if (debugMmo) getLogger().info("[Debug] [MMOItems PDC] Match on mythiclib:type + mythiclib:id");
            return "MMOITEMS:" + pdc.get(ML_TYPE_KEY_ALT, PersistentDataType.STRING).toUpperCase() + ":"
                    + pdc.get(ML_ID_KEY_ALT, PersistentDataType.STRING).toUpperCase();
        } else if (debugMmo) {
            getLogger().info("[Debug] [MMOItems PDC] No match in namespace 'mythiclib' (item_type=" + pdc.has(ML_TYPE_KEY, PersistentDataType.STRING) + ", item_id=" + pdc.has(ML_ID_KEY, PersistentDataType.STRING) + ")");
        }

        // Variant 3: namespace "public" (for legacy NBT tags in 1.20.5+ custom_data mapping)
        if (pdc.has(PUB_TYPE_KEY, PersistentDataType.STRING) && pdc.has(PUB_ID_KEY, PersistentDataType.STRING)) {
            if (debugMmo) getLogger().info("[Debug] [MMOItems PDC] Match on public:mmoitems_item_type + public:mmoitems_item_id");
            return "MMOITEMS:" + pdc.get(PUB_TYPE_KEY, PersistentDataType.STRING).toUpperCase() + ":"
                    + pdc.get(PUB_ID_KEY, PersistentDataType.STRING).toUpperCase();
        } else if (debugMmo) {
            getLogger().info("[Debug] [MMOItems PDC] No match in namespace 'public'");
        }

        // Variant 4: Reflection-based NBT fallback using MMOItems / MythicLib native NBTItem
        if (debugMmo) getLogger().info("[Debug] [MMOItems PDC] All PDC variants exhausted, falling back to NBT reflection...");
        String mmoIdReflection = getMMOItemsIdFromReflection(item);
        if (mmoIdReflection != null) {
            return mmoIdReflection;
        }
        if (debugMmo) getLogger().info("[Debug] [MMOItems PDC] Reflection also returned null. Item is not detected as MMOItem.");

        // MythicItems (Direct)
        if (pdc.has(MYTHIC_KEY, PersistentDataType.STRING)) {
            return "MYTHIC:" + pdc.get(MYTHIC_KEY, PersistentDataType.STRING).toUpperCase();
        }

        // MythicDrops
        if (pdc.has(MD_KEY, PersistentDataType.STRING)) {
            return "MYTHICDROPS:" + pdc.get(MD_KEY, PersistentDataType.STRING).toUpperCase();
        }

        return null;
    }

    private void playSpawnAnimation(Item item, String id) {
        org.bukkit.persistence.PersistentDataContainer pdc = item.getItemStack().getItemMeta().getPersistentDataContainer();
        if (pdc.has(sourceMobKey, org.bukkit.persistence.PersistentDataType.STRING)) {
            // Fountain Animation (Boss Loot Piñata)
            item.setVelocity(new Vector(Math.random() * 0.4 - 0.2, jumpForce * 2.5, Math.random() * 0.4 - 0.2));
            Particle particle = categoryParticles.get(id);
            if (particle == null) particle = Particle.TOTEM_OF_UNDYING;
            item.getWorld().spawnParticle(particle, item.getLocation(), burstAmount * 2, 0.1, 0.1, 0.1, 0.2);
        } else {
            // Standard Pop Animation
            item.setVelocity(item.getVelocity().add(new Vector(0, jumpForce, 0)));
            Particle particle = categoryParticles.get(id);
            if (particle != null) {
                item.getWorld().spawnParticle(particle, item.getLocation().add(0, 0.2, 0), burstAmount, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }

    public void updateHologram(Item item, NamedTextColor color) {
        if (!holoEnabled || item == null)
            return;
        UUID uuid = item.getUniqueId();
        String cat = itemCategoriesCache.get(uuid);
        if (holoHideUncategorized && cat == null)
            return;

        TextDisplay display = activeLabels.get(uuid);
        if (display == null || !display.isValid()) {
            if (display != null)
                activeLabels.remove(uuid);
            spawnHologram(item, color);
            return;
        }

        // Optimization: don't update if it's a group leader (the grouping task handles
        // it)
        boolean isGroupLeader = groupLeaders.containsKey(uuid);
        if (isGroupLeader)
            return;

        // Ultra-fast state check: seconds | count | groupSize (0 here)
        int currentSec = (6000 - item.getTicksLived()) / 20;
        int currentCount = item.getItemStack().getAmount();

        long stateHash = ((long) currentSec << 32) | ((long) currentCount << 16);
        Long lastHash = lastHoloState.get(uuid);

        if (lastHash != null && lastHash == stateHash) {
            return;
        }

        Component baseName = baseNameCache.get(uuid);
        if (baseName == null) {
            baseName = calculateBaseName(item, color);
            baseNameCache.put(uuid, baseName);
        }
        display.text(buildFinalName(item, baseName));
        lastHoloState.put(uuid, stateHash);
    }

    private void startGarbageCollectorTask() {
        FoliaScheduler.runTimer(this, () -> {
            if (!isEnabled)
                return;

            // Clean up items that were removed without triggering events (e.g. ClearLag)
            java.util.List<UUID> toRemove = new java.util.ArrayList<>();
            for (Map.Entry<UUID, Item> entry : activeItems.entrySet()) {
                if (entry.getValue() == null || !entry.getValue().isValid() || entry.getValue().isDead()) {
                    toRemove.add(entry.getKey());
                }
            }

            for (UUID uuid : toRemove) {
                removeGlow(uuid);
            }
        }, 600L, 600L); // Every 30 seconds
    }

    public void removeGlow(UUID uuid) {
        if (uuid == null)
            return;

        TextDisplay display = activeLabels.remove(uuid);
        if (display != null) cleanVisibleSet(display.getUniqueId());

        BlockDisplay beam = activeBeams.remove(uuid);
        if (beam != null) cleanVisibleSet(beam.getUniqueId());

        ItemDisplay visual = activeItemVisuals.remove(uuid);
        if (visual != null) cleanVisibleSet(visual.getUniqueId());

        try {
            org.bukkit.scoreboard.Scoreboard scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
            String itemEntry = uuid.toString();
            org.bukkit.scoreboard.Team itemTeam = scoreboard.getEntryTeam(itemEntry);
            if (itemTeam != null && itemTeam.getName().startsWith("LG_")) {
                itemTeam.removeEntry(itemEntry);
            }
            if (visual != null) {
                String visualEntry = visual.getUniqueId().toString();
                org.bukkit.scoreboard.Team visualTeam = scoreboard.getEntryTeam(visualEntry);
                if (visualTeam != null && visualTeam.getName().startsWith("LG_")) {
                    visualTeam.removeEntry(visualEntry);
                }
            }
        } catch (Exception e) {
            // Ignore scoreboard error
        }

        org.bukkit.entity.Display shadow = activeShadows.remove(uuid);
        if (shadow != null) cleanVisibleSet(shadow.getUniqueId());

        activeBeamConfigs.remove(uuid);
        trackedItems.remove(uuid);
        if (beam != null) {
            beam.getPassengers().forEach(passenger -> { if (passenger != null) passenger.remove(); });
            beam.remove();
        }

        if (shadow != null) {
            entityIdMap.remove(shadow.getEntityId());
            shadow.remove();
        }
        if (display != null) {
            entityIdMap.remove(display.getEntityId());
        }
        if (visual != null) {
            entityIdMap.remove(visual.getEntityId());
        }

        // Cleanup by UUID
        itemSpawnTimes.remove(uuid);
        itemCategoriesCache.remove(uuid);
        itemMoneyAmounts.remove(uuid);
        lastHoloState.remove(uuid);
        baseNameCache.remove(uuid);
        itemParticlesCache.remove(uuid);
        bounceCounts.remove(uuid);
        recentlyBounced.remove(uuid);
        waterLogCache.remove(uuid);
        surfaceStates.remove(uuid);

        Item item = activeItems.remove(uuid);
        if (item != null) {
            int entityId = item.getEntityId();
            entityIdMap.remove(entityId);
            hiddenVanillaItems.remove(entityId);
            String world = item.getWorld().getName();
            if (itemsByWorld.containsKey(world)) {
                itemsByWorld.get(world).remove(uuid);
            }
        }

        if (display != null)
            display.remove();
        if (visual != null && !flyingVisuals.containsKey(uuid))
            visual.remove();
    }

    private void startLODTask() {
        FoliaScheduler.runTimer(this, () -> {
            if (!isEnabled || !lodEnabled)
                return;

            Set<UUID> newGloballyVisible = new HashSet<>();
            double maxLodRadius = Math.sqrt(Math.max(lodBeamDistSq, lodHoloDistSq));
            double farmDistSq = farmingViewDistance * farmingViewDistance;

            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID pUuid = p.getUniqueId();
                World pWorld = p.getWorld();
                String worldName = pWorld.getName();
                double px = p.getX(), py = p.getY(), pz = p.getZ();
                Set<UUID> visibleSet = visibleEntities.computeIfAbsent(pUuid, k -> new HashSet<>());
                boolean isHiddenToggle = isHiddenToggleFor(p);

                // Track which items are currently in LOD range for this player
                Set<UUID> inRangeItemUuids = new HashSet<>();

                // Use Minecraft's optimized spatial index instead of iterating all world items
                for (Entity ent : p.getNearbyEntities(maxLodRadius, maxLodRadius, maxLodRadius)) {
                    if (!(ent instanceof Item item)) continue;
                    UUID uuid = item.getUniqueId();
                    if (!activeItems.containsKey(uuid)) continue;
                    inRangeItemUuids.add(uuid);

                    double ix = item.getX(), iy = item.getY(), iz = item.getZ();
                    double dx = px - ix, dy = py - iy, dz = pz - iz;
                    double dSq = dx * dx + dy * dy + dz * dz;
                    boolean isGrouped = groupedItems.contains(uuid);

                    // Unified LOD check for all components
                    TextDisplay label = activeLabels.get(uuid);
                    if (label != null && label.isValid()) {
                        boolean shouldSee = !isHiddenToggle && !isGrouped && dSq <= lodHoloDistSq;
                        updateEntityVisibility(p, label, shouldSee, visibleSet);
                    }

                    BlockDisplay beam = activeBeams.get(uuid);
                    if (beam != null && beam.isValid()) {
                        boolean shouldSee = !isHiddenToggle && !isGrouped && dSq <= lodBeamDistSq;
                        updateEntityVisibility(p, beam, shouldSee, visibleSet);
                        if (shouldSee) newGloballyVisible.add(uuid);
                    }

                    ItemDisplay visual = activeItemVisuals.get(uuid);
                    if (visual != null && visual.isValid()) {
                        boolean shouldSee = !isHiddenToggle && !isGrouped && dSq <= lodHoloDistSq;
                        updateEntityVisibility(p, visual, shouldSee, visibleSet);
                    }

                    org.bukkit.entity.Display shadow = activeShadows.get(uuid);
                    if (shadow != null && shadow.isValid()) {
                        boolean shouldSee = !isHiddenToggle && !isGrouped && dSq <= lodHoloDistSq;
                        updateEntityVisibility(p, shadow, shouldSee, visibleSet);
                    }
                }

                // Hide displays for items previously visible but now out of LOD range
                Set<UUID> worldItems = itemsByWorld.get(worldName);
                if (worldItems != null) {
                    for (UUID uuid : worldItems) {
                        if (inRangeItemUuids.contains(uuid)) continue;
                        TextDisplay label = activeLabels.get(uuid);
                        if (label != null && label.isValid()) updateEntityVisibility(p, label, false, visibleSet);
                        BlockDisplay beam = activeBeams.get(uuid);
                        if (beam != null && beam.isValid()) updateEntityVisibility(p, beam, false, visibleSet);
                        ItemDisplay visual = activeItemVisuals.get(uuid);
                        if (visual != null && visual.isValid()) updateEntityVisibility(p, visual, false, visibleSet);
                        org.bukkit.entity.Display shadow = activeShadows.get(uuid);
                        if (shadow != null && shadow.isValid()) updateEntityVisibility(p, shadow, false, visibleSet);
                    }
                }

                // Farming Symbols LOD
                if (farmingEnabled) {
                    for (Map.Entry<org.bukkit.block.Block, CropSymbol> entry : activeCropSymbols.entrySet()) {
                        if (!entry.getKey().getWorld().getName().equals(worldName))
                            continue;

                        // Use cached location with primitive math (no Location allocation)
                        Location csLoc = entry.getValue().location;
                        double cdx = px - csLoc.getX(), cdy = py - csLoc.getY(), cdz = pz - csLoc.getZ();
                        boolean shouldSee = !isHiddenToggle && (cdx * cdx + cdy * cdy + cdz * cdz) <= farmDistSq;

                        for (BlockDisplay bd : entry.getValue()) {
                            if (bd.isValid()) {
                                updateEntityVisibility(p, bd, shouldSee, visibleSet);
                                if (shouldSee) newGloballyVisible.add(bd.getUniqueId());
                            }
                        }
                    }
                }
            }
            this.globallyVisibleEntities = newGloballyVisible;
        }, 200L, (long) lodInterval);
    }


    private void updateEntityVisibility(Player p, Entity entity, boolean shouldSee, Set<UUID> visibleSet) {
        UUID entUuid = entity.getUniqueId();
        boolean currentlyVisible = visibleSet.contains(entUuid);

        if (shouldSee && !currentlyVisible) {
            p.showEntity(this, entity);
            entity.getPassengers().forEach(pass -> p.showEntity(this, pass));
            visibleSet.add(entUuid);
        } else if (!shouldSee && currentlyVisible) {
            p.hideEntity(this, entity);
            entity.getPassengers().forEach(pass -> p.hideEntity(this, pass));
            visibleSet.remove(entUuid);
        }
    }

    private boolean isHiddenToggleFor(Player p) {
        return hiddenVisuals.contains(p.getUniqueId());
    }

    private boolean canFit(org.bukkit.inventory.Inventory inv, ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return false;
        if (inv.firstEmpty() != -1) return true;
        int maxStack = item.getMaxStackSize();
        if (maxStack <= 1) return false;
        for (ItemStack is : inv.getStorageContents()) {
            if (is != null && is.isSimilar(item)) {
                if (is.getAmount() < maxStack) {
                    return true;
                }
            }
        }
        return false;
    }



    /**
     * Single unified scheduler that replaces 6 high-frequency independent Bukkit tasks.
     * Runs every tick; internal counter dispatches 2-tick subtasks via modulo.
     * This reduces Bukkit scheduler overhead (6 → 1 dispatch per tick).
     */
    private void startUnifiedTickTask() {
        FoliaScheduler.runTimer(this, new Runnable() {
            private int unifiedTick = 0;
            private float beamAngle = 0f;
            private float farmAngle = 0f;

            @Override
            public void run() {
                unifiedTick++;
                if (!isEnabled) return;

                // --- Every tick (1L) ---
                tickGlobalSync();
                tickBouncing();
                tickAspiration();

                // --- Every 2 ticks ---
                if (unifiedTick % 2 == 0) {
                    tickMagnet();
                    farmAngle += 0.1f;
                    tickFarmingAnimation(farmAngle);
                    beamAngle += 0.1f;
                    tickBeamAnimation(beamAngle);
                }
            }
        }, 1L, 1L);
    }

    /** Magnet logic. Uses getNearbyEntities (already optimal) + primitive coords — no Location allocation. */
    private void tickMagnet() {
        if (!magnetEnabled) return;

        double dist = magnetDistance;
        String perm = magnetPermission;
        List<String> magnetCats = magnetCategories;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (disabledMagnets.contains(p.getUniqueId()) || !p.hasPermission(perm))
                continue;

            double px = p.getX(), py = p.getY() + 1.0, pz = p.getZ();

            for (Entity ent : p.getNearbyEntities(dist, dist, dist)) {
                if (!(ent instanceof Item item) || !item.isValid() || item.getPickupDelay() > 0)
                    continue;

                UUID itemUuid = item.getUniqueId();
                if (!magnetEnableForGroups && (groupMembers.containsKey(itemUuid) || groupedItems.contains(itemUuid)))
                    continue;
                String category = itemCategoriesCache.get(itemUuid);
                if (!magnetCats.isEmpty() && (category == null || !magnetCats.contains(category.toLowerCase())))
                    continue;

                // Respect Hard Lock
                UUID owner = item.getThrower();
                if (owner != null && !owner.equals(p.getUniqueId()) && protectionEnabled) {
                    long spawnTime = itemSpawnTimes.getOrDefault(itemUuid, System.currentTimeMillis());
                    if (System.currentTimeMillis() - spawnTime < (protectionDuration * 1000L)) {
                        if (!p.hasPermission("lootglow.bypass.lock"))
                            continue;
                    }
                }

                if (!canFit(p.getInventory(), item.getItemStack()))
                    continue;

                double dx = px - item.getX();
                double dy = py - item.getY();
                double dz = pz - item.getZ();
                double d2 = dx * dx + dy * dy + dz * dz;

                if (d2 < 0.04 || d2 > dist * dist)
                    continue;

                double d = Math.sqrt(d2);
                if (d < 0.01) continue; // Guard against division by zero
                double speed = 0.4;
                item.setVelocity(new Vector((dx / d) * speed, (dy / d) * speed, (dz / d) * speed));
            }
        }
    }

    /** Beam rotation + pulse animation (every 2 ticks). */
    private void tickBeamAnimation(float angle) {
        if (!beamsEnabled || !beamsAnimate) return;

        org.joml.Quaternionf rot = new org.joml.Quaternionf().rotationY(angle);
        float scalePulse = (float) (1.0 + Math.sin(angle * 2) * 0.15);
        int tick = (int) (angle * 20);

        for (java.util.Map.Entry<UUID, BlockDisplay> entry : activeBeams.entrySet()) {
            BlockDisplay beam = entry.getValue();
            if (beam == null || !beam.isValid()) continue;
            if (!globallyVisibleEntities.contains(entry.getKey())) continue;

            BeamConfig config = activeBeamConfigs.get(entry.getKey());
            boolean shouldAnimate = (config != null) ? config.animate() : beamsAnimate;
            boolean shouldPulse   = (config != null) ? config.pulse() : true;

            Transformation trans = beam.getTransformation();
            if (shouldAnimate) trans.getLeftRotation().set(rot);

            float bH = (config != null) ? config.height() : beamHeight;
            float bW = (config != null) ? config.width() : beamWidth;
            float currentWidth = shouldPulse ? bW * scalePulse : bW;

            trans.getScale().set(currentWidth, bH, currentWidth);
            trans.getTranslation().set(-currentWidth / 2, 0, -currentWidth / 2);
            beam.setTransformation(trans);
            beam.setInterpolationDuration(2);
            beam.setInterpolationDelay(0);

            // Animate passengers (core beam)
            for (Entity pass : beam.getPassengers()) {
                if (pass instanceof BlockDisplay bd) {
                    Transformation cTrans = bd.getTransformation();
                    cTrans.getLeftRotation().set(rot);
                    float cWidth = beamWidth * 0.4f * scalePulse;
                    cTrans.getScale().set(cWidth, beamHeight, cWidth);
                    cTrans.getTranslation().set(-cWidth / 2, 0, -cWidth / 2);
                    bd.setTransformation(cTrans);
                    bd.setInterpolationDuration(2);
                    bd.setInterpolationDelay(0);
                }
            }

            // Rising particles along the beam
            if (tick % 2 == 0) {
                UUID itemUuid = entry.getKey();
                Particle part = itemParticlesCache.get(itemUuid);
                if (part != null) {
                    double heightOffset = (angle * 5) % beamHeight;
                    Location beamLoc = beam.getLocation().add(0, heightOffset, 0);
                    beam.getWorld().spawnParticle(part, beamLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }
    }

    /** Crop symbol rotation animation (every 2 ticks). */
    private void tickFarmingAnimation(float angle) {
        if (!farmingEnabled || !farmingAnimation) return;

        org.joml.Quaternionf rot = new org.joml.Quaternionf().rotationY(angle);

        for (List<BlockDisplay> parts : activeCropSymbols.values()) {
            if (parts.size() < 2) continue;
            BlockDisplay bar = parts.get(0);
            BlockDisplay dot = parts.get(1);
            if (!bar.isValid()) continue;
            if (!globallyVisibleEntities.contains(bar.getUniqueId())) continue;

            org.bukkit.util.Transformation bT = bar.getTransformation();
            bT.getLeftRotation().set(rot);
            bar.setTransformation(bT);
            bar.setInterpolationDuration(2);
            bar.setInterpolationDelay(0);

            org.bukkit.util.Transformation dT = dot.getTransformation();
            dT.getLeftRotation().set(rot);
            dot.setTransformation(dT);
            dot.setInterpolationDuration(2);
            dot.setInterpolationDelay(0);
        }
    }


    private void startFarmingTask() {
        FoliaScheduler.runTimer(this, () -> {
            if (!isEnabled || !farmingEnabled)
                return;

            // Active validation & cleanup of spawned farming symbols (Zero-Allocation Iterator)
            if (!activeCropSymbols.isEmpty()) {
                java.util.Iterator<Map.Entry<org.bukkit.block.Block, CropSymbol>> it = activeCropSymbols.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<org.bukkit.block.Block, CropSymbol> entry = it.next();
                    org.bukkit.block.Block b = entry.getKey();
                    boolean valid = b.getType() != Material.AIR && farmingCrops.contains(b.getType());
                    if (valid) {
                        if (b.getBlockData() instanceof org.bukkit.block.data.Ageable age) {
                            if (age.getAge() != age.getMaximumAge()) {
                                valid = false;
                            }
                        } else {
                            valid = false;
                        }
                    }
                    if (valid && !isFarmingAllowed(b.getLocation())) {
                        valid = false;
                    }
                    if (!valid) {
                        entry.getValue().forEach(e -> e.remove());
                        it.remove();
                    }
                }
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!isWorldAllowed(p.getWorld().getName()))
                    continue;

                Location loc = p.getLocation();
                Location lastLoc = lastFarmingScanLocations.get(p.getUniqueId());
                if (lastLoc != null && lastLoc.getWorld().equals(loc.getWorld()) && lastLoc.distanceSquared(loc) < 64.0) {
                    continue;
                }
                lastFarmingScanLocations.put(p.getUniqueId(), loc.clone());

                org.bukkit.block.Block center = loc.getBlock();
                int r = (int) Math.min(16.0, Math.ceil(farmingViewDistance));
                for (int x = -r; x <= r; x += 2) {
                    for (int z = -r; z <= r; z += 2) {
                        for (int y = -2; y <= 2; y++) {
                            org.bukkit.block.Block b = center.getRelative(x, y, z);
                            if (farmingCrops.contains(b.getType())) {
                                if (b.getBlockData() instanceof org.bukkit.block.data.Ageable age) {
                                    if (age.getAge() == age.getMaximumAge()) {
                                        spawnCropSymbol(b);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }, 100L, 100L);
    }

    private final Set<UUID> groupedItems = new HashSet<>();
    private final Map<UUID, Integer> groupLeaders = new HashMap<>();

    private void startGroupingTask() {
        FoliaScheduler.runTimer(this, () -> {
            if (!isEnabled || !groupingEnabled)
                return;

            double radius = getConfig().getDouble("settings.grouping.radius", 2.0);
            int minItems = getConfig().getInt("settings.grouping.min-items", 5);
            boolean byCategory = getConfig().getBoolean("settings.grouping.group-by-category", true);

            double radiusSq = radius * radius;
            double holoDistSq = Math.pow(getConfig().getDouble("settings.performance.lod.hologram-distance", 24.0), 2);

            groupedItems.clear();
            groupLeaders.clear();

            // Cache player positions primitives once to eliminate dynamic Location allocation storm
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            final int numPlayers = onlinePlayers.size();
            final double[] px = new double[numPlayers];
            final double[] py = new double[numPlayers];
            final double[] pz = new double[numPlayers];
            final World[] pWorlds = new World[numPlayers];
            int pIdx = 0;
            for (Player p : onlinePlayers) {
                px[pIdx] = p.getX();
                py[pIdx] = p.getY();
                pz[pIdx] = p.getZ();
                pWorlds[pIdx] = p.getWorld();
                pIdx++;
            }

            Map<World, List<Item>> worldItemsMap = new HashMap<>();
            for (Map.Entry<UUID, TrackedItem> entry : trackedItems.entrySet()) {
                if (entry.getValue().label == null) continue;
                UUID uuid = entry.getKey();
                Item item = activeItems.get(uuid);
                if (item != null && item.isValid()) {
                    worldItemsMap.computeIfAbsent(item.getWorld(), w -> new ArrayList<>()).add(item);
                }
            }

            Set<UUID> tempGrouped = new HashSet<>();
            Map<UUID, Integer> tempLeaders = new HashMap<>();
            Map<UUID, List<UUID>> tempMembers = new HashMap<>();
            Set<UUID> processed = new HashSet<>();

            for (List<Item> items : worldItemsMap.values()) {
                int size = items.size();
                
                if (size < minItems)
                    continue;
                double[] xs = new double[size];
                double[] ys = new double[size];
                double[] zs = new double[size];
                String[] cats = new String[size];

                // Optimization: Cache coordinates and categories ONCE before entering nested loops.
                // Eradicates thousands of repetitive Object allocations and Spigot lookups per iteration!
                for (int k = 0; k < size; k++) {
                    Item it = items.get(k);
                    xs[k] = it.getX();
                    ys[k] = it.getY();
                    zs[k] = it.getZ();
                    cats[k] = itemCategoriesCache.get(it.getUniqueId());
                }

                for (int i = 0; i < size; i++) {
                    Item item = items.get(i);
                    UUID uuid = item.getUniqueId();
                    if (processed.contains(uuid))
                        continue;

                    List<Item> nearby = new ArrayList<>();
                    nearby.add(item);
                    String cat = cats[i];

                    for (int j = i + 1; j < size; j++) {
                        Item other = items.get(j);
                        if (processed.contains(other.getUniqueId()))
                            continue;

                        // Pure primitive math calculation. Absolutely zero Spigot methods invoked here.
                        double dx = xs[i] - xs[j];
                        double dy = ys[i] - ys[j];
                        double dz = zs[i] - zs[j];

                        if ((dx * dx + dy * dy + dz * dz) < radiusSq) {
                            if (!byCategory || Objects.equals(cat, cats[j])) {
                                nearby.add(other);
                            }
                        }
                    }

                    // Only group if there's a mix of different materials
                    Set<Material> materials = new HashSet<>();
                    for (Item ni : nearby)
                        materials.add(ni.getItemStack().getType());

                    if (nearby.size() >= minItems && materials.size() > 1) {
                        UUID leaderUuid = nearby.get(0).getUniqueId();
                        tempLeaders.put(leaderUuid, nearby.size());
                        List<UUID> members = new ArrayList<>();
                        for (int k = 0; k < nearby.size(); k++) {
                            UUID mUuid = nearby.get(k).getUniqueId();
                            members.add(mUuid);
                            if (k > 0) {
                                tempGrouped.add(mUuid);
                                processed.add(mUuid);
                            }
                        }
                        tempMembers.put(leaderUuid, members);
                        processed.add(leaderUuid);
                    }
                }
            }

            // Atomic swap to prevent flickering
            this.groupedItems.clear();
            this.groupedItems.addAll(tempGrouped);
            this.groupLeaders.clear();
            this.groupLeaders.putAll(tempLeaders);
            this.groupMembers.clear();
            this.groupMembers.putAll(tempMembers);

            // Update visual bag model
            if (useVisualBag) {
                activeItemVisuals.forEach((uuid, visual) -> {
                    if (!visual.isValid())
                        return;
                    Item item = activeItems.get(uuid);
                    if (item == null || !item.isValid())
                        return;

                    boolean isLeader = groupLeaders.containsKey(uuid);
                    ItemStack currentStack = visual.getItemStack();
                    if (isLeader) {
                        if (currentStack == null || currentStack.getType() != bagMaterial) {
                            ItemStack bag;
                            if (bagMaterial == Material.PLAYER_HEAD) {
                                if (useOwnerHead && item.getThrower() != null) {
                                    bag = getOwnerHead(item.getThrower());
                                } else if (!bagHeadTexture.isEmpty()) {
                                    bag = createTexturedHead(bagHeadTexture);
                                } else {
                                    bag = new ItemStack(bagMaterial);
                                }
                            } else {
                                bag = new ItemStack(bagMaterial);
                            }

                            if (bagCustomModelData != 0) {
                                org.bukkit.inventory.meta.ItemMeta meta = bag.getItemMeta();
                                if (meta != null) {
                                    meta.setCustomModelData(bagCustomModelData);
                                    bag.setItemMeta(meta);
                                }
                            }
                            visual.setItemStack(bag);
                            visual.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.FIXED);
                            // Adjust transformation for upright bag
                            org.bukkit.util.Transformation t = visual.getTransformation();
                            t.getLeftRotation().set(new org.joml.Quaternionf()); // Upright
                            t.getTranslation().set(0f, 0.05f, 0f); // Elevate slightly above ground
                            t.getScale().set(1.0f, 1.0f, 1.0f);
                            visual.setTransformation(t);
                        }
                    } else {
                        // Restore original stack if it was a bag
                        if (currentStack != null && currentStack.getType() == bagMaterial) {
                            visual.setItemStack(item.getItemStack());
                            visual.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.NONE);
                            // Restore RPG rotation
                            org.bukkit.util.Transformation t = visual.getTransformation();
                            Material itemMat = item.getItemStack().getType();
                            boolean isCustom = isCustomItem(item.getItemStack());
                            boolean isUpright = isUprightItem(itemMat);
                            float targetRotX = (isCustom || isUpright) ? 0f : rpgRotation;
                            t.getLeftRotation().set(new org.joml.Quaternionf().rotationX(targetRotX));
                            visual.setTransformation(t);
                        }
                    }
                });
            }

            // Update holograms with visibility check and caching
            for (Map.Entry<UUID, TrackedItem> entry : trackedItems.entrySet()) {
                UUID uuid = entry.getKey();
                TrackedItem ti = entry.getValue();
                TextDisplay display = ti.label;
                if (display == null || !display.isValid())
                    continue;

                // Optimized visibility check: Pure primitive vectorization (ZERO allocations)
                boolean hasPlayerNearby = false;
                final double lx = display.getX();
                final double ly = display.getY();
                final double lz = display.getZ();
                final World lWorld = display.getWorld();

                for (int i = 0; i < numPlayers; i++) {
                    if (pWorlds[i].equals(lWorld)) {
                        double pdx = px[i] - lx;
                        double pdy = py[i] - ly;
                        double pdz = pz[i] - lz;
                        if ((pdx * pdx + pdy * pdy + pdz * pdz) < holoDistSq) {
                            hasPlayerNearby = true;
                            break;
                        }
                    }
                }

                boolean isGroupLeader = groupLeaders.containsKey(uuid);
                boolean isGrouped = groupedItems.contains(uuid);

                // If no player is nearby, we skip the text update UNLESS it's a group change
                // (to ensure consistent state when players approach)
                if (!hasPlayerNearby && !isGroupLeader && !isGrouped)
                    continue;

                // Use activeItems map instead of Bukkit.getEntity
                Item item = activeItems.get(uuid);
                if (item == null || !item.isValid())
                    continue;

                int currentSec = holoShowTimer ? (6000 - item.getTicksLived()) / 20 : 0;
                int currentCount = item.getItemStack().getAmount();
                int currentGroupSize = isGroupLeader ? groupLeaders.getOrDefault(uuid, 1) : 0;

                long stateHash = ((long) currentSec << 32) | ((long) currentCount << 16) | currentGroupSize;
                Long lastHash = ti.lastHoloState;

                if (lastHash != null && lastHash == stateHash) {
                    continue;
                }

                Component newContent;
                if (isGroupLeader) {
                    int count = groupLeaders.get(uuid);
                    newContent = miniMessage.deserialize(rawBundleFormat.replace("<count>", String.valueOf(count)));
                } else if (!isGrouped) {
                    NamedTextColor color = itemCategories.get(ti.category);
                    if (color == null)
                        color = defaultColor;

                    Component baseName = ti.baseName;
                    if (baseName == null) {
                        baseName = calculateBaseName(item, color);
                        ti.baseName = baseName;
                    }
                    newContent = buildFinalName(item, baseName);
                } else {
                    continue;
                }

                display.text(newContent);
                ti.lastHoloState = stateHash;
            }
        }, 20L, 20L);
    }

    private void spawnHologram(Item item, NamedTextColor color) {
        if (!holoEnabled || item == null)
            return;
        UUID uuid = item.getUniqueId();
        String cat = itemCategoriesCache.get(uuid);
        if (holoHideUncategorized && cat == null)
            return;
        if (activeLabels.containsKey(uuid))
            return;
        Location spawnLoc = item.getLocation().clone();
        if (holoFrontOffset != 0.0) {
            spawnLoc.add(0, 0, holoFrontOffset);
        }
        TextDisplay display = item.getWorld().spawn(spawnLoc, TextDisplay.class, ent -> {
            ent.setShadowed(true);
            ent.setBillboard(Display.Billboard.CENTER);
            ent.setSeeThrough(holoSeeThrough);
            ent.setViewRange(holoViewDistance / 16.0f);
            if (!holoBackground)
                ent.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

            Transformation transformation = ent.getTransformation();
            transformation.getTranslation().set(0, (float) holoOffset + (float) (Math.random() * 0.02), 0.0f);
            ent.setTransformation(transformation);

            Component baseName = baseNameCache.get(item.getUniqueId());
            if (baseName == null) {
                baseName = calculateBaseName(item, color);
                baseNameCache.put(item.getUniqueId(), baseName);
            }
            ent.text(buildFinalName(item, baseName));

            // Per-player visibility (Zero Entity logic)
            ent.setVisibleByDefault(false);
            ent.setTeleportDuration(1);
            ent.setPersistent(false);
        });

        activeLabels.put(item.getUniqueId(), display);

        // Show to nearby players (Respecting LOD)
        double holoDistSq = Math.pow(getConfig().getDouble("settings.performance.lod.hologram-distance", 24.0), 2);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hiddenVisuals.contains(p.getUniqueId()) && p.getWorld().equals(item.getWorld())) {
                if (p.getLocation().distanceSquared(item.getLocation()) < holoDistSq) {
                    p.showEntity(this, display);
                    visibleEntities.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>()).add(display.getUniqueId());
                }
            }
        }
    }

    private void spawnBeam(Item item, String category, NamedTextColor color) {
        if (activeBeams.containsKey(item.getUniqueId()))
            return;

        // Load Overrides from Category
        float h = beamHeight;
        float w = beamWidth;
        boolean anim = beamsAnimate;
        boolean pulse = true;
        Material mat = beamsUseCategoryColor ? getColorStainedGlass(color) : Material.WHITE_STAINED_GLASS;

        if (category != null) {
            String path = "categories." + category + ".beam.";
            if (getConfig().contains(path + "height"))
                h = (float) getConfig().getDouble(path + "height");
            if (getConfig().contains(path + "width"))
                w = (float) getConfig().getDouble(path + "width");
            if (getConfig().contains(path + "animate"))
                anim = getConfig().getBoolean(path + "animate");
            if (getConfig().contains(path + "pulse"))
                pulse = getConfig().getBoolean(path + "pulse");
            if (getConfig().contains(path + "material")) {
                Material m = Material.matchMaterial(getConfig().getString(path + "material", ""));
                if (m != null)
                    mat = m;
            }
        }

        final float finalH = h;
        final float finalW = w;
        final Material finalMat = mat;

        BlockDisplay beam = item.getWorld().spawn(item.getLocation(), BlockDisplay.class, ent -> {
            ent.setBlock(finalMat.createBlockData());
            ent.setGlowColorOverride(Color.fromRGB(color.red(), color.green(), color.blue()));
            ent.setGlowing(true);
            ent.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));

            Transformation transformation = ent.getTransformation();
            transformation.getScale().set(finalW, finalH, finalW);
            transformation.getTranslation().set(-finalW / 2, 0, -finalW / 2);
            ent.setTransformation(transformation);

            ent.setVisibleByDefault(false);
            ent.setTeleportDuration(1);
            ent.setPersistent(false);
        });

        activeBeams.put(item.getUniqueId(), beam);
        activeBeamConfigs.put(item.getUniqueId(), new BeamConfig(finalH, finalW, anim, pulse));

        // Show to nearby players (Respecting LOD)
        double beamDistSq = Math.pow(getConfig().getDouble("settings.performance.lod.beam-distance", 48.0), 2);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hiddenVisuals.contains(p.getUniqueId()) && p.getWorld().equals(item.getWorld())) {
                if (p.getLocation().distanceSquared(item.getLocation()) < beamDistSq) {
                    p.showEntity(this, beam);
                    visibleEntities.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>()).add(beam.getUniqueId());
                }
            }
        }
    }

    private Component calculateBaseName(Item item, NamedTextColor color) {
        String customId = getInternalId(item.getItemStack());
        String matName = item.getItemStack().getType().name();

        // Check for Display Name Overrides (Now from cache)
        Component name = displayNameOverridesCache.get(customId);
        if (name == null)
            name = displayNameOverridesCache.get(matName);

        if (name == null) {
            // Check for Economy amount display
            Double amount = itemMoneyAmounts.get(item.getUniqueId());
            if (amount != null) {
                String formatted = economyFormat
                        .replace("<prefix>", economyPrefix)
                        .replace("<amount>", String.format("%.2f", amount));
                name = miniMessage.deserialize(formatted);
            }
        }

        if (name == null) {
            if (item.getItemStack().hasItemMeta()) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemStack().getItemMeta();
                if (meta.hasDisplayName()) {
                    name = meta.displayName();
                } else if (meta.hasItemName()) {
                    name = meta.itemName();
                }
            }
            if (name == null) {
                name = Component.translatable(item.getItemStack().translationKey());
            }
        }

        return name.decorate(TextDecoration.BOLD).colorIfAbsent(color);
    }

    private Component buildFinalName(Item item, Component baseName) {
        Component name = baseName;

        // 1. Dynamic Amount Display (Supports merges in real-time!)
        int amount = item.getItemStack().getAmount();
        if (holoShowAmount && amount > 0) {
            name = name.append(miniMessage.deserialize(rawAmountFormat.replace("<amount>", String.valueOf(amount))));
        }

        // 2. Dynamic Protection Owner Name Display (Disappears automatically after duration!)
        UUID thrower = item.getThrower();
        if (thrower != null && protectionEnabled) {
            long spawnTime = itemSpawnTimes.getOrDefault(item.getUniqueId(), System.currentTimeMillis());
            long elapsed = (System.currentTimeMillis() - spawnTime) / 1000L;

            if (elapsed < protectionDuration) {
                String ownerName = Bukkit.getOfflinePlayer(thrower).getName();
                if (ownerName == null)
                    ownerName = "Inconnu";

                String processed = rawOwnerFormat.replace("<owner>", ownerName);
                if (usePapi)
                    processed = PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(thrower), processed);

                name = name.append(miniMessage.deserialize(processed));
            }
        }

        // 3. Dynamic Despawn Timer Display
        if (holoShowTimer) {
            int remainingTicks = 6000 - item.getTicksLived();
            if (remainingTicks > 0) {
                int seconds = remainingTicks / 20;
                Component timerComp = timerComponentCache.get(seconds);
                if (timerComp != null) {
                    if (holoTimerNewLine) {
                        name = name.append(Component.newline()).append(timerComp);
                    } else {
                        name = name.append(timerComp);
                    }
                }
            }
        }

        return name;
    }

    private Material getColorStainedGlass(NamedTextColor color) {
        if (color == null)
            return Material.WHITE_STAINED_GLASS;
        if (color.equals(NamedTextColor.GOLD))
            return Material.YELLOW_STAINED_GLASS;
        if (color.equals(NamedTextColor.LIGHT_PURPLE))
            return Material.MAGENTA_STAINED_GLASS;
        if (color.equals(NamedTextColor.AQUA))
            return Material.LIGHT_BLUE_STAINED_GLASS;
        if (color.equals(NamedTextColor.GREEN))
            return Material.LIME_STAINED_GLASS;
        if (color.equals(NamedTextColor.RED))
            return Material.RED_STAINED_GLASS;
        if (color.equals(NamedTextColor.BLUE))
            return Material.BLUE_STAINED_GLASS;
        if (color.equals(NamedTextColor.DARK_PURPLE))
            return Material.PURPLE_STAINED_GLASS;
        if (color.equals(NamedTextColor.YELLOW))
            return Material.YELLOW_STAINED_GLASS;
        if (color.equals(NamedTextColor.WHITE))
            return Material.WHITE_STAINED_GLASS;
        if (color.equals(NamedTextColor.GRAY))
            return Material.LIGHT_GRAY_STAINED_GLASS;
        if (color.equals(NamedTextColor.DARK_GRAY))
            return Material.GRAY_STAINED_GLASS;
        if (color.equals(NamedTextColor.BLACK))
            return Material.BLACK_STAINED_GLASS;
        return Material.WHITE_STAINED_GLASS;
    }

    public void removeGlow(Item item) {
        if (item == null)
            return;
        removeGlow(item.getUniqueId());
    }

    public void refreshHologram(Item item) {
        if (!holoEnabled || item == null || !item.isValid())
            return;
        UUID uuid = item.getUniqueId();
        String cat = itemCategoriesCache.get(uuid);
        if (holoHideUncategorized && cat == null)
            return;
        NamedTextColor color = itemCategories.get(cat);
        if (color == null)
            color = defaultColor;

        lastHoloState.remove(uuid);
        updateHologram(item, color);
    }

    public void clearVisualsForPlayer(Player player) {
        if (player == null)
            return;
        // Hide all active visuals for this player (useful on world change)
        for (TrackedItem ti : trackedItems.values()) {
            if (ti.label != null && ti.label.isValid())
                player.hideEntity(this, ti.label);
            if (ti.beam != null && ti.beam.isValid()) {
                player.hideEntity(this, ti.beam);
                ti.beam.getPassengers().forEach(p -> player.hideEntity(this, p));
            }
            if (ti.visual != null && ti.visual.isValid())
                player.hideEntity(this, ti.visual);
            if (ti.shadow != null && ti.shadow.isValid())
                player.hideEntity(this, ti.shadow);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(org.bukkit.event.world.WorldUnloadEvent event) {
        String worldName = event.getWorld().getName();
        java.util.List<UUID> toRemove = new java.util.ArrayList<>();
        for (java.util.Map.Entry<UUID, TrackedItem> entry : trackedItems.entrySet()) {
            TrackedItem ti = entry.getValue();
            org.bukkit.entity.Entity testEnt = ti.label != null ? ti.label : (ti.beam != null ? ti.beam : (ti.visual != null ? ti.visual : ti.shadow));
            if (testEnt != null && testEnt.getWorld().getName().equals(worldName)) {
                toRemove.add(entry.getKey());
            }
        }
        for (java.util.Map.Entry<UUID, Item> entry : activeItems.entrySet()) {
            if (entry.getValue().getWorld().getName().equals(worldName)) {
                UUID uuid = entry.getKey();
                if (!toRemove.contains(uuid)) {
                    toRemove.add(uuid);
                }
            }
        }
        for (UUID uuid : toRemove) {
            removeGlow(uuid);
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        visibleEntities.remove(uuid);
        hiddenVisuals.remove(uuid);
        disabledMagnets.remove(uuid);
        lastFarmingScanLocations.remove(uuid);
    }

    private NamedTextColor parseNamedColor(String colorStr) {
        NamedTextColor color = NamedTextColor.NAMES.value(colorStr.toLowerCase());
        return color != null ? color : NamedTextColor.WHITE;
    }

    private Sound parseSound(String soundStr) {
        if (soundStr == null || soundStr.trim().isEmpty())
            return null;
        String lower = soundStr.trim().toLowerCase();
        if (lower.equals("none") || lower.equals("off") || lower.equals("disabled") || lower.equals("false") || lower.equals("\"\"") || lower.equals("''"))
            return null;

        try {
            // 1. Try as a full NamespacedKey (e.g. "minecraft:entity.player.levelup")
            if (lower.contains(":")) {
                NamespacedKey key = NamespacedKey.fromString(lower);
                if (key != null) {
                    Sound sound = Registry.SOUND_EVENT.get(key);
                    if (sound != null)
                        return sound;
                }
            }

            // 2. Try as a Minecraft key or legacy name
            NamespacedKey mcKey = NamespacedKey.minecraft(lower);
            Sound mcSound = Registry.SOUND_EVENT.get(mcKey);
            if (mcSound != null)
                return mcSound;

            // 3. Fallback for legacy underscore names (e.g. "ENTITY_PLAYER_LEVELUP" -> "entity.player.levelup")
            NamespacedKey legacyKey = NamespacedKey.minecraft(lower.replace("_", "."));
            Sound legacySound = Registry.SOUND_EVENT.get(legacyKey);
            if (legacySound != null)
                return legacySound;
        } catch (Exception ignored) {
        }

        return null;
    }

    private boolean isNewerVersion(String current, String online) {
        try {
            String[] currentParts = current.split("\\.");
            String[] onlineParts = online.split("\\.");
            int length = Math.max(currentParts.length, onlineParts.length);
            for (int i = 0; i < length; i++) {
                int c = (i < currentParts.length) ? Integer.parseInt(currentParts[i].replaceAll("[^0-9]", "")) : 0;
                int o = (i < onlineParts.length) ? Integer.parseInt(onlineParts[i].replaceAll("[^0-9]", "")) : 0;
                if (o > c)
                    return true;
                if (c > o)
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private void spawnShadow(Item item) {
        if (item == null || !item.isValid())
            return;

        org.bukkit.entity.Display existing = activeShadows.get(item.getUniqueId());
        if (existing != null) {
            if (existing.isValid()) {
                return; // Shadow already exists and is valid
            }
            activeShadows.remove(item.getUniqueId());
        }

        Location loc = item.getLocation();
        // On utilise un BlockDisplay vide (plus léger que TextDisplay) pour porter l'ombre
        org.bukkit.entity.BlockDisplay shadow = item.getWorld().spawn(loc, org.bukkit.entity.BlockDisplay.class, ent -> {
            ent.setShadowRadius(shadowScale * 0.8f); // L'ombre native de Minecraft !
            ent.setShadowStrength(1.0f);
            ent.setPersistent(false);
            ent.setVisibleByDefault(false);
        });

        activeShadows.put(item.getUniqueId(), shadow);

        // Visibility
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hiddenVisuals.contains(p.getUniqueId()) && p.getWorld().equals(item.getWorld())) {
                if (p.getLocation().distanceSquared(item.getLocation()) < lodHoloDistSq) {
                    p.showEntity(this, shadow);
                    visibleEntities.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>()).add(shadow.getUniqueId());
                }
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            Map<String, String> info = new HashMap<>();
            info.put("version", getPluginMeta().getVersion());
            info.put("author", String.join(", ", getPluginMeta().getAuthors()));
            sendMessage(sender, "plugin-info", info);
            return true;
        }

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("lootglow.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                loadConfiguration();
                sendMessage(sender, "config-reloaded");
                return true;
            } else if (args[0].equalsIgnoreCase("toggle") && sender instanceof Player p) {
                if (!p.hasPermission("lootglow.toggle")) {
                    sendMessage(p, "no-permission");
                    return true;
                }
                if (hiddenVisuals.contains(p.getUniqueId())) {
                    hiddenVisuals.remove(p.getUniqueId());
                    savePlayerData(p.getUniqueId());
                    refreshGlowForPlayer(p, true);
                    sendMessage(p, "toggle-on");
                } else {
                    hiddenVisuals.add(p.getUniqueId());
                    savePlayerData(p.getUniqueId());
                    refreshGlowForPlayer(p, false);
                    sendMessage(p, "toggle-off");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("magnet") && sender instanceof Player p) {
                if (!p.hasPermission("lootglow.magnet")) {
                    sendMessage(p, "no-permission");
                    return true;
                }
                if (disabledMagnets.contains(p.getUniqueId())) {
                    disabledMagnets.remove(p.getUniqueId());
                    savePlayerData(p.getUniqueId());
                    sendMessage(p, "magnet-on");
                } else {
                    disabledMagnets.add(p.getUniqueId());
                    savePlayerData(p.getUniqueId());
                    sendMessage(p, "magnet-off");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("help")) {
                sendMessage(sender, "help-header");
                sendMessage(sender, "help-reload");
                sendMessage(sender, "help-toggle");
                sendMessage(sender, "help-magnet");
                return true;
            }
        }
        // Commande inconnue : on montre l'aide
        sendMessage(sender, "help-header");
        sendMessage(sender, "help-reload");
        sendMessage(sender, "help-toggle");
        sendMessage(sender, "help-magnet");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            if (sender.hasPermission("lootglow.admin")) list.add("reload");
            if (sender.hasPermission("lootglow.toggle")) list.add("toggle");
            if (sender.hasPermission("lootglow.magnet")) list.add("magnet");
            list.add("help");
            return list;
        }
        return Collections.emptyList();
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
        if (!isEnabled || !rpgDropsEnabled)
            return;
        if (!isWorldAllowed(item.getWorld().getName()))
            return;

        // Résoudre la catégorie (même logique que applyGlow)
        String customId = getInternalId(item.getItemStack());
        if (customId == null) {
            org.bukkit.persistence.PersistentDataContainer pdc = item.getPersistentDataContainer();
            if (pdc.has(sourceMobKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                customId = "MYTHIC:" + pdc.get(sourceMobKey, org.bukkit.persistence.PersistentDataType.STRING);
            }
        }
        if (customId == null && item.getItemStack().hasItemMeta()) {
            org.bukkit.persistence.PersistentDataContainer pdc = item.getItemStack().getItemMeta()
                    .getPersistentDataContainer();
            if (pdc.has(sourceMobKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                customId = "MYTHIC:" + pdc.get(sourceMobKey, org.bukkit.persistence.PersistentDataType.STRING);
            }
        }
        String matName = item.getItemStack().getType().name();
        String category = null;
        if (customId != null && itemCategories.containsKey(customId)) {
            category = categoryNames.get(customId);
        } else if (itemCategories.containsKey(matName)) {
            category = categoryNames.get(matName);
        }

        boolean isRpg = rpgEnabledCategories.isEmpty()
                || (category != null && rpgEnabledCategories.contains(category.toLowerCase()));
        if (!isRpg)
            return;

        // Enregistrer immédiatement : ProtocolLib pourra intercepter SPAWN_ENTITY dès
        // le 1er tick
        entityIdMap.put(item.getEntityId(), item.getUniqueId());
        hiddenVanillaItems.add(item.getEntityId());
        try {
            item.setVisibleByDefault(false);
        } catch (NoSuchMethodError ignored) {
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
        if (Bukkit.getPluginManager().isPluginEnabled("PacketEvents")) {
            this.packetProvider = new fr.skynex.lootglow.packets.PacketEventsProvider();
            getLogger().info("Using PacketEvents for packet handling.");
        } else if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            this.packetProvider = new fr.skynex.lootglow.packets.ProtocolLibProvider();
            getLogger().info("Using ProtocolLib for packet handling.");
        }

        if (this.packetProvider != null) {
            this.packetProvider.register(this);
            this.usePacketProvider = true;
        } else {
            getLogger().warning(
                    "Neither ProtocolLib nor PacketEvents found! Per-player glow toggle and RPG item hiding will not work.");
        }
    }

    public void refreshGlowForPlayer(Player player, boolean showVisuals) {
        World world = player.getWorld();
        // 1. Manage vanilla items visibility
        for (int entityId : hiddenVanillaItems) {
            UUID uuid = entityIdMap.get(entityId);
            if (uuid == null)
                continue;
            Entity ent = Bukkit.getEntity(uuid);
            if (ent instanceof Item item && item.getWorld().equals(world)) {
                if (showVisuals) {
                    player.hideEntity(this, item);
                } else {
                    player.showEntity(this, item);
                }
            }
        }

        // 2. Manage RPG visuals
        Set<UUID> visibleSet = visibleEntities.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        Location pLoc = player.getLocation();
        double farmDistSq = farmingViewDistance * farmingViewDistance;

        for (Map.Entry<UUID, TrackedItem> entry : trackedItems.entrySet()) {
            UUID uuid = entry.getKey();
            TrackedItem ti = entry.getValue();
            Item item = activeItems.get(uuid);
            if (item == null || !item.isValid() || !item.getWorld().equals(world))
                continue;

            double dSq = pLoc.distanceSquared(item.getLocation());
            boolean isGrouped = groupedItems.contains(uuid);

            if (showVisuals) {
                if (ti.label != null && ti.label.isValid()) {
                    boolean shouldSee = !isGrouped && dSq <= lodHoloDistSq;
                    updateEntityVisibility(player, ti.label, shouldSee, visibleSet);
                }
                if (ti.beam != null && ti.beam.isValid()) {
                    boolean shouldSee = !isGrouped && dSq <= lodBeamDistSq;
                    updateEntityVisibility(player, ti.beam, shouldSee, visibleSet);
                }
                if (ti.visual != null && ti.visual.isValid()) {
                    boolean shouldSee = !isGrouped && dSq <= lodHoloDistSq;
                    updateEntityVisibility(player, ti.visual, shouldSee, visibleSet);
                }
                if (ti.shadow != null && ti.shadow.isValid()) {
                    boolean shouldSee = !isGrouped && dSq <= lodHoloDistSq;
                    updateEntityVisibility(player, ti.shadow, shouldSee, visibleSet);
                }
            } else {
                if (ti.label != null && ti.label.isValid()) {
                    updateEntityVisibility(player, ti.label, false, visibleSet);
                }
                if (ti.beam != null && ti.beam.isValid()) {
                    updateEntityVisibility(player, ti.beam, false, visibleSet);
                }
                if (ti.visual != null && ti.visual.isValid()) {
                    updateEntityVisibility(player, ti.visual, false, visibleSet);
                }
                if (ti.shadow != null && ti.shadow.isValid()) {
                    updateEntityVisibility(player, ti.shadow, false, visibleSet);
                }
            }
        }

        // 3. Manage Farming symbols
        activeCropSymbols.values().forEach(symbol -> {
            if (symbol.location.getWorld().equals(world)) {
                boolean shouldSee = showVisuals && pLoc.distanceSquared(symbol.location) <= farmDistSq;
                symbol.forEach(bd -> {
                    if (bd.isValid()) {
                        updateEntityVisibility(player, bd, shouldSee, visibleSet);
                    }
                });
            }
        });

        // 4. Update glow state for non-RPG items by hiding and showing them
        for (Item item : activeItems.values()) {
            if (item.getWorld().equals(world) && !hiddenVanillaItems.contains(item.getEntityId())) {
                player.hideEntity(this, item);
                player.showEntity(this, item);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Database
    // -------------------------------------------------------------------------

    private void initDatabase() {
        try {
            if (!getDataFolder().exists())
                getDataFolder().mkdirs();
            File dbFile = new File(getDataFolder(), "database.db");
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement s = dbConnection.createStatement()) {
                s.execute(
                        "CREATE TABLE IF NOT EXISTS player_settings (uuid TEXT PRIMARY KEY, hidden_visuals INTEGER, magnet_disabled INTEGER DEFAULT 0)");
                // Migration: check if column exists
                try {
                    s.execute("ALTER TABLE player_settings ADD COLUMN magnet_disabled INTEGER DEFAULT 0");
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
            getLogger().severe("Could not initialize SQLite database: " + e.getMessage());
        }
    }

    private void closeDatabase() {
        try {
            if (dbConnection != null && !dbConnection.isClosed())
                dbConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            synchronized (dbConnection) {
                try (PreparedStatement ps = dbConnection
                        .prepareStatement(
                                "SELECT hidden_visuals, magnet_disabled FROM player_settings WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            boolean hidden = rs.getInt("hidden_visuals") == 1;
                            boolean magDisabled = rs.getInt("magnet_disabled") == 1;
                            Bukkit.getScheduler().runTask(this, () -> {
                                if (hidden)
                                    hiddenVisuals.add(uuid);
                                if (magDisabled)
                                    disabledMagnets.add(uuid);
                            });
                        }
                    }
                } catch (SQLException e) {
                    getLogger().severe("Could not load player data for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    private void savePlayerData(UUID uuid) {
        boolean hidden = hiddenVisuals.contains(uuid);
        boolean magDisabled = disabledMagnets.contains(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            synchronized (dbConnection) {
                try (PreparedStatement ps = dbConnection
                        .prepareStatement(
                                "INSERT OR REPLACE INTO player_settings (uuid, hidden_visuals, magnet_disabled) VALUES (?, ?, ?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, hidden ? 1 : 0);
                    ps.setInt(3, magDisabled ? 1 : 0);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    getLogger().severe("Could not save player data for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Farming Highlights
    // -------------------------------------------------------------------------

    public boolean isFarmingAllowed(Location loc) {
        if (!useWorldGuard || !wgEnabled)
            return true;
        if (isInBlockedRegion(loc))
            return false;
        return fr.skynex.lootglow.integration.WorldGuardHook.isFarmingAllowed(loc);
    }

    public void spawnCropSymbol(org.bukkit.block.Block block) {
        if (!farmingEnabled || activeCropSymbols.containsKey(block))
            return;
        if (!isFarmingAllowed(block.getLocation()))
            return;
        if (farmingMaterial == null || !farmingMaterial.isBlock()) {
            farmingMaterial = Material.EMERALD_BLOCK;
        }

        Location loc = block.getLocation().add(0.5, farmingOffset, 0.5);
        // CropSymbol pré-cache la location pour éviter block.getLocation() dans les boucles LOD
        CropSymbol cs = new CropSymbol(loc.clone());

        org.bukkit.util.Transformation barTrans = new org.bukkit.util.Transformation(
                new org.joml.Vector3f(-farmingScale / 2, farmingScale, -farmingScale / 2), // translation
                new org.joml.Quaternionf(), // left rotation
                new org.joml.Vector3f(farmingScale, farmingScale * 2.5f, farmingScale), // scale (thin and tall)
                new org.joml.Quaternionf() // right rotation
        );

        org.bukkit.util.Transformation dotTrans = new org.bukkit.util.Transformation(
                new org.joml.Vector3f(-farmingScale / 2, -farmingScale / 2, -farmingScale / 2), // translation
                new org.joml.Quaternionf(), // left rotation
                new org.joml.Vector3f(farmingScale, farmingScale, farmingScale), // scale (small cube)
                new org.joml.Quaternionf() // right rotation
        );

        // Spawn Bar
        BlockDisplay bar = block.getWorld().spawn(loc, BlockDisplay.class, ent -> {
            ent.setBlock(farmingMaterial.createBlockData());
            ent.setTransformation(barTrans);
            ent.setGlowing(true);
            ent.setGlowColorOverride(
                    Color.fromRGB(farmingGlowColor.red(), farmingGlowColor.green(), farmingGlowColor.blue()));
            ent.setViewRange((float) (farmingViewDistance / 16.0));
            ent.setPersistent(true);
            ent.getPersistentDataContainer().set(farmingKey, PersistentDataType.BYTE, (byte) 1);
            ent.setVisibleByDefault(false);
        });

        // Spawn Dot
        BlockDisplay dot = block.getWorld().spawn(loc, BlockDisplay.class, ent -> {
            ent.setBlock(farmingMaterial.createBlockData());
            ent.setTransformation(dotTrans);
            ent.setGlowing(true);
            ent.setGlowColorOverride(
                    Color.fromRGB(farmingGlowColor.red(), farmingGlowColor.green(), farmingGlowColor.blue()));
            ent.setViewRange((float) (farmingViewDistance / 16.0));
            ent.setPersistent(true);
            ent.getPersistentDataContainer().set(farmingKey, PersistentDataType.BYTE, (byte) 1);
            ent.setVisibleByDefault(false);
        });

        cs.add(bar);
        cs.add(dot);
        activeCropSymbols.put(block, cs);

        updateCropSymbolVisibilityForWorld(cs);
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

    public void removeCropSymbol(org.bukkit.block.Block block) {
        CropSymbol parts = activeCropSymbols.remove(block);
        if (parts != null) {
            parts.forEach(bd -> bd.remove());
        } else {
            // Fallback: cleanup nearby "ghost" symbols if not in map (e.g. after restart)
            Location loc = block.getLocation().add(0.5, farmingOffset, 0.5);
            block.getWorld().getNearbyEntities(loc, 0.2, 2.0, 0.2).forEach(ent -> {
                if (ent instanceof BlockDisplay bd
                        && bd.getPersistentDataContainer().has(farmingKey, PersistentDataType.BYTE)) {
                    bd.remove();
                }
            });
        }
    }

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
    private void broadcastRpgDropVisibility(Item item) {
        if (item == null || !item.isValid())
            return;
        ItemDisplay display = activeItemVisuals.get(item.getUniqueId());

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(item.getWorld()))
                continue;

            boolean wantsVanilla = hiddenVisuals.contains(p.getUniqueId());
            boolean isGrouped = groupedItems.contains(item.getUniqueId());

            if (wantsVanilla) {
                // Toggle off : voir l'item vanilla, pas le display fancy
                p.showEntity(this, item);
                if (display != null && display.isValid()) {
                    p.hideEntity(this, display);
                }
            } else {
                // Default : cacher vanilla, montrer le display
                p.hideEntity(this, item);

                // Si l'item est dans un groupe (Sac de Butin), on cache son display individuel
                if (isGrouped) {
                    if (display != null && display.isValid()) {
                        p.hideEntity(this, display);
                    }
                } else if (display != null && display.isValid()) {
                    p.showEntity(this, display);
                }
            }
        }
    }

    private void spawnItemVisual(Item item, String category, NamedTextColor color) {
        boolean isGroupVisual = useVisualBag && groupLeaders.containsKey(item.getUniqueId());
        if (!rpgDropsEnabled && !isGroupVisual)
            return;

        // Vérifier la VALIDITÉ du display existant, pas juste la présence dans la map.
        // Un display peut être removed externe (autre plugin, world tick race) sans
        // qu'on
        // soit notifié → l'entrée stale dans activeItemVisuals empêchait la recréation
        // et l'item se retrouvait setVisibleByDefault(false) mais sans display visible.
        ItemDisplay existing = activeItemVisuals.get(item.getUniqueId());
        if (existing != null) {
            if (existing.isValid()) {
                existing.setItemStack(item.getItemStack().clone());
                return; // Display valide en place, rien à faire
            }
            // Display invalide → cleanup pour permettre le respawn
            activeItemVisuals.remove(item.getUniqueId());
            entityIdMap.remove(existing.getEntityId());
        }

        if (!isGroupVisual && !rpgEnabledCategories.isEmpty()
                && (category == null || !rpgEnabledCategories.contains(category.toLowerCase())))
            return;

        // Spawn the ItemDisplay independently (NOT as passenger, to avoid vanilla item
        // showing through)
        Location spawnLoc = item.getLocation().clone();
        ItemStack visualStack = item.getItemStack().clone();

        ItemDisplay display = item.getWorld().spawn(spawnLoc, ItemDisplay.class, ent -> {
            if (isGroupVisual) {
                // Spawn directly as a visual loot bag with consistent transform
                ItemStack bag;
                if (bagMaterial == Material.PLAYER_HEAD) {
                    if (useOwnerHead && item.getThrower() != null) {
                        bag = getOwnerHead(item.getThrower());
                    } else if (!bagHeadTexture.isEmpty()) {
                        bag = createTexturedHead(bagHeadTexture);
                    } else {
                        bag = new ItemStack(bagMaterial);
                    }
                } else {
                    bag = new ItemStack(bagMaterial);
                }
                if (bagCustomModelData != 0) {
                    org.bukkit.inventory.meta.ItemMeta bMeta = bag.getItemMeta();
                    if (bMeta != null) {
                        bMeta.setCustomModelData(bagCustomModelData);
                        bag.setItemMeta(bMeta);
                    }
                }
                ent.setItemStack(bag);
                ent.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.FIXED);
                org.bukkit.util.Transformation bagTransform = new org.bukkit.util.Transformation(
                        new org.joml.Vector3f(0f, 0.05f, 0f),
                        new org.joml.Quaternionf(),
                        new org.joml.Vector3f(1.0f, 1.0f, 1.0f),
                        new org.joml.Quaternionf());
                ent.setTransformation(bagTransform);
            } else {
                ent.setItemStack(visualStack);

                Material mat = visualStack.getType();
                boolean isCustom = isCustomItem(visualStack);
                boolean isUpright = isUprightItem(mat);
                org.bukkit.entity.ItemDisplay.ItemDisplayTransform transform = isCustom
                        ? org.bukkit.entity.ItemDisplay.ItemDisplayTransform.FIXED
                        : org.bukkit.entity.ItemDisplay.ItemDisplayTransform.NONE;

                float baseScale = isUpright ? rpgBlockScale : rpgItemScale;
                if (isFishItem(mat)) {
                    baseScale *= 0.55f;
                }

                float rotX = (isCustom || isUpright) ? 0f : rpgRotation;
                float transY = isCustom ? 0.1f : 0.02f;
                if (mat == Material.TRIDENT) {
                    transY += 0.35f;
                } else if (mat == Material.SHIELD) {
                    transY += 0.42f;
                }

                org.bukkit.util.Transformation transformation = new org.bukkit.util.Transformation(
                        new org.joml.Vector3f(0, transY, 0),
                        new org.joml.Quaternionf().rotationX(rotX),
                        new org.joml.Vector3f(baseScale, baseScale, baseScale),
                        new org.joml.Quaternionf());

                ent.setTransformation(transformation);
                ent.setItemDisplayTransform(transform);
            }
            ent.setVisibleByDefault(false);
            ent.setTeleportDuration(1);
            ent.setPersistent(false);
        });

        // Hide the vanilla item model by replacing its stack with AIR visually
        // We do this by making the item entity invisible server-side
        // We now make the ItemDisplay glow instead!
        boolean shouldGlow = categoryGlow.getOrDefault(category, defaultGlow);
        if (shouldGlow) {
            display.setGlowing(true);
        }
        entityIdMap.put(display.getEntityId(), display.getUniqueId());
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam("LG_" + color.toString().toUpperCase());
            if (team != null)
                team.addEntry(display.getUniqueId().toString());
        } catch (Throwable ignored) {}
        activeItemVisuals.put(item.getUniqueId(), display);

        // Initial location handled by passenger system

        // Initial visibility
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hiddenVisuals.contains(p.getUniqueId()) && p.getWorld().equals(item.getWorld())) {
                p.showEntity(this, display);
                visibleEntities.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>()).add(display.getUniqueId());
            }
        }
    }

    /** Global sync tick: repositions all Display entities to follow their parent Item. Runs every tick. */
    private void tickGlobalSync() {
        if (!isEnabled)
            return;

        globalSyncTick++;

        java.util.List<UUID> staleEntries = null;

        for (java.util.Map.Entry<UUID, Item> entry : activeItems.entrySet()) {
            UUID itemUuid = entry.getKey();
            Item item = entry.getValue();

            if (item == null || !item.isValid()) {
                if (staleEntries == null)
                    staleEntries = new java.util.ArrayList<>();
                staleEntries.add(itemUuid);
                continue;
            }

            TrackedItem ti = trackedItems.get(itemUuid);
            if (ti == null) continue;
            ItemDisplay visual = ti.visual;
            TextDisplay label = ti.label;
            BlockDisplay beam = ti.beam;
            org.bukkit.entity.Display shadow = ti.shadow;

            Location itemLoc = item.getLocation();

            SurfaceState state = surfaceStates.get(itemUuid);
            boolean itemActuallyMoved = true;
            if (state != null) {
                // Critical optimisation: only invalidate if the ITEM itself moved
                double dx = state.lastItemX - itemLoc.getX();
                double dy = state.lastItemY - itemLoc.getY();
                double dz = state.lastItemZ - itemLoc.getZ();
                double distSq = dx*dx + dy*dy + dz*dz;
                if (distSq > 0.0001) {
                    surfaceStates.remove(itemUuid);
                } else {
                    itemActuallyMoved = false;
                }
            }

            if (itemActuallyMoved) {
                // Throttle expensive ray-tracing to once every 4 ticks per item
                if ((globalSyncTick - ti.lastRayTraceTick) >= 4) {
                    updateSurfaceAlignment(item);
                    ti.lastRayTraceTick = globalSyncTick;
                }
                state = surfaceStates.get(itemUuid); // Refresh post-alignment
            }

            double targetSurfaceY = state != null ? state.y : itemLoc.getY();
            // Position offset for visuals
            double baseWeight = 0.02;
            boolean isBlockItem = isUprightItem(item.getItemStack().getType());
            double visualYOffset = baseWeight + (isBlockItem ? (rpgBlockScale / 2.0) : 0.0);
            if (visual != null && visual.isValid() && visual.getItemStack() != null) {
                Material vMat = visual.getItemStack().getType();
                if (vMat == bagMaterial && groupLeaders.containsKey(itemUuid)) {
                    visualYOffset = 0.05;
                } else if (vMat == Material.PLAYER_HEAD) {
                    visualYOffset += 0.15;
                } else if (vMat == Material.CHEST || vMat == Material.TRAPPED_CHEST || vMat == Material.ENDER_CHEST) {
                    visualYOffset += 0.20;
                } else if (vMat == Material.BUNDLE) {
                    visualYOffset += 0.15;
                }
            }
            Entity representative = (visual != null) ? (Entity) visual : (Entity) label;

            boolean moved = false;
            if (itemActuallyMoved && representative != null && representative.isValid()) {
                Location repLoc = representative.getLocation();
                double dx = itemLoc.getX() - repLoc.getX();
                double dy = (targetSurfaceY + visualYOffset) - repLoc.getY();
                double dz = itemLoc.getZ() - repLoc.getZ();
                moved = (dx*dx + dy*dy + dz*dz) > 0.0001;
            }

            if (moved) {
                if (visual != null && visual.isValid()) {
                    Location teleportLoc = itemLoc.clone();
                    teleportLoc.setY(targetSurfaceY + visualYOffset);
                    if (state != null && state.yaw != null)
                        teleportLoc.setYaw(state.yaw);
                    if (state != null && state.pitch != null)
                        teleportLoc.setPitch(state.pitch);
                    visual.setTeleportDuration(1);
                    visual.teleport(teleportLoc);
                }

                if (label != null && label.isValid()) {
                    Location labelLoc = itemLoc.clone();
                    labelLoc.setY(targetSurfaceY + visualYOffset + holoOffset);
                    if (holoFrontOffset != 0.0) {
                        labelLoc.add(0, 0, holoFrontOffset);
                    }
                    label.setTeleportDuration(1);
                    label.teleport(labelLoc);
                }
            }

            // Always sync beam and shadow if they exist (ONLY if item moved!)
            if (itemActuallyMoved) {
                if (beam != null && beam.isValid()) {
                    Location beamTarget = itemLoc.clone();
                    beamTarget.setY(targetSurfaceY + baseWeight);
                    if (beam.getLocation().distanceSquared(beamTarget) > 0.0001) {
                        beam.setTeleportDuration(1);
                        beam.teleport(beamTarget);
                    }
                }

                if (shadow != null && shadow.isValid()) {
                    if (item.isOnGround()) {
                        double height = itemLoc.getY() - targetSurfaceY;
                        float radiusFactor = (float) Math.max(0.4, 1.0 - (height * 0.3));
                        float baseRadius = shadowScale * 0.8f;
                        if (item.getItemStack().getType().isBlock())
                            baseRadius *= 1.4f;

                        float scale = item.getItemStack().getType().isBlock() ? rpgBlockScale : rpgItemScale;
                        shadow.setShadowRadius(baseRadius * radiusFactor * (scale / 0.8f));
                        shadow.setShadowStrength((float) Math.max(0.2, 1.0 - (height * 0.5)));

                        Location shadowTarget = itemLoc.clone();
                        shadowTarget.setY(targetSurfaceY);
                        if (shadow.getLocation().distanceSquared(shadowTarget) > 0.0001) {
                            shadow.setTeleportDuration(1);
                            shadow.teleport(shadowTarget);
                        }
                    }
                }
            }

            // Physics: Water handling & animations
            if (itemActuallyMoved && visual != null && visual.isValid()) {
                Material mat = item.getItemStack().getType();
                boolean isFish = isFishItem(mat);
                boolean currentlyInWater = item.isInWater();

                if (currentlyInWater) {
                    waterLogCache.add(itemUuid);
                    if (isFish) {
                        Location vLoc = visual.getLocation();
                        vLoc.setYaw(vLoc.getYaw() + 3.0f);
                        visual.teleport(vLoc);
                    }
                } else {
                    if (waterLogCache.remove(itemUuid)) {
                        boolean isLeader = groupLeaders.containsKey(itemUuid);
                        if (!isLeader) {
                            boolean isCustom = isCustomItem(item.getItemStack());
                            boolean isUpright = isUprightItem(mat);
                            float targetRotX = (isCustom || isUpright) ? 0f : rpgRotation;
                            org.bukkit.util.Transformation t = visual.getTransformation();
                            t.getLeftRotation().set(new org.joml.Quaternionf().rotationX(targetRotX));
                            visual.setTransformation(t);
                        }
                    }
                }
            }
        }

        if (staleEntries != null) {
            for (UUID staleUuid : staleEntries) {
                removeGlow(staleUuid);
            }
        }
    }

    /** Bouncing tick: applies bounce physics to items. Runs every tick. */
    private void tickBouncing() {
        if (!bouncingEnabled) return;

        Iterator<Map.Entry<UUID, Integer>> it = bounceCounts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            UUID uuid = entry.getKey();
            int count = entry.getValue();

            Item item = activeItems.get(uuid);
            if (item == null || !item.isValid() || item.isDead()) {
                it.remove();
                recentlyBounced.remove(uuid);
                continue;
            }

            if (item.isOnGround()) {
                if (!recentlyBounced.contains(uuid)) {
                    org.bukkit.block.Block blockAt = item.getLocation().getBlock();
                    org.bukkit.block.Block blockBelow = blockAt.getRelative(org.bukkit.block.BlockFace.DOWN);
                    Material blockMat = blockBelow.getType();
                    Material atMat = blockAt.getType();
                    boolean isSnowBlock = blockMat == Material.SNOW || blockMat == Material.SNOW_BLOCK || blockMat == Material.POWDER_SNOW
                            || atMat == Material.SNOW || atMat == Material.SNOW_BLOCK || atMat == Material.POWDER_SNOW;
                    boolean isBlocked = bouncingBlockedBlocks.contains(blockMat) || bouncingBlockedBlocks.contains(atMat) || (bouncingOnlyOnSnow && !isSnowBlock);

                    if (!isBlocked && count < maxBounces) {
                        double force = jumpForce * Math.pow(bounceDamping, count + 1);
                        if (force > 0.05) {
                            Vector vel = item.getVelocity();
                            item.setVelocity(vel.setY(force));
                            surfaceStates.remove(uuid);
                            bounceCounts.put(uuid, count + 1);
                            recentlyBounced.add(uuid);
                        } else {
                            it.remove();
                            recentlyBounced.remove(uuid);
                        }
                    } else {
                        it.remove();
                        recentlyBounced.remove(uuid);
                    }
                }
            } else {
                recentlyBounced.remove(uuid);
            }
        }
    }



    public void playAspirationAnimation(Item item, Player player) {
        if (!aspirationEnabled)
            return;
        UUID uuid = item.getUniqueId();
        ItemDisplay visual = activeItemVisuals.remove(uuid);
        if (visual != null && visual.isValid()) {
            flyingVisuals.put(uuid, new VisualAnimation(visual, player));
        }
    }

    /** Aspiration animation tick: flies item visuals towards the collecting player. Runs every tick. */
    private void tickAspiration() {
        if (!aspirationEnabled) return;

        Iterator<Map.Entry<UUID, VisualAnimation>> it = flyingVisuals.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, VisualAnimation> entry = it.next();
            VisualAnimation anim = entry.getValue();

            if (!anim.target.isOnline() || !anim.display.isValid()) {
                anim.display.remove();
                it.remove();
                continue;
            }

            Location targetLoc = anim.target.getLocation().add(0, anim.target.getEyeHeight() - 0.3, 0);
            Location displayLoc = anim.display.getLocation();

            double distSq = displayLoc.distanceSquared(targetLoc);
            if (distSq < 0.09 || anim.ticks > 20) {
                anim.display.remove();
                it.remove();
                continue;
            }

            double dist = Math.sqrt(distSq);
            if (dist < 0.01) { // Guard against division by zero
                anim.display.remove();
                it.remove();
                continue;
            }

            double dx = targetLoc.getX() - displayLoc.getX();
            double dy = targetLoc.getY() - displayLoc.getY();
            double dz = targetLoc.getZ() - displayLoc.getZ();
            double speed = aspirationSpeed + (anim.ticks * 0.02);
            Location newLoc = displayLoc.clone().add((dx / dist) * speed, (dy / dist) * speed, (dz / dist) * speed);

            // Shrinking
            anim.scale = Math.max(0.1, anim.scale - 0.05);
            Transformation trans = anim.display.getTransformation();
            trans.getScale().set((float) anim.scale);
            anim.display.setTransformation(trans);

            // Increase rotation
            newLoc.setYaw(displayLoc.getYaw() + 20);
            anim.display.teleport(newLoc);
            anim.ticks++;
        }
    }


    public void openLootContainer(Player player, UUID leaderUuid) {
        if (!containerEnabled)
            return;
        java.util.List<UUID> members = groupMembers.get(leaderUuid);
        if (members == null || members.isEmpty())
            return;

        Item leaderItem = activeItems.get(leaderUuid);
        if (leaderItem != null && leaderItem.isValid()) {
            Location loc = leaderItem.getLocation();
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 0.3, 0), 20, 0.2, 0.2, 0.2, 0.1);

            ItemDisplay visual = activeItemVisuals.get(leaderUuid);
            if (visual != null && visual.isValid()) {
                org.bukkit.util.Transformation trans = visual.getTransformation();
                float baseScale = trans.getScale().x();
                if (baseScale <= 0.01f) baseScale = rpgBlockScale;
                org.bukkit.util.Transformation baseTrans = new org.bukkit.util.Transformation(
                        trans.getTranslation(), trans.getLeftRotation(),
                        new org.joml.Vector3f(baseScale, baseScale, baseScale),
                        trans.getRightRotation());
                org.bukkit.util.Transformation bumped = new org.bukkit.util.Transformation(
                        trans.getTranslation(), trans.getLeftRotation(),
                        new org.joml.Vector3f(baseScale * 1.3f, baseScale * 1.3f, baseScale * 1.3f),
                        trans.getRightRotation());
                
                visual.setInterpolationDelay(0);
                visual.setInterpolationDuration(4);
                visual.setTransformation(bumped);

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (visual.isValid()) {
                        visual.setInterpolationDelay(0);
                        visual.setInterpolationDuration(4);
                        visual.setTransformation(baseTrans);
                    }
                }, 4L);
            }
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) return;
            
            int size = ((members.size() / 9) + 1) * 9;
            if (size > 54)
                size = 54;

            org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, size,
                    miniMessage.deserialize(containerTitle));
            for (int i = 0; i < Math.min(members.size(), 54); i++) {
                Item item = activeItems.get(members.get(i));
                if (item != null && item.isValid()) {
                    gui.setItem(i, item.getItemStack());
                }
            }

            player.openInventory(gui);
            openContainers.put(player.getUniqueId(), leaderUuid);
        }, 8L);
    }

    public Map<UUID, UUID> getOpenContainers() {
        return openContainers;
    }

    public Map<UUID, List<UUID>> getGroupMembers() {
        return groupMembers;
    }

    public Map<UUID, Item> getActiveItems() {
        return activeItems;
    }

    public Set<UUID> getGroupedItems() {
        return groupedItems;
    }

    public boolean isContainerEnabled() {
        return containerEnabled;
    }

    public boolean isContainerRequireClick() {
        return containerRequireClick;
    }

    public UUID getGroupLeader(UUID itemUuid) {
        if (itemUuid == null) return null;
        if (groupMembers.containsKey(itemUuid)) return itemUuid;
        for (Map.Entry<UUID, List<UUID>> entry : groupMembers.entrySet()) {
            if (entry.getValue() != null && entry.getValue().contains(itemUuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public ItemStack createTexturedHead(String textureInput) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (textureInput == null || textureInput.trim().isEmpty())
            return head;

        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
        if (meta == null)
            return head;

        try {
            String trimmed = textureInput.trim();
            String base64Value = getBase64Texture(trimmed);
            if (base64Value != null && !base64Value.isEmpty()) {
                UUID id = UUID.nameUUIDFromBytes(base64Value.getBytes(StandardCharsets.UTF_8));
                boolean profileSet = false;

                // 1. Paper / Spigot PlayerProfile API (Paper 1.18+)
                try {
                    org.bukkit.profile.PlayerProfile profile = Bukkit.createProfile(id, "LootBag");
                    try {
                        Class<?> propClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
                        Object prop = propClass.getConstructor(String.class, String.class).newInstance("textures", base64Value);
                        profile.getClass().getMethod("setProperty", propClass).invoke(profile, prop);
                        meta.setOwnerProfile(profile);
                        profileSet = true;
                    } catch (Throwable t1) {
                        try {
                            Class<?> propClass = Class.forName("org.bukkit.profile.ProfileProperty");
                            Object prop = propClass.getConstructor(String.class, String.class).newInstance("textures", base64Value);
                            profile.getClass().getMethod("setProperty", propClass).invoke(profile, prop);
                            meta.setOwnerProfile(profile);
                            profileSet = true;
                        } catch (Throwable t2) {
                        }
                    }
                } catch (Throwable ignored) {
                }

                // 2. Authlib reflection fallback
                if (!profileSet) {
                    try {
                        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
                        Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(id, "LootBag");
                        Object property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", base64Value);
                        Object properties = gameProfileClass.getMethod("getProperties").invoke(gameProfile);
                        properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);

                        java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
                        profileField.setAccessible(true);
                        profileField.set(meta, gameProfile);
                        profileSet = true;
                    } catch (Throwable ignored) {
                    }
                }

                // 3. Texture URL fallback
                if (!profileSet) {
                    org.bukkit.profile.PlayerProfile profile = Bukkit.createProfile(id, "LootBag");
                    String textureUrl = parseTextureUrl(trimmed);
                    if (textureUrl != null) {
                        try {
                            profile.getTextures().setSkin(new java.net.URL(textureUrl));
                        } catch (Throwable ignored) {
                        }
                    }
                    meta.setOwnerProfile(profile);
                }
                head.setItemMeta(meta);
            }
        } catch (Exception e) {
            getLogger().warning("[LootGlow] Failed to set head texture: " + e.getMessage());
        }
        return head;
    }

    private String getBase64Texture(String input) {
        if (input == null || input.isEmpty()) return null;
        if (input.startsWith("eyJ")) {
            return input;
        }
        String url = parseTextureUrl(input);
        if (url == null) return null;
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String parseTextureUrl(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String trimmed = input.trim();

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed.replace("http://", "https://");
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(trimmed), StandardCharsets.UTF_8);
            int index = decoded.indexOf("\"url\":\"");
            if (index != -1) {
                String extracted = decoded.substring(index + 7, decoded.indexOf("\"", index + 7));
                return extracted.replace("http://", "https://");
            }
        } catch (Exception ignored) {
        }

        return "https://textures.minecraft.net/texture/" + trimmed;
    }

    public Item getItemForDisplay(ItemDisplay display) {
        if (display == null) return null;
        for (Map.Entry<UUID, ItemDisplay> entry : activeItemVisuals.entrySet()) {
            if (entry.getValue().equals(display)) {
                return activeItems.get(entry.getKey());
            }
        }
        return null;
    }

    public Item getItemForLabel(TextDisplay label) {
        if (label == null) return null;
        for (Map.Entry<UUID, TextDisplay> entry : activeLabels.entrySet()) {
            if (entry.getValue().equals(label)) {
                return activeItems.get(entry.getKey());
            }
        }
        return null;
    }

    public void transferLeaderVisuals(UUID oldLeader, UUID newLeader) {
        if (oldLeader == null || newLeader == null) return;
        Integer count = groupLeaders.remove(oldLeader);
        if (count != null) {
            groupLeaders.put(newLeader, Math.max(1, count - 1));
        }
        ItemDisplay visual = activeItemVisuals.remove(oldLeader);
        if (visual != null) {
            activeItemVisuals.put(newLeader, visual);
        }
        TextDisplay label = activeLabels.remove(oldLeader);
        if (label != null) {
            activeLabels.put(newLeader, label);
        }
        BlockDisplay beam = activeBeams.remove(oldLeader);
        if (beam != null) {
            activeBeams.put(newLeader, beam);
        }
        org.bukkit.entity.Display shadow = activeShadows.remove(oldLeader);
        if (shadow != null) {
            activeShadows.put(newLeader, shadow);
        }
        List<UUID> members = groupMembers.remove(oldLeader);
        if (members != null) {
            groupMembers.put(newLeader, members);
        }
    }

    public ItemStack getOwnerHead(UUID owner) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (owner == null)
            return head;
        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            head.setItemMeta(meta);
        }
        return head;
    }

    @EventHandler
    public void onItemMerge(org.bukkit.event.entity.ItemMergeEvent event) {
        removeGlow(event.getEntity().getUniqueId());
    }

    public boolean isFlatItemOrBlock(Material mat) {
        if (mat == null || mat == Material.AIR) return false;
        if (rpgForceFlatMaterials.contains(mat)) return true;
        if (rpgForceUprightMaterials.contains(mat)) return false;
        String name = mat.name();
        return name.endsWith("_DOOR") || name.endsWith("_SIGN") || name.endsWith("_HANGING_SIGN")
                || mat == Material.LADDER || mat == Material.PAINTING
                || mat == Material.ITEM_FRAME || mat == Material.GLOW_ITEM_FRAME;
    }

    public boolean isUprightItem(Material mat) {
        if (mat == null || mat == Material.AIR) return false;
        if (isFlatItemOrBlock(mat)) return false;
        if (mat.isBlock()) return true;
        String name = mat.name();
        return name.endsWith("_HEAD") || name.endsWith("_SKULL")
                || name.endsWith("_BANNER") || name.endsWith("_BED")
                || mat == Material.ARMOR_STAND || mat == Material.END_CRYSTAL;
    }

    public boolean isFishItem(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.contains("FISH") || name.contains("SALMON") || name.contains("COD");
    }

    private boolean isCustomItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta())
            return false;
        org.bukkit.persistence.PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        // Check for common custom item plugin tags
        return pdc.getKeys().stream().anyMatch(key -> key.getNamespace().equalsIgnoreCase("oraxen") ||
                key.getNamespace().equalsIgnoreCase("itemsadder") ||
                key.getNamespace().equalsIgnoreCase("nexo") ||
                key.getKey().equalsIgnoreCase("custom_item"));
    }

    private Double getMoneyAmount(ItemStack stack) {
        if (!economyEnabled || stack == null || !stack.hasItemMeta())
            return null;
        org.bukkit.persistence.PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();

        // Scan custom keys from config
        for (NamespacedKey key : economyKeys) {
            if (pdc.has(key, PersistentDataType.DOUBLE))
                return pdc.get(key, PersistentDataType.DOUBLE);
            if (pdc.has(key, PersistentDataType.INTEGER))
                return (double) pdc.get(key, PersistentDataType.INTEGER);
            if (pdc.has(key, PersistentDataType.STRING)) {
                try {
                    return Double.parseDouble(pdc.get(key, PersistentDataType.STRING));
                } catch (Exception ignored) {
                }
            }
        }

        // Internal presets (Common economy plugins)
        NamespacedKey[] presets = {
                new NamespacedKey("economyshopgui", "value"),
                new NamespacedKey("money", "amount"),
                new NamespacedKey("moneydrops", "value"),
                new NamespacedKey("tne", "value")
        };

        for (NamespacedKey key : presets) {
            if (pdc.has(key, PersistentDataType.DOUBLE))
                return pdc.get(key, PersistentDataType.DOUBLE);
            if (pdc.has(key, PersistentDataType.INTEGER))
                return (double) pdc.get(key, PersistentDataType.INTEGER);
        }

        return null;
    }

    private void updateSurfaceAlignment(Item item) {
        UUID uuid = item.getUniqueId();
        if (!item.isOnGround() || item.isInWater() || recentlyBounced.contains(uuid)) {
            surfaceStates.remove(uuid);
            return;
        }
        if (surfaceStates.containsKey(uuid))
            return;

        Location loc = item.getLocation();
        org.bukkit.util.RayTraceResult result = loc.getWorld().rayTraceBlocks(
                loc.clone().add(0, 0.4, 0),
                new org.bukkit.util.Vector(0, -1, 0),
                0.8,
                org.bukkit.FluidCollisionMode.NEVER,
                false);

        double targetY = loc.getY();
        Float forcedYaw = null;
        Float forcedPitch = null;

        if (result != null && result.getHitPosition() != null) {
            targetY = result.getHitPosition().getY();
            if (result.getHitBlock() != null) {
                org.bukkit.block.data.BlockData data = result.getHitBlock().getBlockData();
                if (data instanceof org.bukkit.block.data.type.Stairs stairs) {
                    if (stairs.getHalf() == org.bukkit.block.data.Bisected.Half.BOTTOM) {
                        switch (stairs.getFacing()) {
                            case NORTH:
                                forcedYaw = 180f;
                                break;
                            case SOUTH:
                                forcedYaw = 0f;
                                break;
                            case WEST:
                                forcedYaw = 90f;
                                break;
                            case EAST:
                                forcedYaw = 270f;
                                break;
                            default:
                                break;
                        }
                        forcedPitch = -30f;
                    }
                }
            }
        }
        surfaceStates.put(uuid, new SurfaceState(targetY, forcedYaw, forcedPitch, loc.getX(), loc.getY(), loc.getZ()));
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
    private static class TrackedItem {
        // Displays visuels
        TextDisplay label;
        BlockDisplay beam;
        ItemDisplay visual;
        org.bukkit.entity.Display shadow;
        // Timing
        Long spawnTime;
        // Hologram state
        Long lastHoloState;
        Component baseName;
        // Catégorie & particules
        String category;
        Particle particle;
        // Économie
        Double moneyAmount;
        // Throttle ray-trace (globalSyncTick du dernier appel à updateSurfaceAlignment)
        int lastRayTraceTick = -999;
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
    public void resetGlowColor(@NotNull Item item) {
        if (item == null || !item.isValid()) return;
        applyGlow(item, true);
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
    public void setBeaconBeam(@NotNull Item item, boolean enabled) {
        if (item == null || !item.isValid()) return;
        if (!enabled) {
            BlockDisplay beam = activeBeams.remove(item.getUniqueId());
            if (beam != null && beam.isValid()) beam.remove();
        } else {
            spawnBeam(item, null, defaultColor);
        }
    }

    @Override
    public void setLootProtection(@NotNull Item item, @NotNull UUID ownerUuid, long durationSeconds) {
        if (item == null || !item.isValid() || ownerUuid == null) return;
        item.setOwner(ownerUuid);
        PersistentDataContainer pdc = item.getPersistentDataContainer();
        pdc.set(new NamespacedKey(this, "owner"), PersistentDataType.STRING, ownerUuid.toString());
        pdc.set(new NamespacedKey(this, "protect_until"), PersistentDataType.LONG, System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    @Override
    public boolean isMagnetEnabled(@NotNull Player player) {
        return player != null && !disabledMagnets.contains(player.getUniqueId());
    }

    @Override
    public void setMagnetEnabled(@NotNull Player player, boolean enabled) {
        if (player == null) return;
        if (enabled) {
            disabledMagnets.remove(player.getUniqueId());
        } else {
            disabledMagnets.add(player.getUniqueId());
        }
    }

    @Override
    public void pullItemsToPlayer(@NotNull Player player, double radius) {
        if (player == null || !player.isOnline()) return;
        Location loc = player.getLocation();
        double radiusSq = radius * radius;
        for (Item item : player.getWorld().getEntitiesByClass(Item.class)) {
            if (item.isValid() && item.getLocation().distanceSquared(loc) <= radiusSq) {
                item.teleport(loc);
            }
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
        if (item == null || !item.isValid()) return;
        item.setVelocity(new Vector(0, Math.max(0.1, jumpVelocity), 0));
        item.getWorld().spawnParticle(Particle.FIREWORK, item.getLocation(), 15, 0.2, 0.2, 0.2, 0.05);
    }

    @Override
    public void setBouncingEnabled(@NotNull Item item, boolean bouncing) {
        if (item == null || !item.isValid()) return;
        if (!bouncing) {
            recentlyBounced.add(item.getUniqueId());
        } else {
            recentlyBounced.remove(item.getUniqueId());
        }
    }

    @Override
    public void setCropHighlight(@NotNull org.bukkit.block.Block cropBlock, boolean highlight) {
        if (cropBlock == null) return;
        if (highlight) {
            if (!activeCropSymbols.containsKey(cropBlock)) {
                activeCropSymbols.put(cropBlock, new CropSymbol(cropBlock.getLocation()));
            }
        } else {
            CropSymbol symbol = activeCropSymbols.remove(cropBlock);
            if (symbol != null) {
                symbol.forEach(d -> { if (d != null && d.isValid()) d.remove(); });
            }
        }
    }

    @Override
    public boolean isCropHighlighted(@NotNull org.bukkit.block.Block cropBlock) {
        return cropBlock != null && activeCropSymbols.containsKey(cropBlock);
    }

    @Nullable
    @Override
    public String getItemCategory(@NotNull Item item) {
        if (item == null) return null;
        return itemCategoriesCache.get(item.getUniqueId());
    }

    @NotNull
    @Override
    public List<Item> getNearbyGlowingItems(@NotNull Location location, double radius) {
        if (location == null || location.getWorld() == null) return List.of();
        double radiusSq = radius * radius;
        List<Item> result = new ArrayList<>();
        for (Item item : location.getWorld().getEntitiesByClass(Item.class)) {
            if (item.isValid() && item.isGlowing() && item.getLocation().distanceSquared(location) <= radiusSq) {
                result.add(item);
            }
        }
        return result;
    }
}