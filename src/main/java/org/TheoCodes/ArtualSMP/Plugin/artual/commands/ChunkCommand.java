package org.TheoCodes.ArtualSMP.Plugin.artual.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.TheoCodes.ArtualSMP.Plugin.artual.Artual;
import org.TheoCodes.ArtualSMP.Plugin.artual.claims.ClaimManager;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class ChunkCommand implements CommandExecutor, TabExecutor {

    private JavaPlugin plugin;
    private Artual artual;
    private ClaimManager claimManager;
    private File playerDataFile;
    private Set<String> knownPlayers;
    private Gson gson;

    public ChunkCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.artual = (Artual) plugin;
        this.claimManager = new ClaimManager(plugin);
        artual.getCommand("chunk").setExecutor(this);

        this.playerDataFile = new File(plugin.getDataFolder(), "known_players.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.knownPlayers = loadKnownPlayers();
    }

    public Set<String> loadKnownPlayers() {
        if (!playerDataFile.exists()) {
            return new HashSet<>();
        }

        try (Reader reader = new FileReader(playerDataFile)) {
            Type setType = new TypeToken<HashSet<String>>(){}.getType();
            return gson.fromJson(reader, setType);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    public void saveKnownPlayers() {
        try (Writer writer = new FileWriter(playerDataFile)) {
            gson.toJson(knownPlayers, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addKnownPlayer(String playerName) {
        knownPlayers.add(playerName);
        saveKnownPlayers();
    }

    public boolean isKnownPlayer(String playerName) {
        return knownPlayers.contains(playerName);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "(✘) This command can only be used by players.");
            return true;
        }

        Player player = (Player) commandSender;
        addKnownPlayer(player.getName());

        if (command.getName().equalsIgnoreCase("chunk")) {
            if (args.length == 0) {
                sendUsage(player);
                return true;
            }

            String action = args[0].toLowerCase();

            switch (action) {
                case "claim":
                    claimManager.attemptClaim(player);
                    return true;
                case "unclaim":
                    claimManager.attemptDelete(player);
                    return true;
                case "trust":
                    handleTrustedPlayer(player, args);
                    return true;
                case "admin":
                    if (player.hasPermission("claims.admin")) {
                        handleAdmin(player, args);
                        return true;
                    }
                    sendUsage(player);
                    return true;
                default:
                    sendUsage(player);
                    return true;
            }
        }
        sendUsage(player);
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Artual.color("&6&lClaims &7» &f/chunk &7(&6claim/unclaim/trust&7)"));
    }

    private void handleTrustedPlayer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Artual.color("&6&lClaims &7» &f/chunk &ftrust &7(&6add/remove/list&7) &6<player>"));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add":
            case "remove":
                if (args.length < 3) {
                    player.sendMessage(Artual.color("&6&lClaims &7» &fPlease specify a player name."));
                    return;
                }
                String trustedName = args[2];
                if (!isKnownPlayer(trustedName)) {
                    player.sendMessage(Artual.color("&6&lClaims &7» &fThat player has never joined!"));
                    return;
                }
                OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedName);
                handleAddRemoveTrust(player, trusted, action);
                break;
            case "list":
                handleListTrusted(player);
                break;
            default:
                player.sendMessage(Artual.color("&6&lClaims &7» &fInvalid action! Use <add|remove|list>."));
                break;
        }
    }

    private void handleAddRemoveTrust(Player player, OfflinePlayer trusted, String action) {
        if (action.equals("add")) {
            if (claimManager.isTrusted(player, trusted)) {
                player.sendMessage(Artual.color("&6&lClaims &7» &fThat player is already trusted."));
                return;
            }
            else if (player.getUniqueId().equals(trusted.getUniqueId())) {
                player.sendMessage(Artual.color("&6&lClaims &7» &fDo you not trust yourself? You can not trust yourself."));
                return;
            }
            claimManager.addTrusted(player, trusted);
            player.sendMessage(Artual.color("&6&lClaims &7» &fYou have trusted &6" + trusted.getName()));
        } else {
            if (!claimManager.isTrusted(player, trusted)) {
                player.sendMessage(Artual.color("&6&lClaims &7» &fThat player is not trusted."));
                return;
            }
            claimManager.removeTrusted(player, trusted);
            player.sendMessage(Artual.color("&6&lClaims &7» &fYou have removed trust for &6" + trusted.getName()));
        }
    }

    private void handleListTrusted(Player player) {
        List<OfflinePlayer> trustedPlayers = claimManager.getTrustedPlayers(player.getUniqueId());
        if (trustedPlayers.isEmpty()) {
            player.sendMessage(Artual.color("&6&lClaims &7» &fYou have not trusted any players."));
            return;
        }

        StringBuilder message = new StringBuilder(Artual.color("&6&lClaims &7» &fTrusted players:"));
        for (OfflinePlayer offlinePlayer : trustedPlayers) {
            if (offlinePlayer.isOnline()) {
                message.append("\n&7- &6").append(offlinePlayer.getName()).append(" &a⏺");
            }
            else if (!offlinePlayer.isOnline()) {
                message.append("\n&7- &6").append(offlinePlayer.getName()).append(" &c⏺");
            }
        }
        player.sendMessage(Artual.color(message.toString()));
    }

    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("chunk.admin")) {
            sendUsage(player);
            return;
        }
        if (args.length <= 1) {
            player.sendMessage(Artual.color("&6&lClaims &7» &f/chunk &fadmin &7(&6clear/list/info/blacklist&7)"));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "clear":
                handleAdminClear(player, args);
                break;
            case "list":
                handleAdminList(player, args);
                break;
            case "info":
                handleAdminInfo(player);
                break;
            case "blacklist":
                handleAdminBlacklist(player, args);
                break;
            case "forceunclaim":
                handleAdminTeleport(player, args);
                break;
            case "bypass":
                handleAdminTeleport(player, args);
                break;
            default:
                player.sendMessage(Artual.color("&6&lClaims &7» &fInvalid action! Use <clear/list/info/blacklist>."));
                break;
        }
    }

    private void handleAdminClear(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Artual.color("&6&lClaims &7» &fPlease specify a player name."));
            return;
        }
        String targetName = args[2];
        if (!isKnownPlayer(targetName)) {
            player.sendMessage(Artual.color("&6&lClaims &7» &fThat player has never joined!"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        claimManager.clearPlayerData(target);
        player.sendMessage(Artual.color("&6&lClaims &7» &fYou have cleared all the data from &6" + targetName));
    }

    private void handleAdminList(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Artual.color("&6&lClaims &7» &fPlease specify a player name."));
            return;
        }

        String listTargetName = args[2];
        if (!isKnownPlayer(listTargetName)) {
            player.sendMessage(Artual.color("&6&lClaims &7» &fThat player has never joined!"));
            return;
        }

        OfflinePlayer listTarget = Bukkit.getOfflinePlayer(listTargetName);
        List<Chunk> claims = claimManager.getPlayerClaims(listTarget);

        if (claims.isEmpty()) {
            player.sendMessage(Artual.color("&6&lClaims &7» &fThat player has no claimed chunks."));
            return;
        }

        player.sendMessage(Artual.color("&6&lClaims &7» &f" + listTargetName + "'s claimed chunks:"));

        int count = 1;
        for (Chunk chunk : claims) {
            String chunkID = claimManager.getChunkID(chunk);
            String claimID = claimManager.getClaimID(chunk);

            World world = chunk.getWorld();
            int blockX = chunk.getX() * 16 + 8;
            int blockZ = chunk.getZ() * 16 + 8;
            int blockY = world.getHighestBlockYAt(blockX, blockZ) + 1;

            String formattedCoords = String.format("\"%s\", %d, %d, %d", chunk.getWorld().getName(), blockX, blockY, blockZ);

            TextComponent chunkMessage = new TextComponent(Artual.color("&7- &fChunk #" + count + ": &6" + chunkID));
            chunkMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chunk admin tp " + chunk.getWorld().getName() + " " + chunk.getX() + " " + chunk.getZ()));

            chunkMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                    Artual.color("&6Click to teleport\n&fChunk ID: &6" + chunkID + "\n&fClaim ID: &6" + claimID + "\n&fCoordinates: &6" + formattedCoords)
            ).create()));

            player.spigot().sendMessage(chunkMessage);
            count++;
        }
    }

    private void handleAdminInfo(Player player) {
        Chunk currentChunk = player.getLocation().getChunk();
        String chunkID = claimManager.getChunkID(currentChunk);
        String claimID = claimManager.getClaimID(currentChunk);
        OfflinePlayer owner = claimManager.getOfflineClaimOwner(currentChunk);

        StringBuilder message = new StringBuilder(Artual.color("&6&lClaims &7» &fCurrent chunk information:"));
        message.append("\n&7- &f").append("Owner: &6").append(owner != null ? owner.getName() : "None");
        message.append("\n&7- &f").append("Chunk-ID: &6").append(chunkID);
        message.append("\n&7- &f").append("Claim-ID: &6").append(claimID != null ? claimID : "Unclaimed");
        player.sendMessage(Artual.color(message.toString()));
    }

    private void handleAdminBlacklist(Player player, String[] args) {
        player.sendMessage(Artual.color("&6&lClaims &7» &fBlacklist coming soon."));
    }

    private void handleAdminTeleport(Player player, String[] args) {
        if (args.length != 5) {
            player.sendMessage(Artual.color("&6&lClaims &7» &fUsage: /chunk admin tp <world> <chunkX> <chunkZ>"));
            return;
        }

        String worldName = args[2];
        int chunkX, chunkZ;

        try {
            chunkX = Integer.parseInt(args[3]);
            chunkZ = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            player.sendMessage(Artual.color("&6&lClaims &7» &fInvalid chunk coordinates. Please use numbers for X and Z."));
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(Artual.color("&6&lClaims &7» &fInvalid world name: " + worldName));
            return;
        }

        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        int blockX = chunkX * 16 + 8;
        int blockZ = chunkZ * 16 + 8;
        int y = world.getHighestBlockYAt(blockX, blockZ) + 1;

        Location location = new Location(world, blockX, y, blockZ);
        player.teleport(location);
        player.sendMessage(Artual.color("&6&lClaims &7» &fTeleported to chunk at X: " + blockX + ", Z: " + blockZ + " in world: " + worldName));
        return;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("chunk")) {
            if (args.length == 1) {
                List<String> subCommands = new ArrayList<>();
                subCommands.add("claim");
                subCommands.add("unclaim");
                subCommands.add("trust");
                if (player.hasPermission("claims.admin")) subCommands.add("admin");

                for (String subCommand : subCommands) {
                    if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(subCommand);
                    }
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("trust")) {
                List<String> trustSubCommands = new ArrayList<>();
                trustSubCommands.add("add");
                trustSubCommands.add("remove");
                trustSubCommands.add("list");

                for (String trustSubCommand : trustSubCommands) {
                    if (trustSubCommand.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(trustSubCommand);
                    }
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("trust") && args[1].equalsIgnoreCase("add")) {
                for (String knownPlayer : knownPlayers) {
                    if (knownPlayer.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(knownPlayer);
                    }
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("trust") && args[1].equalsIgnoreCase("remove")) {
                List<OfflinePlayer> players = claimManager.getTrustedPlayers(player.getUniqueId());
                if (players != null && !players.isEmpty()) {
                    for (OfflinePlayer trustedPlayer : players) {
                        if (trustedPlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(trustedPlayer.getName());
                        }
                    }
                }
            }

            } if (args[0].equalsIgnoreCase("admin")) {
                if (args.length == 2) {
                    if (player.hasPermission("claims.admin")) {
                        List<String> adminSubCommands = new ArrayList<>();
                        adminSubCommands.add("clear");
                        adminSubCommands.add("list");
                        adminSubCommands.add("info");
                        adminSubCommands.add("blacklist");
                        adminSubCommands.add("tp");

                        for (String adminSubCommand : adminSubCommands) {
                            if (adminSubCommand.toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(adminSubCommand);
                            }
                        }
                    }
                } else if (args.length == 3 && (args[1].equalsIgnoreCase("clear") || args[1].equalsIgnoreCase("list"))) {
                    for (String knownPlayer : knownPlayers) {
                        if (knownPlayer.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(knownPlayer);
                        }
                    }
                }
            }
        return completions;
    }
}
