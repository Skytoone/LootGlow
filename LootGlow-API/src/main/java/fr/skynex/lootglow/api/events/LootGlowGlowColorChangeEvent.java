package fr.skynex.lootglow.api.events;

import org.bukkit.Color;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when an item's glowing color is modified or overridden.
 * Cancellable to prevent color changes.
 */
public class LootGlowGlowColorChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Item item;
    private Color newColor;
    private final Player targetPlayer;

    public LootGlowGlowColorChangeEvent(@NotNull Item item, @Nullable Color newColor, @Nullable Player targetPlayer) {
        this.item = item;
        this.newColor = newColor;
        this.targetPlayer = targetPlayer;
    }

    @NotNull
    public Item getItem() {
        return item;
    }

    @Nullable
    public Color getNewColor() {
        return newColor;
    }

    public void setNewColor(@Nullable Color newColor) {
        this.newColor = newColor;
    }

    @Nullable
    public Player getTargetPlayer() {
        return targetPlayer;
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
