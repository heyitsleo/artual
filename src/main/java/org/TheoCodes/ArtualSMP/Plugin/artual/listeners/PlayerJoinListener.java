package org.TheoCodes.ArtualSMP.Plugin.artual.listeners;

import org.TheoCodes.ArtualSMP.Plugin.artual.commands.ChunkCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerJoinListener implements Listener {

    private JavaPlugin plugin;
    private final ChunkCommand chunkCommand;

    public PlayerJoinListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.chunkCommand = new ChunkCommand(plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        chunkCommand.addKnownPlayer(playerName);
        chunkCommand.saveKnownPlayers();
    }
}