package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Material;

/**
 * Manages RPG item rotation math and display transformation updates.
 */
public class ItemRotationService {

    private final LootGlow plugin;
    private float rpgRotation = 0f;

    public ItemRotationService(LootGlow plugin) {
        this.plugin = plugin;
    }

    public float getRpgRotation() {
        return rpgRotation;
    }

    public void incrementRpgRotation(float amount) {
        rpgRotation = (rpgRotation + amount) % 360f;
    }

    public float calculateTargetRotX(boolean isCustom, boolean isUpright, float defaultRpgRotation) {
        return (isCustom || isUpright) ? 0f : defaultRpgRotation;
    }
}
