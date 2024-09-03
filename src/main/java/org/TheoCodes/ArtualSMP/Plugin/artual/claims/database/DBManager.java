package org.TheoCodes.ArtualSMP.Plugin.artual.claims.database;

import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DBManager {

    private final JavaPlugin plugin;
    private final DBHandler dbHandler;

    public DBManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dbHandler = new DBHandler(plugin);
    }

    public List<String> getPlayerClaims(UUID playerUUID) {
        List<String> claims = new ArrayList<>();
        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT ID FROM Claims WHERE OwnerUUID = ?")) {
            pstmt.setString(1, playerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    claims.add(rs.getString("ID"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch player claims: " + e.getMessage());
        }
        return claims;
    }

    public void makeClaim(UUID ownerUUID, Location loc1, Location loc2) {
        String claimID = generateClaimID(ownerUUID); // Generate a unique claim ID
        String loc1String = locationToString(loc1);
        String loc2String = locationToString(loc2);

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Claims (ID, OwnerUUID, Location1, Location2) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE Location1 = ?, Location2 = ?")) {
            pstmt.setString(1, claimID);
            pstmt.setString(2, ownerUUID.toString());
            pstmt.setString(3, loc1String);
            pstmt.setString(4, loc2String);
            pstmt.setString(5, loc1String);
            pstmt.setString(6, loc2String);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to make claim: " + e.getMessage());
        }
    }

    public void addTrusted(String claimID, UUID playerUUID) {
        try {
            if (dbHandler.getClaimsCollection() != null) {
                dbHandler.getTrustedCollection().updateOne(
                        new Document("ClaimID", claimID).append("TrustedUUID", playerUUID.toString()),
                        new Document("$set", new Document("TrustedUUID", playerUUID.toString())),
                        new UpdateOptions().upsert(true)
                );
            } else {
                try (Connection conn = dbHandler.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Trusted (ClaimID, TrustedUUID) VALUES (?, ?)")) {
                    pstmt.setString(1, claimID);
                    pstmt.setString(2, playerUUID.toString());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add trusted player: " + e.getMessage());
        }
    }

    public List<UUID> getTrusted(String claimID) {
        List<UUID> trusted = new ArrayList<>();
        try {
            if (dbHandler.getClaimsCollection() != null) {
                for (Document doc : dbHandler.getTrustedCollection().find(new Document("ClaimID", claimID))) {
                    trusted.add(UUID.fromString(doc.getString("TrustedUUID")));
                }
            } else {
                try (Connection conn = dbHandler.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("SELECT TrustedUUID FROM Trusted WHERE ClaimID = ?")) {
                    pstmt.setString(1, claimID);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            trusted.add(UUID.fromString(rs.getString("TrustedUUID")));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get trusted players: " + e.getMessage());
        }
        return trusted;
    }

    private String generateClaimID(UUID ownerUUID) { // claim id system i made
        return UUID.randomUUID().toString();
    }

    private String locationToString(Location location) {
        return String.format("%s,%s,%s,%s,%s,%s",
                location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getPitch(), location.getYaw());
    }

    public void close() {
        dbHandler.close();
    }
}
