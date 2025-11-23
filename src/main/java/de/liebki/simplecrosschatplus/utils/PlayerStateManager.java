package de.liebki.simplecrosschatplus.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerStateManager {

    private final File playerDataFile;
    private FileConfiguration playerData;

    private final Map<UUID, Boolean> chatDisabledCache;
    private final Map<UUID, Boolean> notifyDisabledCache;
    private final Set<UUID> adminDisabledPlayers;

    public PlayerStateManager(String dataFolder) {
        this.playerDataFile = new File(dataFolder, "playerdata.yml");
        this.chatDisabledCache = new HashMap<>();
        this.notifyDisabledCache = new HashMap<>();
        this.adminDisabledPlayers = new HashSet<>();

        loadPlayerData();
    }

    private void loadPlayerData() {
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.getParentFile().mkdirs();
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        playerData = YamlConfiguration.loadConfiguration(playerDataFile);

        for (String uuidString : playerData.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);

                boolean chatDisabled = playerData.getBoolean(uuidString + ".chat-disabled", false);
                boolean notifyDisabled = playerData.getBoolean(uuidString + ".notify-disabled", false);
                boolean adminDisabled = playerData.getBoolean(uuidString + ".admin-disabled", false);

                chatDisabledCache.put(uuid, chatDisabled);
                notifyDisabledCache.put(uuid, notifyDisabled);

                if (adminDisabled) {
                    adminDisabledPlayers.add(uuid);
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID, skip
            }
        }
    }

    public void savePlayerData() {
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isChatDisabled(UUID playerUuid) {
        return chatDisabledCache.getOrDefault(playerUuid, false);
    }

    public void setChatDisabled(UUID playerUuid, boolean disabled) {
        chatDisabledCache.put(playerUuid, disabled);
        playerData.set(playerUuid.toString() + ".chat-disabled", disabled);
        savePlayerData();
    }

    public boolean isNotifyDisabled(UUID playerUuid) {
        return notifyDisabledCache.getOrDefault(playerUuid, false);
    }

    public void setNotifyDisabled(UUID playerUuid, boolean disabled) {
        notifyDisabledCache.put(playerUuid, disabled);
        playerData.set(playerUuid.toString() + ".notify-disabled", disabled);
        savePlayerData();
    }

    public boolean isAdminDisabled(UUID playerUuid) {
        return adminDisabledPlayers.contains(playerUuid);
    }

    public void setAdminDisabled(UUID playerUuid, boolean disabled) {
        if (disabled) {
            adminDisabledPlayers.add(playerUuid);
        } else {
            adminDisabledPlayers.remove(playerUuid);
        }

        playerData.set(playerUuid.toString() + ".admin-disabled", disabled);
        savePlayerData();
    }

}

