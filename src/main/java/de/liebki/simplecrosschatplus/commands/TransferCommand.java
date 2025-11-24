package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.Messages;
import de.liebki.simplecrosschatplus.utils.VaultIntegration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TransferCommand {

    private final SimpleCrossChat plugin;

    public TransferCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("global.only_players"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sccplus.money.transfer")) {
            player.sendMessage(Messages.get("transfer.no_permission"));
            return true;
        }

        if (plugin.getPlayerStateManager().isAdminDisabled(player.getUniqueId())) {
            player.sendMessage(Messages.get("global.cross_server_disabled"));
            return true;
        }

        if (!VaultIntegration.isVaultAvailable()) {
            player.sendMessage(Messages.get("transfer.vault_unavailable"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Messages.get("transfer.usage"));
            return true;
        }

        if (plugin.getRateLimiter().isOnCooldown(player.getUniqueId(), "money_transfer")) {
            long remaining = plugin.getRateLimiter().getRemainingCooldown(player.getUniqueId(), "money_transfer");
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("seconds", String.valueOf(remaining));
            player.sendMessage(Messages.get("transfer.cooldown", placeholders));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Messages.get("transfer.invalid_amount"));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(Messages.get("transfer.amount_must_be_positive"));
            return true;
        }

        String targetPlayer = args[1];

        if (!VaultIntegration.hasEnough(player, amount)) {
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("needed", VaultIntegration.format(amount));
            player.sendMessage(Messages.get("transfer.insufficient_funds", placeholders));
            return true;
        }

        if (!VaultIntegration.withdraw(player, amount)) {
            player.sendMessage(Messages.get("transfer.withdraw_failed"));
            return true;
        }

        String uid = plugin.getAssetTransferManager().generateTransferUid();
        plugin.getMqttManager().sendMoneyTransfer(uid, amount, targetPlayer, player.getName());

        plugin.getRateLimiter().setCooldown(player.getUniqueId(), "money_transfer");

        java.util.Map<String, String> successPlaceholders = new java.util.HashMap<>();
        successPlaceholders.put("amount", VaultIntegration.format(amount));
        successPlaceholders.put("target", targetPlayer);
        player.sendMessage(Messages.get("transfer.success", successPlaceholders));

        return true;
    }

}
