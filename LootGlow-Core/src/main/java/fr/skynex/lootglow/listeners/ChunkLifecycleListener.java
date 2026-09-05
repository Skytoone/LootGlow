package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChunkLifecycleListener implements Listener {

    private final LootGlow plugin;

    public ChunkLifecycleListener(LootGlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        var cfgMgr = plugin.getConfigManager();
        var farmMgr = plugin.getService(fr.skynex.lootglow.managers.FarmingManager.class);
        var stateRepo = plugin.getStateRepository();

        if (!event.isNewChunk()) {
            for (Entity entity : event.getChunk().getEntities()) {
                if (entity instanceof BlockDisplay bd && bd.getPersistentDataContainer().has(plugin.getFarmingKey(), PersistentDataType.BYTE)) {
                    Block block = bd.getLocation().getBlock();
                    if (cfgMgr != null && cfgMgr.isFarmingEnabled() && cfgMgr.getFarmingCrops().contains(block.getType())) {
                        if (block.getBlockData() instanceof Ageable age && age.getAge() == age.getMaximumAge()) {
                            if (farmMgr != null) farmMgr.relinkCropSymbol(block, bd);
                            continue;
                        }
                    }
                    bd.remove();
                    continue;
                }
                if (entity instanceof Item item) {
                    UUID uuid = item.getUniqueId();
                    ItemDisplay oldDisplay = stateRepo.getActiveItemVisuals().remove(uuid);
                    if (oldDisplay != null && oldDisplay.isValid()) {
                        oldDisplay.remove();
                    }
                    TextDisplay oldLabel = stateRepo.getActiveLabels().remove(uuid);
                    if (oldLabel != null && oldLabel.isValid()) {
                        oldLabel.remove();
                    }
                    BlockDisplay oldBeam = stateRepo.getActiveBeams().remove(uuid);
                    if (oldBeam != null && oldBeam.isValid()) {
                        oldBeam.getPassengers().forEach(passenger -> passenger.remove());
                        oldBeam.remove();
                    }
                    Entity oldShadow = stateRepo.getActiveShadows().get(uuid);
                    if (oldShadow != null) {
                        stateRepo.getActiveShadows().remove(uuid);
                        if (oldShadow.isValid()) oldShadow.remove();
                    }
                }
            }
        }

        FoliaScheduler.runSync(plugin, () -> {
            if (cfgMgr == null || !cfgMgr.isEnabled()) return;
            var pipeline = plugin.getService(fr.skynex.lootglow.pipeline.LootRenderPipeline.class);

            for (Entity entity : event.getChunk().getEntities()) {
                if (!(entity instanceof Item item) || !item.isValid()) continue;

                if (pipeline != null) pipeline.render(item, false);

                if (plugin.isProtocolLibEnabled()
                        && stateRepo.getHiddenVanillaItems().contains(item.getEntityId())) {
                    ItemDisplay visual = stateRepo.getActiveItemVisuals().get(item.getUniqueId());
                    
                    double ix = item.getX();
                    double iy = item.getY();
                    double iz = item.getZ();

                    for (Player p : item.getWorld().getPlayers()) {
                        double dx = p.getX() - ix;
                        double dy = p.getY() - iy;
                        double dz = p.getZ() - iz;
                        
                        if ((dx * dx + dy * dy + dz * dz) > 4096) continue;
                        
                        if (!stateRepo.getHiddenVisuals().contains(p.getUniqueId())) {
                            p.hideEntity(plugin, item);
                            if (visual != null && visual.isValid()) {
                                p.showEntity(plugin, visual);
                            }
                        }
                    }
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        var pipeline = plugin.getService(fr.skynex.lootglow.pipeline.LootRenderPipeline.class);
        if (pipeline == null) return;
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Item item) {
                pipeline.unrender(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        String worldName = event.getWorld().getName();
        List<UUID> toRemove = new ArrayList<>();
        var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
        var stateRepo = plugin.getStateRepository();
        if (trackedMgr != null) {
            for (Map.Entry<UUID, fr.skynex.lootglow.model.TrackedItem> entry : trackedMgr.getTrackedItems().entrySet()) {
                fr.skynex.lootglow.model.TrackedItem ti = entry.getValue();
                Entity testEnt = ti.label != null ? ti.label : (ti.beam != null ? ti.beam : (ti.visual != null ? ti.visual : ti.shadow));
                if (testEnt != null && testEnt.getWorld().getName().equals(worldName)) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        var activeItems = trackedMgr != null ? trackedMgr.getActiveItems() : stateRepo.getActiveItems();
        for (Map.Entry<UUID, Item> entry : activeItems.entrySet()) {
            if (entry.getValue().getWorld().getName().equals(worldName)) {
                UUID uuid = entry.getKey();
                if (!toRemove.contains(uuid)) {
                    toRemove.add(uuid);
                }
            }
        }
        var pipeline = plugin.getService(fr.skynex.lootglow.pipeline.LootRenderPipeline.class);
        if (pipeline != null) {
            for (UUID uuid : toRemove) {
                pipeline.unrender(uuid);
            }
        }
    }
}
