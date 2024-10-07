package org.TheoCodes.ArtualSMP.Plugin.artual.claims.listeners;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.ClaimManager;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.RaidStatus;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class RaidListener implements Listener {

    private final JavaPlugin plugin;
    private final ClaimManager claimManager;

    private static final long CHECK_INTERVAL = 1L;
    private static final double MAX_DISTANCE = 5.0;
    private static final long RAID_TIMEOUT = 20 * 60 * 1000L;

    private final Map<Location, BlockState> breakingBlocks = new HashMap<>();
    private final Map<Location, UUID> breakingPlayers = new HashMap<>();
    private static final Map<Location, Chunk> placedDuringRaid = new HashMap<>();
    private static final Map<UUID, RaidStatus> raids = new HashMap<>();
    private static final Map<UUID, Long> lastRaidActivity = new HashMap<>();
    private static final Map<UUID, Set<UUID>> activeRaids = new HashMap<>();
    private static final Map<UUID, Map<UUID, BossBar>> raidBossBars = new HashMap<>();

    public RaidListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.claimManager = new ClaimManager(plugin);
        startRaycastCheck();
        startRaidTimeoutCheck();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handlePlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handlePlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        handleBlockDamage(event);
    }

    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        handleBlockDamageAbort(event);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockBreak(event);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        handleBlockPlace(event);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        handleItemConsume(event);
    }

    private void handlePlayerQuit(Player player) {
        UUID playerUUID = player.getUniqueId();
        removeBreakingBlocksForPlayer(playerUUID);
        removePlayerFromBossBars(playerUUID);
    }

    private void removeBreakingBlocksForPlayer(UUID playerUUID) {
        breakingPlayers.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(playerUUID)) {
                revertBlock(entry.getKey().getBlock());
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    player.removePotionEffect(PotionEffectType.HASTE);
                    player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                }
                return true;
            }
            return false;
        });
    }

    private void removePlayerFromBossBars(UUID playerUUID) {
        Map<UUID, BossBar> playerBossBars = raidBossBars.get(playerUUID);
        if (playerBossBars != null) {
            playerBossBars.values().forEach(bossBar -> bossBar.removePlayer(Objects.requireNonNull(Bukkit.getPlayer(playerUUID))));
        }

        raidBossBars.values().forEach(raiderBossBars ->
                raiderBossBars.values().forEach(bossBar -> bossBar.removePlayer(Objects.requireNonNull(Bukkit.getPlayer(playerUUID))))
        );
    }

    private void handlePlayerJoin(Player player) {
        UUID playerUUID = player.getUniqueId();
        resumeRaidBossBars(player);
        if (raids.containsKey(playerUUID) && raids.get(playerUUID) == RaidStatus.RAIDING) {
            resumeRaidBossBarsForRaidedPlayer(player);
        }
    }

    private void resumeRaidBossBars(Player player) {
        UUID playerUUID = player.getUniqueId();
        for (Map.Entry<UUID, Set<UUID>> entry : activeRaids.entrySet()) {
            UUID raiderUUID = entry.getKey();
            Set<UUID> raidedPlayers = entry.getValue();
            if (raiderUUID.equals(playerUUID)) {
                for (UUID raidedPlayerUUID : raidedPlayers) {
                    long remainingTime = getRemainingRaidTime(raidedPlayerUUID);
                    if (remainingTime > 0) {
                        createOrUpdateBossBar(raidedPlayerUUID, playerUUID, player);
                        updateBossBar(raidedPlayerUUID, playerUUID, remainingTime);
                    }
                }
            }
        }
    }

    private void resumeRaidBossBarsForRaidedPlayer(Player player) {
        UUID playerUUID = player.getUniqueId();
        for (Map.Entry<UUID, Set<UUID>> entry : activeRaids.entrySet()) {
            UUID raiderUUID = entry.getKey();
            Set<UUID> raidedPlayers = entry.getValue();
            if (raidedPlayers.contains(playerUUID)) {
                long remainingTime = getRemainingRaidTime(playerUUID);
                if (remainingTime > 0) {
                    Player raider = Bukkit.getPlayer(raiderUUID);
                    if (raider != null && raider.isOnline()) {
                        createOrUpdateBossBar(playerUUID, raiderUUID, raider);
                        updateBossBar(playerUUID, raiderUUID, remainingTime);
                    }
                }
            }
        }
    }

    private void handleBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (!isBlockInClaimedChunk(block)) return;
        if (isPlayerAllowedToBreak(player, block)) return;
        if (isBlockPlacedDuringRaid(block)) return;
        if (isUnbreakableBlock(block)) return;

        event.setCancelled(event.getInstaBreak());

        if (!breakingBlocks.containsKey(block.getLocation())) {
            initializeBlockBreaking(block, player);
        }
    }

    private boolean isBlockInClaimedChunk(Block block) {
        return claimManager.isClaimed(block.getChunk());
    }

    private boolean isPlayerAllowedToBreak(Player player, Block block) {
        return claimManager.isTrusted(Bukkit.getOfflinePlayer(claimManager.getClaimOwnerUUID(block.getChunk())), player) || claimManager.doesPlayerOwnChunk(player, block.getChunk());
    }

    private boolean isBlockPlacedDuringRaid(Block block) {
        return placedDuringRaid.containsKey(block.getLocation()) &&
                raids.containsKey(claimManager.getClaimOwner(block.getChunk()).getUniqueId());
    }

    private boolean isUnbreakableBlock(Block block) {
        return block.getType().getHardness() < 0;
    }

    private void initializeBlockBreaking(Block block, Player player) {
        UUID ownerUUID = claimManager.getOfflineClaimOwner(block.getChunk()).getUniqueId();
        if (!claimManager.isTeamOffline(ownerUUID)) {
            breakingBlocks.put(block.getLocation(), block.getState());
            breakingPlayers.put(block.getLocation(), player.getUniqueId());
            handleRaidEffects(block, player, ownerUUID);
        }
        else {
            handleOfflineEffects(block, player);
        }
    }

    private void handleBlockDamageAbort(BlockDamageAbortEvent event) {
        Block block = event.getBlock();
        if (breakingPlayers.containsKey(block.getLocation()) &&
                breakingPlayers.get(block.getLocation()).equals(event.getPlayer().getUniqueId())) {
            revertBlock(block);
        }
    }

    private void handleBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (!isBlockInClaimedChunk(block)) return;
        if (isPlayerAllowedToBreak(player, block)) return;

        if (isBlockPlacedDuringRaid(block)) {
            placedDuringRaid.remove(block.getLocation());
            return;
        }

        if (claimManager.isTeamOffline(claimManager.getClaimOwnerUUID(block.getChunk()))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c(✘) You cannot mine that as the team is offline!");
            return;
        }

        if (block.getType() != Material.HONEYCOMB_BLOCK && block.getType() != Material.REINFORCED_DEEPSLATE) {
            event.setCancelled(true);
            return;
        }

        if (isBlockBeingBroken(block, player)) {
            handleSuccessfulBlockBreak(block, player);
        }
    }

    private boolean isBlockBeingBroken(Block block, Player player) {
        return breakingBlocks.containsKey(block.getLocation()) &&
                breakingPlayers.get(block.getLocation()).equals(player.getUniqueId());
    }

    private void handleSuccessfulBlockBreak(Block block, Player player) {
        UUID ownerUUID = getOwnerUUID(block.getChunk());
        handleRaidStart(ownerUUID, player);
        revertBlock(block);
        block.breakNaturally(player.getInventory().getItemInMainHand());
    }

    private void handleBlockPlace(BlockPlaceEvent event) {
        if (!claimManager.isClaimed(event.getBlock().getChunk())) return;
        OfflinePlayer claimOwner = claimManager.getOfflineClaimOwner(event.getBlock().getChunk());
        if (claimOwner != null && raids.containsKey(claimOwner.getUniqueId())) {
            placedDuringRaid.put(event.getBlock().getLocation(), event.getBlock().getChunk());
        }
    }

    private void handleItemConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.CHORUS_FRUIT && claimManager.isPlayerNearClaim(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c(✘) You cannot use that near a claimed chunk!");
        }
    }

    private void handleRaidEffects(Block block, Player player, UUID ownerUUID) {
        if (raids.containsKey(ownerUUID) && raids.get(ownerUUID) == RaidStatus.RAIDING) {
            block.setType(Material.HONEYCOMB_BLOCK);
            applyEffect(player, PotionEffectType.MINING_FATIGUE, 1);
        } else {
            block.setType(Material.REINFORCED_DEEPSLATE);
            applyEffect(player, PotionEffectType.HASTE, 0);
        }
        lastRaidActivity.put(ownerUUID, System.currentTimeMillis());
    }

    private void handleOfflineEffects(Block block, Player player) {
        breakingBlocks.put(block.getLocation(), block.getState());
        breakingPlayers.put(block.getLocation(), player.getUniqueId());
        block.setType(Material.BEDROCK);
        applyEffect(player, PotionEffectType.MINING_FATIGUE, 255);
        player.sendMessage("§c(✘) You cannot mine that as the team is offline!");
    }


    private void handleRaidStart(UUID ownerUUID, Player raider) {
        raids.put(ownerUUID, RaidStatus.RAIDING);
        lastRaidActivity.put(ownerUUID, System.currentTimeMillis());

        UUID raiderUUID = raider.getUniqueId();
        notifyClaimOwner(ownerUUID, raider.getName());
        createOrUpdateBossBar(ownerUUID, raiderUUID, raider);
    }

    private void notifyClaimOwner(UUID ownerUUID, String raiderName) {
        OfflinePlayer offlineOwner = Bukkit.getOfflinePlayer(ownerUUID);
        String message = "§c(✘) Your claim is being raided by " + raiderName + "!";
        if (offlineOwner.isOnline()) {
            Objects.requireNonNull(offlineOwner.getPlayer()).sendMessage(message);
        }
        String trustedMessage = "§c(✘) " + offlineOwner.getName() + "'s claim is being raided by " + raiderName + "!";
        for (OfflinePlayer trusted : claimManager.getTrustedPlayers(ownerUUID)) {
            if (trusted.isOnline()) {
                Objects.requireNonNull(trusted.getPlayer()).sendMessage(trustedMessage);
            }
        }
    }

    private void revertBlock(Block block) {
        BlockState originalState = breakingBlocks.remove(block.getLocation());
        if (originalState != null) {
            originalState.update(true, false);
        }
        breakingPlayers.remove(block.getLocation());
    }

    private UUID getOwnerUUID(Chunk chunk) {
        return claimManager.getOfflineClaimOwner(chunk).getUniqueId();
    }


    private void applyEffect(Player player, PotionEffectType effect, int amplifier) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (breakingPlayers.containsValue(player.getUniqueId()) && player.isOnline()) {
                    player.addPotionEffect(new PotionEffect(effect, PotionEffect.INFINITE_DURATION, amplifier, true, false));
                } else {
                    player.removePotionEffect(effect);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private long getRemainingRaidTime(UUID raidedPlayerUUID) {
        Long lastActivity = lastRaidActivity.get(raidedPlayerUUID);
        return lastActivity != null ? Math.max(0, RAID_TIMEOUT - (System.currentTimeMillis() - lastActivity)) : 0;
    }

    private void createOrUpdateBossBar(UUID raidedPlayerUUID, UUID raiderUUID, Player raider) {
        String raidedPlayerName = Bukkit.getOfflinePlayer(raidedPlayerUUID).getName();
        BossBar bossBar = raidBossBars
                .computeIfAbsent(raiderUUID, k -> new HashMap<>())
                .computeIfAbsent(raidedPlayerUUID, k -> Bukkit.createBossBar(
                        Artual.color("&7Raid - " + raidedPlayerName),
                        BarColor.PURPLE,
                        BarStyle.SOLID
                ));

        bossBar.addPlayer(raider);

        Player raidedPlayer = Bukkit.getPlayer(raidedPlayerUUID);
        if (raidedPlayer != null) {
            bossBar.addPlayer(raidedPlayer);
            for (OfflinePlayer trustedPlayer : claimManager.getTrustedPlayers(raidedPlayerUUID)) {
                if (trustedPlayer != null && trustedPlayer.isOnline()) {
                    bossBar.addPlayer(Objects.requireNonNull(Bukkit.getPlayer(Objects.requireNonNull(trustedPlayer.getName()))));
                }
            }
        }

        activeRaids.computeIfAbsent(raiderUUID, k -> new HashSet<>()).add(raidedPlayerUUID);
    }

    private void updateBossBar(UUID raidedPlayerUUID, UUID raiderUUID, long remainingTime) {
        BossBar bossBar = raidBossBars.getOrDefault(raiderUUID, Collections.emptyMap()).get(raidedPlayerUUID);
        if (bossBar != null) {
            long seconds = remainingTime / 1000;
            String timeFormatted = String.format("%02d:%02d", seconds / 60, seconds % 60);
            bossBar.setTitle(Artual.color("&7Raid - " + Bukkit.getOfflinePlayer(raidedPlayerUUID).getName() + ": <##7b00ec>" + timeFormatted));
            bossBar.setProgress((double) remainingTime / RAID_TIMEOUT);
        }
    }

    private void removeBossBar(UUID raidedPlayerUUID, UUID raiderUUID) {
        Map<UUID, BossBar> raiderBossBars = raidBossBars.get(raiderUUID);
        if (raiderBossBars != null) {
            BossBar bossBar = raiderBossBars.remove(raidedPlayerUUID);
            if (bossBar != null) {
                bossBar.removeAll();
            }
            if (raiderBossBars.isEmpty()) {
                raidBossBars.remove(raiderUUID);
            }
        }

        Set<UUID> raidedPlayers = activeRaids.get(raiderUUID);
        if (raidedPlayers != null) {
            raidedPlayers.remove(raidedPlayerUUID);
            if (raidedPlayers.isEmpty()) {
                activeRaids.remove(raiderUUID);
            }
        }
    }

    private void startRaycastCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<Location, UUID>> iterator = breakingPlayers.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Location, UUID> entry = iterator.next();
                    Location blockLoc = entry.getKey();
                    UUID playerUUID = entry.getValue();
                    Player player = plugin.getServer().getPlayer(playerUUID);
                    if (player == null || !player.isOnline() || !isPlayerLookingAtBlock(player, blockLoc.getBlock())
                            || player.getLocation().distance(blockLoc) > MAX_DISTANCE) {
                        revertBlock(blockLoc.getBlock());
                        iterator.remove();
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
                for (UUID raiderUUID : new HashSet<>(activeRaids.keySet())) {
                    for (UUID raidedPlayerUUID : new HashSet<>(activeRaids.get(raiderUUID))) {
                        Long lastActivityTime = lastRaidActivity.get(raidedPlayerUUID);
                        if (lastActivityTime == null) continue;

                        if (currentTime - lastActivityTime > RAID_TIMEOUT) {
                            endRaid(raidedPlayerUUID, raiderUUID);
                        } else {
                            updateBossBar(raidedPlayerUUID, raiderUUID, RAID_TIMEOUT - (currentTime - lastActivityTime));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void endRaid(UUID raidedPlayerUUID, UUID raiderUUID) {
        raids.remove(raidedPlayerUUID);
        lastRaidActivity.remove(raidedPlayerUUID);
        OfflinePlayer offlineRaidedPlayer = plugin.getServer().getOfflinePlayer(raidedPlayerUUID);
        if (offlineRaidedPlayer.isOnline()) {
            Objects.requireNonNull(offlineRaidedPlayer.getPlayer()).sendMessage(ChatColor.RED + "(✘) The raid has ended.");
        }
        removeBossBar(raidedPlayerUUID, raiderUUID);
    }

    public void onDisable() {
        for (Map<UUID, BossBar> raiderBossBars : raidBossBars.values()) {
            for (BossBar bossBar : raiderBossBars.values()) {
                bossBar.removeAll();
            }
        }
        raidBossBars.clear();
        activeRaids.clear();
        breakingBlocks.keySet().forEach(location -> revertBlock(location.getBlock()));
        raids.clear();
        lastRaidActivity.clear();
    }
}