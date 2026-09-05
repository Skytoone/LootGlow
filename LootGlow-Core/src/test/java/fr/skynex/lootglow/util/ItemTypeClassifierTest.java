package fr.skynex.lootglow.util;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ItemTypeClassifierTest {

    @Test
    public void testIsFlatItemOrBlockNullAndAir() {
        Set<Material> empty = Collections.emptySet();
        assertFalse(ItemTypeClassifier.isFlatItemOrBlock(null, empty, empty));
        assertFalse(ItemTypeClassifier.isFlatItemOrBlock(Material.AIR, empty, empty));
    }

    @Test
    public void testIsFlatItemOrBlockStandardFlat() {
        Set<Material> empty = Collections.emptySet();
        assertTrue(ItemTypeClassifier.isFlatItemOrBlock(Material.OAK_DOOR, empty, empty));
        assertTrue(ItemTypeClassifier.isFlatItemOrBlock(Material.OAK_SIGN, empty, empty));
        assertTrue(ItemTypeClassifier.isFlatItemOrBlock(Material.LADDER, empty, empty));
        assertTrue(ItemTypeClassifier.isFlatItemOrBlock(Material.PAINTING, empty, empty));
        assertTrue(ItemTypeClassifier.isFlatItemOrBlock(Material.ITEM_FRAME, empty, empty));
    }

    @Test
    public void testIsFlatItemOrBlockForceFlat() {
        Set<Material> forceFlat = new HashSet<>();
        forceFlat.add(Material.STONE);
        Set<Material> empty = Collections.emptySet();
        assertTrue(ItemTypeClassifier.isFlatItemOrBlock(Material.STONE, forceFlat, empty));
    }

    @Test
    public void testIsUprightItemNullAndAir() {
        Set<Material> empty = Collections.emptySet();
        assertFalse(ItemTypeClassifier.isUprightItem(null, empty, empty));
        assertFalse(ItemTypeClassifier.isUprightItem(Material.AIR, empty, empty));
    }

    @Test
    public void testIsUprightItemHeadsAndBanners() {
        Set<Material> empty = Collections.emptySet();
        assertTrue(ItemTypeClassifier.isUprightItem(Material.PLAYER_HEAD, empty, empty));
        assertTrue(ItemTypeClassifier.isUprightItem(Material.WHITE_BANNER, empty, empty));
        assertTrue(ItemTypeClassifier.isUprightItem(Material.RED_BED, empty, empty));
        assertFalse(ItemTypeClassifier.isUprightItem(Material.DIAMOND_BLOCK, empty, empty));
        assertFalse(ItemTypeClassifier.isUprightItem(Material.STONE, empty, empty));
    }

    @Test
    public void testIsFishItem() {
        assertTrue(ItemTypeClassifier.isFishItem(Material.COD));
        assertTrue(ItemTypeClassifier.isFishItem(Material.SALMON));
        assertTrue(ItemTypeClassifier.isFishItem(Material.PUFFERFISH));
        assertTrue(ItemTypeClassifier.isFishItem(Material.TROPICAL_FISH));
        assertFalse(ItemTypeClassifier.isFishItem(Material.DIAMOND));
        assertFalse(ItemTypeClassifier.isFishItem(null));
    }

    @Test
    public void testIsCustomItemNull() {
        assertFalse(ItemTypeClassifier.isCustomItem(null));
    }

    @Test
    public void testGetInternalIdNull() {
        assertNull(ItemTypeClassifier.getInternalId(null));
    }
}
