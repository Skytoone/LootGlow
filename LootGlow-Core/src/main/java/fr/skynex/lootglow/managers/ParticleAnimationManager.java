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

import java.util.Collections;
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

    public void triggerParabolaDropAnimation(Item item, fr.skynex.lootglow.managers.RarityManager.ItemRarity rarity) {
        if (item == null || !item.isValid()) return;
        double angle = Math.random() * Math.PI * 2;
        double speed = 0.2 + Math.random() * 0.15;
        double vx = Math.cos(angle) * speed;
        double vz = Math.sin(angle) * speed;
        double vy = 0.35 + (rarity == fr.skynex.lootglow.managers.RarityManager.ItemRarity.MYTHIC || rarity == fr.skynex.lootglow.managers.RarityManager.ItemRarity.LEGENDARY ? 0.25 : 0.1);

        item.setVelocity(new org.bukkit.util.Vector(vx, vy, vz));

        Particle part = switch (rarity) {
            case MYTHIC -> Particle.TOTEM_OF_UNDYING;
            case LEGENDARY -> Particle.FLAME;
            case EPIC -> Particle.DRAGON_BREATH;
            case RARE -> Particle.SOUL_FIRE_FLAME;
            default -> Particle.END_ROD;
        };

        item.getWorld().spawnParticle(part, item.getLocation(), 20, 0.2, 0.2, 0.2, 0.05);
    }

    public void triggerImpactShockwave(Item item, String category) {
        if (item == null || !item.isValid()) return;
        if (!plugin.getConfig().getBoolean("settings.wow-effects.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("settings.wow-effects.impact-shockwave.enabled", true)) return;

        java.util.List<String> enabledCategories = plugin.getConfig().getStringList("settings.wow-effects.impact-shockwave.categories");
        if (category != null && !enabledCategories.isEmpty() && enabledCategories.stream().noneMatch(c -> c.equalsIgnoreCase(category))) {
            return;
        }

        int count = plugin.getConfig().getInt("settings.wow-effects.impact-shockwave.particle-count", 24);
        String soundStr = plugin.getConfig().getString("settings.wow-effects.impact-shockwave.sound", "BLOCK_AMETHYST_BLOCK_RESONATE");

        Location loc = item.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        net.kyori.adventure.text.format.NamedTextColor color = category != null ? plugin.getConfigManager().getCategoryColors().get(category) : null;
        org.bukkit.Color dustColor = color != null ? org.bukkit.Color.fromRGB(color.red(), color.green(), color.blue()) : org.bukkit.Color.WHITE;
        Particle.DustOptions dustOptions = new Particle.DustOptions(dustColor, 1.2f);

        for (int r = 1; r <= 3; r++) {
            final double radius = r * 0.4;
            FoliaScheduler.runLater(plugin, () -> {
                if (!item.isValid()) return;
                double cx = item.getX();
                double cy = item.getY() + 0.1;
                double cz = item.getZ();
                org.bukkit.World w = item.getWorld();
                for (int i = 0; i < count; i++) {
                    double angle = (2 * Math.PI * i) / count;
                    double x = cx + radius * Math.cos(angle);
                    double z = cz + radius * Math.sin(angle);
                    w.spawnParticle(Particle.DUST, x, cy, z, 1, 0, 0, 0, 0, dustOptions);
                }
            }, (r - 1) * 2L);
        }

        var parser = plugin.getService(fr.skynex.lootglow.config.ConfigParser.class);
        Sound sound = parser != null ? parser.parseSound(soundStr) : null;
        if (sound != null) {
            world.playSound(loc, sound, 1.0f, 1.2f);
        }
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

    public void stopParticleTask() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }

    public org.bukkit.scheduler.BukkitTask getParticleTask() {
        return particleTask;
    }

    public void startParticleTask(boolean isEnabled,
                                  boolean particleLoopEnabled,
                                  double particleDist,
                                  Map<UUID, Item> activeItems,
                                  Map<UUID, Particle> itemParticlesCache,
                                  Map<UUID, String> itemCategoriesCache,
                                  Set<UUID> hiddenVisuals,
                                  Map<String, Particle.DustOptions> categoryDustOptions,
                                  Particle.DustOptions defaultDustOptions,
                                  Map<String, String> categoryAnimTypes,
                                  String particleAnimType,
                                  int particlesFrequency) {
        stopParticleTask();

        double lodPartDistSq = particleDist * particleDist;
        double partDistSq = lodPartDistSq;
        double maxPartDist = Math.sqrt(lodPartDistSq);
        int chunkRadius = (int) Math.ceil(maxPartDist / 16.0);

        particleTask = FoliaScheduler.runTimer(plugin, () -> {
            if (!isEnabled || !particleLoopEnabled) return;
            particleTick++;
            var auraMgr = plugin.getService(GroundAuraManager.class);
            if (auraMgr != null) {
                auraMgr.tickAuras(activeItems, itemCategoriesCache);
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (hiddenVisuals.contains(p.getUniqueId())) continue;

                World pWorld = p.getWorld();
                double px = p.getX();
                double py = p.getY();
                double pz = p.getZ();

                var trackedMgr = plugin.getService(TrackedItemManager.class);
                Set<UUID> nearbyItemUuids = trackedMgr != null
                        ? trackedMgr.getItemsInChunkRadius(pWorld, ((int) px) >> 4, ((int) pz) >> 4, chunkRadius)
                        : Collections.emptySet();
                if (nearbyItemUuids == null || nearbyItemUuids.isEmpty()) continue;

                var gcMgr = plugin.getService(GroupContainerManager.class);
                Set<UUID> groupedItems = gcMgr != null ? gcMgr.getGroupedItems() : plugin.getStateRepository().getGroupedItems();

                for (UUID uuid : nearbyItemUuids) {
                    if (groupedItems.contains(uuid)) continue;
                    Item item = activeItems.get(uuid);
                    if (item == null || item.isDead() || !item.isValid()) continue;

                    Particle particle = itemParticlesCache.get(uuid);
                    if (particle == null) continue;

                    Location loc = item.getLocation();
                    double ix = loc.getX();
                    double iy = loc.getY();
                    double iz = loc.getZ();

                    double dx = px - ix;
                    double dy = py - iy;
                    double dz = pz - iz;
                    if ((dx * dx + dy * dy + dz * dz) > partDistSq) continue;

                    String cat = itemCategoriesCache.get(uuid);
                    String animType = cat != null ? categoryAnimTypes.getOrDefault(cat, particleAnimType) : particleAnimType;

                    Object data = null;
                    if (particle.getDataType() == Particle.DustOptions.class) {
                        data = cat != null ? categoryDustOptions.getOrDefault(cat, defaultDustOptions)
                                : defaultDustOptions;
                    }

                    double xCoord = ix;
                    double yCoord = iy + 0.5;
                    double zCoord = iz;

                    switch (animType != null ? animType.toUpperCase() : "") {
                        case "CIRCLE" -> {
                            double radius = 0.4;
                            double x = Math.cos(particleTick * 0.2) * radius;
                            double z = Math.sin(particleTick * 0.2) * radius;
                            p.spawnParticle(particle, xCoord + x, yCoord, zCoord + z, 1, 0, 0, 0, 0, data);
                        }
                        case "SPIRAL" -> {
                            double radius = 0.3;
                            double x = Math.cos(particleTick * 0.3) * radius;
                            double z = Math.sin(particleTick * 0.3) * radius;
                            double yOffset = (particleTick % 20) * 0.05;
                            p.spawnParticle(particle, xCoord + x, yCoord + yOffset, zCoord + z, 1, 0, 0, 0, 0, data);
                        }
                        default -> p.spawnParticle(particle, xCoord, yCoord, zCoord, 1, 0.1, 0.1, 0.1, 0.02, data);
                    }
                }

                // Actionbar Loot Compass Indicator for rare items
                var rarityMgr = plugin.getService(RarityManager.class);
                if (particleTick % 2 == 0 && rarityMgr != null) {
                    double closestDistSq = 900.0; // 30 blocks radius
                    Item rarestItem = null;
                    for (UUID u : nearbyItemUuids) {
                        Item itemObj = activeItems.get(u);
                        if (itemObj == null || !itemObj.isValid() || itemObj.isDead()) continue;

                        double dx = px - itemObj.getX();
                        double dy = py - itemObj.getY();
                        double dz = pz - itemObj.getZ();
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq < closestDistSq) {
                            fr.skynex.lootglow.model.TrackedItem ti = trackedMgr != null ? trackedMgr.getTrackedItem(u) : null;
                            fr.skynex.lootglow.managers.RarityManager.ItemRarity rarity = ti != null ? ti.rarity : null;
                            if (rarity == null && trackedMgr != null && itemObj.getItemStack() != null) {
                                ti = trackedMgr.getOrCreateTrackedItem(u);
                                rarity = rarityMgr.detectRarity(itemObj.getItemStack());
                                ti.rarity = rarity;
                            }
                            if (rarity == fr.skynex.lootglow.managers.RarityManager.ItemRarity.LEGENDARY || rarity == fr.skynex.lootglow.managers.RarityManager.ItemRarity.MYTHIC) {
                                closestDistSq = distSq;
                                rarestItem = itemObj;
                            }
                        }
                    }

                    if (rarestItem != null) {
                        Location targetLoc = rarestItem.getLocation();
                        String arrow = getDirectionalArrow(p, targetLoc);
                        int distance = (int) Math.sqrt(closestDistSq);
                        p.sendActionBar(fr.skynex.lootglow.util.ColorUtil.parse(
                                "<gold><b>★ Item d'Élite à proximité</b> <yellow>(" + arrow + " " + distance + "m)</yellow></gold>"
                        ));
                    }
                }
            }
        }, 20L, (long) particlesFrequency);
    }

    private String getDirectionalArrow(Player p, Location targetLoc) {
        Location pLoc = p.getLocation();
        double yaw = pLoc.getYaw();
        double angle = Math.toDegrees(Math.atan2(targetLoc.getZ() - pLoc.getZ(), targetLoc.getX() - pLoc.getX())) - 90;
        double diff = (angle - yaw + 360) % 360;

        if (diff >= 337.5 || diff < 22.5) return "↑";
        if (diff >= 22.5 && diff < 67.5) return "↗";
        if (diff >= 67.5 && diff < 112.5) return "→";
        if (diff >= 112.5 && diff < 157.5) return "↘";
        if (diff >= 157.5 && diff < 202.5) return "↓";
        if (diff >= 202.5 && diff < 247.5) return "↙";
        if (diff >= 247.5 && diff < 292.5) return "←";
        return "↖";
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
        stopParticleTask();
        customParticles.clear();
    }
}
