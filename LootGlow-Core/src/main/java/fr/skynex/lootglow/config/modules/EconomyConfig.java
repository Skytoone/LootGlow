package fr.skynex.lootglow.config.modules;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class EconomyConfig {

    private boolean enabled = true;
    private String format = "<prefix><amount>";
    private String prefix = "&a$&f";
    private NamedTextColor color = NamedTextColor.GOLD;
    private Sound sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private final List<NamespacedKey> economyKeys = new ArrayList<>();

    public void load(FileConfiguration config, LootGlow plugin) {
        this.enabled = config.getBoolean("settings.economy.enabled", true);
        this.format = config.getString("settings.economy.format", "<prefix><amount>");
        this.prefix = config.getString("settings.economy.prefix", "&a$&f");
        var cfgParser = plugin.getConfigManager() != null ? plugin.getConfigManager().getConfigParser() : new fr.skynex.lootglow.config.ConfigParser();
        this.color = cfgParser.parseNamedColor(config.getString("settings.economy.color", "GOLD"));
        String ecoSoundStr = config.getString("settings.economy.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        this.sound = cfgParser.parseSound(ecoSoundStr);
        if (this.sound == null) this.sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;

        this.economyKeys.clear();
        List<String> rawKeys = config.getStringList("settings.economy.custom-nbt-keys");
        if (rawKeys != null && !rawKeys.isEmpty()) {
            for (String raw : rawKeys) {
                if (raw.contains(":")) {
                    String[] parts = raw.split(":", 2);
                    this.economyKeys.add(new NamespacedKey(parts[0], parts[1]));
                } else {
                    this.economyKeys.add(NamespacedKey.minecraft(raw));
                }
            }
        }
    }

    public boolean isEnabled() { return enabled; }
    public String getFormat() { return format; }
    public String getPrefix() { return prefix; }
    public NamedTextColor getColor() { return color; }
    public Sound getSound() { return sound; }
    public List<NamespacedKey> getEconomyKeys() { return economyKeys; }
}
