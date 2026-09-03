package fr.skynex.lootglow.api;

import fr.skynex.lootglow.api.util.LootGlowItemBuilder;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Official LootGlow API interface for controlling item glows, holograms, loot protection, particles, and VIP magnet effects.
 */
public interface LootGlowAPI {

    /**
     * Creates a new LootGlowItemBuilder for fluent spawning and styling of item drops.
     *
     * @param location Target world location
     * @param itemStack Item stack to drop
     * @return LootGlowItemBuilder instance
     */
    static LootGlowItemBuilder builder(@NotNull Location location, @NotNull ItemStack itemStack) {
        return new LootGlowItemBuilder(location, itemStack);
    }

    /**
     * Overrides the glowing color of a specific dropped item entity.
     *
     * @param item Dropped item entity
     * @param color Custom RGB Color
     */
    void setGlowColor(@NotNull Item item, @NotNull Color color);

    /**
     * Overrides the glowing color of a specific dropped item entity for a specific player.
     *
     * @param item Dropped item entity
     * @param color Custom RGB Color
     * @param player Target player who sees the custom glow color
     */
    void setGlowColor(@NotNull Item item, @NotNull Color color, @NotNull Player player);

    /**
     * Resets the glowing color of an item to its default configured category color.
     *
     * @param item Dropped item entity
     */
    void resetGlowColor(@NotNull Item item);

    /**
     * Resets the glowing color of an item for a specific player.
     *
     * @param item Dropped item entity
     * @param player Target player
     */
    void resetGlowColor(@NotNull Item item, @NotNull Player player);

    /**
     * Sets a custom holographic label above a dropped item entity.
     *
     * @param item Dropped item entity
     * @param text Custom text label (supports MiniMessage & legacy color codes)
     */
    void setCustomHologram(@NotNull Item item, @Nullable String text);

    /**
     * Sets a custom holographic label above a dropped item entity for a specific player.
     *
     * @param item Dropped item entity
     * @param text Custom text label
     * @param player Target player
     */
    void setCustomHologram(@NotNull Item item, @Nullable String text, @NotNull Player player);

    /**
     * Removes the custom holographic label above a dropped item entity, restoring default label / stack counter.
     *
     * @param item Dropped item entity
     */
    void removeCustomHologram(@NotNull Item item);

    /**
     * Removes the custom holographic label above a dropped item entity for a specific player.
     *
     * @param item Dropped item entity
     * @param player Target player
     */
    void removeCustomHologram(@NotNull Item item, @NotNull Player player);

    /**
     * Toggles vertical beacon light beam effect for a dropped item entity.
     *
     * @param item Dropped item entity
     * @param enabled True to enable beam, false to disable
     */
    void setBeaconBeam(@NotNull Item item, boolean enabled);

    /**
     * Toggles vertical beacon light beam effect with a custom color for a dropped item entity.
     *
     * @param item Dropped item entity
     * @param enabled True to enable beam, false to disable
     * @param color Custom RGB Color for the beacon beam
     */
    void setBeaconBeam(@NotNull Item item, boolean enabled, @Nullable Color color);

    /**
     * Grants owner-only loot protection for an item drop.
     * Pass -1 as durationSeconds for permanent/infinite protection until despawn or pickup.
     *
     * @param item Dropped item entity
     * @param ownerUuid UUID of the player allowed to pick up the item
     * @param durationSeconds Duration of protection in seconds (-1 for permanent)
     */
    void setLootProtection(@NotNull Item item, @NotNull UUID ownerUuid, long durationSeconds);

    /**
     * Grants permanent owner-only loot protection for an item drop until it despawns or is picked up.
     *
     * @param item Dropped item entity
     * @param ownerUuid UUID of the player allowed to pick up the item
     */
    default void setLootProtection(@NotNull Item item, @NotNull UUID ownerUuid) {
        setLootProtection(item, ownerUuid, -1L);
    }

    /**
     * Grants permanent owner-only loot protection for an item drop until it despawns or is picked up.
     *
     * @param item Dropped item entity
     * @param ownerUuid UUID of the player allowed to pick up the item
     */
    default void setPermanentLootProtection(@NotNull Item item, @NotNull UUID ownerUuid) {
        setLootProtection(item, ownerUuid, -1L);
    }

    /**
     * Removes owner loot protection and clears all whitelisted sharers for a dropped item entity.
     *
     * @param item Dropped item entity
     */
    void resetLootProtection(@NotNull Item item);

    /**
     * Checks if a dropped item currently has active loot protection.
     *
     * @param item Dropped item entity
     * @return True if protected
     */
    boolean isLootProtected(@NotNull Item item);

