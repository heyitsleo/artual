package org.TheoCodes.ArtualSMP.Plugin.artual.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public class CompassTrackerListener implements Listener {

    private final Plugin plugin;  // Reference to the main plugin instance

    // Constructor to initialize the listener with the plugin reference
    public CompassTrackerListener(Plugin plugin) {
        this.plugin = plugin;
        registerCompassRecipe();  // Register the custom crafting recipe when this listener is initialized
    }

    // Event handler for when a player switches the item they're holding
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();  // Get the player who triggered the event
        ItemStack item = player.getInventory().getItem(event.getNewSlot());  // Get the item in the new slot

        // Check if the item is a compass with the custom "tracker" tag
        if (item != null && item.getType() == Material.COMPASS &&
                Objects.requireNonNull(item.getItemMeta())
                        .getPersistentDataContainer()
                        .has(new NamespacedKey("artual", "tracker"), PersistentDataType.BOOLEAN)) {
            startTracking(player);  // Start tracking players if conditions are met
        }
    }

    // Method to create the special "Player Tracker" compass
    public ItemStack compassItem() {
        ItemStack compass = new ItemStack(Material.COMPASS);  // Create a new compass item
        ItemMeta meta = compass.getItemMeta();  // Get the item's metadata

        if (meta != null) {
            // Add an enchantment and hide it from the item description
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Add a custom tag to the item to identify it as a "tracker"
            meta.getPersistentDataContainer().set(new NamespacedKey("artual", "tracker"), PersistentDataType.BOOLEAN, true);

            // Set a custom name for the item with color codes
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Player Tracker"));

            // Apply the metadata back to the item
            compass.setItemMeta(meta);
        }

        return compass;  // Return the customized compass item
    }

    // Method to register the custom crafting recipe for the Player Tracker compass
    private void registerCompassRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "player_tracker_compass");  // Create a unique key for the recipe

        // Remove any old recipes with the same key to avoid conflicts
        Bukkit.removeRecipe(recipeKey);

        // Get the custom compass item to be crafted
        ItemStack compass = compassItem();

        // Define the crafting recipe (shaped recipe with a 3x3 grid)
        ShapedRecipe compassRecipe = new ShapedRecipe(recipeKey, compass);
        compassRecipe.shape(" D ", "DRD", " D ");  // Set the crafting shape (D = Diamond Block, R = Redstone Block)

        // Assign the ingredients for the recipe
        compassRecipe.setIngredient('D', Material.DIAMOND_BLOCK);  // Diamond Block for 'D'
        compassRecipe.setIngredient('R', Material.REDSTONE_BLOCK);  // Redstone Block for 'R'

        // Register the recipe with the server
        Bukkit.addRecipe(compassRecipe);
    }

    // Method to start tracking the nearest player when the tracker compass is held
    public void startTracking(Player player) {
        // Get the update interval from the config and convert it to ticks
        long refreshRateTicks = (long) (plugin.getConfig().getDouble("tracker.update-interval") * 20);

        // Schedule a repeating task to update the tracking information
        new BukkitRunnable() {
            @Override
            public void run() {
                // Cancel the task if the player is offline or null
                if (player == null || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Get the item currently held by the player
                ItemStack itemInMainHand = player.getInventory().getItemInMainHand();

                // Cancel the task if the held item is not a valid tracker compass
                if (itemInMainHand == null || itemInMainHand.getType() == Material.AIR || itemInMainHand.getType() != Material.COMPASS) {
                    this.cancel();
                    return;
                }

                // Check if the held compass is a "tracker" compass
                ItemMeta itemMeta = itemInMainHand.getItemMeta();
                if (itemMeta == null || !itemMeta.getPersistentDataContainer().has(new NamespacedKey("artual", "tracker"), PersistentDataType.BOOLEAN)) {
                    this.cancel();
                    return;
                }

                // Find the nearest player to the one holding the tracker compass
                Player nearestPlayer = findNearestPlayer(player);
                double maxDistance = plugin.getConfig().getInt("tracker.max-distance");  // Get the maximum tracking distance from config

                // If a nearby player is found within the allowed distance
                if (nearestPlayer != null) {
                    double distance = player.getLocation().distance(nearestPlayer.getLocation());  // Calculate the distance to the nearest player

                    // If the nearest player is too far away, show a message
                    if (distance > maxDistance) {
                        String message = ChatColor.translateAlternateColorCodes('&', "&cNo players nearby.");
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                        return;
                    }

                    // Round the distance to two decimal places for display
                    int rounded = (int) (Math.round(distance * 100.0) / 100.0);

                    // Create the tracking message using a template from the config
                    String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("tracker.text", "&6%target% &8| &6%distance%&7 blocks away."))
                            .replace("%target%", nearestPlayer.getName())
                            .replace("%distance%", String.valueOf(rounded));

                    // If configured, point the compass to the nearest player
                    if (plugin.getConfig().getBoolean("tracker.point-to-target", true)) {
                        player.setCompassTarget(nearestPlayer.getLocation());
                    }

                    // Display the tracking message in the action bar
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                } else {
                    // Show a message if no players are nearby
                    String message = ChatColor.translateAlternateColorCodes('&', "&cNo players nearby.");
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                }
            }
        }.runTaskTimer(plugin, 0, refreshRateTicks);  // Schedule the task to repeat at the configured interval
    }

    // Method to find the nearest player to the given player
    private Player findNearestPlayer(Player player) {
        double minDistanceSquared = Double.MAX_VALUE;  // Start with the maximum possible distance
        Player nearestPlayer = null;  // Initialize the nearest player as null
        Location playerLocation = player.getLocation();  // Get the location of the player holding the tracker

        // Loop through all online players to find the nearest one
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(player)) continue;  // Skip the player holding the tracker

            Location otherLocation = p.getLocation();  // Get the location of the other player
            double distanceSquared = playerLocation.distanceSquared(otherLocation);  // Calculate the squared distance to avoid using sqrt

            // Update the nearest player if this one is closer
            if (distanceSquared < minDistanceSquared) {
                minDistanceSquared = distanceSquared;
                nearestPlayer = p;
            }
        }

        return nearestPlayer;  // Return the nearest player found, or null if none are nearby
    }
}
