package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.Messages;
import org.bukkit.command.CommandSender;

public class InfoCommand {

    private final SimpleCrossChat plugin;

    public InfoCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sccplus.admin.info")) {
            sender.sendMessage(Messages.get("info.no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Messages.get("info.usage"));
            return true;
        }

        String targetServer = args[0];

        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("server", targetServer);
        sender.sendMessage(Messages.get("info.requesting_info", placeholders));
        plugin.getMqttManager().requestServerInfo(targetServer, sender);

        return true;
    }


}
