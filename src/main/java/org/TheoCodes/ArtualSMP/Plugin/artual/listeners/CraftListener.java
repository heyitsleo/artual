package org.TheoCodes.ArtualSMP.Plugin.artual.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;

public class CraftListener implements Listener {

    private JavaPlugin plugin;

    public CraftListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (!plugin.getConfig().getBoolean("double-gaps", true)) return;

        Recipe recipe = event.getRecipe();

        if (recipe == null || recipe.getResult() == null) return;

        if (recipe.getResult().getType() == Material.GOLDEN_APPLE) {
            int currentAmount = recipe.getResult().getAmount();
            int newAmount = Math.min(currentAmount * 2, 64);
            ItemStack newResult = new ItemStack(Material.GOLDEN_APPLE, newAmount);

            event.getInventory().setResult(newResult);
        }
    }
}