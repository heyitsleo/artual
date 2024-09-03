package org.TheoCodes.ArtualSMP.Plugin.artual.listeners;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class EnderChestDropper implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        World world = player.getWorld();

        // Drop all items from the player's Ender Chest
        for (ItemStack item : player.getEnderChest().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                world.dropItemNaturally(player.getLocation(), item);
            }
        }

        // Clear the player's Ender Chest
        player.getEnderChest().clear();
    }
}