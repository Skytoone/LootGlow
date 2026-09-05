package fr.skynex.lootglow.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ItemMergeManagerTest {

    private ItemMergeManager mergeManager;

    @BeforeEach
    public void setUp() {
        mergeManager = new ItemMergeManager(null);
    }

    @Test
    public void testDefaultConfigDefaults() {
        assertTrue(mergeManager.isIgnoreAllUuidKeys());
        assertNotNull(mergeManager.getIgnoredPdcKeys());
        assertFalse(mergeManager.isAutoStackEnabled());
        assertEquals(3.0, mergeManager.getAutoStackDistance(), 0.001);
    }
}
