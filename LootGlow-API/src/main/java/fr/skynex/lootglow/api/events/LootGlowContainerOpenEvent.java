package fr.skynex.lootglow.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fired when a player right-clicks a grouped LootGlow item stack to open the Loot Container GUI.
 */
public class LootGlowContainerOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final List<ItemStack> items;

    public LootGlowContainerOpenEvent(@NotNull Player player, @NotNull List<ItemStack> items) {
        this.player = player;
        this.items = items;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public List<ItemStack> getItems() {
        return items;
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
