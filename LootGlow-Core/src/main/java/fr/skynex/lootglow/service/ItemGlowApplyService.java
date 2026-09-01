package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import fr.skynex.lootglow.util.MoneyAmountParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles full item glow resolution, MMOItems/MythicDrops smart rarity detection,
 * scoreboard team assignments, and visual spawn triggers.
 */
public class ItemGlowApplyService {

    private final LootGlow plugin;

    public ItemGlowApplyService(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void applyGlow(Item item, boolean playAnimation,
                          boolean isEnabled,
                          boolean economyEnabled,
                          List<NamespacedKey> economyKeys,
                          NamedTextColor economyColor,
                          Sound economySound,
                          Map<UUID, Double> itemMoneyAmounts,
                          Map<String, NamedTextColor> itemCategories,
                          Map<String, String> categoryNames,
                          NamedTextColor defaultColor,
                          Map<String, Particle> categoryParticles,
                          Map<UUID, Particle> itemParticlesCache,
                          Map<UUID, String> itemCategoriesCache,
                          int despawnTime,
                          Map<Integer, UUID> entityIdMap,
                          Map<UUID, Item> activeItems,
                          Map<String, Set<UUID>> itemsByWorld,
                          boolean rpgDropsEnabled,
                          List<String> rpgEnabledCategories,
                          Map<String, Boolean> categoryGlow,
                          boolean defaultGlow,
                          Set<Integer> hiddenVanillaItems,
                          Map<String, Sound> categorySounds,
                          boolean holoEnabled,
                          boolean holoHideUncategorized,
                          Map<UUID, Long> itemSpawnTimes,
                          Map<UUID, Component> baseNameCache,
                          boolean protectionEnabled,
                          int protectionDuration,
                          boolean shadowsEnabled,
                          boolean beamsEnabled,
                          List<String> beamCategories) {

        if (!isEnabled || item == null) return;
        if (!plugin.isWorldAllowed(item.getWorld().getName())) return;
        if (plugin.isInBlockedRegion(item.getLocation())) return;

        // Apply configurable pickup delay for mob/MythicMobs drops so items land properly before being picked up
        if (item.getThrower() == null) {
            int mobPickupDelay = plugin.getConfig().getInt("settings.mob-drop-pickup-delay", 20);
            if (mobPickupDelay > 0 && item.getPickupDelay() < mobPickupDelay) {
                item.setPickupDelay(mobPickupDelay);
            }
        }

        ItemStack stack = item.getItemStack();
        String customId = plugin.getInternalId(stack);
        String matName = stack.getType().name();

        String category = null;
        NamedTextColor color = defaultColor;

        Double moneyAmount = MoneyAmountParser.getMoneyAmount(stack, economyEnabled, economyKeys);
        if (moneyAmount != null) {
            category = "ECONOMY";
            color = economyColor;
            itemMoneyAmounts.put(item.getUniqueId(), moneyAmount);

            if (playAnimation && economySound != null) {
                item.getWorld().playSound(item.getLocation(), economySound, 1.0f, 1.2f);
            }
        }

        if (category == null) {
            if (customId != null && itemCategories.containsKey(customId)) {
                category = categoryNames.get(customId);
                color = itemCategories.get(customId);
            } else if (itemCategories.containsKey(matName)) {
                category = categoryNames.get(matName);
                color = itemCategories.get(matName);
            } else if (customId != null && item.getItemStack().hasItemMeta()) {
                PersistentDataContainer pdc = item.getItemStack().getItemMeta().getPersistentDataContainer();

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
                    if (plugin.getConfig().contains("categories." + tier)) {
                        category = tier;
                        String colorStr = plugin.getConfig().getString("categories." + tier + ".color", "WHITE");
                        color = plugin.parseNamedColor(colorStr);
                    }
                }

                NamespacedKey mdKey = new NamespacedKey("mythicdrops", "tier");
                if (pdc.has(mdKey, PersistentDataType.STRING)) {
                    String mdTier = pdc.get(mdKey, PersistentDataType.STRING).toLowerCase();
                    if (plugin.getConfig().contains("categories." + mdTier)) {
                        category = mdTier;
                        String colorStr = plugin.getConfig().getString("categories." + mdTier + ".color", "WHITE");
                        color = plugin.parseNamedColor(colorStr);
                    }
                }

                if (category == null && item.getItemStack().hasItemMeta()) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemStack().getItemMeta();

                    // 1) Lore Pattern matching
                    if (meta.hasLore() && !plugin.getConfigManager().getCategoryLorePatterns().isEmpty()) {
                        List<Component> loreLines = meta.lore();
                        if (loreLines != null) {
                            for (Map.Entry<String, List<String>> entry : plugin.getConfigManager().getCategoryLorePatterns().entrySet()) {
                                String catName = entry.getKey();
                                for (String pattern : entry.getValue()) {
                                    for (Component lineComp : loreLines) {
                                        if (lineComp != null) {
                                            String line = LegacyComponentSerializer.legacySection().serialize(lineComp);
                                            if (line.toLowerCase().contains(pattern)) {
                                                category = catName;
                                                NamedTextColor catColor = plugin.getConfigManager().getCategoryColors().get(catName);
                                                if (catColor != null) color = catColor;
                                                break;
                                            }
                                        }
                                    }
                                    if (category != null) break;
                                }
                                if (category != null) break;
                            }
                        }
                    }

                    // 2) NBT / PDC Pattern matching
                    if (category == null && !plugin.getConfigManager().getCategoryNbtPatterns().isEmpty()) {
                        PersistentDataContainer metaPdc = meta.getPersistentDataContainer();
                        for (Map.Entry<String, List<String>> entry : plugin.getConfigManager().getCategoryNbtPatterns().entrySet()) {
                            String catName = entry.getKey();
                            for (String pattern : entry.getValue()) {
                                for (NamespacedKey pdcKey : metaPdc.getKeys()) {
                                    if (pdcKey.toString().toLowerCase().contains(pattern) || pdcKey.getKey().toLowerCase().contains(pattern)) {
                                        category = catName;
                                        NamedTextColor catColor = plugin.getConfigManager().getCategoryColors().get(catName);
                                        if (catColor != null) color = catColor;
                                        break;
                                    }
                                }
                                if (category != null) break;
                            }
                            if (category != null) break;
                        }
                    }

                    if (category == null) {
                        Component displayName = meta.displayName();
                        if (displayName != null) {
                            TextColor nameColor = displayName.color();

                            if (nameColor == null) {
                                String legacyName = LegacyComponentSerializer.legacySection().serialize(displayName);
                                if (legacyName.contains("§")) {
                                    int index = legacyName.indexOf("§");
                                    if (index != -1 && index + 1 < legacyName.length()) {
                                        char code = legacyName.charAt(index + 1);
                                        switch (code) {
                                            case '0' -> nameColor = NamedTextColor.BLACK;
                                            case '1' -> nameColor = NamedTextColor.DARK_BLUE;
                                            case '2' -> nameColor = NamedTextColor.DARK_GREEN;
                                            case '3' -> nameColor = NamedTextColor.DARK_AQUA;
                                            case '4' -> nameColor = NamedTextColor.DARK_RED;
                                            case '5' -> nameColor = NamedTextColor.DARK_PURPLE;
                                            case '6' -> nameColor = NamedTextColor.GOLD;
                                            case '7' -> nameColor = NamedTextColor.GRAY;
                                            case '8' -> nameColor = NamedTextColor.DARK_GRAY;
                                            case '9' -> nameColor = NamedTextColor.BLUE;
                                            case 'a' -> nameColor = NamedTextColor.GREEN;
                                            case 'b' -> nameColor = NamedTextColor.AQUA;
                                            case 'c' -> nameColor = NamedTextColor.RED;
                                            case 'd' -> nameColor = NamedTextColor.LIGHT_PURPLE;
                                            case 'e' -> nameColor = NamedTextColor.YELLOW;
                                            case 'f' -> nameColor = NamedTextColor.WHITE;
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
        }

        if (category != null) {
            itemCategoriesCache.put(item.getUniqueId(), category);

            Particle part = categoryParticles.get(customId != null ? customId : matName);
            if (part == null) {
                String partStr = plugin.getConfig().getString("categories." + category + ".particle");
                if (partStr != null) {
                    try {
                        NamespacedKey particleKey = NamespacedKey.minecraft(partStr.toLowerCase());
                        part = Registry.PARTICLE_TYPE.get(particleKey);
                    } catch (Exception ignored) {}
                }
            }

            if (part != null) {
                itemParticlesCache.put(item.getUniqueId(), part);
            }
        }

        final NamedTextColor finalColor = color;
        final String finalCategory = category;

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
            try {
                item.setVisibleByDefault(false);
            } catch (NoSuchMethodError ignored) {}
            hiddenVanillaItems.add(item.getEntityId());
        }

        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = "LG_" + finalColor.toString().toUpperCase();
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.addEntry(item.getUniqueId().toString());
            }
        } catch (Throwable ignored) {}

        Sound sound = categorySounds.get(customId);
        if (sound == null) sound = categorySounds.get(matName);
        if (sound == null && finalCategory != null) {
            String soundStr = plugin.getConfig().getString("categories." + finalCategory + ".sound");
            if (soundStr != null) {
                sound = plugin.parseSound(soundStr);
            }
        }

        if (playAnimation) {
            if (sound != null) {
                item.getWorld().playSound(item.getLocation(), sound, 1.0f, 1.0f);
            }

            // Title & Subtitle RPG drop notification broadcast
            if (finalCategory != null) {
                String titleStr = plugin.getConfigManager().getCategoryTitles().get(finalCategory);
                String subTitleStr = plugin.getConfigManager().getCategorySubtitles().get(finalCategory);
                if (titleStr != null || subTitleStr != null) {
                    net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
                    Component mainTitle = titleStr != null ? mm.deserialize(titleStr) : Component.empty();
                    Component subTitle = subTitleStr != null ? mm.deserialize(subTitleStr) : Component.empty();
                    net.kyori.adventure.title.Title titleObj = net.kyori.adventure.title.Title.title(mainTitle, subTitle);
                    double radius = plugin.getConfigManager().getCategoryNotificationRadius().getOrDefault(finalCategory, 15.0);

                    item.getWorld().getNearbyPlayers(item.getLocation(), radius).forEach(p -> p.showTitle(titleObj));
                }
            }

            if (finalColor.equals(NamedTextColor.GOLD)) {
                item.getWorld().getNearbyPlayers(item.getLocation(), 15)
                        .forEach(p -> plugin.sendMessage(p, "legendary-found"));
            }
        }

        if (holoEnabled) {
            if (holoHideUncategorized && finalCategory == null) {
                // Skip hologram for uncategorized
            } else {
                if (!itemSpawnTimes.containsKey(item.getUniqueId())) {
                    itemSpawnTimes.put(item.getUniqueId(), System.currentTimeMillis());
                }
                baseNameCache.put(item.getUniqueId(), plugin.getHologramService() != null ? plugin.getHologramService().calculateBaseName(item, finalColor, plugin.getDisplayNameOverridesCache(), itemMoneyAmounts, plugin.getEconomyFormat(), plugin.getEconomyPrefix()) : Component.empty());
                plugin.updateHologram(item, finalColor);

                if (protectionEnabled) {
                    FoliaScheduler.runLater(plugin, () -> {
                        if (item.isValid()) plugin.updateHologram(item, finalColor);
                    }, protectionDuration * 20L);
                }
            }
        }

        if (isRpgDrop) {
            plugin.broadcastRpgDropVisibility(item);

            FoliaScheduler.runLater(plugin, () -> {
                if (!item.isValid() || !activeItems.containsKey(item.getUniqueId())) return;
                plugin.spawnItemVisual(item, finalCategory, finalColor);
                if (shadowsEnabled) {
                    plugin.spawnShadow(item);
                }
                if (beamsEnabled && finalCategory != null && beamCategories.contains(finalCategory.toLowerCase())) {
                    plugin.spawnBeam(item, finalCategory, finalColor);
                }
                plugin.broadcastRpgDropVisibility(item);
            }, 1L);
        }
    }

    public void preHideItem(Item item, boolean isEnabled, boolean rpgDropsEnabled,
                            NamespacedKey sourceMobKey,
                            Map<String, NamedTextColor> itemCategories,
                            Map<String, String> categoryNames,
                            List<String> rpgEnabledCategories,
                            Map<Integer, UUID> entityIdMap,
                            Set<Integer> hiddenVanillaItems) {
        if (!isEnabled || !rpgDropsEnabled) return;
        if (!plugin.isWorldAllowed(item.getWorld().getName())) return;

        String customId = plugin.getInternalId(item.getItemStack());
        if (customId == null) {
            PersistentDataContainer pdc = item.getPersistentDataContainer();
            if (pdc.has(sourceMobKey, PersistentDataType.STRING)) {
                customId = "MYTHIC:" + pdc.get(sourceMobKey, PersistentDataType.STRING);
            }
        }
        if (customId == null && item.getItemStack().hasItemMeta()) {
            PersistentDataContainer pdc = item.getItemStack().getItemMeta().getPersistentDataContainer();
            if (pdc.has(sourceMobKey, PersistentDataType.STRING)) {
                customId = "MYTHIC:" + pdc.get(sourceMobKey, PersistentDataType.STRING);
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
        if (!isRpg) return;

        entityIdMap.put(item.getEntityId(), item.getUniqueId());
        hiddenVanillaItems.add(item.getEntityId());
        try {
            item.setVisibleByDefault(false);
        } catch (NoSuchMethodError ignored) {}
    }
}
