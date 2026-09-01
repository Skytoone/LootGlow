package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages world filtering (whitelisted and blacklisted worlds).
 */
public class LootWorldManager {

    private final LootGlow plugin;
    private final Set<String> disabledWorlds = new HashSet<>();
    private final Set<String> enabledWorlds = new HashSet<>();
    private boolean useWhitelist = false;

    public LootWorldManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Set<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public Set<String> getEnabledWorlds() {
        return enabledWorlds;
    }

    public boolean isUseWhitelist() {
        return useWhitelist;
    }

    public void reloadWorldFilters() {
        disabledWorlds.clear();
        enabledWorlds.clear();
        
        List<String> disabledList = plugin.getConfig().getStringList("settings.disabled-worlds");
        if (disabledList != null) {
            disabledWorlds.addAll(disabledList);
        }

        List<String> enabledList = plugin.getConfig().getStringList("settings.enabled-worlds");
        if (enabledList != null && !enabledList.isEmpty()) {
            enabledWorlds.addAll(enabledList);
            useWhitelist = true;
        } else {
            useWhitelist = false;
        }
    }

    public boolean isWorldEnabled(World world) {
        if (world == null) return false;
        String name = world.getName();
        if (useWhitelist) {
            return enabledWorlds.contains(name);
        }
        return !disabledWorlds.contains(name);
    }
}
