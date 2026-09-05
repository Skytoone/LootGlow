package fr.skynex.lootglow.api.events;

import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fired when multiple nearby items are grouped together into a Loot Bag container.
 * Cancellable to prevent item grouping into Loot Bag.
 */
public class LootBagGroupEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Item leaderItem;
    private final List<Item> memberItems;

    public LootBagGroupEvent(@NotNull Item leaderItem, @NotNull List<Item> memberItems) {
        this.leaderItem = leaderItem;
        this.memberItems = memberItems;
    }

    @NotNull
    public Item getLeaderItem() {
        return leaderItem;
    }

    @NotNull
    public List<Item> getMemberItems() {
        return memberItems;
    }

    public int getGroupSize() {
        return memberItems.size();
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
