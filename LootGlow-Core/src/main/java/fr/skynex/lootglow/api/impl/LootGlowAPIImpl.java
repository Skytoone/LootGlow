package fr.skynex.lootglow.api.impl;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.api.LootGlowAPI;
import fr.skynex.lootglow.api.events.LootGlowBeamToggleEvent;
import fr.skynex.lootglow.api.events.LootGlowCategoryAssignEvent;
import fr.skynex.lootglow.api.events.LootGlowGlowColorChangeEvent;
import fr.skynex.lootglow.api.events.LootGlowPlayerToggleVisualsEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import org.bukkit.entity.TextDisplay;
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
        if (item == null || !item.isValid() || color == null) return;
        LootGlowGlowColorChangeEvent event = new LootGlowGlowColorChangeEvent(item, color, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        Color finalColor = event.getNewColor() != null ? event.getNewColor() : color;
        if (plugin.getGlowManager() != null) {
            plugin.getGlowManager().setGlowColor(item, finalColor);
        }
        item.setGlowing(true);
    }

    @Override
    public void setGlowColor(@NotNull Item item, @NotNull Color color, @NotNull Player player) {
        if (item == null || !item.isValid() || color == null || player == null || !player.isOnline()) return;
        LootGlowGlowColorChangeEvent event = new LootGlowGlowColorChangeEvent(item, color, player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        Color finalColor = event.getNewColor() != null ? event.getNewColor() : color;
        if (plugin.getGlowManager() != null) {
            plugin.getGlowManager().setGlowColor(item, finalColor, player);
        }
        item.setGlowing(true);
    }

    @Override
    public void resetGlowColor(@NotNull Item item) {
        if (item == null || !item.isValid()) return;
        if (plugin.getGlowManager() != null) {
            plugin.getGlowManager().resetGlowColor(item);
        }
        item.setGlowing(true);
    }

    @Override
    public void resetGlowColor(@NotNull Item item, @NotNull Player player) {
        if (item == null || !item.isValid() || player == null || !player.isOnline()) return;
        item.setGlowing(true);
    }

    @Override
    public void setCustomHologram(@NotNull Item item, @Nullable String text) {
        if (item == null || !item.isValid()) return;
        TextDisplay display = plugin.getActiveLabels().get(item.getUniqueId());
        if (display != null && display.isValid()) {
            if (text == null || text.isEmpty()) {
                display.text(Component.empty());
            } else {
                display.text(MiniMessage.miniMessage().deserialize(text));
            }
        }
    }

    @Override
    public void setCustomHologram(@NotNull Item item, @Nullable String text, @NotNull Player player) {
        if (item == null || !item.isValid() || player == null || !player.isOnline()) return;
        setCustomHologram(item, text);
    }

    @Override
    public void setBeaconBeam(@NotNull Item item, boolean enabled) {
        setBeaconBeam(item, enabled, null);
    }

    @Override
    public void setBeaconBeam(@NotNull Item item, boolean enabled, @Nullable Color color) {
        if (item == null || !item.isValid()) return;
        LootGlowBeamToggleEvent event = new LootGlowBeamToggleEvent(item, enabled, color);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        boolean finalEnabled = event.isEnabled();
        Color finalColor = event.getBeamColor();
        if (!finalEnabled) {
            BlockDisplay beam = plugin.getActiveBeams().remove(item.getUniqueId());
            if (beam != null && beam.isValid()) beam.remove();
        } else {
            NamedTextColor textColor = finalColor != null ? NamedTextColor.nearestTo(net.kyori.adventure.text.format.TextColor.color(finalColor.asRGB())) : NamedTextColor.WHITE;
            plugin.spawnBeam(item, null, textColor);
        }
    }

    @Override
    public void setLootProtection(@NotNull Item item, @NotNull UUID ownerUuid, long durationSeconds) {
        if (plugin.getLootProtectionManager() != null) {
            plugin.getLootProtectionManager().setLootProtection(item, ownerUuid, durationSeconds);
        }
    }

    @Override
    public boolean isLootProtected(@NotNull Item item) {
        return plugin.getLootProtectionManager() != null ? plugin.getLootProtectionManager().isLootProtected(item) : false;
    }

    @Override
    public boolean isPlayerAllowedToPickup(@NotNull Player player, @NotNull Item item) {
        return plugin.getLootProtectionManager() != null ? plugin.getLootProtectionManager().isPlayerAllowedToPickup(player, item) : true;
    }

    @Override
    public UUID getLootOwner(@NotNull Item item) {
        return plugin.getLootProtectionManager() != null ? plugin.getLootProtectionManager().getLootOwner(item) : null;
    }

    @Override
    public boolean isMagnetEnabled(@NotNull Player player) {
        return plugin.getItemMagnetManager() != null && plugin.getItemMagnetManager().isMagnetEnabled(player);
    }

    @Override
    public void setMagnetEnabled(@NotNull Player player, boolean enabled) {
        if (plugin.getItemMagnetManager() != null) {
            plugin.getItemMagnetManager().setMagnetEnabled(player, enabled);
        }
    }

    @Override
    public void pullItemsToPlayer(@NotNull Player player, double radius) {
        if (plugin.getItemMagnetManager() != null) {
            plugin.getItemMagnetManager().pullItemsToPlayer(player, radius);
        }
    }

    @Override
    public boolean isVisualsHidden(@NotNull Player player) {
        return player != null && plugin.getHiddenVisuals().contains(player.getUniqueId());
    }

    @Override
    public void setVisualsHidden(@NotNull Player player, boolean hidden) {
        if (player == null) return;
        boolean previous = plugin.getHiddenVisuals().contains(player.getUniqueId());
        if (hidden) {
            plugin.getHiddenVisuals().add(player.getUniqueId());
        } else {
            plugin.getHiddenVisuals().remove(player.getUniqueId());
        }
        if (previous != hidden) {
            Bukkit.getPluginManager().callEvent(new LootGlowPlayerToggleVisualsEvent(player, hidden));
        }
    }

    @Override
    public boolean hasLineOfSight(@NotNull Player player, @NotNull Item item, double maxDistance) {
        return plugin.getOcclusionManager() != null && plugin.getOcclusionManager().hasLineOfSight(player, item, maxDistance);
    }

    @Override
    public boolean updateOcclusionVisibility(@NotNull Player player, @NotNull Item item, double maxDistance) {
        boolean visible = hasLineOfSight(player, item, maxDistance);
        setVisualsHidden(player, !visible);
        return visible;
    }

    @Override
    public void setParticleEffect(@NotNull Item item, @Nullable Particle particle) {
        if (item == null || !item.isValid()) return;
        plugin.getItemParticlesCache().put(item.getUniqueId(), particle);
    }

    @Override
    public void clearParticleEffect(@NotNull Item item) {
        if (item == null || !item.isValid()) return;
        plugin.getItemParticlesCache().remove(item.getUniqueId());
    }

    @Override
    public void setDropSound(@NotNull Item item, @Nullable Sound sound, float volume, float pitch) {
        if (item == null || !item.isValid() || sound == null) return;
        item.getWorld().playSound(item.getLocation(), sound, volume, pitch);
    }

    @Override
    public void triggerPopAnimation(@NotNull Item item, double jumpVelocity) {
        if (plugin.getParticleAnimationManager() != null) {
            plugin.getParticleAnimationManager().triggerPopAnimation(item, jumpVelocity);
        }
    }

    @Override
    public void setBouncingEnabled(@NotNull Item item, boolean bouncing) {
        if (plugin.getParticleAnimationManager() != null) {
            plugin.getParticleAnimationManager().setBouncingEnabled(item, bouncing, plugin.getRecentlyBounced());
        }
    }

    @Override
    public void setCropHighlight(@NotNull Block cropBlock, boolean highlight) {
        if (plugin.getFarmingManager() != null) {
            plugin.getFarmingManager().setCropHighlight(cropBlock, highlight);
        }
    }

    @Override
    public boolean isCropHighlighted(@NotNull Block cropBlock) {
        return plugin.getFarmingManager() != null && plugin.getFarmingManager().isCropHighlighted(cropBlock);
    }

    @Override
    public void setItemCategory(@NotNull Item item, @NotNull String category) {
        if (item == null || !item.isValid() || category == null) return;
        LootGlowCategoryAssignEvent event = new LootGlowCategoryAssignEvent(item, category);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        String finalCategory = event.getCategory() != null ? event.getCategory() : category;
        if (plugin.getTrackedItemManager() != null) {
            plugin.getTrackedItemManager().setItemCategory(item.getUniqueId(), finalCategory);
        }
    }

    @Nullable
    @Override
    public String getItemCategory(@NotNull Item item) {
        if (item == null) return null;
        return plugin.getTrackedItemManager() != null ? plugin.getTrackedItemManager().getItemCategory(item.getUniqueId()) : null;
    }

    @NotNull
    @Override
    public List<Item> getNearbyGlowingItems(@NotNull Location location, double radius) {
        return plugin.getTrackedItemManager() != null ? plugin.getTrackedItemManager().getNearbyGlowingItems(location, radius) : List.of();
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
        if (item == null || !item.isValid() || player == null || !player.isOnline()) return;
        if (plugin.getEntityVisibilityService() != null && plugin.getTrackedItemManager() != null) {
            plugin.getEntityVisibilityService().refreshGlowForPlayer(
                    player,
                    !isVisualsHidden(player),
                    plugin.getVanillaItemVisibilityManager() != null ? plugin.getVanillaItemVisibilityManager().getHiddenVanillaItems() : Set.of(),
                    plugin.getTrackedItemManager().getEntityIdMap(),
                    plugin.getVisibleEntities(),
                    plugin.getFarmingViewDistance(),
                    plugin.getTrackedItemManager().getActiveItems(),
                    plugin.getGroupedItems(),
                    plugin.getLodHoloDistSq(),
                    plugin.getLodBeamDistSq(),
                    plugin.getActiveCropSymbols()
            );
        }
    }

    @Override
    public boolean isTracked(@NotNull Item item) {
        return item != null && plugin.getTrackedItemManager() != null && plugin.getTrackedItemManager().getActiveItems().containsKey(item.getUniqueId());
    }

    @NotNull
    @Override
    public List<Item> getTrackedItemsInChunk(@NotNull Chunk chunk) {
        if (chunk == null || plugin.getTrackedItemManager() == null) return List.of();
        List<Item> result = new ArrayList<>();
        for (Item item : plugin.getTrackedItemManager().getActiveItems().values()) {
            if (item != null && item.isValid() && item.getLocation().getChunk().equals(chunk)) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public void addLootSharer(@NotNull Item item, @NotNull UUID playerUuid) {
        if (plugin.getLootProtectionManager() != null) {
            plugin.getLootProtectionManager().addLootSharer(item, playerUuid);
        }
    }

    @Override
    public void removeLootSharer(@NotNull Item item, @NotNull UUID playerUuid) {
        if (plugin.getLootProtectionManager() != null) {
            plugin.getLootProtectionManager().removeLootSharer(item, playerUuid);
        }
    }

    @NotNull
    @Override
    public Set<UUID> getLootSharers(@NotNull Item item) {
        return plugin.getLootProtectionManager() != null ? plugin.getLootProtectionManager().getLootSharers(item) : Set.of();
    }

    @NotNull
    @Override
    public String detectItemRarity(@NotNull ItemStack itemStack) {
        if (itemStack == null || plugin.getRarityManager() == null) return "COMMON";
        return plugin.getRarityManager().detectRarity(itemStack).name();
    }

    @NotNull
    @Override
    public String detectItemRarity(@NotNull Item item) {
        if (item == null || !item.isValid()) return "COMMON";
        return detectItemRarity(item.getItemStack());
    }
}
