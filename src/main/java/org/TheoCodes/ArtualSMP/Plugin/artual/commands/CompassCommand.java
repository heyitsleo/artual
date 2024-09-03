package org.TheoCodes.ArtualSMP.Plugin.artual.commands;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CompassCommand implements CommandExecutor {

    private Artual plugin;
    public CompassCommand(Artual plugin){
        this.plugin = plugin;
        plugin.getCommand("getcompass").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Give the player a compass
            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta meta = compass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Nearest Player Tracker");
                compass.setItemMeta(meta);
            }
            player.getInventory().addItem(compass);

            player.sendMessage("You have been given a tracking compass!");

            return true;
        } else {
            sender.sendMessage("This command can only be used by a player.");
            return false;
        }
    }
}

