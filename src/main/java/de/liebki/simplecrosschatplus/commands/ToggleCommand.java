package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleCommand {

    private final SimpleCrossChat plugin;

    public ToggleCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String subCommand, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("toggle.only_players"));
            return true;
        }

        Player player = (Player) sender;

        if (subCommand.equals("disabled")) {
            return handleDisabledToggle(player);
        } else if (subCommand.equals("notify")) {
            return handleNotifyToggle(player, args);
        }

        return true;
    }

    private boolean handleDisabledToggle(Player player) {
        if (!player.hasPermission("sccplus.toggle.disabled")) {
            player.sendMessage(Messages.get("toggle.no_permission_disabled"));
            return true;
        }

        boolean currentStatus = plugin.getPlayerStateManager().isChatDisabled(player.getUniqueId());
        plugin.getPlayerStateManager().setChatDisabled(player.getUniqueId(), !currentStatus);

        if (!currentStatus) {
            player.sendMessage(Messages.get("toggle.chat_disabled"));
        } else {
            player.sendMessage(Messages.get("toggle.chat_enabled"));
        }

        return true;
    }

    private boolean handleNotifyToggle(Player player, String[] args) {
        if (!player.hasPermission("sccplus.toggle.notify")) {
            player.sendMessage(Messages.get("toggle.no_permission_notify"));
            return true;
        }

        if (args.length < 1) {
            boolean currentStatus = plugin.getPlayerStateManager().isNotifyDisabled(player.getUniqueId());
            java.util.Map<String, String> statusPlaceholders = new java.util.HashMap<>();
            statusPlaceholders.put("status", currentStatus ? "&cOFF" : "&aON");
            player.sendMessage(Messages.get("toggle.notify_status", statusPlaceholders));
            player.sendMessage(Messages.get("toggle.notify_hint"));
            return true;
        }

        String toggle = args[0].toLowerCase();
        if (toggle.equals("on")) {
            plugin.getPlayerStateManager().setNotifyDisabled(player.getUniqueId(), false);
            player.sendMessage(Messages.get("toggle.notify_enabled"));
        } else if (toggle.equals("off")) {
            plugin.getPlayerStateManager().setNotifyDisabled(player.getUniqueId(), true);
            player.sendMessage(Messages.get("toggle.notify_disabled_msg"));
        } else {
            player.sendMessage(Messages.get("toggle.notify_usage"));
        }

        return true;
    }

}
