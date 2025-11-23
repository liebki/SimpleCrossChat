package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleCommand {

    private final SimpleCrossChat plugin;

    public ToggleCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String subCommand, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.ColorConvert("&cOnly players can use this command."));
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
            player.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to use this command."));
            return true;
        }

        boolean currentStatus = plugin.getPlayerStateManager().isChatDisabled(player.getUniqueId());
        plugin.getPlayerStateManager().setChatDisabled(player.getUniqueId(), !currentStatus);

        if (!currentStatus) {
            player.sendMessage(MessageUtils.ColorConvert("&aYour cross-server chat is now disabled."));
        } else {
            player.sendMessage(MessageUtils.ColorConvert("&aYour cross-server chat is now enabled."));
        }

        return true;
    }

    private boolean handleNotifyToggle(Player player, String[] args) {
        if (!player.hasPermission("sccplus.toggle.notify")) {
            player.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            boolean currentStatus = plugin.getPlayerStateManager().isNotifyDisabled(player.getUniqueId());
            player.sendMessage(MessageUtils.ColorConvert("&7Notifications are currently: " + (currentStatus ? "&cOFF" : "&aON")));
            player.sendMessage(MessageUtils.ColorConvert("&7Use /scc notify <on|off> to change."));
            return true;
        }

        String toggle = args[0].toLowerCase();
        if (toggle.equals("on")) {
            plugin.getPlayerStateManager().setNotifyDisabled(player.getUniqueId(), false);
            player.sendMessage(MessageUtils.ColorConvert("&aNotifications enabled."));
        } else if (toggle.equals("off")) {
            plugin.getPlayerStateManager().setNotifyDisabled(player.getUniqueId(), true);
            player.sendMessage(MessageUtils.ColorConvert("&cNotifications disabled."));
        } else {
            player.sendMessage(MessageUtils.ColorConvert("&cUsage: /scc notify <on|off>"));
        }

        return true;
    }

}

