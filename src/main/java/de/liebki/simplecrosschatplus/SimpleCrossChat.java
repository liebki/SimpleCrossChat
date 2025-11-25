package de.liebki.simplecrosschatplus;

import de.liebki.simplecrosschatplus.commands.SccCommand;
import de.liebki.simplecrosschatplus.commands.SccmCommand;
import de.liebki.simplecrosschatplus.events.ChatEvent;
import de.liebki.simplecrosschatplus.utils.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.UUID;

public final class SimpleCrossChat extends JavaPlugin {

    public ConfigManager configManager;
    private MQTTClientManager mqttClientManager;
    private PlayerStateManager playerStateManager;
    private AuditLogger auditLogger;
    private AssetTransferManager assetTransferManager;
    private RateLimiter rateLimiter;
    private final String userUuid = UUID.randomUUID().toString();

    @Override
    public void onEnable() {
        configManager = new ConfigManager("plugins/simplecrosschatplus", "options.yml", this);
        boolean configExists = configManager.check("donottouch.configexists");

        if (!configExists) {
            createConfigDefaults();
        }

        boolean isEnabled = configManager.get("general.enabled");
        if (!isEnabled) {
            this.getServer().getLogger().warning("'general.enabled' is set to false, the plugin is not doing anything.");
            return;
        }

        playerStateManager = new PlayerStateManager("plugins/simplecrosschatplus");
        auditLogger = new AuditLogger("plugins/simplecrosschatplus");
        assetTransferManager = new AssetTransferManager("plugins/simplecrosschatplus");
        rateLimiter = new RateLimiter(configManager.get("ratelimit.cooldown-seconds", 30));

        if (VaultIntegration.setupEconomy()) {
            this.getLogger().info("Vault economy integration enabled.");
        } else {
            this.getLogger().warning("Vault not found. Economy features disabled.");
        }

        mqttClientManager = new MQTTClientManager(configManager, userUuid, this);
        this.getServer().getPluginManager().registerEvents(new ChatEvent(mqttClientManager, configManager, this), this);

        this.getCommand("scc").setExecutor(new SccCommand(this));
        this.getCommand("sccpm").setExecutor(new SccmCommand(this));

        ServerListGUI.setPluginInstance(this);

        new Thread(mqttClientManager::connect).start();
        mqttClientManager.startHeartbeatTask();
        new Metrics(this, 24174);

        // After configManager is ready:
        Messages.init(configManager);
    }

