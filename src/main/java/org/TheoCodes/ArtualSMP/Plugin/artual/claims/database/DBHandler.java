package org.TheoCodes.ArtualSMP.Plugin.artual.claims.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;

public class DBHandler {

    private final JavaPlugin plugin;
    private HikariDataSource hikariDataSource;

    public DBHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        setupDatabase();
    }

    public void setupDatabase() {
        setupSQLite();
    }

    private void setupSQLite() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + "/claimsData.db");
        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(1);
        config.setPoolName("SQLitePool");

        hikariDataSource = new HikariDataSource(config);

        try (Connection conn = hikariDataSource.getConnection()) {
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Claims (" +
                    "ID TEXT PRIMARY KEY, " +
                    "OwnerUUID TEXT, " +
                    "ChunkID TEXT)");
            conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_owner_uuid ON Claims(OwnerUUID)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set up SQLite database: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return hikariDataSource != null ? hikariDataSource.getConnection() : null;
    }

    public void close() {
        if (hikariDataSource != null) {
            hikariDataSource.close();
        }
    }
}
