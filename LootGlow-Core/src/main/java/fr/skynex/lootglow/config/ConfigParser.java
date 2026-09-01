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

    private final LootGlow plugin;

    public ConfigParser(LootGlow plugin) {
        this.plugin = plugin;
    }

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

        try {
            java.lang.reflect.Field field = Color.class.getField(trimmed.toUpperCase());
            if (field.getType().equals(Color.class)) {
                return (Color) field.get(null);
            }
        } catch (Exception ignored) {}

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
