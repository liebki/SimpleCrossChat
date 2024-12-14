package de.liebki.simplecrosschat.utils;

import de.liebki.simplecrosschat.SimpleCrossChat;
import de.liebki.simplecrosschat.models.JsonPayload;
import org.bukkit.scheduler.BukkitRunnable;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.Arrays;

public class MQTTClientManager {

    private static String Prefix = "[SimpleCrossChat] ";
    private int errorSendCounter = 0;
    private final ConfigManager configManager;
    private MqttAsyncClient client;
    private final String userUuid;
    private final MqttProperties properties;
    private final SimpleCrossChat pluginInstance;

    public MQTTClientManager(ConfigManager config, String userUuid, SimpleCrossChat pluginInstance) {
        this.configManager = config;
        this.userUuid = userUuid;
        this.properties = new MqttProperties();
        this.pluginInstance = pluginInstance;

        this.properties.setSubscriptionIdentifiers(Arrays.asList(new Integer[]{0}));
    }

    public void connect() {
        try {
            String broker = getCompleteBrokerAddress();
            client = new MqttAsyncClient(broker, userUuid);

            MqttConnectionOptions connOpts = new MqttConnectionOptions();
            connOpts.setCleanStart(true);

            pluginInstance.getLogger().info(Prefix + "Connecting to your configured broker: " + broker);
            IMqttToken token = client.connect(connOpts);

            token.waitForCompletion();
            pluginInstance.getLogger().info(Prefix + "Connected to your configured broker: " + broker);

            String convTopic = configManager.get("communication.channel.id");
            subscribeToTopic(convTopic);

        } catch (MqttException e) {
            pluginInstance.getLogger().warning(Prefix + "Disabling SimpleCrossChat because of connection problems, please resolve these!");
            pluginInstance.getLogger().severe(Prefix + "Error while connecting to broker: " + e.getMessage());

            pluginInstance.getServer().getPluginManager().disablePlugin(pluginInstance);
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
        } catch (MqttException e) {
            pluginInstance.getLogger().severe(Prefix + "Error while disconnecting from broker: " + e.getMessage());
        }
    }

    public void sendMessage(String messageContent, String username, String servername) {
        try {

            String encryptedUserMessage = ConversationUtils.encrypt(messageContent, configManager.get("communication.channel.key"));
            String encryptedUsername = ConversationUtils.encrypt(username, configManager.get("communication.channel.key"));

            String encryptedServername = ConversationUtils.encrypt(servername, configManager.get("communication.channel.key"));

            if(encryptedUserMessage != null && encryptedUserMessage.length() > 0 && encryptedUsername.length() > 0 && encryptedServername.length() > 0 && encryptedServername.length() > 0) {
                String jsonMessagePayload = JsonPayloadHandler.createJsonPayload(this.userUuid, encryptedUserMessage, encryptedUsername, encryptedServername);

                MqttMessage message = new MqttMessage(jsonMessagePayload.getBytes());
                message.setQos(1);

                String convTopic = configManager.get("communication.channel.id");
                IMqttToken token = client.publish(convTopic, message);

                token.waitForCompletion();

                if(configManager.get("debug.showmessages")) {
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

    private void subscribeToTopic(String topic) {
        if (topic != null && !topic.isEmpty()) {
            try {
                MqttSubscription subscription = new MqttSubscription(topic, 1);
                IMqttToken token = client.subscribe(new MqttSubscription[]{subscription}, null, null, (receivedTopic, message) -> {
                    processMessage(receivedTopic, message);
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

    private void processMessage(String topic, MqttMessage message) {
        JsonPayload receivedMessage = JsonPayloadHandler.readJsonPayload(new String(message.getPayload()));

        if (receivedMessage != null) {
            if (receivedMessage.getSenderUuid().equals(this.userUuid)) {

                if(configManager.get("debug.showmessages")) {
                    pluginInstance.getLogger().warning(Prefix + "The sending server and receiving is the same, no action required.");
                }
                return;
            }

            if(configManager.get("debug.showmessages")) {
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
    }

    private void broadcastDecryptedMessage(String message, String playername, String servername) {
        if(configManager.get("debug.showmessages")) {
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
}