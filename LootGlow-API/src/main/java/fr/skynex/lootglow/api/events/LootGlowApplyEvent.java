package fr.skynex.lootglow.api.events;

import org.bukkit.Color;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when LootGlow visual effects (glow, beam, hologram) are applied to a dropped item entity.
 * Cancellable to prevent visual effects application.
 */
public class LootGlowApplyEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Item item;
    private String category;
    private Color glowColor;

    public LootGlowApplyEvent(@NotNull Item item, @Nullable String category, @Nullable Color glowColor) {
        this.item = item;
        this.category = category;
        this.glowColor = glowColor;
    }

    @NotNull
    public Item getItem() {
        return item;
    }

    @Nullable
    public String getCategory() {
        return category;
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    @Nullable
    public Color getGlowColor() {
        return glowColor;
    }

    public void setGlowColor(@Nullable Color glowColor) {
        this.glowColor = glowColor;
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
