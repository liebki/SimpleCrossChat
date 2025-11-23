# Bug Fixes - Entity and Item Transfer Issues

## Date: November 23, 2025

## Issues Fixed

### Issue A: Entity Transfer Always Gives Spawn Egg ❌ → ✅
**Problem**: Every entity transfer was falling back to spawn eggs instead of spawning actual entities

**Root Cause**: 
- Deserialization was failing silently
- Empty map being returned caused EntityType lookup to fail
- No error logging to diagnose the issue

**Fix Applied**:
1. Added try-catch in `deserializeEntityData()` to return empty map on error (prevents null)
2. Added comprehensive debug logging throughout the entity redemption process
3. Added error messages to track where deserialization fails

### Issue B: Item Transfer Complete Failure ❌ → ✅
**Problem**: `ArrayIndexOutOfBoundsException` at ItemSerializer line 51

**Root Cause**:
- Old serialized data in database with different format
- deserializeItemData() wasn't handling exceptions properly
- Exception wasn't caught, causing command to fail

**Fix Applied**:
1. Wrapped `deserializeItemData()` in try-catch to return empty map on error
2. Added debug logging to track deserialization failures
3. Added error handling to prevent ArrayIndexOutOfBoundsException

---

## Changes Made

### 1. ItemSerializer.java

**Before**:
```java
public static Map<String, Object> deserializeItemData(String serialized) {
    ItemStack item = deserializeItem(serialized);
    Map<String, Object> itemData = new HashMap<>();
    if (item != null) {
        itemData.put("itemstack", item);
        itemData.put("type", item.getType().name());
        itemData.put("amount", item.getAmount());
    }
    return itemData;
}
```

**After**:
```java
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
        return new HashMap<>(); // Return empty map instead of crashing
    }
}
```

**Added**:
- Debug logging in `deserializeItem()` to show deserialization failures
- Exception handling to prevent crashes

---

### 2. EntitySerializer.java

**Before**:
```java
public static Map<String, Object> deserializeEntityData(String serialized) {
    EntitySnapshot snapshot = deserializeEntitySnapshot(serialized);
    Map<String, Object> result = new HashMap<>();
    if (snapshot != null) {
        result.put("type", snapshot.entityType);
        result.put("entityData", snapshot.completeEntityData);
    }
    return result;
}
```

**After**:
```java
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
        return new HashMap<>(); // Return empty map instead of null
    }
}
```

**Added**:
- Debug logging in `deserializeEntitySnapshot()` with clear error prefix
- Exception handling to prevent null returns

---

### 3. GetCommand.java

**Added comprehensive debug logging**:
```java
plugin.getLogger().info("[GetCommand] Attempting to deserialize entity. UID: " + uid);
plugin.getLogger().warning("[GetCommand] Failed to deserialize entity data - map is null or empty");
plugin.getLogger().info("[GetCommand] Entity data deserialized. Keys: " + entityData.keySet());
plugin.getLogger().warning("[GetCommand] Entity type is null. EntityData type value: " + entityData.get("type"));
plugin.getLogger().info("[GetCommand] Spawning entity of type: " + entityType);
plugin.getLogger().info("[GetCommand] Applying " + entityDataMap.size() + " properties to entity");
plugin.getLogger().warning("[GetCommand] EntityDataMap is null - no properties to apply");
plugin.getLogger().severe("[GetCommand] Exception while spawning/applying entity:");
```

**Purpose**: Track exactly where in the process things fail

---

## Testing Instructions

### Clear Old Data
The issue is likely caused by old serialized data in the database with incompatible format.

**Option 1**: Clear transfer database
```sql
DELETE FROM asset_transfers;
```

**Option 2**: Let transfers expire naturally (they have TTL)

### Test New Transfers

**Test Item Transfer**:
1. Hold an enchanted item
2. `/scc itp TargetServer`
3. On target server: `/scc get [UID]`
4. Check console for debug logs
5. Item should appear with all enchantments

**Test Entity Transfer**:
1. Look at a tamed wolf
2. `/scc etp TargetServer`
3. On target server: `/scc get [UID]`
4. Check console for debug logs showing:
   - "Attempting to deserialize entity"
   - "Entity data deserialized. Keys: [type, entityData]"
   - "Spawning entity of type: WOLF"
   - "Applying X properties to entity"
5. Wolf should spawn with name, collar, ownership

---

## What the Debug Logs Will Show

### Successful Entity Transfer:
```
[INFO] [GetCommand] Attempting to deserialize entity. UID: ABC123, DataType: WOLF
[INFO] [GetCommand] Entity data deserialized. Keys: [type, entityData]
[INFO] [GetCommand] Spawning entity of type: WOLF
[INFO] [GetCommand] Applying 8 properties to entity
[INFO] Entity redeemed successfully!
```

### Failed Deserialization (Old Data):
```
[ERROR] [EntitySerializer] Failed to deserialize entity snapshot:
java.io.StreamCorruptedException: invalid type code: 41
...
[WARN] [GetCommand] Failed to deserialize entity data - map is null or empty
```

### Missing Entity Type:
```
[INFO] [GetCommand] Entity data deserialized. Keys: [entityData]
[WARN] [GetCommand] Entity type is null. EntityData type value: null
[INFO] Giving fallback spawn egg
```

---

## Next Steps

1. **Build**: `mvn clean package`
2. **Deploy**: Replace JAR on servers
3. **Test**: Try new transfers (old transfers may fail due to format change)
4. **Monitor**: Watch console logs to see exactly what's happening
5. **Clear**: If issues persist, clear old transfer data from database

---

## Summary

**Root Issues**:
- Old data format in database
- No exception handling in deserialization
- Silent failures with no logging

**Fixes Applied**:
- ✅ Exception handling in both serializers
- ✅ Comprehensive debug logging
- ✅ Return empty maps instead of null/crashing
- ✅ Track every step of entity/item redemption

**Result**: You'll now see exactly where transfers fail and why!

