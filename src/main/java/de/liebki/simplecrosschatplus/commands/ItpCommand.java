package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.ItemSerializer;
import de.liebki.simplecrosschatplus.utils.Messages;
import de.liebki.simplecrosschatplus.utils.VaultIntegration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItpCommand {

    private final SimpleCrossChat plugin;

    public ItpCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        // Check if item transfers are enabled
        boolean itemTransfersEnabled = plugin.getConfigManager().getConfig().getBoolean("transfer.items.enabled", true);
        if (!itemTransfersEnabled) {
            sender.sendMessage(Messages.get("itp.disabled"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("itp.only_players"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sccplus.item.transfer")) {
            player.sendMessage(Messages.get("itp.no_permission"));
            return true;
        }

        if (plugin.getPlayerStateManager().isAdminDisabled(player.getUniqueId())) {
            player.sendMessage(Messages.get("itp.cross_server_disabled"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Messages.get("itp.usage"));
            return true;
        }

        if (plugin.getRateLimiter().isOnCooldown(player.getUniqueId(), "item_transfer")) {
            long remaining = plugin.getRateLimiter().getRemainingCooldown(player.getUniqueId(), "item_transfer");
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("seconds", String.valueOf(remaining));
            player.sendMessage(Messages.get("itp.cooldown", placeholders));
            return true;
        }

        String targetServer = args[0];

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            player.sendMessage(Messages.get("itp.must_hold_item"));
            return true;
        }

        double cost = plugin.configManager.get("economy.item.cost", 25.0);

        if (cost > 0 && !VaultIntegration.hasEnough(player, cost)) {
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("cost", VaultIntegration.format(cost));
            player.sendMessage(Messages.get("itp.insufficient_funds", placeholders));
            return true;
        }

        if (cost > 0 && !VaultIntegration.withdraw(player, cost)) {
            player.sendMessage(Messages.get("itp.withdraw_failed"));
            return true;
        }

        String serializedItem = ItemSerializer.serializeItem(heldItem);
        if (serializedItem == null) {
            player.sendMessage(Messages.get("itp.serialize_failed"));
            if (cost > 0) {
                VaultIntegration.deposit(player, cost);
            }
            return true;
        }

        String uid = plugin.getAssetTransferManager().generateTransferUid();
        plugin.getMqttManager().sendItemTransfer(uid, serializedItem, heldItem.getType().name(),
                player.getName(), targetServer);

        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        plugin.getRateLimiter().setCooldown(player.getUniqueId(), "item_transfer");

        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("uid", uid);
        player.sendMessage(Messages.get("itp.transfer_success", placeholders));
        player.sendMessage(Messages.get("itp.transfer_hint", placeholders));

        return true;
    }

}
