package fr.skynex.lootglow.api.events;

import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when two dropped item stacks are about to be merged together.
 * Cancellable to prevent item merging.
 */
public class ItemMergeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Item entity;
    private final Item target;

    public ItemMergeEvent(@NotNull Item entity, @NotNull Item target) {
        this.entity = entity;
        this.target = target;
    }

    /**
     * Gets the source item entity being merged into the target.
     *
     * @return Source dropped item entity
     */
    @NotNull
    public Item getEntity() {
        return entity;
    }

    /**
     * Gets the target item entity receiving the merged item stack.
     *
     * @return Target dropped item entity
     */
    @NotNull
    public Item getTarget() {
        return target;
    }

    /**
     * Gets the amount of items in the source item stack being merged.
     *
     * @return Amount of items in source stack
     */
    public int getMergedAmount() {
        return entity.getItemStack().getAmount();
    }

    /**
     * Gets the current amount of items in the target item stack.
     *
     * @return Amount of items in target stack
     */
    public int getTargetAmount() {
        return target.getItemStack().getAmount();
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
