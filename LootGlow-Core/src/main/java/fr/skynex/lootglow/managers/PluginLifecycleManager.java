package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.listeners.FarmingListener;
import fr.skynex.lootglow.listeners.FishingListener;
import fr.skynex.lootglow.listeners.ItemListener;
import fr.skynex.lootglow.listeners.LootContainerListener;
import fr.skynex.lootglow.model.TrackedItem;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles plugin startup lifecycle steps: listener registration, command registration, and state resets.
 */
public class PluginLifecycleManager {

    private final LootGlow plugin;

    public PluginLifecycleManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void registerListeners(boolean useMythic) {
        plugin.getServer().getPluginManager().registerEvents(new ItemListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new FarmingListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new LootContainerListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new FishingListener(plugin), plugin);
        if (useMythic) {
            try {
                plugin.getServer().getPluginManager().registerEvents(new fr.skynex.lootglow.listeners.MythicListener(plugin), plugin);
            } catch (NoClassDefFoundError ignored) {}
        }
    }

    public void registerCommands() {
        // 1. Try Paper 1.21+ Lifecycle API
        try {
            plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                final var registrar = event.registrar();
                registrar.register("lootglow", "Main command for LootGlow", List.of("lg", "glow", "loot"),
                        new BasicCommand() {
                            @Override
                            public void execute(CommandSourceStack stack, String[] args) {
                                plugin.onCommand(stack.getSender(), null, "lootglow", args);
                            }

                            @Override
                            public java.util.Collection<String> suggest(CommandSourceStack stack, String[] args) {
                                return plugin.onTabComplete(stack.getSender(), null, "lootglow", args);
                            }
                        });
            });
        } catch (Throwable ignored) {}

        // 2. Standard Bukkit plugin.yml command binding or dynamic fallback
        try {
            org.bukkit.command.PluginCommand cmd = plugin.getCommand("lootglow");
            if (cmd != null && plugin.getCommandManager() != null) {
                cmd.setExecutor(plugin.getCommandManager());
                cmd.setTabCompleter(plugin.getCommandManager());
            } else {
                registerDynamicCommand("lootglow", List.of("lg", "glow", "loot"));
            }
        } catch (Throwable t) {
            // Paper plugin loader throws UnsupportedOperationException for JavaPlugin#getCommand
            registerDynamicCommand("lootglow", List.of("lg", "glow", "loot"));
        }
    }

    private void registerDynamicCommand(String name, List<String> aliases) {
        try {
            java.lang.reflect.Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) field.get(Bukkit.getServer());

            if (commandMap != null) {
                org.bukkit.command.Command dynCmd = new org.bukkit.command.Command(name, "Main command for LootGlow", "/" + name, aliases) {
                    @Override
                    public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                        return plugin.onCommand(sender, this, commandLabel, args);
                    }

                    @Override
                    public List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                        List<String> completions = plugin.onTabComplete(sender, this, alias, args);
                        return completions != null ? completions : super.tabComplete(sender, alias, args);
                    }
                };
                commandMap.register("lootglow", dynCmd);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to dynamically register /" + name + " command: " + t.getMessage());
        }
    }

    public void resetStateOnReload() {
        if (plugin.getTrackedItemManager() != null) {
            plugin.getTrackedItemManager().clearAll();
        } else {
            for (TrackedItem ti : plugin.getTrackedItems().values()) {
                if (ti.label != null && ti.label.isValid()) ti.label.remove();
                if (ti.beam != null && ti.beam.isValid()) {
                    ti.beam.getPassengers().forEach(e -> { if (e != null) e.remove(); });
                    ti.beam.remove();
                }
                if (ti.visual != null && ti.visual.isValid()) ti.visual.remove();
                if (ti.shadow != null && ti.shadow.isValid()) ti.shadow.remove();
            }
            plugin.getTrackedItems().clear();
            plugin.getActiveItems().clear();
            plugin.getItemsByWorld().clear();
            plugin.getEntityIdMap().clear();
        }

        plugin.getHiddenVanillaItems().clear();
        plugin.getItemSpawnTimes().clear();
        plugin.getItemCategories().clear();
        plugin.getCategoryParticles().clear();
        plugin.getCategorySounds().clear();
        plugin.getCategoryNames().clear();

        if (plugin.getConfigManager() != null) {
            plugin.getConfigManager().getCategoryGlow().clear();
            plugin.getConfigManager().getFilteredWorlds().clear();
        }
        plugin.getCategoryColors().clear();
        plugin.getDisplayNameOverridesCache().clear();
        plugin.getCategoryLights().clear();

        plugin.getActiveLights().forEach((uuid, loc) -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(loc.getWorld())) {
                    p.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }
        });
        plugin.getActiveLights().clear();

        plugin.getActiveCropSymbols().values().forEach(list -> list.forEach(d -> {
            if (d != null && d.isValid()) d.remove();
        }));
        plugin.getActiveCropSymbols().clear();

        plugin.getVisibleEntities().clear();
        plugin.getHiddenVisuals().clear();
        plugin.getDisabledMagnets().clear();
        plugin.getCategoryDustOptions().clear();

        if (plugin.getSurfaceAlignmentManager() != null) {
            plugin.getSurfaceAlignmentManager().clearAll();
        }
        plugin.getLastFarmingScanLocations().clear();

        plugin.getGloballyVisibleEntities().clear();

        if (plugin.getGroupContainerManager() != null) {
            plugin.getGroupContainerManager().clearAll();
        }
        plugin.getGroupMembers().clear();
        plugin.getGroupedItems().clear();
        plugin.getOpenContainers().clear();

        if (plugin.getBeamManager() != null) {
            plugin.getBeamManager().clearAll();
        }
        if (plugin.getParticleAnimationManager() != null) {
            plugin.getParticleAnimationManager().getCustomParticles().clear();
        }
        if (plugin.getHologramRenderer() != null) {
            plugin.getHologramRenderer().getCustomHolograms().clear();
        }

        if (plugin.getSpatialIndexService() != null) {
            plugin.getSpatialIndexService().clearAll();
        }
        plugin.getRecentlyBounced().clear();
        plugin.getBounceCounts().clear();
    }
}
