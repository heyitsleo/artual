package org.TheoCodes.ArtualSMP.Plugin.artual.commands;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// Test command, if this works you broke something

public class TestCommand implements CommandExecutor {

    private Artual plugin;

    public TestCommand(Artual plugin){
        this.plugin = plugin;
        plugin.getCommand("test").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "(✘) This command can only be run by players.");
            return true;
        }

        Player p = (Player) sender;

        if (p.hasPermission("artual.test")){  // Corrected permission check
            p.sendMessage(ChatColor.GREEN + "(✔) Registered this command. Ran into issues?");
            return true;
        } else {
            p.sendMessage(ChatColor.RED + "(✘) You lack permissions to use this command.");  // Corrected message
        }
        return false;
    }
}
