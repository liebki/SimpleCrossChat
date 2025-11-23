package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommand implements CommandExecutor {

    private final SimpleCrossChat plugin;

    public ChatCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.ColorConvert("&cOnly players can use this command."));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sccplus.message.group")) {
            player.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to use group chat."));
            return true;
        }

        if (plugin.getPlayerStateManager().isAdminDisabled(player.getUniqueId())) {
            player.sendMessage(MessageUtils.ColorConvert("&cYour cross-server functionality has been disabled."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(MessageUtils.ColorConvert("&cUsage: /chat <group> <message>"));
            return true;
        }

        String groupName = args[0];
        StringBuilder messageBuilder = new StringBuilder();

        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
                messageBuilder.append(" ");
            }
        }

        String message = messageBuilder.toString();

        plugin.getMqttManager().sendGroupMessage(groupName, message, player.getName());

        player.sendMessage(MessageUtils.ColorConvert("&7[&6" + groupName + "&7] &e" + player.getName() + "&7: &f" + message));

        return true;
    }

}

