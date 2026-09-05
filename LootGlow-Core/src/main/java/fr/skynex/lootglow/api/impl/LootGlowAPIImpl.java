package fr.skynex.lootglow.api.impl;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.api.LootGlowAPI;
import fr.skynex.lootglow.api.events.LootGlowBeamToggleEvent;
import fr.skynex.lootglow.api.events.LootGlowCategoryAssignEvent;
import fr.skynex.lootglow.api.events.LootGlowGlowColorChangeEvent;
import fr.skynex.lootglow.api.events.LootGlowPlayerToggleVisualsEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of LootGlowAPI interface.
 * Delegates calls to relevant plugin managers and services.
 */
public class LootGlowAPIImpl implements LootGlowAPI {

    private final LootGlow plugin;

    public LootGlowAPIImpl(LootGlow plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setGlowColor(@NotNull Item item, @NotNull Color color) {
        if (item == null || !item.isValid() || color == null)
            return;
        LootGlowGlowColorChangeEvent event = new LootGlowGlowColorChangeEvent(item, color, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        Color finalColor = event.getNewColor() != null ? event.getNewColor() : color;
        var glowMgr = plugin.getService(fr.skynex.lootglow.managers.GlowManager.class);
        if (glowMgr != null) {
            glowMgr.setGlowColor(item, finalColor);
        }
        item.setGlowing(true);
    }

    @Override
    public void setGlowColor(@NotNull Item item, @NotNull Color color, @NotNull Player player) {
        if (item == null || !item.isValid() || color == null || player == null || !player.isOnline())
            return;
        LootGlowGlowColorChangeEvent event = new LootGlowGlowColorChangeEvent(item, color, player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        Color finalColor = event.getNewColor() != null ? event.getNewColor() : color;
        var glowMgr = plugin.getService(fr.skynex.lootglow.managers.GlowManager.class);
        if (glowMgr != null) {
            glowMgr.setGlowColor(item, finalColor, player);
        }
        item.setGlowing(true);
    }

    @Override
    public void resetGlowColor(@NotNull Item item) {
        if (item == null || !item.isValid())
            return;
        var glowMgr = plugin.getService(fr.skynex.lootglow.managers.GlowManager.class);
        if (glowMgr != null) {
            glowMgr.resetGlowColor(item);
        }
        item.setGlowing(true);
    }

    @Override
    public void resetGlowColor(@NotNull Item item, @NotNull Player player) {
        if (item == null || !item.isValid() || player == null || !player.isOnline())
            return;
        item.setGlowing(true);
    }

    @Override
    public void setCustomHologram(@NotNull Item item, @Nullable String text) {
        if (item == null || !item.isValid())
            return;
        var holoRenderer = plugin.getService(fr.skynex.lootglow.managers.HologramRenderer.class);
        if (holoRenderer != null) {
            if (text == null || text.isEmpty()) {
                holoRenderer.removeCustomHologram(item);
            } else {
                holoRenderer.setCustomHologram(item, text, (net.kyori.adventure.text.minimessage.MiniMessage) null);
            }
        }
        var stateRepo = plugin.getStateRepository();
        if (stateRepo != null) {
            stateRepo.getBaseNameCache().remove(item.getUniqueId());
            stateRepo.getLastHoloState().remove(item.getUniqueId());
        }
    }

    @Override
    public void setCustomHologram(@NotNull Item item, @Nullable String text, @NotNull Player player) {
        if (item == null || !item.isValid() || player == null || !player.isOnline())
            return;
        var holoRenderer = plugin.getService(fr.skynex.lootglow.managers.HologramRenderer.class);
        if (holoRenderer != null) {
            if (text == null || text.isEmpty()) {
                holoRenderer.removeCustomHologram(item, player);
            } else {
                holoRenderer.setCustomHologram(item, text, player);
            }
        }
        var stateRepo = plugin.getStateRepository();
        if (stateRepo != null) {
            stateRepo.getBaseNameCache().remove(item.getUniqueId());
            stateRepo.getLastHoloState().remove(item.getUniqueId());
        }
    }

    @Override
    public void removeCustomHologram(@NotNull Item item) {
        if (item == null || !item.isValid()) return;
        var holoRenderer = plugin.getService(fr.skynex.lootglow.managers.HologramRenderer.class);
        if (holoRenderer != null) {
            holoRenderer.removeCustomHologram(item);
        }
        var stateRepo = plugin.getStateRepository();
        if (stateRepo != null) {
            stateRepo.getBaseNameCache().remove(item.getUniqueId());
            stateRepo.getLastHoloState().remove(item.getUniqueId());
        }
    }

    @Override
    public void removeCustomHologram(@NotNull Item item, @NotNull Player player) {
        if (item == null || !item.isValid() || player == null) return;
        var holoRenderer = plugin.getService(fr.skynex.lootglow.managers.HologramRenderer.class);
        if (holoRenderer != null) {
            holoRenderer.removeCustomHologram(item, player);
        }
        var stateRepo = plugin.getStateRepository();
        if (stateRepo != null) {
            stateRepo.getBaseNameCache().remove(item.getUniqueId());
            stateRepo.getLastHoloState().remove(item.getUniqueId());
        }
    }

    @Override
    public void setBeaconBeam(@NotNull Item item, boolean enabled) {
        setBeaconBeam(item, enabled, null);
    }

    @Override
    public void setBeaconBeam(@NotNull Item item, boolean enabled, @Nullable Color color) {
        if (item == null || !item.isValid())
            return;
        LootGlowBeamToggleEvent event = new LootGlowBeamToggleEvent(item, enabled, color);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        boolean finalEnabled = event.isEnabled();
        Color finalColor = event.getBeamColor();
        var stateRepo = plugin.getStateRepository();
        if (!finalEnabled) {
            BlockDisplay beam = stateRepo != null ? stateRepo.getActiveBeams().remove(item.getUniqueId()) : null;
            if (beam != null && beam.isValid())
                beam.remove();
        } else {
            NamedTextColor textColor = finalColor != null
                    ? NamedTextColor.nearestTo(net.kyori.adventure.text.format.TextColor.color(finalColor.asRGB()))
                    : NamedTextColor.WHITE;
            var beamMgr = plugin.getService(fr.skynex.lootglow.managers.BeamManager.class);
            if (beamMgr != null) beamMgr.spawnBeam(item, null, textColor);
        }
    }

    @Override
    public void setLootProtection(@NotNull Item item, @NotNull UUID ownerUuid, long durationSeconds) {
        var protMgr = plugin.getService(fr.skynex.lootglow.managers.LootProtectionManager.class);
        if (protMgr != null) {
            protMgr.setLootProtection(item, ownerUuid, durationSeconds);
        }
    }

    @Override
    public void resetLootProtection(@NotNull Item item) {
        var protMgr = plugin.getService(fr.skynex.lootglow.managers.LootProtectionManager.class);
        if (protMgr != null) {
            protMgr.resetLootProtection(item);
        }
    }

    @Override
    public boolean isLootProtected(@NotNull Item item) {
        var protMgr = plugin.getService(fr.skynex.lootglow.managers.LootProtectionManager.class);
        return protMgr != null && protMgr.isLootProtected(item);
    }

    @Override
    public boolean isPlayerAllowedToPickup(@NotNull Player player, @NotNull Item item) {
        var protMgr = plugin.getService(fr.skynex.lootglow.managers.LootProtectionManager.class);
        return protMgr == null || protMgr.isPlayerAllowedToPickup(player, item);
    }

    @Override
    public UUID getLootOwner(@NotNull Item item) {
        var protMgr = plugin.getService(fr.skynex.lootglow.managers.LootProtectionManager.class);
        return protMgr != null ? protMgr.getLootOwner(item) : null;
    }

    @Override
    public boolean isMagnetEnabled(@NotNull Player player) {
        var magMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMagnetManager.class);
        return magMgr != null && magMgr.isMagnetEnabled(player);
    }

    @Override
    public void setMagnetEnabled(@NotNull Player player, boolean enabled) {
        var magMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMagnetManager.class);
        if (magMgr != null) {
            magMgr.setMagnetEnabled(player, enabled);
        }
    }

    @Override
    public void pullItemsToPlayer(@NotNull Player player, double radius) {
        var magMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMagnetManager.class);
        if (magMgr != null) {
            magMgr.pullItemsToPlayer(player, radius);
        }
    }

