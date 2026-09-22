package fr.skynex.lootglow.config;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility parser for colors, particles, sounds, and configuration parameters.
 */
public class ConfigParser {

    public ConfigParser() {}

    public ConfigParser(LootGlow plugin) {}

    public NamedTextColor parseNamedColor(String input) {
        if (input == null || input.trim().isEmpty()) return NamedTextColor.WHITE;
        String clean = input.trim().toLowerCase().replace("&", "").replace("§", "").replace("-", "_").replace(" ", "_");
        try {
            NamedTextColor ntc = NamedTextColor.NAMES.value(clean);
            if (ntc != null) return ntc;
        } catch (Exception ignored) {}

        return switch (clean) {
            case "0", "black" -> NamedTextColor.BLACK;
            case "1", "dark_blue", "darkblue" -> NamedTextColor.DARK_BLUE;
            case "2", "dark_green", "darkgreen" -> NamedTextColor.DARK_GREEN;
            case "3", "dark_aqua", "darkaqua" -> NamedTextColor.DARK_AQUA;
            case "4", "dark_red", "darkred" -> NamedTextColor.DARK_RED;
            case "5", "dark_purple", "darkpurple", "purple" -> NamedTextColor.DARK_PURPLE;
            case "6", "gold" -> NamedTextColor.GOLD;
            case "7", "gray", "grey" -> NamedTextColor.GRAY;
            case "8", "dark_gray", "dark_grey", "darkgray", "darkgrey" -> NamedTextColor.DARK_GRAY;
            case "9", "blue" -> NamedTextColor.BLUE;
            case "a", "green" -> NamedTextColor.GREEN;
            case "b", "aqua" -> NamedTextColor.AQUA;
            case "c", "red" -> NamedTextColor.RED;
            case "d", "light_purple", "lightpurple", "pink" -> NamedTextColor.LIGHT_PURPLE;
            case "e", "yellow" -> NamedTextColor.YELLOW;
            case "f", "white" -> NamedTextColor.WHITE;
            default -> NamedTextColor.WHITE;
        };
    }

    private static final Map<String, Sound> SOUND_CACHE = new HashMap<>();
    private static volatile boolean soundCacheInitialized = false;

    private static void ensureSoundCache() {
        if (soundCacheInitialized) return;
        synchronized (SOUND_CACHE) {
            if (soundCacheInitialized) return;
            try {
                for (Sound sound : Registry.SOUNDS) {
                    NamespacedKey key = Registry.SOUNDS.getKey(sound);
                    if (key == null) continue;
                    String full = key.toString().toLowerCase(Locale.ROOT);
                    String path = key.getKey().toLowerCase(Locale.ROOT);
                    String enumStyle = path.replace('.', '_');

                    SOUND_CACHE.putIfAbsent(full, sound);
                    SOUND_CACHE.putIfAbsent(path, sound);
                    SOUND_CACHE.putIfAbsent(enumStyle, sound);
                }
                soundCacheInitialized = true;
            } catch (Throwable ignored) {}
        }
    }

    public Sound parseSound(String soundStr) {
        if (soundStr == null || soundStr.trim().isEmpty()) return null;
        String trimmed = soundStr.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.equals("none") || lower.equals("off") || lower.equals("disabled") || lower.equals("false") || lower.equals("\"\"") || lower.equals("''")) {
            return null;
        }

        // 1. Check sound cache populated from Registry.SOUNDS (supports minecraft:key, key, and legacy ENUM_STYLE)
        ensureSoundCache();
        Sound cached = SOUND_CACHE.get(lower);
        if (cached != null) {
            return cached;
        }

        // 2. Direct Registry lookup for NamespacedKey (for datapacks or dynamically registered sounds)
        try {
            if (lower.contains(":")) {
                NamespacedKey key = NamespacedKey.fromString(lower);
                if (key != null) {
                    Sound sound = Registry.SOUNDS.get(key);
                    if (sound != null) return sound;
                }
            } else {
                NamespacedKey mcKey = NamespacedKey.minecraft(lower);
                Sound mcSound = Registry.SOUNDS.get(mcKey);
                if (mcSound != null) return mcSound;

                NamespacedKey legacyKey = NamespacedKey.minecraft(lower.replace('_', '.'));
                Sound legacySound = Registry.SOUNDS.get(legacyKey);
                if (legacySound != null) return legacySound;
            }
        } catch (Throwable ignored) {}

        // 3. Fallback for offline/unit-test environments where Registry may not be initialized
        try {
            @SuppressWarnings("deprecation")
            Sound legacy = Sound.valueOf(trimmed.toUpperCase(Locale.ROOT));
            return legacy;
        } catch (Throwable ignored) {}

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
