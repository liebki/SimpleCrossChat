package de.liebki.simplecrosschatplus.models.payloads;

import de.liebki.simplecrosschatplus.models.JsonPayload;
import de.liebki.simplecrosschatplus.models.PayloadType;

public class ItemTransferPayload extends JsonPayload {

    private final String transferUid;
    private final String encryptedItemData;
    private final String encryptedItemType;
    private final String targetServer;

    public ItemTransferPayload(String senderUuid, String encryptedMessage, String encryptedPlayerDisplayname,
                                String encryptedServerName, String transferUid, String encryptedItemData,
                                String encryptedItemType, String targetServer) {

        super(senderUuid, PayloadType.ITEM_TRANSFER, encryptedMessage, encryptedPlayerDisplayname, encryptedServerName);
        this.transferUid = transferUid;
        this.encryptedItemData = encryptedItemData;
        this.encryptedItemType = encryptedItemType;
        this.targetServer = targetServer;
    }

    public String getTransferUid() {
        return transferUid;
    }

    public String getEncryptedItemData() {
        return encryptedItemData;
    }

    public String getEncryptedItemType() {
        return encryptedItemType;
    }

    public String getTargetServer() {
        return targetServer;
    }

}

