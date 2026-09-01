package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * Handles fishing drop visual effects when players catch fish or treasure items.
 */
public class FishingListener implements Listener {

    private final LootGlow plugin;

    public FishingListener(LootGlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (!plugin.isEnabled()) return;
        PlayerFishEvent.State state = event.getState();
        if (state == PlayerFishEvent.State.CAUGHT_FISH || state == PlayerFishEvent.State.CAUGHT_ENTITY) {
            if (event.getCaught() instanceof Item item && item.isValid()) {
                Location loc = item.getLocation();
                if (!plugin.isWorldAllowed(loc.getWorld().getName())) return;

                FoliaScheduler.runAtEntity(plugin, item, () -> {
                    if (!item.isValid()) return;
                    Location itemLoc = item.getLocation();
                    itemLoc.getWorld().spawnParticle(Particle.SPLASH, itemLoc, 25, 0.3, 0.3, 0.3, 0.1);
                    itemLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, itemLoc, 15, 0.2, 0.2, 0.2, 0.05);
                    itemLoc.getWorld().playSound(itemLoc, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
                });
            }
        }
    }
}
