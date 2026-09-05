package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

import fr.skynex.lootglow.managers.HologramManager;
import fr.skynex.lootglow.managers.HologramRenderer;
import fr.skynex.lootglow.managers.LODManager;
import fr.skynex.lootglow.managers.LootProtectionManager;
import fr.skynex.lootglow.managers.RarityManager;
import fr.skynex.lootglow.managers.TrackedItemManager;
import fr.skynex.lootglow.util.ItemNameFormatter;
import fr.skynex.lootglow.util.ItemTypeClassifier;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles TextDisplay hologram creation, base name resolution, and dynamic tag formatting.
 */
public class HologramService {

    private final LootGlow plugin;
    private static final Component FREED_COMPONENT = fr.skynex.lootglow.util.ColorUtil.parse("<green>🔓 Libéré</green>");
    private final Map<Long, Component> protComponentCache = new java.util.concurrent.ConcurrentHashMap<>();

    public HologramService(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Component calculateBaseName(Item item, NamedTextColor color,
                                       Map<String, Component> displayNameOverridesCache,
                                       Map<UUID, Double> itemMoneyAmounts,
                                       String economyFormat,
                                       String economyPrefix) {
        var holoRen = plugin.getService(HologramRenderer.class);
        if (holoRen != null) {
            Component customHolo = holoRen.getCustomHologram(item, null);
            if (customHolo != null) {
                return customHolo.decoration(TextDecoration.ITALIC, false);
            }
        }

        String customId = ItemTypeClassifier.getInternalId(item.getItemStack());
        String matName = item.getItemStack().getType().name();

        Component name = displayNameOverridesCache.get(customId);
        if (name == null) name = displayNameOverridesCache.get(matName);

        if (name == null) {
            Double amount = itemMoneyAmounts.get(item.getUniqueId());
            if (amount != null) {
                String formatted = economyFormat
                        .replace("<prefix>", economyPrefix)
                        .replace("<amount>", String.format("%.2f", amount));
                name = fr.skynex.lootglow.util.ColorUtil.parse(formatted);
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
        }

        if (name == null) {
            var nameFmt = plugin.getService(ItemNameFormatter.class);
            name = nameFmt != null ? nameFmt.getItemName(item.getItemStack()) : Component.text(matName);
        }

        if (name.color() == null && !name.children().stream().anyMatch(c -> c.color() != null)) {
            name = name.color(color);
        }

        return name.decoration(TextDecoration.ITALIC, false);
    }

    public Component buildFinalName(Item item, Component baseName,
                                    boolean holoShowAmount,
                                    String rawAmountFormat,
                                    boolean protectionEnabled,
                                    int protectionDuration,
                                    Map<UUID, Long> itemSpawnTimes,
                                    String rawOwnerFormat,
                                    boolean usePapi,
                                    boolean holoShowTimer,
                                    Map<Integer, Component> timerComponentCache,
                                    boolean holoTimerNewLine) {

        Component result = baseName;

        var rarityMgr = plugin.getService(RarityManager.class);
        var trackedMgr = plugin.getService(TrackedItemManager.class);
        if (rarityMgr != null) {
            fr.skynex.lootglow.model.TrackedItem ti = trackedMgr != null ? trackedMgr.getTrackedItem(item.getUniqueId()) : null;
            fr.skynex.lootglow.managers.RarityManager.ItemRarity rarity = ti != null ? ti.rarity : null;
            if (rarity == null) {
                rarity = rarityMgr.detectRarity(item.getItemStack());
                if (ti != null) ti.rarity = rarity;
            }
            Component rarityHeader = rarityMgr.getRarityHeaderComponent(rarity);
            if (rarityHeader != null) {
                result = rarityHeader.append(Component.newline()).append(result);
            }
        }

        if (holoShowAmount && item.getItemStack().getAmount() > 1) {
            String amountText = rawAmountFormat.replace("<amount>", String.valueOf(item.getItemStack().getAmount()));
            result = result.append(fr.skynex.lootglow.util.ColorUtil.parse(amountText));
        }

        if (protectionEnabled) {
            Long spawnTime = itemSpawnTimes.get(item.getUniqueId());
            if (spawnTime != null) {
                long elapsed = (System.currentTimeMillis() - spawnTime) / 1000;
                long remaining = protectionDuration - elapsed;
                var lootProtMgr = plugin.getService(LootProtectionManager.class);
                UUID ownerUuid = lootProtMgr != null ? lootProtMgr.getLootOwner(item) : null;
                if (ownerUuid == null) {
                    ownerUuid = item.getThrower();
                }
                boolean isProtected = (lootProtMgr != null && lootProtMgr.isLootProtected(item)) || ownerUuid != null;
                if (isProtected && remaining > 0) {
                    String defaultOwnerName = plugin.getConfig().getString("settings.loot-protection.default-owner-name", "Inconnu");
                    String ownerName = defaultOwnerName;
                    if (ownerUuid != null) {
                        Player ownerPlayer = Bukkit.getPlayer(ownerUuid);
                        if (ownerPlayer != null) {
                            ownerName = ownerPlayer.getName();
                        } else {
                            org.bukkit.OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(ownerUuid);
                            if (offPlayer.getName() != null) {
                                ownerName = offPlayer.getName();
                            }
                        }
                    }
                    long timerKey = (((long) remaining) << 32) | (protectionDuration & 0xFFFFFFFFL);
                    Component protComp = protComponentCache.computeIfAbsent(timerKey, k -> {
                        String progressBar = buildProgressBar(remaining, protectionDuration);
                        return fr.skynex.lootglow.util.ColorUtil.parse("<gold>🔒 Protégé <yellow>" + progressBar + "</yellow> (" + remaining + "s)</gold>");
                    });
                    result = result.append(Component.newline()).append(protComp);
                    if (rawOwnerFormat != null && !rawOwnerFormat.isEmpty() && ownerUuid != null) {
                        result = result.append(Component.newline()).append(fr.skynex.lootglow.util.ColorUtil.parse(rawOwnerFormat.replace("<owner>", ownerName)));
                    }
                } else if (isProtected && remaining >= -3) {
                    if (remaining == 0) {
                        item.getWorld().playSound(item.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.4f, 1.3f);
                    }
                    result = result.append(Component.newline()).append(FREED_COMPONENT);
                }
            }
        }

        if (usePapi) {
            String legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(result);
            if (legacy.indexOf('%') != -1) {
                legacy = PlaceholderAPI.setPlaceholders((Player) null, legacy);
                result = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(legacy);
            }
        }

        if (holoShowTimer) {
            int remaining = Math.max(0, (6000 - item.getTicksLived()) / 20);
            Component timerComp = timerComponentCache.get(remaining);
            if (timerComp == null) {
                var msgSvc = plugin.getService(MessageService.class);
                timerComp = fr.skynex.lootglow.util.ColorUtil.parse(msgSvc != null ? msgSvc.getRawTimerFormat().replace("<time>", String.valueOf(remaining)) : "");
            }
            if (timerComp != null) {
                if (holoTimerNewLine) {
                    result = result.append(Component.newline()).append(timerComp);
                } else {
                    result = result.append(timerComp);
                }
            }
        }

        return result;
    }

    public void updateHologram(Item item, NamedTextColor color, fr.skynex.lootglow.model.HologramContext ctx) {
        if (ctx == null) return;
        updateHologram(item, color, ctx.holoEnabled(), ctx.itemCategoriesCache(), ctx.holoHideUncategorized(),
                ctx.activeLabels(), ctx.groupLeaders(), ctx.lastHoloState(), ctx.baseNameCache(),
                ctx.displayNameOverridesCache(), ctx.itemMoneyAmounts(), ctx.economyFormat(), ctx.economyPrefix(),
                ctx.holoShowAmount(), ctx.rawAmountFormat(), ctx.protectionEnabled(), ctx.protectionDuration(),
                ctx.itemSpawnTimes(), ctx.rawOwnerFormat(), ctx.usePapi(), ctx.holoShowTimer(),
                ctx.timerComponentCache(), ctx.holoTimerNewLine());
    }

    public void updateHologram(Item item, NamedTextColor color,
                               boolean holoEnabled,
                               Map<UUID, String> itemCategoriesCache,
                               boolean holoHideUncategorized,
                               Map<UUID, TextDisplay> activeLabels,
                               Map<UUID, ?> groupLeaders,
                               Map<UUID, Long> lastHoloState,
                               Map<UUID, Component> baseNameCache,
                               Map<String, Component> displayNameOverridesCache,
                               Map<UUID, Double> itemMoneyAmounts,
                               String economyFormat,
                               String economyPrefix,
                               boolean holoShowAmount,
                               String rawAmountFormat,
                               boolean protectionEnabled,
                               int protectionDuration,
                               Map<UUID, Long> itemSpawnTimes,
                               String rawOwnerFormat,
                               boolean usePapi,
                               boolean holoShowTimer,
                               Map<Integer, Component> timerComponentCache,
                               boolean holoTimerNewLine) {
        if (!holoEnabled || item == null || item.isDead()) return;

        UUID uuid = item.getUniqueId();
        String cat = itemCategoriesCache.get(uuid);
        var trackedMgr = plugin.getService(TrackedItemManager.class);
        if (cat == null && trackedMgr != null) cat = trackedMgr.getItemCategory(uuid);
        if (holoHideUncategorized && cat == null) {
            var holoMgr = plugin.getService(HologramManager.class);
            if (holoMgr != null) holoMgr.removeHologram(uuid);
            return;
        }

        TextDisplay display = activeLabels.get(uuid);
        if (display == null || !display.isValid()) {
            if (display != null) activeLabels.remove(uuid);
            boolean seeThrough = plugin.getConfigManager() != null ? plugin.getConfigManager().isHoloSeeThrough() : plugin.getConfig().getBoolean("settings.holograms.see-through", false);
            double viewDistance = plugin.getConfigManager() != null ? plugin.getConfigManager().getHoloViewDistance() : plugin.getConfig().getDouble("settings.holograms.view-distance", 48.0);
            boolean background = plugin.getConfigManager() != null ? plugin.getConfigManager().isHoloBackground() : plugin.getConfig().getBoolean("settings.holograms.background", false);
            double offset = plugin.getConfigManager() != null ? plugin.getConfigManager().getHoloOffset() : plugin.getConfig().getDouble("settings.holograms.offset", 0.6);
            var lodMgr = plugin.getService(LODManager.class);
            var stateRepo = plugin.getStateRepository();
            spawnHologram(item, color, holoEnabled, itemCategoriesCache, holoHideUncategorized,
                    activeLabels, seeThrough, viewDistance, background, offset,
                    baseNameCache, displayNameOverridesCache, itemMoneyAmounts, economyFormat, economyPrefix,
                    holoShowAmount, rawAmountFormat, protectionEnabled, protectionDuration, itemSpawnTimes,
                    rawOwnerFormat, usePapi, holoShowTimer, timerComponentCache, holoTimerNewLine,
                    lodMgr != null ? lodMgr.getLodHoloDistanceSquared() : 1024.0,
                    stateRepo != null ? stateRepo.getHiddenVisuals() : java.util.Collections.emptySet(),
                    stateRepo != null ? stateRepo.getVisibleEntities() : java.util.Collections.emptyMap());
            return;
        }

        if (groupLeaders.containsKey(uuid)) return;

        int currentSec = (6000 - item.getTicksLived()) / 20;
        int currentCount = item.getItemStack().getAmount();

        long stateHash = ((long) currentSec << 32) | ((long) currentCount << 16);
        Long lastHash = lastHoloState.get(uuid);

        if (lastHash != null && lastHash == stateHash) return;

        Component baseName = baseNameCache.get(uuid);
        if (baseName == null) {
            baseName = calculateBaseName(item, color, displayNameOverridesCache, itemMoneyAmounts, economyFormat, economyPrefix);
            baseNameCache.put(uuid, baseName);
        }
        display.text(buildFinalName(item, baseName, holoShowAmount, rawAmountFormat, protectionEnabled, protectionDuration, itemSpawnTimes, rawOwnerFormat, usePapi, holoShowTimer, timerComponentCache, holoTimerNewLine));
        lastHoloState.put(uuid, stateHash);
    }

    public void spawnHologram(Item item, NamedTextColor color,
                              boolean holoEnabled,
                              Map<UUID, String> itemCategoriesCache,
                              boolean holoHideUncategorized,
                              Map<UUID, TextDisplay> activeLabels,
                              boolean holoSeeThrough,
                              double holoViewDistance,
                              boolean holoBackground,
                              double holoOffset,
                              Map<UUID, Component> baseNameCache,
                              Map<String, Component> displayNameOverridesCache,
                              Map<UUID, Double> itemMoneyAmounts,
                              String economyFormat,
                              String economyPrefix,
                              boolean holoShowAmount,
                              String rawAmountFormat,
                              boolean protectionEnabled,
                              int protectionDuration,
                              Map<UUID, Long> itemSpawnTimes,
                              String rawOwnerFormat,
                              boolean usePapi,
                              boolean holoShowTimer,
                              Map<Integer, Component> timerComponentCache,
                              boolean holoTimerNewLine,
                              double lodHoloDistSq,
                              Set<UUID> hiddenVisuals,
                              Map<UUID, Set<UUID>> visibleEntities) {

        if (!holoEnabled || item == null) return;

        UUID uuid = item.getUniqueId();
        String cat = itemCategoriesCache.get(uuid);
        var trackedMgr = plugin.getService(TrackedItemManager.class);
        if (cat == null && trackedMgr != null) cat = trackedMgr.getItemCategory(uuid);
        if (holoHideUncategorized && cat == null) {
            var holoMgr = plugin.getService(HologramManager.class);
            if (holoMgr != null) holoMgr.removeHologram(uuid);
            return;
        }

        TextDisplay existing = activeLabels.get(uuid);
        if (existing != null && existing.isValid()) return;

        Component baseName = calculateBaseName(item, color, displayNameOverridesCache, itemMoneyAmounts, economyFormat, economyPrefix);
        baseNameCache.put(uuid, baseName);

        Component finalName = buildFinalName(item, baseName, holoShowAmount, rawAmountFormat, protectionEnabled, protectionDuration, itemSpawnTimes, rawOwnerFormat, usePapi, holoShowTimer, timerComponentCache, holoTimerNewLine);

        Location loc = item.getLocation().add(0, holoOffset, 0);
        TextDisplay label = item.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.text(finalName);
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(holoSeeThrough);
            td.setViewRange((float) (holoViewDistance / 64.0));
            td.setTeleportDuration(2);
            td.setPersistent(false);

            if (holoBackground) {
                td.setBackgroundColor(org.bukkit.Color.fromARGB(160, 0, 0, 0));
            } else {
                td.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            }

            td.setTransformation(new Transformation(
                    new org.joml.Vector3f(0, 0, 0),
                    new org.joml.Quaternionf(),
                    new org.joml.Vector3f(1.0f, 1.0f, 1.0f),
                    new org.joml.Quaternionf()
            ));
        });

        activeLabels.put(uuid, label);
        if (trackedMgr != null) {
            fr.skynex.lootglow.model.TrackedItem ti = trackedMgr.getOrCreateTrackedItem(uuid);
            ti.label = label;
            trackedMgr.registerDisplayEntity(label.getUniqueId(), uuid);
        }

        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("[LootGlow Debug] Spawned TextDisplay hologram for item " + item.getItemStack().getType() + " (UUID: " + item.getUniqueId() + ", Label UUID: " + label.getUniqueId() + ")");
        }

