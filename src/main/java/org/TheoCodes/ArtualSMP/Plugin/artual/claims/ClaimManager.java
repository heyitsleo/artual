package org.TheoCodes.ArtualSMP.Plugin.artual.claims;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.database.DBManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class ClaimManager {

    private JavaPlugin plugin;
    private DBManager dbManager;
    private Artual artual;

    public ClaimManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dbManager = new DBManager(plugin);
        this.artual = (Artual) plugin;
    }

    public void attemptClaim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String chunkID = getChunkID(chunk);

        if (dbManager.checkClaimed(chunkID)) {
            player.sendMessage(artual.color("&c(✘) This chunk is already claimed! Try again."));
            return;
        }

        if (getChunkAmount(player) >= 5) {
            if (player.hasPermission("artual.bypassmax")) {
                player.sendMessage(artual.color("&a(✔) You hit the max claims, but bypass it."));
            }
            else {
                player.sendMessage(artual.color("&c(✘) You have hit the max claimed chunks! Please try again after you have unclaimed one."));
                return;
            }
        }

        dbManager.makeClaim(player.getUniqueId(), chunk);

        player.sendMessage(artual.color("&a(✔) You have claimed this chunk!"));
    }

    public void attemptDelete(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String chunkID = getChunkID(chunk);

        if (!dbManager.checkClaimed(chunkID)) {
            player.sendMessage(artual.color("&c(✘) This chunk isn't claimed!"));
            return;
        }
        if (!doesPlayerOwnChunk(player, chunk)) {
            player.sendMessage(artual.color("&c(✘) You do not own this chunk!"));
            return;
        }

        dbManager.deleteClaim(dbManager.getClaimID(chunk));

        player.sendMessage(artual.color("&a(✔) You have deleted this chunk!"));
    }

    public boolean isClaimed(Chunk chunk) {
        return dbManager.checkClaimed(getChunkID(chunk));
    }

    public boolean isBlockInClaimedChunk(Block block) {
        Chunk chunk = block.getChunk();
        String chunkID = getChunkID(chunk);
        return dbManager.checkClaimed(chunkID);
    }

    public String getChunkID(Chunk chunk) {
        return dbManager.getChunkID(chunk);
    }

    public String getClaimID(Chunk chunk) {
        return dbManager.getClaimID(chunk);
    }

    public String getChunkIDAtPlayer(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        return getChunkID(chunk);
    }

    public boolean doesPlayerOwnChunk(Player player, Chunk chunk) {
        OfflinePlayer owner = getOfflineClaimOwner(chunk);
        return owner != null && owner.getUniqueId().equals(player.getUniqueId());
    }

    public Player getClaimOwner(Chunk chunk) {
        String chunkID = getChunkID(chunk);
        return (Player) dbManager.getClaimOwnerOffline(chunkID);
    }

    public OfflinePlayer getOfflineClaimOwner(Chunk chunk) {
        String chunkID = getChunkID(chunk);
        return dbManager.getClaimOwnerOffline(chunkID);
    }

    public UUID getClaimOwnerUUID(Chunk chunk) {
        OfflinePlayer owner = getOfflineClaimOwner(chunk);
        return owner != null ? owner.getUniqueId() : null;
    }

    public boolean isPlayerNearClaim(Player player) {
        Location location = player.getLocation();
        int chunkX = location.getChunk().getX();
        int chunkZ = location.getChunk().getZ();

        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {

                int neighborChunkX = chunkX + xOffset;
                int neighborChunkZ = chunkZ + zOffset;

                Chunk neighborChunk = location.getWorld().getChunkAt(neighborChunkX, neighborChunkZ);

                if (isClaimed(neighborChunk)) {
                    if (isTrusted(getOfflineClaimOwner(neighborChunk), player)) return false;
                    return !doesPlayerOwnChunk(player, neighborChunk);
                }
            }
        }
        return false;
    }

    public void addTrusted(Player owner, OfflinePlayer trusted) {
        if (owner == null || trusted == null) {
            return;
        }
        dbManager.addTrustedPlayer(owner.getUniqueId(), trusted.getUniqueId());
    }

    public void removeTrusted(Player owner, OfflinePlayer trusted) {
        if (owner == null || trusted == null) {
            return;
        }
        dbManager.removeTrustedPlayer(owner.getUniqueId(), trusted.getUniqueId());
    }

    public boolean isTrusted(OfflinePlayer owner, OfflinePlayer trusted) {
        if (owner == null || trusted == null) {
            return false;
        }
        return dbManager.isTrusted(owner.getUniqueId(), trusted.getUniqueId());
    }

    public List<OfflinePlayer> getTrustedPlayers(UUID owneruuid) {
        return dbManager.getTrustedPlayers(owneruuid);
    }

    public void clearPlayerData(OfflinePlayer player) {
        dbManager.clearData(player.getUniqueId());
    }

    public List<Chunk> getPlayerClaims(OfflinePlayer player) {
        return dbManager.getPlayerClaims(player.getUniqueId());
    }

    public boolean isTeamOffline(UUID playeruuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playeruuid);
        List<OfflinePlayer> trustedPlayers = getTrustedPlayers(playeruuid);

        for (OfflinePlayer trustedPlayer : trustedPlayers) {
            if (trustedPlayer.isOnline()) {
                return false;
            }
        }

        return !player.isOnline();
    }

    public int getChunkAmount(Player player) {
        return dbManager.getChunkAmount(player.getUniqueId());
    }
}