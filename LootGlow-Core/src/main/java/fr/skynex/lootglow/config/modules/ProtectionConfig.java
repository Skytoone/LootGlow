package fr.skynex.lootglow.config.modules;

import org.bukkit.configuration.file.FileConfiguration;

public class ProtectionConfig {

    private boolean enabled = true;
    private int duration = 10;
    private boolean hardLockEnabled = true;
    private String bypassPermission = "lootglow.bypass.lock";

    public void load(FileConfiguration config) {
        this.enabled = config.getBoolean("settings.protection.enabled", true);
        this.duration = config.getInt("settings.protection.duration", 10);
        this.hardLockEnabled = config.getBoolean("settings.protection.hard-lock", true);
        this.bypassPermission = config.getString("settings.protection.bypass-permission", "lootglow.bypass.lock");
    }

    public boolean isEnabled() { return enabled; }
    public int getDuration() { return duration; }
    public boolean isHardLockEnabled() { return hardLockEnabled; }
    public String getBypassPermission() { return bypassPermission; }
}
