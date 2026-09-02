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

            double itemX = item.getX();
            double itemY = item.getY();
            double itemZ = item.getZ();

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

            double baseWeight = 0.08;
            Material itemMat = ti.itemMaterial;
            if (itemMat == null) {
                itemMat = item.getItemStack().getType();
                ti.itemMaterial = itemMat;
            }
            if (ti.isBlockItem == null) {
                ti.isBlockItem = plugin.isUprightItem(itemMat);
            }
            boolean isBlockItem = ti.isBlockItem;
            double visualYOffset = baseWeight + (isBlockItem ? (rpgBlockScale / 2.0) : 0.0);
            if (visual != null && visual.isValid()) {
                Material vMat = ti.visualMaterial;
                if (vMat == null && visual.getItemStack() != null) {
                    vMat = visual.getItemStack().getType();
                    ti.visualMaterial = vMat;
                }
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
                double dx = itemX - representative.getX();
                double dy = (targetSurfaceY + visualYOffset) - representative.getY();
                double dz = itemZ - representative.getZ();
                moved = (dx * dx + dy * dy + dz * dz) > 0.0001;
            }

            if (!moved && !itemActuallyMoved) {
                continue;
            }

            final boolean finalMoved = moved;
            final boolean finalItemActuallyMoved = itemActuallyMoved;
            final double finalTargetSurfaceY = targetSurfaceY;
            final double finalVisualYOffset = visualYOffset;
            final Material finalItemMat = itemMat;

            FoliaScheduler.runAtEntity(plugin, item, () -> {
                if (!item.isValid()) return;

                Location itemLoc = null;

                if (finalMoved) {
                    itemLoc = item.getLocation();
                    if (visual != null && visual.isValid()) {
                        itemLoc.setY(finalTargetSurfaceY + finalVisualYOffset);
                        if (yaw != null) itemLoc.setYaw(yaw);
                        if (pitch != null) itemLoc.setPitch(pitch);
                        visual.setTeleportDuration(1);
                        visual.teleport(itemLoc);
                    }

                    if (label != null && label.isValid()) {
                        if (itemLoc == null) itemLoc = item.getLocation();
                        itemLoc.setY(finalTargetSurfaceY + finalVisualYOffset + holoOffset);
                        label.setTeleportDuration(1);
                        label.teleport(itemLoc);
                    }
                }

                if (finalItemActuallyMoved) {
                    if (beam != null && beam.isValid()) {
                        double targetBeamY = finalTargetSurfaceY + baseWeight;
                        double bdx = itemX - beam.getX();
                        double bdy = targetBeamY - beam.getY();
                        double bdz = itemZ - beam.getZ();
                        if ((bdx * bdx + bdy * bdy + bdz * bdz) > 0.0001) {
                            if (itemLoc == null) itemLoc = item.getLocation();
                            itemLoc.setY(targetBeamY);
                            beam.setTeleportDuration(1);
                            beam.teleport(itemLoc);
                        }
                    }

                    if (shadow != null && shadow.isValid()) {
                        if (item.isOnGround()) {
                            double height = itemY - finalTargetSurfaceY;
                            float radiusFactor = (float) Math.max(0.4, 1.0 - (height * 0.3));
                            float baseRadius = shadowScale * 0.8f;
                            boolean isBlock = finalItemMat.isBlock();
                            if (isBlock) baseRadius *= 1.4f;

                            float scale = isBlock ? rpgBlockScale : rpgItemScale;
                            float targetRadius = baseRadius * radiusFactor * (scale / 0.8f);
                            float targetStrength = (float) Math.max(0.2, 1.0 - (height * 0.5));

                            shadow.setShadowRadius(targetRadius);
                            shadow.setShadowStrength(targetStrength);

                            double sdx = itemX - shadow.getX();
                            double sdy = finalTargetSurfaceY - shadow.getY();
                            double sdz = itemZ - shadow.getZ();
                            if ((sdx * sdx + sdy * sdy + sdz * sdz) > 0.0001) {
                                if (itemLoc == null) itemLoc = item.getLocation();
                                itemLoc.setY(finalTargetSurfaceY);
                                shadow.setTeleportDuration(1);
                                shadow.teleport(itemLoc);
                            }
                        }
                    }

                    if (visual != null && visual.isValid()) {
                        if (ti.isFishItem == null) {
                            ti.isFishItem = plugin.isFishItem(finalItemMat);
                        }
                        boolean isFish = ti.isFishItem;
                        boolean currentlyInWater = item.isInWater();

                        if (currentlyInWater) {
                            if (plugin.getSurfaceAlignmentManager() != null) {
                                plugin.getSurfaceAlignmentManager().getWaterLogCache().add(itemUuid);
                            }
                            if (isFish) {
                                Location vLoc = visual.getLocation();
                                vLoc.setYaw(vLoc.getYaw() + 3.0f);
                                visual.teleport(vLoc);
                            }
                        } else {
                            boolean removed = plugin.getSurfaceAlignmentManager() != null && plugin.getSurfaceAlignmentManager().getWaterLogCache().remove(itemUuid);
                            if (removed) {
                                boolean isLeader = groupLeaders.containsKey(itemUuid);
                                if (!isLeader) {
                                    if (ti.isCustomItem == null) {
                                        ti.isCustomItem = plugin.isCustomItem(item.getItemStack());
                                    }
                                    float targetRotX = (ti.isCustomItem || isBlockItem) ? 0f : rpgRotation;
                                    org.bukkit.util.Transformation t = visual.getTransformation();
                                    t.getLeftRotation().set(new org.joml.Quaternionf().rotationX(targetRotX));
                                    visual.setTransformation(t);
                                }
                            }
                        }
                    }
                }
            });
        }

        if (staleEntries != null) {
            for (UUID staleUuid : staleEntries) {
                plugin.removeGlow(staleUuid);
            }
        }
    }
}
