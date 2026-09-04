package fr.skynex.lootglow.config.modules;

import org.bukkit.configuration.file.FileConfiguration;

public class LodConfig {

    private boolean enabled = true;
    private double holoDistSq = 576.0;
    private double beamDistSq = 2304.0;
    private double partDistSq = 1024.0;
    private int interval = 20;

    public void load(FileConfiguration config) {
        this.enabled = config.getBoolean("settings.performance.lod.enabled", true);
        double holoDist = config.getDouble("settings.performance.lod.hologram-distance", 24.0);
        this.holoDistSq = holoDist * holoDist;
        double beamDist = config.getDouble("settings.performance.lod.beam-distance", 48.0);
        this.beamDistSq = beamDist * beamDist;
        double partDist = config.getDouble("settings.performance.lod.particle-distance", 32.0);
        this.partDistSq = partDist * partDist;
        this.interval = config.getInt("settings.performance.lod.check-interval", 20);
    }

    public boolean isEnabled() { return enabled; }
    public double getHoloDistSq() { return holoDistSq; }
    public double getBeamDistSq() { return beamDistSq; }
    public double getPartDistSq() { return partDistSq; }
    public int getInterval() { return interval; }
}
