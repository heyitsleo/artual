package org.TheoCodes.ArtualSMP.Plugin.artual.commands;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

// Test command, if this works you broke something

public class TestCommand implements CommandExecutor {

    private Artual plugin;

    public TestCommand(Artual plugin){
        this.plugin = plugin;
        plugin.getCommand("gold").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "(✘) This command can only be ran by players.");
            return true;
        }

        Player p = (Player) sender;

        if (p.hasPermission("artual.gold")){
            p.sendMessage(ChatColor.GREEN + "(✔) Here is your free gold!");
            p.getInventory().addItem(new ItemStack(Material.GOLD_BLOCK, 1));
            return true;
        } else {
            p.sendMessage(ChatColor.RED + "(✘) You lack permissions to use this command.");
        }
        return false;
    }
}
