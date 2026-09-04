package fr.skynex.lootglow.event;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.api.events.LootGlowItemPickupEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Event Dispatcher for LootGlow domain events.
 * Decouples sound, actionbar notifications, and database stat tracking from Bukkit Event Listeners.
 */
public class LootEventDispatcher {

    private final LootGlow plugin;

    public LootEventDispatcher(LootGlow plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles item pickup events, recording stats and triggering WoW-style actionbar notifications.
     */
    public boolean handleItemPickup(Player player, Item item, ItemStack itemStack, String category) {
        LootGlowItemPickupEvent apiEvent = new LootGlowItemPickupEvent(player, item, itemStack, category);
        Bukkit.getPluginManager().callEvent(apiEvent);
        if (apiEvent.isCancelled()) {
            return false;
        }

        // Record loot statistics in Database
        if (plugin.getDatabaseManager() != null) {
            plugin.getDatabaseManager().incrementLootStat(player.getUniqueId(), category != null ? category : "DEFAULT", itemStack.getAmount());
        }

        // Trigger actionbar notification & pickup sound
        triggerPickupActionBar(player, item, itemStack, category);
        return true;
    }

    /**
     * Triggers configurable actionbar notifications and sounds on item pickup.
     */
    private void triggerPickupActionBar(Player player, Item item, ItemStack itemStack, String category) {
        if (!plugin.getConfig().getBoolean("settings.wow-effects.enabled", true) ||
            !plugin.getConfig().getBoolean("settings.wow-effects.pickup-actionbar.enabled", true)) {
            return;
        }

        String itemCat = category != null ? category : (plugin.getTrackedItemManager() != null ? plugin.getTrackedItemManager().getItemCategory(item.getUniqueId()) : null);
        if (itemCat == null) return;

        List<String> enabledCategories = plugin.getConfig().getStringList("settings.wow-effects.pickup-actionbar.categories");
        if (!enabledCategories.isEmpty() && enabledCategories.stream().noneMatch(c -> c.equalsIgnoreCase(itemCat))) {
            return;
        }

        String format = plugin.getConfig().getString("settings.wow-effects.pickup-actionbar.format", "<gradient:#FF0055:#FF8800><b>✦ BUTIN <category> ✦</b></gradient> <gray>—</gray> <white><item></white>");
        Component itemComp = plugin.getItemNameFormatter() != null ? plugin.getItemNameFormatter().getItemName(itemStack) : Component.text(itemStack.getType().name());
        Component headerComp = fr.skynex.lootglow.util.ColorUtil.parse(format.replace("<category>", itemCat.toUpperCase()));
        Component actionbarComp = headerComp.replaceText(b -> b.matchLiteral("<item>").replacement(itemComp));
        player.sendActionBar(actionbarComp);

        String soundStr = plugin.getConfig().getString("settings.wow-effects.pickup-actionbar.sound", "UI_TOAST_CHALLENGE_COMPLETE");
        Sound sound = plugin.parseSound(soundStr);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 0.8f, 1.2f);
        }
    }
}
