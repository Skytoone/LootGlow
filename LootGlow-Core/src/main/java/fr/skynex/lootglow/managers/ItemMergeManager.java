package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Manages item stack merging, splitting, loot protection safety checks, and auto-stacking logic.
 */
public class ItemMergeManager {

    private final LootGlow plugin;
    private boolean autoStackEnabled = false;
    private double autoStackDistance = 3.0;
    private boolean ignoreAllUuidKeys = true;
    private final Set<String> ignoredPdcKeys = new HashSet<>();
    private BukkitTask autoStackTask;

    public ItemMergeManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        this.autoStackEnabled = plugin.getConfig().getBoolean("settings.AutoStack", plugin.getConfig().getBoolean("AutoStack", false));
        this.autoStackDistance = plugin.getConfig().getDouble("settings.AutoStackDistance", plugin.getConfig().getDouble("AutoStackDistance", 3.0));
        this.ignoreAllUuidKeys = plugin.getConfig().getBoolean("settings.ignore-uuid-pdc-on-merge", true);
        this.ignoredPdcKeys.clear();
        for (String k : plugin.getConfig().getStringList("settings.ignore-pdc-keys-on-merge")) {
            this.ignoredPdcKeys.add(k.toLowerCase());
        }
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

        if (!isSimilarIgnorePdc(stack1, stack2)) return false;

        // Check loot protection matching
        LootProtectionManager lpm = plugin.getService(LootProtectionManager.class);
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

    public boolean isIgnoreAllUuidKeys() {
        return ignoreAllUuidKeys;
    }

    public Set<String> getIgnoredPdcKeys() {
        return Collections.unmodifiableSet(ignoredPdcKeys);
    }

    public boolean isSimilarIgnorePdc(@NotNull ItemStack stack1, @NotNull ItemStack stack2) {
        if (stack1.getType() != stack2.getType()) return false;
        if (!stack1.hasItemMeta() && !stack2.hasItemMeta()) return true;
        if (stack1.hasItemMeta() != stack2.hasItemMeta()) return false;

        org.bukkit.inventory.meta.ItemMeta meta1 = stack1.getItemMeta().clone();
        org.bukkit.inventory.meta.ItemMeta meta2 = stack2.getItemMeta().clone();

        org.bukkit.persistence.PersistentDataContainer pdc1 = meta1.getPersistentDataContainer();
        org.bukkit.persistence.PersistentDataContainer pdc2 = meta2.getPersistentDataContainer();

        filterPdcKeys(pdc1);
        filterPdcKeys(pdc2);

        return Objects.equals(meta1, meta2);
    }

    private void filterPdcKeys(@NotNull org.bukkit.persistence.PersistentDataContainer pdc) {
        for (org.bukkit.NamespacedKey key : new ArrayList<>(pdc.getKeys())) {
            if (shouldIgnorePdcKey(key)) {
                pdc.remove(key);
            }
        }
    }

    private boolean shouldIgnorePdcKey(@NotNull org.bukkit.NamespacedKey key) {
        String keyName = key.getKey().toLowerCase();
        String fullKey = key.toString().toLowerCase();

        if (ignoredPdcKeys.contains(keyName) || ignoredPdcKeys.contains(fullKey)) {
            return true;
        }

        if (ignoreAllUuidKeys) {
            if (keyName.contains("uuid") || keyName.contains("guid")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Merges item2 into item1 if allowed.
     */
    public boolean mergeAmount(@NotNull Item item1, @NotNull Item item2) {
        if (!canMerge(item1, item2)) return false;
        ItemStack stack1 = item1.getItemStack();
        ItemStack stack2 = item2.getItemStack();

        int maxStack = stack1.getMaxStackSize();
        int total = stack1.getAmount() + stack2.getAmount();

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

        plugin.getStateRepository().getLastHoloState().remove(item1.getUniqueId());
        plugin.getStateRepository().getBaseNameCache().remove(item1.getUniqueId());

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

        plugin.getStateRepository().getLastHoloState().remove(item.getUniqueId());
        plugin.getStateRepository().getBaseNameCache().remove(item.getUniqueId());

        ItemStack splitStack = stack.clone();
        splitStack.setAmount(amount);

        Item splitItem = item.getWorld().dropItem(item.getLocation(), splitStack);

        LootProtectionManager lpm = plugin.getService(LootProtectionManager.class);
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

    public void setMergeAmount(@NotNull Item item, int amount) {
        if (item == null || !item.isValid()) return;
        if (amount <= 0) {
            item.remove();
            return;
        }
        ItemStack stack = item.getItemStack();
        stack.setAmount(amount);
        item.setItemStack(stack);

        plugin.getStateRepository().getLastHoloState().remove(item.getUniqueId());
        plugin.getStateRepository().getBaseNameCache().remove(item.getUniqueId());
    }

    public void addMergeAmount(@NotNull Item item, int amount) {
        if (item == null || !item.isValid() || amount <= 0) return;
        setMergeAmount(item, getMergeAmount(item) + amount);
    }

    public void removeMergeAmount(@NotNull Item item, int amount) {
        if (item == null || !item.isValid() || amount <= 0) return;
        setMergeAmount(item, getMergeAmount(item) - amount);
    }

    /**
     * Ticker logic for automatic item stacking using spatial chunk bucketing.
     */
    private void processAutoStack() {
        if (!autoStackEnabled) return;

        double dist = autoStackDistance;
        double distSq = dist * dist;
        int chunkRadius = (int) Math.ceil(dist / 16.0);

        for (World world : Bukkit.getWorlds()) {
            Collection<Item> items = world.getEntitiesByClass(Item.class);
            if (items.size() < 2) continue;

            Map<Long, List<Item>> chunkMap = new HashMap<>();
            for (Item item : items) {
                if (item != null && item.isValid() && !item.isDead()) {
                    int cx = item.getLocation().getBlockX() >> 4;
                    int cz = item.getLocation().getBlockZ() >> 4;
                    long key = TrackedItemManager.getChunkKey(cx, cz);
                    chunkMap.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
                }
            }

            Set<Long> processedPairs = new HashSet<>();
            for (Map.Entry<Long, List<Item>> entry : chunkMap.entrySet()) {
                long chunkKey = entry.getKey();
                int cX = (int) (chunkKey >> 32);
                int cZ = (int) (chunkKey & 0xFFFFFFFFL);

                List<Item> candidates = new ArrayList<>();
                for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                    for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                        long nKey = TrackedItemManager.getChunkKey(cX + dx, cZ + dz);
                        List<Item> nItems = chunkMap.get(nKey);
                        if (nItems != null) {
                            candidates.addAll(nItems);
                        }
                    }
                }

                if (candidates.size() < 2) continue;

                for (int i = 0; i < candidates.size(); i++) {
                    Item item1 = candidates.get(i);
                    for (int j = i + 1; j < candidates.size(); j++) {
                        Item item2 = candidates.get(j);
                        if (!item1.isValid() || item1.isDead() || !item2.isValid() || item2.isDead()) continue;

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