        for (Player p : item.getWorld().getPlayers()) {
            UUID pUuid = p.getUniqueId();
            if (hiddenVisuals.contains(pUuid)) {
                p.hideEntity(plugin, label);
                continue;
            }
            if (p.getLocation().distanceSquared(item.getLocation()) <= lodHoloDistSq) {
                p.showEntity(plugin, label);
                visibleEntities.computeIfAbsent(pUuid, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(label.getUniqueId());
            } else {
                p.hideEntity(plugin, label);
            }
        }
    }

    public void refreshHologram(Item item, boolean holoEnabled, boolean holoHideUncategorized,
                                Map<UUID, String> itemCategoriesCache, Map<String, NamedTextColor> itemCategories,
                                NamedTextColor defaultColor, Map<UUID, Long> lastHoloState) {
        if (!holoEnabled || item == null || item.isDead())
            return;
        UUID uuid = item.getUniqueId();
        String cat = itemCategoriesCache.get(uuid);
        if (holoHideUncategorized && cat == null) {
            var holoMgr = plugin.getService(HologramManager.class);
            if (holoMgr != null) holoMgr.removeHologram(uuid);
            return;
        }
        NamedTextColor color = itemCategories.get(cat);
        if (color == null)
            color = defaultColor;

        if (lastHoloState != null) lastHoloState.remove(uuid);
        var cfgMgr = plugin.getConfigManager();
        var stateRepo = plugin.getStateRepository();
        if (cfgMgr != null && stateRepo != null) {
            updateHologram(item, color,
                    cfgMgr.isHoloEnabled(),
                    itemCategoriesCache,
                    cfgMgr.isHoloHideUncategorized(),
                    stateRepo.getActiveLabels(),
                    stateRepo.getGroupLeaders(),
                    lastHoloState,
                    stateRepo.getBaseNameCache(),
                    stateRepo.getDisplayNameOverridesCache(),
                    stateRepo.getItemMoneyAmounts(),
                    cfgMgr.getEconomyFormat(),
                    cfgMgr.getEconomyPrefix(),
                    cfgMgr.isHoloShowAmount(),
                    stateRepo.getRawAmountFormat(),
                    cfgMgr.isProtectionEnabled(),
                    cfgMgr.getProtectionDuration(),
                    stateRepo.getItemSpawnTimes(),
                    stateRepo.getRawOwnerFormat(),
                    cfgMgr.isUsePapi(),
                    cfgMgr.isHoloShowTimer(),
                    stateRepo.getTimerComponentCache(),
                    cfgMgr.isHoloTimerNewLine());
        }
    }

    private String buildProgressBar(long remaining, long totalDuration) {
        if (totalDuration <= 0) return "[████████]";
        int totalBars = 8;
        int filled = (int) Math.max(0, Math.min(totalBars, (double) remaining / totalDuration * totalBars));
        return "[" + "█".repeat(filled) + "░".repeat(totalBars - filled) + "]";
    }
}
