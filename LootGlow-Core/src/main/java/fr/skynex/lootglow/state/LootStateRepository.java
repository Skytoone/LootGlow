package fr.skynex.lootglow.state;

import fr.skynex.lootglow.managers.TrackedItemManager;
import fr.skynex.lootglow.model.CropSymbol;
import fr.skynex.lootglow.model.DelegatingMap;
import fr.skynex.lootglow.model.TrackedItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Central state repository holding runtime entity mappings, delegating maps, and caches.
 */
public class LootStateRepository {

    private Supplier<TrackedItemManager> trackedItemManagerSupplier;

    private final Map<String, NamedTextColor> itemCategories = new HashMap<>();
    private final Map<String, NamedTextColor> categoryColors = new HashMap<>();
    private final Map<String, Particle> categoryParticles = new HashMap<>();
    private final Map<String, String> categoryAnimTypes = new HashMap<>();
    private final Map<String, Sound> categorySounds = new HashMap<>();
    private final Map<UUID, TrackedItem> trackedItems = new ConcurrentHashMap<>();

    private final Map<UUID, TextDisplay> activeLabels;
    private final Map<UUID, BlockDisplay> activeBeams;
    private final Map<UUID, Long> itemSpawnTimes;
    private final Map<String, String> categoryNames = new HashMap<>();
    private final Map<String, Component> displayNameOverridesCache = new HashMap<>();
    private final Map<String, Integer> categoryLights = new HashMap<>();
    private final Set<Integer> hiddenVanillaItems = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Location> activeLights = new HashMap<>();
    private final Set<UUID> hiddenVisuals = ConcurrentHashMap.newKeySet();
    private final Set<UUID> disabledMagnets = ConcurrentHashMap.newKeySet();
    private final Map<Block, CropSymbol> activeCropSymbols = new HashMap<>();
    private final Map<UUID, Location> lastFarmingScanLocations = new HashMap<>();
    private final Map<UUID, Display> activeShadows;
    private final Map<UUID, ItemDisplay> activeItemVisuals;
    private final Map<UUID, Item> activeItems = new ConcurrentHashMap<>();

    private final Map<Integer, Component> timerComponentCache = new HashMap<>();
    private final Map<UUID, Set<UUID>> visibleEntities = new ConcurrentHashMap<>();
    private final Map<String, DustOptions> categoryDustOptions = new HashMap<>();
    private DustOptions defaultDustOptions;
    private Set<UUID> globallyVisibleEntities = ConcurrentHashMap.newKeySet();

    private final Map<UUID, Long> lastHoloState;
    private final Map<UUID, Component> baseNameCache;
    private final Map<UUID, String> itemCategoriesCache;
    private final Map<UUID, Particle> itemParticlesCache;
    private final Map<UUID, Double> itemMoneyAmounts;

    private String rawAmountFormat;
    private String rawOwnerFormat;
    private String rawBundleFormat;

    private final Map<Integer, UUID> entityIdMap = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> groupMembers = new HashMap<>();
    private final Map<UUID, UUID> openContainers = new HashMap<>();
    private final Set<UUID> groupedItems = new HashSet<>();
    private final Map<UUID, Integer> groupLeaders = new HashMap<>();

    public LootStateRepository() {
        this.activeLabels = new DelegatingMap<>(trackedItems, ti -> ti.label, (ti, v) -> ti.label = v, 
            (dUuid, iUuid) -> { if (getTrackedItemManager() != null) getTrackedItemManager().registerDisplayEntity(dUuid, iUuid); }, 
            dUuid -> { if (getTrackedItemManager() != null) getTrackedItemManager().unregisterDisplayEntity(dUuid); });

        this.activeBeams = new DelegatingMap<>(trackedItems, ti -> ti.beam, (ti, v) -> ti.beam = v, 
            (dUuid, iUuid) -> { if (getTrackedItemManager() != null) getTrackedItemManager().registerDisplayEntity(dUuid, iUuid); }, 
            dUuid -> { if (getTrackedItemManager() != null) getTrackedItemManager().unregisterDisplayEntity(dUuid); });

        this.itemSpawnTimes = new DelegatingMap<>(trackedItems, ti -> ti.spawnTime, (ti, v) -> ti.spawnTime = v);

        this.activeShadows = new DelegatingMap<>(trackedItems, ti -> ti.shadow, (ti, v) -> ti.shadow = v, 
            (dUuid, iUuid) -> { if (getTrackedItemManager() != null) getTrackedItemManager().registerDisplayEntity(dUuid, iUuid); }, 
            dUuid -> { if (getTrackedItemManager() != null) getTrackedItemManager().unregisterDisplayEntity(dUuid); });

        this.activeItemVisuals = new DelegatingMap<>(trackedItems, ti -> ti.visual, (ti, v) -> ti.visual = v, 
            (dUuid, iUuid) -> { if (getTrackedItemManager() != null) getTrackedItemManager().registerDisplayEntity(dUuid, iUuid); }, 
            dUuid -> { if (getTrackedItemManager() != null) getTrackedItemManager().unregisterDisplayEntity(dUuid); });

        this.lastHoloState = new DelegatingMap<>(trackedItems, ti -> ti.lastHoloState, (ti, v) -> ti.lastHoloState = v);
        this.baseNameCache = new DelegatingMap<>(trackedItems, ti -> ti.baseName, (ti, v) -> ti.baseName = v);
        this.itemCategoriesCache = new DelegatingMap<>(trackedItems, ti -> ti.category, (ti, v) -> ti.category = v);
        this.itemParticlesCache = new DelegatingMap<>(trackedItems, ti -> ti.particle, (ti, v) -> ti.particle = v);
        this.itemMoneyAmounts = new DelegatingMap<>(trackedItems, ti -> ti.moneyAmount, (ti, v) -> ti.moneyAmount = v);
    }

