package com.elvarg.game.content.skill.mining;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.elvarg.game.definition.ObjectDefinition;

/**
 * Registry for mineable rocks that automatically discovers rocks from the OSRS cache.
 * Scans all ObjectDefinitions at startup and registers objects with "Mine" interaction.
 * Provides O(1) lookup for rock types by object ID.
 * 
 * @author Cache-driven Mining System
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
        
        // If no rocks were found from cache scanning, register known ones manually
        if (ROCKS.isEmpty()) {
            System.out.println("MiningRockRegistry: No rocks found from cache, registering known rocks manually...");
            registerKnownMiningRocks();
        }
        
        initialized = true;
        System.out.println("MiningRockRegistry initialized with " + ROCKS.size() + " rocks.");
    }
    
    /**
     * Manually registers known mining rocks as a fallback
     */
    private static void registerKnownMiningRocks() {
        // Register correct OSRS mining rock IDs based on user's data
        int[] clayRocks = {11362, 11363};
        int[] copperRocks = {10943, 11161};
        int[] tinRocks = {11360, 11361};
        int[] ironRocks = {11364, 11365}; // These are working!
        int[] silverRocks = {11368, 11369};
        int[] coalRocks = {11366, 11367};
        int[] goldRocks = {11370, 11371};
        int[] mithrilRocks = {11372, 11373};
        int[] adamantiteRocks = {11374, 11375};
        int[] runiteRocks = {11376, 11377};
        
        // Special mineable objects
        int[] runeEssenceRocks = {14912, 14915}; // Standard and Pure Essence
        int[] gemRocks = {11380, 11381}; // Shilo Village style
        int[] graniteRocks = {11387, 11388, 11389}; // 500g, 2kg, 5kg
        int[] sandstoneRocks = {11382, 11383, 11384, 11385}; // 1kg, 2kg, 5kg, 10kg
        
        // Register standard rocks
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
        
        // Register special mineable objects
        registerRockRange(runeEssenceRocks, MiningRockType.RUNE_ESSENCE);
        registerRockRange(gemRocks, MiningRockType.GEM_ROCK);
        registerRockRange(graniteRocks, MiningRockType.GRANITE_1); // Will use weight determination
        registerRockRange(sandstoneRocks, MiningRockType.SANDSTONE_1); // Will use weight determination
        
        System.out.println("📊 Registered OSRS mining rocks:");
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
            ROCKS.put(id, rockType);
            System.out.println("Manually registered mining rock: ID=" + id + ", Type=" + rockType.name());
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
            if (action != null && action.equalsIgnoreCase("Mine")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets the rock type for a given object ID
     * @param objectId The object ID to look up
     * @return The rock type, or null if not a mineable rock
     */
    public static MiningRockType getRockType(int objectId) {
        return ROCKS.get(objectId);
    }
    
    /**
     * Checks if an object ID is registered as a mineable rock
     * @param objectId The object ID to check
     * @return true if the object is a mineable rock
     */
    public static boolean isMineableRock(int objectId) {
        return ROCKS.containsKey(objectId);
    }
    
    /**
     * Gets all registered rock mappings
     * @return A copy of the internal rock map
     */
    public static Map<Integer, MiningRockType> getAllRocks() {
        return new HashMap<>(ROCKS);
    }
    
    /**
     * Gets the number of registered rocks
     * @return The count of registered mineable rocks
     */
    public static int getRockCount() {
        return ROCKS.size();
    }
    
    /**
     * Checks if the registry has been initialized
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Manually registers a rock (for custom rocks not in cache)
     * @param objectId The object ID
     * @param rockType The rock type
     */
    public static void registerRock(int objectId, MiningRockType rockType) {
        ROCKS.put(objectId, rockType);
    }
    
    /**
     * Gets rock types by required level
     * @param level The mining level
     * @return Array of rock types that can be mined at this level
     */
    public static MiningRockType[] getRocksByLevel(int level) {
        return ROCKS.values().stream()
                .filter(rock -> rock.getLevelRequired() <= level)
                .distinct()
                .toArray(MiningRockType[]::new);
    }
}
