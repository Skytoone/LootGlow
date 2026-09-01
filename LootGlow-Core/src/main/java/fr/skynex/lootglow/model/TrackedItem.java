package fr.skynex.lootglow.model;

import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

/**
 * Regroupe tous les états visuels et cachés d'un item tracké en un seul objet.
 * Remplace les 10+ Maps séparées par un seul lookup dans trackedItems.
 */
public class TrackedItem {
    // Displays visuels
    public TextDisplay label;
    public BlockDisplay beam;
    public ItemDisplay visual;
    public org.bukkit.entity.Display shadow;
    // Timing
    public Long spawnTime;
    // Hologram state
    public Long lastHoloState;
    public Component baseName;
    // Catégorie & particules
    public String category;
    public Particle particle;
    // Économie
    public Double moneyAmount;
    // Throttle ray-trace (globalSyncTick du dernier appel à updateSurfaceAlignment)
    public int lastRayTraceTick = -999;
}
