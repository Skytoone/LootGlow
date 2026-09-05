package fr.skynex.lootglow.commands;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles command execution and tab completion for /lootglow commands.
 */
public class LootGlowCommandManager implements CommandExecutor, TabCompleter {

    private final LootGlow plugin;

    public LootGlowCommandManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    private void sendMessage(CommandSender sender, String key) {
        var msgSvc = plugin.getService(fr.skynex.lootglow.service.MessageService.class);
        if (msgSvc != null) msgSvc.sendMessage(sender, key);
    }

    private void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        var msgSvc = plugin.getService(fr.skynex.lootglow.service.MessageService.class);
        if (msgSvc != null) msgSvc.sendMessage(sender, key, placeholders);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Map<String, String> info = new HashMap<>();
            info.put("version", plugin.getPluginMeta().getVersion());
            info.put("author", String.join(", ", plugin.getPluginMeta().getAuthors()));
            sendMessage(sender, "plugin-info", info);
            return true;
        }

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("lootglow.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                if (plugin.getConfigManager() != null) plugin.getConfigManager().loadConfiguration();
                sendMessage(sender, "config-reloaded");
                return true;
            } else if (args[0].equalsIgnoreCase("toggle") && sender instanceof Player p) {
                if (!p.hasPermission("lootglow.toggle")) {
                    sendMessage(p, "no-permission");
                    return true;
                }
                var stateRepo = plugin.getStateRepository();
                var db = plugin.getService(fr.skynex.lootglow.database.DatabaseManager.class);
                if (stateRepo != null && stateRepo.getHiddenVisuals().contains(p.getUniqueId())) {
                    stateRepo.getHiddenVisuals().remove(p.getUniqueId());
                    if (db != null) db.savePlayerData(p.getUniqueId(), true, stateRepo.getDisabledMagnets().contains(p.getUniqueId()));
                    sendMessage(p, "toggle-on");
                } else if (stateRepo != null) {
                    stateRepo.getHiddenVisuals().add(p.getUniqueId());
                    if (db != null) db.savePlayerData(p.getUniqueId(), false, stateRepo.getDisabledMagnets().contains(p.getUniqueId()));
                    sendMessage(p, "toggle-off");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("magnet") && sender instanceof Player p) {
                if (!p.hasPermission("lootglow.magnet")) {
                    sendMessage(p, "no-permission");
                    return true;
                }
                var stateRepo = plugin.getStateRepository();
                var db = plugin.getService(fr.skynex.lootglow.database.DatabaseManager.class);
                if (stateRepo != null && stateRepo.getDisabledMagnets().contains(p.getUniqueId())) {
                    stateRepo.getDisabledMagnets().remove(p.getUniqueId());
                    if (db != null) db.savePlayerData(p.getUniqueId(), !stateRepo.getHiddenVisuals().contains(p.getUniqueId()), false);
                    sendMessage(p, "magnet-on");
                } else if (stateRepo != null) {
                    stateRepo.getDisabledMagnets().add(p.getUniqueId());
                    if (db != null) db.savePlayerData(p.getUniqueId(), !stateRepo.getHiddenVisuals().contains(p.getUniqueId()), true);
                    sendMessage(p, "magnet-off");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("stats") && sender instanceof Player p) {
                var dbMgr = plugin.getService(fr.skynex.lootglow.database.DatabaseManager.class);
                if (dbMgr != null) {
                    dbMgr.getPlayerLootStats(p.getUniqueId(), stats -> {
                        sendMessage(p, "stats-header");
                        if (stats.isEmpty()) {
                            sendMessage(p, "stats-empty");
                        } else {
                            stats.forEach((cat, count) -> {
                                Map<String, String> ph = new HashMap<>();
                                ph.put("category", cat);
                                ph.put("count", String.valueOf(count));
                                sendMessage(p, "stats-line", ph);
                            });
                        }
                    });
                }
                return true;
            } else if (args[0].equalsIgnoreCase("top")) {
                String cat = args.length >= 2 ? args[1] : "ALL";
                var dbMgr = plugin.getService(fr.skynex.lootglow.database.DatabaseManager.class);
                if (dbMgr != null) {
                    dbMgr.getTopLooters(cat, 10, top -> {
                        Map<String, String> headerPh = new HashMap<>();
                        headerPh.put("category", cat.toUpperCase());
                        sendMessage(sender, "top-header", headerPh);
                        if (top.isEmpty()) {
                            sendMessage(sender, "top-empty");
                        } else {
                            int rank = 1;
                            for (fr.skynex.lootglow.database.DatabaseManager.LooterStat stat : top) {
                                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(UUID.fromString(stat.uuid()));
                                String name = op.getName() != null ? op.getName() : stat.uuid().substring(0, 8);
                                Map<String, String> ph = new HashMap<>();
                                ph.put("rank", String.valueOf(rank));
                                ph.put("name", name);
                                ph.put("count", String.valueOf(stat.count()));
                                sendMessage(sender, "top-line", ph);
                                rank++;
                            }
                        }
                    });
                }
                return true;
            } else if (args[0].equalsIgnoreCase("help")) {
                sendMessage(sender, "help-header");
                sendMessage(sender, "help-reload");
                sendMessage(sender, "help-toggle");
                sendMessage(sender, "help-magnet");
                sendMessage(sender, "help-stats");
                sendMessage(sender, "help-top");
                return true;
            }
        }
        sendMessage(sender, "help-header");
        sendMessage(sender, "help-reload");
        sendMessage(sender, "help-toggle");
        sendMessage(sender, "help-magnet");
        sendMessage(sender, "help-stats");
        sendMessage(sender, "help-top");
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
            var cfgMgr = plugin.getService(fr.skynex.lootglow.config.LootGlowConfigManager.class);
            List<String> list = cfgMgr != null ? new ArrayList<>(cfgMgr.getCategoryColors().keySet()) : new ArrayList<>();
            list.add("ALL");
            return list;
        }
        return Collections.emptyList();
    }
}
