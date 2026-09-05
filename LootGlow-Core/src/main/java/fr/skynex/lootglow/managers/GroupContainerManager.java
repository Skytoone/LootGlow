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
/**
 * Manages item grouping, visual bags, and group loot container interactions.
 */
public class GroupContainerManager {

    private final LootGlow plugin;

    public GroupContainerManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, List<UUID>> getGroupMembers() {
        return plugin.getStateRepository().getGroupMembers();
    }

    public Map<UUID, UUID> getOpenContainers() {
        return plugin.getStateRepository().getOpenContainers();
    }

    public Set<UUID> getGroupedItems() {
        return plugin.getStateRepository().getGroupedItems();
    }

    public Set<UUID> getGroupLeaders() {
        return plugin.getStateRepository().getGroupLeaders().keySet();
    }

    public UUID getGroupLeader(UUID itemUuid) {
        if (itemUuid == null) return null;
        var members = getGroupMembers();
        if (members.containsKey(itemUuid)) return itemUuid;
        for (Map.Entry<UUID, List<UUID>> entry : members.entrySet()) {
            if (entry.getValue() != null && entry.getValue().contains(itemUuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void transferLeaderVisuals(UUID oldLeader, UUID newLeader) {
        if (oldLeader == null || newLeader == null) return;
        
        var stateRepo = plugin.getStateRepository();
        Integer count = stateRepo.getGroupLeaders().remove(oldLeader);
        if (count != null) {
            stateRepo.getGroupLeaders().put(newLeader, count);
        }
        stateRepo.getGroupedItems().remove(newLeader);

        List<UUID> members = stateRepo.getGroupMembers().remove(oldLeader);
        if (members != null) {
            stateRepo.getGroupMembers().put(newLeader, members);
        }

        var trackedMgr = plugin.getService(TrackedItemManager.class);
        if (trackedMgr != null) {
            fr.skynex.lootglow.model.TrackedItem tiOld = trackedMgr.getTrackedItems().remove(oldLeader);
            if (tiOld != null) {
                trackedMgr.getTrackedItems().put(newLeader, tiOld);
            }
        }

        Long spawnTime = plugin.getStateRepository().getItemSpawnTimes().remove(oldLeader);
        if (spawnTime != null) {
            plugin.getStateRepository().getItemSpawnTimes().put(newLeader, spawnTime);
        }

        // Instantly refresh hologram label for the new leader
        var activeItems = trackedMgr != null ? trackedMgr.getActiveItems() : plugin.getStateRepository().getActiveItems();
        Item newLeaderItem = activeItems.get(newLeader);
        var holoSvc = plugin.getService(fr.skynex.lootglow.service.HologramService.class);
        var cfgMgr = plugin.getConfigManager();
        if (newLeaderItem != null && newLeaderItem.isValid() && holoSvc != null && cfgMgr != null) {
            holoSvc.refreshHologram(newLeaderItem, cfgMgr.isHoloEnabled(), cfgMgr.isHoloHideUncategorized(), plugin.getStateRepository().getItemCategoriesCache(), plugin.getStateRepository().getItemCategories(), cfgMgr.getDefaultColor(), plugin.getStateRepository().getLastHoloState());
        }
    }

    public void openLootContainer(Player player, UUID leaderUuid, boolean containerEnabled, String containerTitle, Map<UUID, ItemDisplay> activeItemVisuals, float rpgBlockScale, MiniMessage miniMessage) {
        if (!containerEnabled) return;
        List<UUID> members = getGroupMembers().get(leaderUuid);
        if (members == null || members.isEmpty()) return;

        var trackedMgr = plugin.getService(TrackedItemManager.class);
        var activeItems = trackedMgr != null ? trackedMgr.getActiveItems() : plugin.getStateRepository().getActiveItems();
        Item leaderItem = activeItems.get(leaderUuid);
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

            Inventory gui = Bukkit.createInventory(null, size, fr.skynex.lootglow.util.ColorUtil.parse(containerTitle));
            for (int i = 0; i < Math.min(members.size(), 54); i++) {
                Item item = activeItems.get(members.get(i));
                if (item != null && item.isValid()) {
                    gui.setItem(i, item.getItemStack());
                }
            }

            player.openInventory(gui);
            getOpenContainers().put(player.getUniqueId(), leaderUuid);
        }, 8L);
    }

    public void clearAll() {
        var stateRepo = plugin.getStateRepository();
        stateRepo.getGroupMembers().clear();
        stateRepo.getOpenContainers().clear();
        stateRepo.getGroupedItems().clear();
        stateRepo.getGroupLeaders().clear();
    }
}
