package fr.skynex.lootglow.api.particle;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fluent builder for spawning particle effects efficiently targeting a specific player viewer without intermediate Location allocations.
 */
public class ParticleBuilder {

    private final Player viewer;
    private final Particle particle;
    private double x;
    private double y;
    private double z;
    private int count = 1;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double offsetZ = 0.0;
    private double extra = 0.0;
    private Object data = null;

    public ParticleBuilder(@NotNull Player viewer, @NotNull Particle particle) {
        this.viewer = viewer;
        this.particle = particle;
    }

    public ParticleBuilder location(@NotNull Location location) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        return this;
    }

    public ParticleBuilder location(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public ParticleBuilder count(int count) {
        this.count = count;
        return this;
    }

    public ParticleBuilder offset(double offsetX, double offsetY, double offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        return this;
    }

    public ParticleBuilder extra(double extra) {
        this.extra = extra;
        return this;
    }

    public ParticleBuilder speed(double speed) {
        this.extra = speed;
        return this;
    }

    public ParticleBuilder data(@Nullable Object data) {
        this.data = data;
        return this;
    }

    public void spawn() {
        if (viewer == null || !viewer.isOnline()) return;
        viewer.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, data);
    }
}
