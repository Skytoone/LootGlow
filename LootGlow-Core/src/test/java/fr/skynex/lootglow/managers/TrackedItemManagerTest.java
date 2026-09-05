package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.model.TrackedItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class TrackedItemManagerTest {

    private TrackedItemManager trackedItemManager;
    private Map<UUID, TrackedItem> trackedItems;
    private Map<UUID, org.bukkit.entity.Item> activeItems;
    private Map<Integer, UUID> entityIdMap;
    private Set<UUID> globallyVisibleEntities;

    @BeforeEach
    public void setUp() {
        trackedItems = new ConcurrentHashMap<>();
        activeItems = new ConcurrentHashMap<>();
        entityIdMap = new ConcurrentHashMap<>();
        globallyVisibleEntities = ConcurrentHashMap.newKeySet();

        trackedItemManager = new TrackedItemManager(
                null,
                trackedItems,
                activeItems,
                entityIdMap,
                globallyVisibleEntities
        );
    }

    @Test
    public void testChunkKeyGeneration() {
        long key1 = TrackedItemManager.getChunkKey(0, 0);
        assertEquals(0L, key1);

        long key2 = TrackedItemManager.getChunkKey(10, -5);
        long key3 = TrackedItemManager.getChunkKey(10, -5);
        assertEquals(key2, key3);
        assertNotEquals(key1, key2);
    }

    @Test
    public void testDisplayViewerRegistration() {
        UUID displayUuid = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();

        trackedItemManager.registerDisplayViewer(displayUuid, playerUuid);
        // Verify viewer registered without throws
        trackedItemManager.unregisterDisplayViewer(displayUuid, playerUuid);
    }

    @Test
    public void testDisplayEntityMapping() {
        UUID displayUuid = UUID.randomUUID();
        UUID itemUuid = UUID.randomUUID();

        trackedItemManager.registerDisplayEntity(displayUuid, itemUuid);
        assertEquals(itemUuid, trackedItemManager.getDisplayToItemMap().get(displayUuid));

        trackedItemManager.unregisterDisplayEntity(displayUuid);
        assertNull(trackedItemManager.getDisplayToItemMap().get(displayUuid));
    }

    @Test
    public void testItemCategoryManagement() {
        UUID itemUuid = UUID.randomUUID();
        assertNull(trackedItemManager.getItemCategory(itemUuid));

        trackedItemManager.getOrCreateTrackedItem(itemUuid);
        trackedItemManager.setItemCategory(itemUuid, "EPIC");

        assertEquals("EPIC", trackedItemManager.getItemCategory(itemUuid));
    }

    @Test
    public void testClearAll() {
        UUID itemUuid = UUID.randomUUID();
        trackedItemManager.getOrCreateTrackedItem(itemUuid);
        trackedItemManager.registerDisplayEntity(UUID.randomUUID(), itemUuid);

        assertFalse(trackedItemManager.getTrackedItems().isEmpty());

        trackedItemManager.clearAll();

        assertTrue(trackedItemManager.getTrackedItems().isEmpty());
        assertTrue(trackedItemManager.getActiveItems().isEmpty());
        assertTrue(trackedItemManager.getItemsByWorld().isEmpty());
        assertTrue(trackedItemManager.getDisplayToItemMap().isEmpty());
    }
}
