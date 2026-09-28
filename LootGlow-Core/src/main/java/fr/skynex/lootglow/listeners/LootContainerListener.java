package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LootContainerListener implements Listener {

    private final LootGlow plugin;

    /**
     * Maps each player UUID to the ordered list of item UUIDs currently displayed in
     * their open loot-container GUI.
     * Slot N in the inventory corresponds to slotMap.get(playerUuid).get(N).
     * This is necessary because refreshInventory skips invalid items, so the GUI
     * slot index no longer matches the raw members list index.
     */
    private final Map<UUID, List<UUID>> slotMap = new HashMap<>();

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
        if (members == null) {
            event.setCancelled(true);
            return;
        }

        // Resolve the clicked slot using the slot map built by refreshInventory.
        // Lazily build the slot map on the very first click (before any refresh has run).
        List<UUID> currentSlotMap = slotMap.computeIfAbsent(player.getUniqueId(),
                k -> buildSlotList(members, activeItems));
        if (slot < 0 || slot >= currentSlotMap.size()) {
            event.setCancelled(true);
            return;
        }

        UUID itemUuid = currentSlotMap.get(slot);
        Item item = activeItems.get(itemUuid);

        if (item == null || !item.isValid() || item.isDead()) {
            // Remove by UUID (Object), not by index — the members list index != GUI slot index.
            members.remove(itemUuid);
            List<UUID> newSlotOrder = refreshInventory(event.getClickedInventory(), members, activeItems);
            slotMap.put(player.getUniqueId(), newSlotOrder);
            event.setCancelled(true);
            return;
        }

        if (item != null && item.isValid()) {
            Location oldLoc = item.getLocation().clone();
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
                // Use Object form remove(itemUuid) — NOT remove(int slot) —
                // because the slotMap index != the members list index.
                members.remove(itemUuid);

                if (members.isEmpty()) {
                    // Last item picked up - clean up everything normally
                    if (spawner != null) spawner.removeGlow(itemUuid);
                    item.remove();
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                    player.closeInventory();
                } else if (members.size() == 1) {
                    // Only 1 item remains - disband group and restore normal ground alignment
                    if (spawner != null) spawner.removeGlow(itemUuid);
                    item.remove();
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);

                    UUID remainingUuid = members.get(0);
                    Item remainingItem = activeItems.get(remainingUuid);

                    plugin.getStateRepository().getGroupLeaders().remove(leaderUuid);
                    plugin.getStateRepository().getGroupMembers().remove(leaderUuid);
                    plugin.getStateRepository().getGroupedItems().remove(remainingUuid);

                    ItemDisplay bagDisplay = plugin.getStateRepository().getActiveItemVisuals().remove(leaderUuid);
                    if (bagDisplay != null && bagDisplay.isValid()) {
                        plugin.getStateRepository().getEntityIdMap().remove(bagDisplay.getEntityId());
                        bagDisplay.remove();
                    }

                    TextDisplay bagLabel = plugin.getStateRepository().getActiveLabels().remove(leaderUuid);
                    if (bagLabel != null && bagLabel.isValid()) {
                        bagLabel.remove();
                    }

                    var beamMgr = plugin.getService(fr.skynex.lootglow.managers.BeamManager.class);
                    if (beamMgr != null) {
                        beamMgr.removeBeam(leaderUuid);
                    }

                    var rpgMgr = plugin.getService(fr.skynex.lootglow.managers.RPGDropManager.class);
                    if (rpgMgr != null) {
                        rpgMgr.removeShadow(leaderUuid);
                    }

                    if (remainingItem != null && remainingItem.isValid()) {
                        remainingItem.teleport(oldLoc);
                        remainingItem.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                        try { remainingItem.setVisibleByDefault(true); } catch (Throwable ignored) {}
                        if (trackedMgr != null) {
                            fr.skynex.lootglow.model.TrackedItem ti = trackedMgr.getTrackedItem(remainingUuid);
                            if (ti != null) {
                                ti.visualMaterial = remainingItem.getItemStack().getType();
                                ti.isBlockItem = null;
                            }
                        }
                        var surfMgr = plugin.getService(fr.skynex.lootglow.managers.SurfaceAlignmentManager.class);
                        if (surfMgr != null) {
                            surfMgr.getSurfaceStates().remove(remainingUuid);
                            surfMgr.updateSurfaceAlignment(remainingItem, null);
                        }
                        for (Player p : remainingItem.getWorld().getPlayers()) {
                            p.showEntity(plugin, remainingItem);
                        }
                        var glowSvc = plugin.getService(fr.skynex.lootglow.service.ItemGlowApplyService.class);
                        if (glowSvc != null) {
                            glowSvc.applyGlow(remainingItem, false, fr.skynex.lootglow.model.ItemGlowContext.from(plugin));
                        }
                    }
                    player.closeInventory();
                } else {
                    if (itemUuid.equals(leaderUuid)) {
                        // Leader was removed: members.remove(itemUuid) was called above (members no longer
                        // contains the old leader), so members.get(0) is now the new leader.
                        UUID newLeaderUuid = members.get(0);
                        Item newLeaderItem = activeItems.get(newLeaderUuid);
                        if (newLeaderItem != null && newLeaderItem.isValid()) {
                            newLeaderItem.teleport(oldLoc);
                            newLeaderItem.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                        }

                        // 1. Transfer visuals to the new leader BEFORE destroying old leader's entity & displays
                        if (gcMgr != null) {
                            gcMgr.transferLeaderVisuals(leaderUuid, newLeaderUuid, oldLoc);
                        }

                        // 2. Clean up scoreboard team and grouped/active state for old leader
                        try {
                            org.bukkit.scoreboard.Scoreboard scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
                            String itemEntry = leaderUuid.toString();
                            org.bukkit.scoreboard.Team itemTeam = scoreboard.getEntryTeam(itemEntry);
                            if (itemTeam != null && itemTeam.getName().startsWith("LG_")) {
                                itemTeam.removeEntry(itemEntry);
                            }
                        } catch (Exception ignored) {}

                        plugin.getStateRepository().getGroupedItems().remove(itemUuid);
                        plugin.getStateRepository().getActiveItems().remove(itemUuid);
                        item.remove();

                        // 3. Update open-container map so players who have the GUI open track the new leader.
                        for (Map.Entry<UUID, UUID> entry : openContainers.entrySet()) {
                            if (entry.getValue().equals(leaderUuid)) {
                                entry.setValue(newLeaderUuid);
                            }
                        }
                    } else {
                        // Non-leader slot removed
                        if (spawner != null) spawner.removeGlow(itemUuid);
                        plugin.getStateRepository().getGroupedItems().remove(itemUuid);
                        item.remove();

                        // Recalculate total items count and refresh hologram
                        int total = 0;
                        for (UUID mUuid : members) {
                            Item it = activeItems.get(mUuid);
                            if (it != null && it.isValid() && it.getItemStack() != null) {
                                total += it.getItemStack().getAmount();
                            }
                        }
                        plugin.getStateRepository().getGroupLeaders().put(leaderUuid, total);

                        var holoSvc = plugin.getService(fr.skynex.lootglow.service.HologramService.class);
                        var cfgMgr = plugin.getConfigManager();
                        Item leaderItem = activeItems.get(leaderUuid);
                        if (leaderItem != null && leaderItem.isValid() && holoSvc != null && cfgMgr != null) {
                            holoSvc.refreshHologram(leaderItem, cfgMgr.isHoloEnabled(), cfgMgr.isHoloHideUncategorized(),
                                    plugin.getStateRepository().getItemCategoriesCache(), plugin.getStateRepository().getItemCategories(),
                                    cfgMgr.getDefaultColor(), plugin.getStateRepository().getLastHoloState());
                        }
                    }

                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);

                    // Refresh GUI and rebuild the slot map so future clicks resolve correctly
                    List<UUID> newSlotOrder = refreshInventory(event.getClickedInventory(), members, activeItems);
                    slotMap.put(player.getUniqueId(), newSlotOrder);
                }
            } else {
                // Inventory full
                var msgSvc = plugin.getService(fr.skynex.lootglow.service.MessageService.class);
                if (msgSvc != null) msgSvc.sendMessage(player, "inventory-full");
            }
        }
        
        event.setCancelled(true);
    }

    /**
     * Rebuilds the inventory contents from the current members list, skipping invalid items.
     * Returns the ordered list of UUIDs actually placed into GUI slots so that
     * {@link #slotMap} can be updated — ensuring slot N in the GUI always resolves to
     * the correct item UUID even when some members are temporarily invalid.
     *
     * Note: do NOT call members.removeIf() here.
     * Mutating the members list during a GUI refresh caused race conditions where the leader
     * entity could be briefly invalid (e.g. mid-tick teleport), pruning it from the list and
     * making the visual loot bag lose its reference, causing it to disappear randomly.
     * Invalid entries are simply skipped visually; the members list is only cleaned up
     * at the authoritative pick-up site (top of onInventoryClick).
     */
    private List<UUID> refreshInventory(Inventory inv, List<UUID> members, java.util.Map<UUID, Item> activeItems) {
        inv.clear();
        List<UUID> slotOrder = new ArrayList<>();
        for (UUID mUuid : members) {
            if (slotOrder.size() >= inv.getSize()) break;
            Item item = activeItems.get(mUuid);
            if (item != null && item.isValid() && !item.isDead()) {
                inv.setItem(slotOrder.size(), item.getItemStack());
                slotOrder.add(mUuid);
            }
        }
        return slotOrder;
    }

    /**
     * Builds the ordered list of valid item UUIDs without modifying any inventory.
     * Used for lazy initialisation of {@link #slotMap} on the first click.
     */
    private List<UUID> buildSlotList(List<UUID> members, java.util.Map<UUID, Item> activeItems) {
        List<UUID> slotOrder = new ArrayList<>();
        for (UUID mUuid : members) {
            Item item = activeItems.get(mUuid);
            if (item != null && item.isValid() && !item.isDead()) {
                slotOrder.add(mUuid);
            }
        }
        return slotOrder;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        var gcMgr = plugin.getService(fr.skynex.lootglow.managers.GroupContainerManager.class);
        var openContainers = gcMgr != null ? gcMgr.getOpenContainers() : plugin.getStateRepository().getOpenContainers();
        openContainers.remove(playerUuid);
        slotMap.remove(playerUuid);
    }
}
