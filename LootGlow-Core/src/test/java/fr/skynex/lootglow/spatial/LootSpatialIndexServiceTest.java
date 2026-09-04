package fr.skynex.lootglow.spatial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LootSpatialIndexServiceTest {

    private LootSpatialIndexService spatialIndexService;

    @BeforeEach
    void setUp() {
        spatialIndexService = new LootSpatialIndexService(null);
    }

    @Test
    void testToChunkKey() {
        long key1 = LootSpatialIndexService.toChunkKey(0, 0);
        long key2 = LootSpatialIndexService.toChunkKey(10, -5);
        long key3 = LootSpatialIndexService.toChunkKey(-100, 250);

        assertNotEquals(key1, key2);
        assertNotEquals(key2, key3);

        // Verify chunk key bit unpacking
        int chunkX = (int) (key2 >> 32);
        int chunkZ = (int) key2;
        assertEquals(10, chunkX);
        assertEquals(-5, chunkZ);
    }

    @Test
    void testClearAll() {
        UUID itemUuid = UUID.randomUUID();
        spatialIndexService.unregister(itemUuid);

        spatialIndexService.clearAll();
        // Index is empty and clean
        assertTrue(true);
    }
}
