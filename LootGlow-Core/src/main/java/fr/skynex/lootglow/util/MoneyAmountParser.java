package fr.skynex.lootglow.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Utility for reading money amounts from ItemStack PDC for economy drop display.
 */
public class MoneyAmountParser {

    private MoneyAmountParser() {}

    private static final NamespacedKey[] PRESETS = {
            new NamespacedKey("economyshopgui", "value"),
            new NamespacedKey("money", "amount"),
            new NamespacedKey("moneydrops", "value"),
            new NamespacedKey("tne", "value")
    };

    public static Double getMoneyAmount(ItemStack stack, boolean economyEnabled, List<NamespacedKey> economyKeys) {
        if (!economyEnabled || stack == null || !stack.hasItemMeta()) return null;
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();

        // Scan custom keys from config
        for (NamespacedKey key : economyKeys) {
            if (pdc.has(key, PersistentDataType.DOUBLE))
                return pdc.get(key, PersistentDataType.DOUBLE);
            if (pdc.has(key, PersistentDataType.INTEGER))
                return (double) pdc.get(key, PersistentDataType.INTEGER);
            if (pdc.has(key, PersistentDataType.STRING)) {
                try {
                    return Double.parseDouble(pdc.get(key, PersistentDataType.STRING));
                } catch (Exception ignored) {}
            }
        }

        // Internal presets (common economy plugins)
        for (NamespacedKey key : PRESETS) {
            if (pdc.has(key, PersistentDataType.DOUBLE))
                return pdc.get(key, PersistentDataType.DOUBLE);
            if (pdc.has(key, PersistentDataType.INTEGER))
                return (double) pdc.get(key, PersistentDataType.INTEGER);
        }

        return null;
    }
}
