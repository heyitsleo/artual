package org.TheoCodes.ArtualSMP.Plugin.artual.claims;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.database.DBManager;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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

        if (isClaimed(chunkID)) {
            player.sendMessage(artual.color("&c(✘) This chunk is already claimed! Try again."));
            return;
        }

        dbManager.makeClaim(player.getUniqueId(), chunk);

        // Get chunk coordinates
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // Include coordinates in the success message
        player.sendMessage(artual.color("&a(✔) You have claimed this chunk!"));
    }


    public boolean isClaimed(String chunkID) {
        return dbManager.checkClaimed(chunkID);
    }

    public boolean isBlockInClaimedChunk(Block block) {
        Chunk chunk = block.getChunk();
        String chunkID = getChunkID(chunk);
        return isClaimed(chunkID);
    }

    public String getChunkID(Chunk chunk) {
        World world = chunk.getWorld();
        int x = chunk.getX();
        int z = chunk.getZ();
        return world.getName() + "-" + x + "-" + z;
    }

    public String getChunkIDAtPlayer(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        return getChunkID(chunk);
    }

    public boolean doesPlayerOwnChunk(Player player, Chunk chunk) {
        if (getClaimOwner(chunk) == player) {
            return true;
        }
        return false;
    }

    public Player getClaimOwner(Chunk chunk) {
        String chunkID = getChunkID(chunk);
        return dbManager.getClaimOwner(chunkID);
    }
}