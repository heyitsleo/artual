package org.TheoCodes.ArtualSMP.Plugin.artual.commands;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.ClaimManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkCommand implements CommandExecutor {

    private JavaPlugin plugin;
    private Artual artual;
    private ClaimManager claimManager;

    public ChunkCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.artual = (Artual) plugin;
        this.claimManager = new ClaimManager(plugin);
        artual.getCommand("chunk").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) commandSender;

        if (command.getName().equalsIgnoreCase("chunk")) {
            if (args.length == 0) {
                sendUsage(player);
                return true;
            }

            String action = args[0].toLowerCase();

            switch (action) {
                case "claim":
                    claimManager.attemptClaim(player);
                    return true;
                case "delete":
                    player.sendMessage(artual.color("&6&lClaims &7» &fcoming soon.")); // &c&o&m&i&n&g &s&o&o&n
                    return true;
                default:
                    sendUsage(player);
                    return true;
            }
        }
        sendUsage(player);
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(artual.color("&6&lClaims &7» &f/chunk &7(&6claim/delete&7)"));
    }
}