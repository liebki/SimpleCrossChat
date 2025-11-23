package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
import org.bukkit.command.CommandSender;

public class LocateCommand {

    private final SimpleCrossChat plugin;

    public LocateCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sccplus.player.locate")) {
            sender.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to use this command."));
            return true;
        }

        boolean locateEnabled = plugin.getConfigManager().get("locate.enabled", true);
        if (!locateEnabled) {
            sender.sendMessage(MessageUtils.ColorConvert("&cThe locate command is disabled on this server."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtils.ColorConvert("&cUsage: /scc locate <playername>"));
            return true;
        }

        String targetPlayer = args[0];

        // Check if player is on this server first
        org.bukkit.entity.Player localPlayer = plugin.getServer().getPlayer(targetPlayer);
        if (localPlayer != null && localPlayer.isOnline()) {
            String serverName = plugin.getConfigManager().get("general.servername");
            String contact = plugin.getConfigManager().get("general.serverip", "");

            sender.sendMessage(MessageUtils.ColorConvert("&a&l=== Player Location ==="));
            sender.sendMessage(MessageUtils.ColorConvert("&e" + targetPlayer + " &7is on &e" + serverName));

            if (contact != null && !contact.isEmpty()) {
                sender.sendMessage(MessageUtils.ColorConvert("&7Location: &e" + contact));
            }

            // Notify the located player for privacy
            boolean notifyPlayer = plugin.getConfigManager().get("locate.notify-located-player", true);
            if (notifyPlayer) {
                localPlayer.sendMessage(MessageUtils.ColorConvert("&7[Privacy Notice] Your location was queried by &e" + sender.getName()));
            }

            return true;
        }

        // Request location from other servers
        sender.sendMessage(MessageUtils.ColorConvert("&7Searching for player: &e" + targetPlayer + "&7..."));
        plugin.getMqttManager().requestPlayerLocation(targetPlayer, sender);

        return true;
    }

}

