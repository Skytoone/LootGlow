package fr.skynex.lootglow.config.modules;

import org.bukkit.configuration.file.FileConfiguration;

public class HologramConfig {

    private boolean enabled = true;
    private double offset = 0.7;
    private boolean seeThrough = false;
    private boolean background = false;
    private float viewDistance = 15.0f;
    private boolean showAmount = true;
    private boolean showTimer = true;
    private boolean timerNewLine = true;
    private boolean hideUncategorized = false;

    public void load(FileConfiguration config) {
        this.enabled = config.getBoolean("settings.hologram.enabled", true);
        this.offset = config.getDouble("settings.hologram.height-offset", 0.7);
        this.seeThrough = config.getBoolean("settings.hologram.see-through", false);
        this.background = config.getBoolean("settings.hologram.background", false);
        this.viewDistance = (float) config.getDouble("settings.hologram.view-distance", 15.0);
        this.showAmount = config.getBoolean("settings.hologram.show-amount", true);
        this.showTimer = config.getBoolean("settings.hologram.show-timer", true);
        this.timerNewLine = config.getBoolean("settings.hologram.timer-new-line", true);
        this.hideUncategorized = config.getBoolean("settings.hologram.hide-uncategorized", false);
    }

    public boolean isEnabled() { return enabled; }
    public double getOffset() { return offset; }
    public boolean isSeeThrough() { return seeThrough; }
    public boolean isBackground() { return background; }
    public float getViewDistance() { return viewDistance; }
    public boolean isShowAmount() { return showAmount; }
    public boolean isShowTimer() { return showTimer; }
    public boolean isTimerNewLine() { return timerNewLine; }
    public boolean isHideUncategorized() { return hideUncategorized; }
}
