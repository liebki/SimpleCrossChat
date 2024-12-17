package de.liebki.simplecrosschat;

import de.liebki.simplecrosschat.events.ChatEvent;
import de.liebki.simplecrosschat.utils.ConfigManager;
import de.liebki.simplecrosschat.utils.MQTTClientManager;
import de.liebki.simplecrosschat.utils.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class SimpleCrossChat extends JavaPlugin {

    public ConfigManager configManager;
    private MQTTClientManager mqttClientManager;
    private final String userUuid = UUID.randomUUID().toString();

    @Override
    public void onEnable() {
        configManager = new ConfigManager("plugins/simplecrosschat", "options.yml", this);
        boolean configExists = configManager.check("donottouch.configexists");

        if (!configExists) {
            createConfigDefaults();
        }

        boolean isEnabled = configManager.get("general.enabled");
        if (!isEnabled) {
            this.getServer().getLogger().warning("'general.enabled' is set to false, the plugin is not doing anything.");
            return;
        }

        mqttClientManager = new MQTTClientManager(configManager, userUuid, this);
        this.getServer().getPluginManager().registerEvents(new ChatEvent(mqttClientManager, configManager), this);

        new Thread(mqttClientManager::connect).start();
        new Metrics(this, 24174);
    }

    private void createConfigDefaults() {
        configManager.set("donottouch.configexists", true);
        configManager.set("debug.showmessages", false);

        configManager.set("general.info", "- INFO: your servername is publicly viewable and so are the names of the others!");
        configManager.set("general.servername", this.getServer().getName());
        configManager.set("general.broadcastmessageformat", "&a%PLAYER% &0| &f%MESSAGE%");
        configManager.set("general.enabled", true);

        configManager.set("general.info", "- INFO: Please be advised to take a look at the brokers technical informations");
        configManager.set("general.privacyinfo", "- INFO: Even tho the messages are not readable for outstanders, I would host my own broker for a safe communication!");
        configManager.set("technical.broker.address", "test.mosquitto.org");
        configManager.set("technical.broker.protocol", "tcp");
        configManager.set("technical.broker.port", 1883);

        configManager.set("general.info", "- INFO: id and key have to match on other servers, to enable a global chat, one value different and it wont work");
        configManager.set("communication.channel.id", "simplecrosschatwelcome");
        configManager.set("communication.channel.key", "emoclewtahcssorcelpmis");
        configManager.saveConfig();
    }

    @Override
    public void onDisable() {
        mqttClientManager.disconnect();
    }

}