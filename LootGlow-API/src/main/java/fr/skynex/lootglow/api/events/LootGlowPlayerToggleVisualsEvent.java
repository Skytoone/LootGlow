package fr.skynex.lootglow.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player toggles their global LootGlow visual effects visibility setting.
 */
public class LootGlowPlayerToggleVisualsEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final boolean visualsHidden;

    public LootGlowPlayerToggleVisualsEvent(@NotNull Player player, boolean visualsHidden) {
        this.player = player;
        this.visualsHidden = visualsHidden;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    public boolean isVisualsHidden() {
        return visualsHidden;
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
