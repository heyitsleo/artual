package org.TheoCodes.ArtualSMP.Plugin.artual.claims.listeners;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.ClaimManager;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.RaidStatus;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class RaidListener implements Listener {

    private final JavaPlugin plugin;
    private final ClaimManager claimManager;

    private final Map<Location, BlockData> breaking = new HashMap<>();
    private final Map<Location, UUID> breakingPlayers = new HashMap<>();
    private static final Map<UUID, RaidStatus> raid = new HashMap<>();
    private static final Map<UUID, Long> lastRaidActivity = new HashMap<>();
    private static final Map<UUID, BossBar> raidBossBars = new HashMap<>();

    private static final long CHECK_INTERVAL = 1L;
    private static final double MAX_DISTANCE = 5.0;
    private static final long RAID_TIMEOUT = 20 * 60 * 1000L; // 20 minutes in milliseconds

    public RaidListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.claimManager = new ClaimManager(plugin);
        startRaycastCheck();
        startRaidTimeoutCheck();
    }

    private void applyEffect(Player player, PotionEffectType effect, int amplifier) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID playerUUID = player.getUniqueId();
                if (breakingPlayers.containsValue(playerUUID) && player.isOnline()) {
                    player.addPotionEffect(new PotionEffect(effect, PotionEffect.INFINITE_DURATION, amplifier, true, false));
                } else {
                    player.removePotionEffect(effect);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (breakingPlayers.containsValue(player.getUniqueId())) {
            for (Map.Entry<Location, UUID> entry : new HashMap<>(breakingPlayers).entrySet()) {
                if (entry.getValue().equals(player.getUniqueId())) {
                    revertBlock(entry.getKey().getBlock());
                    player.removePotionEffect(PotionEffectType.FAST_DIGGING);
                    player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
                }
            }
        }
        removeBossBar(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (raid.containsKey(playerUUID) && raid.get(playerUUID) == RaidStatus.RAIDING) {
            long remainingTime = RAID_TIMEOUT - (System.currentTimeMillis() - lastRaidActivity.get(playerUUID));
            if (remainingTime > 0) {
                createOrUpdateBossBar(playerUUID, player);
                updateBossBar(playerUUID, remainingTime);
            }
        }
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Player claimOwner = claimManager.getClaimOwner(block.getChunk());
        OfflinePlayer offlineClaimOwner = claimManager.getOfflineClaimOwner(block.getChunk());

        if (claimOwner == null && offlineClaimOwner == null) {
            return;
        }

        UUID ownerUUID = offlineClaimOwner != null ? offlineClaimOwner.getUniqueId() : claimOwner.getUniqueId();

        if (offlineClaimOwner != null && !offlineClaimOwner.isOnline() && !raid.containsKey(ownerUUID) && !breaking.containsKey(block.getLocation()) && !breakingPlayers.containsKey(block.getLocation())) {

            if (event.getInstaBreak()) {
                event.setCancelled(true);
            }

            breaking.put(block.getLocation(), block.getBlockData());
            breakingPlayers.put(block.getLocation(), player.getUniqueId());

            block.setType(Material.BEDROCK);
            applyEffect(player, PotionEffectType.SLOW_DIGGING, 255);
            player.sendMessage("§c(✘) The claim owner is not online.");
        }

        if (event.getInstaBreak()) {
            event.setCancelled(true);
        }

        if (claimManager.isBlockInClaimedChunk(block) && !claimManager.doesPlayerOwnChunk(player, block.getChunk()) && !breaking.containsKey(block.getLocation()) && !breakingPlayers.containsKey(block.getLocation())) {
            breaking.put(block.getLocation(), block.getBlockData());
            breakingPlayers.put(block.getLocation(), player.getUniqueId());

            if (raid.containsKey(ownerUUID) && raid.get(ownerUUID) == RaidStatus.RAIDING) {
                block.setType(Material.HONEYCOMB_BLOCK);
                applyEffect(player, PotionEffectType.SLOW_DIGGING, 1);
            } else {
                block.setType(Material.DEEPSLATE);
                applyEffect(player, PotionEffectType.FAST_DIGGING, 0);
            }
            lastRaidActivity.put(ownerUUID, System.currentTimeMillis());
            createOrUpdateBossBar(ownerUUID, claimOwner);
        }
    }

    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        Block block = event.getBlock();
        if (breaking.containsKey(block.getLocation())) {
            revertBlock(block);
            breaking.remove(block.getLocation());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        Player claimOwner = claimManager.getClaimOwner(loc.getChunk());
        OfflinePlayer offlineClaimOwner = claimManager.getOfflineClaimOwner(loc.getChunk());

        if (claimOwner == null && offlineClaimOwner == null) {
            return;
        }

        UUID ownerUUID = offlineClaimOwner != null ? offlineClaimOwner.getUniqueId() : claimOwner.getUniqueId();

        if (breaking.containsKey(loc)) {
            if (raid.containsKey(ownerUUID) && raid.get(ownerUUID) == RaidStatus.RAIDING) {
                revertBlock(event.getBlock());
            } else {
                revertBlock(event.getBlock());
                raid.put(ownerUUID, RaidStatus.RAIDING);
                lastRaidActivity.put(ownerUUID, System.currentTimeMillis());
                if (offlineClaimOwner != null && offlineClaimOwner.isOnline()) {
                    Objects.requireNonNull(offlineClaimOwner.getPlayer()).sendMessage("§c(✘) Your claim is being raided by " + event.getPlayer().getName() + "!");
                } else if (claimOwner != null) {
                    claimOwner.sendMessage("§c(✘) Your claim is being raided by " + event.getPlayer().getName() + "!");
                }
                createOrUpdateBossBar(ownerUUID, claimOwner);
            }
        }
    }

    private void revertBlock(Block block) {
        Location loc = block.getLocation();
        BlockData originalData = breaking.remove(loc);
        if (originalData != null) {
            block.setBlockData(originalData);
        }
        breakingPlayers.remove(loc);
    }

    private void startRaycastCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Location, UUID> entry : new HashMap<>(breakingPlayers).entrySet()) {
                    Location blockLoc = entry.getKey();
                    UUID playerUUID = entry.getValue();
                    Player player = plugin.getServer().getPlayer(playerUUID);

                    if (player == null || !player.isOnline()) {
                        revertBlock(blockLoc.getBlock());
                        continue;
                    }

                    if (!isPlayerLookingAtBlock(player, blockLoc.getBlock())) {
                        revertBlock(blockLoc.getBlock());
                    }

                    if (player.getLocation().distance(blockLoc) > MAX_DISTANCE) {
                        revertBlock(blockLoc.getBlock());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, CHECK_INTERVAL);
    }

    private boolean isPlayerLookingAtBlock(Player player, Block block) {
        RayTraceResult result = player.rayTraceBlocks(MAX_DISTANCE);
        return result != null && result.getHitBlock() != null && result.getHitBlock().equals(block);
    }

    private void startRaidTimeoutCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                for (Map.Entry<UUID, Long> entry : new HashMap<>(lastRaidActivity).entrySet()) {
                    UUID ownerUUID = entry.getKey();
                    long lastActivityTime = entry.getValue();

                    if (currentTime - lastActivityTime > RAID_TIMEOUT) {
                        raid.remove(ownerUUID);
                        lastRaidActivity.remove(ownerUUID);

                        OfflinePlayer offlineClaimOwner = plugin.getServer().getOfflinePlayer(ownerUUID);
                        if (offlineClaimOwner != null && offlineClaimOwner.isOnline()) {
                            Objects.requireNonNull(offlineClaimOwner.getPlayer()).sendMessage("§c(✘) The raid has ended.");
                        }
                        removeBossBar(ownerUUID);
                    } else {
                        updateBossBar(ownerUUID, RAID_TIMEOUT - (currentTime - lastActivityTime));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void updateBossBar(UUID ownerUUID, long remainingTime) {
        BossBar bossBar = raidBossBars.get(ownerUUID);
        if (bossBar != null) {
            long seconds = remainingTime / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            String timeFormatted = String.format("%02d:%02d", minutes, seconds);
            Artual artual = (Artual) plugin;
            bossBar.setTitle(artual.color("&7Raid: <##7b00ec>" + timeFormatted));
            bossBar.setProgress((double) remainingTime / RAID_TIMEOUT);
        }
    }

    private void createOrUpdateBossBar(UUID ownerUUID, Player claimOwner) {
        BossBar bossBar = raidBossBars.get(ownerUUID);
        if (bossBar == null) {
            Artual artual = (Artual) plugin;
            bossBar = Bukkit.createBossBar(artual.color("&7Raid: <##7b00ec>20:00"), BarColor.PURPLE, BarStyle.SOLID);
            raidBossBars.put(ownerUUID, bossBar);
        }
        bossBar.addPlayer(claimOwner);
    }

    private void removeBossBar(UUID ownerUUID) {
        BossBar bossBar = raidBossBars.remove(ownerUUID);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void disableMethod() {
        for (BossBar bossBar : raidBossBars.values()) {
            bossBar.removeAll();
        }
        for (Map.Entry<Location, UUID> entry : new HashMap<>(breakingPlayers).entrySet()) {
            revertBlock(entry.getKey().getBlock());
        }
        for (UUID ownerUUID : raid.keySet()) {
            OfflinePlayer offlineClaimOwner = plugin.getServer().getOfflinePlayer(ownerUUID);
            if (offlineClaimOwner != null && offlineClaimOwner.isOnline()) {
                Objects.requireNonNull(offlineClaimOwner.getPlayer()).sendMessage("§c(✘) The raid has ended.");
            }
        }
    }
}