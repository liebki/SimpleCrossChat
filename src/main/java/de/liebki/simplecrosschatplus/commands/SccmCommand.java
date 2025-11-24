package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SccmCommand implements CommandExecutor {

    private final SimpleCrossChat plugin;

    public SccmCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return execute(sender, args);
    }

    public boolean execute(CommandSender sender, String[] args) {
        // Check if cross-server PM is enabled - handle both boolean and string "false"
        Object pmEnabledObj = plugin.getConfigManager().getConfig().get("crossserverpm.enabled");
        boolean pmEnabled = pmEnabledObj == null || pmEnabledObj.toString().equalsIgnoreCase("true");

        if (!pmEnabled) {
            sender.sendMessage(Messages.get("pm.disabled"));
            return true;
        }


        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("pm.only_players"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Messages.get("pm.usage"));
            return true;
        }

        String targetPlayer = args[0];
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Player player = (Player) sender;

        // Check if player is online locally first
        org.bukkit.entity.Player localPlayer = plugin.getServer().getPlayer(targetPlayer);
        if (localPlayer != null && localPlayer.isOnline()) {
            // Deliver locally
            java.util.Map<String, String> recvPlaceholders = new java.util.HashMap<>();
            recvPlaceholders.put("sender", player.getName());
            recvPlaceholders.put("message", message);
            localPlayer.sendMessage(Messages.get("pm.received_local", recvPlaceholders));

            java.util.Map<String, String> sentPlaceholders = new java.util.HashMap<>();
            sentPlaceholders.put("target", targetPlayer);
            sender.sendMessage(Messages.get("pm.sent", sentPlaceholders));
            return true;
        }

        // Send across servers via MQTT
        plugin.getMqttManager().sendPrivateMessage(targetPlayer, message, player.getName());

        java.util.Map<String, String> sentPlaceholders = new java.util.HashMap<>();
        sentPlaceholders.put("target", targetPlayer);
        sender.sendMessage(Messages.get("pm.sent", sentPlaceholders));

        return true;
    }

}