    private void createConfigDefaults() {
        configManager.set("donottouch.configexists", true);
        configManager.set("debug.showmessages", false);

        configManager.set("general.info", "- INFO: your servername is publicly viewable and so are the names of the others!");
        String randomServerName = generateRandomServerName();
        configManager.set("general.servername", randomServerName);
        configManager.set("general.broadcastmessageformat", "&a%PLAYER% &0| &f%MESSAGE%");
        configManager.set("general.enabled", true);

        configManager.set("general.servercontact", "");
        configManager.set("general.serverip", "");

        configManager.set("general.info", "- INFO: Please be advised to take a look at the brokers technical informations");
        configManager.set("general.privacyinfo", "- INFO: Even tho the messages are not readable for outstanders, I would host my own broker for a safe communication!");
        configManager.set("technical.broker.address", "broker.emqx.io");
        configManager.set("technical.broker.protocol", "tcp");
        configManager.set("technical.broker.port", 1883);

        configManager.set("general.info", "- INFO: id and key have to match on other servers, to enable a global chat, one value different and it wont work");
        configManager.set("communication.channel.id", "simplecrosschatwelcome");
        configManager.set("communication.channel.key", "emoclewtahcssorcelpmis");

        configManager.set("economy.entity.cost-owned", 50.0);
        configManager.set("economy.entity.cost-animals", 100.0);
        configManager.set("economy.entity.cost-everything", 500.0);
        configManager.set("economy.item.cost", 25.0);

        configManager.set("ratelimit.cooldown-seconds", 30);

        configManager.set("locate.enabled", true);
        configManager.set("locate.allow-remote-resolution", true);
        configManager.set("locate.notify-located-player", true);

        configManager.set("crossserverpm.enabled", true);

        configManager.set("transfer.items.enabled", true);
        configManager.set("transfer.entities.enabled", true);
        configManager.setWithComment("transfer.entities.tier", "owned",
                "Entity transfer tier - Controls what players can transfer:",
                "  owned - Only transfer entities owned by the player (e.g., tamed animals)",
                "  animals - Transfer all animal entities (not owned)",
                "  everything - Transfer any entity type (players, mobs, armor stands, etc.)");

        // Message defaults
        configManager.setWithComment("messages.global.only_players", "&cOnly players can use this command.", "Shown when a non-player tries to use a player-only command.");
        configManager.setWithComment("messages.global.cross_server_disabled", "&cYour cross-server functionality has been disabled.", "Shown when a player's cross-server features are disabled by an admin.");

        configManager.setWithComment("messages.transfer.no_permission", "&cYou don't have permission to transfer money.", "Shown when player lacks permission for /scc transfer.");
        configManager.setWithComment("messages.transfer.vault_unavailable", "&cVault is not available. Economy features are disabled.", "Shown when Vault is missing.");
        configManager.setWithComment("messages.transfer.usage", "&cUsage: /scc transfer <amount> <player>", "Usage for the transfer command.");
        configManager.setWithComment("messages.transfer.cooldown", "&cPlease wait %seconds% seconds before transferring again.", "Cooldown message for money transfers. %seconds% = remaining time.");
        configManager.setWithComment("messages.transfer.invalid_amount", "&cInvalid amount.", "Shown when the entered amount is not a number.");
        configManager.setWithComment("messages.transfer.amount_must_be_positive", "&cAmount must be positive.", "Shown when the entered amount is zero or negative.");
        configManager.setWithComment("messages.transfer.insufficient_funds", "&cYou don't have enough money. You need %needed%.", "Shown when the player has insufficient funds. %needed% = required amount.");
        configManager.setWithComment("messages.transfer.withdraw_failed", "&cFailed to withdraw funds.", "Shown when withdrawal from the player's balance fails.");
        configManager.setWithComment("messages.transfer.success", "&aTransferred %amount% to %target%.", "Shown when a transfer succeeds. %amount% = amount transferred, %target% = target player.");

        configManager.setWithComment("messages.get.usage", "&cUsage: /scc get <UID>", "Usage for the get command.");
        configManager.setWithComment("messages.get.no_pending", "&cNo pending transfer found with UID: %uid%.", "Shown when no pending transfer exists for the given UID. %uid% = entered UID.");
        configManager.setWithComment("messages.get.entity_deserialize_failed", "&cFailed to deserialize entity data.", "Shown when entity transfer data cannot be deserialized.");
        configManager.setWithComment("messages.get.entity_redeemed", "&aEntity redeemed successfully!", "Shown when an entity transfer is successfully redeemed.");
        configManager.setWithComment("messages.get.item_deserialize_failed", "&cFailed to deserialize item data.", "Shown when item transfer data cannot be deserialized.");
        configManager.setWithComment("messages.get.item_redeemed_inventory_full", "&aItem redeemed! (dropped at your feet - inventory full)", "Shown when redeemed item is dropped because inventory is full.");
        configManager.setWithComment("messages.get.item_redeemed_inventory_ok", "&aItem redeemed successfully!", "Shown when redeemed item is added to inventory.");
        configManager.setWithComment("messages.get.entity_spawn_failed_drop_egg", "&eEntity spawn failed. Spawn egg dropped at your feet.", "Shown when entity spawn fails and a spawn egg is dropped.");
        configManager.setWithComment("messages.get.entity_spawn_failed_give_egg", "&eEntity spawn failed. Given spawn egg instead.", "Shown when entity spawn fails and a spawn egg is given.");
        configManager.setWithComment("messages.get.entity_fallback_failed", "&cFailed to redeem entity. Contact an admin.", "Shown when entity redemption completely fails.");

        configManager.setWithComment("messages.com.no_permission", "&cYou don't have permission to use this command.", "Shown when sender lacks permission for /scc com.");
        configManager.setWithComment("messages.com.only_players", "&cOnly players can use this command.", "Shown when console tries to use /scc com.");
        configManager.setWithComment("messages.com.requesting_server_list", "&7Requesting server list from broker...", "Shown when requesting server list from MQTT broker.");

        configManager.setWithComment("messages.scc.unknown_subcommand", "&cUnknown subcommand. Use /scc for help.", "Shown when an unknown /scc subcommand is used.");

        // Disable command messages
        configManager.setWithComment("messages.disable.no_permission", "&cYou don't have permission to use this command.", "Shown when sender lacks permission for /scc disable.");
        configManager.setWithComment("messages.disable.usage", "&cUsage: /scc disable <player>", "Usage for the disable command.");
        configManager.setWithComment("messages.disable.player_not_found", "&cPlayer not found or not online.", "Shown when target player cannot be found.");
        configManager.setWithComment("messages.disable.disabled_for_player", "&aDisabled cross-server functionality for %player%", "Shown when an admin disables cross-server for a player. %player% = target name.");
        configManager.setWithComment("messages.disable.enabled_for_player", "&aEnabled cross-server functionality for %player%", "Shown when an admin enables cross-server for a player. %player% = target name.");
        configManager.setWithComment("messages.disable.notify_disabled", "&cYour cross-server functionality has been disabled by an admin.", "Notification to the player when an admin disables cross-server.");
        configManager.setWithComment("messages.disable.notify_enabled", "&aYour cross-server functionality has been enabled.", "Notification to the player when an admin enables cross-server.");

        // Item transfer command messages
        configManager.setWithComment("messages.itp.disabled", "&cItem transfers are disabled on this server.", "Shown when item transfers are disabled in config.");
        configManager.setWithComment("messages.itp.only_players", "&cOnly players can use this command.", "Shown when console tries to use /scc itp.");
        configManager.setWithComment("messages.itp.no_permission", "&cYou don't have permission to transfer items.", "Shown when player lacks permission for item transfers.");
        configManager.setWithComment("messages.itp.cross_server_disabled", "&cYour cross-server functionality has been disabled.", "Shown when player's cross-server features are disabled.");
        configManager.setWithComment("messages.itp.usage", "&cUsage: /scc itp <server>", "Usage for the itp command.");
        configManager.setWithComment("messages.itp.cooldown", "&cPlease wait %seconds% seconds before transferring again.", "Cooldown message for item transfers. %seconds% = remaining time.");
        configManager.setWithComment("messages.itp.must_hold_item", "&cYou must hold an item to transfer.", "Shown when player is not holding an item.");
        configManager.setWithComment("messages.itp.insufficient_funds", "&cYou need %cost% to transfer items.", "Shown when player lacks funds for item transfer. %cost% = required amount.");
        configManager.setWithComment("messages.itp.withdraw_failed", "&cFailed to withdraw funds.", "Shown when withdrawing item transfer cost fails.");
        configManager.setWithComment("messages.itp.serialize_failed", "&cFailed to serialize item.", "Shown when item serialization fails.");
        configManager.setWithComment("messages.itp.transfer_success", "&aItem transferred! UID: &e%uid%", "Shown when an item transfer is queued. %uid% = transfer UID.");
        configManager.setWithComment("messages.itp.transfer_hint", "&7Use /scc get %uid% on the target server to redeem.", "Hint message after an item transfer. %uid% = transfer UID.");

        // Entity transfer command messages
        configManager.setWithComment("messages.etp.disabled", "&cEntity transfers are disabled on this server.", "Shown when entity transfers are disabled in config.");
        configManager.setWithComment("messages.etp.only_players", "&cOnly players can use this command.", "Shown when console tries to use /scc etp.");
        configManager.setWithComment("messages.etp.no_permission", "&cYou don't have permission to transfer entities.", "Shown when player lacks permission for entity transfers.");
        configManager.setWithComment("messages.etp.cross_server_disabled", "&cYour cross-server functionality has been disabled.", "Shown when player's cross-server features are disabled.");
        configManager.setWithComment("messages.etp.usage", "&cUsage: /scc etp <server>", "Usage for the etp command.");
        configManager.setWithComment("messages.etp.cooldown", "&cPlease wait %seconds% seconds before transferring again.", "Cooldown message for entity transfers. %seconds% = remaining time.");
        configManager.setWithComment("messages.etp.no_entity", "&cNo entity found. Look at an entity.", "Shown when no entity is targeted.");
        configManager.setWithComment("messages.etp.no_permission_entity_type", "&cYou don't have permission to transfer this entity type.", "Shown when player lacks tier permission for this entity.");
        configManager.setWithComment("messages.etp.insufficient_funds", "&cYou need %cost% to transfer this entity.", "Shown when player lacks funds for entity transfer. %cost% = required amount.");
        configManager.setWithComment("messages.etp.withdraw_failed", "&cFailed to withdraw funds.", "Shown when withdrawing entity transfer cost fails.");
        configManager.setWithComment("messages.etp.serialize_failed", "&cFailed to serialize entity.", "Shown when entity serialization fails.");
        configManager.setWithComment("messages.etp.transfer_success", "&aEntity transferred! UID: &e%uid%", "Shown when an entity transfer is queued. %uid% = transfer UID.");
        configManager.setWithComment("messages.etp.transfer_hint", "&7Use /scc get %uid% on the target server to redeem.", "Hint message after an entity transfer. %uid% = transfer UID.");

        // Info command messages
        configManager.setWithComment("messages.info.no_permission", "&cYou don't have permission to use this command.", "Shown when sender lacks permission for /scc info.");
        configManager.setWithComment("messages.info.usage", "&cUsage: /scc info <server>", "Usage for the info command.");
        configManager.setWithComment("messages.info.requesting_info", "&7Requesting info from server: &e%server%", "Shown when requesting server info. %server% = target server.");

        // Locate command messages
        configManager.setWithComment("messages.locate.no_permission", "&cYou don't have permission to use this command.", "Shown when sender lacks permission for /scc locate.");
        configManager.setWithComment("messages.locate.disabled", "&cThe locate command is disabled on this server.", "Shown when locate is disabled in config.");
        configManager.setWithComment("messages.locate.usage", "&cUsage: /scc locate <playername>", "Usage for the locate command.");
        configManager.setWithComment("messages.locate.player_header", "&a&l=== Player Location ===", "Header for player location results.");
        configManager.setWithComment("messages.locate.player_location", "&e%player% &7is on &e%server%", "Shows player location. %player% = player name, %server% = server name.");
        configManager.setWithComment("messages.locate.player_contact", "&7Location: &e%contact%", "Shows contact/location info for server. %contact% = contact info.");
        configManager.setWithComment("messages.locate.privacy_notice", "&7[Privacy Notice] Your location was queried by &e%requester%", "Privacy notice to located player. %requester% = name of requester.");
        configManager.setWithComment("messages.locate.searching", "&7Searching for player: &e%player%&7...", "Shown while searching for player across servers.");
        configManager.setWithComment("messages.locate.player_not_found_global", "&c%player% could not be found on any connected server.", "Shown when player was not found on any server.");
        configManager.setWithComment("messages.locate.disabled_remote", "&cThe server the player is on has disabled this functionality.", "Shown when remote server has disabled locate.");

        // Cross-server PM command messages
        configManager.setWithComment("messages.pm.disabled", "&cThe /sccpm command is disabled on this server.", "Shown when cross-server PM is disabled.");
        configManager.setWithComment("messages.pm.no_permission", "&cYou don't have permission to use this command.", "Shown when sender lacks permission for /sccpm.");
        configManager.setWithComment("messages.pm.only_players", "&cOnly players can use this command.", "Shown when console tries to use /sccpm.");
        configManager.setWithComment("messages.pm.usage", "&cUsage: /sccpm <player> <message>", "Usage for the /sccpm command.");
        configManager.setWithComment("messages.pm.received_local", "&7[&dPM&7] &efrom %sender%&7: &f%message%", "Local PM receive format. %sender% = sender name, %message% = message.");
        configManager.setWithComment("messages.pm.sent", "&a[PM] &7Message sent to &e%target%", "Shown to sender when PM is sent. %target% = target player.");
        configManager.setWithComment("messages.pm.received_cross", "&7[&dPM&7] &efrom %sender%&7: &f%message%", "Cross-server PM receive format. %sender% = sender name, %message% = message.");

        // Toggle command messages
        configManager.setWithComment("messages.toggle.only_players", "&cOnly players can use this command.", "Shown when console tries to use /scc disabled or /scc notify.");
        configManager.setWithComment("messages.toggle.no_permission_disabled", "&cYou don't have permission to use this command.", "Shown when player lacks permission to toggle disabled.");
        configManager.setWithComment("messages.toggle.chat_disabled", "&aYour cross-server chat is now disabled.", "Shown when player disables cross-server chat.");
        configManager.setWithComment("messages.toggle.chat_enabled", "&aYour cross-server chat is now enabled.", "Shown when player enables cross-server chat.");
        configManager.setWithComment("messages.toggle.no_permission_notify", "&cYou don't have permission to use this command.", "Shown when player lacks permission to toggle notifications.");
        configManager.setWithComment("messages.toggle.notify_status", "&7Notifications are currently: %status%", "Shown when querying notification status. %status% = &cOFF or &aON.");
        configManager.setWithComment("messages.toggle.notify_hint", "&7Use /scc notify <on|off> to change.", "Hint for notification toggle usage.");
        configManager.setWithComment("messages.toggle.notify_enabled", "&aNotifications enabled.", "Shown when notifications are enabled.");
        configManager.setWithComment("messages.toggle.notify_disabled_msg", "&cNotifications disabled.", "Shown when notifications are disabled.");
        configManager.setWithComment("messages.toggle.notify_usage", "&cUsage: /scc notify <on|off>", "Usage for the notify toggle command.");

        // MQTT / money transfer and info messages
        configManager.setWithComment("messages.money.received", "&aYou received %amount% from %sender% (from %server%)", "Shown when a player receives money. %amount% = amount, %sender% = sender, %server% = source server.");
        configManager.setWithComment("messages.serverinfo.header", "&a=== Server Info: %server% ===", "Header for server info response. %server% = server name.");
        configManager.setWithComment("messages.serverinfo.players", "&7Players: &e%players%", "Base players line. Additional /%max% is appended if available.");
        configManager.setWithComment("messages.serverinfo.motd", "&7MOTD: &e%motd%", "MOTD line in server info.");
        configManager.setWithComment("messages.serverinfo.version", "&7Version: &e%version%", "Version line in server info.");
        configManager.setWithComment("messages.serverinfo.contact", "&7Contact: &e%contact%", "Contact line in server info.");
        configManager.setWithComment("messages.serverinfo.no_response", "&cNo response from server %server%", "Shown when no server info response is received. %server% = target server.");

        // Server list GUI messages
        configManager.setWithComment("messages.serverlist.no_servers", "&cNo servers are currently available.", "Shown when no servers are available in the server list GUI.");
        configManager.setWithComment("messages.serverlist.title", "&eConnected Servers", "Title for the connected servers GUI.");
        configManager.setWithComment("messages.serverlist.players", "&7Players: &f%players%", "Players line in server list. %players% = current/maximum players.");
        configManager.setWithComment("messages.serverlist.contact", "&7Contact: &e%contact%", "Contact line in server list.");
        configManager.setWithComment("messages.serverlist.offline", "&cNo players online", "Shown when a server has no players online.");
        configManager.setWithComment("messages.serverlist.offline_contact", "&7Contact: &e%contact%", "Contact line for offline server; contact may be N/A.");
        configManager.setWithComment("messages.serverlist.close_title", "&c&lClose", "Title of the close button in the GUI.");
        configManager.setWithComment("messages.serverlist.close_lore", "&7Click to close this GUI", "Lore for the close button in the GUI.");

        configManager.saveConfig();
    }

    @Override
    public void onDisable() {
        mqttClientManager.disconnect();

        if (playerStateManager != null) {
            playerStateManager.savePlayerData();
        }
    }

    private String generateRandomServerName() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 3; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return "server-" + sb.toString();
    }

    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    public AuditLogger getAuditLogger() {
        return auditLogger;
    }

    public AssetTransferManager getAssetTransferManager() {
        return assetTransferManager;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public MQTTClientManager getMqttManager() {
        return mqttClientManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

}

