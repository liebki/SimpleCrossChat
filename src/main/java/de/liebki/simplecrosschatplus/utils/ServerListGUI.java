package de.liebki.simplecrosschatplus.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerListGUI {

    private ServerListGUI() {
        // Utility class
    }

    private static JavaPlugin pluginInstance;

    public static void setPluginInstance(JavaPlugin plugin) {
        pluginInstance = plugin;
    }

    public static void openServerListGUI(Player player, Map<String, ServerRegistry.ServerInfo> remoteServers,
                                         String currentServerName, int currentPlayers, String currentContact) {

        // Use chest inventory (6 rows = 54 slots)
        Inventory gui = Bukkit.createInventory(null, 54, Messages.get("serverlist.title"));

        int slot = 0;

        // Add current server first (highlighted in green)
        ItemStack currentServerItem = createServerHead(currentServerName, currentPlayers, -1, currentContact, true);
        gui.setItem(slot, currentServerItem);
        slot++;

        // Add remote servers
        for (ServerRegistry.ServerInfo info : remoteServers.values()) {
            ItemStack serverItem = createServerHead(info.name, info.playerCount, -1, info.contact, false);
            gui.setItem(slot, serverItem);
            slot++;
        }

        // Add close button in bottom right (slot 53 = row 5, column 8)
        ItemStack closeButton = createCloseButton();
        gui.setItem(53, closeButton);

        // Register click listener for this inventory
        if (pluginInstance != null) {
            Bukkit.getPluginManager().registerEvents(new ServerListGUIListener(gui, player), pluginInstance);
        }

        player.openInventory(gui);
    }

    private static ItemStack createServerHead(String serverName, int playerCount, int maxPlayers,
                                              String contact, boolean isCurrentServer) {

        // Use SKELETON_SKULL if no players online, PLAYER_HEAD if players online
        Material headMaterial = playerCount > 0 ? Material.PLAYER_HEAD : Material.SKELETON_SKULL;
        ItemStack head = new ItemStack(headMaterial);
        ItemMeta meta = head.getItemMeta();

        String rawSeparator = "&7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        String coloredSeparator = MessageUtils.ColorConvert(rawSeparator);

        if (meta instanceof SkullMeta && headMaterial == Material.PLAYER_HEAD) {
            SkullMeta skullMeta = (SkullMeta) meta;
            // Display name with different color for current server
            String displayColor = isCurrentServer ? "&a" : "&e";
            String displayName = displayColor + serverName;
            if (isCurrentServer) {
                displayName += " &7(This Server)";
            }
            skullMeta.setDisplayName(MessageUtils.ColorConvert(displayName));

            List<String> lore = new ArrayList<>();
            lore.add(coloredSeparator);

            String playersValue = String.valueOf(playerCount);
            if (maxPlayers > 0) {
                playersValue = playersValue + "&7/&f" + maxPlayers;
            }
            java.util.Map<String, String> playersPlaceholders = new java.util.HashMap<>();
            playersPlaceholders.put("players", playersValue);
            lore.add(Messages.get("serverlist.players", playersPlaceholders));

            if (contact != null && !contact.isEmpty()) {
                lore.add("");
                java.util.Map<String, String> contactPlaceholders = new java.util.HashMap<>();
                contactPlaceholders.put("contact", contact);
                lore.add(Messages.get("serverlist.contact", contactPlaceholders));
            }

            lore.add("");
            lore.add(coloredSeparator);

            skullMeta.setLore(lore);
            head.setItemMeta(skullMeta);
        } else if (meta != null) {
            // Skeleton skull - no players online
            String displayColor = isCurrentServer ? "&a" : "&e";
            String displayName = displayColor + serverName + " &7(OFFLINE)";
            meta.setDisplayName(MessageUtils.ColorConvert(displayName));

            List<String> lore = new ArrayList<>();
            lore.add(coloredSeparator);
            lore.add(Messages.get("serverlist.offline"));
            lore.add("");

            java.util.Map<String, String> contactPlaceholders = new java.util.HashMap<>();
            contactPlaceholders.put("contact", (contact != null && !contact.isEmpty()) ? contact : "N/A");
            lore.add(Messages.get("serverlist.offline_contact", contactPlaceholders));
            lore.add("");
            lore.add(coloredSeparator);

            meta.setLore(lore);
            head.setItemMeta(meta);
        }

        return head;
    }

    private static ItemStack createCloseButton() {
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta meta = closeButton.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Messages.get("serverlist.close_title"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.get("serverlist.close_lore"));
            meta.setLore(lore);
            closeButton.setItemMeta(meta);
        }

        return closeButton;
    }

    // Listener for inventory events to prevent item movement and handle close button
    private static class ServerListGUIListener implements Listener {

        private final Inventory inventory;
        private final Player player;

        ServerListGUIListener(Inventory inventory, Player player) {
            this.inventory = inventory;
            this.player = player;
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            // Prevent any interaction with items in this GUI
            if (event.getInventory().equals(inventory)) {
                event.setCancelled(true);

                // Check if close button was clicked (slot 53)
                if (event.getSlot() == 53) {
                    player.closeInventory();
                    HandlerList.unregisterAll(this);
                }
            }
        }

        @EventHandler
        public void onInventoryDrag(InventoryDragEvent event) {
            // Prevent dragging items in this GUI
            if (event.getInventory().equals(inventory)) {
                event.setCancelled(true);
            }
        }
    }
}
