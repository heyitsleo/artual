package org.TheoCodes.ArtualSMP.Plugin.artual.claims.database;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DBManager {

    private final JavaPlugin plugin;
    private final DBHandler dbHandler;

    public DBManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dbHandler = new DBHandler(plugin);
    }

    public List<Chunk> getPlayerClaims(UUID playerUUID) {
        List<Chunk> claimedChunks = new ArrayList<>();
        String sql = "SELECT ChunkID FROM Claims WHERE OwnerUUID = ?";

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerUUID.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String chunkID = rs.getString("ChunkID");
                    Chunk chunk = getChunkFromID(chunkID);
                    if (chunk != null) {
                        claimedChunks.add(chunk);
                    }
                }
            }
        } catch (SQLException e) {
            logError("Failed to fetch player claims", e);
        }

        return claimedChunks;
    }

    public void makeClaim(UUID ownerUUID, Chunk chunk) {
        String claimID = UUID.randomUUID().toString();
        String chunkID = getChunkID(chunk);
        String sql = "INSERT INTO Claims (ID, OwnerUUID, ChunkID) VALUES (?, ?, ?)";

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, claimID);
            pstmt.setString(2, ownerUUID.toString());
            pstmt.setString(3, chunkID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logError("Failed to make claim", e);
        }
    }

    public void deleteClaim(String claimID) {
        String sql = "DELETE FROM Claims WHERE ID = ?";

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, claimID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logError("Failed to delete claim", e);
        }
    }

    public String getClaimID(Chunk chunk) {
        String chunkID = getChunkID(chunk);
        String sql = "SELECT ID FROM Claims WHERE ChunkID = ?";

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chunkID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ID");
                }
            }
        } catch (SQLException e) {
            logError("Failed to get claim ID for chunk", e);
        }

        return null;
    }

    public boolean checkClaimed(String chunkID) {
        String sql = "SELECT COUNT(*) FROM Claims WHERE ChunkID = ?";

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chunkID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logError("Failed to check claim", e);
        }
        return false;
    }

    public OfflinePlayer getClaimOwnerOffline(String chunkID) {
        String sql = "SELECT OwnerUUID FROM Claims WHERE ChunkID = ?";

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chunkID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return plugin.getServer().getOfflinePlayer(UUID.fromString(rs.getString("OwnerUUID")));
                }
            }
        } catch (SQLException e) {
            logError("Failed to get claim owner", e);
        }
        return null;
    }

    public int getChunkAmount(UUID playerUUID) {
        String sql = "SELECT COUNT(*) FROM Claims WHERE OwnerUUID = ?";
        int chunkCount = 0;

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerUUID.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    chunkCount = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logError("Failed to get chunk amount for player", e);
        }

        return chunkCount;
    }

    public List<OfflinePlayer> getTrustedPlayers(UUID ownerUUID) {
        List<OfflinePlayer> trustedPlayers = new ArrayList<>();
        String sql = "SELECT TrustedUUID FROM Trusts WHERE OwnerUUID = ?";

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ownerUUID.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String uuidString = rs.getString("TrustedUUID");
                    if (uuidString != null && !uuidString.isEmpty()) {
                        try {
                            UUID trustedUUID = UUID.fromString(uuidString);
                            OfflinePlayer trustedPlayer = Bukkit.getOfflinePlayer(trustedUUID);
                            trustedPlayers.add(trustedPlayer);
                        } catch (IllegalArgumentException e) {
                            logError("Invalid UUID in Trusts table: " + uuidString, e);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logError("Failed to get trusted players for owner", e);
        }

        return trustedPlayers;
    }

    public void clearData(UUID playerUUID) {
        String[] sqlStatements = {
                "DELETE FROM Claims WHERE OwnerUUID = ?",
                "DELETE FROM Trusts WHERE OwnerUUID = ?",
                "DELETE FROM Trusts WHERE TrustedUUID = ?"
        };

        try (Connection conn = dbHandler.getConnection()) {
            for (String sql : sqlStatements) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    pstmt.executeUpdate();
                }
            }
            plugin.getLogger().info("Successfully cleared data for player: " + playerUUID);
        } catch (SQLException e) {
            logError("Failed to clear data for player", e);
        }
    }

    public void addTrustedPlayer(UUID ownerUUID, UUID trustedPlayerUUID) {
        String sql = "INSERT INTO Trusts (OwnerUUID, TrustedUUID) VALUES (?, ?)";

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ownerUUID.toString());
            pstmt.setString(2, trustedPlayerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logError("Failed to add trusted player", e);
        }
    }

    public void removeTrustedPlayer(UUID ownerUUID, UUID trustedPlayerUUID) {
        String sql = "DELETE FROM Trusts WHERE OwnerUUID = ? AND TrustedUUID = ?";

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ownerUUID.toString());
            pstmt.setString(2, trustedPlayerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logError("Failed to remove trusted player", e);
        }
    }

    public boolean isTrusted(UUID ownerUUID, UUID trustedPlayerUUID) {
        String sql = "SELECT COUNT(*) FROM Trusts WHERE OwnerUUID = ? AND TrustedUUID = ?";

        try (Connection conn = dbHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ownerUUID.toString());
            pstmt.setString(2, trustedPlayerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logError("Failed to check if player is trusted", e);
        }
        return false;
    }

    public String getChunkID(Chunk chunk) {
        return chunk.getWorld().getName() + "[" + chunk.getX() + ", " + chunk.getZ() + "]";
    }

    private Chunk getChunkFromID(String chunkID) {
        if (!chunkID.contains("[") || !chunkID.contains("]") || !chunkID.contains(",")) {
            logError("Invalid ChunkID format: " + chunkID, null);
            return null;
        }

        try {
            int startBracket = chunkID.indexOf("[");
            int endBracket = chunkID.indexOf("]");

            String worldName = chunkID.substring(0, startBracket);
            String coordinates = chunkID.substring(startBracket + 1, endBracket);

            String[] coords = coordinates.split(", ");
            if (coords.length != 2) {
                logError("Invalid coordinates in ChunkID: " + chunkID, null);
                return null;
            }

            int x = Integer.parseInt(coords[0]);
            int z = Integer.parseInt(coords[1]);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                logError("World not found: " + worldName, null);
                return null;
            }

            return world.getChunkAt(x, z);

        } catch (NumberFormatException e) {
            logError("Failed to parse chunk coordinates from ChunkID: " + chunkID, e);
            return null;
        } catch (IndexOutOfBoundsException e) {
            logError("Invalid ChunkID format: " + chunkID, e);
            return null;
        }
    }

    private void logError(String message, Exception e) {
        if (e != null) {
            plugin.getLogger().log(Level.SEVERE, message, e);
        } else {
            plugin.getLogger().severe(message);
        }
    }
}