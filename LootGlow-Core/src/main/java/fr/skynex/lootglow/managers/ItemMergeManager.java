package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.api.events.ItemMergeEvent;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Manages item stack merging, splitting, loot protection safety checks, and auto-stacking logic.
 */
public class ItemMergeManager {

    private final LootGlow plugin;
    private boolean autoStackEnabled = false;
    private double autoStackDistance = 3.0;
    private BukkitTask autoStackTask;

    public ItemMergeManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        this.autoStackEnabled = plugin.getConfig().getBoolean("settings.AutoStack", plugin.getConfig().getBoolean("AutoStack", false));
        this.autoStackDistance = plugin.getConfig().getDouble("settings.AutoStackDistance", plugin.getConfig().getDouble("AutoStackDistance", 3.0));
        restartAutoStackTask();
    }

    public boolean isAutoStackEnabled() {
        return autoStackEnabled;
    }

    public double getAutoStackDistance() {
        return autoStackDistance;
    }

    public void restartAutoStackTask() {
        if (autoStackTask != null) {
            autoStackTask.cancel();
            autoStackTask = null;
        }

        if (autoStackEnabled) {
            autoStackTask = FoliaScheduler.runTimer(plugin, this::processAutoStack, 20L, 20L);
        }
    }

    /**
     * Checks if two items can be merged based on ItemStack similarity and loot protection matching.
     */
    public boolean canMerge(@NotNull Item item1, @NotNull Item item2) {
        if (item1 == null || item2 == null) return false;
        if (!item1.isValid() || !item2.isValid()) return false;
        if (item1.equals(item2)) return false;
        if (item1.isDead() || item2.isDead()) return false;

        ItemStack stack1 = item1.getItemStack();
        ItemStack stack2 = item2.getItemStack();
        if (stack1.getAmount() >= stack1.getMaxStackSize() && stack2.getAmount() >= stack2.getMaxStackSize()) {
            return false;
        }

        if (!stack1.isSimilar(stack2)) return false;

        // Check loot protection matching
        LootProtectionManager lpm = plugin.getLootProtectionManager();
        if (lpm != null) {
            boolean prot1 = lpm.isLootProtected(item1);
            boolean prot2 = lpm.isLootProtected(item2);

            if (prot1 || prot2) {
                if (prot1 != prot2) return false;

                UUID owner1 = lpm.getLootOwner(item1);
                UUID owner2 = lpm.getLootOwner(item2);
                if (!Objects.equals(owner1, owner2)) return false;

                Set<UUID> sharers1 = lpm.getLootSharers(item1);
                Set<UUID> sharers2 = lpm.getLootSharers(item2);
                if (!Objects.equals(sharers1, sharers2)) return false;
            }
        }

        return true;
    }

    /**
     * Merges item2 into item1 if allowed.
     */
    public boolean mergeAmount(@NotNull Item item1, @NotNull Item item2) {
        if (!canMerge(item1, item2)) return false;

        ItemMergeEvent event = new ItemMergeEvent(item2, item1);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        ItemStack stack1 = item1.getItemStack();
        ItemStack stack2 = item2.getItemStack();

        int amount1 = stack1.getAmount();
        int amount2 = stack2.getAmount();
        int maxStack = stack1.getMaxStackSize();
        int total = amount1 + amount2;

        if (total <= maxStack) {
            stack1.setAmount(total);
            item1.setItemStack(stack1);
            item2.remove();
        } else {
            stack1.setAmount(maxStack);
            item1.setItemStack(stack1);
            stack2.setAmount(total - maxStack);
            item2.setItemStack(stack2);
        }

        plugin.getLastHoloState().remove(item1.getUniqueId());
        plugin.getBaseNameCache().remove(item1.getUniqueId());

        return true;
    }

    /**
     * Splits a specified amount from a dropped item entity.
     */
    public boolean unMergeAmount(@NotNull Item item, int amount) {
        if (item == null || !item.isValid() || amount <= 0) return false;
        ItemStack stack = item.getItemStack();
        int currentAmount = stack.getAmount();
        if (amount >= currentAmount) return false;

        stack.setAmount(currentAmount - amount);
        item.setItemStack(stack);

        plugin.getLastHoloState().remove(item.getUniqueId());
        plugin.getBaseNameCache().remove(item.getUniqueId());

        ItemStack splitStack = stack.clone();
        splitStack.setAmount(amount);

        Item splitItem = item.getWorld().dropItem(item.getLocation(), splitStack);

        LootProtectionManager lpm = plugin.getLootProtectionManager();
        if (lpm != null && lpm.isLootProtected(item)) {
            UUID owner = lpm.getLootOwner(item);
            if (owner != null) {
                lpm.setLootProtection(splitItem, owner, -1L);
                for (UUID sharer : lpm.getLootSharers(item)) {
                    lpm.addLootSharer(splitItem, sharer);
                }
            }
        }

        return true;
    }

    public int getMergeAmount(@NotNull Item item) {
        if (item == null || !item.isValid()) return 0;
        return item.getItemStack().getAmount();
    }

    /**
     * Ticker logic for automatic item stacking using spatial entity indexing.
     */
    private void processAutoStack() {
        if (!autoStackEnabled || plugin.getTrackedItemManager() == null) return;

        double dist = autoStackDistance;
        double distSq = dist * dist;
        TrackedItemManager tim = plugin.getTrackedItemManager();
        Map<UUID, Item> activeItems = tim.getActiveItems();

        for (World world : Bukkit.getWorlds()) {
            Map<Long, Set<UUID>> worldChunks = tim.getItemsByChunk().get(world.getName());
            if (worldChunks == null || worldChunks.isEmpty()) continue;

            Set<Long> processedPairs = new HashSet<>();

            for (Map.Entry<Long, Set<UUID>> chunkEntry : worldChunks.entrySet()) {
                Set<UUID> chunkItemUuids = chunkEntry.getValue();
                if (chunkItemUuids == null || chunkItemUuids.isEmpty()) continue;

                long chunkKey = chunkEntry.getKey();
                int cX = (int) (chunkKey >> 32);
                int cZ = (int) (chunkKey & 0xFFFFFFFFL);

                Set<UUID> candidateUuids = tim.getItemsInChunkRadius(world, cX, cZ, 1);
                if (candidateUuids.size() < 2) continue;

                List<Item> candidateItems = new ArrayList<>();
                for (UUID u : candidateUuids) {
                    Item it = activeItems.get(u);
                    if (it != null && it.isValid() && !it.isDead()) {
                        candidateItems.add(it);
                    }
                }

                for (int i = 0; i < candidateItems.size(); i++) {
                    Item item1 = candidateItems.get(i);
                    for (int j = i + 1; j < candidateItems.size(); j++) {
                        Item item2 = candidateItems.get(j);
                        if (!item1.isValid() || item1.isDead() || !item2.isValid() || item2.isDead()) continue;
                        if (!item1.getWorld().equals(item2.getWorld())) continue;

                        long pairHash = (((long) Math.min(item1.getEntityId(), item2.getEntityId())) << 32)
                                | (Math.max(item1.getEntityId(), item2.getEntityId()) & 0xFFFFFFFFL);
                        if (!processedPairs.add(pairHash)) continue;

                        if (item1.getLocation().distanceSquared(item2.getLocation()) <= distSq) {
                            if (canMerge(item1, item2)) {
                                mergeAmount(item1, item2);
                            }
                        }
                    }
                }
            }
        }
    }

    public void clearAll() {
        if (autoStackTask != null) {
            autoStackTask.cancel();
            autoStackTask = null;
        }
    }
}
