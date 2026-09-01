package fr.skynex.lootglow.model;

import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;

/**
 * Wrapper pour les symboles de crop farming.
 * Étend ArrayList<BlockDisplay> pour la compatibilité avec le code existant
 * et pré-cache la Location pour éviter block.getLocation() dans les boucles LOD.
 */
public final class CropSymbol extends java.util.ArrayList<BlockDisplay> {
    public final Location location;

    public CropSymbol(Location location) {
        this.location = location;
    }
}
