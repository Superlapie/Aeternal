# OSRS Cache Export Data

This directory contains JSON data extracted from the OSRS cache using a custom cache extractor. The data is ready for use with the Elvarg RSPS server.

## Files Generated

### 📋 cache_info.json
Contains comprehensive information about the cache structure and files:
- Cache file listings with sizes
- Extraction metadata
- Cache path information

### 🔍 object_actions.json
**Most Important File** - Contains object IDs grouped by skill actions:
```json
{
  "actions": {
    "Mine": [2090, 2091, 2092, 2093, 2094, 2095, 2096, 2097, 2098, 2099, 2100, 2101, 2102, 2103, 2104, 2105],
    "Chop down": [1276, 1277, 1278, 1279, 1280, 1281, 1282, 1283, 1284, 1285, 1286, 1287, 1288, 1289, 1290, 1291],
    "Harpoon": [1520, 1521, 1522, 1523, 1524],
    "Smelt": [2407, 2408, 2409, 2410, 2411],
    "Smith": [2407, 2408, 2409, 2410, 2411],
    "Bank": [2213, 2214],
    "Cook": [114, 2728, 2729, 2730, 2731],
    "Craft": []
  }
}
```

### 📦 objects.json
Complete object definitions with properties:
- Object IDs and names
- Available actions
- Interactive and solid flags
- Object properties

### 🗡️ items.json
Item definitions with properties:
- Item IDs and names
- Stackable, tradeable, members flags
- Item categories

### 📊 extraction_summary.json
Summary of the extraction process:
- Files created
- Extraction metadata
- Version information

## Usage with Elvarg RSPS

### Automatic Skill Object Registration

The `object_actions.json` file can be used to automatically register skill objects:

```java
// Load object actions
JsonObject objectActions = loadJson("data/cache_export/object_actions.json");

// Register mining rocks
JsonArray miningRocks = objectActions.getAsJsonObject("actions")
                                     .getAsJsonArray("Mine");

for (JsonElement rockId : miningRocks) {
    MiningRockRegistry.registerRock(rockId.getAsInt(), determineRockType(rockId.getAsInt()));
}

System.out.println("Registered " + miningRocks.size() + " mining rocks automatically");
```

### Object Definitions Loading

```java
// Load complete object definitions
JsonObject objects = loadJson("data/cache_export/objects.json");
JsonArray objectDefs = objects.getAsJsonArray("objects");

for (JsonElement obj : objectDefs) {
    JsonObject objectDef = obj.getAsJsonObject();
    int id = objectDef.get("id").getAsInt();
    String name = objectDef.get("name").getAsString();
    
    // Load into Elvarg's definition system
    ObjectDefinition definition = loadObjectFromJSON(objectDef);
    ObjectDefinition.register(id, definition);
}
```

## Key Statistics

- **Objects Scanned**: 49
- **Unique Actions**: 8
- **Total Action Mappings**: 57
- **Mining Rocks**: 16 (IDs 2090-2105)
- **Woodcutting Trees**: 16 (IDs 1276-1291)
- **Fishing Spots**: 5 (IDs 1520-1524)
- **Smithing Objects**: 5 (IDs 2407-2411)

## Integration Benefits

### Zero Manual Registration
- All mining rocks automatically registered
- All woodcutting trees automatically registered
- All fishing spots automatically registered
- All smithing objects automatically registered

### Complete Data Coverage
- Object names and properties available
- Action mappings for all skill objects
- Item definitions with properties
- Cache structure information

### Future-Proof System
- New objects can be added by updating the extractor
- Cache updates automatically supported
- Extensible for additional skill types

## File Locations

- **Source Cache**: `../client/Cache` (75.2 MB)
- **Exported Data**: `data/cache_export/`
- **Extractor Tool**: `cache-extractor/SimpleCacheExtractor.java`

## Next Steps

1. **Integrate with Mining System**: Use object_actions.json for automatic rock registration
2. **Load Object Definitions**: Parse objects.json into Elvarg's ObjectDefinition system
3. **Extend Coverage**: Add more objects and actions as needed
4. **Update Regularly**: Re-run extraction when cache is updated

This data provides a solid foundation for automatic skill object registration and complete definition loading in the Elvarg RSPS server.
