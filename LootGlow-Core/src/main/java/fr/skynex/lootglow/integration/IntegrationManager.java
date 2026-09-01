package fr.skynex.lootglow.integration;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Bukkit;

/**
 * Manages soft dependencies and third-party plugin integrations (MythicMobs, Oraxen, ItemsAdder, WorldGuard).
 */
public class IntegrationManager {

    private final LootGlow plugin;
    private boolean mythicMobsHooked = false;
    private boolean oraxenHooked = false;
    private boolean itemsAdderHooked = false;
    private boolean worldGuardHooked = false;

    public IntegrationManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public boolean isMythicMobsHooked() {
        return mythicMobsHooked;
    }

    public boolean isOraxenHooked() {
        return oraxenHooked;
    }

    public boolean isItemsAdderHooked() {
        return itemsAdderHooked;
    }

    public boolean isWorldGuardHooked() {
        return worldGuardHooked;
    }

    public void checkIntegrations() {
        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            mythicMobsHooked = true;
            plugin.getLogger().info("Hooked into MythicMobs!");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Oraxen")) {
            oraxenHooked = true;
            plugin.getLogger().info("Hooked into Oraxen!");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            itemsAdderHooked = true;
            plugin.getLogger().info("Hooked into ItemsAdder!");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            worldGuardHooked = true;
            plugin.getLogger().info("Hooked into WorldGuard!");
        }
    }
}
