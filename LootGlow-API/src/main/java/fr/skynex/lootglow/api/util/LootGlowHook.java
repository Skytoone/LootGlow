package fr.skynex.lootglow.api.util;

import fr.skynex.lootglow.api.LootGlowAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

/**
 * Developer helper class providing 1-line convenient access to LootGlowAPI.
 */
public final class LootGlowHook {

    private LootGlowHook() {}

    /**
     * Retrieves LootGlowAPI registered provider instance if LootGlow is active.
     *
     * @return Optional containing LootGlowAPI if available
     */
    public static Optional<LootGlowAPI> getAPI() {
        RegisteredServiceProvider<LootGlowAPI> rsp = Bukkit.getServicesManager().getRegistration(LootGlowAPI.class);
        return rsp != null ? Optional.ofNullable(rsp.getProvider()) : Optional.empty();
    }
}
