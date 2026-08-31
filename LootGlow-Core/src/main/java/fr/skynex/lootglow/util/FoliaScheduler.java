package fr.skynex.lootglow.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.util.concurrent.TimeUnit;

public class FoliaScheduler {
    private static final boolean IS_FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            // Non-Folia
        }
        IS_FOLIA = folia;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    private static Object getAsyncScheduler() throws Exception {
        return Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
    }

    private static Object getGlobalRegionScheduler() throws Exception {
        return Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
    }

    public static org.bukkit.scheduler.BukkitTask runAsync(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA) {
            try {
                Object scheduler = getAsyncScheduler();
                java.util.function.Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = scheduler.getClass()
                        .getMethod("runNow", Plugin.class, java.util.function.Consumer.class)
                        .invoke(scheduler, plugin, consumer);
                return new FoliaBukkitTask(scheduledTask, plugin);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia AsyncScheduler error, falling back: " + e.getMessage());
            }
        }
        return new FoliaBukkitTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    public static org.bukkit.scheduler.BukkitTask runSync(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA) {
            try {
                Object scheduler = getGlobalRegionScheduler();
                java.util.function.Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = scheduler.getClass()
                        .getMethod("run", Plugin.class, java.util.function.Consumer.class)
                        .invoke(scheduler, plugin, consumer);
                return new FoliaBukkitTask(scheduledTask, plugin);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia GlobalRegionScheduler error, falling back: " + e.getMessage());
            }
        }
        return new FoliaBukkitTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static org.bukkit.scheduler.BukkitTask runLater(Plugin plugin, Runnable runnable, long ticks) {
        if (IS_FOLIA) {
            try {
                Object scheduler = getGlobalRegionScheduler();
                java.util.function.Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = scheduler.getClass()
                        .getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, long.class)
                        .invoke(scheduler, plugin, consumer, ticks);
                return new FoliaBukkitTask(scheduledTask, plugin);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia GlobalRegionScheduler error, falling back: " + e.getMessage());
            }
        }
        return new FoliaBukkitTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks));
    }

    public static org.bukkit.scheduler.BukkitTask runLaterAsync(Plugin plugin, Runnable runnable, long ticks) {
        if (IS_FOLIA) {
            try {
                Object scheduler = getAsyncScheduler();
                java.util.function.Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = scheduler.getClass()
                        .getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, long.class, TimeUnit.class)
                        .invoke(scheduler, plugin, consumer, ticks * 50L, TimeUnit.MILLISECONDS);
                return new FoliaBukkitTask(scheduledTask, plugin);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia AsyncScheduler error, falling back: " + e.getMessage());
            }
        }
        return new FoliaBukkitTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, ticks));
    }

    public static org.bukkit.scheduler.BukkitTask runTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (IS_FOLIA) {
            try {
                Object scheduler = getGlobalRegionScheduler();
                java.util.function.Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = scheduler.getClass()
                        .getMethod("runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class)
                        .invoke(scheduler, plugin, consumer, delayTicks, periodTicks);
                return new FoliaBukkitTask(scheduledTask, plugin);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia GlobalRegionScheduler error, falling back: " + e.getMessage());
            }
        }
        return new FoliaBukkitTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks));
    }

    public static org.bukkit.scheduler.BukkitTask runTimerAsync(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (IS_FOLIA) {
            try {
                Object scheduler = getAsyncScheduler();
                java.util.function.Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = scheduler.getClass()
                        .getMethod("runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class, TimeUnit.class)
                        .invoke(scheduler, plugin, consumer, delayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS);
                return new FoliaBukkitTask(scheduledTask, plugin);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia AsyncScheduler error, falling back: " + e.getMessage());
            }
        }
        return new FoliaBukkitTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks));
    }

    public static void runAtEntity(Plugin plugin, org.bukkit.entity.Entity entity, Runnable runnable) {
        if (entity == null) return;
        if (IS_FOLIA) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                java.util.function.Consumer<Object> consumer = task -> runnable.run();
                scheduler.getClass()
                        .getMethod("run", Plugin.class, java.util.function.Consumer.class, Runnable.class)
                        .invoke(scheduler, plugin, consumer, null);
                return;
            } catch (Exception ignored) {}
        }
        runnable.run();
    }

    public static void removeEntity(Plugin plugin, org.bukkit.entity.Entity entity) {
        if (entity == null) return;
        runAtEntity(plugin, entity, () -> {
            try {
                if (entity.isValid()) {
                    entity.remove();
                }
            } catch (Exception ignored) {}
        });
    }

    public static void cancelTasks(Plugin plugin) {
        if (!IS_FOLIA) {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }

    public static class FoliaBukkitTask implements org.bukkit.scheduler.BukkitTask {
        private final Object foliaTask;
        private final org.bukkit.scheduler.BukkitTask spigotTask;
        private final Plugin plugin;

        public FoliaBukkitTask(Object foliaTask, Plugin plugin) {
            this.foliaTask = foliaTask;
            this.spigotTask = null;
            this.plugin = plugin;
        }

        public FoliaBukkitTask(org.bukkit.scheduler.BukkitTask spigotTask) {
            this.foliaTask = null;
            this.spigotTask = spigotTask;
            this.plugin = spigotTask != null ? spigotTask.getOwner() : null;
        }

        @Override
        public int getTaskId() {
            if (spigotTask != null) return spigotTask.getTaskId();
            return -1;
        }

        @Override
        public Plugin getOwner() {
            if (spigotTask != null) return spigotTask.getOwner();
            return plugin;
        }

        @Override
        public boolean isSync() {
            if (spigotTask != null) return spigotTask.isSync();
            return false;
        }

        @Override
        public boolean isCancelled() {
            if (spigotTask != null) return spigotTask.isCancelled();
            if (foliaTask != null) {
                try {
                    Object res = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask").getMethod("isCancelled").invoke(foliaTask);
                    if (res instanceof Boolean) return (Boolean) res;
                } catch (Exception ignored) {}
            }
            return false;
        }

        @Override
        public void cancel() {
            if (spigotTask != null) {
                spigotTask.cancel();
            } else if (foliaTask != null) {
                try {
                    Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask").getMethod("cancel").invoke(foliaTask);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
