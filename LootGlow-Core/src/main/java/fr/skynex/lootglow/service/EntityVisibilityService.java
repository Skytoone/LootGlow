package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import fr.skynex.lootglow.managers.TrackedItemManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles per-player show/hide of vanilla item entities and their display replacements.
 */
public class EntityVisibilityService {

    private final LootGlow plugin;

    public EntityVisibilityService(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void broadcastRpgDropVisibility(Item item,
                                           Map<UUID, ItemDisplay> activeItemVisuals,
                                           Set<UUID> hiddenVisuals,
                                           Set<UUID> groupedItems) {
        if (item == null || item.isDead()) return;
        ItemDisplay display = activeItemVisuals.get(item.getUniqueId());

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(item.getWorld())) continue;

            boolean wantsVanilla = hiddenVisuals.contains(p.getUniqueId());
            boolean isGrouped = groupedItems.contains(item.getUniqueId());

            applyVisibility(p, item, display, wantsVanilla, isGrouped);
        }
    }

    public void applyVisibility(Player p, Item item, ItemDisplay display, boolean wantsVanilla, boolean isGrouped) {
        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("[LootGlow Debug] applyVisibility for player=" + p.getName() + ", item=" + item.getType() + ", displayNull=" + (display == null) + ", wantsVanilla=" + wantsVanilla + ", isGrouped=" + isGrouped);
        }
        if (wantsVanilla) {
            p.showEntity(plugin, item);
            if (display != null && display.isValid()) {
                p.hideEntity(plugin, display);
            }
        } else {
            p.hideEntity(plugin, item);
            if (isGrouped) {
                if (display != null && display.isValid()) {
                    p.hideEntity(plugin, display);
                }
            } else if (display != null && display.isValid()) {
                p.showEntity(plugin, display);
            }
        }
    }

    public void refreshGlowForPlayer(Player player, boolean showVisuals,
                                     Set<Integer> hiddenVanillaItems,
                                     Map<Integer, UUID> entityIdMap,
                                     Map<UUID, Set<UUID>> visibleEntities,
                                     double farmingViewDistance,
                                     Map<UUID, Item> activeItems,
                                     Set<UUID> groupedItems,
                                     double lodHoloDistSq,
                                     double lodBeamDistSq,
                                     Map<org.bukkit.block.Block, ? extends List<BlockDisplay>> activeCropSymbols) {
        World world = player.getWorld();

        for (int entityId : hiddenVanillaItems) {
            UUID uuid = entityIdMap.get(entityId);
            if (uuid == null) continue;
            Item item = activeItems.get(uuid);
            if (item != null && item.isValid() && item.getWorld().equals(world)) {
                if (showVisuals) {
                    player.hideEntity(plugin, item);
                } else {
                    player.showEntity(plugin, item);
                }
            }
        }

        Set<UUID> visibleSet = visibleEntities.computeIfAbsent(player.getUniqueId(), k -> java.util.concurrent.ConcurrentHashMap.newKeySet());
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double farmDistSq = farmingViewDistance * farmingViewDistance;

        for (Map.Entry<UUID, Item> entry : activeItems.entrySet()) {
            UUID uuid = entry.getKey();
            Item item = entry.getValue();
            if (item == null || !item.isValid() || !item.getWorld().equals(world)) continue;

            double dx = px - item.getX();
            double dy = py - item.getY();
            double dz = pz - item.getZ();
            double dSq = dx * dx + dy * dy + dz * dz;
            boolean isGrouped = groupedItems.contains(uuid);
            if (isGrouped && showVisuals) {
                player.hideEntity(plugin, item);
            }

            TextDisplay label = plugin.getStateRepository().getActiveLabels().get(uuid);
            BlockDisplay beam = plugin.getStateRepository().getActiveBeams().get(uuid);
            ItemDisplay visual = plugin.getStateRepository().getActiveItemVisuals().get(uuid);
            Entity shadow = plugin.getStateRepository().getActiveShadows().get(uuid);

            if (showVisuals) {
                if (label != null && label.isValid()) {
                    updateEntityVisibility(player, label, !isGrouped && dSq <= lodHoloDistSq, visibleSet);
                }
                if (beam != null && beam.isValid()) {
                    updateEntityVisibility(player, beam, !isGrouped && dSq <= lodBeamDistSq, visibleSet);
                }
                if (visual != null && visual.isValid()) {
                    updateEntityVisibility(player, visual, !isGrouped && dSq <= lodHoloDistSq, visibleSet);
                }
                if (shadow != null && shadow.isValid()) {
                    updateEntityVisibility(player, shadow, !isGrouped && dSq <= lodHoloDistSq, visibleSet);
                }
            } else {
                if (label != null && label.isValid()) updateEntityVisibility(player, label, false, visibleSet);
                if (beam != null && beam.isValid()) updateEntityVisibility(player, beam, false, visibleSet);
                if (visual != null && visual.isValid()) updateEntityVisibility(player, visual, false, visibleSet);
                if (shadow != null && shadow.isValid()) updateEntityVisibility(player, shadow, false, visibleSet);
            }
        }

        activeCropSymbols.values().forEach(symbol -> {
            BlockDisplay first = symbol.isEmpty() ? null : symbol.get(0);
            Location loc = first != null ? first.getLocation() : null;
            if (loc != null && loc.getWorld().equals(world)) {
                double cdx = px - loc.getX();
                double cdy = py - loc.getY();
                double cdz = pz - loc.getZ();
                boolean shouldSee = showVisuals && (cdx * cdx + cdy * cdy + cdz * cdz) <= farmDistSq;
                symbol.forEach(bd -> {
                    if (bd.isValid()) {
                        updateEntityVisibility(player, bd, shouldSee, visibleSet);
                    }
                });
            }
        });
    }

    public void updateEntityVisibility(Player p, Entity entity, boolean shouldSee, Set<UUID> visibleSet) {
        UUID entUuid = entity.getUniqueId();
        boolean currentlyVisible = visibleSet.contains(entUuid);

        var trackedMgr = plugin.getService(TrackedItemManager.class);

        if (shouldSee && !currentlyVisible) {
            if (!p.canSee(entity)) {
                p.showEntity(plugin, entity);
            }
            entity.getPassengers().forEach(pass -> {
                if (!p.canSee(pass)) p.showEntity(plugin, pass);
            });
            visibleSet.add(entUuid);
            if (trackedMgr != null) {
                trackedMgr.registerDisplayViewer(entUuid, p.getUniqueId());
            }
        } else if (!shouldSee && currentlyVisible) {
            if (p.canSee(entity)) {
                p.hideEntity(plugin, entity);
            }
            entity.getPassengers().forEach(pass -> {
                if (p.canSee(pass)) p.hideEntity(plugin, pass);
            });
            visibleSet.remove(entUuid);
            if (trackedMgr != null) {
                trackedMgr.unregisterDisplayViewer(entUuid, p.getUniqueId());
            }
        }
    }
}
