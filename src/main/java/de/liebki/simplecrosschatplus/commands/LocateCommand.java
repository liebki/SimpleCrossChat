package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.Messages;
import org.bukkit.command.CommandSender;

public class LocateCommand {

    private final SimpleCrossChat plugin;

    public LocateCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sccplus.player.locate")) {
            sender.sendMessage(Messages.get("locate.no_permission"));
            return true;
        }

        boolean locateEnabled = plugin.getConfigManager().get("locate.enabled", true);
        if (!locateEnabled) {
            sender.sendMessage(Messages.get("locate.disabled"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Messages.get("locate.usage"));
            return true;
        }

        String targetPlayer = args[0];

        // Check if player is on this server first
        org.bukkit.entity.Player localPlayer = plugin.getServer().getPlayer(targetPlayer);
        if (localPlayer != null && localPlayer.isOnline()) {
            String serverName = plugin.getConfigManager().get("general.servername");
            String contact = plugin.getConfigManager().get("general.serverip", "");

            sender.sendMessage(Messages.get("locate.player_header"));

            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("player", targetPlayer);
            placeholders.put("server", serverName);
            sender.sendMessage(Messages.get("locate.player_location", placeholders));

            if (contact != null && !contact.isEmpty()) {
                java.util.Map<String, String> contactPlaceholders = new java.util.HashMap<>();
                contactPlaceholders.put("contact", contact);
                sender.sendMessage(Messages.get("locate.player_contact", contactPlaceholders));
            }

            // Notify the located player for privacy
            boolean notifyPlayer = plugin.getConfigManager().get("locate.notify-located-player", true);
            if (notifyPlayer) {
                java.util.Map<String, String> noticePlaceholders = new java.util.HashMap<>();
                noticePlaceholders.put("requester", sender.getName());
                localPlayer.sendMessage(Messages.get("locate.privacy_notice", noticePlaceholders));
            }

            return true;
        }

        // Request location from other servers
        java.util.Map<String, String> searchPlaceholders = new java.util.HashMap<>();
        searchPlaceholders.put("player", targetPlayer);
        sender.sendMessage(Messages.get("locate.searching", searchPlaceholders));
        plugin.getMqttManager().requestPlayerLocation(targetPlayer, sender);

        return true;
    }

}
