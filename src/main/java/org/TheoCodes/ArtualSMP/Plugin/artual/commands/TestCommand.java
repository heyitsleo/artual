package org.TheoCodes.ArtualSMP.Plugin.artual.commands;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestCommand implements CommandExecutor {

    private Artual plugin;

    public TestCommand(Artual plugin){
        this.plugin = plugin;
        plugin.getCommand("test").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)){
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Player p = (Player) sender;

        if (p.hasPermission("test.use")){  // Corrected permission check
            p.sendMessage("This worked!");
            return true;
        } else {
            p.sendMessage(ChatColor.RED + "You lack the permission.");  // Corrected message
        }
        return false;
    }
}
