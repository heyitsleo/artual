package org.TheoCodes.ArtualSMP.Plugin.artual.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import javax.xml.stream.events.Namespace;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BrewListener implements Listener {

    // Done in a different plugin, disabled by defualt.

    private JavaPlugin plugin;

    public BrewListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // @EventHandler
    // public void onBrew(BrewEvent event) {
    //     event.getResults().forEach(item -> {
    //         if (item.hasItemMeta() && (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION) && plugin.getConfig().getBoolean("extended-t2-strength", true)) {
    //             ItemMeta meta = item.getItemMeta();
    //
    //             if (meta instanceof PotionMeta potionMeta) {
    //
    //                if (potionMeta.getBasePotionType() == PotionType.STRONG_STRENGTH && potionMeta.getBasePotionData().isUpgraded()) {
    //                    potionMeta.setBasePotionType(PotionType.AWKWARD);
    //                    potionMeta.setDisplayName("§fPotion of Strength");
    //                    List<PotionEffect> newEffects = new ArrayList<>();
    //                    newEffects.add(new PotionEffect(PotionEffectType.STRENGTH, 1800, 1));
    //
    //                   potionMeta.clearCustomEffects();
    //                    newEffects.forEach(effect -> potionMeta.addCustomEffect(effect, true));
    //
    //                   // add persistent data to check when using redstone
    //                   potionMeta.getPersistentDataContainer().set(new NamespacedKey("potion", "strengtht2"), PersistentDataType.BOOLEAN, true);
    //               }
    //
    //               else if (potionMeta.getPersistentDataContainer().has(new NamespacedKey("potion", "strengtht2"), PersistentDataType.BOOLEAN)) {
    //                  if (Objects.requireNonNull(event.getContents().getIngredient()).getType() == Material.REDSTONE) {
    //                      potionMeta.setBasePotionType(PotionType.THICK);
    //                      potionMeta.setDisplayName("§fPotion of Strength");
    //                       List<PotionEffect> newEffects = new ArrayList<>();
    //                       newEffects.add(new PotionEffect(PotionEffectType.STRENGTH, 9600, 1));
    //
    //                      potionMeta.clearCustomEffects();
    //                       newEffects.forEach(effect -> potionMeta.addCustomEffect(effect, true));
    //                   }
    //                        else {
    //                            event.setCancelled(true);
    //                    }
    //                }
    //
    //                   item.setItemMeta(potionMeta);
    //            }
    //        }
    //    });
    //}
}
