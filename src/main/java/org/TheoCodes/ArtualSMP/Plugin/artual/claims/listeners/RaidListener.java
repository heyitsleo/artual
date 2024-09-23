package org.TheoCodes.ArtualSMP.Plugin.artual.claims.listeners;

import org.TheoCodes.ArtualSMP.Plugin.artual.claims.ClaimManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class RaidListener implements Listener {

    private final JavaPlugin plugin;
    private final ClaimManager claimManager;

    private final HashMap<Location, BlockData> breaking = new HashMap<>();
    private final HashMap<Player, Player> raid = new HashMap<>();

    public RaidListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.claimManager = new ClaimManager(plugin);
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        if (claimManager.isBlockInClaimedChunk(block) && claimManager.getClaimOwner(block.getChunk()).isOnline() && !breaking.containsKey(block.getLocation())) {
            event.setCancelled(true);
            breaking.put(block.getLocation(), block.getBlockData());
            block.setBlockData(plugin.getServer().createBlockData("minecraft:reinforced_deepslate"));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (breaking.containsKey(block.getLocation())) {
            BlockData originalData = breaking.get(block.getLocation());
            block.setBlockData(originalData);
            breaking.remove(block.getLocation());
        }
    }

    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        Block block = event.getBlock();
        if (breaking.containsKey(block.getLocation())) {
            block.setBlockData(breaking.get(block.getLocation()));
            breaking.remove(block.getLocation());
        }
    }
}