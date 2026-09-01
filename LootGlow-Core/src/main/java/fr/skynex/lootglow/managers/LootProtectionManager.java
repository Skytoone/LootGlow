package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages owner-only loot protection for dropped items.
 */
public class LootProtectionManager {

    private final LootGlow plugin;
    private final Map<UUID, UUID> lootOwners = new ConcurrentHashMap<>();
    private final Map<UUID, Long> protectionExpiry = new ConcurrentHashMap<>();

    public LootProtectionManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, UUID> getLootOwners() {
        return lootOwners;
    }

    public Map<UUID, Long> getProtectionExpiry() {
        return protectionExpiry;
    }

    public void setLootProtection(Item item, UUID ownerUuid, long durationSeconds) {
        if (item == null || ownerUuid == null || !item.isValid()) return;
        UUID uuid = item.getUniqueId();
        lootOwners.put(uuid, ownerUuid);
        long protectUntil = System.currentTimeMillis() + (durationSeconds * 1000L);
        protectionExpiry.put(uuid, protectUntil);

        item.setOwner(ownerUuid);
        org.bukkit.persistence.PersistentDataContainer pdc = item.getPersistentDataContainer();
        pdc.set(new org.bukkit.NamespacedKey(plugin, "owner"), org.bukkit.persistence.PersistentDataType.STRING, ownerUuid.toString());
        pdc.set(new org.bukkit.NamespacedKey(plugin, "protect_until"), org.bukkit.persistence.PersistentDataType.LONG, protectUntil);
    }

    public boolean isLootProtected(Item item) {
        if (item == null || !item.isValid()) return false;
        Long expiry = protectionExpiry.get(item.getUniqueId());
        if (expiry != null && System.currentTimeMillis() <= expiry) {
            return true;
        }
        org.bukkit.persistence.PersistentDataContainer pdc = item.getPersistentDataContainer();
        Long protectUntil = pdc.get(new org.bukkit.NamespacedKey(plugin, "protect_until"), org.bukkit.persistence.PersistentDataType.LONG);
        return protectUntil != null && protectUntil > System.currentTimeMillis();
    }

    public boolean isPlayerAllowedToPickup(Player player, Item item) {
        if (player == null || item == null || !item.isValid()) return true;
        if (!isLootProtected(item)) return true;
        UUID owner = getLootOwner(item);
        return owner == null || owner.equals(player.getUniqueId()) || player.hasPermission("lootglow.bypass");
    }

    public UUID getLootOwner(Item item) {
        if (item == null || !item.isValid()) return null;
        UUID owner = lootOwners.get(item.getUniqueId());
        if (owner != null) return owner;

        org.bukkit.persistence.PersistentDataContainer pdc = item.getPersistentDataContainer();
        String ownerStr = pdc.get(new org.bukkit.NamespacedKey(plugin, "owner"), org.bukkit.persistence.PersistentDataType.STRING);
        if (ownerStr != null) {
            try {
                return UUID.fromString(ownerStr);
            } catch (Exception ignored) {}
        }
        return item.getOwner();
    }

    public void removeProtection(UUID uuid) {
        lootOwners.remove(uuid);
        protectionExpiry.remove(uuid);
    }

    public void clearAll() {
        lootOwners.clear();
        protectionExpiry.clear();
    }
}
