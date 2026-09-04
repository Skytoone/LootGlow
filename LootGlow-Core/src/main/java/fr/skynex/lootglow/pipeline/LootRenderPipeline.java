package fr.skynex.lootglow.pipeline;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.entity.Item;

import java.util.UUID;

/**
 * Unified Render Pipeline for LootGlow.
 * Centralizes item evaluation, composite entity rendering (ItemDisplay, TextDisplay, Shadow, Beam),
 * spatial visibility distribution, and cleanup.
 */
public class LootRenderPipeline {

    private final LootGlow plugin;

    public LootRenderPipeline(LootGlow plugin) {
        this.plugin = plugin;
    }

    /**
     * Convenience method to render with default animations enabled.
     *
     * @param item The parent Bukkit Item entity.
     */
    public void render(Item item) {
        render(item, true);
    }

    /**
     * Executes the complete render pipeline for a newly dropped or spawned item entity.
     * Evaluates item properties, registers spatial tracking, spawns composite displays,
     * and broadcasts visibility.
     *
     * @param item          The parent Bukkit Item entity.
     * @param playAnimation Whether drop/pickup sounds and particle effects should trigger.
     */
    public void render(Item item, boolean playAnimation) {
        if (item == null || item.isDead()) return;
        var registry = plugin.getServiceRegistry();
        var applyService = registry != null ? registry.get(fr.skynex.lootglow.service.ItemGlowApplyService.class) : plugin.getItemGlowApplyService();
        if (applyService != null) {
            applyService.applyGlow(item, playAnimation, new fr.skynex.lootglow.model.ItemGlowContext(
                    plugin.isPluginEnabled(), plugin.getConfigManager().isEconomyEnabled(), plugin.getConfigManager().getEconomyKeys(), plugin.getConfigManager().getEconomyColor(), plugin.getConfigManager().getEconomySound(),
                    plugin.getStateRepository().getItemMoneyAmounts(), plugin.getItemCategories(), plugin.getCategoryNames(), plugin.getConfigManager().getDefaultColor(), plugin.getCategoryParticles(),
                    plugin.getItemParticlesCache(), plugin.getItemCategoriesCache(), plugin.getConfigManager().getDespawnTime(), plugin.getEntityIdMap(), plugin.getActiveItems(), plugin.getItemsByWorld(),
                    plugin.getConfigManager().isRpgDropsEnabled(), plugin.getConfigManager().getRpgEnabledCategories(), plugin.getConfigManager().getCategoryGlow(), plugin.getConfigManager().isDefaultGlow(), plugin.getHiddenVanillaItems(),
                    plugin.getCategorySounds(), plugin.getConfigManager().isHoloEnabled(), plugin.getConfigManager().isHoloHideUncategorized(), plugin.getItemSpawnTimes(), plugin.getBaseNameCache(),
                    plugin.getConfigManager().isProtectionEnabled(), plugin.getConfigManager().getProtectionDuration(), plugin.getConfigManager().isShadowsEnabled(), plugin.getConfigManager().isBeamsEnabled(), plugin.getConfigManager().getBeamCategories()
            ));
        } else {
            plugin.applyGlow(item, playAnimation);
        }
    }

    /**
     * Completely unrenders and untracks an item and all associated composite display entities.
     *
     * @param itemUuid The UUID of the parent Item entity.
     */
    public void unrender(UUID itemUuid) {
        if (itemUuid == null) return;
        plugin.removeGlow(itemUuid);
    }

    /**
     * Convenience method to unrender by Bukkit Item entity.
     *
     * @param item The parent Bukkit Item entity.
     */
    public void unrender(Item item) {
        if (item == null) return;
        unrender(item.getUniqueId());
    }

    /**
     * Triggers the global physics and position synchronization tick for all active composites.
     */
    public void tickSync() {
        if (!plugin.isPluginEnabled()) return;
        var registry = plugin.getServiceRegistry();
        var physicsService = registry != null ? registry.get(fr.skynex.lootglow.service.ItemPhysicsService.class) : plugin.getItemPhysicsService();
        if (physicsService != null) {
            physicsService.tickGlobalSync(
                    plugin.isPluginEnabled(),
                    plugin.getActiveItems(),
                    plugin.getTrackedItems(),
                    plugin.getConfigManager().getRpgBlockScale(),
                    plugin.getConfigManager().getRpgItemScale(),
                    plugin.getConfigManager().getBagMaterial(),
                    plugin.getGroupLeaders(),
                    plugin.getConfigManager().getHoloOffset(),
                    plugin.getConfigManager().getShadowScale(),
                    plugin.getConfigManager().getRpgRotation()
            );
        }
    }
}
