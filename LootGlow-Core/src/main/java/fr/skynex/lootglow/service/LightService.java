package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Handles dynamic light block updates for glowing items in real-time.
 */
public class LightService {

    private final LootGlow plugin;
    private org.bukkit.scheduler.BukkitTask lightingTask;

    public LightService(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void stopLightingTask() {
        if (lightingTask != null) {
            lightingTask.cancel();
            lightingTask = null;
        }
    }

    public void startLightingTask(boolean isEnabled,
                                  boolean lightingEnabled,
                                  Map<UUID, Location> activeLights,
                                  Map<UUID, Item> activeItems,
                                  Map<UUID, String> itemCategoriesCache,
                                  Map<String, Integer> categoryLights,
                                  Light[] cachedLightBlockData,
                                  int interval) {

        stopLightingTask();

        lightingTask = FoliaScheduler.runTimer(plugin, () -> {
            if (!isEnabled || !lightingEnabled) return;

            activeLights.keySet().removeIf(uuid -> {
                Item ent = activeItems.get(uuid);
                if (ent == null || ent.isDead() || !ent.isValid()) {
                    Location loc = activeLights.get(uuid);
                    if (loc != null) {
                        BlockData blockData = loc.getBlock().getBlockData();
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getWorld().equals(loc.getWorld())) {
                                p.sendBlockChange(loc, blockData);
                            }
                        }
                    }
                    return true;
                }
                return false;
            });

            for (Map.Entry<UUID, Item> lightEntry : activeItems.entrySet()) {
                UUID uuid = lightEntry.getKey();
                Item item = lightEntry.getValue();
                if (item == null || !item.isValid()) continue;

                String category = itemCategoriesCache.get(uuid);
                if (category == null) continue;

                int lightLevel = categoryLights.getOrDefault(category, 0);
                if (lightLevel <= 0) continue;

                Block block = item.getLocation().getBlock();
                Location currentLoc = block.getLocation();
                Location oldLoc = activeLights.get(uuid);

                if (oldLoc != null && oldLoc.equals(currentLoc)) continue;

                if (oldLoc != null) {
                    BlockData oldBlockData = oldLoc.getBlock().getBlockData();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getWorld().equals(oldLoc.getWorld())) {
                            p.sendBlockChange(oldLoc, oldBlockData);
                        }
                    }
                }

                Material blockType = block.getType();
                if (blockType.isAir() || blockType == Material.WATER) {
                    Light lightData = cachedLightBlockData[Math.max(0, Math.min(lightLevel, 15))];
                    if (lightData != null) {
                        if (blockType == Material.WATER) {
                            Light waterloggedLight = (Light) lightData.clone();
                            waterloggedLight.setWaterlogged(true);
                            lightData = waterloggedLight;
                        }

                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getWorld().equals(currentLoc.getWorld())) {
                                p.sendBlockChange(currentLoc, lightData);
                            }
                        }
                        activeLights.put(uuid, currentLoc);
                    }
                } else {
                    activeLights.remove(uuid);
                }
            }
        }, 20L, (long) interval);
    }
}