    /**
     * Checks if a player is allowed to pick up a protected dropped item.
     *
     * @param player Target player attempting to pick up item
     * @param item Target dropped item
     * @return True if player is allowed to pick up
     */
    boolean isPlayerAllowedToPickup(@NotNull Player player, @NotNull Item item);

    /**
     * Retrieves the UUID of the owner who has loot protection over an item.
     *
     * @param item Target dropped item
     * @return UUID of owner or null if unprotected
     */
    @Nullable
    UUID getLootOwner(@NotNull Item item);

    /**
     * Checks if VIP magnet is enabled for a player.
     *
     * @param player Target player
     * @return True if magnet is enabled
     */
    boolean isMagnetEnabled(@NotNull Player player);

    /**
     * Sets the VIP magnet state for a player.
     *
     * @param player Target player
     * @param enabled True to enable magnet
     */
    void setMagnetEnabled(@NotNull Player player, boolean enabled);

    /**
     * Manually triggers item attraction towards a player within a specified radius.
     *
     * @param player Target player
     * @param radius Radius in blocks to pull items
     */
    void pullItemsToPlayer(@NotNull Player player, double radius);

    /**
     * Checks if visual effects (glow, holograms, beams) are hidden for a player.
     *
     * @param player Target player
     * @return True if visuals are hidden
     */
    boolean isVisualsHidden(@NotNull Player player);

    /**
     * Toggles visual effects visibility for a specific player.
     *
     * @param player Target player
     * @param hidden True to hide visuals
     */
    void setVisualsHidden(@NotNull Player player, boolean hidden);

    /**
     * Checks if a dropped item entity is visible to a player within max distance and line-of-sight raycast.
     *
     * @param player Target player
     * @param item Target dropped item
     * @param maxDistance Maximum viewing distance in blocks
     * @return True if player has direct line of sight to item within maxDistance
     */
    boolean hasLineOfSight(@NotNull Player player, @NotNull Item item, double maxDistance);

    /**
     * Updates visibility of item glow/visuals for a player based on line-of-sight raycast occlusion check.
     *
     * @param player Target player
     * @param item Target dropped item
     * @param maxDistance Max distance in blocks for visibility
     * @return True if item is visible to the player after occlusion check
     */
    boolean updateOcclusionVisibility(@NotNull Player player, @NotNull Item item, double maxDistance);

    /**
     * Assigns a custom particle effect to surround a dropped item entity.
     *
     * @param item Target dropped item
     * @param particle Particle type (e.g. FLAME, SOUL_FIRE_FLAME, TOTEM)
     */
    void setParticleEffect(@NotNull Item item, @Nullable Particle particle);

    /**
     * Clears any custom particle effect assigned to a dropped item.
     *
     * @param item Target dropped item
     */
    void clearParticleEffect(@NotNull Item item);

    /**
     * Plays a custom sound effect at the item's location.
     *
     * @param item Target dropped item
     * @param sound Sound to play
     * @param volume Volume level
     * @param pitch Pitch level
     */
    void setDropSound(@NotNull Item item, @Nullable Sound sound, float volume, float pitch);

    /**
     * Triggers a pop jump animation with particle burst for an item entity.
     *
     * @param item Target dropped item
     * @param jumpVelocity Upward jump velocity factor
     */
    void triggerPopAnimation(@NotNull Item item, double jumpVelocity);

    /**
     * Toggles ground bouncing physics for an item entity.
     *
     * @param item Target dropped item
     * @param bouncing True to enable ground bounce
     */
    void setBouncingEnabled(@NotNull Item item, boolean bouncing);

    /**
     * Highlights or removes the farming marker ('!') on a crop block.
     *
     * @param cropBlock Target crop block
     * @param highlight True to add highlight, false to remove
     */
    void setCropHighlight(@NotNull Block cropBlock, boolean highlight);

    /**
     * Checks if a crop block currently has a LootGlow farming highlight marker.
     *
     * @param cropBlock Target crop block
     * @return True if highlighted
     */
    boolean isCropHighlighted(@NotNull Block cropBlock);

    /**
     * Overrides the LootGlow category assigned to a dropped item dynamically.
     *
     * @param item Target dropped item
     * @param category Category name (e.g. "legendary", "epic", "rare")
     */
    void setItemCategory(@NotNull Item item, @NotNull String category);

    /**
     * Gets the configured LootGlow category name assigned to a dropped item.
     *
     * @param item Target dropped item
     * @return Category name string or null if uncategorized
     */
    @Nullable
    String getItemCategory(@NotNull Item item);

