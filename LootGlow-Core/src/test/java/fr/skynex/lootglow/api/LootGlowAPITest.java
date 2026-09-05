package fr.skynex.lootglow.api;

import fr.skynex.lootglow.api.events.LootBagGroupEvent;
import fr.skynex.lootglow.api.events.LootGlowApplyEvent;
import fr.skynex.lootglow.api.events.LootProtectionExpireEvent;
import org.bukkit.Color;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LootGlowAPITest {

    @Test
    public void testLootGlowApplyEvent() {
        LootGlowApplyEvent event = new LootGlowApplyEvent(null, "legendary", Color.YELLOW);
        assertEquals("legendary", event.getCategory());
        assertEquals(Color.YELLOW, event.getGlowColor());
        assertFalse(event.isCancelled());

        event.setCategory("epic");
        event.setGlowColor(Color.PURPLE);
        event.setCancelled(true);

        assertEquals("epic", event.getCategory());
        assertEquals(Color.PURPLE, event.getGlowColor());
        assertTrue(event.isCancelled());
    }

    @Test
    public void testLootBagGroupEvent() {
        LootBagGroupEvent event = new LootBagGroupEvent(null, List.of());
        assertNull(event.getLeaderItem());
        assertEquals(0, event.getGroupSize());
        assertFalse(event.isCancelled());

        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    public void testLootProtectionExpireEvent() {
        UUID ownerUuid = UUID.randomUUID();
        LootProtectionExpireEvent event = new LootProtectionExpireEvent(null, ownerUuid);
        assertNull(event.getItem());
        assertEquals(ownerUuid, event.getOwnerUuid());
    }
}
