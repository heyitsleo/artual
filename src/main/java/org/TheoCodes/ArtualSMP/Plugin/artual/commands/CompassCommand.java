package org.TheoCodes.ArtualSMP.Plugin.artual.commands;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.TheoCodes.ArtualSMP.Plugin.artual.listeners.CompassTrackerListener;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

// Player compass tracker I think its straight forward

public class CompassCommand implements CommandExecutor {

    private Artual plugin;
    private CompassTrackerListener compassTrackerListener;

    public CompassCommand(Artual plugin){
        this.plugin = plugin;
        this.compassTrackerListener = new CompassTrackerListener(plugin);
        plugin.getCommand("getcompass").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("artual.getcompass")) {
                ItemStack compass = compassTrackerListener.createCompassItem();
                player.getInventory().addItem(compass);

                player.sendMessage(ChatColor.GREEN + "(✔) Gave you a tracking compass!"); // Send a verified message
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