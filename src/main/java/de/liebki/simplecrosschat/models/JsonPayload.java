package de.liebki.simplecrosschat.models;

public class JsonPayload {

    private final String senderUuid;
    private final String encryptedMessage;
    private final String encryptedPlayerDisplayname;
    private final String encryptedServerName;

    public JsonPayload(String senderUuid, String encryptedMessage, String encryptedPlayerDisplayname, String encryptedServerName) {
        this.senderUuid = senderUuid;
        this.encryptedMessage = encryptedMessage;
        this.encryptedPlayerDisplayname = encryptedPlayerDisplayname;
        this.encryptedServerName = encryptedServerName;
    }

    public String getSenderUuid() {
        return senderUuid;
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
