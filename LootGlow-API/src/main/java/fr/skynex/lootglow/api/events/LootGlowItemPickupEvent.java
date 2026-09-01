package fr.skynex.lootglow.api.events;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player picks up a LootGlow-tracked item.
 * Cancellable to prevent the player from picking up the item.
 */
public class LootGlowItemPickupEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final Item item;
    private final ItemStack itemStack;
    private final String category;

    public LootGlowItemPickupEvent(@NotNull Player player, @NotNull Item item, @NotNull ItemStack itemStack, @Nullable String category) {
        this.player = player;
        this.item = item;
        this.itemStack = itemStack;
        this.category = category;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Item getItem() {
        return item;
    }

    @NotNull
    public ItemStack getItemStack() {
        return itemStack;
    }

    @Nullable
    public String getCategory() {
        return category;
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
