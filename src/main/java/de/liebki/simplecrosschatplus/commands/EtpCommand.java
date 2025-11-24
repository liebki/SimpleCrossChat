package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.EntitySerializer;
import de.liebki.simplecrosschatplus.utils.Messages;
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
        // Check if entity transfers are enabled
        boolean entityTransfersEnabled = plugin.getConfigManager().getConfig().getBoolean("transfer.entities.enabled", true);
        if (!entityTransfersEnabled) {
            sender.sendMessage(Messages.get("etp.disabled"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("etp.only_players"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sccplus.entity.transfer")) {
            player.sendMessage(Messages.get("etp.no_permission"));
            return true;
        }

        if (plugin.getPlayerStateManager().isAdminDisabled(player.getUniqueId())) {
            player.sendMessage(Messages.get("etp.cross_server_disabled"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Messages.get("etp.usage"));
            return true;
        }

        if (plugin.getRateLimiter().isOnCooldown(player.getUniqueId(), "entity_transfer")) {
            long remaining = plugin.getRateLimiter().getRemainingCooldown(player.getUniqueId(), "entity_transfer");
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("seconds", String.valueOf(remaining));
            player.sendMessage(Messages.get("etp.cooldown", placeholders));
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
            player.sendMessage(Messages.get("etp.no_entity"));
            return true;
        }


        String tier = determineTier(player, entity);
        if (tier == null) {
            player.sendMessage(Messages.get("etp.no_permission_entity_type"));
            return true;
        }

        boolean hasBypassPermission = player.hasPermission("sccplus.entity.transfer.bypass");
        double cost = getCostForTier(tier);

        if (!hasBypassPermission && cost > 0 && !VaultIntegration.hasEnough(player, cost)) {
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("cost", VaultIntegration.format(cost));
            player.sendMessage(Messages.get("etp.insufficient_funds", placeholders));
            return true;
        }

        if (!hasBypassPermission && cost > 0 && !VaultIntegration.withdraw(player, cost)) {
            player.sendMessage(Messages.get("etp.withdraw_failed"));
            return true;
        }

        String serializedEntity = EntitySerializer.serializeEntity(entity);
        if (serializedEntity == null) {
            player.sendMessage(Messages.get("etp.serialize_failed"));
            if (!hasBypassPermission && cost > 0) {
                VaultIntegration.deposit(player, cost);
            }
            return true;
        }

        String uid = plugin.getAssetTransferManager().generateTransferUid();
        plugin.getMqttManager().sendEntityTransfer(uid, serializedEntity, entity.getType().name(),
                player.getName(), targetServer);

        entity.remove();

        plugin.getRateLimiter().setCooldown(player.getUniqueId(), "entity_transfer");

        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("uid", uid);
        player.sendMessage(Messages.get("etp.transfer_success", placeholders));
        player.sendMessage(Messages.get("etp.transfer_hint", placeholders));

        return true;
    }

    private String determineTier(Player player, Entity entity) {
        // Check if player has bypass permission - allows any entity
        if (player.hasPermission("sccplus.entity.transfer.bypass")) {
            return "EVERYTHING";
        }

        // Get the server-wide tier limit from config
        String configTier = plugin.getConfigManager().getConfig().getString("transfer.entities.tier", "owned");

        // Check permissions and config tier together
        // Config tier acts as a server-wide limit, permissions still required

        if (player.hasPermission("sccplus.entity.tier.everything") && configTier.equalsIgnoreCase("everything")) {
            return "EVERYTHING";
        }

        if (player.hasPermission("sccplus.entity.tier.animals") &&
            (configTier.equalsIgnoreCase("animals") || configTier.equalsIgnoreCase("everything")) &&
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
