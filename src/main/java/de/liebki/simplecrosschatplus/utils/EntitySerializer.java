package de.liebki.simplecrosschatplus.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class EntitySerializer {

    /**
     * EntitySnapshot - Contains COMPLETE entity data from entity.serialize()
     * We serialize THIS object - Bukkit handles extracting the data.
     */
    public static class EntitySnapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        public String entityType;
        public Map<String, Object> completeEntityData;

        public EntitySnapshot(String entityType, Map<String, Object> completeEntityData) {
            this.entityType = entityType;
            this.completeEntityData = completeEntityData;
        }
    }

    /**
     * Serialize entity by capturing properties Bukkit exposes.
     * We create a complete snapshot object and serialize THAT.
     */
    public static String serializeEntity(Entity entity) {
        try {
            // Build property map with what Bukkit gives us access to
            Map<String, Object> properties = new HashMap<>();

            // Bukkit API provides these getters - we use them
            properties.put("CustomName", entity.getCustomName());
            properties.put("CustomNameVisible", entity.isCustomNameVisible());
            properties.put("Glowing", entity.isGlowing());
            properties.put("Gravity", entity.hasGravity());
            properties.put("Invulnerable", entity.isInvulnerable());
            properties.put("Silent", entity.isSilent());
            properties.put("FireTicks", entity.getFireTicks());
            properties.put("Persistent", entity.isPersistent());

            // Living entities
            if (entity instanceof org.bukkit.entity.LivingEntity) {
                org.bukkit.entity.LivingEntity living = (org.bukkit.entity.LivingEntity) entity;
                properties.put("Health", living.getHealth());
                properties.put("AI", living.hasAI());

                // Equipment - serialize complete items
                if (living.getEquipment() != null) {
                    properties.put("Equipment.Helmet", living.getEquipment().getHelmet());
                    properties.put("Equipment.Chestplate", living.getEquipment().getChestplate());
                    properties.put("Equipment.Leggings", living.getEquipment().getLeggings());
                    properties.put("Equipment.Boots", living.getEquipment().getBoots());
                    properties.put("Equipment.ItemInMainHand", living.getEquipment().getItemInMainHand());
                    properties.put("Equipment.ItemInOffHand", living.getEquipment().getItemInOffHand());
                }
            }

            // Tameable
            if (entity instanceof org.bukkit.entity.Tameable) {
                org.bukkit.entity.Tameable tameable = (org.bukkit.entity.Tameable) entity;
                if (tameable.getOwner() != null) {
                    properties.put("Owner", tameable.getOwner().getUniqueId().toString());
                }
                properties.put("Tamed", tameable.isTamed());
            }

            // Ageable
            if (entity instanceof org.bukkit.entity.Ageable) {
                org.bukkit.entity.Ageable ageable = (org.bukkit.entity.Ageable) entity;
                properties.put("Age", ageable.getAge());
                properties.put("AgeLock", ageable.getAgeLock());
            }

            // Create complete snapshot and serialize IT
            EntitySnapshot snapshot = new EntitySnapshot(
                entity.getType().name(),
                properties
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(snapshot);
            dataOutput.close();

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserialize - get back the complete snapshot
     */
    public static EntitySnapshot deserializeEntitySnapshot(String serialized) {
        try {
            byte[] data = Base64.getDecoder().decode(serialized);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            EntitySnapshot snapshot = (EntitySnapshot) dataInput.readObject();
            dataInput.close();

            return snapshot;

        } catch (Exception e) {
            System.err.println("[SimpleCrossChatPlus] Entity could not be restored due to version incompatibility or corruption.");
            return null;
        }
    }

    /**
     * Legacy compatibility
     */
    public static Map<String, Object> deserializeEntityData(String serialized) {
        try {
            EntitySnapshot snapshot = deserializeEntitySnapshot(serialized);

            Map<String, Object> result = new HashMap<>();
            if (snapshot != null) {
                result.put("type", snapshot.entityType);
                result.put("entityData", snapshot.completeEntityData);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            // Return empty map instead of null to avoid NullPointerException
            return new HashMap<>();
        }
    }

    /**
     * Apply entity data - safely apply what we have
     */
    public static void applyEntityData(Entity entity, Map<String, Object> entityData) {
        if (entityData == null || entityData.isEmpty()) return;

        try {
            // Apply core properties
            applyIfPresent(entityData, "CustomName", v -> entity.setCustomName((String) v));
            applyIfPresent(entityData, "CustomNameVisible", v -> entity.setCustomNameVisible((Boolean) v));
            applyIfPresent(entityData, "Glowing", v -> entity.setGlowing((Boolean) v));
            applyIfPresent(entityData, "Gravity", v -> entity.setGravity((Boolean) v));
            applyIfPresent(entityData, "Invulnerable", v -> entity.setInvulnerable((Boolean) v));
            applyIfPresent(entityData, "Silent", v -> entity.setSilent((Boolean) v));
            applyIfPresent(entityData, "FireTicks", v -> entity.setFireTicks(((Number) v).intValue()));

            // Living entity
            if (entity instanceof org.bukkit.entity.LivingEntity) {
                org.bukkit.entity.LivingEntity living = (org.bukkit.entity.LivingEntity) entity;

                if (entityData.containsKey("Health")) {
                    double health = ((Number) entityData.get("Health")).doubleValue();
                    double maxHealth = living.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                    living.setHealth(Math.min(health, maxHealth));
                }

                applyIfPresent(entityData, "AI", v -> living.setAI((Boolean) v));

                // Equipment
                if (living.getEquipment() != null) {
                    applyIfPresent(entityData, "Equipment.Helmet", v -> living.getEquipment().setHelmet((org.bukkit.inventory.ItemStack) v));
                    applyIfPresent(entityData, "Equipment.Chestplate", v -> living.getEquipment().setChestplate((org.bukkit.inventory.ItemStack) v));
                    applyIfPresent(entityData, "Equipment.Leggings", v -> living.getEquipment().setLeggings((org.bukkit.inventory.ItemStack) v));
                    applyIfPresent(entityData, "Equipment.Boots", v -> living.getEquipment().setBoots((org.bukkit.inventory.ItemStack) v));
                    applyIfPresent(entityData, "Equipment.ItemInMainHand", v -> living.getEquipment().setItemInMainHand((org.bukkit.inventory.ItemStack) v));
                    applyIfPresent(entityData, "Equipment.ItemInOffHand", v -> living.getEquipment().setItemInOffHand((org.bukkit.inventory.ItemStack) v));
                }
            }

            // Tameable
            if (entity instanceof org.bukkit.entity.Tameable) {
                org.bukkit.entity.Tameable tameable = (org.bukkit.entity.Tameable) entity;
                if (entityData.containsKey("Owner")) {
                    try {
                        org.bukkit.OfflinePlayer owner = entity.getServer().getOfflinePlayer(
                            java.util.UUID.fromString((String) entityData.get("Owner"))
                        );
                        tameable.setOwner(owner);
                    } catch (Exception ignored) {}
                }
                applyIfPresent(entityData, "Tamed", v -> tameable.setTamed((Boolean) v));
            }

            // Ageable
            if (entity instanceof org.bukkit.entity.Ageable) {
                org.bukkit.entity.Ageable ageable = (org.bukkit.entity.Ageable) entity;
                applyIfPresent(entityData, "Age", v -> ageable.setAge(((Number) v).intValue()));
                applyIfPresent(entityData, "AgeLock", v -> ageable.setAgeLock((Boolean) v));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper to safely apply property if it exists
     */
    private static void applyIfPresent(Map<String, Object> data, String key, java.util.function.Consumer<Object> applier) {
        if (data.containsKey(key) && data.get(key) != null) {
            try {
                applier.accept(data.get(key));
            } catch (Exception ignored) {
                // Property can't be applied - skip it
            }
        }
    }

    public static EntityType getEntityType(Map<String, Object> entityData) {
        try {
            return EntityType.valueOf((String) entityData.get("type"));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isTransferableType(EntityType type, String tier) {
        if (tier.equals("EVERYTHING")) return true;
        if (tier.equals("ALL_ANIMALS")) {
            return type.name().contains("ANIMAL") || type == EntityType.HORSE ||
                   type == EntityType.DONKEY || type == EntityType.WOLF ||
                   type == EntityType.CAT || type == EntityType.PARROT;
        }
        return false;
    }
}
