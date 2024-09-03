package org.TheoCodes.ArtualSMP.Plugin.artual;

import org.TheoCodes.ArtualSMP.Plugin.artual.claims.database.DBHandler;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.database.DBManager;
import org.TheoCodes.ArtualSMP.Plugin.artual.listeners.EnderChestDropper;
import org.TheoCodes.ArtualSMP.Plugin.artual.commands.TestCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Artual extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getLogger().info("Enabling ArutalSMP+ by &aprodtheo");
        new TestCommand(this);
        Bukkit.getPluginManager().registerEvents(new EnderChestDropper(), this);

        saveDefaultConfig();
        DBHandler dbHandler = new DBHandler(this);
        dbHandler.setupDatabase();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Disabling ArutalSMP+ by &aprodtheo");

        DBHandler dbHandler = new DBHandler(this);
        dbHandler.close();
    }
}
