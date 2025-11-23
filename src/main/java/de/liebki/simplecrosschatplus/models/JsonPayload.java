package de.liebki.simplecrosschatplus.models;

public class JsonPayload {

    private final String senderUuid;
    private final PayloadType payloadType;
    private final String encryptedMessage;
    private final String encryptedPlayerDisplayname;
    private final String encryptedServerName;

    public JsonPayload(String senderUuid, PayloadType payloadType, String encryptedMessage, String encryptedPlayerDisplayname, String encryptedServerName) {
        this.senderUuid = senderUuid;
        this.payloadType = payloadType;
        this.encryptedMessage = encryptedMessage;
        this.encryptedPlayerDisplayname = encryptedPlayerDisplayname;
        this.encryptedServerName = encryptedServerName;
    }

    public String getSenderUuid() {
        return senderUuid;
    }

    public PayloadType getPayloadType() {
        return payloadType;
    }

    public String getEncryptedMessage() {
        return encryptedMessage;
    }

    public String getEncryptedPlayerDisplayname() {
        return encryptedPlayerDisplayname;
    }

    public String getEncryptedServerName() {
        return encryptedServerName;
    }

}
