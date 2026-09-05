package fr.skynex.lootglow.api.events;

import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired when loot protection expires on a dropped item entity.
 */
public class LootProtectionExpireEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Item item;
    private final UUID ownerUuid;

    public LootProtectionExpireEvent(@NotNull Item item, @Nullable UUID ownerUuid) {
        this.item = item;
        this.ownerUuid = ownerUuid;
    }

    @NotNull
    public Item getItem() {
        return item;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
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
