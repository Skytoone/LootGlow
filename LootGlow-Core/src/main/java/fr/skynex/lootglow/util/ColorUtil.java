package fr.skynex.lootglow.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Utility for parsing text components supporting MiniMessage tags (<color>, <gradient>),
 * legacy section codes (§a, §l), legacy ampersand codes (&a, &l), and hex codes (&#RRGGBB).
 */
public class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // 1. Replace section signs § with ampersands &
        String processed = text.replace('§', '&');

        // 2. Convert hex pattern &#RRGGBB to MiniMessage <#RRGGBB>
        processed = processed.replaceAll("(?i)&#([a-f0-9]{6})", "<#$1>");

        // 3. Convert legacy color and format codes &0-&f, &k-&r to MiniMessage tags
        if (processed.contains("&")) {
            processed = processed
                    .replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                    .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                    .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                    .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                    .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                    .replace("&f", "<white>").replace("&k", "<obfuscated>").replace("&l", "<bold>")
                    .replace("&m", "<strikethrough>").replace("&n", "<underlined>").replace("&o", "<italic>")
                    .replace("&r", "<reset>")
                    .replace("&A", "<green>").replace("&B", "<aqua>").replace("&C", "<red>")
                    .replace("&D", "<light_purple>").replace("&E", "<yellow>").replace("&F", "<white>")
                    .replace("&K", "<obfuscated>").replace("&L", "<bold>").replace("&M", "<strikethrough>")
                    .replace("&N", "<underlined>").replace("&O", "<italic>").replace("&R", "<reset>");
        }

        try {
            return MINI_MESSAGE.deserialize(processed);
        } catch (Exception e) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }
    }
}
