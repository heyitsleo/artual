package org.TheoCodes.ArtualSMP.Plugin.artual.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderChestDropper implements Listener {

    private final JavaPlugin plugin;

    public EnderChestDropper(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.getConfig().getBoolean("enderchest.drop-on-death", true)) {
            Player player = event.getEntity();
            Entity killer = player.getKiller();

            boolean pvpOnly = plugin.getConfig().getBoolean("enderchest.drop-on-pvp", true);

            if (pvpOnly && killer != null && killer.getType() == EntityType.PLAYER) {
                dropEnderChest(player);
            } else if (!pvpOnly) {
                dropEnderChest(player);
            }
        }
    }

    private void dropEnderChest(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) return; // Ensure world is not null

        for (ItemStack item : player.getEnderChest().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                world.dropItemNaturally(location, item);
            }
        }

        player.getEnderChest().clear();
    }
}
