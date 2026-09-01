package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages item particle effects, custom particle assignments, particle tick loop tasks, and drop sound effects.
 */
public class ParticleAnimationManager {

    private final LootGlow plugin;
    private final Map<UUID, Particle> customParticles = new ConcurrentHashMap<>();
    private int particleTick = 0;

    public ParticleAnimationManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, Particle> getCustomParticles() {
        return customParticles;
    }

    public void setParticleEffect(Item item, Particle particle) {
        if (item == null) return;
        if (particle == null) {
            customParticles.remove(item.getUniqueId());
        } else {
            customParticles.put(item.getUniqueId(), particle);
        }
    }

    public void clearParticleEffect(Item item) {
        if (item != null) {
            customParticles.remove(item.getUniqueId());
        }
    }

    public void setDropSound(Item item, Sound sound, float volume, float pitch) {
        if (item == null || sound == null || !item.isValid()) return;
        Location loc = item.getLocation();
        item.getWorld().playSound(loc, sound, volume, pitch);
    }

    public void triggerPopAnimation(Item item, double jumpVelocity) {
        if (item == null || !item.isValid()) return;
        item.setVelocity(new org.bukkit.util.Vector(0, Math.max(0.1, jumpVelocity), 0));
        item.getWorld().spawnParticle(Particle.FIREWORK, item.getLocation(), 15, 0.2, 0.2, 0.2, 0.05);
    }

    public void setBouncingEnabled(Item item, boolean bouncing, Set<UUID> recentlyBounced) {
        if (item == null || !item.isValid() || recentlyBounced == null) return;
        if (!bouncing) {
            recentlyBounced.add(item.getUniqueId());
        } else {
            recentlyBounced.remove(item.getUniqueId());
        }
    }

    private org.bukkit.scheduler.BukkitTask particleTask;

    public void startParticleTask(boolean isEnabled,
                                  boolean particlesEnabled,
                                  double lodPartDistSq,
                                  Map<UUID, Item> activeItems,
                                  Map<UUID, Particle> itemParticlesCache,
                                  Map<UUID, String> itemCategoriesCache,
                                  Set<UUID> hiddenVisuals,
                                  Map<String, Particle.DustOptions> categoryDustOptions,
                                  Particle.DustOptions defaultDustOptions,
                                  Map<String, String> categoryAnimTypes,
                                  String particleAnimType,
                                  int particlesFrequency) {

        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }

        particleTask = FoliaScheduler.runTimer(plugin, () -> {
            if (!isEnabled || !particlesEnabled) return;

            particleTick++;
            double partDistSq = lodPartDistSq;

            record CachedItemLoc(UUID uuid, double x, double y, double z, World world, Particle particle, String category, AnimType animType) {}
            Map<World, java.util.List<CachedItemLoc>> itemsByWorldMap = new java.util.HashMap<>();
            for (Map.Entry<UUID, Item> e : activeItems.entrySet()) {
                Item item = e.getValue();
                if (item == null || item.isDead() || !item.isValid()) continue;
                Particle particle = itemParticlesCache.get(e.getKey());
                if (particle == null) continue;
                String category = itemCategoriesCache.get(e.getKey());
                Location loc = item.getLocation();
                String rawAnimType = (category != null)
                        ? categoryAnimTypes.getOrDefault(category, particleAnimType)
                        : particleAnimType;
                AnimType animType = AnimType.fromString(rawAnimType);
                CachedItemLoc cached = new CachedItemLoc(e.getKey(), loc.getX(), loc.getY(), loc.getZ(), item.getWorld(), particle, category, animType);
                itemsByWorldMap.computeIfAbsent(item.getWorld(), w -> new java.util.ArrayList<>()).add(cached);
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (hiddenVisuals.contains(p.getUniqueId())) continue;

                double px = p.getX();
                double py = p.getY();
                double pz = p.getZ();
                World pWorld = p.getWorld();

                java.util.List<CachedItemLoc> worldItems = itemsByWorldMap.get(pWorld);
                if (worldItems == null || worldItems.isEmpty()) continue;

                for (CachedItemLoc ci : worldItems) {
                    double dx = px - ci.x();
                    double dy = py - ci.y();
                    double dz = pz - ci.z();
                    if ((dx * dx + dy * dy + dz * dz) >= partDistSq) continue;

                    Particle particle = ci.particle();
                    String category = ci.category();
                    double xCoord = ci.x();
                    double yCoord = ci.y() + 0.2;
                    double zCoord = ci.z();

                    Object data = null;
                    if (particle.getDataType() == Particle.DustOptions.class) {
                        data = category != null ? categoryDustOptions.getOrDefault(category, defaultDustOptions)
                                : defaultDustOptions;
                    }

                    switch (ci.animType()) {
                        case CIRCLE -> {
                            double radius = 0.4;
                            double x = Math.cos(particleTick * 0.2) * radius;
                            double z = Math.sin(particleTick * 0.2) * radius;
                            p.spawnParticle(particle, xCoord + x, yCoord, zCoord + z, 1, 0, 0, 0, 0, data);
                        }
                        case SPIRAL -> {
                            double radius = 0.3;
                            double x = Math.cos(particleTick * 0.3) * radius;
                            double z = Math.sin(particleTick * 0.3) * radius;
                            double yOffset = (particleTick % 20) * 0.05;
                            p.spawnParticle(particle, xCoord + x, yCoord + yOffset, zCoord + z, 1, 0, 0, 0, 0, data);
                        }
                        default -> p.spawnParticle(particle, xCoord, yCoord, zCoord, 1, 0.1, 0.1, 0.1, 0.02, data);
                    }
                }
            }
        }, 20L, (long) particlesFrequency);
    }

    public enum AnimType {
        CIRCLE, SPIRAL, DEFAULT;

        public static AnimType fromString(String str) {
            if (str == null) return DEFAULT;
            if (str.equalsIgnoreCase("CIRCLE")) return CIRCLE;
            if (str.equalsIgnoreCase("SPIRAL")) return SPIRAL;
            return DEFAULT;
        }
    }

    public void playSpawnAnimation(Item item, String id, NamespacedKey sourceMobKey, Map<String, Particle> categoryParticles, double jumpForce, int burstAmount) {
        if (item == null || !item.isValid() || !item.getItemStack().hasItemMeta()) return;
        org.bukkit.persistence.PersistentDataContainer pdc = item.getItemStack().getItemMeta().getPersistentDataContainer();
        if (sourceMobKey != null && pdc.has(sourceMobKey, org.bukkit.persistence.PersistentDataType.STRING)) {
            item.setVelocity(new org.bukkit.util.Vector(Math.random() * 0.4 - 0.2, jumpForce * 2.5, Math.random() * 0.4 - 0.2));
            Particle particle = categoryParticles.get(id);
            if (particle == null) particle = Particle.TOTEM_OF_UNDYING;
            item.getWorld().spawnParticle(particle, item.getLocation(), burstAmount * 2, 0.1, 0.1, 0.1, 0.2);
        } else {
            item.setVelocity(item.getVelocity().add(new org.bukkit.util.Vector(0, jumpForce, 0)));
            Particle particle = categoryParticles.get(id);
            if (particle != null) {
                item.getWorld().spawnParticle(particle, item.getLocation().add(0, 0.2, 0), burstAmount, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }

    public void clearAll() {
        customParticles.clear();
    }
}
