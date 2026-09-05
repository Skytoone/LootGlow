package fr.skynex.lootglow.config.modules;

import fr.skynex.lootglow.config.ConfigParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CategoryConfigTest {

    private CategoryConfig categoryConfig;
    private ConfigParser configParser;
    private Map<String, Component> displayNameOverridesCache;

    @BeforeEach
    public void setUp() {
        categoryConfig = new CategoryConfig();
        configParser = new ConfigParser();
        displayNameOverridesCache = new HashMap<>();
    }

    @Test
    public void testEmptyConfig() {
        YamlConfiguration config = new YamlConfiguration();
        categoryConfig.load(config, configParser, 1.0, "ORBIT", displayNameOverridesCache);

        assertTrue(categoryConfig.getCategoryColors().isEmpty());
        assertTrue(categoryConfig.getItemCategories().isEmpty());
        assertTrue(categoryConfig.getCategoryNames().isEmpty());
        assertTrue(displayNameOverridesCache.isEmpty());
    }

    @Test
    public void testCategoryParsing() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("categories.legendary.color", "GOLD");
        config.set("categories.legendary.light-level", 15);
        config.set("categories.legendary.glow", true);
        config.set("categories.legendary.items", List.of("DIAMOND_SWORD", "NETHERITE_INGOT"));
        config.set("categories.legendary.title", "<gold>LÉGENDAIRE</gold>");
        config.set("categories.legendary.notification-radius", 25.0);
        config.set("categories.legendary.subtitle", "<yellow>Objet Mythique</yellow>");
        config.set("categories.legendary.nbt-patterns", List.of("CustomNbtFlag"));
        config.set("categories.legendary.particle-animation", "SPIRAL");
        config.set("categories.legendary.lore-patterns", List.of("Épique", "Mythique"));
        config.set("categories.legendary.display-names.DIAMOND_SWORD", "<gold>Épée Divine</gold>");

        categoryConfig.load(config, configParser, 1.2, "ORBIT", displayNameOverridesCache);

        assertEquals(NamedTextColor.GOLD, categoryConfig.getCategoryColors().get("legendary"));
        assertEquals(15, categoryConfig.getCategoryLights().get("legendary"));
        assertTrue(categoryConfig.getCategoryGlow().get("legendary"));
        assertEquals(25.0, categoryConfig.getCategoryNotificationRadius().get("legendary"), 0.001);
        assertEquals("<gold>LÉGENDAIRE</gold>", categoryConfig.getCategoryTitles().get("legendary"));
        assertEquals("<yellow>Objet Mythique</yellow>", categoryConfig.getCategorySubtitles().get("legendary"));
        assertEquals("SPIRAL", categoryConfig.getCategoryAnimTypes().get("legendary"));
        assertNotNull(categoryConfig.getCategoryDustOptions().get("legendary"));
        assertEquals(1.2f, categoryConfig.getCategoryDustOptions().get("legendary").getSize(), 0.001f);

        // Material associations
        assertEquals(NamedTextColor.GOLD, categoryConfig.getItemCategories().get("DIAMOND_SWORD"));
        assertEquals("legendary", categoryConfig.getCategoryNames().get("DIAMOND_SWORD"));
        assertEquals(NamedTextColor.GOLD, categoryConfig.getItemCategories().get("NETHERITE_INGOT"));
        assertEquals("legendary", categoryConfig.getCategoryNames().get("NETHERITE_INGOT"));

        // Lowercase patterns
        List<String> lorePats = categoryConfig.getCategoryLorePatterns().get("legendary");
        assertNotNull(lorePats);
        assertEquals(List.of("épique", "mythique"), lorePats);

        List<String> nbtPats = categoryConfig.getCategoryNbtPatterns().get("legendary");
        assertNotNull(nbtPats);
        assertEquals(List.of("customnbtflag"), nbtPats);

        // Display name overrides cache
        assertNotNull(displayNameOverridesCache.get("DIAMOND_SWORD"));
    }
}
