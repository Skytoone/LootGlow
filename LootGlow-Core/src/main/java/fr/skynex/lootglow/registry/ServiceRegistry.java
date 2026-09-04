package fr.skynex.lootglow.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight IoC Service Registry for LootGlow.
 * Provides type-safe registration and resolution of plugin managers, services, and pipelines.
 */
public class ServiceRegistry {

    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    /**
     * Registers a service implementation instance.
     */
    public <T> ServiceRegistry registerService(Class<T> serviceClass, T instance) {
        if (serviceClass != null && instance != null) {
            services.put(serviceClass, instance);
        }
        return this;
    }

    /**
     * Retrieves a registered service instance by its class or interface type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        if (serviceClass == null) return null;
        Object instance = services.get(serviceClass);
        if (instance == null) {
            // Fallback search for subclasses/implementations
            for (Map.Entry<Class<?>, Object> entry : services.entrySet()) {
                if (serviceClass.isAssignableFrom(entry.getKey())) {
                    return (T) entry.getValue();
                }
            }
        }
        return (T) instance;
    }

    /**
     * Removes a registered service.
     */
    public <T> void unregisterService(Class<T> serviceClass) {
        if (serviceClass != null) {
            services.remove(serviceClass);
        }
    }

    /**
     * Clears all registered services.
     */
    public void clear() {
        services.clear();
    }
}
