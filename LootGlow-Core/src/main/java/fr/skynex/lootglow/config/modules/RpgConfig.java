package fr.skynex.lootglow.config.modules;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RpgConfig {

    private boolean enabled = true;
    private List<String> enabledCategories = new ArrayList<>();
    private float rotation = (float) Math.toRadians(90.0);
    private float itemScale = 0.6f;
    private float blockScale = 0.8f;
    private final Set<Material> forceFlatMaterials = new HashSet<>();
    private final Set<Material> forceUprightMaterials = new HashSet<>();
    private boolean shadowsEnabled = true;
    private float shadowScale = 0.4f;
    private boolean bobbingEnabled = true;
    private double bobbingAmplitude = 0.05;
    private double bobbingSpeed = 0.08;

    public void load(FileConfiguration config) {
        this.enabled = config.getBoolean("settings.rpg-drops.enabled", true);
        List<String> rawRpgCats = config.getStringList("settings.rpg-drops.enabled-categories");
        this.enabledCategories = new ArrayList<>();
        if (rawRpgCats != null) {
            for (String c : rawRpgCats) {
                if (c != null && !c.isBlank()) this.enabledCategories.add(c.toLowerCase());
            }
        }
        this.rotation = (float) Math.toRadians(config.getDouble("settings.rpg-drops.rotation-angle", 90.0));
        this.itemScale = (float) config.getDouble("settings.rpg-drops.item-scale", 0.6);
        this.blockScale = (float) config.getDouble("settings.rpg-drops.block-scale", 0.8);
        this.shadowsEnabled = config.getBoolean("settings.rpg-drops.shadows.enabled", true);
        this.shadowScale = (float) config.getDouble("settings.rpg-drops.shadows.scale", 0.4);
        this.bobbingEnabled = config.getBoolean("settings.rpg-drops.bobbing.enabled", true);
        this.bobbingAmplitude = config.getDouble("settings.rpg-drops.bobbing.amplitude", 0.05);
        this.bobbingSpeed = config.getDouble("settings.rpg-drops.bobbing.speed", 0.08);

        this.forceFlatMaterials.clear();
        List<String> flatList = config.getStringList("settings.rpg-drops.force-flat-items");
        if (flatList != null) {
            for (String matStr : flatList) {
                Material mat = Material.matchMaterial(matStr);
                if (mat != null) this.forceFlatMaterials.add(mat);
            }
        }

        this.forceUprightMaterials.clear();
        List<String> uprightList = config.getStringList("settings.rpg-drops.force-upright-items");
        if (uprightList != null) {
            for (String matStr : uprightList) {
                Material mat = Material.matchMaterial(matStr);
                if (mat != null) this.forceUprightMaterials.add(mat);
            }
        }
    }

    public boolean isEnabled() { return enabled; }
    public List<String> getEnabledCategories() { return enabledCategories; }
    public float getRotation() { return rotation; }
    public float getItemScale() { return itemScale; }
    public float getBlockScale() { return blockScale; }
    public Set<Material> getForceFlatMaterials() { return forceFlatMaterials; }
    public Set<Material> getForceUprightMaterials() { return forceUprightMaterials; }
    public boolean isShadowsEnabled() { return shadowsEnabled; }
    public float getShadowScale() { return shadowScale; }
    public boolean isBobbingEnabled() { return bobbingEnabled; }
    public double getBobbingAmplitude() { return bobbingAmplitude; }
    public double getBobbingSpeed() { return bobbingSpeed; }
}
