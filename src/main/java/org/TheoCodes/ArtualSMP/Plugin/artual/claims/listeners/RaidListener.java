package org.TheoCodes.ArtualSMP.Plugin.artual.claims.listeners;

import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.ClaimManager;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.RaidStatus;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
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

    private final Map<Location, BlockState> breakingBlocks = new HashMap<>();
    private final Map<Location, UUID> breakingPlayers = new HashMap<>();
    private static final Map<Location, Chunk> placedDuringRaid = new HashMap<>();
    private static final Map<UUID, RaidStatus> raids = new HashMap<>();
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
                if (breakingPlayers.containsValue(player.getUniqueId()) && player.isOnline()) {
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
        breakingPlayers.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(player.getUniqueId())) {
                revertBlock(entry.getKey().getBlock());
                player.removePotionEffect(PotionEffectType.HASTE);
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                return true;
            }
            return false;
        });
        removeBossBar(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (raids.containsKey(playerUUID) && raids.get(playerUUID) == RaidStatus.RAIDING) {
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

        UUID ownerUUID = getOwnerUUID(claimOwner, offlineClaimOwner);

        if (placedDuringRaid.containsKey(block.getLocation()) && raids.containsKey(claimManager.getClaimOwner(block.getChunk()).getUniqueId())) return;

        if (ownerUUID == null || ownerUUID.equals(player.getUniqueId())) return;

        if (shouldPreventDamage(block, event, offlineClaimOwner, ownerUUID, player)) return;

        if (block.getType().getHardness() < 0) return;
        
        if (claimManager.isBlockInClaimedChunk(block) && !claimManager.doesPlayerOwnChunk(player, block.getChunk()) &&
                !breakingBlocks.containsKey(block.getLocation())) {
            breakingBlocks.put(block.getLocation(), block.getState());
            breakingPlayers.put(block.getLocation(), player.getUniqueId());
            handleRaidEffects(block, player, ownerUUID);
        }
    }

    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        Block block = event.getBlock();
        if (breakingPlayers.containsKey(block.getLocation()) && breakingPlayers.get(block.getLocation()).equals(event.getPlayer().getUniqueId())) {
            revertBlock(block);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.HONEY_BLOCK) event.setDropItems(false);
        if (event.getBlock().getType() == Material.REINFORCED_DEEPSLATE) event.setDropItems(false);

        Block block = event.getBlock();
        Location loc = block.getLocation();
        Player claimOwner = claimManager.getClaimOwner(loc.getChunk());
        OfflinePlayer offlineClaimOwner = claimManager.getOfflineClaimOwner(loc.getChunk());

        UUID ownerUUID = getOwnerUUID(claimOwner, offlineClaimOwner);

        if (placedDuringRaid.containsKey(block.getLocation()) && raids.containsKey(claimManager.getClaimOwner(block.getChunk()).getUniqueId())) {
            placedDuringRaid.remove(block.getLocation());
            return;
        }

        if (ownerUUID == null || ownerUUID.equals(event.getPlayer().getUniqueId())) return;

        if (event.getBlock().getType() != Material.HONEY_BLOCK) event.setCancelled(true);
        if (event.getBlock().getType() != Material.REINFORCED_DEEPSLATE) event.setCancelled(true);

        if (breakingBlocks.containsKey(loc) && breakingPlayers.get(loc).equals(event.getPlayer().getUniqueId())) {
            handleBlockBreak(event, ownerUUID, claimOwner, offlineClaimOwner);
            revertBlock(block);
            // make player break block naturally w held item
            block.breakNaturally(event.getPlayer().getInventory().getItemInMainHand());
        }
    }


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player claimowner = claimManager.getClaimOwner(event.getBlock().getChunk());
        assert claimowner != null;
        if (claimowner != null && raids.containsKey(claimowner.getUniqueId())) {
            placedDuringRaid.put(event.getBlock().getLocation(), event.getBlock().getChunk());
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.CHORUS_FRUIT && claimManager.isPlayerNearClaim(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c(✘) You cannot use that near a claimed chunk!");
        }
    }

    private void revertBlock(Block block) {
        BlockState originalState = breakingBlocks.remove(block.getLocation());
        if (originalState != null) {
            originalState.update(true, false);
        }
        breakingPlayers.remove(block.getLocation());
    }

    private void startRaycastCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                breakingPlayers.forEach((blockLoc, playerUUID) -> {
                    Player player = plugin.getServer().getPlayer(playerUUID);
                    if (player == null || !player.isOnline() || !isPlayerLookingAtBlock(player, blockLoc.getBlock())
                            || player.getLocation().distance(blockLoc) > MAX_DISTANCE) {
                        revertBlock(blockLoc.getBlock());
                    }
                });
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
                lastRaidActivity.forEach((ownerUUID, lastActivityTime) -> {
                    if (currentTime - lastActivityTime > RAID_TIMEOUT) {
                        endRaid(ownerUUID);
                    } else {
                        updateBossBar(ownerUUID, RAID_TIMEOUT - (currentTime - lastActivityTime));
                    }
                });
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void updateBossBar(UUID ownerUUID, long remainingTime) {
        BossBar bossBar = raidBossBars.get(ownerUUID);
        if (bossBar != null) {
            long seconds = remainingTime / 1000;
            String timeFormatted = String.format("%02d:%02d", seconds / 60, seconds % 60);
            bossBar.setTitle(Artual.color("&7Raid: <##7b00ec>" + timeFormatted));
            bossBar.setProgress((double) remainingTime / RAID_TIMEOUT);
        }
    }

    private void createOrUpdateBossBar(UUID ownerUUID, Player claimOwner) {
        BossBar bossBar = raidBossBars.computeIfAbsent(ownerUUID, uuid -> {
            BossBar newBossBar = Bukkit.createBossBar(Artual.color("&7Raid: <##7b00ec>20:00"), BarColor.PURPLE, BarStyle.SOLID);
            newBossBar.addPlayer(claimOwner);
            return newBossBar;
        });
        bossBar.addPlayer(claimOwner);
    }

    private void removeBossBar(UUID ownerUUID) {
        BossBar bossBar = raidBossBars.remove(ownerUUID);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void onDisable() {
        raidBossBars.values().forEach(BossBar::removeAll);
        breakingBlocks.keySet().forEach(location -> revertBlock(location.getBlock()));
        raids.keySet().forEach(this::endRaid);
    }

    private void endRaid(UUID ownerUUID) {
        raids.remove(ownerUUID);
        lastRaidActivity.remove(ownerUUID);
        OfflinePlayer offlineClaimOwner = plugin.getServer().getOfflinePlayer(ownerUUID);
        if (offlineClaimOwner.isOnline()) {
            Objects.requireNonNull(offlineClaimOwner.getPlayer()).sendMessage(ChatColor.RED + "(✘) The raid has ended.");
        }
        removeBossBar(ownerUUID);
    }

    private UUID getOwnerUUID(Player claimOwner, OfflinePlayer offlineClaimOwner) {
        if (claimOwner != null) {
            return claimOwner.getUniqueId();
        } else if (offlineClaimOwner != null) {
            return offlineClaimOwner.getUniqueId();
        }
        return null;
    }

    private boolean shouldPreventDamage(Block block, BlockDamageEvent event, OfflinePlayer offlineClaimOwner, UUID ownerUUID, Player player) {
        if (offlineClaimOwner != null && !offlineClaimOwner.isOnline() && !raids.containsKey(ownerUUID) &&
                !breakingBlocks.containsKey(block.getLocation()) && !breakingPlayers.containsKey(block.getLocation())) {
            event.setCancelled(event.getInstaBreak());
            breakingBlocks.put(block.getLocation(), block.getState());
            breakingPlayers.put(block.getLocation(), player.getUniqueId());

            block.setType(Material.BEDROCK);
            applyEffect(player, PotionEffectType.MINING_FATIGUE, 255);
            player.sendMessage("§c(✘) The claim owner is not online.");
            return true;
        }
        return false;
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

    private void handleBlockBreak(BlockBreakEvent event, UUID ownerUUID, Player claimOwner, OfflinePlayer offlineClaimOwner) {
        revertBlock(event.getBlock());
        raids.put(ownerUUID, RaidStatus.RAIDING);
        lastRaidActivity.put(ownerUUID, System.currentTimeMillis());

        if (offlineClaimOwner != null && offlineClaimOwner.isOnline()) {
            Objects.requireNonNull(offlineClaimOwner.getPlayer()).sendMessage("§c(✘) Your claim is being raided by " + event.getPlayer().getName() + "!");
        } else if (claimOwner != null) {
            claimOwner.sendMessage("§c(✘) Your claim is being raided by " + event.getPlayer().getName() + "!");
        }
        createOrUpdateBossBar(ownerUUID, claimOwner);
    }
}
