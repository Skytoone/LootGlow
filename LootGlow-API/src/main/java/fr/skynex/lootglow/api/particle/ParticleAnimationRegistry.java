package fr.skynex.lootglow.api.particle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe global registry for custom LootGlow particle animation renderers.
 */
public final class ParticleAnimationRegistry {

    private static final Map<String, ParticleAnimation> REGISTRY = new ConcurrentHashMap<>();

    private ParticleAnimationRegistry() {
    }

    /**
     * Registers a custom particle animation renderer under a unique string identifier.
     *
     * @param id Unique animation identifier (case-insensitive, e.g. "dust_transition_orbit")
     * @param animation ParticleAnimation implementation or lambda
     */
    public static void register(@NotNull String id, @NotNull ParticleAnimation animation) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(animation, "animation cannot be null");
        REGISTRY.put(id.toLowerCase(), animation);
    }

    /**
     * Unregisters a particle animation renderer by ID.
     *
     * @param id Animation identifier
     * @return True if an animation was removed
     */
    public static boolean unregister(@NotNull String id) {
        if (id == null) return false;
        return REGISTRY.remove(id.toLowerCase()) != null;
    }

    /**
     * Retrieves a registered particle animation renderer by ID.
     *
     * @param id Animation identifier
     * @return ParticleAnimation instance or null if not found
     */
    @Nullable
    public static ParticleAnimation get(@NotNull String id) {
        if (id == null) return null;
        return REGISTRY.get(id.toLowerCase());
    }

    /**
     * Checks if a particle animation is registered under the given ID.
     *
     * @param id Animation identifier
     * @return True if registered
     */
    public static boolean has(@NotNull String id) {
        if (id == null) return false;
        return REGISTRY.containsKey(id.toLowerCase());
    }

    /**
     * Returns an unmodifiable set of all registered animation identifiers.
     */
    @NotNull
    public static Set<String> getRegisteredIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }

    /**
     * Clears all registered custom animations (useful during plugin reload or disable).
     */
    public static void clear() {
        REGISTRY.clear();
    }
}
