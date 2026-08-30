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
                // Remove from members list first so we know the new state
                members.remove(slot);

                if (members.isEmpty()) {
                    // Last item picked up — clean up everything normally
                    plugin.removeGlow(item);
                    item.remove();
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                    player.closeInventory();
                } else {
                    if (slot == 0) {
                        // ── Leader was removed ──
                        // STEP 1: Teleport the new leader to the old leader position BEFORE transfer,
                        //         so tickGlobalSync can place displays at the correct location immediately.
                        UUID newLeaderUuid = members.get(0);
                        Item newLeaderItem = plugin.getActiveItems().get(newLeaderUuid);
                        if (newLeaderItem != null && newLeaderItem.isValid()) {
                            newLeaderItem.teleport(oldLoc);
                        }

                        // STEP 2: Seamlessly re-key all Display entities to the new leader
                        //         BEFORE destroying anything — this prevents the flicker/respawn delay.
                        plugin.transferLeaderVisuals(leaderUuid, newLeaderUuid);
                        plugin.getOpenContainers().put(player.getUniqueId(), newLeaderUuid);

                        // STEP 3: Clean up the old leader item data WITHOUT destroying the displays
                        //         (they now belong to the new leader).
                        plugin.removeGlowKeepDisplays(itemUuid);
                        item.remove();
                    } else {
                        // Non-leader slot removed — normal cleanup (displays belong to the leader, untouched)
                        plugin.removeGlow(item);
                        item.remove();
                    }

                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);

                    // Refresh GUI
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
