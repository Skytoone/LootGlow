package fr.skynex.lootglow.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Stateless utility for classifying item types: flat/upright/fish/custom.
 */
public class ItemTypeClassifier {

    private ItemTypeClassifier() {}

    public static boolean isFlatItemOrBlock(Material mat, Set<Material> forceFlatMaterials, Set<Material> forceUprightMaterials) {
        if (mat == null || mat == Material.AIR) return false;
        if (forceFlatMaterials.contains(mat)) return true;
        if (forceUprightMaterials.contains(mat)) return false;
        String name = mat.name();
        return name.endsWith("_DOOR") || name.endsWith("_SIGN") || name.endsWith("_HANGING_SIGN")
                || mat == Material.LADDER || mat == Material.PAINTING
                || mat == Material.ITEM_FRAME || mat == Material.GLOW_ITEM_FRAME;
    }

    public static boolean isUprightItem(Material mat, Set<Material> forceFlatMaterials, Set<Material> forceUprightMaterials) {
        if (mat == null || mat == Material.AIR) return false;
        if (isFlatItemOrBlock(mat, forceFlatMaterials, forceUprightMaterials)) return false;
        if (forceUprightMaterials != null && forceUprightMaterials.contains(mat)) return true;
        String name = mat.name();
        return name.endsWith("_HEAD") || name.endsWith("_SKULL")
                || name.endsWith("_BANNER") || name.endsWith("_BED")
                || mat == Material.ARMOR_STAND;
    }

    public static boolean isFishItem(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.contains("FISH") || name.contains("SALMON") || name.contains("COD");
    }

    public static boolean isCustomItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        org.bukkit.persistence.PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
            String ns = key.getNamespace();
            if (ns.equalsIgnoreCase("oraxen") || ns.equalsIgnoreCase("itemsadder") || ns.equalsIgnoreCase("nexo") || key.getKey().equalsIgnoreCase("custom_item")) {
                return true;
            }
        }
        return false;
    }

    public static String getInternalId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        org.bukkit.persistence.PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
            String ns = key.getNamespace();
            if (ns.equalsIgnoreCase("oraxen") || ns.equalsIgnoreCase("itemsadder") || ns.equalsIgnoreCase("nexo") || key.getKey().equalsIgnoreCase("custom_item")) {
                return pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
            }
        }
        return null;
    }
}
