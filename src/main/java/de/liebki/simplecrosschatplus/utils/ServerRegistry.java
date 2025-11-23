package de.liebki.simplecrosschatplus.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerRegistry {

    private final Map<String, ServerInfo> servers;
    private final long timeoutMillis;

    public ServerRegistry(long timeoutSeconds) {
        this.servers = new ConcurrentHashMap<>();
        this.timeoutMillis = timeoutSeconds * 1000;
    }

    public void registerServer(String serverName, int playerCount) {
        ServerInfo info = servers.getOrDefault(serverName, new ServerInfo(serverName));
        info.lastSeen = System.currentTimeMillis();
        info.playerCount = playerCount;
        servers.put(serverName, info);
    }

    public Set<String> getActiveServers() {
        cleanupStaleServers();
        return servers.keySet();
    }

    public Map<String, ServerInfo> getServerDetails() {
        cleanupStaleServers();
        return new HashMap<>(servers);
    }

    private void cleanupStaleServers() {
        long now = System.currentTimeMillis();
        servers.entrySet().removeIf(entry -> (now - entry.getValue().lastSeen) > timeoutMillis);
    }

    public static class ServerInfo {
        public final String name;
        public long lastSeen;
        public int playerCount;

        public ServerInfo(String name) {
            this.name = name;
            this.lastSeen = System.currentTimeMillis();
            this.playerCount = 0;
        }
    }

}