    @Override
    public boolean isVisualsHidden(@NotNull Player player) {
        var stateRepo = plugin.getStateRepository();
        return player != null && stateRepo != null && stateRepo.getHiddenVisuals().contains(player.getUniqueId());
    }

    @Override
    public void setVisualsHidden(@NotNull Player player, boolean hidden) {
        if (player == null)
            return;
        var stateRepo = plugin.getStateRepository();
        if (stateRepo == null) return;
        boolean previous = stateRepo.getHiddenVisuals().contains(player.getUniqueId());
        if (hidden) {
            stateRepo.getHiddenVisuals().add(player.getUniqueId());
        } else {
            stateRepo.getHiddenVisuals().remove(player.getUniqueId());
        }
        if (previous != hidden) {
            Bukkit.getPluginManager().callEvent(new LootGlowPlayerToggleVisualsEvent(player, hidden));
        }
    }

    @Override
    public boolean hasLineOfSight(@NotNull Player player, @NotNull Item item, double maxDistance) {
        var occlMgr = plugin.getService(fr.skynex.lootglow.managers.OcclusionManager.class);
        return occlMgr != null && occlMgr.hasLineOfSight(player, item, maxDistance);
    }

    @Override
    public boolean updateOcclusionVisibility(@NotNull Player player, @NotNull Item item, double maxDistance) {
        boolean visible = hasLineOfSight(player, item, maxDistance);
        setVisualsHidden(player, !visible);
        return visible;
    }

