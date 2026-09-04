package fr.skynex.lootglow.config.modules;

import org.bukkit.configuration.file.FileConfiguration;

public class ParticleConfig {

    private boolean enabled = true;
    private int frequency = 10;
    private String animType = "STILL";
    private double size = 1.0;

    public void load(FileConfiguration config) {
        this.enabled = config.getBoolean("settings.particles.enabled", true);
        this.frequency = config.getInt("settings.particles.update-frequency", 10);
        this.animType = config.getString("settings.particles.animation-type", "STILL");
        this.size = config.getDouble("settings.particles.particle-size", 1.0);
    }

    public boolean isEnabled() { return enabled; }
    public int getFrequency() { return frequency; }
    public String getAnimType() { return animType; }
    public double getSize() { return size; }
}
