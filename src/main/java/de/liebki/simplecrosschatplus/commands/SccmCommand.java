package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
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
            sender.sendMessage(MessageUtils.ColorConvert("&cThe /sccpm command is disabled on this server."));
            return true;
        }

        if (!sender.hasPermission("sccplus.message.crossserver")) {
            sender.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to use this command."));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.ColorConvert("&cOnly players can use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.ColorConvert("&cUsage: /sccpm <player> <message>"));
            return true;
        }

        String targetPlayer = args[0];
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Player player = (Player) sender;

        // Check if player is online locally first
        org.bukkit.entity.Player localPlayer = plugin.getServer().getPlayer(targetPlayer);
        if (localPlayer != null && localPlayer.isOnline()) {
            // Deliver locally
            localPlayer.sendMessage(MessageUtils.ColorConvert("&7[&dPM&7] &efrom " + player.getName() + "&7: &f" + message));
            sender.sendMessage(MessageUtils.ColorConvert("&a[PM] &7Message sent to &e" + targetPlayer));
            return true;
        }

        // Send across servers via MQTT
        plugin.getMqttManager().sendPrivateMessage(targetPlayer, message, player.getName());
        sender.sendMessage(MessageUtils.ColorConvert("&a[PM] &7Message sent to &e" + targetPlayer));

        return true;
    }

}

