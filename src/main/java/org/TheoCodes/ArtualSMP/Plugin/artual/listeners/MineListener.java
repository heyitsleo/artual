package org.TheoCodes.ArtualSMP.Plugin.artual.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class MineListener implements Listener {

    private JavaPlugin plugin;

    public MineListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMine(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.ANCIENT_DEBRIS && plugin.getConfig().getBoolean("ancient-debris.enabled", true)) {
            event.setDropItems(false);
            int min = plugin.getConfig().getInt("ancient-debris.minimum-scrap", 1);
            int max = plugin.getConfig().getInt("ancient-debris.maximum-scrap", 3);
            int amount = new Random().nextInt(max - min + 1) + min;
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.NETHERITE_SCRAP, amount));
        }
    }
}
