package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;

public class FarmingListener implements Listener {

    private final LootGlow plugin;

    public FarmingListener(LootGlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent event) {
        var cfgMgr = plugin.getConfigManager();
        var farmMgr = plugin.getService(fr.skynex.lootglow.managers.FarmingManager.class);
        if (cfgMgr != null && cfgMgr.getFarmingCrops().contains(event.getNewState().getType())) {
            if (event.getNewState().getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() == ageable.getMaximumAge()) {
                    // We need a small delay because the block state is being updated
                    FoliaScheduler.runLater(plugin, () -> {
                        if (farmMgr != null) farmMgr.spawnCropSymbol(event.getBlock());
                    }, 1L);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        var farmMgr = plugin.getService(fr.skynex.lootglow.managers.FarmingManager.class);
        if (farmMgr != null) farmMgr.removeCropSymbol(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        var farmMgr = plugin.getService(fr.skynex.lootglow.managers.FarmingManager.class);
        if (farmMgr != null) event.blockList().forEach(farmMgr::removeCropSymbol);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        var farmMgr = plugin.getService(fr.skynex.lootglow.managers.FarmingManager.class);
        if (farmMgr != null) event.blockList().forEach(farmMgr::removeCropSymbol);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        var farmMgr = plugin.getService(fr.skynex.lootglow.managers.FarmingManager.class);
        if (farmMgr != null) event.getBlocks().forEach(farmMgr::removeCropSymbol);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        var farmMgr = plugin.getService(fr.skynex.lootglow.managers.FarmingManager.class);
        if (farmMgr != null) event.getBlocks().forEach(farmMgr::removeCropSymbol);
    }
}
