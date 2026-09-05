package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class ItemInteractionListener implements Listener {

    private final LootGlow plugin;

    public ItemInteractionListener(LootGlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        var cfgMgr = plugin.getConfigManager();
        var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
        var gcMgr = plugin.getService(fr.skynex.lootglow.managers.GroupContainerManager.class);
        double range = (cfgMgr != null && cfgMgr.isRmbPickupEnabled()) ? cfgMgr.getRmbPickupRange() : 4.0;
        
        Item targetItem = null;
        double bestDist = range * range;
        
        Location eyeLoc = player.getEyeLocation();
        Vector eyeVec = eyeLoc.toVector();
        Vector eyeDir = eyeLoc.getDirection();
        
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(eyeLoc, range, range, range);
        Vector toItem = new Vector();

        for (Entity ent : nearby) {
            Item item = null;
            if (ent instanceof Item i) {
                item = i;
            } else if (ent instanceof ItemDisplay display) {
                item = trackedMgr != null ? trackedMgr.getItemForDisplay(display) : null;
            } else if (ent instanceof TextDisplay label) {
                item = trackedMgr != null ? trackedMgr.getItemForLabel(label) : null;
            }
            if (item == null || !item.isValid() || item.isDead()) continue;
            
            Location itemLoc = item.getLocation();
            toItem.setX(itemLoc.getX() - eyeVec.getX());
            toItem.setY(itemLoc.getY() - eyeVec.getY());
            toItem.setZ(itemLoc.getZ() - eyeVec.getZ());
            
            double len = toItem.length();
            if (len < 0.0001) continue;
            toItem.multiply(1.0 / len);
            
            double dot = eyeDir.dot(toItem);
            
            if (dot > 0.85) {
                double dist = eyeLoc.distanceSquared(itemLoc);
                if (dist < bestDist) {
                    bestDist = dist;
                    targetItem = item;
                }
            }
        }

        if (targetItem != null) {
            UUID leaderUuid = gcMgr != null ? gcMgr.getGroupLeader(targetItem.getUniqueId()) : null;
            boolean isGroup = (leaderUuid != null);
            if (isGroup && (cfgMgr == null || !cfgMgr.isRmbPickupEnableForGroups())) {
                event.setCancelled(true);
                if (cfgMgr != null && cfgMgr.isContainerEnabled() && gcMgr != null) {
                    gcMgr.openLootContainer(player, leaderUuid, cfgMgr.isContainerEnabled(), cfgMgr.getContainerTitle(), plugin.getStateRepository().getActiveItemVisuals(), cfgMgr.getRpgBlockScale(), net.kyori.adventure.text.minimessage.MiniMessage.miniMessage());
                }
                return;
            }

            if (cfgMgr != null && cfgMgr.isHardLockEnabled() && targetItem.getThrower() != null) {
                if (!targetItem.getThrower().equals(player.getUniqueId()) && !player.hasPermission(cfgMgr.getBypassPermission())) {
                    long spawnTime = plugin.getStateRepository().getItemSpawnTimes().getOrDefault(targetItem.getUniqueId(), 0L);
                    long elapsed = (System.currentTimeMillis() - spawnTime) / 1000L;
                    if (elapsed < cfgMgr.getProtectionDuration()) {
                        return; 
                    }
                }
            }

            if (cfgMgr == null || !cfgMgr.isRmbPickupEnabled()) return;

            if (targetItem.isValid() && !targetItem.isDead()) {
                final Item finalTargetItem = targetItem;
                FoliaScheduler.runAtEntity(plugin, finalTargetItem, () -> {
                    if (!finalTargetItem.isValid() || finalTargetItem.isDead()) return;
                    HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(finalTargetItem.getItemStack());
                    if (leftovers.isEmpty()) {
                        var rpgMgr = plugin.getService(fr.skynex.lootglow.managers.RPGDropManager.class);
                        if (rpgMgr != null) rpgMgr.playAspirationAnimation(finalTargetItem, player, plugin.getStateRepository().getActiveItemVisuals(), cfgMgr.isAspirationEnabled());
                        var spawner = plugin.getService(fr.skynex.lootglow.managers.VisualSpawner.class);
                        if (spawner != null) spawner.removeGlow(finalTargetItem.getUniqueId());
                        finalTargetItem.remove();
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                    } else {
                        finalTargetItem.getItemStack().setAmount(leftovers.get(0).getAmount());
                    }
                });
            }
        }
    }
}
