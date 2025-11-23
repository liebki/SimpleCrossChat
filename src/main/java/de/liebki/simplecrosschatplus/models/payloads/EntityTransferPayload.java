package de.liebki.simplecrosschatplus.models.payloads;

import de.liebki.simplecrosschatplus.models.JsonPayload;
import de.liebki.simplecrosschatplus.models.PayloadType;

public class EntityTransferPayload extends JsonPayload {

    private final String transferUid;
    private final String encryptedEntityData;
    private final String encryptedEntityType;
    private final String targetServer;

    public EntityTransferPayload(String senderUuid, String encryptedMessage, String encryptedPlayerDisplayname,
                                  String encryptedServerName, String transferUid, String encryptedEntityData,
                                  String encryptedEntityType, String targetServer) {

        super(senderUuid, PayloadType.ENTITY_TRANSFER, encryptedMessage, encryptedPlayerDisplayname, encryptedServerName);
        this.transferUid = transferUid;
        this.encryptedEntityData = encryptedEntityData;
        this.encryptedEntityType = encryptedEntityType;
        this.targetServer = targetServer;
    }

    public String getTransferUid() {
        return transferUid;
    }

    public String getEncryptedEntityData() {
        return encryptedEntityData;
    }

    public String getEncryptedEntityType() {
        return encryptedEntityType;
    }

    public String getTargetServer() {
        return targetServer;
    }

}

