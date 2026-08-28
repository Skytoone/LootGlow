package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicListener implements Listener {

    private final LootGlow plugin;

    public MythicListener(LootGlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicDeath(io.lumine.mythic.bukkit.events.MythicMobDeathEvent event) {
        String mobName = event.getMobType().getInternalName();
        org.bukkit.NamespacedKey key = plugin.getSourceMobKey();

        // Tag each item stack dropped by the mob if enabled in config (disabled by default to allow items to stack in inventory)
        if (plugin.getConfig().getBoolean("settings.mythicmobs.tag-itemstack", false)) {
            for (org.bukkit.inventory.ItemStack stack : event.getDrops()) {
                if (stack == null)
                    continue;
                org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, mobName);
                    stack.setItemMeta(meta);
                }
            }
        }
    }
}
