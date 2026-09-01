package fr.skynex.lootglow.model;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

/**
 * Anime un visual display en vol vers un joueur (aspiration pickup).
 */
public class VisualAnimation {
    public ItemDisplay display;
    public Player target;
    public double scale = 1.0;
    public int ticks = 0;

    public VisualAnimation(ItemDisplay display, Player target) {
        this.display = display;
        this.target = target;
    }
}
