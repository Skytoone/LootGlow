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
        
        UUID leaderUuid = plugin.getOpenContainers().get(player.getUniqueId());
        if (leaderUuid == null) return;

        // Prevent moving items into the loot container
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }

        int slot = event.getSlot();
        List<UUID> members = plugin.getGroupMembers().get(leaderUuid);
        if (members == null || slot < 0 || slot >= members.size()) {
            event.setCancelled(true);
            return;
        }

        UUID itemUuid = members.get(slot);
        Item item = plugin.getActiveItems().get(itemUuid);

        if (item != null && item.isValid()) {
            org.bukkit.Location oldLoc = item.getLocation().clone();
            ItemStack toAdd = item.getItemStack().clone();
            // Try to add to player inventory
            java.util.HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(toAdd);
            
            if (leftovers.isEmpty()) {
                // Success, remove item from world
                plugin.removeGlow(item);
                item.remove();
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                
                // Remove from members list
                members.remove(slot);
                
                if (members.isEmpty()) {
                    player.closeInventory();
                } else {
                    if (slot == 0 && !members.isEmpty()) {
                        UUID newLeaderUuid = members.get(0);
                        Item newLeaderItem = plugin.getActiveItems().get(newLeaderUuid);
                        if (newLeaderItem != null && newLeaderItem.isValid() && oldLoc != null) {
                            newLeaderItem.teleport(oldLoc);
                        }
                        plugin.transferLeaderVisuals(leaderUuid, newLeaderUuid);
                        plugin.getOpenContainers().put(player.getUniqueId(), newLeaderUuid);
                    }
                    // Refresh GUI
                    event.getClickedInventory().setItem(slot, null);
                    // Shift items in GUI to avoid gaps
                    refreshInventory(event.getClickedInventory(), members);
                }
            } else {
                // Inventory full
                plugin.sendMessage(player, "inventory-full");
            }
        }
        
        event.setCancelled(true);
    }

    private void refreshInventory(Inventory inv, List<UUID> members) {
        inv.clear();
        for (int i = 0; i < Math.min(members.size(), inv.getSize()); i++) {
            Item item = plugin.getActiveItems().get(members.get(i));
            if (item != null && item.isValid()) {
                inv.setItem(i, item.getItemStack());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        plugin.getOpenContainers().remove(event.getPlayer().getUniqueId());
    }
}
