package de.liebki.simplecrosschatplus.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ItemSerializer {

    /**
     * Serialize the COMPLETE ItemStack object using Java serialization.
     * The ItemStack IS the data - we serialize the entire object.
     * BukkitObjectOutputStream handles the complete object graph including all NBT.
     */
    public static String serializeItem(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Write the COMPLETE ItemStack object - Bukkit handles everything
            dataOutput.writeObject(item);
            dataOutput.close();

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserialize the COMPLETE ItemStack object.
     * We get back the exact same object with all data intact.
     * Only thing we adjust: nothing! The item is complete as-is.
     */
    public static ItemStack deserializeItem(String serialized) {
        try {
            byte[] data = Base64.getDecoder().decode(serialized);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            // Read the COMPLETE ItemStack object
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();

            return item;

        } catch (Exception e) {
            System.err.println("[SimpleCrossChatPlus] Item could not be restored due to version incompatibility or corruption.");
            return null;
        }
    }

    /**
     * Legacy method for compatibility - returns map with the item
     */
    public static Map<String, Object> deserializeItemData(String serialized) {
        try {
            ItemStack item = deserializeItem(serialized);

            Map<String, Object> itemData = new HashMap<>();
            if (item != null) {
                itemData.put("itemstack", item);
                itemData.put("type", item.getType().name());
                itemData.put("amount", item.getAmount());
            }

            return itemData;

        } catch (Exception e) {
            e.printStackTrace();
            // Return empty map on error
            return new HashMap<>();
        }
    }

    public static Material getMaterial(Map<String, Object> itemData) {
        String typeName = (String) itemData.get("type");
        try {
            return Material.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static ItemStack createFallbackItem(Material material, int amount) {
        Material fallback = material;

        if (material == null || !material.isItem() || material == Material.AIR) {
            fallback = Material.PAPER;
        }

        return new ItemStack(fallback, amount);
    }

}
