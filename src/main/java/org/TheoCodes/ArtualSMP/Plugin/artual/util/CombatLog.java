package org.TheoCodes.ArtualSMP.Plugin.artual.util;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class CombatLog implements Listener {
    private final Plugin plugin;
    private final Map<UUID, Integer> combatTimers = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Set<String> blockedCommands = new HashSet<>();
    private final Map<UUID, UUID> combatOpponents = new HashMap<>();
    private final Map<UUID, BukkitTask> combatTasks = new HashMap<>();

    public CombatLog(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player && plugin.getConfig().getBoolean("combat.enabled", true)) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();
            if (((Player) event.getDamager()).getEquipment().getItemInMainHand().getType() == Material.MACE) {
                if (getOpponent((Player) event.getEntity()) != event.getDamager()) {
                    event.setCancelled(true);
                    return;
                }
            }
            addPlayerToCombat(attacker, victim);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (isInCombat(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§c(✘) You cannot execute that command in combat!");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (isInCombat(playerId)) {
            UUID opponentId = combatOpponents.get(playerId);
            Player opponent = Bukkit.getPlayer(opponentId);

            player.setHealth(0);

            removeCombatLog(playerId);
            removeCombatLog(opponentId);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        UUID victimUUID = event.getEntity().getUniqueId();
        UUID killerUUID = event.getEntity().getKiller().getUniqueId();
        removeCombatLog(victimUUID);
        removeCombatLog(killerUUID);
    }


    public void addPlayerToCombat(Player attacker, Player victim) {
        UUID attackerId = attacker.getUniqueId();
        UUID victimId = victim.getUniqueId();

        setCombatTimer(attackerId, victimId);
        setCombatTimer(victimId, attackerId);

        combatOpponents.put(attackerId, victimId);
        combatOpponents.put(victimId, attackerId);
    }

    private void setCombatTimer(UUID playerId, UUID opponentId) {
        int initialTime = plugin.getConfig().getInt("combat.duration", 25);

        BukkitTask existingTask = combatTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        combatTimers.put(playerId, initialTime);
        updateBossBar(playerId, initialTime);

        BukkitTask task = new BukkitRunnable() {
            int timeLeft = initialTime;

            @Override
            public void run() {
                if (timeLeft > 0) {
                    updateBossBar(playerId, timeLeft);
                    timeLeft--;
                    combatTimers.put(playerId, timeLeft);
                } else {
                    removeCombatLog(playerId);
                    combatTasks.remove(playerId);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        combatTasks.put(playerId, task);
    }

    private void updateBossBar(UUID playerId, int timeLeft) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        BossBar bossBar = bossBars.computeIfAbsent(playerId, id -> Bukkit.createBossBar(
                "", BarColor.RED, BarStyle.SOLID));

        bossBar.setTitle(Artual.color("&7Combat: <##dc3606>" + timeLeft));
        bossBar.setProgress(Math.max(0, Math.min(1, timeLeft / plugin.getConfig().getDouble("combat.duration", 25))));
        bossBar.addPlayer(player);
    }

    public void removeCombatLog(UUID playerId) {
        combatTimers.remove(playerId);
        BossBar bossBar = bossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
        Player player = Bukkit.getPlayer(playerId);
        combatOpponents.remove(playerId);

        BukkitTask task = combatTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public boolean isInCombat(UUID playerId) {
        return combatTimers.containsKey(playerId);
    }

    public Player getOpponent(Player player) {
        UUID playerId = player.getUniqueId();
        UUID opponentId = combatOpponents.get(playerId);

        if (opponentId != null) {
            return Bukkit.getPlayer(opponentId);
        }

        return null;
    }
}