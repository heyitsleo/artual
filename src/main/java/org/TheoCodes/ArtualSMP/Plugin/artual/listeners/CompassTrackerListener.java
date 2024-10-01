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

    private final Plugin plugin;

    public CompassTrackerListener(Plugin plugin) {
        this.plugin = plugin;
        registerCompassRecipe();
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item != null && item.getType() == Material.COMPASS && Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().has(new NamespacedKey("artual", "tracker"), PersistentDataType.BOOLEAN)) {
            startTracking(player);
        }
    }

    // Creates the special "Player tracker" compass
    public ItemStack compassItem() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(new NamespacedKey("artual", "tracker"), PersistentDataType.BOOLEAN, true);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Player Tracker"));
            compass.setItemMeta(meta);
        }
        return compass;
    }

    // Register the custom crafting recipe for the Player Tracker compass
    private void registerCompassRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "player_tracker_compass");

        // Delete old recipe to avoid conflicts
        Bukkit.removeRecipe(recipeKey);

        ItemStack compass = compassItem();  // Get the custom compass item

        // Define the crafting recipe (shaped recipe with 3x3 grid)
        ShapedRecipe compassRecipe = new ShapedRecipe(recipeKey, compass);
        compassRecipe.shape(" D ", "DRD", " D ");  // Crafting shape with air in between

        // Set ingredients: D (diamond NOT BLOCK), R (redstone block)
        compassRecipe.setIngredient('D', Material.DIAMOND);  // Diamond
        compassRecipe.setIngredient('R', Material.REDSTONE_BLOCK);  // Redstone block

        // Register the recipe
        Bukkit.addRecipe(compassRecipe);
    }


    public void startTracking(Player player) {
        long refreshRateTicks = (long) (plugin.getConfig().getDouble("tracker.update-interval") * 20);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
                if (itemInMainHand == null || itemInMainHand.getType() == Material.AIR || itemInMainHand.getType() != Material.COMPASS) {
                    this.cancel();
                    return;
                }

                ItemMeta itemMeta = itemInMainHand.getItemMeta();
                if (itemMeta == null || !itemMeta.getPersistentDataContainer().has(new NamespacedKey("artual", "tracker"), PersistentDataType.BOOLEAN)) {
                    this.cancel();
                    return;
                }

                Player nearestPlayer = findNearestPlayer(player);
                if (nearestPlayer != null) {
                    player.setCompassTarget(nearestPlayer.getLocation());
                } else {
                    String message = ChatColor.translateAlternateColorCodes('&', "&cNo players nearby.");
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                }
            }
        }.runTaskTimer(plugin, 0, refreshRateTicks);
    }

    private Player findNearestPlayer(Player player) {
        double minDistanceSquared = Double.MAX_VALUE;
        Player nearestPlayer = null;
        Location playerLocation = player.getLocation();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(player)) continue;

            Location otherLocation = p.getLocation();
            double distance = playerLocation.distance(otherLocation);

            if (distance < minDistanceSquared) {
                minDistanceSquared = distance;
                nearestPlayer = p;
            }
        }

        return nearestPlayer;
    }
}
