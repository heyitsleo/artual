package org.TheoCodes.ArtualSMP.Plugin.artual.claims.database;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;
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

    public void makeClaim(UUID ownerUUID, Chunk chunk) {
        String claimID = UUID.randomUUID().toString();
        String chunkID = getChunkID(chunk);

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Claims (ID, OwnerUUID, ChunkID) VALUES (?, ?, ?)")) {
            pstmt.setString(1, claimID);
            pstmt.setString(2, ownerUUID.toString());
            pstmt.setString(3, chunkID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to make claim: " + e.getMessage());
        }
    }

    public void deleteClaim(String claimID) {
        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Claims WHERE ID = ?")) {
            pstmt.setString(1, claimID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete claim: " + e.getMessage());
        }
    }

    public boolean checkClaimed(String chunkID) {
        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT ID FROM Claims WHERE ChunkID = ?")) {
            pstmt.setString(1, chunkID);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check claim: " + e.getMessage());
        }
        return false;
    }

    public Player getClaimOwner(String chunkID) {
        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT OwnerUUID FROM Claims WHERE ChunkID = ?")) {
            pstmt.setString(1, chunkID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return plugin.getServer().getPlayer(UUID.fromString(rs.getString("OwnerUUID")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get claim owner: " + e.getMessage());
        }
        return null;
    }

    private String getChunkID(Chunk chunk) {
        return chunk.getWorld().getName() + "-" + chunk.getX() + "-" + chunk.getZ();
    }
}