package fr.skynex.lootglow.database;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles SQLite database initialization, connection lifecycle, and player preferences persistence.
 */
public class DatabaseManager {

    private final LootGlow plugin;
    private final Logger logger;
    private Connection dbConnection;

    public DatabaseManager(LootGlow plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void initDatabase() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement s = dbConnection.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS player_settings (uuid TEXT PRIMARY KEY, hidden_visuals INTEGER, magnet_disabled INTEGER DEFAULT 0)");
                try {
                    s.execute("ALTER TABLE player_settings ADD COLUMN magnet_disabled INTEGER DEFAULT 0");
                } catch (SQLException ignored) {
                }
                s.execute("CREATE TABLE IF NOT EXISTS player_loot_stats (uuid TEXT, category TEXT, count INTEGER DEFAULT 0, PRIMARY KEY (uuid, category))");
            }
        } catch (SQLException e) {
            logger.severe("Could not initialize SQLite database: " + e.getMessage());
        }
    }

    public void closeDatabase() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException e) {
            logger.severe("Error closing SQLite database connection: " + e.getMessage());
        }
    }

    public record LooterStat(String uuid, String category, int count) {}

    public void incrementLootStat(UUID uuid, String category, int amount) {
        if (uuid == null || category == null || amount <= 0) return;
        String catUpper = category.toUpperCase();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (dbConnection == null) return;
            synchronized (dbConnection) {
                try (PreparedStatement ps = dbConnection.prepareStatement(
                        "INSERT INTO player_loot_stats (uuid, category, count) VALUES (?, ?, ?) " +
                                "ON CONFLICT(uuid, category) DO UPDATE SET count = count + ?")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, catUpper);
                    ps.setInt(3, amount);
                    ps.setInt(4, amount);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.severe("Could not increment loot stat for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    public void getPlayerLootStats(UUID uuid, java.util.function.Consumer<Map<String, Integer>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Integer> stats = new java.util.HashMap<>();
            if (dbConnection != null) {
                synchronized (dbConnection) {
                    try (PreparedStatement ps = dbConnection.prepareStatement(
                            "SELECT category, count FROM player_loot_stats WHERE uuid = ?")) {
                        ps.setString(1, uuid.toString());
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                stats.put(rs.getString("category"), rs.getInt("count"));
                            }
                        }
                    } catch (SQLException e) {
                        logger.severe("Could not load loot stats for " + uuid + ": " + e.getMessage());
                    }
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(stats));
        });
    }

    public void getTopLooters(String category, int limit, java.util.function.Consumer<List<LooterStat>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LooterStat> top = new java.util.ArrayList<>();
            if (dbConnection != null) {
                synchronized (dbConnection) {
                    String query = category != null && !category.equalsIgnoreCase("ALL")
                            ? "SELECT uuid, category, count FROM player_loot_stats WHERE category = ? ORDER BY count DESC LIMIT ?"
                            : "SELECT uuid, 'ALL' AS category, SUM(count) AS count FROM player_loot_stats GROUP BY uuid ORDER BY count DESC LIMIT ?";
                    try (PreparedStatement ps = dbConnection.prepareStatement(query)) {
                        if (category != null && !category.equalsIgnoreCase("ALL")) {
                            ps.setString(1, category.toUpperCase());
                            ps.setInt(2, limit);
                        } else {
                            ps.setInt(1, limit);
                        }
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                top.add(new LooterStat(rs.getString("uuid"), rs.getString("category"), rs.getInt("count")));
                            }
                        }
                    } catch (SQLException e) {
                        logger.severe("Could not fetch top looters: " + e.getMessage());
                    }
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(top));
        });
    }

    public void loadPlayerData(Player player, Set<UUID> hiddenVisuals, Set<UUID> disabledMagnets) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (dbConnection == null) return;
            synchronized (dbConnection) {
                try (PreparedStatement ps = dbConnection.prepareStatement(
                        "SELECT hidden_visuals, magnet_disabled FROM player_settings WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            boolean hidden = rs.getInt("hidden_visuals") == 1;
                            boolean magDisabled = rs.getInt("magnet_disabled") == 1;
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (hidden) hiddenVisuals.add(uuid);
                                if (magDisabled) disabledMagnets.add(uuid);
                            });
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("Could not load player data for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    public void savePlayerData(UUID uuid, boolean hidden, boolean magDisabled) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (dbConnection == null) return;
            synchronized (dbConnection) {
                try (PreparedStatement ps = dbConnection.prepareStatement(
                        "INSERT OR REPLACE INTO player_settings (uuid, hidden_visuals, magnet_disabled) VALUES (?, ?, ?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, hidden ? 1 : 0);
                    ps.setInt(3, magDisabled ? 1 : 0);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.severe("Could not save player data for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }
}
