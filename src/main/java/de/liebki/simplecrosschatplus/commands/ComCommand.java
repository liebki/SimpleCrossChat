package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComCommand {

    private final SimpleCrossChat plugin;

    public ComCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sccplus.admin.com")) {
            sender.sendMessage(Messages.get("com.no_permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("global.only_players"));
            return true;
        }

        sender.sendMessage(Messages.get("com.requesting_server_list"));

        plugin.getMqttManager().requestServerList((Player) sender);

        return true;
    }

}
