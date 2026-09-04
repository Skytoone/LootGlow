package fr.skynex.lootglow.model;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Context record encapsulating grouping task parameters and visual bundle options.
 */
public record ItemGroupingContext(
        boolean isEnabled,
        boolean groupingEnabled,
        Map<UUID, ?> trackedItems,
        Map<UUID, Item> activeItems,
        Map<UUID, String> itemCategoriesCache,
        Set<UUID> groupedItems,
        Map<UUID, Integer> groupLeaders,
        Map<UUID, List<UUID>> groupMembers,
        Map<UUID, ItemDisplay> activeItemVisuals,
        boolean useVisualBag,
        Material bagMaterial,
        String bagHeadTexture,
        boolean useOwnerHead,
        int bagCustomModelData,
        float rpgRotation,
        boolean holoShowTimer,
        String rawBundleFormat,
        Map<String, NamedTextColor> itemCategories,
        NamedTextColor defaultColor,
        MiniMessage miniMessage
) {}