    @Override
    public void setParticleEffect(@NotNull Item item, @Nullable Particle particle) {
        if (item == null || !item.isValid())
            return;
        var stateRepo = plugin.getStateRepository();
        if (stateRepo != null) stateRepo.getItemParticlesCache().put(item.getUniqueId(), particle);
    }

    @Override
    public void clearParticleEffect(@NotNull Item item) {
        if (item == null || !item.isValid())
            return;
        var stateRepo = plugin.getStateRepository();
        if (stateRepo != null) stateRepo.getItemParticlesCache().remove(item.getUniqueId());
    }

    @Override
    public void setDropSound(@NotNull Item item, @Nullable Sound sound, float volume, float pitch) {
        if (item == null || !item.isValid() || sound == null)
            return;
        item.getWorld().playSound(item.getLocation(), sound, volume, pitch);
    }

    @Override
    public void triggerPopAnimation(@NotNull Item item, double jumpVelocity) {
        var particleAnimMgr = plugin.getService(fr.skynex.lootglow.managers.ParticleAnimationManager.class);
        if (particleAnimMgr != null) {
            particleAnimMgr.triggerPopAnimation(item, jumpVelocity);
        }
    }

    @Override
    public void setBouncingEnabled(@NotNull Item item, boolean bouncing) {
        var particleAnimMgr = plugin.getService(fr.skynex.lootglow.managers.ParticleAnimationManager.class);
        if (particleAnimMgr != null) {
            particleAnimMgr.setBouncingEnabled(item, bouncing, plugin.getStateRepository() != null ? plugin.getStateRepository().getRecentlyBounced() : null);
        }
    }

    @Override
    public void setCropHighlight(@NotNull Block cropBlock, boolean highlight) {
        var farmMgr = plugin.getService(fr.skynex.lootglow.managers.FarmingManager.class);
        if (farmMgr != null) {
            farmMgr.setCropHighlight(cropBlock, highlight);
        }
    }

    @Override
    public boolean isCropHighlighted(@NotNull Block cropBlock) {
        var farmMgr = plugin.getService(fr.skynex.lootglow.managers.FarmingManager.class);
        return farmMgr != null && farmMgr.isCropHighlighted(cropBlock);
    }

    @Override
    public void setItemCategory(@NotNull Item item, @NotNull String category) {
        if (item == null || !item.isValid() || category == null)
            return;
        LootGlowCategoryAssignEvent event = new LootGlowCategoryAssignEvent(item, category);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        String finalCategory = event.getCategory() != null ? event.getCategory() : category;
        var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
        if (trackedMgr != null) {
            trackedMgr.setItemCategory(item.getUniqueId(), finalCategory);
        }
    }

    @Nullable
    @Override
    public String getItemCategory(@NotNull Item item) {
        if (item == null)
            return null;
        var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
        return trackedMgr != null ? trackedMgr.getItemCategory(item.getUniqueId()) : null;
    }

    @NotNull
    @Override
    public List<Item> getNearbyGlowingItems(@NotNull Location location, double radius) {
        var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
        return trackedMgr != null ? trackedMgr.getNearbyGlowingItems(location, radius) : List.of();
    }

