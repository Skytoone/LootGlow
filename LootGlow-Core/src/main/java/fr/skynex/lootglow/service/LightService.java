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
                        int columnHeight = plugin.getConfigManager().getLightColumnHeight();
                        for (int h = 0; h < columnHeight; h++) {
                            Location restoreLoc = loc.clone().add(0, h, 0);
                            BlockData blockData = restoreLoc.getBlock().getBlockData();
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (p.getWorld().equals(restoreLoc.getWorld())) {
                                    p.sendBlockChange(restoreLoc, blockData);
                                }
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

                int columnHeight = plugin.getConfigManager().getLightColumnHeight();
                if (oldLoc != null) {
                    for (int h = 0; h < columnHeight; h++) {
                        Location restoreLoc = oldLoc.clone().add(0, h, 0);
                        BlockData oldBlockData = restoreLoc.getBlock().getBlockData();
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getWorld().equals(restoreLoc.getWorld())) {
                                p.sendBlockChange(restoreLoc, oldBlockData);
                            }
                        }
                    }
                }

                boolean placedAny = false;
                for (int h = 0; h < columnHeight; h++) {
                    Block targetBlock = block.getRelative(0, h, 0);
                    Material blockType = targetBlock.getType();
                    if (blockType.isAir() || blockType == Material.WATER) {
                        int currentLevel = Math.max(1, lightLevel - (h * 2));
                        Light lightData = cachedLightBlockData[Math.max(0, Math.min(currentLevel, 15))];
                        if (lightData != null) {
                            if (blockType == Material.WATER) {
                                Light waterloggedLight = (Light) lightData.clone();
                                waterloggedLight.setWaterlogged(true);
                                lightData = waterloggedLight;
                            }

                            Location targetLoc = targetBlock.getLocation();
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (p.getWorld().equals(targetLoc.getWorld())) {
                                    p.sendBlockChange(targetLoc, lightData);
                                }
                            }
                            placedAny = true;
                        }
                    } else {
                        break;
                    }
                }

                if (placedAny) {
                    activeLights.put(uuid, currentLoc);
                } else {
                    activeLights.remove(uuid);
                }
            }
        }, 20L, (long) interval);
    }
}
