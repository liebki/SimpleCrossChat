package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.AuditLogger;
import de.liebki.simplecrosschatplus.utils.EntitySerializer;
import de.liebki.simplecrosschatplus.utils.ItemSerializer;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class GetCommand {

    private final SimpleCrossChat plugin;

    public GetCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.ColorConvert("&cOnly players can use this command."));
            return true;
        }

        Player player = (Player) sender;

        if (plugin.getPlayerStateManager().isAdminDisabled(player.getUniqueId())) {
            player.sendMessage(MessageUtils.ColorConvert("&cYour cross-server functionality has been disabled."));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtils.ColorConvert("&cUsage: /scc get <UID>"));
            return true;
        }

        String uid = args[0].toUpperCase();

        if (!plugin.getAssetTransferManager().hasPendingTransfer(uid)) {
            player.sendMessage(MessageUtils.ColorConvert("&cNo pending transfer found with UID: " + uid));
            return true;
        }

        Map<String, Object> transfer = plugin.getAssetTransferManager().getPendingTransfer(uid);
        String type = (String) transfer.get("type");

        if (type.equals("ENTITY")) {
            redeemEntity(player, uid, transfer);
        } else if (type.equals("ITEM")) {
            redeemItem(player, uid, transfer);
        }

        return true;
    }

    private void redeemEntity(Player player, String uid, Map<String, Object> transfer) {
        String data = (String) transfer.get("data");
        String dataType = (String) transfer.get("dataType");
        String sourcePlayer = (String) transfer.get("player");
        String sourceServer = (String) transfer.get("source");

        plugin.getLogger().info("[GetCommand] Attempting to deserialize entity. UID: " + uid + ", DataType: " + dataType);

        Map<String, Object> entityData = EntitySerializer.deserializeEntityData(data);
        if (entityData == null || entityData.isEmpty()) {
            plugin.getLogger().warning("[GetCommand] Failed to deserialize entity data - map is null or empty");
            player.sendMessage(MessageUtils.ColorConvert("&cFailed to deserialize entity data."));
            plugin.getAuditLogger().logTransfer(uid, sourcePlayer, sourceServer, plugin.configManager.get("general.servername"),
                    AuditLogger.TransferType.ENTITY, AuditLogger.TransferResult.FAILED, "Deserialization failed");
            return;
        }

        plugin.getLogger().info("[GetCommand] Entity data deserialized. Keys: " + entityData.keySet());

        EntityType entityType = EntitySerializer.getEntityType(entityData);
        if (entityType == null) {
            plugin.getLogger().warning("[GetCommand] Entity type is null. EntityData type value: " + entityData.get("type"));
            giveFallbackSpawnEgg(player, dataType);
            plugin.getAssetTransferManager().removePendingTransfer(uid);
            plugin.getAuditLogger().logTransfer(uid, sourcePlayer, sourceServer, plugin.configManager.get("general.servername"),
                    AuditLogger.TransferType.ENTITY, AuditLogger.TransferResult.FALLBACK_EGG, "Unknown entity type");
            return;
        }

        plugin.getLogger().info("[GetCommand] Spawning entity of type: " + entityType);

        try {
            // Spawn the entity
            org.bukkit.entity.Entity entity = player.getWorld().spawnEntity(player.getLocation(), entityType);

            // Apply the entity data (restores all properties)
            @SuppressWarnings("unchecked")
            Map<String, Object> entityDataMap = (Map<String, Object>) entityData.get("entityData");
            if (entityDataMap != null) {
                plugin.getLogger().info("[GetCommand] Applying " + entityDataMap.size() + " properties to entity");
                EntitySerializer.applyEntityData(entity, entityDataMap);
            } else {
                plugin.getLogger().warning("[GetCommand] EntityDataMap is null - no properties to apply");
            }

            plugin.getAssetTransferManager().removePendingTransfer(uid);
            player.sendMessage(MessageUtils.ColorConvert("&aEntity redeemed successfully!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            plugin.getAuditLogger().logTransfer(uid, sourcePlayer, sourceServer, plugin.configManager.get("general.servername"),
                    AuditLogger.TransferType.ENTITY, AuditLogger.TransferResult.SUCCESS, entityType.name());

        } catch (Exception e) {
            plugin.getLogger().severe("[GetCommand] Exception while spawning/applying entity:");
            e.printStackTrace();
            giveFallbackSpawnEgg(player, dataType);
            plugin.getAssetTransferManager().removePendingTransfer(uid);
            plugin.getAuditLogger().logTransfer(uid, sourcePlayer, sourceServer, plugin.configManager.get("general.servername"),
                    AuditLogger.TransferType.ENTITY, AuditLogger.TransferResult.FALLBACK_EGG, "Spawn failed: " + e.getMessage());
        }
    }

    private void redeemItem(Player player, String uid, Map<String, Object> transfer) {
        String data = (String) transfer.get("data");
        String sourcePlayer = (String) transfer.get("player");
        String sourceServer = (String) transfer.get("source");

        Map<String, Object> itemData = ItemSerializer.deserializeItemData(data);
        if (itemData == null || itemData.isEmpty()) {
            player.sendMessage(MessageUtils.ColorConvert("&cFailed to deserialize item data."));
            plugin.getAuditLogger().logTransfer(uid, sourcePlayer, sourceServer, plugin.configManager.get("general.servername"),
                    AuditLogger.TransferType.ITEM, AuditLogger.TransferResult.FAILED, "Deserialization failed");
            return;
        }

        ItemStack item;
        if (itemData.containsKey("itemstack")) {
            // Get the complete ItemStack with ALL NBT data preserved by BukkitObjectInputStream
            item = (ItemStack) itemData.get("itemstack");
            plugin.getAuditLogger().logTransfer(uid, sourcePlayer, sourceServer, plugin.configManager.get("general.servername"),
                    AuditLogger.TransferType.ITEM, AuditLogger.TransferResult.SUCCESS, item.getType().name());
        } else {
            // Fallback if something went wrong
            Material material = ItemSerializer.getMaterial(itemData);
            int amount = (int) itemData.getOrDefault("amount", 1);
            item = ItemSerializer.createFallbackItem(material, amount);
            plugin.getAuditLogger().logTransfer(uid, sourcePlayer, sourceServer, plugin.configManager.get("general.servername"),
                    AuditLogger.TransferType.ITEM, AuditLogger.TransferResult.FALLBACK_BASE_ITEM, "Invalid material");
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage(MessageUtils.ColorConvert("&aItem redeemed! (dropped at your feet - inventory full)"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        } else {
            player.getInventory().addItem(item);
            player.sendMessage(MessageUtils.ColorConvert("&aItem redeemed successfully!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        }

        plugin.getAssetTransferManager().removePendingTransfer(uid);
    }

    private void giveFallbackSpawnEgg(Player player, String entityTypeName) {
        try {
            EntityType type = EntityType.valueOf(entityTypeName);
            Material spawnEgg = Material.valueOf(type.name() + "_SPAWN_EGG");

            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(spawnEgg, 1));
                player.sendMessage(MessageUtils.ColorConvert("&eEntity spawn failed. Spawn egg dropped at your feet."));
            } else {
                player.getInventory().addItem(new ItemStack(spawnEgg, 1));
                player.sendMessage(MessageUtils.ColorConvert("&eEntity spawn failed. Given spawn egg instead."));
            }
        } catch (Exception e) {
            player.sendMessage(MessageUtils.ColorConvert("&cFailed to redeem entity. Contact an admin."));
        }
    }

}

