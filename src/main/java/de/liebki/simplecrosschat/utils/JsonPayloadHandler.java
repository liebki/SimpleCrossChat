package de.liebki.simplecrosschat.utils;

import de.liebki.simplecrosschat.models.JsonPayload;
import org.json.JSONException;
import org.json.JSONObject;

public final class JsonPayloadHandler {

    private JsonPayloadHandler() {
    }

    public static String createJsonPayload(String senderUuid, String message, String playerName, String serverName) {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("senderuuid", senderUuid);

        jsonPayload.put("message", message);
        jsonPayload.put("playername", playerName);

        jsonPayload.put("servername", serverName);
        return jsonPayload.toString();
    }

    public static JsonPayload readJsonPayload(String jsonString) {
        try {
            JSONObject jsonPayload = new JSONObject(jsonString);
            String senderUuid = jsonPayload.getString("senderuuid");

            String encryptedMessage = jsonPayload.getString("message");
            String encryptedPlayerName = jsonPayload.getString("playername");

            String encryptedServerName = jsonPayload.getString("servername");
            return new JsonPayload(senderUuid, encryptedMessage, encryptedPlayerName, encryptedServerName);

        } catch (JSONException e) {
            return null;
        }
    }

}