package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.EntitySerializer;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
import de.liebki.simplecrosschatplus.utils.VaultIntegration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

public class EtpCommand {

    private final SimpleCrossChat plugin;

    public EtpCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.ColorConvert("&cOnly players can use this command."));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sccplus.entity.transfer")) {
            player.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to transfer entities."));
            return true;
        }

        if (plugin.getPlayerStateManager().isAdminDisabled(player.getUniqueId())) {
            player.sendMessage(MessageUtils.ColorConvert("&cYour cross-server functionality has been disabled."));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtils.ColorConvert("&cUsage: /scc etp <server>"));
            return true;
        }

        if (plugin.getRateLimiter().isOnCooldown(player.getUniqueId(), "entity_transfer")) {
            long remaining = plugin.getRateLimiter().getRemainingCooldown(player.getUniqueId(), "entity_transfer");
            player.sendMessage(MessageUtils.ColorConvert("&cPlease wait " + remaining + " seconds before transferring again."));
            return true;
        }

        String targetServer = args[0];

        Entity entity = null;

        // Try to use rayTraceEntities (Paper/newer Spigot)
        try {
            Object rayTrace = player.getClass().getMethod("rayTraceEntities", int.class).invoke(player, 5);
            if (rayTrace != null) {
                entity = (Entity) rayTrace.getClass().getMethod("getHitEntity").invoke(rayTrace);
            }
        } catch (Exception e) {
            // Fall back to getNearbyEntities for older versions
            for (Entity nearby : player.getNearbyEntities(5, 5, 5)) {
                if (player.hasLineOfSight(nearby)) {
                    entity = nearby;
                    break;
                }
            }
        }

        if (entity == null) {
            player.sendMessage(MessageUtils.ColorConvert("&cNo entity found. Look at an entity."));
            return true;
        }


        String tier = determineTier(player, entity);
        if (tier == null) {
            player.sendMessage(MessageUtils.ColorConvert("&cYou don't have permission to transfer this entity type."));
            return true;
        }

        double cost = getCostForTier(tier);
        if (cost > 0 && !VaultIntegration.hasEnough(player, cost)) {
            player.sendMessage(MessageUtils.ColorConvert("&cYou need " + VaultIntegration.format(cost) + " to transfer this entity."));
            return true;
        }

        if (cost > 0 && !VaultIntegration.withdraw(player, cost)) {
            player.sendMessage(MessageUtils.ColorConvert("&cFailed to withdraw funds."));
            return true;
        }

        String serializedEntity = EntitySerializer.serializeEntity(entity);
        if (serializedEntity == null) {
            player.sendMessage(MessageUtils.ColorConvert("&cFailed to serialize entity."));
            if (cost > 0) {
                VaultIntegration.deposit(player, cost);
            }
            return true;
        }

        String uid = plugin.getAssetTransferManager().generateTransferUid();
        plugin.getMqttManager().sendEntityTransfer(uid, serializedEntity, entity.getType().name(),
                player.getName(), targetServer);

        entity.remove();

        plugin.getRateLimiter().setCooldown(player.getUniqueId(), "entity_transfer");

        player.sendMessage(MessageUtils.ColorConvert("&aEntity transferred! UID: &e" + uid));
        player.sendMessage(MessageUtils.ColorConvert("&7Use /scc get " + uid + " on the target server to redeem."));

        return true;
    }

    private String determineTier(Player player, Entity entity) {
        if (player.hasPermission("sccplus.entity.tier.everything")) {
            return "EVERYTHING";
        }

        if (player.hasPermission("sccplus.entity.tier.animals") &&
            EntitySerializer.isTransferableType(entity.getType(), "ALL_ANIMALS")) {
            return "ALL_ANIMALS";
        }

        if (player.hasPermission("sccplus.entity.tier.owned") && entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            if (tameable.isTamed() && tameable.getOwner() != null &&
                tameable.getOwner().getUniqueId().equals(player.getUniqueId())) {
                return "OWNED_ONLY";
            }
        }

        return null;
    }

    private double getCostForTier(String tier) {
        switch (tier) {
            case "OWNED_ONLY":
                return plugin.configManager.get("economy.entity.cost-owned", 50.0);
            case "ALL_ANIMALS":
                return plugin.configManager.get("economy.entity.cost-animals", 100.0);
            case "EVERYTHING":
                return plugin.configManager.get("economy.entity.cost-everything", 500.0);
            default:
                return 0.0;
        }
    }

}

