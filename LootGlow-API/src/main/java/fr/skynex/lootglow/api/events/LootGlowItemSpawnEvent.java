package fr.skynex.lootglow.api.events;

import org.bukkit.Color;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when an item drop receives LootGlow visual effects (glowing, hologram, particles).
 * Cancellable to prevent LootGlow effects on specific items.
 */
public class LootGlowItemSpawnEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Item item;
    private Color glowColor;
    private String hologramText;
    private boolean beaconBeam;

    public LootGlowItemSpawnEvent(@NotNull Item item, @Nullable Color glowColor, @Nullable String hologramText, boolean beaconBeam) {
        this.item = item;
        this.glowColor = glowColor;
        this.hologramText = hologramText;
        this.beaconBeam = beaconBeam;
    }

    @NotNull
    public Item getItem() {
        return item;
    }

    @Nullable
    public Color getGlowColor() {
        return glowColor;
    }

    public void setGlowColor(@Nullable Color glowColor) {
        this.glowColor = glowColor;
    }

    @Nullable
    public String getHologramText() {
        return hologramText;
    }

    public void setHologramText(@Nullable String hologramText) {
        this.hologramText = hologramText;
    }

    public boolean isBeaconBeam() {
        return beaconBeam;
    }

    public void setBeaconBeam(boolean beaconBeam) {
        this.beaconBeam = beaconBeam;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
