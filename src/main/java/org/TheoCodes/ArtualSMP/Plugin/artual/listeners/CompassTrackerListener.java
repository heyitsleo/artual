package org.TheoCodes.ArtualSMP.Plugin.artual.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CompassTrackerListener implements Listener {

    private final Plugin plugin;
    private final NamespacedKey trackerKey;
    private final long refreshRateTicks;
    private final double maxTrackingDistance;
    private BukkitRunnable trackingTask;

    public CompassTrackerListener(Plugin plugin) {
        this.plugin = plugin;
        this.trackerKey = new NamespacedKey(plugin, "tracker");
        this.refreshRateTicks = (long) (plugin.getConfig().getDouble("tracker.update-interval", 1.0) * 20);
        this.maxTrackingDistance = plugin.getConfig().getDouble("tracker.max-distance", 5000.0);
        registerCompassRecipe();
        startTracking();
    }

    private void startTracking() {
        trackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (hasTrackerCompass(player)) {
                        updateTracking(player);
                    }
                }
            }
        };
        trackingTask.runTaskTimer(plugin, 0, refreshRateTicks);
    }

    private boolean hasTrackerCompass(Player player) {
        return player.getInventory().contains(Material.COMPASS) &&
                Arrays.stream(player.getInventory().getContents())
                        .filter(Objects::nonNull)
                        .anyMatch(this::isTrackerCompass);
    }

    private boolean isTrackerCompass(ItemStack item) {
        return item.getType() == Material.COMPASS &&
                item.getItemMeta() != null &&
                item.getItemMeta().getPersistentDataContainer().has(trackerKey, PersistentDataType.BOOLEAN);
    }

    public ItemStack createCompassItem() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(trackerKey, PersistentDataType.BOOLEAN, true);
            meta.setDisplayName(ChatColor.GOLD + "Player Tracker");
            compass.setItemMeta(meta);
        }
        return compass;
    }

    private void registerCompassRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "player_tracker_compass");
        Bukkit.removeRecipe(recipeKey);

        ShapedRecipe compassRecipe = new ShapedRecipe(recipeKey, createCompassItem());
        compassRecipe.shape(" D ", "DRD", " D ");
        compassRecipe.setIngredient('D', Material.DIAMOND);
        compassRecipe.setIngredient('R', Material.REDSTONE_BLOCK);

        Bukkit.addRecipe(compassRecipe);
    }

    private void updateTracking(Player player) {
        Player nearestPlayer = findNearestPlayer(player);
        if (nearestPlayer != null) {
            player.setCompassTarget(nearestPlayer.getLocation());
        } else {
            player.setCompassTarget(player.getWorld().getSpawnLocation());
            sendActionBar(player, ChatColor.RED + "No players nearby");
        }
    }

    private Player findNearestPlayer(Player player) {
        Location playerLocation = player.getLocation();
        World playerWorld = player.getWorld();

        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(player) && p.getWorld().equals(playerWorld))
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(playerLocation)))
                .filter(p -> p.getLocation().distanceSquared(playerLocation) <= maxTrackingDistance * maxTrackingDistance)
                .orElse(null);
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}