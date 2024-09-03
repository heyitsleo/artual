package org.TheoCodes.ArtualSMP.Plugin.artual.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

public class CompassTrackerListener implements Listener {

    private final Plugin plugin;

    public CompassTrackerListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        startCompassTracking(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopCompassTracking(event.getPlayer());
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item != null && item.getType() == Material.COMPASS) {
            startCompassTracking(player);
        } else {
            stopCompassTracking(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.getType() == Material.COMPASS) {
            updateCompassTarget(player, item);
        }
    }

    private void startCompassTracking(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item != null && item.getType() == Material.COMPASS) {
                    updateCompassTarget(player, item);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Use the stored plugin instance
    }

    private void stopCompassTracking(Player player) {
        Bukkit.getScheduler().cancelTasks(plugin); // Use the stored plugin instance
    }

    private void updateCompassTarget(Player player, ItemStack compass) {
        Player nearestPlayer = getNearestPlayer(player);

        if (nearestPlayer != null) {
            Location playerLoc = player.getLocation();
            Location targetLoc = nearestPlayer.getLocation();

            player.setCompassTarget(targetLoc);

            int distance = (int) playerLoc.distance(targetLoc);

            ItemMeta meta = compass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Nearest Player: " + nearestPlayer.getName() + " (" + distance + " blocks away)");
                compass.setItemMeta(meta);
            }
        } else {
            player.setCompassTarget(player.getWorld().getSpawnLocation());
            ItemMeta meta = compass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Nearest Player: None");
                compass.setItemMeta(meta);
            }
        }
    }

    private Player getNearestPlayer(Player player) {
        Location playerLocation = player.getLocation();
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;

            double distance = playerLocation.distanceSquared(other.getLocation());

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = other;
            }
        }

        return nearestPlayer;
    }
}