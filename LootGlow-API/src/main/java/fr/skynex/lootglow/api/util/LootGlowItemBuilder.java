package fr.skynex.lootglow.api.util;

import fr.skynex.lootglow.api.LootGlowAPI;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Fluent builder class for spawning and configuring dropped items with LootGlow visual effects and protection.
 */
public class LootGlowItemBuilder {

    private final Location location;
    private final ItemStack itemStack;
    private String category;
    private Color glowColor;
    private final Map<UUID, Color> playerGlowColors = new HashMap<>();
    private Boolean beaconBeam;
    private Color beamColor;
    private String hologramText;
    private Particle particle;
    private UUID protectionOwner;
    private long protectionDurationSeconds;
    private Boolean bouncing;
    private Sound dropSound;
    private float soundVolume = 1.0f;
    private float soundPitch = 1.0f;

    public LootGlowItemBuilder(@NotNull Location location, @NotNull ItemStack itemStack) {
        this.location = Objects.requireNonNull(location, "Location cannot be null");
        this.itemStack = Objects.requireNonNull(itemStack, "ItemStack cannot be null");
    }

    public LootGlowItemBuilder category(@Nullable String category) {
        this.category = category;
        return this;
    }

    public LootGlowItemBuilder glowColor(@Nullable Color glowColor) {
        this.glowColor = glowColor;
        return this;
    }

    public LootGlowItemBuilder glowColor(@NotNull Player player, @Nullable Color glowColor) {
        if (glowColor != null) {
            this.playerGlowColors.put(player.getUniqueId(), glowColor);
        } else {
            this.playerGlowColors.remove(player.getUniqueId());
        }
        return this;
    }

    public LootGlowItemBuilder beaconBeam(boolean enabled) {
        this.beaconBeam = enabled;
        return this;
    }

    public LootGlowItemBuilder beaconBeam(boolean enabled, @Nullable Color color) {
        this.beaconBeam = enabled;
        this.beamColor = color;
        return this;
    }

    public LootGlowItemBuilder hologram(@Nullable String hologramText) {
        this.hologramText = hologramText;
        return this;
    }

    public LootGlowItemBuilder particle(@Nullable Particle particle) {
        this.particle = particle;
        return this;
    }

    public LootGlowItemBuilder protection(@NotNull UUID ownerUuid, long durationSeconds) {
        this.protectionOwner = ownerUuid;
        this.protectionDurationSeconds = durationSeconds;
        return this;
    }

    public LootGlowItemBuilder permanentProtection(@NotNull UUID ownerUuid) {
        this.protectionOwner = ownerUuid;
        this.protectionDurationSeconds = -1;
        return this;
    }

    public LootGlowItemBuilder bouncing(boolean bouncing) {
        this.bouncing = bouncing;
        return this;
    }

    public LootGlowItemBuilder dropSound(@Nullable Sound sound, float volume, float pitch) {
        this.dropSound = sound;
        this.soundVolume = volume;
        this.soundPitch = pitch;
        return this;
    }

    /**
     * Spawns the item entity in world and applies all configured LootGlow effects.
     *
     * @return The spawned Item entity
     */
    public Item spawn() {
        if (location.getWorld() == null) {
            throw new IllegalStateException("World cannot be null when spawning item");
        }
        LootGlowAPI api = LootGlowHook.getAPI().orElseThrow(() ->
                new IllegalStateException("LootGlowAPI is not registered or plugin is disabled"));

        Item spawnedItem = api.spawnGlowItem(location, itemStack, category);

        if (glowColor != null) {
            api.setGlowColor(spawnedItem, glowColor);
        }
        for (Map.Entry<UUID, Color> entry : playerGlowColors.entrySet()) {
            Player p = location.getWorld().getPlayers().stream()
                    .filter(pl -> pl.getUniqueId().equals(entry.getKey()))
                    .findFirst().orElse(null);
            if (p != null) {
                api.setGlowColor(spawnedItem, entry.getValue(), p);
            }
        }
        if (beaconBeam != null) {
            api.setBeaconBeam(spawnedItem, beaconBeam, beamColor);
        }
        if (hologramText != null) {
            api.setCustomHologram(spawnedItem, hologramText);
        }
        if (particle != null) {
            api.setParticleEffect(spawnedItem, particle);
        }
        if (protectionOwner != null && protectionDurationSeconds != 0) {
            api.setLootProtection(spawnedItem, protectionOwner, protectionDurationSeconds);
        }
        if (bouncing != null) {
            api.setBouncingEnabled(spawnedItem, bouncing);
        }
        if (dropSound != null) {
            api.setDropSound(spawnedItem, dropSound, soundVolume, soundPitch);
        }

        return spawnedItem;
    }
}
