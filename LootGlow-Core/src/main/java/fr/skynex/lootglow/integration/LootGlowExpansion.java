package fr.skynex.lootglow.integration;

import fr.skynex.lootglow.LootGlow;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import fr.skynex.lootglow.managers.TrackedItemManager;
import fr.skynex.lootglow.managers.RarityManager;

/**
 * PlaceholderAPI expansion for LootGlow.
 * Provides placeholders: %lootglow_items_nearby%, %lootglow_rarest_item_nearby%,
 * %lootglow_toggle_status%, %lootglow_total_tracked%.
 */
public class LootGlowExpansion extends PlaceholderExpansion {

    private final LootGlow plugin;

    public LootGlowExpansion(LootGlow plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lootglow";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Skynex";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        var trackedMgr = plugin.getService(TrackedItemManager.class);
        if (params.equalsIgnoreCase("total_tracked")) {
            return String.valueOf(trackedMgr != null ? trackedMgr.getTrackedItems().size() : 0);
        }

        if (player == null) {
            return "";
        }

        String lowerParams = params.toLowerCase();

        switch (lowerParams) {
            case "toggle_status" -> {
                boolean isHidden = plugin.getStateRepository().getHiddenVisuals().contains(player.getUniqueId());
                return isHidden ? "Désactivé" : "Activé";
            }
            case "items_nearby" -> {
                List<Item> nearby = trackedMgr != null ? trackedMgr.getNearbyGlowingItems(player.getLocation(), 15.0) : Collections.emptyList();
                return String.valueOf(nearby.size());
            }
            case "rarest_item_nearby" -> {
                List<Item> nearby = trackedMgr != null ? trackedMgr.getNearbyGlowingItems(player.getLocation(), 15.0) : Collections.emptyList();
                if (nearby.isEmpty()) return "Aucun";

                String rarestCat = null;
                for (Item item : nearby) {
                    String cat = trackedMgr != null ? trackedMgr.getItemCategory(item.getUniqueId()) : null;
                    if (cat != null) {
                        rarestCat = cat;
                        if (cat.equalsIgnoreCase("LEGENDARY") || cat.equalsIgnoreCase("MYTHIC")) {
                            break;
                        }
                    }
                }
                return rarestCat != null ? rarestCat.toUpperCase() : "COMMUN";
            }
        }

        // Handle dynamic parameter: items_nearby_<radius> or items_nearby_<rarity>_<radius>
        if (lowerParams.startsWith("items_nearby_")) {
            String sub = lowerParams.substring("items_nearby_".length());
            String[] parts = sub.split("_");
            double radius = 15.0;
            String rarityFilter = null;

            if (parts.length == 1) {
                try {
                    radius = Double.parseDouble(parts[0]);
                } catch (NumberFormatException e) {
                    rarityFilter = parts[0];
                }
            } else if (parts.length >= 2) {
                rarityFilter = parts[0];
                try {
                    radius = Double.parseDouble(parts[1]);
                } catch (NumberFormatException ignored) {}
            }

            List<Item> nearby = trackedMgr != null ? trackedMgr.getNearbyGlowingItems(player.getLocation(), radius) : Collections.emptyList();
            if (rarityFilter == null) {
                return String.valueOf(nearby.size());
            }

            final String finalRarityFilter = rarityFilter;
            var rarityMgr = plugin.getService(RarityManager.class);
            long count = nearby.stream().filter(item -> {
                if (rarityMgr != null) {
                    fr.skynex.lootglow.managers.RarityManager.ItemRarity rarity = rarityMgr.detectRarity(item.getItemStack());
                    return rarity.name().equalsIgnoreCase(finalRarityFilter);
                }
                return false;
            }).count();

            return String.valueOf(count);
        }

        return null;
    }
}
