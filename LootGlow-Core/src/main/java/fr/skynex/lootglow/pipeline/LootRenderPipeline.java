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
        var applyService = plugin.getService(fr.skynex.lootglow.service.ItemGlowApplyService.class);
        if (applyService != null) {
            applyService.applyGlow(item, playAnimation, fr.skynex.lootglow.model.ItemGlowContext.from(plugin));
        }
    }

    /**
     * Completely unrenders and untracks an item and all associated composite display entities.
     *
     * @param itemUuid The UUID of the parent Item entity.
     */
    public void unrender(UUID itemUuid) {
        if (itemUuid == null) return;
        var spawner = plugin.getService(fr.skynex.lootglow.managers.VisualSpawner.class);
        if (spawner != null) spawner.removeGlow(itemUuid);
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
        var physicsService = plugin.getService(fr.skynex.lootglow.service.ItemPhysicsService.class);
        if (physicsService != null) {
            var cfg = plugin.getConfigManager();
            var repo = plugin.getStateRepository();
            physicsService.tickGlobalSync(
                    plugin.isPluginEnabled(),
                    repo.getActiveItems(),
                    repo.getTrackedItems(),
                    cfg.getRpgBlockScale(),
                    cfg.getRpgItemScale(),
                    cfg.getBagMaterial(),
                    repo.getGroupLeaders(),
                    cfg.getHoloOffset(),
                    cfg.getShadowScale(),
                    cfg.getRpgRotation()
            );
        }
    }
}

