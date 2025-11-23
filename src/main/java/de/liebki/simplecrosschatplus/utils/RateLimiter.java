package de.liebki.simplecrosschatplus.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RateLimiter {

    private final Map<String, Long> cooldowns;
    private final int defaultCooldownSeconds;

    public RateLimiter(int defaultCooldownSeconds) {
        this.cooldowns = new HashMap<>();
        this.defaultCooldownSeconds = defaultCooldownSeconds;
    }

    public boolean isOnCooldown(UUID playerUuid, String action) {
        String key = playerUuid.toString() + ":" + action;
        Long lastUse = cooldowns.get(key);

        if (lastUse == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownTime = defaultCooldownSeconds * 1000L;

        return (currentTime - lastUse) < cooldownTime;
    }

    public long getRemainingCooldown(UUID playerUuid, String action) {
        String key = playerUuid.toString() + ":" + action;
        Long lastUse = cooldowns.get(key);

        if (lastUse == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownTime = defaultCooldownSeconds * 1000L;
        long elapsed = currentTime - lastUse;

        if (elapsed >= cooldownTime) {
            return 0;
        }

        return (cooldownTime - elapsed) / 1000;
    }

    public void setCooldown(UUID playerUuid, String action) {
        String key = playerUuid.toString() + ":" + action;
        cooldowns.put(key, System.currentTimeMillis());
    }

    public void removeCooldown(UUID playerUuid, String action) {
        String key = playerUuid.toString() + ":" + action;
        cooldowns.remove(key);
    }

}

