package fr.skynex.lootglow.config;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;

/**
 * Utility parser for colors, particles, sounds, and configuration parameters.
 */
public class ConfigParser {

    public ConfigParser() {}

    public ConfigParser(LootGlow plugin) {}

    public NamedTextColor parseNamedColor(String input) {
        if (input == null || input.trim().isEmpty()) return NamedTextColor.WHITE;
        try {
            NamedTextColor ntc = NamedTextColor.NAMES.value(input.trim().toLowerCase());
            if (ntc != null) return ntc;
        } catch (Exception ignored) {}
        return NamedTextColor.WHITE;
    }

    public Sound parseSound(String soundStr) {
        if (soundStr == null || soundStr.trim().isEmpty()) return null;
        String lower = soundStr.trim().toLowerCase();
        if (lower.equals("none") || lower.equals("off") || lower.equals("disabled") || lower.equals("false") || lower.equals("\"\"") || lower.equals("''")) {
            return null;
        }

        try {
            if (lower.contains(":")) {
                NamespacedKey key = NamespacedKey.fromString(lower);
                if (key != null) {
                    Sound sound = Registry.SOUND_EVENT.get(key);
                    if (sound != null) return sound;
                }
            }

            NamespacedKey mcKey = NamespacedKey.minecraft(lower);
            Sound mcSound = Registry.SOUND_EVENT.get(mcKey);
            if (mcSound != null) return mcSound;

            NamespacedKey legacyKey = NamespacedKey.minecraft(lower.replace("_", "."));
            Sound legacySound = Registry.SOUND_EVENT.get(legacyKey);
            if (legacySound != null) return legacySound;
        } catch (Exception ignored) {}

        return null;
    }

    private static final java.util.Map<String, Color> BUKKIT_COLORS = java.util.Map.ofEntries(
            java.util.Map.entry("WHITE", Color.WHITE),
            java.util.Map.entry("SILVER", Color.SILVER),
            java.util.Map.entry("GRAY", Color.GRAY),
            java.util.Map.entry("BLACK", Color.BLACK),
            java.util.Map.entry("RED", Color.RED),
            java.util.Map.entry("MAROON", Color.MAROON),
            java.util.Map.entry("YELLOW", Color.YELLOW),
            java.util.Map.entry("OLIVE", Color.OLIVE),
            java.util.Map.entry("LIME", Color.LIME),
            java.util.Map.entry("GREEN", Color.GREEN),
            java.util.Map.entry("AQUA", Color.AQUA),
            java.util.Map.entry("TEAL", Color.TEAL),
            java.util.Map.entry("BLUE", Color.BLUE),
            java.util.Map.entry("NAVY", Color.NAVY),
            java.util.Map.entry("FUCHSIA", Color.FUCHSIA),
            java.util.Map.entry("PURPLE", Color.PURPLE),
            java.util.Map.entry("ORANGE", Color.ORANGE)
    );

    public Color parseColor(String input) {
        if (input == null || input.trim().isEmpty()) return Color.WHITE;
        String trimmed = input.trim();

        if (trimmed.startsWith("#") && (trimmed.length() == 7 || trimmed.length() == 9)) {
            try {
                int hex = Integer.parseInt(trimmed.substring(1), 16);
                return Color.fromRGB((hex >> 16) & 0xFF, (hex >> 8) & 0xFF, hex & 0xFF);
            } catch (Exception ignored) {}
        }

        try {
            NamedTextColor ntc = NamedTextColor.NAMES.value(trimmed.toLowerCase());
            if (ntc != null) {
                return Color.fromRGB(ntc.red(), ntc.green(), ntc.blue());
            }
        } catch (Exception ignored) {}

        Color predefined = BUKKIT_COLORS.get(trimmed.toUpperCase());
        if (predefined != null) return predefined;

        return Color.WHITE;
    }

    public Particle parseParticle(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        try {
            return Particle.valueOf(input.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
