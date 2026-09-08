package fr.skynex.lootglow.api;

import fr.skynex.lootglow.api.particle.ParticleAnimation;
import fr.skynex.lootglow.api.particle.ParticleAnimationContext;
import fr.skynex.lootglow.api.particle.ParticleAnimationRegistry;
import fr.skynex.lootglow.managers.ParticleAnimationManager;
import org.bukkit.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ParticleAnimationRegistryTest {

    @BeforeEach
    public void setUp() {
        ParticleAnimationRegistry.clear();
        ParticleAnimationManager.registerDefaultAnimations();
    }

    @Test
    public void testDefaultAnimationsRegistered() {
        assertTrue(ParticleAnimationRegistry.has("circle"));
        assertTrue(ParticleAnimationRegistry.has("spiral"));
        assertTrue(ParticleAnimationRegistry.has("dust_transition_orbit"));
        assertTrue(ParticleAnimationRegistry.has("default"));
    }

    @Test
    public void testCustomAnimationRegistration() {
        AtomicInteger count = new AtomicInteger(0);
        ParticleAnimation customAnim = context -> count.incrementAndGet();

        ParticleAnimationRegistry.register("custom_sparkle", customAnim);

        assertTrue(ParticleAnimationRegistry.has("custom_sparkle"));
        assertNotNull(ParticleAnimationRegistry.get("custom_sparkle"));

        ParticleAnimation retrieved = ParticleAnimationRegistry.get("custom_sparkle");
        retrieved.render(null);
        assertEquals(1, count.get());
    }

    @Test
    public void testUnregisterAnimation() {
        ParticleAnimationRegistry.register("temp_anim", context -> {});
        assertTrue(ParticleAnimationRegistry.has("temp_anim"));

        boolean removed = ParticleAnimationRegistry.unregister("temp_anim");
        assertTrue(removed);
        assertFalse(ParticleAnimationRegistry.has("temp_anim"));
    }

    @Test
    public void testChainingAndFiltering() {
        AtomicInteger count = new AtomicInteger(0);

        ParticleAnimation anim1 = ctx -> count.addAndGet(1);
        ParticleAnimation anim2 = ctx -> count.addAndGet(10);

        ParticleAnimation combined = anim1.andThen(anim2);
        combined.render(null);
        assertEquals(11, count.get());

        ParticleAnimation filteredFalse = combined.filter(ctx -> false);
        filteredFalse.render(null);
        assertEquals(11, count.get());
    }

    @Test
    public void testCompositeAndFactoryPresets() {
        ParticleAnimation orbit = ParticleAnimation.orbit(0.5, 0.2);
        ParticleAnimation helix = ParticleAnimation.helix(0.3, 0.3, 0.05);
        ParticleAnimation dust = ParticleAnimation.dustTransitionOrbit(Color.RED, Color.BLUE, 0.5, 0.3, 1.5f);

        ParticleAnimation composite = ParticleAnimation.composite(orbit, helix, dust);
        assertNotNull(composite);
    }
}
