package fr.skynex.lootglow.config.modules;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class BeamConfig {

    private boolean enabled = true;
    private float height = 10.0f;
    private float width = 0.05f;
    private List<String> categories = new ArrayList<>();
    private boolean animate = true;
    private boolean useCategoryColor = true;

    public void load(FileConfiguration config) {
        this.enabled = config.getBoolean("settings.beams.enabled", true);
        this.height = (float) config.getDouble("settings.beams.height", 10.0);
        this.width = (float) config.getDouble("settings.beams.width", 0.05);
        List<String> rawBeamCats = config.getStringList("settings.beams.enabled-categories");
        this.categories = new ArrayList<>();
        if (rawBeamCats != null) {
            for (String c : rawBeamCats) {
                if (c != null && !c.isBlank()) this.categories.add(c.toLowerCase());
            }
        }
        this.animate = config.getBoolean("settings.beams.animate", true);
        this.useCategoryColor = config.getBoolean("settings.beams.use-category-color", true);
    }

    public boolean isEnabled() { return enabled; }
    public float getHeight() { return height; }
    public float getWidth() { return width; }
    public List<String> getCategories() { return categories; }
    public boolean isAnimate() { return animate; }
    public boolean isUseCategoryColor() { return useCategoryColor; }
}
