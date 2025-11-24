package de.liebki.simplecrosschatplus.utils;

import java.util.HashMap;
import java.util.Map;

public class Messages {

    private static ConfigManager configManager;

    public static void init(ConfigManager manager) {
        configManager = manager;
    }

    public static String get(String key) {
        if (configManager == null) {
            return MessageUtils.ColorConvert("&cMissing message config: " + key);
        }

        String path = "messages." + key;
        String value = configManager.get(path);
        if (value == null) {
            return MessageUtils.ColorConvert("&cMissing message: " + key);
        }

        return MessageUtils.ColorConvert(value);
    }

    public static String get(String key, Map<String, String> placeholders) {
        String message = get(key);
        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }

        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            result = result.replace(placeholder, entry.getValue());
        }

        return result;
    }

    public static String get(String key, String placeholderKey, String placeholderValue) {
        Map<String, String> map = new HashMap<>();
        map.put(placeholderKey, placeholderValue);
        return get(key, map);
    }
}

