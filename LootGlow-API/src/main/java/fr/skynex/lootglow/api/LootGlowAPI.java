package fr.skynex.lootglow.api;

import org.bukkit.Color;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Official LootGlow API interface for controlling item glows, holograms, loot protection, and VIP magnet effects.
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
     * Resets the glowing color of an item to its default configured category color.
     *
     * @param item Dropped item entity
     */
    void resetGlowColor(@NotNull Item item);

    /**
     * Sets a custom holographic label above a dropped item entity.
     *
     * @param item Dropped item entity
     * @param text Custom text label (supports MiniMessage & legacy color codes)
     */
    void setCustomHologram(@NotNull Item item, @Nullable String text);

    /**
     * Toggles vertical beacon light beam effect for a dropped item entity.
     *
     * @param item Dropped item entity
     * @param enabled True to enable beam, false to disable
     */
    void setBeaconBeam(@NotNull Item item, boolean enabled);

    /**
     * Grants temporary owner-only loot protection for an item drop.
     *
     * @param item Dropped item entity
     * @param ownerUuid UUID of the player allowed to pick up the item
     * @param durationSeconds Duration of protection in seconds
     */
    void setLootProtection(@NotNull Item item, @NotNull UUID ownerUuid, long durationSeconds);

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
}
