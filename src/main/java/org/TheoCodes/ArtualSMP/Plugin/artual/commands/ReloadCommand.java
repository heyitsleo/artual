package org.TheoCodes.ArtualSMP.Plugin.artual.commands;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// This just does the reloading of the config.yml for you, incase idiot

public class ReloadCommand implements CommandExecutor {

    private final Artual plugin;

    public ReloadCommand(Artual plugin) {
        this.plugin = plugin;
        plugin.getCommand("reloadconfig").setExecutor(this);  // Register the command to ArtualClass
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player and cancel if they lack the required permission
        if (sender instanceof Player && !sender.hasPermission("artual.reload")) {
            sender.sendMessage(ChatColor.RED + "(✘) You lack permissions to use this command.");
            return true;
        }

        // Reload the config file
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "(✔) Configuration has reloaded.");

        // Play a sound effect for the player if valid
        if (sender instanceof Player) {
            ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.0f);
        }

        return true;
    }
}