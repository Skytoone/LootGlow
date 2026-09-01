package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages item grouping, visual bags, and group loot container interactions.
 */
public class GroupContainerManager {

    private final LootGlow plugin;
    private final Map<UUID, List<UUID>> groupMembers = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> openContainers = new ConcurrentHashMap<>();
    private final Set<UUID> groupedItems = ConcurrentHashMap.newKeySet();
    private final Set<UUID> groupLeaders = ConcurrentHashMap.newKeySet();

    public GroupContainerManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, List<UUID>> getGroupMembers() {
        return groupMembers;
    }

    public Map<UUID, UUID> getOpenContainers() {
        return openContainers;
    }

    public Set<UUID> getGroupedItems() {
        return groupedItems;
    }

    public Set<UUID> getGroupLeaders() {
        return groupLeaders;
    }

    public UUID getGroupLeader(UUID itemUuid) {
        if (itemUuid == null) return null;
        if (groupMembers.containsKey(itemUuid)) return itemUuid;
        for (Map.Entry<UUID, List<UUID>> entry : groupMembers.entrySet()) {
            if (entry.getValue() != null && entry.getValue().contains(itemUuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void transferLeaderVisuals(UUID oldLeader, UUID newLeader) {
        if (oldLeader == null || newLeader == null) return;
        
        groupLeaders.remove(oldLeader);
        groupLeaders.add(newLeader);
        groupedItems.remove(newLeader);

        List<UUID> members = groupMembers.remove(oldLeader);
        if (members != null) {
            groupMembers.put(newLeader, members);
        }

        TrackedItemManager.TrackedItem tiOld = plugin.getTrackedItemManager().getTrackedItems().remove(oldLeader);
        if (tiOld != null) {
            plugin.getTrackedItemManager().getTrackedItems().put(newLeader, tiOld);
        }

        Long spawnTime = plugin.getItemSpawnTimes().remove(oldLeader);
        if (spawnTime != null) {
            plugin.getItemSpawnTimes().put(newLeader, spawnTime);
        }

        // Instantly refresh hologram label for the new leader
        Item newLeaderItem = plugin.getActiveItems().get(newLeader);
        if (newLeaderItem != null && newLeaderItem.isValid()) {
            plugin.refreshHologram(newLeaderItem);
        }
    }

    public void openLootContainer(Player player, UUID leaderUuid, boolean containerEnabled, String containerTitle, Map<UUID, ItemDisplay> activeItemVisuals, float rpgBlockScale, MiniMessage miniMessage) {
        if (!containerEnabled) return;
        List<UUID> members = groupMembers.get(leaderUuid);
        if (members == null || members.isEmpty()) return;

        Item leaderItem = plugin.getTrackedItemManager().getActiveItems().get(leaderUuid);
        if (leaderItem != null && leaderItem.isValid()) {
            Location loc = leaderItem.getLocation();
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 0.3, 0), 20, 0.2, 0.2, 0.2, 0.1);

            ItemDisplay visual = activeItemVisuals.get(leaderUuid);
            if (visual != null && visual.isValid()) {
                org.bukkit.util.Transformation trans = visual.getTransformation();
                float baseScale = trans.getScale().x();
                if (baseScale <= 0.01f) baseScale = rpgBlockScale;
                org.bukkit.util.Transformation baseTrans = new org.bukkit.util.Transformation(
                        trans.getTranslation(), trans.getLeftRotation(),
                        new org.joml.Vector3f(baseScale, baseScale, baseScale),
                        trans.getRightRotation());
                org.bukkit.util.Transformation bumped = new org.bukkit.util.Transformation(
                        trans.getTranslation(), trans.getLeftRotation(),
                        new org.joml.Vector3f(baseScale * 1.3f, baseScale * 1.3f, baseScale * 1.3f),
                        trans.getRightRotation());

                visual.setInterpolationDelay(0);
                visual.setInterpolationDuration(4);
                visual.setTransformation(bumped);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (visual.isValid()) {
                        visual.setInterpolationDelay(0);
                        visual.setInterpolationDuration(4);
                        visual.setTransformation(baseTrans);
                    }
                }, 4L);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            int size = ((members.size() / 9) + 1) * 9;
            if (size > 54) size = 54;

            Inventory gui = Bukkit.createInventory(null, size, miniMessage.deserialize(containerTitle));
            for (int i = 0; i < Math.min(members.size(), 54); i++) {
                Item item = plugin.getTrackedItemManager().getActiveItems().get(members.get(i));
                if (item != null && item.isValid()) {
                    gui.setItem(i, item.getItemStack());
                }
            }

            player.openInventory(gui);
            openContainers.put(player.getUniqueId(), leaderUuid);
        }, 8L);
    }

    public void clearAll() {
        groupMembers.clear();
        openContainers.clear();
        groupedItems.clear();
        groupLeaders.clear();
    }
}
