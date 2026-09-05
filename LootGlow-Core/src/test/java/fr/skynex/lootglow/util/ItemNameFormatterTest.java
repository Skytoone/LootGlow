package fr.skynex.lootglow.util;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ItemNameFormatterTest {

    private ItemNameFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new ItemNameFormatter();
    }

    @Test
    public void testGetItemNameNullStack() {
        Component name = formatter.getItemName(null);
        assertNotNull(name);
        assertEquals(Component.empty(), name);
    }

    @Test
    public void testParseMiniMessageSimple() {
        Component component = formatter.parseMiniMessage("<gold>Test Item</gold>");
        assertNotNull(component);
    }

    @Test
    public void testParseMiniMessageNull() {
        Component component = formatter.parseMiniMessage(null);
        assertNotNull(component);
        assertEquals(Component.empty(), component);
    }
}
