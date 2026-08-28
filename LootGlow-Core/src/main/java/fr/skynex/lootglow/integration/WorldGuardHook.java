package fr.skynex.lootglow.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import java.util.List;

public class WorldGuardHook {
    public static StateFlag LOOTGLOW_FARMING_FLAG;

    public static void registerFlag() {
        try {
            StateFlag flag = new StateFlag("lootglow-farming", true);
            WorldGuard.getInstance().getFlagRegistry().register(flag);
            LOOTGLOW_FARMING_FLAG = flag;
            Bukkit.getLogger().info("Successfully registered WorldGuard flag 'lootglow-farming'");
        } catch (FlagConflictException e) {
            com.sk89q.worldguard.protection.flags.Flag<?> existing = WorldGuard.getInstance().getFlagRegistry().get("lootglow-farming");
            if (existing instanceof StateFlag) {
                LOOTGLOW_FARMING_FLAG = (StateFlag) existing;
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean isInBlockedRegion(Location loc, List<String> wgBlockedRegions) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(loc);
            ApplicableRegionSet set = query.getApplicableRegions(wgLoc);
            for (ProtectedRegion region : set) {
                if (wgBlockedRegions.contains(region.getId().toLowerCase()))
                    return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isFarmingAllowed(Location loc) {
        if (LOOTGLOW_FARMING_FLAG == null) {
            try {
                com.sk89q.worldguard.protection.flags.Flag<?> existing = WorldGuard.getInstance().getFlagRegistry().get("lootglow-farming");
                if (existing instanceof StateFlag) {
                    LOOTGLOW_FARMING_FLAG = (StateFlag) existing;
                }
            } catch (Throwable ignored) {}
        }

        if (LOOTGLOW_FARMING_FLAG == null)
            return true;

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(loc);
            StateFlag.State state = query.queryState(wgLoc, null, LOOTGLOW_FARMING_FLAG);
            return state != StateFlag.State.DENY;
        } catch (Exception e) {
            return true;
        }
    }
}
