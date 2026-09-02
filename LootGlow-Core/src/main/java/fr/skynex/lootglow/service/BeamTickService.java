package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.managers.BeamManager;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.util.Transformation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages periodic light beam updates, animations, and rotation tasks.
 */
public class BeamTickService {

    private final LootGlow plugin;

    public BeamTickService(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void tickBeamAnimation(float angle,
                                  boolean beamsEnabled,
                                  boolean beamsAnimate,
                                  Map<UUID, BlockDisplay> activeBeams,
                                  Set<UUID> globallyVisibleEntities,
                                  Map<UUID, BeamManager.BeamConfig> activeBeamConfigs,
                                  float beamHeight,
                                  float beamWidth,
                                  Map<UUID, Particle> itemParticlesCache) {

        if (!beamsEnabled || !beamsAnimate) return;

        org.joml.Quaternionf rot = new org.joml.Quaternionf().rotationY(angle);
        float scalePulse = (float) (1.0 + Math.sin(angle * 2) * 0.15);
        int tick = (int) (angle * 20);

        for (Map.Entry<UUID, BlockDisplay> entry : activeBeams.entrySet()) {
            BlockDisplay beam = entry.getValue();
            if (beam == null || !beam.isValid()) continue;
            if (!globallyVisibleEntities.contains(entry.getKey())) continue;

            BeamManager.BeamConfig config = activeBeamConfigs.get(entry.getKey());
            boolean shouldAnimate = (config != null) ? config.animate : beamsAnimate;
            boolean shouldPulse = (config != null) ? config.pulse : true;

            FoliaScheduler.runAtEntity(plugin, beam, () -> {
                if (!beam.isValid()) return;
                Transformation trans = beam.getTransformation();
                if (shouldAnimate) trans.getLeftRotation().set(rot);

                float bH = (config != null) ? config.height : beamHeight;
                float bW = (config != null) ? config.width : beamWidth;
                float currentWidth = shouldPulse ? bW * scalePulse : bW;

                trans.getScale().set(currentWidth, bH, currentWidth);
                trans.getTranslation().set(-currentWidth / 2, 0, -currentWidth / 2);
                beam.setTransformation(trans);
                beam.setInterpolationDuration(2);
                beam.setInterpolationDelay(0);

                for (Entity pass : beam.getPassengers()) {
                    if (pass instanceof BlockDisplay bd && bd.isValid()) {
                        Transformation cTrans = bd.getTransformation();
                        cTrans.getLeftRotation().set(rot);
                        float cWidth = beamWidth * 0.4f * scalePulse;
                        cTrans.getScale().set(cWidth, beamHeight, cWidth);
                        cTrans.getTranslation().set(-cWidth / 2, 0, -cWidth / 2);
                        bd.setTransformation(cTrans);
                        bd.setInterpolationDuration(2);
                        bd.setInterpolationDelay(0);
                    }
                }
            });

            if (tick % 2 == 0) {
                UUID itemUuid = entry.getKey();
                Particle part = itemParticlesCache.get(itemUuid);
                if (part != null) {
                    double heightOffset = (angle * 5) % beamHeight;
                    double maxDistSq = plugin.getConfigManager() != null ? plugin.getConfigManager().getLodPartDistSq() : 1024.0;
                    FoliaScheduler.runAtEntity(plugin, beam, () -> {
                        if (!beam.isValid()) return;
                        double bx = beam.getX();
                        double by = beam.getY() + heightOffset;
                        double bz = beam.getZ();
                        org.bukkit.World world = beam.getWorld();

                        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                            if (plugin.getHiddenVisuals().contains(p.getUniqueId()) || !p.getWorld().equals(world)) continue;
                            double dx = p.getX() - bx;
                            double dy = p.getY() - by;
                            double dz = p.getZ() - bz;
                            if ((dx * dx + dy * dy + dz * dz) <= maxDistSq) {
                                p.spawnParticle(part, bx, by, bz, 1, 0.05, 0.05, 0.05, 0.01);
                            }
                        }
                    });
                }
            }
        }
    }
}
