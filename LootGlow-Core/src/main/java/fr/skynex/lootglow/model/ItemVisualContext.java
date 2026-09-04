package fr.skynex.lootglow.model;

import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Context record encapsulating visual spawn configuration for 3D ItemDisplays.
 */
public record ItemVisualContext(
        boolean useVisualBag,
        boolean rpgDropsEnabled,
        Map<UUID, ?> groupLeaders,
        Map<UUID, ItemDisplay> activeItemVisuals,
        Map<Integer, UUID> entityIdMap,
        Set<String> rpgEnabledCategories,
        Set<UUID> hiddenVisuals,
        Map<UUID, Set<UUID>> visibleEntities,
        Map<String, Boolean> categoryGlow,
        boolean defaultGlow,
        Material bagMaterial,
        String bagHeadTexture,
        boolean useOwnerHead,
        int bagCustomModelData,
        float rpgItemScale,
        float rpgBlockScale,
        float rpgRotation
) {}
