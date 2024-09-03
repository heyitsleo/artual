package org.TheoCodes.ArtualSMP.Plugin.artual;

import org.TheoCodes.ArtualSMP.Plugin.artual.commands.CompassCommand;
import org.TheoCodes.ArtualSMP.Plugin.artual.listeners.CompassTrackerListener;
import org.TheoCodes.ArtualSMP.Plugin.artual.listeners.EnderChestDropper;
import org.TheoCodes.ArtualSMP.Plugin.artual.commands.TestCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Artual extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getLogger().info(ChatColor.GREEN + "Enabling ArtualSMP+ by prodtheo & Harfull");
        new TestCommand(this);
        new CompassCommand(this);
        // Event Listeners
        Bukkit.getPluginManager().registerEvents(new CompassTrackerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EnderChestDropper(), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getLogger().info(ChatColor.RED + "Disabling ArtualSMP+ by prodtheo & Harfull");
    }
}
