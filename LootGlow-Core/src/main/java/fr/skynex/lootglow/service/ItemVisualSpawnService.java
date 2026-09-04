package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ItemVisualSpawnService {

    private final LootGlow plugin;

    public ItemVisualSpawnService(LootGlow plugin) {
        this.plugin = plugin;
    }

    public ItemDisplay validateExisting(Map<UUID, ItemDisplay> activeItemVisuals,
                                        Map<Integer, UUID> entityIdMap,
                                        Item item) {
        ItemDisplay existing = activeItemVisuals.get(item.getUniqueId());
        if (existing != null) {
            if (existing.isValid()) {
                existing.setItemStack(item.getItemStack().clone());
                return existing;
            }
            activeItemVisuals.remove(item.getUniqueId());
            entityIdMap.remove(existing.getEntityId());
        }
        return null;
    }

    public boolean isFilteredByCategory(Set<String> rpgEnabledCategories, String category, boolean isGroupVisual) {
        return !isGroupVisual
                && !rpgEnabledCategories.isEmpty()
                && (category == null || !rpgEnabledCategories.contains(category.toLowerCase()));
    }

    public void spawnItemVisual(Item item, String category, NamedTextColor color, fr.skynex.lootglow.model.ItemVisualContext ctx) {
        if (ctx == null) return;
        spawnItemVisual(item, category, color,
                ctx.useVisualBag(), ctx.rpgDropsEnabled(), ctx.groupLeaders(),
                ctx.activeItemVisuals(), ctx.entityIdMap(), ctx.rpgEnabledCategories(),
                ctx.hiddenVisuals(), ctx.visibleEntities(), ctx.categoryGlow(),
                ctx.defaultGlow(), ctx.bagMaterial(), ctx.bagHeadTexture(),
                ctx.useOwnerHead(), ctx.bagCustomModelData(), ctx.rpgItemScale(),
                ctx.rpgBlockScale(), ctx.rpgRotation());
    }

    public void spawnItemVisual(Item item, String category, NamedTextColor color,
                                boolean useVisualBag,
                                boolean rpgDropsEnabled,
                                Map<UUID, ?> groupLeaders,
                                Map<UUID, ItemDisplay> activeItemVisuals,
                                Map<Integer, UUID> entityIdMap,
                                Set<String> rpgEnabledCategories,
                                Set<UUID> hiddenVisuals,
                                Map<UUID, Set<UUID>> visibleEntities,
                                Map<String, Boolean> categoryGlow,
                                boolean defaultGlow,
                                Material bagMaterial,
                                String bagHeadTexture,
                                boolean useOwnerHead,
                                int bagCustomModelData,
                                float rpgItemScale,
                                float rpgBlockScale,
                                float rpgRotation) {

        boolean isGroupVisual = useVisualBag && groupLeaders.containsKey(item.getUniqueId());
        if (!rpgDropsEnabled && !isGroupVisual) return;

        if (validateExisting(activeItemVisuals, entityIdMap, item) != null) return;

        if (isFilteredByCategory(rpgEnabledCategories, category, isGroupVisual)) return;

        Location spawnLoc = item.getLocation().clone();
        ItemStack visualStack = item.getItemStack().clone();

        ItemDisplay display = item.getWorld().spawn(spawnLoc, ItemDisplay.class, ent -> {
            if (isGroupVisual) {
                ItemStack bag;
                if (bagMaterial == Material.PLAYER_HEAD) {
                    if (useOwnerHead && item.getThrower() != null) {
                        bag = plugin.getVisualDisplayManager().getOwnerHead(item.getThrower());
                    } else if (!bagHeadTexture.isEmpty()) {
                        bag = plugin.getVisualDisplayManager().createTexturedHead(bagHeadTexture);
                    } else {
                        bag = new ItemStack(bagMaterial);
                    }
                } else {
                    bag = new ItemStack(bagMaterial);
                }
                if (bagCustomModelData != 0) {
                    org.bukkit.inventory.meta.ItemMeta bMeta = bag.getItemMeta();
                    if (bMeta != null) {
                        bMeta.getCustomModelDataComponent().setFloats(java.util.List.of((float) bagCustomModelData));
                        bag.setItemMeta(bMeta);
                    }
                }
                ent.setItemStack(bag);
                ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                ent.setTransformation(new org.bukkit.util.Transformation(
                        new org.joml.Vector3f(0f, 0.30f, 0f),
                        new org.joml.Quaternionf(),
                        new org.joml.Vector3f(1.0f, 1.0f, 1.0f),
                        new org.joml.Quaternionf()));
            } else {
                ent.setItemStack(visualStack);
                Material mat = visualStack.getType();
                boolean isCustom = plugin.isCustomItem(visualStack);
                boolean isUpright = plugin.isUprightItem(mat);
                float baseScale = isUpright ? rpgBlockScale : rpgItemScale;
                if (plugin.isFishItem(mat)) baseScale *= 0.55f;
                float rotX = (isCustom || isUpright) ? 0f : rpgRotation;
                float transY = isCustom ? 0.18f : 0.15f;
                if (mat == Material.TRIDENT) transY += 0.35f;
                else if (mat == Material.SHIELD) transY += 0.42f;
                ent.setTransformation(new org.bukkit.util.Transformation(
                        new org.joml.Vector3f(0, transY, 0),
                        new org.joml.Quaternionf().rotationX(rotX),
                        new org.joml.Vector3f(baseScale, baseScale, baseScale),
                        new org.joml.Quaternionf()));
                ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            }
            ent.setTeleportDuration(2);
            ent.setPersistent(false);
        });

        boolean shouldGlow = categoryGlow.getOrDefault(category, defaultGlow);
        if (shouldGlow) display.setGlowing(true);

        entityIdMap.put(display.getEntityId(), display.getUniqueId());
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam("LG_" + color.toString().toUpperCase());
            if (team != null) team.addEntry(display.getUniqueId().toString());
        } catch (Throwable ignored) {}
        activeItemVisuals.put(item.getUniqueId(), display);
        if (plugin.getTrackedItemManager() != null) {
            fr.skynex.lootglow.model.TrackedItem ti = plugin.getTrackedItemManager().getOrCreateTrackedItem(item.getUniqueId());
            ti.visual = display;
            plugin.getTrackedItemManager().registerDisplayEntity(display.getUniqueId(), item.getUniqueId());
        }

        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("[LootGlow Debug] Spawned ItemDisplay for item "
                    + item.getItemStack().getType() + " (UUID: " + item.getUniqueId()
                    + ", Display UUID: " + display.getUniqueId() + ", Category: " + category + ")");
        }

        boolean debugMode = plugin.getConfig().getBoolean("settings.debug", false);
        // Synchronous visibility: display is visible by default (setVisibleByDefault=true).
        // Paper auto-sends SPAWN_ENTITY. We hide vanilla item and handle toggle players.
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(item.getWorld())) continue;
            p.hideEntity(plugin, item);
            if (hiddenVisuals.contains(p.getUniqueId())) {
                p.hideEntity(plugin, display);
                if (debugMode) plugin.getLogger().info("[LootGlow Debug] applyVisibility HIDE display for " + p.getName() + " (toggle=true)");
            } else {
                p.showEntity(plugin, display);
                visibleEntities.computeIfAbsent(p.getUniqueId(),
                        k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(display.getUniqueId());
                if (debugMode) plugin.getLogger().info("[LootGlow Debug] applyVisibility SHOW display " + display.getUniqueId() + " for " + p.getName() + " canSee=" + p.canSee(display));
            }
        }
    }
}
