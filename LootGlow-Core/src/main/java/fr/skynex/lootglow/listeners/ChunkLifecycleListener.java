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
        if (!event.isNewChunk()) {
            for (Entity entity : event.getChunk().getEntities()) {
                if (entity instanceof BlockDisplay bd && bd.getPersistentDataContainer().has(plugin.getFarmingKey(), PersistentDataType.BYTE)) {
                    Block block = bd.getLocation().getBlock();
                    if (plugin.isFarmingEnabled() && plugin.getFarmingCrops().contains(block.getType())) {
                        if (block.getBlockData() instanceof Ageable age && age.getAge() == age.getMaximumAge()) {
                            plugin.relinkCropSymbol(block, bd);
                            continue;
                        }
                    }
                    bd.remove();
                    continue;
                }
                if (entity instanceof Item item) {
                    UUID uuid = item.getUniqueId();
                    ItemDisplay oldDisplay = plugin.getActiveItemVisuals().remove(uuid);
                    if (oldDisplay != null && oldDisplay.isValid()) {
                        oldDisplay.remove();
                    }
                    TextDisplay oldLabel = plugin.getActiveLabels().remove(uuid);
                    if (oldLabel != null && oldLabel.isValid()) {
                        oldLabel.remove();
                    }
                    BlockDisplay oldBeam = plugin.getActiveBeams().remove(uuid);
                    if (oldBeam != null && oldBeam.isValid()) {
                        oldBeam.getPassengers().forEach(passenger -> passenger.remove());
                        oldBeam.remove();
                    }
                    Entity oldShadow = plugin.getActiveShadows().get(uuid);
                    if (oldShadow != null) {
                        plugin.getActiveShadows().remove(uuid);
                        if (oldShadow.isValid()) oldShadow.remove();
                    }
                }
            }
        }

        FoliaScheduler.runSync(plugin, () -> {
            if (!plugin.isPluginEnabled()) return;

            for (Entity entity : event.getChunk().getEntities()) {
                if (!(entity instanceof Item item) || !item.isValid()) continue;

                plugin.getLootRenderPipeline().render(item, false);

                if (plugin.isProtocolLibEnabled()
                        && plugin.getHiddenVanillaItems().contains(item.getEntityId())) {
                    ItemDisplay visual = plugin.getActiveItemVisuals().get(item.getUniqueId());
                    
                    double ix = item.getX();
                    double iy = item.getY();
                    double iz = item.getZ();

                    for (Player p : item.getWorld().getPlayers()) {
                        double dx = p.getX() - ix;
                        double dy = p.getY() - iy;
                        double dz = p.getZ() - iz;
                        
                        if ((dx * dx + dy * dy + dz * dz) > 4096) continue;
                        
                        if (!plugin.getHiddenVisuals().contains(p.getUniqueId())) {
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
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Item item) {
                plugin.getLootRenderPipeline().unrender(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        String worldName = event.getWorld().getName();
        List<UUID> toRemove = new ArrayList<>();
        if (plugin.getTrackedItemManager() != null) {
            for (Map.Entry<UUID, fr.skynex.lootglow.model.TrackedItem> entry : plugin.getTrackedItemManager().getTrackedItems().entrySet()) {
                fr.skynex.lootglow.model.TrackedItem ti = entry.getValue();
                Entity testEnt = ti.label != null ? ti.label : (ti.beam != null ? ti.beam : (ti.visual != null ? ti.visual : ti.shadow));
                if (testEnt != null && testEnt.getWorld().getName().equals(worldName)) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        for (Map.Entry<UUID, Item> entry : plugin.getActiveItems().entrySet()) {
            if (entry.getValue().getWorld().getName().equals(worldName)) {
                UUID uuid = entry.getKey();
                if (!toRemove.contains(uuid)) {
                    toRemove.add(uuid);
                }
            }
        }
        for (UUID uuid : toRemove) {
            plugin.getLootRenderPipeline().unrender(uuid);
        }
    }
}
