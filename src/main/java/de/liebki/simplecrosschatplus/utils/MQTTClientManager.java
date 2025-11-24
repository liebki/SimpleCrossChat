package de.liebki.simplecrosschatplus.utils;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.models.JsonPayload;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MQTTClientManager {

    private static final String Prefix = "[SimpleCrossChat] ";
    private int errorSendCounter = 0;
    private final ConfigManager configManager;
    private MqttAsyncClient client;
    private final String userUuid;
    private final MqttProperties properties;
    private final SimpleCrossChat pluginInstance;

    private final Map<String, org.bukkit.command.CommandSender> pendingInfoRequests;
    private final Map<String, org.bukkit.command.CommandSender> pendingLocationRequests;
    private final ServerRegistry serverRegistry;

    // Connection state management
    private volatile boolean isConnected = false;
    private volatile long lastConnectionAttempt = 0;
    private volatile int reconnectAttempts = 0;
    private static final long RECONNECT_BACKOFF_BASE_MS = 5000; // 5 seconds
    private static final long RECONNECT_MAX_BACKOFF_MS = 300000; // 5 minutes
    private static final int MAX_RECONNECT_ATTEMPTS = 10;

    public MQTTClientManager(ConfigManager config, String userUuid, SimpleCrossChat pluginInstance) {
        this.configManager = config;
        this.userUuid = userUuid;
        this.properties = new MqttProperties();
        this.pluginInstance = pluginInstance;
        this.pendingInfoRequests = new HashMap<>();
        this.pendingLocationRequests = new HashMap<>();
        this.serverRegistry = new ServerRegistry(60); // 60 second timeout

        this.properties.setSubscriptionIdentifiers(Arrays.asList(new Integer[]{0}));
    }

    public void connect() {
        try {
            String broker = getCompleteBrokerAddress();

            if (client == null) {
                client = new MqttAsyncClient(broker, userUuid);
            }

            if (client.isConnected()) {
                pluginInstance.getLogger().info(Prefix + "Already connected to broker: " + broker);
                isConnected = true;
                reconnectAttempts = 0;
                return;
            }

            MqttConnectionOptions connOpts = new MqttConnectionOptions();
            connOpts.setCleanStart(true);

            pluginInstance.getLogger().info(Prefix + "Connecting to your configured broker: " + broker);
            lastConnectionAttempt = System.currentTimeMillis();

            IMqttToken token = client.connect(connOpts);
            token.waitForCompletion();

            isConnected = true;
            reconnectAttempts = 0;
            errorSendCounter = 0;

            pluginInstance.getLogger().info(Prefix + "Connected to your configured broker: " + broker);

            String convTopic = configManager.get("communication.channel.id");
            subscribeToTopic(convTopic);

        } catch (MqttException e) {
            isConnected = false;
            reconnectAttempts++;

            pluginInstance.getLogger().warning(Prefix + "Connection attempt " + reconnectAttempts + " failed: " + e.getMessage());

            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                pluginInstance.getLogger().severe(Prefix + "Max reconnection attempts (" + MAX_RECONNECT_ATTEMPTS + ") reached. Disabling SimpleCrossChat!");
                pluginInstance.getServer().getPluginManager().disablePlugin(pluginInstance);
            }
        }
    }

    private String getCompleteBrokerAddress() {
        String brokerAddr = configManager.get("technical.broker.address");
        String brokerProt = configManager.get("technical.broker.protocol");
        int brokerPort = configManager.get("technical.broker.port");

        String brokerFormatString = "%s://%s:%d";
        return String.format(brokerFormatString, brokerProt, brokerAddr, brokerPort);
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                pluginInstance.getLogger().info(Prefix + "Disconnecting from your configured broker " + getCompleteBrokerAddress());
                IMqttToken token = client.disconnect();
                token.waitForCompletion();
                pluginInstance.getLogger().info(Prefix + "Disconnected from your configured broker " + getCompleteBrokerAddress());
            }
            isConnected = false;
        } catch (MqttException e) {
            pluginInstance.getLogger().severe(Prefix + "Error while disconnecting from broker: " + e.getMessage());
            isConnected = false;
        }
    }

    public void sendMessage(String messageContent, String username, String servername) {

        if (client == null || !client.isConnected()) {
            pluginInstance.getLogger().warning(Prefix + "Connection lost, attempting to reconnect...");
            connect();
        }

        try {
            String encryptedUserMessage = ConversationUtils.encrypt(messageContent, configManager.get("communication.channel.key"));
            String encryptedUsername = ConversationUtils.encrypt(username, configManager.get("communication.channel.key"));

            String encryptedServername = ConversationUtils.encrypt(servername, configManager.get("communication.channel.key"));

            if (isContentReadyToSend(encryptedUserMessage, encryptedUsername, encryptedServername)) {
                String jsonMessagePayload = JsonPayloadHandler.createJsonPayload(this.userUuid, encryptedUserMessage, encryptedUsername, encryptedServername);
                MqttMessage message = new MqttMessage(jsonMessagePayload.getBytes());

                message.setQos(1);
                String convTopic = configManager.get("communication.channel.id");

                IMqttToken token = client.publish(convTopic, message);
                token.waitForCompletion();

                if (configManager.get("debug.showmessages")) {
                    pluginInstance.getLogger().info(Prefix + "The player (user on your server) " + username + " sent a message");
                }
            }
        } catch (MqttException e) {
            pluginInstance.getLogger().severe(Prefix + "Error while sending message: " + e.getMessage());

            errorSendCounter++;
            if (errorSendCounter >= 3) {
                pluginInstance.getLogger().severe(Prefix + "Messages from users could not be published for at least three times, the plugin will disable itself.");
                pluginInstance.getServer().getPluginManager().disablePlugin(pluginInstance);
            }

        }
    }

    private static boolean isContentReadyToSend(String encryptedUserMessage, String encryptedUsername, String encryptedServername) {
        return encryptedUserMessage != null && !encryptedUserMessage.isEmpty() && encryptedUsername != null && !encryptedUsername.isEmpty() && encryptedServername != null && !encryptedServername.isEmpty();
    }

    private void subscribeToTopic(String topic) {
        if (topic != null && !topic.isEmpty()) {
            try {
                MqttSubscription subscription = new MqttSubscription(topic, 1);
                IMqttToken token = client.subscribe(new MqttSubscription[]{subscription}, null, null, (receivedTopic, message) -> {
                    processReceivedMessage(message);
                }, properties);

                token.waitForCompletion(5000);
                if (token.getException() == null) {
                    pluginInstance.getLogger().info(Prefix + "Successfully subscribed to your configured topic: " + topic);
                } else {
                    pluginInstance.getLogger().severe(Prefix + "Subscription failed to your configured topic because: " + token.getException().getMessage());
                }
            } catch (MqttException e) {
                pluginInstance.getLogger().severe(Prefix + "Error while subscribing to your configured topic because: " + e.getMessage());
            }
        }
    }

    private void processReceivedMessage(MqttMessage message) {
        JsonPayload receivedMessage = JsonPayloadHandler.readJsonPayload(new String(message.getPayload()));

        if (receivedMessage != null) {
            if (receivedMessage.getSenderUuid().equals(this.userUuid)) {
                if (configManager.get("debug.showmessages")) {
                    pluginInstance.getLogger().warning(Prefix + "The sending server and receiving is the same, no action required.");
                }
                return;
            }

            switch (receivedMessage.getPayloadType()) {
                case CHAT:
                    handleChatPayload(receivedMessage);
                    break;
                case ENTITY_TRANSFER:
                    handleEntityTransferPayload(message.getPayload());
                    break;
                case ITEM_TRANSFER:
                    handleItemTransferPayload(message.getPayload());
                    break;
                case MONEY_TRANSFER:
                    handleMoneyTransferPayload(message.getPayload());
                    break;
                case PRIVATE_MESSAGE:
                    handlePrivateMessagePayload(message.getPayload());
                    break;
                case SERVER_INFO_REQUEST:
                    handleServerInfoRequest(message.getPayload());
                    break;
                case SERVER_INFO_RESPONSE:
                    handleServerInfoResponse(message.getPayload());
                    break;
                case SERVER_HEARTBEAT:
                    handleServerHeartbeat(message.getPayload());
                    break;
                case PLAYER_LOCATION_REQUEST:
                    handlePlayerLocationRequest(message.getPayload());
                    break;
                case PLAYER_LOCATION_RESPONSE:
                    handlePlayerLocationResponse(message.getPayload());
                    break;
                default:
                    if (configManager.get("debug.showmessages")) {
                        pluginInstance.getLogger().warning(Prefix + "Unknown payload type: " + receivedMessage.getPayloadType());
                    }
            }
        }
    }

    private void handleChatPayload(JsonPayload receivedMessage) {
        if (configManager.get("debug.showmessages")) {
            pluginInstance.getLogger().info(Prefix + "A Message was received, trying to decrypt it's content");
        }

        String convKey = configManager.get("communication.channel.key");
        String receivedEncryptedMessage = receivedMessage.getEncryptedMessage();
        String decryptedMessage = ConversationUtils.decrypt(receivedEncryptedMessage, convKey);

        String receivedEncryptedPlayername = receivedMessage.getEncryptedPlayerDisplayname();
        String decryptedPlayername = ConversationUtils.decrypt(receivedEncryptedPlayername, convKey);

        String receivedEncryptedServerName = receivedMessage.getEncryptedServerName();
        String decryptedServerName = ConversationUtils.decrypt(receivedEncryptedServerName, convKey);

        if (receivedEncryptedPlayername != null && !receivedEncryptedPlayername.isEmpty()) {
            broadcastDecryptedMessage(decryptedMessage, decryptedPlayername, decryptedServerName);
        } else {
            pluginInstance.getLogger().warning(Prefix + "The keys are not the same, message is not decryptable!");
        }
    }

    private void handleEntityTransferPayload(byte[] payload) {
        org.json.JSONObject json = JsonPayloadHandler.parseJson(new String(payload));
        if (json == null) return;

        String targetServer = json.optString("targetserver", "");
        String currentServer = configManager.get("general.servername");

        if (!targetServer.equalsIgnoreCase(currentServer)) {
            return;
        }

        String convKey = configManager.get("communication.channel.key");

        String transferUid = json.optString("transferuid", "");
        String encryptedEntityData = json.optString("entitydata", "");
        String encryptedEntityType = json.optString("entitytype", "");
        String playerName = json.optString("playername", "");
        String sourceServer = json.optString("servername", "");

        // Decrypt the data before storing
        String entityData = ConversationUtils.decrypt(encryptedEntityData, convKey);
        String entityType = ConversationUtils.decrypt(encryptedEntityType, convKey);

        pluginInstance.getAssetTransferManager().storePendingTransfer(
            transferUid,
            AssetTransferManager.TransferType.ENTITY,
            entityData,
            entityType,
            playerName,
            sourceServer,
            targetServer
        );

        // Play sound for receiving entity
        org.bukkit.entity.Player targetPlayer = pluginInstance.getServer().getPlayer(playerName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.playSound(targetPlayer.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
        }

        pluginInstance.getLogger().info(Prefix + "Entity transfer received. UID: " + transferUid);
    }

    private void handleItemTransferPayload(byte[] payload) {
        org.json.JSONObject json = JsonPayloadHandler.parseJson(new String(payload));
        if (json == null) return;

        String targetServer = json.optString("targetserver", "");
        String currentServer = configManager.get("general.servername");

        if (!targetServer.equalsIgnoreCase(currentServer)) {
            return;
        }

        String convKey = configManager.get("communication.channel.key");

        String transferUid = json.optString("transferuid", "");
        String encryptedItemData = json.optString("itemdata", "");
        String encryptedItemType = json.optString("itemtype", "");
        String playerName = json.optString("playername", "");
        String sourceServer = json.optString("servername", "");

        // Decrypt the data before storing
        String itemData = ConversationUtils.decrypt(encryptedItemData, convKey);
        String itemType = ConversationUtils.decrypt(encryptedItemType, convKey);

        pluginInstance.getAssetTransferManager().storePendingTransfer(
            transferUid,
            AssetTransferManager.TransferType.ITEM,
            itemData,
            itemType,
            playerName,
            sourceServer,
            targetServer
        );

        // Play sound for receiving item
        org.bukkit.entity.Player targetPlayer = pluginInstance.getServer().getPlayer(playerName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.playSound(targetPlayer.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
        }

        pluginInstance.getLogger().info(Prefix + "Item transfer received. UID: " + transferUid);
    }

    private void handleMoneyTransferPayload(byte[] payload) {
        org.json.JSONObject json = JsonPayloadHandler.parseJson(new String(payload));
        if (json == null) return;

        String targetPlayer = json.optString("targetplayer", "");
        String senderPlayer = json.optString("senderplayer", "");
        String amount = json.optString("amount", "0");
        String transferUid = json.optString("transferuid", "");
        String sourceServer = json.optString("servername", "");

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                org.bukkit.entity.Player player = pluginInstance.getServer().getPlayer(targetPlayer);
                if (player != null && player.isOnline()) {
                    double amountValue = Double.parseDouble(amount);
                    if (VaultIntegration.deposit(player, amountValue)) {
                        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                        placeholders.put("amount", VaultIntegration.format(amountValue));
                        placeholders.put("sender", senderPlayer);
                        placeholders.put("server", sourceServer);
                        player.sendMessage(Messages.get("money.received", placeholders));
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                        pluginInstance.getAuditLogger().logTransfer(transferUid, senderPlayer, sourceServer,
                            configManager.get("general.servername"), AuditLogger.TransferType.MONEY,
                            AuditLogger.TransferResult.SUCCESS, "Amount: " + amount);
                    } else {
                        pluginInstance.getAuditLogger().logTransfer(transferUid, senderPlayer, sourceServer,
                            configManager.get("general.servername"), AuditLogger.TransferType.MONEY,
                            AuditLogger.TransferResult.FAILED, "Deposit failed");
                    }
                }
            }
        }.runTask(pluginInstance);
    }

    private void handlePrivateMessagePayload(byte[] payload) {
        // Check if feature is enabled - handle both boolean and string "false"
        Object pmEnabledObj = configManager.getConfig().get("crossserverpm.enabled");
        boolean pmEnabled = pmEnabledObj == null || pmEnabledObj.toString().equalsIgnoreCase("true");
        if (!pmEnabled) {
            return;
        }

        org.json.JSONObject json = JsonPayloadHandler.parseJson(new String(payload));
        if (json == null) return;

        String convKey = configManager.get("communication.channel.key");
        String targetPlayer = ConversationUtils.decrypt(json.optString("targetplayer", ""), convKey);
        String senderName = ConversationUtils.decrypt(json.optString("sendername", ""), convKey);
        String message = ConversationUtils.decrypt(json.optString("message", ""), convKey);

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                org.bukkit.entity.Player player = pluginInstance.getServer().getPlayer(targetPlayer);
                if (player != null && player.isOnline()) {
                    if (!pluginInstance.getPlayerStateManager().isNotifyDisabled(player.getUniqueId())) {
                        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                        placeholders.put("sender", senderName);
                        placeholders.put("message", message);
                        player.sendMessage(Messages.get("pm.received_cross", placeholders));
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }
                }
            }
        }.runTask(pluginInstance);
    }

    private void handleServerInfoRequest(byte[] payload) {
        org.json.JSONObject json = JsonPayloadHandler.parseJson(new String(payload));
        if (json == null) return;

        String targetServer = json.optString("targetserver", "");
        String currentServer = configManager.get("general.servername");

        if (!targetServer.equalsIgnoreCase(currentServer)) {
            return;
        }

        String requestId = json.optString("requestid", "");
        String convKey = configManager.get("communication.channel.key");

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                int playerCount = pluginInstance.getServer().getOnlinePlayers().size();
                int maxPlayers = pluginInstance.getServer().getMaxPlayers();

                String motd = pluginInstance.getServer().getMotd();
                String version = pluginInstance.getServer().getVersion();
                String contact = configManager.get("general.serverip", "");

                String encryptedPlayerCount = ConversationUtils.encrypt(String.valueOf(playerCount), convKey);
                String encryptedMaxPlayers = ConversationUtils.encrypt(String.valueOf(maxPlayers), convKey);
                String encryptedMotd = ConversationUtils.encrypt(motd, convKey);
                String encryptedVersion = ConversationUtils.encrypt(version, convKey);

                sendServerInfoResponse(requestId, encryptedPlayerCount, encryptedMaxPlayers,
                                       encryptedMotd, encryptedVersion, contact);
            }
        }.runTask(pluginInstance);
    }

    private void handleServerInfoResponse(byte[] payload) {
        org.json.JSONObject json = JsonPayloadHandler.parseJson(new String(payload));
        if (json == null) return;

        String requestId = json.optString("requestid", "");
        String serverName = json.optString("servername", "");

        org.bukkit.command.CommandSender requester = pendingInfoRequests.remove(requestId);
        if (requester == null) {
            return;
        }

        String convKey = configManager.get("communication.channel.key");
        String playerCount = ConversationUtils.decrypt(json.optString("playercount", ""), convKey);
        String contact = json.optString("contact", "");

        // Extended info fields
        String maxPlayers = ConversationUtils.decrypt(json.optString("maxplayers", ""), convKey);
        String motd = ConversationUtils.decrypt(json.optString("motd", ""), convKey);
        String version = ConversationUtils.decrypt(json.optString("version", ""), convKey);

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                requester.sendMessage(Messages.get("serverinfo.header", "server", serverName));

                java.util.Map<String, String> playersPlaceholders = new java.util.HashMap<>();
                String playersValue = playerCount + (maxPlayers != null && !maxPlayers.isEmpty() ? "/" + maxPlayers : "");
                playersPlaceholders.put("players", playersValue);
                requester.sendMessage(Messages.get("serverinfo.players", playersPlaceholders));

                if (motd != null && !motd.isEmpty()) {
                    requester.sendMessage(Messages.get("serverinfo.motd", "motd", motd));
                }

                if (version != null && !version.isEmpty()) {
                    requester.sendMessage(Messages.get("serverinfo.version", "version", version));
                }

                if (contact != null && !contact.isEmpty()) {
                    requester.sendMessage(Messages.get("serverinfo.contact", "contact", contact));
                }
            }
        }.runTask(pluginInstance);
    }


    private void handleServerHeartbeat(byte[] payload) {
        org.json.JSONObject json = JsonPayloadHandler.parseJson(new String(payload));
        if (json == null) return;

        String serverName = json.optString("servername", "");
        int playerCount = json.optInt("playercount", 0);
        String contact = json.optString("contact", "");

        String currentServer = (String) configManager.get("general.servername");

        // Don't register own server
        if (!serverName.isEmpty() && !serverName.equals(currentServer)) {
            serverRegistry.registerServer(serverName, playerCount, contact);

            if (configManager.get("debug.showmessages")) {
                pluginInstance.getLogger().info(Prefix + "Heartbeat received from " + serverName + " (" + playerCount + " players)");
            }
        }
    }

    private void broadcastDecryptedMessage(String message, String playername, String servername) {
        if (configManager.get("debug.showmessages")) {
            pluginInstance.getLogger().info(Prefix + "A message was received and is gonna be broadcasted.");
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                String broadcastMessage = configManager.get("general.broadcastmessageformat").toString().replace("%SERVER%", servername).replace("%PLAYER%", playername).replace("%MESSAGE%", message);
                String colorConvertedMessage = MessageUtils.ColorConvert(broadcastMessage);

                pluginInstance.getServer().broadcastMessage(colorConvertedMessage);
            }
        }.runTask(pluginInstance);
    }

    public void sendEntityTransfer(String uid, String entityData, String entityType, String playerName, String targetServer) {
        try {
            String convKey = configManager.get("communication.channel.key");
            String encryptedEntityData = ConversationUtils.encrypt(entityData, convKey);
            String encryptedEntityType = ConversationUtils.encrypt(entityType, convKey);

            String jsonPayload = JsonPayloadHandler.createEntityTransferPayload(
                this.userUuid, uid, encryptedEntityData, encryptedEntityType, playerName,
                configManager.get("general.servername"), targetServer
            );

            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(1);

            String convTopic = configManager.get("communication.channel.id");
            IMqttToken token = client.publish(convTopic, message);
            token.waitForCompletion();

        } catch (MqttException e) {
            pluginInstance.getLogger().severe(Prefix + "Error sending entity transfer: " + e.getMessage());
        }
    }

    public void sendItemTransfer(String uid, String itemData, String itemType, String playerName, String targetServer) {
        try {
            String convKey = configManager.get("communication.channel.key");
            String encryptedItemData = ConversationUtils.encrypt(itemData, convKey);
            String encryptedItemType = ConversationUtils.encrypt(itemType, convKey);

            String jsonPayload = JsonPayloadHandler.createItemTransferPayload(
                this.userUuid, uid, encryptedItemData, encryptedItemType, playerName,
                configManager.get("general.servername"), targetServer
            );

            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(1);

            String convTopic = configManager.get("communication.channel.id");
            IMqttToken token = client.publish(convTopic, message);
            token.waitForCompletion();

        } catch (MqttException e) {
            pluginInstance.getLogger().severe(Prefix + "Error sending item transfer: " + e.getMessage());
        }
    }

    public void sendMoneyTransfer(String uid, double amount, String targetPlayer, String senderPlayer) {
        try {
            String convKey = configManager.get("communication.channel.key");
            String encryptedAmount = ConversationUtils.encrypt(String.valueOf(amount), convKey);
            String encryptedTargetPlayer = ConversationUtils.encrypt(targetPlayer, convKey);
            String encryptedSenderPlayer = ConversationUtils.encrypt(senderPlayer, convKey);

            String jsonPayload = JsonPayloadHandler.createMoneyTransferPayload(
                this.userUuid, uid, encryptedAmount, encryptedTargetPlayer, encryptedSenderPlayer,
                configManager.get("general.servername")
            );

            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(1);

            String convTopic = configManager.get("communication.channel.id");
            IMqttToken token = client.publish(convTopic, message);
            token.waitForCompletion();

        } catch (MqttException e) {
            pluginInstance.getLogger().severe(Prefix + "Error sending money transfer: " + e.getMessage());
        }
    }

    public void sendPrivateMessage(String targetPlayer, String message, String senderName) {
        // Check if feature is enabled - handle both boolean and string "false"
        Object pmEnabledObj = configManager.getConfig().get("crossserverpm.enabled");
        boolean pmEnabled = pmEnabledObj == null || pmEnabledObj.toString().equalsIgnoreCase("true");
        if (!pmEnabled) {
            return;
        }

        try {
            String convKey = configManager.get("communication.channel.key");
            String encryptedTargetPlayer = ConversationUtils.encrypt(targetPlayer, convKey);
            String encryptedMessage = ConversationUtils.encrypt(message, convKey);
            String encryptedSenderName = ConversationUtils.encrypt(senderName, convKey);

            String jsonPayload = JsonPayloadHandler.createPrivateMessagePayload(
                this.userUuid, encryptedMessage, encryptedTargetPlayer, encryptedSenderName,
                configManager.get("general.servername")
            );

            MqttMessage mqttMessage = new MqttMessage(jsonPayload.getBytes());
            mqttMessage.setQos(1);

            String convTopic = configManager.get("communication.channel.id");
            IMqttToken token = client.publish(convTopic, mqttMessage);
            token.waitForCompletion();

        } catch (MqttException e) {
            pluginInstance.getLogger().severe(Prefix + "Error sending private message: " + e.getMessage());
        }
    }


    public void requestServerList(org.bukkit.entity.Player player) {
        Map<String, ServerRegistry.ServerInfo> servers = serverRegistry.getServerDetails();

        if (servers.isEmpty()) {
            player.sendMessage(Messages.get("serverlist.no_servers", "", ""));
            return;
        }

        String currentServer = (String) configManager.get("general.servername");
        int currentPlayers = pluginInstance.getServer().getOnlinePlayers().size();
        String currentContact = configManager.get("general.serverip", "");

        // Open the GUI with all server information
        ServerListGUI.openServerListGUI(player, servers, currentServer, currentPlayers, currentContact);
    }

    public void requestServerInfo(String targetServer, org.bukkit.command.CommandSender sender) {
        try {
            String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
            String convKey = configManager.get("communication.channel.key");

            String encryptedRequesterName = ConversationUtils.encrypt(sender.getName(), convKey);
            String encryptedServerName = ConversationUtils.encrypt(configManager.get("general.servername"), convKey);

            String jsonPayload = JsonPayloadHandler.createServerInfoRequestPayload(
                this.userUuid, requestId, targetServer, encryptedRequesterName, encryptedServerName
            );

            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(1);

            String convTopic = configManager.get("communication.channel.id");
            IMqttToken token = client.publish(convTopic, message);
            token.waitForCompletion();

            pendingInfoRequests.put(requestId, sender);

            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    if (pendingInfoRequests.remove(requestId) != null) {
                        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                        placeholders.put("server", targetServer);
                        sender.sendMessage(Messages.get("serverinfo.no_response", placeholders));
                    }
                }
            }.runTaskLater(pluginInstance, 100L);

        } catch (MqttException e) {
            pluginInstance.getLogger().severe(Prefix + "Error requesting server info: " + e.getMessage());
        }
    }

    private void sendServerInfoResponse(String requestId, String playerCount, String maxPlayers,
                                         String motd, String version, String contact) {
        try {
            String jsonPayload = JsonPayloadHandler.createServerInfoResponsePayload(
                this.userUuid, requestId, playerCount, maxPlayers, motd, version,
                configManager.get("general.servername"), contact
            );

            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(1);

            String convTopic = configManager.get("communication.channel.id");
            IMqttToken token = client.publish(convTopic, message);
            token.waitForCompletion();

        } catch (MqttException e) {
            pluginInstance.getLogger().severe(Prefix + "Error sending server info response: " + e.getMessage());
        }
    }

    public void sendHeartbeat() {
        try {
            org.json.JSONObject jsonPayload = new org.json.JSONObject();
            jsonPayload.put("senderuuid", this.userUuid);
            jsonPayload.put("payloadtype", "SERVER_HEARTBEAT");

            String serverName = (String) configManager.get("general.servername");
            jsonPayload.put("servername", serverName);
            jsonPayload.put("playercount", pluginInstance.getServer().getOnlinePlayers().size());

            String contact = configManager.get("general.serverip", "");
            jsonPayload.put("contact", contact);

            MqttMessage message = new MqttMessage(jsonPayload.toString().getBytes());
            message.setQos(0); // QoS 0 for heartbeats (fire and forget)

            String convTopic = (String) configManager.get("communication.channel.id");
            client.publish(convTopic, message);

        } catch (Exception e) {
            // Silently fail for heartbeats
        }
    }

    public void requestPlayerLocation(String playerName, org.bukkit.command.CommandSender sender) {
        try {
            String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);

            String jsonPayload = JsonPayloadHandler.createPlayerLocationRequestPayload(
                this.userUuid, requestId, playerName, sender.getName()
            );

            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(1);

            String convTopic = configManager.get("communication.channel.id");
            IMqttToken token = client.publish(convTopic, message);
            token.waitForCompletion();

            pendingLocationRequests.put(requestId, sender);

            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    if (pendingLocationRequests.remove(requestId) != null) {
                        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                        placeholders.put("player", playerName);
                        sender.sendMessage(Messages.get("locate.player_not_found_global", placeholders));
                    }
                }
            }.runTaskLater(pluginInstance, 100L);

        } catch (MqttException e) {
            pluginInstance.getLogger().severe(Prefix + "Error requesting player location: " + e.getMessage());
        }
    }

    private void handlePlayerLocationRequest(byte[] payload) {
        org.json.JSONObject json = JsonPayloadHandler.parseJson(new String(payload));
        if (json == null) return;

        String targetPlayerName = json.optString("targetplayer", "");
        String requestId = json.optString("requestid", "");
        String requesterName = json.optString("requestername", "");

        // Check if locate remote resolution is enabled
        boolean allowRemoteResolution = configManager.get("locate.allow-remote-resolution", true);
        if (!allowRemoteResolution) {
            String serverName = configManager.get("general.servername");
            String jsonPayload = JsonPayloadHandler.createPlayerLocationResponsePayload(
                this.userUuid, requestId, targetPlayerName, serverName, "", false, true
            );

            try {
                MqttMessage message = new MqttMessage(jsonPayload.getBytes());
                message.setQos(1);

                String convTopic = configManager.get("communication.channel.id");
                IMqttToken token = client.publish(convTopic, message);
                token.waitForCompletion();

            } catch (MqttException e) {
                pluginInstance.getLogger().severe(Prefix + "Error sending player location response: " + e.getMessage());
            }
            return;
        }

        // Check if player is on this server
        org.bukkit.entity.Player player = pluginInstance.getServer().getPlayer(targetPlayerName);
        if (player != null && player.isOnline()) {
            String serverName = configManager.get("general.servername");
            String contact = configManager.get("general.serverip", "");

            // Notify the player for privacy reasons
            boolean notifyPlayer = configManager.get("locate.notify-located-player", true);
            if (notifyPlayer) {
                java.util.Map<String, String> noticePlaceholders = new java.util.HashMap<>();
                noticePlaceholders.put("requester", requesterName);
                player.sendMessage(Messages.get("locate.privacy_notice", noticePlaceholders));
            }

            String jsonPayload = JsonPayloadHandler.createPlayerLocationResponsePayload(
                this.userUuid, requestId, targetPlayerName, serverName, contact != null ? contact : "", true, false
            );

            try {
                MqttMessage message = new MqttMessage(jsonPayload.getBytes());
                message.setQos(1);

                String convTopic = configManager.get("communication.channel.id");
                IMqttToken token = client.publish(convTopic, message);
                token.waitForCompletion();

            } catch (MqttException e) {
                pluginInstance.getLogger().severe(Prefix + "Error sending player location response: " + e.getMessage());
            }
        }
    }

    private void handlePlayerLocationResponse(byte[] payload) {
        org.json.JSONObject json = JsonPayloadHandler.parseJson(new String(payload));
        if (json == null) return;

        String requestId = json.optString("requestid", "");
        String targetPlayerName = json.optString("targetplayer", "");
        String serverName = json.optString("servername", "");
        String contact = json.optString("contact", "");
        boolean found = json.optBoolean("found", false);
        boolean disabled = json.optBoolean("disabled", false);

        org.bukkit.command.CommandSender requester = pendingLocationRequests.remove(requestId);
        if (requester == null) {
            return;
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (disabled) {
                    requester.sendMessage(Messages.get("locate.disabled_remote"));
                } else if (found) {
                    requester.sendMessage(Messages.get("locate.player_header"));

                    java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                    placeholders.put("player", targetPlayerName);
                    placeholders.put("server", serverName);
                    requester.sendMessage(Messages.get("locate.player_location", placeholders));

                    if (contact != null && !contact.isEmpty()) {
                        java.util.Map<String, String> contactPlaceholders = new java.util.HashMap<>();
                        contactPlaceholders.put("contact", contact);
                        requester.sendMessage(Messages.get("locate.player_contact", contactPlaceholders));
                    }
                }
            }
        }.runTask(pluginInstance);
    }

    public void startHeartbeatTask() {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                // Check connection state and attempt to repair if needed
                if (client == null || !client.isConnected()) {
                    isConnected = false;

                    // Check if enough time has passed before attempting reconnection
                    long timeSinceLastAttempt = System.currentTimeMillis() - lastConnectionAttempt;
                    long backoffDelay = calculateBackoffDelay(reconnectAttempts);

                    if (timeSinceLastAttempt >= backoffDelay) {
                        pluginInstance.getLogger().warning(Prefix + "Connection lost. Attempting automatic reconnection (attempt " + (reconnectAttempts + 1) + "/" + MAX_RECONNECT_ATTEMPTS + ")");
                        connect();
                    }
                } else {
                    // Connection is good, send heartbeat
                    isConnected = true;
                    sendHeartbeat();
                }
            }
        }.runTaskTimerAsynchronously(pluginInstance, 100L, 200L); // Every 10 seconds
    }

    private long calculateBackoffDelay(int attemptCount) {
        // Exponential backoff: 5s, 10s, 20s, 40s, 80s, 160s, 300s, 300s, 300s...
        long delay = RECONNECT_BACKOFF_BASE_MS * (long) Math.pow(2, Math.min(attemptCount, 5));
        return Math.min(delay, RECONNECT_MAX_BACKOFF_MS);
    }

    public boolean isConnectionHealthy() {
        return isConnected && client != null && client.isConnected();
    }

}
