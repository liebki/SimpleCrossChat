package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.Messages;
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
            sender.sendMessage(Messages.get("disable.no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Messages.get("disable.usage"));
            return true;
        }

        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage(Messages.get("disable.player_not_found"));
            return true;
        }

        boolean currentStatus = plugin.getPlayerStateManager().isAdminDisabled(target.getUniqueId());
        plugin.getPlayerStateManager().setAdminDisabled(target.getUniqueId(), !currentStatus);

        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("player", playerName);

        if (!currentStatus) {
            sender.sendMessage(Messages.get("disable.disabled_for_player", placeholders));
            target.sendMessage(Messages.get("disable.notify_disabled"));
        } else {
            sender.sendMessage(Messages.get("disable.enabled_for_player", placeholders));
            target.sendMessage(Messages.get("disable.notify_enabled"));
        }

        return true;
    }

}
