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
        configManager.set("technical.broker.address", "test.mosquitto.org");
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