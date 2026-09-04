package fr.skynex.lootglow.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DelegatingMapTest {

    private Map<UUID, TrackedItem> trackedItems;
    private DelegatingMap<String> delegatingCategoryMap;

    @BeforeEach
    void setUp() {
        trackedItems = new HashMap<>();
        delegatingCategoryMap = new DelegatingMap<>(
                trackedItems,
                ti -> ti.category,
                (ti, val) -> ti.category = val
        );
    }

    @Test
    void testPutAndGet() {
        UUID id = UUID.randomUUID();
        delegatingCategoryMap.put(id, "EPIC");

        assertEquals("EPIC", delegatingCategoryMap.get(id));
        assertTrue(trackedItems.containsKey(id));
        assertEquals("EPIC", trackedItems.get(id).category);
    }

    @Test
    void testContainsKey() {
        UUID id = UUID.randomUUID();
        assertFalse(delegatingCategoryMap.containsKey(id));

        delegatingCategoryMap.put(id, "LEGENDARY");
        assertTrue(delegatingCategoryMap.containsKey(id));
    }

    @Test
    void testRemove() {
        UUID id = UUID.randomUUID();
        delegatingCategoryMap.put(id, "RARE");
        assertEquals("RARE", delegatingCategoryMap.get(id));

        String oldVal = delegatingCategoryMap.remove(id);
        assertEquals("RARE", oldVal);
        assertNull(delegatingCategoryMap.get(id));
        assertNull(trackedItems.get(id).category);
    }

    @Test
    void testEntrySetIteration() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        delegatingCategoryMap.put(id1, "COMMON");
        delegatingCategoryMap.put(id2, "UNCOMMON");

        assertEquals(2, delegatingCategoryMap.entrySet().size());
    }

    @Test
    void testClear() {
        UUID id = UUID.randomUUID();
        delegatingCategoryMap.put(id, "MYTHIC");

        delegatingCategoryMap.clear();
        assertNull(delegatingCategoryMap.get(id));
    }
}
