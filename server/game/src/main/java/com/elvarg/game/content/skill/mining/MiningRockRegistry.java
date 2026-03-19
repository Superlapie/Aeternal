package com.elvarg.game.content.skill.mining;

import java.util.HashMap;
import java.util.Map;

import com.elvarg.game.definition.ObjectDefinition;

/**
 * Registry for mineable rocks that automatically discovers rocks from the OSRS cache.
 * Scans all ObjectDefinitions at startup and registers objects with "Mine" interaction.
 * Provides O(1) lookup for rock types by object ID.
 */
public class MiningRockRegistry {

    private static final Map<Integer, MiningRockType> ROCKS = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Initializes the registry by scanning all ObjectDefinitions for mineable rocks.
     * This method should be called once during server startup.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        initializeInternal();
    }

    /**
     * Refreshes the registry when object definitions were not ready at first init.
     */
    public static synchronized void refreshIfNeeded() {
        if (!initialized || ROCKS.isEmpty()) {
            initialized = false;
            initializeInternal();
        }
    }

    private static synchronized void initializeInternal() {
        if (initialized) {
            return;
        }

        ROCKS.clear();

        System.out.println("MiningRockRegistry: Starting initialization...");

        // First try to scan the cache
        if (ObjectDefinition.totalObjects > 0) {
            System.out.println("MiningRockRegistry: Scanning " + ObjectDefinition.totalObjects + " objects...");

            // Scan all object definitions in the cache
            for (int i = 0; i < ObjectDefinition.totalObjects; i++) {
                ObjectDefinition def = ObjectDefinition.forId(i);

                if (def == null || def.getName() == null) {
                    continue;
                }

                // Check if object has "Mine" interaction
                if (hasMineAction(def)) {
                    MiningRockType rockType = MiningRockType.determineRockType(def.getName(), i);

                    if (rockType != null) {
                        ROCKS.put(i, rockType);

                        // Debug logging (can be disabled in production)
                        System.out.println("Registered mining rock: ID=" + i +
                                ", Name=" + def.getName() +
                                ", Type=" + rockType.name());
                    }
                }
            }
        }

        int scannedCount = ROCKS.size();

        // Always merge known supported rock ids after scanning.
        // This cache exposes only a handful of native Mine objects via definitions,
        // but injected/world-visible ore rocks still need hard registration.
        registerKnownMiningRocks();

        initialized = true;
        System.out.println("MiningRockRegistry initialized with " + ROCKS.size() + " rocks (" + scannedCount + " from scan).");
    }

    /**
     * Manually registers known mining rocks as a fallback
     */
    private static void registerKnownMiningRocks() {
        System.out.println("MiningRockRegistry: Registering known mining rocks...");
        int[] clayRocks = {11362, 11363};
        int[] copperRocks = {7453, 7454, 10943, 11161};
        int[] tinRocks = {7486, 11360, 11361};
        int[] ironRocks = {7455, 7488, 11364, 11365};
        int[] silverRocks = {7457, 11368, 11369};
        int[] coalRocks = {7456, 11366, 11367};
        int[] goldRocks = {9720, 9721, 9722, 11951, 11183, 11184, 11185, 2099, 11370, 11371};
        int[] mithrilRocks = {7459, 7492, 11372, 11373};
        int[] adamantiteRocks = {7460, 11374, 11375};
        int[] runiteRocks = {7461, 14859, 4860, 2106, 2107, 11376, 11377};

        int[] runeEssenceRocks = {14912, 14915};
        int[] gemRocks = {11380, 11381};
        int[] graniteRocks = {11387, 11388, 11389};
        int[] sandstoneRocks = {11382, 11383, 11384, 11385};
        registerRockRange(clayRocks, MiningRockType.CLAY);
        registerRockRange(copperRocks, MiningRockType.COPPER);
        registerRockRange(tinRocks, MiningRockType.TIN);
        registerRockRange(ironRocks, MiningRockType.IRON);
        registerRockRange(silverRocks, MiningRockType.SILVER);
        registerRockRange(coalRocks, MiningRockType.COAL);
        registerRockRange(goldRocks, MiningRockType.GOLD);
        registerRockRange(mithrilRocks, MiningRockType.MITHRIL);
        registerRockRange(adamantiteRocks, MiningRockType.ADAMANTITE);
        registerRockRange(runiteRocks, MiningRockType.RUNITE);

        registerRockRange(runeEssenceRocks, MiningRockType.RUNE_ESSENCE);
        registerRockRange(gemRocks, MiningRockType.GEM_ROCK);
        registerRockRange(graniteRocks, MiningRockType.GRANITE_1);
        registerRockRange(sandstoneRocks, MiningRockType.SANDSTONE_1);

        System.out.println("Registered OSRS mining rocks:");
        System.out.println("   Clay: " + java.util.Arrays.toString(clayRocks));
        System.out.println("   Copper: " + java.util.Arrays.toString(copperRocks));
        System.out.println("   Tin: " + java.util.Arrays.toString(tinRocks));
        System.out.println("   Iron: " + java.util.Arrays.toString(ironRocks));
        System.out.println("   Silver: " + java.util.Arrays.toString(silverRocks));
        System.out.println("   Coal: " + java.util.Arrays.toString(coalRocks));
        System.out.println("   Gold: " + java.util.Arrays.toString(goldRocks));
        System.out.println("   Mithril: " + java.util.Arrays.toString(mithrilRocks));
        System.out.println("   Adamantite: " + java.util.Arrays.toString(adamantiteRocks));
        System.out.println("   Runite: " + java.util.Arrays.toString(runiteRocks));
        System.out.println("   Empty Rock: 11390 (depleted state)");
        System.out.println("   Empty Rock: 11391 (depleted state)");
        System.out.println("   Special Objects:");
        System.out.println("   Rune Essence: " + java.util.Arrays.toString(runeEssenceRocks));
        System.out.println("   Gem Rocks: " + java.util.Arrays.toString(gemRocks));
        System.out.println("   Granite: " + java.util.Arrays.toString(graniteRocks));
        System.out.println("   Sandstone: " + java.util.Arrays.toString(sandstoneRocks));
    }

    /**
     * Registers a range of rock IDs with the specified type
     */
    private static void registerRockRange(int[] rockIds, MiningRockType rockType) {
        for (int id : rockIds) {
            MiningRockType previous = ROCKS.put(id, rockType);
            if (previous == null || previous != rockType) {
                System.out.println("Manually registered mining rock: ID=" + id + ", Type=" + rockType.name());
            }
        }
    }

    /**
     * Checks if an ObjectDefinition has a "Mine" action
     */
    private static boolean hasMineAction(ObjectDefinition def) {
        if (def.interactions == null) {
            return false;
        }

        for (String action : def.interactions) {
            if (action == null) {
                continue;
            }
            if (action.equalsIgnoreCase("Mine") || action.toLowerCase().contains("mine")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the rock type for a given object ID
     */
    public static MiningRockType getRockType(int objectId) {
        return ROCKS.get(objectId);
    }

    /**
     * Checks if an object ID is registered as a mineable rock
     */
    public static boolean isMineableRock(int objectId) {
        return ROCKS.containsKey(objectId);
    }

    /**
     * Gets all registered rock mappings
     */
    public static Map<Integer, MiningRockType> getAllRocks() {
        return new HashMap<>(ROCKS);
    }

    /**
     * Gets the number of registered rocks
     */
    public static int getRockCount() {
        return ROCKS.size();
    }

    /**
     * Checks if the registry has been initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Manually registers a rock (for custom rocks not in cache)
     */
    public static void registerRock(int objectId, MiningRockType rockType) {
        ROCKS.put(objectId, rockType);
    }

    /**
     * Gets rock types by required level
     */
    public static MiningRockType[] getRocksByLevel(int level) {
        return ROCKS.values().stream()
                .filter(rock -> rock.getLevelRequired() <= level)
                .distinct()
                .toArray(MiningRockType[]::new);
    }
}
