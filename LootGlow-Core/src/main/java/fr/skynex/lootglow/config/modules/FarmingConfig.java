package fr.skynex.lootglow.config.modules;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FarmingConfig {

    private boolean enabled = true;
    private NamedTextColor glowColor = NamedTextColor.GREEN;
    private Material material = Material.EMERALD_BLOCK;
    private float scale = 0.2f;
    private double offset = 1.5;
    private boolean animation = true;
    private double viewDistance = 24.0;
    private final Set<Material> crops = new HashSet<>();

    public void load(FileConfiguration config, LootGlow plugin) {
        var cfgParser = plugin.getConfigManager() != null ? plugin.getConfigManager().getConfigParser() : new fr.skynex.lootglow.config.ConfigParser();
        this.glowColor = cfgParser.parseNamedColor(config.getString("settings.farming.glow-color", "GREEN"));
        String symbolMatStr = config.getString("settings.farming.symbol-material", "EMERALD_BLOCK");
        this.material = Material.matchMaterial(symbolMatStr);
        if (this.material == null || !this.material.isBlock()) {
            if (symbolMatStr != null && !symbolMatStr.isEmpty()) {
                plugin.getLogger().warning("[LootGlow] Farming symbol-material '" + symbolMatStr + "' is invalid or not a block! Falling back to EMERALD_BLOCK.");
            }
            this.material = Material.EMERALD_BLOCK;
        }
        this.scale = (float) config.getDouble("settings.farming.symbol-scale", 0.2);
        this.offset = config.getDouble("settings.farming.height-offset", 1.5);
        this.animation = config.getBoolean("settings.farming.animation", true);
        this.viewDistance = config.getDouble("settings.farming.view-distance", 24.0);

        this.crops.clear();
        List<String> rawCrops = config.getStringList("settings.farming.crops");
        if (rawCrops != null && !rawCrops.isEmpty()) {
            for (String matName : rawCrops) {
                Material mat = Material.matchMaterial(matName);
                if (mat != null) {
                    this.crops.add(mat);
                }
            }
        }
    }

    public boolean isEnabled() { return enabled; }
    public NamedTextColor getGlowColor() { return glowColor; }
    public Material getMaterial() { return material; }
    public float getScale() { return scale; }
    public double getOffset() { return offset; }
    public boolean isAnimation() { return animation; }
    public double getViewDistance() { return viewDistance; }
    public Set<Material> getCrops() { return crops; }
}