    public void setTrackedItemManagerSupplier(Supplier<TrackedItemManager> supplier) {
        this.trackedItemManagerSupplier = supplier;
    }

    private TrackedItemManager getTrackedItemManager() {
        return trackedItemManagerSupplier != null ? trackedItemManagerSupplier.get() : null;
    }

    // Getters for state collections
    public Map<String, NamedTextColor> getItemCategories() { return itemCategories; }
    public Map<String, NamedTextColor> getCategoryColors() { return categoryColors; }
    public Map<String, Particle> getCategoryParticles() { return categoryParticles; }
    public Map<String, String> getCategoryAnimTypes() { return categoryAnimTypes; }
    public Map<String, Sound> getCategorySounds() { return categorySounds; }
    public Map<UUID, TrackedItem> getTrackedItems() { return trackedItems; }
    public Map<UUID, TextDisplay> getActiveLabels() { return activeLabels; }
    public Map<UUID, BlockDisplay> getActiveBeams() { return activeBeams; }
    public Map<UUID, Long> getItemSpawnTimes() { return itemSpawnTimes; }
    public Map<String, String> getCategoryNames() { return categoryNames; }
    public Map<String, Component> getDisplayNameOverridesCache() { return displayNameOverridesCache; }
    public Map<String, Integer> getCategoryLights() { return categoryLights; }
    public Set<Integer> getHiddenVanillaItems() { return hiddenVanillaItems; }
    public Map<UUID, Location> getActiveLights() { return activeLights; }
    public Set<UUID> getHiddenVisuals() { return hiddenVisuals; }
    public Set<UUID> getDisabledMagnets() { return disabledMagnets; }
    public Map<Block, CropSymbol> getActiveCropSymbols() { return activeCropSymbols; }
    public Map<UUID, Location> getLastFarmingScanLocations() { return lastFarmingScanLocations; }
    public Map<UUID, Display> getActiveShadows() { return activeShadows; }
    public Map<UUID, ItemDisplay> getActiveItemVisuals() { return activeItemVisuals; }
    public Map<UUID, Item> getActiveItems() { return activeItems; }
    public Map<Integer, Component> getTimerComponentCache() { return timerComponentCache; }
    public Map<UUID, Set<UUID>> getVisibleEntities() { return visibleEntities; }
    public Map<String, DustOptions> getCategoryDustOptions() { return categoryDustOptions; }
    public DustOptions getDefaultDustOptions() { return defaultDustOptions; }
    public void setDefaultDustOptions(DustOptions options) { this.defaultDustOptions = options; }
    public Set<UUID> getGloballyVisibleEntities() { return globallyVisibleEntities; }
    public void setGloballyVisibleEntities(Set<UUID> entities) { this.globallyVisibleEntities = entities; }

    private final Set<UUID> recentlyBounced = ConcurrentHashMap.newKeySet();
    public Set<UUID> getRecentlyBounced() { return recentlyBounced; }

    public Map<UUID, Long> getLastHoloState() { return lastHoloState; }
    public Map<UUID, Component> getBaseNameCache() { return baseNameCache; }
    public Map<UUID, String> getItemCategoriesCache() { return itemCategoriesCache; }
    public Map<UUID, Particle> getItemParticlesCache() { return itemParticlesCache; }
    public Map<UUID, Double> getItemMoneyAmounts() { return itemMoneyAmounts; }

    public String getRawAmountFormat() { return rawAmountFormat; }
    public void setRawAmountFormat(String rawAmountFormat) { this.rawAmountFormat = rawAmountFormat; }
    public String getRawOwnerFormat() { return rawOwnerFormat; }
    public void setRawOwnerFormat(String rawOwnerFormat) { this.rawOwnerFormat = rawOwnerFormat; }
    public String getRawBundleFormat() { return rawBundleFormat; }
    public void setRawBundleFormat(String rawBundleFormat) { this.rawBundleFormat = rawBundleFormat; }

    public Map<Integer, UUID> getEntityIdMap() { return entityIdMap; }
    public Map<UUID, List<UUID>> getGroupMembers() { return groupMembers; }
    public Map<UUID, UUID> getOpenContainers() { return openContainers; }
    public Set<UUID> getGroupedItems() { return groupedItems; }
    public Map<UUID, Integer> getGroupLeaders() { return groupLeaders; }

    public void clearCaches() {
        itemCategories.clear();
        categoryColors.clear();
        categoryParticles.clear();
        categoryAnimTypes.clear();
        categorySounds.clear();
        categoryNames.clear();
        displayNameOverridesCache.clear();
        categoryLights.clear();
        categoryDustOptions.clear();
        timerComponentCache.clear();
        visibleEntities.clear();
    }
}
