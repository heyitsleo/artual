package org.TheoCodes.ArtualSMP.Plugin.artual.claims.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bson.Document;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;

public class DBHandler {

    private final JavaPlugin plugin;
    private final String databaseType;
    private HikariDataSource hikariDataSource;
    private MongoClient mongoClient;
    private MongoCollection<Document> claimsCollection;
    private MongoCollection<Document> trustedCollection;

    public DBHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databaseType = plugin.getConfig().getString("Database.Database-Type", "sqlite").toLowerCase();
        setupDatabase();
    }

    public void setupDatabase() {
        switch (databaseType) {
            case "sqlite":
                setupSQLite();
                break;
            case "mysql":
                setupMySQL();
                break;
            case "mongodb":
                setupMongoDB();
                break;
            default:
                throw new IllegalStateException("Unsupported database type: " + databaseType);
        }
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
                    "Location1 TEXT, " + // Added
                    "Location2 TEXT" + // Added
                    ")");
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Trusted (" +
                    "ClaimID TEXT, " +
                    "TrustedUUID TEXT, " +
                    "PRIMARY KEY(ClaimID, TrustedUUID))");
            conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_owner_uuid ON Claims(OwnerUUID)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set up SQLite database: " + e.getMessage());
        }
    }

    private void setupMySQL() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s",
                plugin.getConfig().getString("Database.MySQL.host"),
                plugin.getConfig().getInt("Database.MySQL.port"),
                plugin.getConfig().getString("Database.MySQL.database")));
        config.setUsername(plugin.getConfig().getString("Database.MySQL.user"));
        config.setPassword(plugin.getConfig().getString("Database.MySQL.password"));
        config.setMaximumPoolSize(plugin.getConfig().getInt("Database.MySQL.max-pool", 10));

        hikariDataSource = new HikariDataSource(config);

        try (Connection conn = hikariDataSource.getConnection()) {
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Claims (" +
                    "ID VARCHAR(36) PRIMARY KEY, " +
                    "OwnerUUID VARCHAR(36), " +
                    "Location1 TEXT, " + // Added
                    "Location2 TEXT" + // Added
                    ")");
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Trusted (" +
                    "ClaimID VARCHAR(36), " +
                    "TrustedUUID VARCHAR(36), " +
                    "PRIMARY KEY(ClaimID, TrustedUUID))");
            conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_owner_uuid ON Claims(OwnerUUID)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set up MySQL database: " + e.getMessage());
        }
    }

    private void setupMongoDB() {
        String connectionString = plugin.getConfig().getString("Database.MongoDB.connection-string");
        mongoClient = MongoClients.create(connectionString);
        MongoDatabase database = mongoClient.getDatabase(plugin.getConfig().getString("Database.MongoDB.database"));
        claimsCollection = database.getCollection("Claims");
        trustedCollection = database.getCollection("Trusted");
        claimsCollection.createIndex(new Document("OwnerUUID", 1));
        claimsCollection.createIndex(new Document("Location1", 1)); // Added
        claimsCollection.createIndex(new Document("Location2", 1)); // Added
        trustedCollection.createIndex(new Document("ClaimID", 1));
    }

    public Connection getConnection() throws SQLException {
        return hikariDataSource != null ? hikariDataSource.getConnection() : null;
    }

    public MongoCollection<Document> getClaimsCollection() {
        return claimsCollection;
    }

    public MongoCollection<Document> getTrustedCollection() {
        return trustedCollection;
    }

    public void close() {
        if (hikariDataSource != null) {
            hikariDataSource.close();
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
