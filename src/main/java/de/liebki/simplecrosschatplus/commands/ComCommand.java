package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
import org.bukkit.command.CommandSender;

public class ComCommand {

    private final SimpleCrossChat plugin;

    public ComCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sccplus.admin.com")) {
            sender.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to use this command."));
            return true;
        }

        sender.sendMessage(MessageUtils.ColorConvert("&a&l=== Connected Servers ==="));
        sender.sendMessage(MessageUtils.ColorConvert("&7Requesting server list from broker..."));

        plugin.getMqttManager().requestServerList(sender);

        return true;
    }

}

