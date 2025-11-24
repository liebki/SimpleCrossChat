package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.ItemSerializer;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
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
            sender.sendMessage(MessageUtils.ColorConvert("&cItem transfers are disabled on this server."));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.ColorConvert("&cOnly players can use this command."));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sccplus.item.transfer")) {
            player.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to transfer items."));
            return true;
        }

        if (plugin.getPlayerStateManager().isAdminDisabled(player.getUniqueId())) {
            player.sendMessage(MessageUtils.ColorConvert("&cYour cross-server functionality has been disabled."));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtils.ColorConvert("&cUsage: /scc itp <server>"));
            return true;
        }

        if (plugin.getRateLimiter().isOnCooldown(player.getUniqueId(), "item_transfer")) {
            long remaining = plugin.getRateLimiter().getRemainingCooldown(player.getUniqueId(), "item_transfer");
            player.sendMessage(MessageUtils.ColorConvert("&cPlease wait " + remaining + " seconds before transferring again."));
            return true;
        }

        String targetServer = args[0];

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            player.sendMessage(MessageUtils.ColorConvert("&cYou must hold an item to transfer."));
            return true;
        }

        double cost = plugin.configManager.get("economy.item.cost", 25.0);

        if (cost > 0 && !VaultIntegration.hasEnough(player, cost)) {
            player.sendMessage(MessageUtils.ColorConvert("&cYou need " + VaultIntegration.format(cost) + " to transfer items."));
            return true;
        }

        if (cost > 0 && !VaultIntegration.withdraw(player, cost)) {
            player.sendMessage(MessageUtils.ColorConvert("&cFailed to withdraw funds."));
            return true;
        }

        String serializedItem = ItemSerializer.serializeItem(heldItem);
        if (serializedItem == null) {
            player.sendMessage(MessageUtils.ColorConvert("&cFailed to serialize item."));
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

        player.sendMessage(MessageUtils.ColorConvert("&aItem transferred! UID: &e" + uid));
        player.sendMessage(MessageUtils.ColorConvert("&7Use /scc get " + uid + " on the target server to redeem."));

        return true;
    }

}
