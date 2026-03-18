# OSRS Cache Extractor

A standalone Java application that extracts OSRS cache data to JSON format for RSPS development using the OpenRS2 library.

## Features

- **Complete Cache Extraction**: Objects, items, NPCs, animations, and models
- **Object Action Scanning**: Automatically groups objects by skill actions
- **JSON Format**: Clean, structured JSON output for easy parsing
- **Auto Cache Discovery**: Automatically finds OSRS cache locations
- **Flexible Export**: Select specific data types to export
- **Progress Reporting**: Real-time extraction progress

## Output Files

- `objects.json` - Complete object definitions with actions, models, and properties
- `items.json` - Item definitions with stats, models, and equipment data
- `npcs.json` - NPC definitions with combat stats, animations, and models
- `animations.json` - Animation sequences with frame data
- `models.json` - Model metadata and geometry information
- `object_actions.json` - Objects grouped by skill actions (Mine, Chop down, Net, etc.)

## Installation

1. Clone or download the cache extractor
2. Build with Gradle:
   ```bash
   ./gradlew build
   ```

## Usage

### Basic Usage
```bash
java -jar cache-extractor.jar
```

### Advanced Usage
```bash
# Extract all data types with object actions
java -jar cache-extractor.jar -c "C:/Users/user/jagexcache/oldschool/LIVE" -o "./export" -a

# Extract only objects and items
java -jar cache-extractor.jar -t "objects,items" -o "./export"

# Verbose mode with custom cache path
java -jar cache-extractor.jar -c "./cache" -o "./export" -v
```

### Command Line Options

| Option | Description | Default |
|--------|-------------|---------|
| `-c, --cache <PATH>` | Cache directory path (auto-discovered if not specified) | Auto-discovered |
| `-o, --output <PATH>` | Output directory path | `./export` |
| `-t, --types <TYPES>` | Export types (comma-separated): objects,items,npcs,animations,models | All types |
| `-a, --actions` | Include object actions scan | `true` |
| `-v, --verbose` | Verbose output | `false` |
| `-f, --format <FORMAT>` | Output format (json, csv) | `json` |
| `-h, --help` | Show help | - |

## Cache Locations

The tool automatically searches for OSRS cache in these locations:
- `~/jagexcache/oldschool/LIVE`
- `~/.jagex_cache_32/oldschool/LIVE`
- `~/.jagex_cache_64/oldschool/LIVE`
- `./cache`
- `../cache`
- `./repository/cache`

## JSON Structure Examples

### objects.json
```json
{
  "objects": {
    "2090": {
      "id": 2090,
      "name": "Copper rock",
      "actions": ["Mine", "Prospect", null, null, null],
      "models": [5322],
      "sizeX": 1,
      "sizeY": 1,
      "interactive": true,
      "solid": true
    }
  }
}
```

### object_actions.json
```json
{
  "actions": {
    "Mine": [2090, 2091, 2092, 2093, 2094],
    "Chop down": [1276, 1277, 1278, 1279, 1280],
    "Net": [1520, 1521, 1522, 1523, 1524],
    "Harpoon": [1522, 1523, 1524, 1525, 1526]
  }
}
```

## Integration with Elvarg RSPS

The extracted JSON files can be used to automatically register skill interactions:

```java
// Load object actions
JsonObject objectActions = loadJson("object_actions.json");

// Register mining rocks
JsonArray miningRocks = objectActions.getAsJsonArray("actions")
                                     .getAsJsonObject("Mine")
                                     .getAsJsonArray("rocks");

for (JsonElement rockId : miningRocks) {
    MiningRockRegistry.registerRock(rockId.getAsInt(), MiningRockType.COPPER);
}
```

## Requirements

- Java 17 or higher
- OSRS cache files (from official client or RuneLite)

## Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd cache-extractor

# Build the project
./gradlew build

# Run tests
./gradlew test

# Create fat JAR
./gradlew fatJar
```

## Troubleshooting

### Cache Not Found
Ensure you have run the OSRS client or RuneLite at least once to generate cache files. You can also specify the cache path manually with the `-c` option.

### Permission Errors
Make sure you have read permissions for the cache directory and write permissions for the output directory.

### Memory Issues
For large caches, you may need to increase JVM memory:
```bash
java -Xmx2G -jar cache-extractor.jar
```

## License

This project is licensed under the MIT License.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Support

For issues and questions, please create an issue on the repository.