    @NotNull
    @Override
    public Item spawnGlowItem(@NotNull Location location, @NotNull ItemStack itemStack, @Nullable String category) {
        if (location == null || location.getWorld() == null || itemStack == null) {
            throw new IllegalArgumentException("Location, World, and ItemStack cannot be null");
        }
        Item item = location.getWorld().dropItem(location, itemStack);
        if (category != null && !category.isEmpty()) {
            setItemCategory(item, category);
        }
        return item;
    }

    @Override
    public void refreshVisuals(@NotNull Item item, @NotNull Player player) {
        if (item == null || !item.isValid() || player == null || !player.isOnline())
            return;
        var entityVisSvc = plugin.getService(fr.skynex.lootglow.service.EntityVisibilityService.class);
        var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
        var vanillaVisMgr = plugin.getService(fr.skynex.lootglow.managers.VanillaItemVisibilityManager.class);
        var stateRepo = plugin.getStateRepository();
        var lodMgr = plugin.getService(fr.skynex.lootglow.managers.LODManager.class);
        var farmMgr = plugin.getService(fr.skynex.lootglow.managers.FarmingManager.class);
        var cfgMgr = plugin.getConfigManager();
        if (entityVisSvc != null && trackedMgr != null) {
            entityVisSvc.refreshGlowForPlayer(
                    player,
                    !isVisualsHidden(player),
                    vanillaVisMgr != null ? vanillaVisMgr.getHiddenVanillaItems() : Set.of(),
                    trackedMgr.getEntityIdMap(),
                    stateRepo != null ? stateRepo.getVisibleEntities() : java.util.Collections.emptyMap(),
                    cfgMgr != null ? cfgMgr.getFarmingViewDistance() : 24.0,
                    trackedMgr.getActiveItems(),
                    stateRepo != null ? stateRepo.getGroupedItems() : Set.of(),
                    lodMgr != null ? lodMgr.getLodHoloDistanceSquared() : 1024.0,
                    lodMgr != null ? lodMgr.getLodBeamDistanceSquared() : 1024.0,
                    farmMgr != null ? farmMgr.getActiveCropSymbols() : java.util.Collections.emptyMap());
        }
    }

    @Override
    public boolean isTracked(@NotNull Item item) {
        var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
        return item != null && trackedMgr != null && trackedMgr.getActiveItems().containsKey(item.getUniqueId());
    }

    @NotNull
    @Override
    public List<Item> getTrackedItemsInChunk(@NotNull Chunk chunk) {
        var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
        if (chunk == null || trackedMgr == null)
            return List.of();
        List<Item> result = new ArrayList<>();
        for (Item item : trackedMgr.getActiveItems().values()) {
            if (item != null && item.isValid() && item.getLocation().getChunk().equals(chunk)) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public void addLootSharer(@NotNull Item item, @NotNull UUID playerUuid) {
        var protMgr = plugin.getService(fr.skynex.lootglow.managers.LootProtectionManager.class);
        if (protMgr != null) {
            protMgr.addLootSharer(item, playerUuid);
        }
    }

    @Override
    public void removeLootSharer(@NotNull Item item, @NotNull UUID playerUuid) {
        var protMgr = plugin.getService(fr.skynex.lootglow.managers.LootProtectionManager.class);
        if (protMgr != null) {
            protMgr.removeLootSharer(item, playerUuid);
        }
    }

    @NotNull
    @Override
    public Set<UUID> getLootSharers(@NotNull Item item) {
        var protMgr = plugin.getService(fr.skynex.lootglow.managers.LootProtectionManager.class);
        return protMgr != null ? protMgr.getLootSharers(item) : Set.of();
    }

    @NotNull
    @Override
    public String detectItemRarity(@NotNull ItemStack itemStack) {
        var rarityMgr = plugin.getService(fr.skynex.lootglow.managers.RarityManager.class);
        if (itemStack == null || rarityMgr == null)
            return "COMMON";
        return rarityMgr.detectRarity(itemStack).name();
    }

    @NotNull
    @Override
    public String detectItemRarity(@NotNull Item item) {
        if (item == null || !item.isValid())
            return "COMMON";
        return detectItemRarity(item.getItemStack());
    }

    @Override
    public boolean canMerge(@NotNull Item item1, @NotNull Item item2) {
        var mergeMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMergeManager.class);
        return mergeMgr != null && mergeMgr.canMerge(item1, item2);
    }

    @Override
    public boolean mergeAmount(@NotNull Item item1, @NotNull Item item2) {
        var mergeMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMergeManager.class);
        return mergeMgr != null && mergeMgr.mergeAmount(item1, item2);
    }

