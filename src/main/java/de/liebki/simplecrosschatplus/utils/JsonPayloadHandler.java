package de.liebki.simplecrosschatplus.utils;

import de.liebki.simplecrosschatplus.models.JsonPayload;
import de.liebki.simplecrosschatplus.models.PayloadType;
import org.json.JSONException;
import org.json.JSONObject;

public final class JsonPayloadHandler {

    private JsonPayloadHandler() {
    }

    public static String createJsonPayload(String senderUuid, String message, String playerName, String serverName) {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("senderuuid", senderUuid);
        jsonPayload.put("payloadtype", PayloadType.CHAT.name());
        jsonPayload.put("message", message);
        jsonPayload.put("playername", playerName);
        jsonPayload.put("servername", serverName);
        return jsonPayload.toString();
    }

    public static String createEntityTransferPayload(String senderUuid, String transferUid, String entityData,
                                                      String entityType, String playerName, String serverName,
                                                      String targetServer) {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("senderuuid", senderUuid);
        jsonPayload.put("payloadtype", PayloadType.ENTITY_TRANSFER.name());
        jsonPayload.put("transferuid", transferUid);
        jsonPayload.put("entitydata", entityData);
        jsonPayload.put("entitytype", entityType);
        jsonPayload.put("playername", playerName);
        jsonPayload.put("servername", serverName);
        jsonPayload.put("targetserver", targetServer);
        return jsonPayload.toString();
    }

    public static String createItemTransferPayload(String senderUuid, String transferUid, String itemData,
                                                    String itemType, String playerName, String serverName,
                                                    String targetServer) {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("senderuuid", senderUuid);
        jsonPayload.put("payloadtype", PayloadType.ITEM_TRANSFER.name());
        jsonPayload.put("transferuid", transferUid);
        jsonPayload.put("itemdata", itemData);
        jsonPayload.put("itemtype", itemType);
        jsonPayload.put("playername", playerName);
        jsonPayload.put("servername", serverName);
        jsonPayload.put("targetserver", targetServer);
        return jsonPayload.toString();
    }

    public static String createMoneyTransferPayload(String senderUuid, String transferUid, String amount,
                                                     String targetPlayer, String senderPlayer, String serverName) {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("senderuuid", senderUuid);
        jsonPayload.put("payloadtype", PayloadType.MONEY_TRANSFER.name());
        jsonPayload.put("transferuid", transferUid);
        jsonPayload.put("amount", amount);
        jsonPayload.put("targetplayer", targetPlayer);
        jsonPayload.put("senderplayer", senderPlayer);
        jsonPayload.put("servername", serverName);
        return jsonPayload.toString();
    }

    public static String createPrivateMessagePayload(String senderUuid, String message, String targetPlayer,
                                                      String senderName, String serverName) {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("senderuuid", senderUuid);
        jsonPayload.put("payloadtype", PayloadType.PRIVATE_MESSAGE.name());
        jsonPayload.put("message", message);
        jsonPayload.put("targetplayer", targetPlayer);
        jsonPayload.put("sendername", senderName);
        jsonPayload.put("servername", serverName);
        return jsonPayload.toString();
    }

    public static String createServerInfoRequestPayload(String senderUuid, String requestId, String targetServer,
                                                         String requesterName, String serverName) {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("senderuuid", senderUuid);
        jsonPayload.put("payloadtype", PayloadType.SERVER_INFO_REQUEST.name());
        jsonPayload.put("requestid", requestId);
        jsonPayload.put("targetserver", targetServer);
        jsonPayload.put("requestername", requesterName);
        jsonPayload.put("servername", serverName);
        return jsonPayload.toString();
    }

    public static String createServerInfoResponsePayload(String senderUuid, String requestId, String playerCount,
                                                          String tps, String serverName, String contact) {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("senderuuid", senderUuid);
        jsonPayload.put("payloadtype", PayloadType.SERVER_INFO_RESPONSE.name());
        jsonPayload.put("requestid", requestId);
        jsonPayload.put("playercount", playerCount);
        jsonPayload.put("tps", tps);
        jsonPayload.put("servername", serverName);
        jsonPayload.put("contact", contact);
        return jsonPayload.toString();
    }

    public static JsonPayload readJsonPayload(String jsonString) {
        try {
            JSONObject jsonPayload = new JSONObject(jsonString);
            String senderUuid = jsonPayload.getString("senderuuid");

            String payloadTypeStr = jsonPayload.optString("payloadtype", "CHAT");
            PayloadType payloadType;
            try {
                payloadType = PayloadType.valueOf(payloadTypeStr);
            } catch (IllegalArgumentException e) {
                payloadType = PayloadType.CHAT;
            }

            String encryptedMessage = jsonPayload.optString("message", "");
            String encryptedPlayerName = jsonPayload.optString("playername", "");
            String encryptedServerName = jsonPayload.optString("servername", "");

            return new JsonPayload(senderUuid, payloadType, encryptedMessage, encryptedPlayerName, encryptedServerName);

        } catch (JSONException e) {
            return null;
        }
    }

    public static JSONObject parseJson(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            return null;
        }
    }

}