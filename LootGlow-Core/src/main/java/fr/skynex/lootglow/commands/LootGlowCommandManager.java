package fr.skynex.lootglow.commands;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Handles command execution and tab completion for /lootglow commands.
 */
public class LootGlowCommandManager implements CommandExecutor, TabCompleter {

    private final LootGlow plugin;

    public LootGlowCommandManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Map<String, String> info = new HashMap<>();
            info.put("version", plugin.getPluginMeta().getVersion());
            info.put("author", String.join(", ", plugin.getPluginMeta().getAuthors()));
            plugin.sendMessage(sender, "plugin-info", info);
            return true;
        }

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("lootglow.admin")) {
                    plugin.sendMessage(sender, "no-permission");
                    return true;
                }
                plugin.loadConfiguration();
                plugin.sendMessage(sender, "config-reloaded");
                return true;
            } else if (args[0].equalsIgnoreCase("toggle") && sender instanceof Player p) {
                if (!p.hasPermission("lootglow.toggle")) {
                    plugin.sendMessage(p, "no-permission");
                    return true;
                }
                if (plugin.getHiddenVisuals().contains(p.getUniqueId())) {
                    plugin.getHiddenVisuals().remove(p.getUniqueId());
                    plugin.savePlayerData(p.getUniqueId());
                    plugin.refreshGlowForPlayer(p, true);
                    plugin.sendMessage(p, "toggle-on");
                } else {
                    plugin.getHiddenVisuals().add(p.getUniqueId());
                    plugin.savePlayerData(p.getUniqueId());
                    plugin.refreshGlowForPlayer(p, false);
                    plugin.sendMessage(p, "toggle-off");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("magnet") && sender instanceof Player p) {
                if (!p.hasPermission("lootglow.magnet")) {
                    plugin.sendMessage(p, "no-permission");
                    return true;
                }
                if (plugin.getDisabledMagnets().contains(p.getUniqueId())) {
                    plugin.getDisabledMagnets().remove(p.getUniqueId());
                    plugin.savePlayerData(p.getUniqueId());
                    plugin.sendMessage(p, "magnet-on");
                } else {
                    plugin.getDisabledMagnets().add(p.getUniqueId());
                    plugin.savePlayerData(p.getUniqueId());
                    plugin.sendMessage(p, "magnet-off");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("stats") && sender instanceof Player p) {
                plugin.getDatabaseManager().getPlayerLootStats(p.getUniqueId(), stats -> {
                    sender.sendMessage("§e=== §6LootGlow Stats §e===");
                    if (stats.isEmpty()) {
                        sender.sendMessage("§7Aucune statistique de loot enregistrée.");
                    } else {
                        stats.forEach((cat, count) -> sender.sendMessage("§8• §f" + cat + ": §a" + count + " items"));
                    }
                });
                return true;
            } else if (args[0].equalsIgnoreCase("top")) {
                String cat = args.length >= 2 ? args[1] : "ALL";
                plugin.getDatabaseManager().getTopLooters(cat, 10, top -> {
                    sender.sendMessage("§e=== §6Top Looters (" + cat.toUpperCase() + ") §e===");
                    if (top.isEmpty()) {
                        sender.sendMessage("§7Aucun joueur dans le classement.");
                    } else {
                        int rank = 1;
                        for (fr.skynex.lootglow.database.DatabaseManager.LooterStat stat : top) {
                            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(UUID.fromString(stat.uuid()));
                            String name = op.getName() != null ? op.getName() : stat.uuid().substring(0, 8);
                            sender.sendMessage("§e#" + rank + " §f" + name + " §8- §a" + stat.count() + " loots");
                            rank++;
                        }
                    }
                });
                return true;
            } else if (args[0].equalsIgnoreCase("help")) {
                plugin.sendMessage(sender, "help-header");
                plugin.sendMessage(sender, "help-reload");
                plugin.sendMessage(sender, "help-toggle");
                plugin.sendMessage(sender, "help-magnet");
                sender.sendMessage("§8• §e/lg stats §7- Affiche vos statistiques de loot");
                sender.sendMessage("§8• §e/lg top [categorie] §7- Affiche le classement des meilleurs looteurs");
                return true;
            }
        }
        plugin.sendMessage(sender, "help-header");
        plugin.sendMessage(sender, "help-reload");
        plugin.sendMessage(sender, "help-toggle");
        plugin.sendMessage(sender, "help-magnet");
        sender.sendMessage("§8• §e/lg stats §7- Affiche vos statistiques de loot");
        sender.sendMessage("§8• §e/lg top [categorie] §7- Affiche le classement des meilleurs looteurs");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            if (sender.hasPermission("lootglow.admin")) list.add("reload");
            if (sender.hasPermission("lootglow.toggle")) list.add("toggle");
            if (sender.hasPermission("lootglow.magnet")) list.add("magnet");
            list.add("stats");
            list.add("top");
            list.add("help");
            return list;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            List<String> list = new ArrayList<>(plugin.getConfigManager().getCategoryColors().keySet());
            list.add("ALL");
            return list;
        }
        return Collections.emptyList();
    }
}
