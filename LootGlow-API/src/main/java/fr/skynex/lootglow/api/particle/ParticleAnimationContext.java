package fr.skynex.lootglow.api.particle;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Execution context provided to a {@link ParticleAnimation} containing rendering data and helper methods.
 */
public class ParticleAnimationContext {

    private final Player viewer;
    private final Item item;
    private final Particle particle;
    private final double originX;
    private final double originY;
    private final double originZ;
    private final long tick;
    private final Object categoryData;

    public ParticleAnimationContext(@NotNull Player viewer,
                                    @NotNull Item item,
                                    @NotNull Particle particle,
                                    double originX,
                                    double originY,
                                    double originZ,
                                    long tick,
                                    @Nullable Object categoryData) {
        this.viewer = viewer;
        this.item = item;
        this.particle = particle;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.tick = tick;
        this.categoryData = categoryData;
    }

    /**
     * Gets the player viewer receiving the rendered particles.
     */
    @NotNull
    public Player getViewer() {
        return viewer;
    }

    /**
     * Gets the dropped item entity being animated.
     */
    @NotNull
    public Item getItem() {
        return item;
    }

    /**
     * Gets the target particle type assigned to the item.
     */
    @NotNull
    public Particle getParticle() {
        return particle;
    }

    /**
     * Gets the origin X coordinate of the item effect center.
     */
    public double getOriginX() {
        return originX;
    }

    /**
     * Gets the origin Y coordinate of the item effect center.
     */
    public double getOriginY() {
        return originY;
    }

    /**
     * Gets the origin Z coordinate of the item effect center.
     */
    public double getOriginZ() {
        return originZ;
    }

    /**
     * Gets the current particle animation tick counter.
     */
    public long getTick() {
        return tick;
    }

    /**
     * Gets default category particle data (such as {@link org.bukkit.Particle.DustOptions}) if configured.
     */
    @Nullable
    public Object getCategoryData() {
        return categoryData;
    }

    /**
     * Creates a Bukkit {@link Location} representing the origin location.
     * Note: Calling this method allocates a new Location object. Prefer using primitive origin coordinates or {@link #builder(Particle)} when possible.
     */
    @NotNull
    public Location getLocation() {
        return new Location(item.getWorld(), originX, originY, originZ);
    }

    /**
     * Creates a fluent {@link ParticleBuilder} initialized for the viewer player, item particle, and origin position.
     *
     * @return ParticleBuilder instance
     */
    @NotNull
    public ParticleBuilder builder() {
        return builder(particle);
    }

    /**
     * Creates a fluent {@link ParticleBuilder} initialized for the viewer player and origin position.
     *
     * @param particle Target particle type
     * @return ParticleBuilder instance
     */
    @NotNull
    public ParticleBuilder builder(@NotNull Particle particle) {
        return new ParticleBuilder(viewer, particle)
                .location(originX, originY, originZ)
                .data(categoryData);
    }

    /**
     * Direct zero-allocation particle spawn call targeting the viewer player.
     */
    public void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, @Nullable Object data) {
        if (viewer != null && viewer.isOnline()) {
            viewer.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, data);
        }
    }

    /**
     * Creates a new context offset by specified delta coordinates (dx, dy, dz).
     */
    @NotNull
    public ParticleAnimationContext withOffset(double dx, double dy, double dz) {
        return new ParticleAnimationContext(viewer, item, particle, originX + dx, originY + dy, originZ + dz, tick, categoryData);
    }

    /**
     * Calculates cosine of (tick * speed) helper.
     */
    public double cos(double speed) {
        return Math.cos(tick * speed);
    }

    /**
     * Calculates sine of (tick * speed) helper.
     */
    public double sin(double speed) {
        return Math.sin(tick * speed);
    }

    /**
     * Checks if current particle tick is divisible by specified interval.
     */
    public boolean isInterval(int interval) {
        return interval > 0 && (tick % interval == 0);
    }

    /**
     * Checks if target item entity is resting on the ground.
     */
    public boolean isOnGround() {
        return item != null && item.isOnGround();
    }
}
