package de.liebki.simplecrosschatplus.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AssetTransferManager {

    private final File transferFile;
    private FileConfiguration transferData;

    public enum TransferType {
        ENTITY,
        ITEM
    }

    public AssetTransferManager(String dataFolder) {
        this.transferFile = new File(dataFolder, "pending_transfers.yml");

        if (!transferFile.exists()) {
            try {
                transferFile.getParentFile().mkdirs();
                transferFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.transferData = YamlConfiguration.loadConfiguration(transferFile);
    }

    public String generateTransferUid() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void storePendingTransfer(String uid, TransferType type, String data, String dataType,
                                      String playerName, String sourceServer, String targetServer) {

        String path = "pending." + uid;

        transferData.set(path + ".type", type.name());
        transferData.set(path + ".data", data);
        transferData.set(path + ".dataType", dataType);
        transferData.set(path + ".player", playerName);
        transferData.set(path + ".source", sourceServer);
        transferData.set(path + ".target", targetServer);
        transferData.set(path + ".timestamp", System.currentTimeMillis());

        saveTransferData();
    }

    public Map<String, Object> getPendingTransfer(String uid) {
        String path = "pending." + uid;

        if (!transferData.contains(path)) {
            return null;
        }

        Map<String, Object> transfer = new HashMap<>();
        transfer.put("type", transferData.getString(path + ".type"));
        transfer.put("data", transferData.getString(path + ".data"));
        transfer.put("dataType", transferData.getString(path + ".dataType"));
        transfer.put("player", transferData.getString(path + ".player"));
        transfer.put("source", transferData.getString(path + ".source"));
        transfer.put("target", transferData.getString(path + ".target"));
        transfer.put("timestamp", transferData.getLong(path + ".timestamp"));

        return transfer;
    }

    public void removePendingTransfer(String uid) {
        transferData.set("pending." + uid, null);
        saveTransferData();
    }

    public boolean hasPendingTransfer(String uid) {
        return transferData.contains("pending." + uid);
    }

    private void saveTransferData() {
        try {
            transferData.save(transferFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

