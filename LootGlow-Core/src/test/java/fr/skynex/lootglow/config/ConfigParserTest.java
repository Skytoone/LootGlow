package fr.skynex.lootglow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigParserTest {

    private ConfigParser parser;

    @BeforeEach
    public void setUp() {
        parser = new ConfigParser();
    }

    @Test
    public void testParseSoundDisabledValues() {
        assertNull(parser.parseSound(null));
        assertNull(parser.parseSound(""));
        assertNull(parser.parseSound("   "));
        assertNull(parser.parseSound("none"));
        assertNull(parser.parseSound("NONE"));
        assertNull(parser.parseSound("off"));
        assertNull(parser.parseSound("disabled"));
        assertNull(parser.parseSound("false"));
        assertNull(parser.parseSound("\"\""));
        assertNull(parser.parseSound("''"));
    }

    @Test
    public void testParseSoundInvalid() {
        assertNull(parser.parseSound("non_existent_sound_12345"));
    }
}
