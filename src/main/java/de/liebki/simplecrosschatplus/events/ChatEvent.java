package de.liebki.simplecrosschatplus.events;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.ConfigManager;
import de.liebki.simplecrosschatplus.utils.MQTTClientManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatEvent implements Listener {

    MQTTClientManager mqttManagerInstance;
    ConfigManager configManager;
    SimpleCrossChat plugin;

    public ChatEvent(MQTTClientManager simpleCrossChat, ConfigManager configManager, SimpleCrossChat plugin) {
        this.mqttManagerInstance = simpleCrossChat;
        this.configManager = configManager;
        this.plugin = plugin;
    }

    @EventHandler
    //(priority = EventPriority.HIGHEST) - Will be used to get the messages AFTER other plugins changed chat format etc.
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!event.getMessage().isEmpty() && !event.getMessage().equalsIgnoreCase(" ")) {

            if (plugin.getPlayerStateManager().isChatDisabled(event.getPlayer().getUniqueId())) {
                return;
            }

            if (plugin.getPlayerStateManager().isAdminDisabled(event.getPlayer().getUniqueId())) {
                return;
            }

            mqttManagerInstance.sendMessage(event.getMessage(), event.getPlayer().getDisplayName(), configManager.get("general.servername"));
        }
    }

}
