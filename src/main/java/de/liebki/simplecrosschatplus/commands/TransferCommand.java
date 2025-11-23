package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
import de.liebki.simplecrosschatplus.utils.VaultIntegration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TransferCommand {

    private final SimpleCrossChat plugin;

    public TransferCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.ColorConvert("&cOnly players can use this command."));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sccplus.money.transfer")) {
            player.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to transfer money."));
            return true;
        }

        if (plugin.getPlayerStateManager().isAdminDisabled(player.getUniqueId())) {
            player.sendMessage(MessageUtils.ColorConvert("&cYour cross-server functionality has been disabled."));
            return true;
        }

        if (!VaultIntegration.isVaultAvailable()) {
            player.sendMessage(MessageUtils.ColorConvert("&cVault is not available. Economy features are disabled."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(MessageUtils.ColorConvert("&cUsage: /scc transfer <amount> <player>"));
            return true;
        }

        if (plugin.getRateLimiter().isOnCooldown(player.getUniqueId(), "money_transfer")) {
            long remaining = plugin.getRateLimiter().getRemainingCooldown(player.getUniqueId(), "money_transfer");
            player.sendMessage(MessageUtils.ColorConvert("&cPlease wait " + remaining + " seconds before transferring again."));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtils.ColorConvert("&cInvalid amount."));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(MessageUtils.ColorConvert("&cAmount must be positive."));
            return true;
        }

        String targetPlayer = args[1];

        if (!VaultIntegration.hasEnough(player, amount)) {
            player.sendMessage(MessageUtils.ColorConvert("&cYou don't have enough money. You need " + VaultIntegration.format(amount)));
            return true;
        }

        if (!VaultIntegration.withdraw(player, amount)) {
            player.sendMessage(MessageUtils.ColorConvert("&cFailed to withdraw funds."));
            return true;
        }

        String uid = plugin.getAssetTransferManager().generateTransferUid();
        plugin.getMqttManager().sendMoneyTransfer(uid, amount, targetPlayer, player.getName());

        plugin.getRateLimiter().setCooldown(player.getUniqueId(), "money_transfer");

        player.sendMessage(MessageUtils.ColorConvert("&aTransferred " + VaultIntegration.format(amount) + " to " + targetPlayer));

        return true;
    }

}

