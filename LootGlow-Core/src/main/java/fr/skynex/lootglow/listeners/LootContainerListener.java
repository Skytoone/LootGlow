package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class LootContainerListener implements Listener {

    private final LootGlow plugin;

    public LootContainerListener(LootGlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        var gcMgr = plugin.getService(fr.skynex.lootglow.managers.GroupContainerManager.class);
        var openContainers = gcMgr != null ? gcMgr.getOpenContainers() : plugin.getStateRepository().getOpenContainers();
        var groupMembers = gcMgr != null ? gcMgr.getGroupMembers() : plugin.getStateRepository().getGroupMembers();
        var trackedMgr = plugin.getService(fr.skynex.lootglow.managers.TrackedItemManager.class);
        var activeItems = trackedMgr != null ? trackedMgr.getActiveItems() : plugin.getStateRepository().getActiveItems();
        var spawner = plugin.getService(fr.skynex.lootglow.managers.VisualSpawner.class);

        UUID leaderUuid = openContainers.get(player.getUniqueId());
        if (leaderUuid == null) return;

        // Prevent moving items into the loot container
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }

        int slot = event.getSlot();
        List<UUID> members = groupMembers.get(leaderUuid);
        if (members == null || slot < 0 || slot >= members.size()) {
            event.setCancelled(true);
            return;
        }

        UUID itemUuid = members.get(slot);
        Item item = activeItems.get(itemUuid);

        if (item != null && item.isValid()) {
            org.bukkit.Location oldLoc = item.getLocation();
            ItemStack toAdd = item.getItemStack().clone();
            // Try to add to player inventory
            java.util.HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(toAdd);
            
            if (leftovers.isEmpty()) {
                // Fire custom API pickup event
                String category = trackedMgr != null ? trackedMgr.getItemCategoriesCache().get(itemUuid) : plugin.getStateRepository().getItemCategoriesCache().get(itemUuid);
                fr.skynex.lootglow.api.events.LootGlowItemPickupEvent apiEvent =
                        new fr.skynex.lootglow.api.events.LootGlowItemPickupEvent(player, item, toAdd, category);
                org.bukkit.Bukkit.getPluginManager().callEvent(apiEvent);
                if (apiEvent.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }

                // Increment loot stats
                var db = plugin.getService(fr.skynex.lootglow.database.DatabaseManager.class);
                if (db != null) {
                    db.incrementLootStat(player.getUniqueId(), category != null ? category : "DEFAULT", toAdd.getAmount());
                }

                // Remove from members list first so we know the new state
                members.remove(slot);

                if (members.isEmpty()) {
                    // Last item picked up — clean up everything normally
                    if (spawner != null) spawner.removeGlow(itemUuid);
                    item.remove();
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                    player.closeInventory();
                } else {
                    if (slot == 0) {
                        // ── Leader was removed ──
                        UUID newLeaderUuid = members.get(0);
                        Item newLeaderItem = activeItems.get(newLeaderUuid);
                        if (newLeaderItem != null && newLeaderItem.isValid()) {
                            fr.skynex.lootglow.util.FoliaScheduler.runAtEntity(plugin, newLeaderItem, () -> {
                                if (newLeaderItem.isValid()) {
                                    newLeaderItem.teleport(oldLoc);
                                }
                            });
                        }

                        if (gcMgr != null) gcMgr.transferLeaderVisuals(leaderUuid, newLeaderUuid);
                        openContainers.put(player.getUniqueId(), newLeaderUuid);

                        if (spawner != null) spawner.removeGlowKeepDisplays(itemUuid);
                        plugin.getStateRepository().getGroupedItems().remove(itemUuid);
                        item.remove();
                    } else {
                        // Non-leader slot removed
                        if (spawner != null) spawner.removeGlow(itemUuid);
                        item.remove();
                    }

                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);

                    // Refresh GUI
                    refreshInventory(event.getClickedInventory(), members, activeItems);
                }
            } else {
                // Inventory full
                var msgSvc = plugin.getService(fr.skynex.lootglow.service.MessageService.class);
                if (msgSvc != null) msgSvc.sendMessage(player, "inventory-full");
            }
        }
        
        event.setCancelled(true);
    }

    private void refreshInventory(Inventory inv, List<UUID> members, java.util.Map<UUID, Item> activeItems) {
        inv.clear();
        for (int i = 0; i < Math.min(members.size(), inv.getSize()); i++) {
            Item item = activeItems.get(members.get(i));
            if (item != null && item.isValid()) {
                inv.setItem(i, item.getItemStack());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        var gcMgr = plugin.getService(fr.skynex.lootglow.managers.GroupContainerManager.class);
        var openContainers = gcMgr != null ? gcMgr.getOpenContainers() : plugin.getStateRepository().getOpenContainers();
        openContainers.remove(event.getPlayer().getUniqueId());
    }
}
