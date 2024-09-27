package org.TheoCodes.ArtualSMP.Plugin.artual.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

public class AntiEctasy implements Listener {

    private final JavaPlugin plugin;

    Logger logger;
    FileHandler fh;

    public AntiEctasy(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();

        File logFolder = new File(plugin.getDataFolder(), "chatlogs");
        logFolder.mkdirs();

        File logFile = new File(logFolder, "ChatLog.log");
        try {
            fh = new FileHandler(logFile.getAbsolutePath(), true);
            fh.setFormatter(new CustomFormatter());
            logger = Logger.getLogger("ACL");
            logger.addHandler(fh);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChatRecieve(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("<" + event.getPlayer().getName() + "> " + event.getMessage());
        }
        logger.info(event.getPlayer().getName() + " » " + event.getMessage());
    }

    static class CustomFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append("<record>\n");
            sb.append("  <method>").append(record.getSourceMethodName()).append("</method>\n");
            sb.append("  <thread>").append(record.getThreadID()).append("</thread>\n");
            sb.append("  <chat>").append(record.getMessage()).append("</chat>\n");
            sb.append("</record>\n");
            return sb.toString();
        }
    }
}
