package com.elvarg.game.content.skill.cache;

import com.elvarg.game.content.skill.mining.MiningRockRegistry;

/**
 * Cache loader for skill-related objects.
 * Initializes all skill object registries during server startup.
 * This ensures all gathering skills can automatically detect their objects.
 * 
 * @author Cache-driven Skill System
 */
public class CacheSkillObjectLoader {
    
    private static boolean initialized = false;
    
    /**
     * Initializes all skill object registries
     * This should be called once during server startup
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        System.out.println("Initializing cache-driven skill objects...");
        
        // Initialize the universal object scanner
        ObjectActionScanner.initialize();
        
        // Initialize mining rock registry
        MiningRockRegistry.initialize();
        
        // Future skill registries would be initialized here:
        // WoodcuttingRegistry.initialize();
        // FishingRegistry.initialize();
        // FarmingRegistry.initialize();
        
        initialized = true;
        
        System.out.println("Cache-driven skill objects initialization complete!");
        
        // Print statistics for debugging
        ObjectActionScanner.printStatistics();
        System.out.println("Mining rocks registered: " + MiningRockRegistry.getRockCount());
    }
    
    /**
     * Checks if the cache loader has been initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Reinitializes all registries (useful for development/debugging)
     */
    public static void reinitialize() {
        initialized = false;
        initialize();
    }
}
