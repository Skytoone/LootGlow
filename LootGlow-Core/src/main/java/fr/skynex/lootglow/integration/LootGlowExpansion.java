package fr.skynex.lootglow.integration;

import fr.skynex.lootglow.LootGlow;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        if (params.equalsIgnoreCase("total_tracked")) {
            return String.valueOf(plugin.getTrackedItemManager().getTrackedItems().size());
        }

        if (player == null) {
            return "";
        }

        String lowerParams = params.toLowerCase();

        switch (lowerParams) {
            case "toggle_status" -> {
                boolean isHidden = plugin.getHiddenVisuals().contains(player.getUniqueId());
                return isHidden ? "Désactivé" : "Activé";
            }
            case "items_nearby" -> {
                List<Item> nearby = plugin.getTrackedItemManager().getNearbyGlowingItems(player.getLocation(), 15.0);
                return String.valueOf(nearby.size());
            }
            case "rarest_item_nearby" -> {
                List<Item> nearby = plugin.getTrackedItemManager().getNearbyGlowingItems(player.getLocation(), 15.0);
                if (nearby.isEmpty()) return "Aucun";

                String rarestCat = null;
                for (Item item : nearby) {
                    String cat = plugin.getTrackedItemManager().getItemCategory(item.getUniqueId());
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

            List<Item> nearby = plugin.getTrackedItemManager().getNearbyGlowingItems(player.getLocation(), radius);
            if (rarityFilter == null) {
                return String.valueOf(nearby.size());
            }

            final String finalRarityFilter = rarityFilter;
            long count = nearby.stream().filter(item -> {
                if (plugin.getRarityManager() != null) {
                    fr.skynex.lootglow.managers.RarityManager.ItemRarity rarity = plugin.getRarityManager().detectRarity(item.getItemStack());
                    return rarity.name().equalsIgnoreCase(finalRarityFilter);
                }
                return false;
            }).count();

            return String.valueOf(count);
        }

        return null;
    }
}
