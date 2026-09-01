package fr.skynex.lootglow.managers;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.model.TrackedItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages item visual displays, head textures, custom model data, and display creation/removal lifecycles.
 */
public class VisualDisplayManager {

    private final LootGlow plugin;
    private final Map<UUID, ItemDisplay> activeItemVisuals = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> entityIdMap = new ConcurrentHashMap<>();

    public VisualDisplayManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, ItemDisplay> getActiveItemVisuals() {
        return activeItemVisuals;
    }

    public Map<Integer, UUID> getEntityIdMap() {
        return entityIdMap;
    }

    public ItemStack createTexturedHead(String textureInput) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (textureInput == null || textureInput.trim().isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        try {
            String trimmed = textureInput.trim();
            String base64Value = getBase64Texture(trimmed);
            if (base64Value != null && !base64Value.isEmpty()) {
                UUID id = UUID.nameUUIDFromBytes(base64Value.getBytes(StandardCharsets.UTF_8));
                try {
                    PlayerProfile profile = Bukkit.createProfile(id, "LootBag");
                    profile.setProperty(new ProfileProperty("textures", base64Value));
                    meta.setPlayerProfile(profile);
                } catch (Throwable t1) {
                    try {
                        Object profile = Bukkit.class.getMethod("createPlayerProfile", UUID.class).invoke(null, id);
                        Class<?> propClass = Class.forName("org.bukkit.profile.ProfileProperty");
                        Object prop = propClass.getConstructor(String.class, String.class).newInstance("textures", base64Value);
                        profile.getClass().getMethod("setProperty", propClass).invoke(profile, prop);
                        meta.getClass().getMethod("setOwnerProfile", profile.getClass().getInterfaces()[0]).invoke(meta, profile);
                    } catch (Throwable ignored) {}
                }
            }
            head.setItemMeta(meta);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse textured head: " + e.getMessage());
        }
        return head;
    }

    public String getBase64Texture(String input) {
        if (input == null || input.isEmpty()) return null;
        if (input.startsWith("eyJ")) return input;
        if (input.length() == 64) {
            String url = "http://textures.minecraft.net/texture/" + input;
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
            return java.util.Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    public ItemStack getOwnerHead(UUID ownerUuid) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (ownerUuid == null) return head;
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            Player p = Bukkit.getPlayer(ownerUuid);
            if (p != null) {
                meta.setPlayerProfile(p.getPlayerProfile());
            } else {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerUuid));
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    public void removeVisual(UUID uuid) {
        ItemDisplay display = activeItemVisuals.remove(uuid);
        if (display != null && display.isValid()) {
            entityIdMap.remove(display.getEntityId());
            display.remove();
        }
    }

    public void clearVisualsForPlayer(Player player, Map<UUID, TrackedItem> trackedItems) {
        if (player == null || trackedItems == null) return;
        for (TrackedItem ti : trackedItems.values()) {
            if (ti.label != null && ti.label.isValid())
                player.hideEntity(plugin, ti.label);
            if (ti.beam != null && ti.beam.isValid()) {
                player.hideEntity(plugin, ti.beam);
                ti.beam.getPassengers().forEach(p -> player.hideEntity(plugin, p));
            }
            if (ti.visual != null && ti.visual.isValid())
                player.hideEntity(plugin, ti.visual);
            if (ti.shadow != null && ti.shadow.isValid())
                player.hideEntity(plugin, ti.shadow);
        }
    }

    public void clearAll() {
        activeItemVisuals.values().forEach(d -> {
            if (d != null && d.isValid()) d.remove();
        });
        activeItemVisuals.clear();
        entityIdMap.clear();
    }
}
