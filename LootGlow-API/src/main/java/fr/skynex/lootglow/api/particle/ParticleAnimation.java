package fr.skynex.lootglow.api.particle;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Functional interface representing a custom particle animation renderer in LootGlow.
 * Supports functional composition, condition filtering, tick pacing, and factory presets.
 */
@FunctionalInterface
public interface ParticleAnimation {

    /**
     * Renders a particle animation tick for a specific item and viewer player.
     *
     * @param context Execution context containing location, tick counter, viewer player, item, and particle builder helper
     */
    void render(@NotNull ParticleAnimationContext context);

    /**
     * Chains another animation to execute sequentially after this animation.
     *
     * @param after The animation to execute second
     * @return Combined ParticleAnimation instance
     */
    @NotNull
    default ParticleAnimation andThen(@NotNull ParticleAnimation after) {
        Objects.requireNonNull(after, "after animation cannot be null");
        return context -> {
            this.render(context);
            after.render(context);
        };
    }

    /**
     * Filters this animation to render only when the given predicate evaluates to true.
     *
     * @param predicate Conditional filter predicate
     * @return Filtered ParticleAnimation instance
     */
    @NotNull
    default ParticleAnimation filter(@NotNull Predicate<ParticleAnimationContext> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return context -> {
            if (predicate.test(context)) {
                this.render(context);
            }
        };
    }

    /**
     * Paces this animation to render only every N ticks (e.g. interval(2) renders every 2 ticks).
     *
     * @param tickInterval Interval frequency in ticks
     * @return Paced ParticleAnimation instance
     */
    @NotNull
    default ParticleAnimation interval(int tickInterval) {
        if (tickInterval <= 1) return this;
        return context -> {
            if (context.isInterval(tickInterval)) {
                this.render(context);
            }
        };
    }

    /**
     * Wraps this animation with a fixed spatial coordinate offset (dx, dy, dz).
     *
     * @param dx Delta X offset
     * @param dy Delta Y offset
     * @param dz Delta Z offset
     * @return Offset ParticleAnimation instance
     */
    @NotNull
    default ParticleAnimation offset(double dx, double dy, double dz) {
        return context -> this.render(context.withOffset(dx, dy, dz));
    }

    // --- Static Factory Presets ---

    /**
     * Creates a circular orbit animation using the item's assigned particle type.
     *
     * @param radius Orbit radius in blocks
     * @param speed Rotation speed multiplier
     * @return Preset ParticleAnimation instance
     */
    @NotNull
    static ParticleAnimation orbit(double radius, double speed) {
        return context -> {
            double x = context.getOriginX() + context.cos(speed) * radius;
            double z = context.getOriginZ() + context.sin(speed) * radius;
            context.spawnParticle(context.getParticle(), x, context.getOriginY(), z, 1, 0, 0, 0, 0, context.getCategoryData());
        };
    }

    /**
     * Creates an ascending spiral helix animation using the item's assigned particle type.
     *
     * @param radius Spiral radius in blocks
     * @param speed Rotation speed multiplier
     * @param heightStep Height increment per cycle
     * @return Preset ParticleAnimation instance
     */
    @NotNull
    static ParticleAnimation helix(double radius, double speed, double heightStep) {
        return context -> {
            double x = context.getOriginX() + context.cos(speed) * radius;
            double z = context.getOriginZ() + context.sin(speed) * radius;
            double yOffset = (context.getTick() % 20) * heightStep;
            context.spawnParticle(context.getParticle(), x, context.getOriginY() + yOffset, z, 1, 0, 0, 0, 0, context.getCategoryData());
        };
    }

    /**
     * Creates a dual-color DUST_COLOR_TRANSITION orbit animation.
     *
     * @param fromColor Starting RGB Color
     * @param toColor Ending RGB Color
     * @param radius Orbit radius in blocks
     * @param speed Rotation speed multiplier
     * @param particleSize Particle size scale factor
     * @return Preset ParticleAnimation instance
     */
    @NotNull
    static ParticleAnimation dustTransitionOrbit(@NotNull Color fromColor, @NotNull Color toColor, double radius, double speed, float particleSize) {
        Particle.DustTransition transition = new Particle.DustTransition(fromColor, toColor, particleSize);
        return context -> {
            double x = context.getOriginX() + context.cos(speed) * radius;
            double y = context.getOriginY() + 0.2;
            double z = context.getOriginZ() + context.sin(speed) * radius;

            context.builder(Particle.DUST_COLOR_TRANSITION)
                    .location(x, y, z)
                    .count(1)
                    .data(transition)
                    .spawn();
        };
    }

    /**
     * Combines multiple particle animations into a single composite animation.
     *
     * @param animations Array of animations to combine
     * @return Composite ParticleAnimation instance
     */
    @NotNull
    static ParticleAnimation composite(ParticleAnimation... animations) {
        return context -> {
            if (animations == null) return;
            for (ParticleAnimation anim : animations) {
                if (anim != null) {
                    anim.render(context);
                }
            }
        };
    }
}
