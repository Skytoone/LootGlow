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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles spawning, updating, stale-cleanup and initial visibility
 * of ItemDisplay visual replacements for dropped items.
 */
public class ItemVisualSpawnService {

    private final LootGlow plugin;

    public ItemVisualSpawnService(LootGlow plugin) {
        this.plugin = plugin;
    }

    /**
     * Validate existing display for an item. If valid, updates its ItemStack and returns it.
     * If stale, removes it from maps and returns null so a new one can be spawned.
     */
    public ItemDisplay validateExisting(Map<UUID, ItemDisplay> activeItemVisuals,
                                        Map<Integer, UUID> entityIdMap,
                                        Item item) {
        ItemDisplay existing = activeItemVisuals.get(item.getUniqueId());
        if (existing != null) {
            if (existing.isValid()) {
                existing.setItemStack(item.getItemStack().clone());
                return existing;
            }
            // Stale → cleanup
            activeItemVisuals.remove(item.getUniqueId());
            entityIdMap.remove(existing.getEntityId());
        }
        return null;
    }

    /**
     * Returns true if this item should be filtered out from RPG display by category whitelist.
     */
    public boolean isFilteredByCategory(Set<String> rpgEnabledCategories, String category, boolean isGroupVisual) {
        return !isGroupVisual
                && !rpgEnabledCategories.isEmpty()
                && (category == null || !rpgEnabledCategories.contains(category.toLowerCase()));
    }

    /**
     * Spawn a visual ItemDisplay for a dropped item, handling both group bag and normal RPG display.
     * Registers in activeItemVisuals, entityIdMap, scoreboard team, and broadcasts initial visibility.
     */
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
                                // Config values passed directly
                                Material bagMaterial,
                                String bagHeadTexture,
                                boolean useOwnerHead,
                                int bagCustomModelData,
                                float rpgItemScale,
                                float rpgBlockScale,
                                float rpgRotation) {

        boolean isGroupVisual = useVisualBag && groupLeaders.containsKey(item.getUniqueId());
        if (!rpgDropsEnabled && !isGroupVisual) return;

        // Validate/cleanup existing display
        if (validateExisting(activeItemVisuals, entityIdMap, item) != null) return;

        // Category filter
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
                org.bukkit.util.Transformation bagTransform = new org.bukkit.util.Transformation(
                        new org.joml.Vector3f(0f, 0.05f, 0f),
                        new org.joml.Quaternionf(),
                        new org.joml.Vector3f(1.0f, 1.0f, 1.0f),
                        new org.joml.Quaternionf());
                ent.setTransformation(bagTransform);
            } else {
                ent.setItemStack(visualStack);

                Material mat = visualStack.getType();
                boolean isCustom = plugin.isCustomItem(visualStack);
                boolean isUpright = plugin.isUprightItem(mat);
                ItemDisplay.ItemDisplayTransform transform = isCustom
                        ? ItemDisplay.ItemDisplayTransform.FIXED
                        : ItemDisplay.ItemDisplayTransform.NONE;

                float baseScale = isUpright ? rpgBlockScale : rpgItemScale;
                if (plugin.isFishItem(mat)) {
                    baseScale *= 0.55f;
                }

                float rotX = (isCustom || isUpright) ? 0f : rpgRotation;
                float transY = isCustom ? 0.1f : 0.08f;
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

        boolean shouldGlow = categoryGlow.getOrDefault(category, defaultGlow);
        if (shouldGlow) {
            display.setGlowing(true);
        }
        entityIdMap.put(display.getEntityId(), display.getUniqueId());
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam("LG_" + color.toString().toUpperCase());
            if (team != null) team.addEntry(display.getUniqueId().toString());
        } catch (Throwable ignored) {}
        activeItemVisuals.put(item.getUniqueId(), display);

        // Initial visibility broadcast
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hiddenVisuals.contains(p.getUniqueId()) && p.getWorld().equals(item.getWorld())) {
                p.showEntity(plugin, display);
                visibleEntities.computeIfAbsent(p.getUniqueId(), k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(display.getUniqueId());
            }
        }
    }
}
