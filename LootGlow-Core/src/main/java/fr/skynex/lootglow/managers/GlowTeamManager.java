package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Bukkit Scoreboard teams for item glowing colors.
 */
public class GlowTeamManager {

    private final LootGlow plugin;

    public GlowTeamManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void applyGlow(Item item) {
        if (item == null || !item.isValid()) return;
        item.setGlowing(true);
    }

    public void removeGlow(Item item) {
        if (item == null || !item.isValid()) return;
        item.setGlowing(false);
    }

    public void setupTeams() {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            for (net.kyori.adventure.text.format.NamedTextColor color : net.kyori.adventure.text.format.NamedTextColor.NAMES.values()) {
                String teamName = "LG_" + color.toString().toUpperCase();
                Team team = scoreboard.getTeam(teamName);
                if (team == null)
                    team = scoreboard.registerNewTeam(teamName);
                team.color(color);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to setup LootGlow scoreboard teams (Scoreboard team registration may not be supported on this server software): " + t.getMessage());
        }
    }

    public void clearScoreboardTeams() {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            for (Team team : scoreboard.getTeams()) {
                if (team.getName().startsWith("LG_")) {
                    for (String entry : new java.util.ArrayList<>(team.getEntries())) {
                        team.removeEntry(entry);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to clear LootGlow scoreboard teams: " + e.getMessage());
        }
    }
}
