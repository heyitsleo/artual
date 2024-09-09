package org.TheoCodes.ArtualSMP.Plugin.artual.commands;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

// Player compass tracker I think its straight forward

public class CompassCommand implements CommandExecutor {

    private Artual plugin;

    public CompassCommand(Artual plugin){
        this.plugin = plugin;
        plugin.getCommand("getcompass").setExecutor(this); // Registering the command to java
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender; // Check if the executor is a player

            // Check if the player has the required permission
            if (player.hasPermission("artual.compass")) {
                // Give the player a compass
                ItemStack compass = plugin.getCompassTrackerListener().compassItem();
                player.getInventory().addItem(compass);

                player.sendMessage(ChatColor.GREEN + "(✔) You have been given a tracking compass!"); // Send a verified message
            } else {
                player.sendMessage(ChatColor.RED + "(✘) You lack permissions to use this command."); // Send error message permissions lacking wise
            }

            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "(✘) This command can only be run by players."); // Let console know player only command
            return false;
        }
    }
}