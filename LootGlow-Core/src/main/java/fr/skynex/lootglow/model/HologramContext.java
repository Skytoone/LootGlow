package fr.skynex.lootglow.model;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.TextDisplay;

import java.util.Map;
import java.util.UUID;

/**
 * Context record encapsulating configuration and state for TextDisplay hologram creation and updates.
 */
public record HologramContext(
        boolean holoEnabled,
        Map<UUID, String> itemCategoriesCache,
        boolean holoHideUncategorized,
        Map<UUID, TextDisplay> activeLabels,
        Map<UUID, ?> groupLeaders,
        Map<UUID, Long> lastHoloState,
        Map<UUID, Component> baseNameCache,
        Map<String, Component> displayNameOverridesCache,
        Map<UUID, Double> itemMoneyAmounts,
        String economyFormat,
        String economyPrefix,
        boolean holoShowAmount,
        String rawAmountFormat,
        boolean protectionEnabled,
        int protectionDuration,
        Map<UUID, Long> itemSpawnTimes,
        String rawOwnerFormat,
        boolean usePapi,
        boolean holoShowTimer,
        Map<Integer, Component> timerComponentCache,
        boolean holoTimerNewLine
) {}
