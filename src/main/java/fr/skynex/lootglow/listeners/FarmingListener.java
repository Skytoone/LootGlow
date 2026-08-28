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
        if (plugin.getFarmingCrops().contains(event.getNewState().getType())) {
            if (event.getNewState().getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() == ageable.getMaximumAge()) {
                    // We need a small delay because the block state is being updated
                    FoliaScheduler.runLater(plugin, () -> {
                        plugin.spawnCropSymbol(event.getBlock());
                    }, 1L);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        plugin.removeCropSymbol(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().forEach(plugin::removeCropSymbol);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        event.blockList().forEach(plugin::removeCropSymbol);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        event.getBlocks().forEach(plugin::removeCropSymbol);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        event.getBlocks().forEach(plugin::removeCropSymbol);
    }
}
