package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.model.TrackedItem;
import fr.skynex.lootglow.managers.SurfaceAlignmentManager.SurfaceState;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles per-tick global synchronization and Display entity position tracking following parent items.
 */
public class ItemPhysicsService {

    private final LootGlow plugin;
    private int globalSyncTick = 0;

    public ItemPhysicsService(LootGlow plugin) {
        this.plugin = plugin;
    }

    /**
     * Global sync tick: repositions all Display entities to follow their parent Item. Runs every tick.
     */
    public void tickGlobalSync(boolean isEnabled,
                               Map<UUID, Item> activeItems,
                               Map<UUID, TrackedItem> trackedItems,
                               float rpgBlockScale,
                               float rpgItemScale,
                               Material bagMaterial,
                               Map<UUID, Integer> groupLeaders,
                               double holoOffset,
                               float shadowScale,
                               float rpgRotation) {
        if (!isEnabled)
            return;

        globalSyncTick++;

        List<UUID> staleEntries = null;

        for (Map.Entry<UUID, Item> entry : activeItems.entrySet()) {
            UUID itemUuid = entry.getKey();
            Item item = entry.getValue();

            if (item == null || !item.isValid()) {
                if (staleEntries == null)
                    staleEntries = new java.util.ArrayList<>();
                staleEntries.add(itemUuid);
                continue;
            }

            TrackedItem ti = trackedItems.get(itemUuid);
            if (ti == null) continue;
            ItemDisplay visual = ti.visual;
            TextDisplay label = ti.label;
            BlockDisplay beam = ti.beam;
            org.bukkit.entity.Display shadow = ti.shadow;

            Location itemLoc = item.getLocation();
            double itemX = itemLoc.getX();
            double itemY = itemLoc.getY();
            double itemZ = itemLoc.getZ();

            SurfaceState state = plugin.getSurfaceAlignmentManager() != null ? plugin.getSurfaceAlignmentManager().getSurfaceStates().get(itemUuid) : null;
            boolean itemActuallyMoved = true;
            if (state != null) {
                double dx = state.lastItemX - itemX;
                double dy = state.lastItemY - itemY;
                double dz = state.lastItemZ - itemZ;
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > 0.0001) {
                    if (plugin.getSurfaceAlignmentManager() != null) {
                        plugin.getSurfaceAlignmentManager().getSurfaceStates().remove(itemUuid);
                    }
                } else {
                    itemActuallyMoved = false;
                }
            }

            if (itemActuallyMoved) {
                if ((globalSyncTick - ti.lastRayTraceTick) >= 4) {
                    plugin.updateSurfaceAlignment(item);
                    ti.lastRayTraceTick = globalSyncTick;
                }
                if (plugin.getSurfaceAlignmentManager() != null) {
                    state = plugin.getSurfaceAlignmentManager().getSurfaceStates().get(itemUuid);
                }
            }

            double targetSurfaceY = state != null ? state.y : itemY;
            Float yaw = state != null ? state.yaw : null;
            Float pitch = state != null ? state.pitch : null;

            double baseWeight = 0.02;
            Material itemMat = item.getItemStack().getType();
            boolean isBlockItem = plugin.isUprightItem(itemMat);
            double visualYOffset = baseWeight + (isBlockItem ? (rpgBlockScale / 2.0) : 0.0);
            if (visual != null && visual.isValid() && visual.getItemStack() != null) {
                Material vMat = visual.getItemStack().getType();
                if (vMat == Material.PLAYER_HEAD) {
                    visualYOffset = 0.15;
                } else if (vMat == Material.BUNDLE) {
                    visualYOffset = 0.32;
                } else if (vMat == Material.CHEST || vMat == Material.TRAPPED_CHEST || vMat == Material.ENDER_CHEST) {
                    visualYOffset = 0.20;
                }
            }
            Entity representative = (visual != null) ? (Entity) visual : (Entity) label;

            boolean moved = false;
            if (itemActuallyMoved && representative != null && representative.isValid()) {
                Location repLoc = representative.getLocation();
                double dx = itemX - repLoc.getX();
                double dy = (targetSurfaceY + visualYOffset) - repLoc.getY();
                double dz = itemZ - repLoc.getZ();
                moved = (dx * dx + dy * dy + dz * dz) > 0.0001;
            }

            if (moved) {
                if (visual != null && visual.isValid()) {
                    Location teleportLoc = new Location(itemLoc.getWorld(), itemX, targetSurfaceY + visualYOffset, itemZ);
                    if (yaw != null) teleportLoc.setYaw(yaw);
                    if (pitch != null) teleportLoc.setPitch(pitch);
                    FoliaScheduler.runAtEntity(plugin, visual, () -> {
                        if (visual.isValid()) {
                            visual.setTeleportDuration(1);
                            visual.teleport(teleportLoc);
                        }
                    });
                }

                if (label != null && label.isValid()) {
                    Location labelLoc = new Location(itemLoc.getWorld(), itemX, targetSurfaceY + visualYOffset + holoOffset, itemZ);
                    FoliaScheduler.runAtEntity(plugin, label, () -> {
                        if (label.isValid()) {
                            label.setTeleportDuration(1);
                            label.teleport(labelLoc);
                        }
                    });
                }
            }

            // Always sync beam and shadow if item moved
            if (itemActuallyMoved) {
                if (beam != null && beam.isValid()) {
                    Location beamTarget = new Location(itemLoc.getWorld(), itemX, targetSurfaceY + baseWeight, itemZ);
                    FoliaScheduler.runAtEntity(plugin, beam, () -> {
                        if (beam.isValid() && beam.getLocation().distanceSquared(beamTarget) > 0.0001) {
                            beam.setTeleportDuration(1);
                            beam.teleport(beamTarget);
                        }
                    });
                }

                if (shadow != null && shadow.isValid()) {
                    if (item.isOnGround()) {
                        double height = itemY - targetSurfaceY;
                        float radiusFactor = (float) Math.max(0.4, 1.0 - (height * 0.3));
                        float baseRadius = shadowScale * 0.8f;
                        boolean isBlock = itemMat.isBlock();
                        if (isBlock)
                            baseRadius *= 1.4f;

                        float scale = isBlock ? rpgBlockScale : rpgItemScale;
                        float targetRadius = baseRadius * radiusFactor * (scale / 0.8f);
                        float targetStrength = (float) Math.max(0.2, 1.0 - (height * 0.5));

                        Location shadowTarget = new Location(itemLoc.getWorld(), itemX, targetSurfaceY, itemZ);
                        FoliaScheduler.runAtEntity(plugin, shadow, () -> {
                            if (shadow.isValid()) {
                                shadow.setShadowRadius(targetRadius);
                                shadow.setShadowStrength(targetStrength);
                                if (shadow.getLocation().distanceSquared(shadowTarget) > 0.0001) {
                                    shadow.setTeleportDuration(1);
                                    shadow.teleport(shadowTarget);
                                }
                            }
                        });
                    }
                }
            }

            // Physics: Water handling & animations
            if (itemActuallyMoved && visual != null && visual.isValid()) {
                Material mat = item.getItemStack().getType();
                boolean isFish = plugin.isFishItem(mat);
                boolean currentlyInWater = item.isInWater();

                if (currentlyInWater) {
                    if (plugin.getSurfaceAlignmentManager() != null) {
                        plugin.getSurfaceAlignmentManager().getWaterLogCache().add(itemUuid);
                    }
                    if (isFish) {
                        FoliaScheduler.runAtEntity(plugin, visual, () -> {
                            if (visual.isValid()) {
                                Location vLoc = visual.getLocation();
                                vLoc.setYaw(vLoc.getYaw() + 3.0f);
                                visual.teleport(vLoc);
                            }
                        });
                    }
                } else {
                    boolean removed = plugin.getSurfaceAlignmentManager() != null && plugin.getSurfaceAlignmentManager().getWaterLogCache().remove(itemUuid);
                    if (removed) {
                        boolean isLeader = groupLeaders.containsKey(itemUuid);
                        if (!isLeader) {
                            boolean isCustom = plugin.isCustomItem(item.getItemStack());
                            boolean isUpright = plugin.isUprightItem(mat);
                            float targetRotX = (isCustom || isUpright) ? 0f : rpgRotation;
                            FoliaScheduler.runAtEntity(plugin, visual, () -> {
                                if (visual.isValid()) {
                                    org.bukkit.util.Transformation t = visual.getTransformation();
                                    t.getLeftRotation().set(new org.joml.Quaternionf().rotationX(targetRotX));
                                    visual.setTransformation(t);
                                }
                            });
                        }
                    }
                }
            }
        }

        if (staleEntries != null) {
            for (UUID staleUuid : staleEntries) {
                plugin.removeGlow(staleUuid);
            }
        }
    }
}
