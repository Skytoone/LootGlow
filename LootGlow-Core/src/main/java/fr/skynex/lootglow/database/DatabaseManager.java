package fr.skynex.lootglow.database;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
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
