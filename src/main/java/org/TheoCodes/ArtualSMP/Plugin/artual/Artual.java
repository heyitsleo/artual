package org.TheoCodes.ArtualSMP.Plugin.artual;

import org.TheoCodes.ArtualSMP.Plugin.artual.claims.database.DBHandler;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.database.DBManager;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.listeners.RaidListener;
import org.TheoCodes.ArtualSMP.Plugin.artual.commands.ChunkCommand;
import org.TheoCodes.ArtualSMP.Plugin.artual.commands.CompassCommand;
import org.TheoCodes.ArtualSMP.Plugin.artual.commands.ReloadCommand;
import org.TheoCodes.ArtualSMP.Plugin.artual.listeners.BrewListener;
import org.TheoCodes.ArtualSMP.Plugin.artual.listeners.CompassTrackerListener;
import org.TheoCodes.ArtualSMP.Plugin.artual.listeners.CraftListener;
import org.TheoCodes.ArtualSMP.Plugin.artual.listeners.EnderChestDropper;
import org.TheoCodes.ArtualSMP.Plugin.artual.commands.TestCommand;
import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Artual extends JavaPlugin {

    private static final Pattern HEX_PATTERN = Pattern.compile("<##([A-Fa-f0-9]{6})>");

    @Override
    public void onEnable() {
        Bukkit.getLogger().info(ChatColor.YELLOW + "(✔) Enabling ArtualSMP+ by prodtheo & Harfull");

        registerEvents();
        registerCommands();

        // Ensure the plugin data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        saveDefaultConfig();
        saveConfig();
        DBHandler dbHandler = new DBHandler(this);
        dbHandler.setupDatabase();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "(✘) Disabling ArtualSMP+ by prodtheo & Harfull");

        DBHandler dbHandler = new DBHandler(this);
        dbHandler.close();

        RaidListener raidListener = new RaidListener(this);
        raidListener.disableMethod();
    }

    private void registerCommands() {
        new CompassCommand(this);
        new ReloadCommand(this);
        new TestCommand(this);
        new ChunkCommand(this);
    }

    private void registerEvents() {
        Arrays.asList(
                new EnderChestDropper(this),
                new CraftListener(this),
                new EnderChestDropper(this),
                new BrewListener(this),
                new RaidListener(this),
                new CompassTrackerListener(this)
        ).forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
    }


    public CompassTrackerListener getCompassTrackerListener() {
        return new CompassTrackerListener(this);
    }

    public String color(String message) {
        return translateHexColorCodes(ChatColor.translateAlternateColorCodes('&', message));
    }

    public String translateHexColorCodes(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 32);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, "§x§" + group.charAt(0) + '§' + group.charAt(1) + '§' + group.charAt(2) + '§' + group.charAt(3) + '§' + group.charAt(4) + '§' + group.charAt(5));
        }
        return matcher.appendTail(buffer).toString();
    } // last test of pro plugin
}
