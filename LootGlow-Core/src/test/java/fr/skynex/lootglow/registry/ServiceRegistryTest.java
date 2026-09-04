package fr.skynex.lootglow.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceRegistryTest {

    private ServiceRegistry registry;

    interface TestService {
        String getName();
    }

    static class TestServiceImpl implements TestService {
        @Override
        public String getName() {
            return "LootGlowTestService";
        }
    }

    static class ConcreteService {
        public int getValue() {
            return 42;
        }
    }

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
    }

    @Test
    void testRegisterAndRetrieveByInterface() {
        TestServiceImpl impl = new TestServiceImpl();
        registry.registerService(TestService.class, impl);

        TestService retrieved = registry.getService(TestService.class);
        assertNotNull(retrieved);
        assertEquals("LootGlowTestService", retrieved.getName());
    }

    @Test
    void testRegisterAndRetrieveByInstanceClass() {
        ConcreteService service = new ConcreteService();
        registry.registerService(service);

        ConcreteService retrieved = registry.get(ConcreteService.class);
        assertNotNull(retrieved);
        assertEquals(42, retrieved.getValue());
    }

    @Test
    void testSubclassFallbackResolution() {
        TestServiceImpl impl = new TestServiceImpl();
        registry.registerService(impl); // Registered under TestServiceImpl.class

        // Retrieve by interface TestService.class (subclass fallback)
        TestService retrieved = registry.getService(TestService.class);
        assertNotNull(retrieved);
        assertEquals("LootGlowTestService", retrieved.getName());
    }

    @Test
    void testUnregisterService() {
        ConcreteService service = new ConcreteService();
        registry.registerService(service);

        assertNotNull(registry.get(ConcreteService.class));
        registry.unregisterService(ConcreteService.class);
        assertNull(registry.get(ConcreteService.class));
    }

    @Test
    void testClearAllServices() {
        registry.registerService(new ConcreteService());
        registry.registerService(TestService.class, new TestServiceImpl());

        registry.clear();
        assertNull(registry.get(ConcreteService.class));
        assertNull(registry.get(TestService.class));
    }
}
