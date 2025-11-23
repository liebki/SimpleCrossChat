package de.liebki.simplecrosschatplus.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditLogger {

    private final File auditFile;
    private FileConfiguration auditData;
    private final DateTimeFormatter formatter;

    public enum TransferType {
        ENTITY,
        ITEM,
        MONEY
    }

    public enum TransferResult {
        SUCCESS,
        VERSION_DOWNGRADE,
        FALLBACK_EGG,
        FALLBACK_BASE_ITEM,
        FAILED,
        ROLLBACK
    }

    public AuditLogger(String dataFolder) {
        this.auditFile = new File(dataFolder, "audit.yml");
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if (!auditFile.exists()) {
            try {
                auditFile.getParentFile().mkdirs();
                auditFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.auditData = YamlConfiguration.loadConfiguration(auditFile);
    }

    public void logTransfer(String transferUid, String playerName, String sourceServer, String targetServer,
                             TransferType type, TransferResult result, String additionalInfo) {

        String timestamp = LocalDateTime.now().format(formatter);
        String path = "transfers." + transferUid;

        auditData.set(path + ".timestamp", timestamp);
        auditData.set(path + ".player", playerName);
        auditData.set(path + ".source-server", sourceServer);
        auditData.set(path + ".target-server", targetServer);
        auditData.set(path + ".type", type.name());
        auditData.set(path + ".result", result.name());

        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            auditData.set(path + ".info", additionalInfo);
        }

        saveAuditData();
    }

    public List<Map<String, String>> getRecentTransfers(int limit) {
        List<Map<String, String>> transfers = new ArrayList<>();

        if (!auditData.contains("transfers")) {
            return transfers;
        }

        for (String uid : auditData.getConfigurationSection("transfers").getKeys(false)) {
            Map<String, String> transfer = new HashMap<>();
            String path = "transfers." + uid;

            transfer.put("uid", uid);
            transfer.put("timestamp", auditData.getString(path + ".timestamp", ""));
            transfer.put("player", auditData.getString(path + ".player", ""));
            transfer.put("source", auditData.getString(path + ".source-server", ""));
            transfer.put("target", auditData.getString(path + ".target-server", ""));
            transfer.put("type", auditData.getString(path + ".type", ""));
            transfer.put("result", auditData.getString(path + ".result", ""));
            transfer.put("info", auditData.getString(path + ".info", ""));

            transfers.add(transfer);

            if (transfers.size() >= limit) {
                break;
            }
        }

        return transfers;
    }

    private void saveAuditData() {
        try {
            auditData.save(auditFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

