package de.liebki.simplecrosschat.events;

import de.liebki.simplecrosschat.utils.ConfigManager;
import de.liebki.simplecrosschat.utils.MQTTClientManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatEvent implements Listener {

    MQTTClientManager mqttManagerInstance;
    ConfigManager configManager;

    public ChatEvent(MQTTClientManager simpleCrossChat, ConfigManager configManager) {
        this.mqttManagerInstance = simpleCrossChat;
        this.configManager = configManager;
    }

    @EventHandler
    //(priority = EventPriority.HIGHEST) - Will be used to get the messages AFTER other plugins changed chat format etc.
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!event.getMessage().isEmpty() && !event.getMessage().equalsIgnoreCase(" ")) {
            mqttManagerInstance.sendMessage(event.getMessage(), event.getPlayer().getDisplayName(), configManager.get("general.servername"));
        }
    }

}
