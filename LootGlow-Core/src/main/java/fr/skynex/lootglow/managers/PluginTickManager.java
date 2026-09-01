package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages recurring scheduled tasks, tick loops, and background animations.
 */
public class PluginTickManager {

    private final LootGlow plugin;
    private BukkitTask globalSyncTask;
    private BukkitTask particleAnimationTask;
    private BukkitTask magnetTask;
    private BukkitTask unifiedTickTask;
    private long globalSyncTick = 0;

    public PluginTickManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public long getGlobalSyncTick() {
        return globalSyncTick;
    }

    public void incrementGlobalSyncTick() {
        globalSyncTick++;
    }

    public BukkitTask getGlobalSyncTask() {
        return globalSyncTask;
    }

    public void setGlobalSyncTask(BukkitTask globalSyncTask) {
        this.globalSyncTask = globalSyncTask;
    }

    public BukkitTask getParticleAnimationTask() {
        return particleAnimationTask;
    }

    public void setParticleAnimationTask(BukkitTask particleAnimationTask) {
        this.particleAnimationTask = particleAnimationTask;
    }

    public BukkitTask getMagnetTask() {
        return magnetTask;
    }

    public void setMagnetTask(BukkitTask magnetTask) {
        this.magnetTask = magnetTask;
    }

    public void startTasks(Runnable syncRunnable, Runnable particleRunnable, Runnable magnetRunnable) {
        cancelTasks();

        if (syncRunnable != null) {
            globalSyncTask = FoliaScheduler.runTimer(plugin, () -> {
                incrementGlobalSyncTick();
                syncRunnable.run();
            }, 1L, 1L);
        }

        if (particleRunnable != null) {
            particleAnimationTask = FoliaScheduler.runTimer(plugin, particleRunnable, 2L, 2L);
        }

        if (magnetRunnable != null) {
            magnetTask = FoliaScheduler.runTimer(plugin, magnetRunnable, 5L, 5L);
        }
    }

    public void startUnifiedTickTask(Runnable syncRunnable, Runnable bounceRunnable, Runnable aspirationRunnable,
                                     Runnable magnetRunnable, java.util.function.Consumer<Float> farmAnimConsumer,
                                     java.util.function.Consumer<Float> beamAnimConsumer) {
        if (unifiedTickTask != null) {
            unifiedTickTask.cancel();
            unifiedTickTask = null;
        }

        unifiedTickTask = FoliaScheduler.runTimer(plugin, new Runnable() {
            private int unifiedTick = 0;
            private float beamAngle = 0f;
            private float farmAngle = 0f;
            private static final float TWO_PI = (float) (Math.PI * 2);

            @Override
            public void run() {
                unifiedTick++;
                if (!plugin.isEnabled()) return;

                // --- Every tick (1L) ---
                if (syncRunnable != null) syncRunnable.run();
                if (bounceRunnable != null) bounceRunnable.run();
                if (aspirationRunnable != null) aspirationRunnable.run();

                // --- Every 2 ticks ---
                if (unifiedTick % 2 == 0) {
                    if (magnetRunnable != null) magnetRunnable.run();
                    farmAngle = (farmAngle + 0.1f) % TWO_PI;
                    if (farmAnimConsumer != null) farmAnimConsumer.accept(farmAngle);
                    beamAngle = (beamAngle + 0.1f) % TWO_PI;
                    if (beamAnimConsumer != null) beamAnimConsumer.accept(beamAngle);
                }
            }
        }, 1L, 1L);
    }

    public void cancelTasks() {
        if (unifiedTickTask != null) {
            unifiedTickTask.cancel();
            unifiedTickTask = null;
        }
        if (globalSyncTask != null) {
            globalSyncTask.cancel();
            globalSyncTask = null;
        }
        if (particleAnimationTask != null) {
            particleAnimationTask.cancel();
            particleAnimationTask = null;
        }
        if (magnetTask != null) {
            magnetTask.cancel();
            magnetTask = null;
        }
    }
}