    @Override
    public boolean unMergeAmount(@NotNull Item item, int amount) {
        var mergeMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMergeManager.class);
        return mergeMgr != null && mergeMgr.unMergeAmount(item, amount);
    }

    @Override
    public int getMergeAmount(@NotNull Item item) {
        var mergeMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMergeManager.class);
        return mergeMgr != null ? mergeMgr.getMergeAmount(item) : 0;
    }

    @Override
    public void setMergeAmount(@NotNull Item item, int amount) {
        var mergeMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMergeManager.class);
        if (mergeMgr != null) {
            mergeMgr.setMergeAmount(item, amount);
        }
    }

    @Override
    public void addMergeAmount(@NotNull Item item, int amount) {
        var mergeMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMergeManager.class);
        if (mergeMgr != null) {
            mergeMgr.addMergeAmount(item, amount);
        }
    }

    @Override
    public void removeMergeAmount(@NotNull Item item, int amount) {
        var mergeMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMergeManager.class);
        if (mergeMgr != null) {
            mergeMgr.removeMergeAmount(item, amount);
        }
    }

    @Override
    public boolean isGrouped(@NotNull Item item) {
        if (item == null || plugin.getStateRepository() == null) return false;
        return plugin.getStateRepository().getGroupedItems().contains(item.getUniqueId());
    }

    @Nullable
    @Override
    public Item getLootBagLeader(@NotNull Item item) {
        if (item == null || plugin.getStateRepository() == null) return null;
        UUID uuid = item.getUniqueId();
        var stateRepo = plugin.getStateRepository();
        if (stateRepo.getGroupLeaders().containsKey(uuid)) return item;
        for (var entry : stateRepo.getGroupMembers().entrySet()) {
            if (entry.getValue() != null && entry.getValue().contains(uuid)) {
                return stateRepo.getActiveItems().get(entry.getKey());
            }
        }
        return null;
    }

    @NotNull
    @Override
    public List<Item> getGroupedMembers(@NotNull Item bagItem) {
        if (bagItem == null || plugin.getStateRepository() == null) return List.of();
        List<UUID> memberUuids = plugin.getStateRepository().getGroupMembers().get(bagItem.getUniqueId());
        if (memberUuids == null || memberUuids.isEmpty()) return List.of();
        List<Item> members = new ArrayList<>();
        var activeItems = plugin.getStateRepository().getActiveItems();
        for (UUID mUuid : memberUuids) {
            Item mItem = activeItems.get(mUuid);
            if (mItem != null && mItem.isValid()) {
                members.add(mItem);
            }
        }
        return members;
    }

    @Override
    public void setParticleAnimationType(@NotNull Item item, @Nullable String animationType) {
        if (item == null || !item.isValid() || plugin.getStateRepository() == null) return;
        if (animationType != null) {
            plugin.getConfigManager().getCategoryAnimTypes().put("custom_" + item.getUniqueId(), animationType);
        }
    }

    @Override
    public void setCustomLightLevel(@NotNull Item item, int lightLevel) {
        if (item == null || !item.isValid() || plugin.getStateRepository() == null) return;
        plugin.getStateRepository().getActiveLights().put(item.getUniqueId(), item.getLocation());
    }

    @Override
    public void pullItemsToPlayer(@NotNull Player player, double radius, @Nullable java.util.function.Predicate<Item> filter) {
        if (player == null || !player.isOnline() || player.getWorld() == null) return;
        var magMgr = plugin.getService(fr.skynex.lootglow.managers.ItemMagnetManager.class);
        if (magMgr != null) {
            if (filter == null) {
                magMgr.pullItemsToPlayer(player, radius);
            } else {
                double radiusSq = radius * radius;
                Location pLoc = player.getLocation();
                for (Item item : getNearbyGlowingItems(pLoc, radius)) {
                    if (item.isValid() && filter.test(item) && item.getLocation().distanceSquared(pLoc) <= radiusSq) {
                        item.teleport(pLoc);
                    }
                }
            }
        }
    }
}
