package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.registry.ServiceRegistry;
import fr.skynex.lootglow.state.LootStateRepository;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages recurring scheduled tasks, tick loops, and background animations.
 */
public class PluginTickManager {

    private final LootGlow plugin;
    private final ServiceRegistry serviceRegistry;
    private final LootStateRepository stateRepository;
    private BukkitTask globalSyncTask;
    private BukkitTask particleAnimationTask;
    private BukkitTask magnetTask;
    private BukkitTask unifiedTickTask;
    private long globalSyncTick = 0;

    public PluginTickManager(LootGlow plugin) {
        this(plugin, plugin.getServiceRegistry(), plugin.getStateRepository());
    }

    public PluginTickManager(LootGlow plugin, ServiceRegistry serviceRegistry, LootStateRepository stateRepository) {
        this.plugin = plugin;
        this.serviceRegistry = serviceRegistry;
        this.stateRepository = stateRepository;
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

    @FunctionalInterface
    public interface FloatConsumer {
        void accept(float value);
    }

    public void startUnifiedTickTask(Runnable syncRunnable, Runnable bounceRunnable, Runnable aspirationRunnable,
            Runnable magnetRunnable, FloatConsumer farmAnimConsumer,
            FloatConsumer beamAnimConsumer) {
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
                if (!plugin.isEnabled())
                    return;

                // --- Every tick (1L) ---
                if (syncRunnable != null)
                    syncRunnable.run();
                if (bounceRunnable != null)
                    bounceRunnable.run();
                if (aspirationRunnable != null)
                    aspirationRunnable.run();

                // --- Every 2 ticks ---
                if (unifiedTick % 2 == 0) {
                    if (magnetRunnable != null)
                        magnetRunnable.run();
                    farmAngle = (farmAngle + 0.1f) % TWO_PI;
                    if (farmAnimConsumer != null)
                        farmAnimConsumer.accept(farmAngle);
                    beamAngle = (beamAngle + 0.1f) % TWO_PI;
                    if (beamAnimConsumer != null)
                        beamAnimConsumer.accept(beamAngle);
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

    public void startUnifiedTickTask() {
        startUnifiedTickTask(
                this::tickGlobalSync,
                this::tickBouncing,
                this::tickAspiration,
                this::tickMagnet,
                this::tickFarmingAnimation,
                this::tickBeamAnimation
        );
    }

    public void tickGlobalSync() {
        var pipeline = serviceRegistry.get(fr.skynex.lootglow.pipeline.LootRenderPipeline.class);
        var physSvc = serviceRegistry.get(fr.skynex.lootglow.service.ItemPhysicsService.class);
        var cfgMgr = serviceRegistry.get(fr.skynex.lootglow.config.LootGlowConfigManager.class);
        if (pipeline != null) {
            pipeline.tickSync();
        } else if (physSvc != null && cfgMgr != null) {
            physSvc.tickGlobalSync(plugin.isPluginEnabled(), stateRepository.getActiveItems(), stateRepository.getTrackedItems(), cfgMgr.getRpgBlockScale(), cfgMgr.getRpgItemScale(), cfgMgr.getBagMaterial(), stateRepository.getGroupLeaders(), cfgMgr.getHoloOffset(), cfgMgr.getShadowScale(), cfgMgr.getRpgRotation());
        }
    }

    public void tickBouncing() {
        var rpgMgr = serviceRegistry.get(fr.skynex.lootglow.managers.RPGDropManager.class);
        var cfgMgr = serviceRegistry.get(fr.skynex.lootglow.config.LootGlowConfigManager.class);
        if (rpgMgr != null && cfgMgr != null) {
            rpgMgr.tickBouncing(cfgMgr.isBouncingEnabled(), stateRepository.getActiveItems(), cfgMgr.getBouncingBlockedBlocks(), cfgMgr.isBouncingOnlyOnSnow(), cfgMgr.getMaxBounces(), cfgMgr.getJumpForce(), cfgMgr.getBounceDamping());
        }
    }

    public void tickAspiration() {
        var rpgMgr = serviceRegistry.get(fr.skynex.lootglow.managers.RPGDropManager.class);
        var cfgMgr = serviceRegistry.get(fr.skynex.lootglow.config.LootGlowConfigManager.class);
        if (rpgMgr != null && cfgMgr != null) {
            rpgMgr.tickAspiration(cfgMgr.isAspirationEnabled(), cfgMgr.getAspirationSpeed());
        }
    }

    public void tickMagnet() {
        var magMgr = serviceRegistry.get(fr.skynex.lootglow.managers.ItemMagnetManager.class);
        var cfgMgr = serviceRegistry.get(fr.skynex.lootglow.config.LootGlowConfigManager.class);
        if (magMgr != null && cfgMgr != null) {
            magMgr.tickMagnet(cfgMgr.isMagnetEnabled(), cfgMgr.getMagnetDistance(), cfgMgr.getMagnetPermission(), cfgMgr.getMagnetCategories(),
                    cfgMgr.isMagnetEnableForGroups(), stateRepository.getGroupLeaders(), stateRepository.getGroupMembers(), stateRepository.getGroupedItems(), stateRepository.getItemCategoriesCache(),
                    cfgMgr.isProtectionEnabled(), cfgMgr.getProtectionDuration(), stateRepository.getItemSpawnTimes());
        }
    }

    public void tickBeamAnimation(float angle) {
        var beamTickSvc = serviceRegistry.get(fr.skynex.lootglow.service.BeamTickService.class);
        var beamMgr = serviceRegistry.get(BeamManager.class);
        var cfgMgr = serviceRegistry.get(fr.skynex.lootglow.config.LootGlowConfigManager.class);
        if (beamTickSvc != null && beamMgr != null && cfgMgr != null) {
            beamTickSvc.tickBeamAnimation(angle, cfgMgr.isBeamsEnabled(), cfgMgr.isBeamsAnimate(), stateRepository.getActiveBeams(), stateRepository.getGloballyVisibleEntities(),
                    beamMgr.getActiveBeamConfigs(), cfgMgr.getBeamHeight(), cfgMgr.getBeamWidth(), stateRepository.getItemParticlesCache());
        }
    }

    public void tickFarmingAnimation(float angle) {
        var farmMgr = serviceRegistry.get(fr.skynex.lootglow.managers.FarmingManager.class);
        var cfgMgr = serviceRegistry.get(fr.skynex.lootglow.config.LootGlowConfigManager.class);
        if (farmMgr != null && cfgMgr != null) {
            farmMgr.tickFarmingAnimation(angle, cfgMgr.isFarmingEnabled(), cfgMgr.isFarmingAnimation(), stateRepository.getGloballyVisibleEntities());
        }
    }
}
