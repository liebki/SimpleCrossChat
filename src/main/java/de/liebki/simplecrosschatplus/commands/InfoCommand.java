package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
import org.bukkit.command.CommandSender;

public class InfoCommand {

    private final SimpleCrossChat plugin;

    public InfoCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sccplus.admin.info")) {
            sender.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtils.ColorConvert("&cUsage: /scc info <server>"));
            return true;
        }

        String targetServer = args[0];

        sender.sendMessage(MessageUtils.ColorConvert("&7Requesting info from server: &e" + targetServer));
        plugin.getMqttManager().requestServerInfo(targetServer, sender);

        return true;
    }


}

