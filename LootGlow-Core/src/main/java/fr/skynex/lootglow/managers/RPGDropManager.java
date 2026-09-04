package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages RPG drop visual effects, native entity shadows, pop jump animations, bouncing physics, and aspiration animations.
 */
public class RPGDropManager {

    public static class VisualAnimation {
        public ItemDisplay display;
        public Player target;
        public double scale = 1.0;
        public volatile int ticks = 0;

        public VisualAnimation(ItemDisplay display, Player target) {
            this.display = display;
            this.target = target;
        }
    }

    private final LootGlow plugin;
    private final Map<UUID, Display> activeShadows = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> bounceCounts = new HashMap<>();
    private final Set<UUID> recentlyBounced = ConcurrentHashMap.newKeySet();
    private final Map<UUID, VisualAnimation> flyingVisuals = new HashMap<>();

    public RPGDropManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, Display> getActiveShadows() {
        return activeShadows;
    }

    public Map<UUID, Integer> getBounceCounts() {
        return bounceCounts;
    }

    public Set<UUID> getRecentlyBounced() {
        return recentlyBounced;
    }

    public Map<UUID, VisualAnimation> getFlyingVisuals() {
        return flyingVisuals;
    }

    public void spawnShadow(Item item) {
        if (item == null || !item.isValid()) return;

        Display existing = activeShadows.get(item.getUniqueId());
        if (existing != null) {
            if (existing.isValid()) return;
            activeShadows.remove(item.getUniqueId());
        }

        Location loc = item.getLocation();
        BlockDisplay shadow = item.getWorld().spawn(loc, BlockDisplay.class, ent -> {
            ent.setShadowRadius(plugin.getShadowScale() * 0.8f);
            ent.setShadowStrength(1.0f);
            ent.setPersistent(false);
        });

        activeShadows.put(item.getUniqueId(), shadow);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(item.getWorld())) continue;
            if (plugin.getHiddenVisuals().contains(p.getUniqueId())
                    || p.getLocation().distanceSquared(item.getLocation()) >= plugin.getLodHoloDistSq()) {
                p.hideEntity(plugin, shadow);
            } else {
                plugin.getVisibleEntities().computeIfAbsent(p.getUniqueId(), k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(shadow.getUniqueId());
            }
        }
    }

    public void removeShadow(UUID uuid) {
        Display d = activeShadows.remove(uuid);
        if (d != null && d.isValid()) {
            d.remove();
        }
    }

    public void triggerPopAnimation(Item item, double jumpVelocity) {
        if (item == null || !item.isValid()) return;
        Vector velocity = item.getVelocity();
        velocity.setY(Math.max(velocity.getY(), jumpVelocity));
        item.setVelocity(velocity);
    }

    public void tickBouncing(boolean bouncingEnabled, Map<UUID, Item> activeItems, Set<Material> bouncingBlockedBlocks, boolean bouncingOnlyOnSnow, int maxBounces, double jumpForce, double bounceDamping) {
        if (!bouncingEnabled) return;

        Iterator<Map.Entry<UUID, Integer>> it = bounceCounts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            UUID uuid = entry.getKey();
            int count = entry.getValue();

            Item item = activeItems.get(uuid);
            if (item == null || !item.isValid() || item.isDead()) {
                it.remove();
                recentlyBounced.remove(uuid);
                continue;
            }

            FoliaScheduler.runAtEntity(plugin, item, () -> {
                if (!item.isValid() || item.isDead()) return;

                if (item.isOnGround()) {
                    if (!recentlyBounced.contains(uuid)) {
                        org.bukkit.block.Block blockAt = item.getLocation().getBlock();
                        org.bukkit.block.Block blockBelow = blockAt.getRelative(org.bukkit.block.BlockFace.DOWN);
                        Material blockMat = blockBelow.getType();
                        Material atMat = blockAt.getType();
                        boolean isSnowBlock = blockMat == Material.SNOW || blockMat == Material.SNOW_BLOCK || blockMat == Material.POWDER_SNOW
                                || atMat == Material.SNOW || atMat == Material.SNOW_BLOCK || atMat == Material.POWDER_SNOW;
                        boolean isBlocked = bouncingBlockedBlocks.contains(blockMat) || bouncingBlockedBlocks.contains(atMat) || (bouncingOnlyOnSnow && !isSnowBlock);

                        if (!isBlocked && count < maxBounces) {
                            double force = jumpForce * Math.pow(bounceDamping, count + 1);
                            if (force > 0.05) {
                                Vector vel = item.getVelocity();
                                item.setVelocity(vel.setY(force));
                                bounceCounts.put(uuid, count + 1);
                                recentlyBounced.add(uuid);
                            }
                        }
                    }
                } else {
                    recentlyBounced.remove(uuid);
                }
            });
        }
    }

    public void playAspirationAnimation(Item item, Player player, Map<UUID, ItemDisplay> activeItemVisuals, boolean aspirationEnabled) {
        if (!aspirationEnabled || item == null) return;
        UUID uuid = item.getUniqueId();
        ItemDisplay visual = activeItemVisuals.remove(uuid);
        if (visual != null && visual.isValid()) {
            flyingVisuals.put(uuid, new VisualAnimation(visual, player));
        }
    }

    public void tickAspiration(boolean aspirationEnabled, double aspirationSpeed) {
        if (!aspirationEnabled || flyingVisuals.isEmpty()) return;

        Iterator<Map.Entry<UUID, VisualAnimation>> it = flyingVisuals.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, VisualAnimation> entry = it.next();
            VisualAnimation anim = entry.getValue();

            if (anim.display == null || !anim.display.isValid() || anim.target == null || !anim.target.isOnline() || anim.ticks > 20) {
                if (anim.display != null && anim.display.isValid()) {
                    FoliaScheduler.removeEntity(plugin, anim.display);
                }
                it.remove();
                continue;
            }

            FoliaScheduler.runAtEntity(plugin, anim.display, () -> {
                if (!anim.target.isOnline() || !anim.display.isValid()) {
                    if (anim.display.isValid()) anim.display.remove();
                    return;
                }

                Location targetLoc = anim.target.getLocation().add(0, anim.target.getEyeHeight() - 0.3, 0);
                Location displayLoc = anim.display.getLocation();

                double distSq = displayLoc.distanceSquared(targetLoc);
                if (distSq < 0.09 || anim.ticks > 20) {
                    anim.display.remove();
                    return;
                }

                double dist = Math.sqrt(distSq);
                if (dist < 0.01) {
                    anim.display.remove();
                    return;
                }

                double dx = targetLoc.getX() - displayLoc.getX();
                double dy = targetLoc.getY() - displayLoc.getY();
                double dz = targetLoc.getZ() - displayLoc.getZ();
                double speed = aspirationSpeed + (anim.ticks * 0.02);
                Location newLoc = displayLoc.clone().add((dx / dist) * speed, (dy / dist) * speed, (dz / dist) * speed);

                anim.scale = Math.max(0.1, anim.scale - 0.05);
                Transformation trans = anim.display.getTransformation();
                trans.getScale().set((float) anim.scale);
                anim.display.setTransformation(trans);

                newLoc.setYaw(displayLoc.getYaw() + 20);
                anim.display.teleport(newLoc);
                anim.ticks++;
            });
        }
    }

    public void clearAll() {
        activeShadows.values().forEach(d -> {
            if (d != null && d.isValid()) d.remove();
        });
        activeShadows.clear();
        bounceCounts.clear();
        recentlyBounced.clear();
        flyingVisuals.clear();
    }
}
