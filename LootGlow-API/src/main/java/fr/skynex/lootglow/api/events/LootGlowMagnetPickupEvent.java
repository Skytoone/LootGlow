package fr.skynex.lootglow.api.events;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player's VIP LootGlow Magnet pulls a dropped item towards them.
 */
public class LootGlowMagnetPickupEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final Item item;

    public LootGlowMagnetPickupEvent(@NotNull Player player, @NotNull Item item) {
        this.player = player;
        this.item = item;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Item getItem() {
        return item;
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
