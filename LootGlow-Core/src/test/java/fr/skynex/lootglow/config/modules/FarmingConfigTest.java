package fr.skynex.lootglow.config.modules;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FarmingConfigTest {

    @Test
    public void testFarmingEnabledFlagLoadedCorrectly() {
        FarmingConfig farmingConfig = new FarmingConfig();
        assertTrue(farmingConfig.isEnabled(), "Default should be true");

        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.farming.enabled", false);

        farmingConfig.load(config, null);

        assertFalse(farmingConfig.isEnabled(), "Should be false when configured as false");
    }
}
