package fr.skynex.lootglow.api;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Official LootGlow API interface for controlling item glows, holograms, loot protection, particles, and VIP magnet effects.
 */
public interface LootGlowAPI {

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
     * Grants temporary owner-only loot protection for an item drop.
     *
     * @param item Dropped item entity
     * @param ownerUuid UUID of the player allowed to pick up the item
     * @param durationSeconds Duration of protection in seconds
     */
    void setLootProtection(@NotNull Item item, @NotNull UUID ownerUuid, long durationSeconds);

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
}