    /**
     * Retrieves all active glowing dropped items near a specified location.
     *
     * @param location Center location
     * @param radius Search radius in blocks
     * @return List of active glowing Item entities
     */
    @NotNull
    List<Item> getNearbyGlowingItems(@NotNull Location location, double radius);

    /**
     * Spawns a dropped item entity at the location with optional LootGlow category assignment.
     *
     * @param location Target spawn location
     * @param itemStack Item stack to spawn
     * @param category Optional category name or null for automatic classification
     * @return Spawned Item entity
     */
    @NotNull
    Item spawnGlowItem(@NotNull Location location, @NotNull ItemStack itemStack, @Nullable String category);

    /**
     * Forces a visual refresh of all LootGlow elements (glow, holograms, beams) of an item for a specific player.
     *
     * @param item Dropped item entity
     * @param player Target player
     */
    void refreshVisuals(@NotNull Item item, @NotNull Player player);

    /**
     * Checks if a dropped item entity is actively tracked by LootGlow.
     *
     * @param item Dropped item entity
     * @return True if tracked
     */
    boolean isTracked(@NotNull Item item);

    /**
     * Retrieves all active tracked glowing dropped items in a specific world chunk.
     *
     * @param chunk Target chunk
     * @return List of active tracked Item entities
     */
    @NotNull
    List<Item> getTrackedItemsInChunk(@NotNull Chunk chunk);

    /**
     * Adds an additional allowed player UUID to an item's loot protection whitelist (shared loot).
     *
     * @param item Dropped item entity
     * @param playerUuid UUID of player allowed to pick up the item
     */
    void addLootSharer(@NotNull Item item, @NotNull UUID playerUuid);

    /**
     * Removes a player UUID from an item's loot protection whitelist.
     *
     * @param item Dropped item entity
     * @param playerUuid UUID of player to remove
     */
    void removeLootSharer(@NotNull Item item, @NotNull UUID playerUuid);

    /**
     * Gets all UUIDs of players allowed to pick up a protected item (including the primary owner and shared players).
     *
     * @param item Dropped item entity
     * @return Set of allowed player UUIDs
     */
    @NotNull
    Set<UUID> getLootSharers(@NotNull Item item);

    /**
     * Detects and returns the configured LootGlow rarity name for an item stack
     * (e.g. "MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON").
     *
     * @param itemStack Target item stack to analyze
     * @return Rarity name string
     */
    @NotNull
    String detectItemRarity(@NotNull ItemStack itemStack);

    /**
     * Detects and returns the configured LootGlow rarity name for a dropped item entity.
     *
     * @param item Target dropped item entity
     * @return Rarity name string
     */
    @NotNull
    String detectItemRarity(@NotNull Item item);

    /**
     * Checks if two dropped item entities can be merged together based on item similarity and loot protection compatibility.
     * If loot protection is active on either item, returns false unless both items share the exact same owner and sharers.
     *
     * @param item1 First dropped item entity
     * @param item2 Second dropped item entity
     * @return True if items can be merged
     */
    boolean canMerge(@NotNull Item item1, @NotNull Item item2);

    /**
     * Merges item2 stack amount into item1, triggering ItemMergeEvent.
     *
     * @param item1 Target dropped item receiving amount
     * @param item2 Source dropped item merging into item1
     * @return True if merged successfully
     */
    boolean mergeAmount(@NotNull Item item1, @NotNull Item item2);

    /**
     * Splits amount from a dropped item entity stack, spawning a new item entity with specified amount.
     *
     * @param item Target dropped item entity
     * @param amount Amount to split from the stack
     * @return True if unmerged successfully
     */
    boolean unMergeAmount(@NotNull Item item, int amount);

    /**
     * Retrieves the total stack amount of a dropped item entity.
     *
     * @param item Target dropped item entity
     * @return Item stack count
     */
    int getMergeAmount(@NotNull Item item);

    /**
     * Sets the stack amount directly on a dropped item entity and updates its visual label.
     * If amount <= 0, the item entity is removed.
     *
     * @param item Target dropped item entity
     * @param amount New stack count
     */
    void setMergeAmount(@NotNull Item item, int amount);

    /**
     * Increments the stack amount on a dropped item entity by specified amount.
     *
     * @param item Target dropped item entity
     * @param amount Amount to add
     */
    void addMergeAmount(@NotNull Item item, int amount);

    /**
     * Decrements the stack amount on a dropped item entity by specified amount.
     * If remaining amount <= 0, the item entity is removed.
     *
     * @param item Target dropped item entity
     * @param amount Amount to remove
     */
    void removeMergeAmount(@NotNull Item item, int amount);
}

