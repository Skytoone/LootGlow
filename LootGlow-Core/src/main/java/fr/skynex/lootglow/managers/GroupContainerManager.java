package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
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
        transferLeaderVisuals(oldLeader, newLeader, null);
    }

    public void transferLeaderVisuals(UUID oldLeader, UUID newLeader, Location forcedLoc) {
        if (oldLeader == null || newLeader == null) return;
        
        var stateRepo = plugin.getStateRepository();
        Integer count = stateRepo.getGroupLeaders().remove(oldLeader);
        stateRepo.getGroupedItems().remove(newLeader);

        List<UUID> members = stateRepo.getGroupMembers().remove(oldLeader);
        if (members != null) {
            stateRepo.getGroupMembers().put(newLeader, members);
        }

        ItemDisplay visualDisp = stateRepo.getActiveItemVisuals().remove(oldLeader);
        ItemDisplay existingNewVisual = stateRepo.getActiveItemVisuals().remove(newLeader);
        if (existingNewVisual != null && existingNewVisual.isValid() && existingNewVisual != visualDisp) {
            stateRepo.getEntityIdMap().remove(existingNewVisual.getEntityId());
            existingNewVisual.remove();
        }
        if (visualDisp != null) {
            stateRepo.getActiveItemVisuals().put(newLeader, visualDisp);
        }

        TextDisplay labelDisp = stateRepo.getActiveLabels().remove(oldLeader);
        TextDisplay existingNewLabel = stateRepo.getActiveLabels().remove(newLeader);
        if (existingNewLabel != null && existingNewLabel.isValid() && existingNewLabel != labelDisp) {
            existingNewLabel.remove();
        }
        if (labelDisp != null) {
            stateRepo.getActiveLabels().put(newLeader, labelDisp);
        }

        BlockDisplay beamDisp = stateRepo.getActiveBeams().remove(oldLeader);
        BlockDisplay existingNewBeam = stateRepo.getActiveBeams().remove(newLeader);
        if (existingNewBeam != null && existingNewBeam.isValid() && existingNewBeam != beamDisp) {
            existingNewBeam.getPassengers().forEach(e -> { if (e != null) e.remove(); });
            existingNewBeam.remove();
        }
        if (beamDisp != null) {
            stateRepo.getActiveBeams().put(newLeader, beamDisp);
        }

        var beamMgr = plugin.getService(BeamManager.class);
        if (beamMgr != null) {
            BeamManager.BeamConfig bCfg = beamMgr.getActiveBeamConfigs().remove(oldLeader);
            if (bCfg != null) {
                beamMgr.getActiveBeamConfigs().put(newLeader, bCfg);
            }
        }

        var rpgMgr = plugin.getService(RPGDropManager.class);
        if (rpgMgr != null) {
            var shadow = rpgMgr.getActiveShadows().remove(oldLeader);
            if (shadow != null) {
                rpgMgr.getActiveShadows().put(newLeader, shadow);
            }
        }

        var trackedMgr = plugin.getService(TrackedItemManager.class);
        fr.skynex.lootglow.model.TrackedItem tiOld = null;
        if (trackedMgr != null) {
            trackedMgr.getTrackedItems().remove(newLeader);
            tiOld = trackedMgr.getTrackedItems().remove(oldLeader);
            if (tiOld != null) {
                tiOld.baseName = null;
                tiOld.visual = visualDisp;
                tiOld.label = labelDisp;
                tiOld.beam = beamDisp;
                if (visualDisp != null) {
                    trackedMgr.registerDisplayEntity(visualDisp.getUniqueId(), newLeader);
                }
                if (labelDisp != null) {
                    trackedMgr.registerDisplayEntity(labelDisp.getUniqueId(), newLeader);
                }
                if (beamDisp != null) {
                    trackedMgr.registerDisplayEntity(beamDisp.getUniqueId(), newLeader);
                }
                trackedMgr.getTrackedItems().put(newLeader, tiOld);
            }
        }

        Long spawnTime = plugin.getStateRepository().getItemSpawnTimes().remove(oldLeader);
        if (spawnTime != null) {
            plugin.getStateRepository().getItemSpawnTimes().put(newLeader, spawnTime);
        }

        plugin.getStateRepository().getBaseNameCache().remove(oldLeader);
        plugin.getStateRepository().getBaseNameCache().remove(newLeader);

        var activeItems = trackedMgr != null ? trackedMgr.getActiveItems() : plugin.getStateRepository().getActiveItems();
        Item newLeaderItem = activeItems.get(newLeader);
        var cfgMgr = plugin.getConfigManager();

        Location targetLoc = forcedLoc;
        if (targetLoc == null && newLeaderItem != null && newLeaderItem.isValid()) {
            targetLoc = newLeaderItem.getLocation();
        }

        if (newLeaderItem != null && newLeaderItem.isValid()) {
            if (forcedLoc != null) {
                newLeaderItem.teleport(forcedLoc);
                newLeaderItem.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            }
            if (targetLoc != null) {
                if (visualDisp != null && visualDisp.isValid()) {
                    visualDisp.teleport(targetLoc);
                }
                if (labelDisp != null && labelDisp.isValid()) {
                    labelDisp.teleport(targetLoc);
                }
                if (beamDisp != null && beamDisp.isValid()) {
                    beamDisp.teleport(targetLoc);
                }
            }
            if (tiOld != null && tiOld.visual != null && tiOld.visual.isValid()) {
                boolean useVisualBag = cfgMgr != null && cfgMgr.isUseVisualBag();
                if (!useVisualBag) {
                    tiOld.visual.setItemStack(newLeaderItem.getItemStack().clone());
                }
            }
        }

        int total = 0;
        if (members != null) {
            for (UUID mUuid : members) {
                Item it = activeItems.get(mUuid);
                if (it != null && it.isValid() && it.getItemStack() != null) {
                    total += it.getItemStack().getAmount();
                }
            }
        }
        if (total > 0) {
            stateRepo.getGroupLeaders().put(newLeader, total);
        } else if (count != null) {
            stateRepo.getGroupLeaders().put(newLeader, count);
        }

        var holoSvc = plugin.getService(fr.skynex.lootglow.service.HologramService.class);
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

            members.removeIf(mUuid -> {
                Item it = activeItems.get(mUuid);
                return it == null || !it.isValid() || it.isDead();
            });

            if (members.isEmpty()) return;

            int size = ((members.size() / 9) + 1) * 9;
            if (size > 54) size = 54;

            Inventory gui = Bukkit.createInventory(null, size, fr.skynex.lootglow.util.ColorUtil.parse(containerTitle));
            int slotIdx = 0;
            for (UUID mUuid : members) {
                if (slotIdx >= size) break;
                Item item = activeItems.get(mUuid);
                if (item != null && item.isValid() && !item.isDead()) {
                    gui.setItem(slotIdx++, item.getItemStack());
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
