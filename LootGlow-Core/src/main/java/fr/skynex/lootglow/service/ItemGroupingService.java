package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.model.TrackedItem;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Handles spatial clustering and grouping of nearby items into Loot Bags.
 */
public class ItemGroupingService {

    private final LootGlow plugin;
    private org.bukkit.scheduler.BukkitTask groupingTask;

    public ItemGroupingService(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void stopGroupingTask() {
        if (groupingTask != null) {
            groupingTask.cancel();
            groupingTask = null;
        }
    }

    /**
     * Compute item spatial clusters and perform atomic updates on grouping maps.
     */
    public void processItemGrouping(Map<UUID, ?> trackedItems,
                                    Map<UUID, Item> activeItems,
                                    Map<UUID, String> itemCategoriesCache,
                                    Set<UUID> groupedItems,
                                    Map<UUID, Integer> groupLeaders,
                                    Map<UUID, List<UUID>> groupMembers,
                                    int minItems,
                                    double radiusSq,
                                    boolean byCategory) {

        Map<World, List<Item>> worldItemsMap = new HashMap<>();
        for (UUID uuid : trackedItems.keySet()) {
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
            if (size < minItems) continue;

            double[] xs = new double[size];
            double[] ys = new double[size];
            double[] zs = new double[size];
            String[] cats = new String[size];

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
                if (processed.contains(uuid)) continue;

                List<Item> nearby = new ArrayList<>();
                nearby.add(item);
                String cat = cats[i];

                for (int j = i + 1; j < size; j++) {
                    Item other = items.get(j);
                    if (processed.contains(other.getUniqueId())) continue;

                    double dx = xs[i] - xs[j];
                    double dy = ys[i] - ys[j];
                    double dz = zs[i] - zs[j];

                    if ((dx * dx + dy * dy + dz * dz) < radiusSq) {
                        if (!byCategory || Objects.equals(cat, cats[j])) {
                            nearby.add(other);
                        }
                    }
                }

                Set<Material> materials = new HashSet<>();
                for (Item ni : nearby) {
                    materials.add(ni.getItemStack().getType());
                }

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
        groupedItems.clear();
        groupedItems.addAll(tempGrouped);
        groupLeaders.clear();
        groupLeaders.putAll(tempLeaders);
        groupMembers.clear();
        groupMembers.putAll(tempMembers);
    }

    public void startGroupingTask(fr.skynex.lootglow.model.ItemGroupingContext ctx) {
        if (ctx == null) return;
        startGroupingTask(ctx.isEnabled(), ctx.groupingEnabled(), ctx.trackedItems(), ctx.activeItems(),
                ctx.itemCategoriesCache(), ctx.groupedItems(), ctx.groupLeaders(), ctx.groupMembers(),
                ctx.activeItemVisuals(), ctx.useVisualBag(), ctx.bagMaterial(), ctx.bagHeadTexture(),
                ctx.useOwnerHead(), ctx.bagCustomModelData(), ctx.rpgRotation(), ctx.holoShowTimer(),
                ctx.rawBundleFormat(), ctx.itemCategories(), ctx.defaultColor(), ctx.miniMessage());
    }

    public void startGroupingTask(boolean isEnabled,
                                  boolean groupingEnabled,
                                  Map<UUID, ?> trackedItems,
                                  Map<UUID, Item> activeItems,
                                  Map<UUID, String> itemCategoriesCache,
                                  Set<UUID> groupedItems,
                                  Map<UUID, Integer> groupLeaders,
                                  Map<UUID, List<UUID>> groupMembers,
                                  Map<UUID, org.bukkit.entity.ItemDisplay> activeItemVisuals,
                                  boolean useVisualBag,
                                  Material bagMaterial,
                                  String bagHeadTexture,
                                  boolean useOwnerHead,
                                  int bagCustomModelData,
                                  float rpgRotation,
                                  boolean holoShowTimer,
                                  String rawBundleFormat,
                                  Map<String, net.kyori.adventure.text.format.NamedTextColor> itemCategories,
                                  net.kyori.adventure.text.format.NamedTextColor defaultColor,
                                  net.kyori.adventure.text.minimessage.MiniMessage miniMessage) {
        stopGroupingTask();
        groupingTask = fr.skynex.lootglow.util.FoliaScheduler.runTimer(plugin, () -> {
            if (!isEnabled || !groupingEnabled)
                return;

            double radius = plugin.getConfig().getDouble("settings.grouping.radius", 2.0);
            int minItems = plugin.getConfig().getInt("settings.grouping.min-items", 5);
            boolean byCategory = plugin.getConfig().getBoolean("settings.grouping.group-by-category", true);

            double radiusSq = radius * radius;
            var lodMgr = plugin.getService(fr.skynex.lootglow.managers.LODManager.class);
            double holoDistSq = lodMgr != null ? lodMgr.getLodHoloDistanceSquared() : 576.0;

            groupedItems.clear();
            groupLeaders.clear();

            // Cache player positions primitives once to eliminate dynamic Location allocation storm
            java.util.Collection<? extends org.bukkit.entity.Player> onlinePlayers = org.bukkit.Bukkit.getOnlinePlayers();
            final int numPlayers = onlinePlayers.size();
            final double[] px = new double[numPlayers];
            final double[] py = new double[numPlayers];
            final double[] pz = new double[numPlayers];
            final World[] pWorlds = new World[numPlayers];
            int pIdx = 0;
            for (org.bukkit.entity.Player p : onlinePlayers) {
                px[pIdx] = p.getX();
                py[pIdx] = p.getY();
                pz[pIdx] = p.getZ();
                pWorlds[pIdx] = p.getWorld();
                pIdx++;
            }

            processItemGrouping(trackedItems, activeItems, itemCategoriesCache, groupedItems, groupLeaders, groupMembers, minItems, radiusSq, byCategory);

            // Update visual bag model
            if (useVisualBag) {
                activeItemVisuals.forEach((uuid, visual) -> {
                    if (!visual.isValid())
                        return;
                    Item item = activeItems.get(uuid);
                    if (item == null || !item.isValid())
                        return;

                    boolean isLeader = groupLeaders.containsKey(uuid);
                    org.bukkit.inventory.ItemStack currentStack = visual.getItemStack();
                    if (isLeader) {
                        if (currentStack == null || currentStack.getType() != bagMaterial) {
                            org.bukkit.inventory.ItemStack bag;
                            var visDispMgr = plugin.getService(fr.skynex.lootglow.managers.VisualDisplayManager.class);
                            if (bagMaterial == Material.PLAYER_HEAD) {
                                if (useOwnerHead && item.getThrower() != null) {
                                    bag = visDispMgr != null ? visDispMgr.getOwnerHead(item.getThrower()) : new org.bukkit.inventory.ItemStack(Material.PLAYER_HEAD);
                                } else if (!bagHeadTexture.isEmpty()) {
                                    bag = visDispMgr != null ? visDispMgr.createTexturedHead(bagHeadTexture) : new org.bukkit.inventory.ItemStack(Material.PLAYER_HEAD);
                                } else {
                                    bag = new org.bukkit.inventory.ItemStack(bagMaterial);
                                }
                            } else {
                                bag = new org.bukkit.inventory.ItemStack(bagMaterial);
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
                            org.bukkit.util.Transformation t = visual.getTransformation();
                            t.getLeftRotation().set(new org.joml.Quaternionf());
                            t.getTranslation().set(0f, 0.30f, 0f);
                            t.getScale().set(1.0f, 1.0f, 1.0f);
                            visual.setTransformation(t);
                        }
                    } else {
                        if (currentStack != null && currentStack.getType() == bagMaterial) {
                            visual.setItemStack(item.getItemStack());
                            visual.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.FIXED);
                            org.bukkit.util.Transformation t = visual.getTransformation();
                            Material itemMat = item.getItemStack().getType();
                            var cfgMgr = plugin.getConfigManager();
                            boolean isCustom = fr.skynex.lootglow.util.ItemTypeClassifier.isCustomItem(item.getItemStack());
                            boolean isUpright = fr.skynex.lootglow.util.ItemTypeClassifier.isUprightItem(itemMat, cfgMgr != null ? cfgMgr.getRpgForceFlatMaterials() : java.util.Collections.emptySet(), cfgMgr != null ? cfgMgr.getRpgForceUprightMaterials() : java.util.Collections.emptySet());
                            float targetRotX = (isCustom || isUpright) ? 0f : rpgRotation;
                            t.getLeftRotation().set(new org.joml.Quaternionf().rotationX(targetRotX));
                            visual.setTransformation(t);
                        }
                    }
                });
            }

            // Hide member vanilla item entities from players
            groupedItems.forEach(gUuid -> {
                Item gItem = activeItems.get(gUuid);
                if (gItem != null && gItem.isValid()) {
                    for (org.bukkit.entity.Player p : gItem.getWorld().getPlayers()) {
                        p.hideEntity(plugin, gItem);
                    }
                }
            });

            // Update holograms with visibility check and caching
            for (Map.Entry<UUID, ?> entry : trackedItems.entrySet()) {
                UUID uuid = entry.getKey();
                TrackedItem ti = (TrackedItem) entry.getValue();
                org.bukkit.entity.TextDisplay display = ti.label;
                if (display == null || !display.isValid())
                    continue;

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

                if (!hasPlayerNearby && !isGroupLeader && !isGrouped)
                    continue;

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

                net.kyori.adventure.text.Component newContent;
                if (isGroupLeader) {
                    int count = groupLeaders.get(uuid);
                    newContent = fr.skynex.lootglow.util.ColorUtil.parse(rawBundleFormat.replace("<count>", String.valueOf(count)));
                } else if (!isGrouped) {
                    net.kyori.adventure.text.format.NamedTextColor color = itemCategories.get(ti.category);
                    if (color == null)
                        color = defaultColor;

                    net.kyori.adventure.text.Component baseName = ti.baseName;
                    var holoSvc = plugin.getService(HologramService.class);
                    var cfgMgr = plugin.getConfigManager();
                    if (baseName == null) {
                        baseName = holoSvc != null ? holoSvc.calculateBaseName(item, color, plugin.getStateRepository().getDisplayNameOverridesCache(), plugin.getStateRepository().getItemMoneyAmounts(), cfgMgr != null ? cfgMgr.getEconomyFormat() : "", cfgMgr != null ? cfgMgr.getEconomyPrefix() : "") : net.kyori.adventure.text.Component.empty();
                        ti.baseName = baseName;
                    }
                    newContent = holoSvc != null ? holoSvc.buildFinalName(item, baseName, cfgMgr != null && cfgMgr.isHoloShowAmount(), plugin.getStateRepository().getRawAmountFormat(), cfgMgr != null && cfgMgr.isProtectionEnabled(), cfgMgr != null ? cfgMgr.getProtectionDuration() : 10, plugin.getStateRepository().getItemSpawnTimes(), plugin.getStateRepository().getRawOwnerFormat(), org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"), cfgMgr != null && cfgMgr.isHoloShowTimer(), plugin.getStateRepository().getTimerComponentCache(), cfgMgr != null && cfgMgr.isHoloTimerNewLine()) : baseName;
                } else {
                    display.text(net.kyori.adventure.text.Component.empty());
                    ti.lastHoloState = -1L;
                    continue;
                }

                display.text(newContent);
                ti.lastHoloState = stateHash;
            }
        }, 20L, 20L);
    }
}
