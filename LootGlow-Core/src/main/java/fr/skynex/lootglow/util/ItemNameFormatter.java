package fr.skynex.lootglow.util;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility for formatting item display names and hologram components via MiniMessage and Legacy Adventure serializers.
 */
public class ItemNameFormatter {

    private final LootGlow plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ItemNameFormatter(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Component getItemName(ItemStack itemStack) {
        if (itemStack == null) return Component.empty();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.displayName();
        }
        String typeName = itemStack.getType().name().toLowerCase().replace('_', ' ');
        String capitalized = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
        return Component.text(capitalized);
    }

    public Component parseMiniMessage(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return miniMessage.deserialize(text);
    }
}
