package org.TheoCodes.ArtualSMP.Plugin.artual;

import org.TheoCodes.ArtualSMP.Plugin.artual.claims.database.DBHandler;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.database.DBManager;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.listeners.RaidListener;
import org.TheoCodes.ArtualSMP.Plugin.artual.commands.*;
import org.TheoCodes.ArtualSMP.Plugin.artual.listeners.*;
import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

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
        registerRecipes();

        // Ensure the plugin data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        saveDefaultConfig();
        saveConfig();
        DBHandler dbHandler = new DBHandler(this);
        dbHandler.setupDatabase();
        new RaidListener(this).onDisable();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "(✘) Disabling ArtualSMP+ by prodtheo & Harfull");

        DBHandler dbHandler = new DBHandler(this);
        dbHandler.close();
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
                new RaidListener(this),
                new MineListener(this),
                new AntiEctasy(this),
                new CompassTrackerListener(this),
                new PlayerJoinListener(this)
        ).forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
    }

    public void registerRecipes() {
        registerSplashStrength();
        registerNormalStrength();
    }

    private void registerSplashStrength() {
        Bukkit.removeRecipe(new NamespacedKey(this, "splash_extended_strength"));
        NamespacedKey key = new NamespacedKey(this, "splash_extended_strength");
        ItemStack result = new ItemStack(Material.SPLASH_POTION);
        PotionMeta resultMeta = (PotionMeta) result.getItemMeta();
        assert resultMeta != null;
        resultMeta.setBasePotionType(PotionType.THICK);
        resultMeta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 9600, 1), true);
        resultMeta.setDisplayName("§fSplash Potion of Strength");
        result.setItemMeta(resultMeta);
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        ItemStack ingredient = new ItemStack(Material.SPLASH_POTION);
        PotionMeta ingredientMeta = (PotionMeta) ingredient.getItemMeta();
        assert ingredientMeta != null;
        ingredientMeta.setBasePotionData(new PotionData(PotionType.STRENGTH, false, true));
        ingredient.setItemMeta(ingredientMeta);
        recipe.addIngredient(new RecipeChoice.ExactChoice(ingredient));
        recipe.addIngredient(Material.REDSTONE);
        Bukkit.addRecipe(recipe);
    }

    private void registerNormalStrength() {
        Bukkit.removeRecipe(new NamespacedKey(this, "normal_extended_strength"));
        NamespacedKey key = new NamespacedKey(this, "normal_extended_strength");
        ItemStack result = new ItemStack(Material.POTION);
        PotionMeta resultMeta = (PotionMeta) result.getItemMeta();
        assert resultMeta != null;
        resultMeta.setBasePotionType(PotionType.THICK);
        resultMeta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 9600, 1), true);
        resultMeta.setDisplayName("§fPotion of Strength");
        result.setItemMeta(resultMeta);
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        ItemStack ingredient = new ItemStack(Material.POTION);
        PotionMeta ingredientMeta = (PotionMeta) ingredient.getItemMeta();
        assert ingredientMeta != null;
        ingredientMeta.setBasePotionData(new PotionData(PotionType.STRENGTH, false, true));
        ingredient.setItemMeta(ingredientMeta);
        recipe.addIngredient(new RecipeChoice.ExactChoice(ingredient));
        recipe.addIngredient(Material.REDSTONE);
        Bukkit.addRecipe(recipe);
    }
    ;

    public static String color(String message) {
        return translateHexColorCodes(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static String translateHexColorCodes(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 32);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, "§x§" + group.charAt(0) + '§' + group.charAt(1) + '§' + group.charAt(2) + '§' + group.charAt(3) + '§' + group.charAt(4) + '§' + group.charAt(5));
        }
        return matcher.appendTail(buffer).toString();
    }
}
