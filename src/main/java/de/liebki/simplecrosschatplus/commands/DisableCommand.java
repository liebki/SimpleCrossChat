package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DisableCommand {

    private final SimpleCrossChat plugin;

    public DisableCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sccplus.admin.disable")) {
            sender.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtils.ColorConvert("&cUsage: /scc disable <player>"));
            return true;
        }

        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage(MessageUtils.ColorConvert("&cPlayer not found or not online."));
            return true;
        }

        boolean currentStatus = plugin.getPlayerStateManager().isAdminDisabled(target.getUniqueId());
        plugin.getPlayerStateManager().setAdminDisabled(target.getUniqueId(), !currentStatus);

        if (!currentStatus) {
            sender.sendMessage(MessageUtils.ColorConvert("&aDisabled cross-server functionality for " + playerName));
            target.sendMessage(MessageUtils.ColorConvert("&cYour cross-server functionality has been disabled by an admin."));
        } else {
            sender.sendMessage(MessageUtils.ColorConvert("&aEnabled cross-server functionality for " + playerName));
            target.sendMessage(MessageUtils.ColorConvert("&aYour cross-server functionality has been enabled."));
        }

        return true;
    }

}

