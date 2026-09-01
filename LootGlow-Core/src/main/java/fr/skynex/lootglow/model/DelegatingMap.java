package fr.skynex.lootglow.model;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Map délégante qui route les accès vers trackedItems pour assurer la
 * compatibilité binaire des getters publics (ex: getActiveLabels()).
 * Chaque opération get/put/remove lit/écrit directement dans le TrackedItem
 * correspondant, sans créer de Map intermédiaire.
 */
public class DelegatingMap<V> extends AbstractMap<UUID, V> {

    private final Map<UUID, TrackedItem> trackedItems;
    private final Function<TrackedItem, V> getter;
    private final BiConsumer<TrackedItem, V> setter;

    public DelegatingMap(Map<UUID, TrackedItem> trackedItems,
                         Function<TrackedItem, V> getter,
                         BiConsumer<TrackedItem, V> setter) {
        this.trackedItems = trackedItems;
        this.getter = getter;
        this.setter = setter;
    }

    private TrackedItem getOrCreate(UUID uuid) {
        return trackedItems.computeIfAbsent(uuid, k -> new TrackedItem());
    }

    @Override
    public V get(Object key) {
        TrackedItem ti = trackedItems.get(key);
        return ti == null ? null : getter.apply(ti);
    }

    @Override
    public boolean containsKey(Object key) {
        TrackedItem ti = trackedItems.get(key);
        return ti != null && getter.apply(ti) != null;
    }

    @Override
    public V put(UUID key, V value) {
        TrackedItem ti = getOrCreate(key);
        V old = getter.apply(ti);
        setter.accept(ti, value);
        return old;
    }

    @Override
    public V remove(Object key) {
        TrackedItem ti = trackedItems.get(key);
        if (ti == null) return null;
        V old = getter.apply(ti);
        setter.accept(ti, null);
        return old;
    }

    @Override
    public void clear() {
        trackedItems.values().forEach(ti -> setter.accept(ti, null));
    }

    @Override
    public Set<Entry<UUID, V>> entrySet() {
        Set<Entry<UUID, V>> result = new LinkedHashSet<>();
        for (Entry<UUID, TrackedItem> e : trackedItems.entrySet()) {
            V v = getter.apply(e.getValue());
            if (v != null) {
                result.add(new SimpleEntry<>(e.getKey(), v));
            }
        }
        return result;
    }
}
