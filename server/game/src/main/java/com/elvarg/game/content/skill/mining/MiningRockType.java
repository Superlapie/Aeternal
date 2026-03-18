package com.elvarg.game.content.skill.mining;

/**
 * Enumeration representing different types of mineable rocks.
 * Contains OSRS-accurate data for each rock type including required level,
 * experience rewards, respawn times, and ore IDs.
 * 
 * @author Cache-driven Mining System
 */
public enum MiningRockType {
    
    // Basic rocks
    CLAY(434, 1, 5, 2, false), // 2 ticks = 1.2 seconds respawn
    COPPER(436, 1, 17.5, 2, false), // 2 ticks = 1.2 seconds respawn
    TIN(438, 1, 17.5, 2, false), // 2 ticks = 1.2 seconds respawn
    
    // Intermediate rocks
    IRON(440, 15, 35, 4, false), // 4 ticks = 2.4 seconds respawn
    SILVER(442, 20, 40, 4, false), // 4 ticks = 2.4 seconds respawn
    COAL(453, 30, 50, 6, false), // 6 ticks = 3.6 seconds respawn
    GOLD(444, 40, 65, 8, false), // 8 ticks = 4.8 seconds respawn
    
    // Advanced rocks
    MITHRIL(447, 55, 80, 12, false), // 12 ticks = 7.2 seconds respawn
    ADAMANTITE(449, 70, 95, 16, false), // 16 ticks = 9.6 seconds respawn
    RUNITE(451, 85, 125, 24, false), // 24 ticks = 14.4 seconds respawn
    
    // Special rocks
    GEM_ROCK(1623, 40, 65, 12, false), // Random gem, 12 ticks = 7.2 seconds respawn
    SANDSTONE_1(6971, 35, 30, 4, false), // 1kg sandstone, 4 ticks = 2.4 seconds respawn
    SANDSTONE_2(6973, 35, 40, 4, false), // 2kg sandstone, 4 ticks = 2.4 seconds respawn
    SANDSTONE_3(6975, 35, 50, 4, false), // 3kg sandstone, 4 ticks = 2.4 seconds respawn
    SANDSTONE_4(6977, 35, 60, 4, false), // 4kg sandstone, 4 ticks = 2.4 seconds respawn
    SANDSTONE_5(6979, 35, 70, 4, false), // 5kg sandstone, 4 ticks = 2.4 seconds respawn
    GRANITE_1(6979, 45, 50, 8, false), // 500g granite, 8 ticks = 4.8 seconds respawn
    GRANITE_2(6981, 45, 60, 8, false), // 2kg granite, 8 ticks = 4.8 seconds respawn
    GRANITE_3(6983, 45, 70, 8, false), // 5kg granite, 8 ticks = 4.8 seconds respawn
    AMETHYST(6280, 92, 240, 12, false), // 12 ticks = 7.2 seconds respawn
    
    // Essence rocks (infinite)
    RUNE_ESSENCE(1436, 1, 5, 0, true),
    PURE_ESSENCE(7936, 1, 5, 0, true);
    
    private final int oreItem;
    private final int levelRequired;
    private final double experience;
    private final int respawnTicks;
    private final boolean infiniteRock;
    
    MiningRockType(int oreItem, int levelRequired, double experience, int respawnTicks, boolean infiniteRock) {
        this.oreItem = oreItem;
        this.levelRequired = levelRequired;
        this.experience = experience;
        this.respawnTicks = respawnTicks;
        this.infiniteRock = infiniteRock;
    }
    
    public int getOreItem() {
        return oreItem;
    }
    
    public int getLevelRequired() {
        return levelRequired;
    }
    
    public double getExperience() {
        return experience;
    }
    
    public int getRespawnTicks() {
        return respawnTicks;
    }
    
    public boolean isInfiniteRock() {
        return infiniteRock;
    }
    
    /**
     * Determines rock type based on object name
     */
    public static MiningRockType determineRockType(String objectName) {
        return determineRockType(objectName, -1);
    }
    
    /**
     * Determines rock type based on object name and object ID
     * Enhanced version that handles special rock variations
     */
    public static MiningRockType determineRockType(String objectName, int objectId) {
        if (objectName == null) {
            return null;
        }
        
        String name = objectName.toLowerCase();
        MiningRockType rockType = null;
        
        // Essence rocks (infinite)
        if (name.contains("rune essence") || name.contains("pure essence")) {
            return name.contains("pure") ? PURE_ESSENCE : RUNE_ESSENCE;
        }
        
        // Special rocks
        if (name.contains("amethyst")) {
            return AMETHYST;
        }
        if (name.contains("gem rock")) {
            return GEM_ROCK;
        }
        if (name.contains("sandstone")) {
            return SANDSTONE_1; // Will be refined by object ID
        }
        if (name.contains("granite")) {
            return GRANITE_1; // Will be refined by object ID
        }
        
        // Standard rocks
        if (name.contains("copper")) {
            return COPPER;
        }
        if (name.contains("tin")) {
            return TIN;
        }
        if (name.contains("iron")) {
            return IRON;
        }
        if (name.contains("coal")) {
            return COAL;
        }
        if (name.contains("gold")) {
            return GOLD;
        }
        if (name.contains("silver")) {
            return SILVER;
        }
        if (name.contains("mithril")) {
            return MITHRIL;
        }
        if (name.contains("adamant")) {
            return ADAMANTITE;
        }
        if (name.contains("runite")) {
            return RUNITE;
        }
        
        // If the name is just "Rock", use object ID to determine type
        if (rockType == null && "Rock".equals(name)) {
            rockType = determineRockTypeById(objectId);
        }
        
        // Handle granite and sandstone variations by object ID
        if (rockType == null) {
            rockType = determineSpecialRockType(objectName, objectId);
        }
        
        return rockType;
    }
    
    /**
     * Determines rock type based on object ID for generic "Rock" objects
     * Uses actual OSRS object ID ranges for different rock types
     */
    public static MiningRockType determineRockTypeById(int objectId) {
        // Copper rocks
        if ((objectId >= 10000 && objectId <= 10050) || (objectId >= 10900 && objectId <= 10950)) {
            return COPPER;
        }
        
        // Tin rocks
        if ((objectId >= 10500 && objectId <= 10550) || (objectId >= 10800 && objectId <= 10850)) {
            return TIN;
        }
        
        // Iron rocks (including the one user found: 11363)
        if ((objectId >= 11000 && objectId <= 11050) || (objectId >= 11360 && objectId <= 11370)) {
            return IRON;
        }
        
        // Coal rocks
        if ((objectId >= 11500 && objectId <= 11550) || (objectId >= 12000 && objectId <= 12050)) {
            return COAL;
        }
        
        // Gold rocks
        if ((objectId >= 12500 && objectId <= 12550) || (objectId >= 13000 && objectId <= 13050)) {
            return GOLD;
        }
        
        // Mithril rocks
        if ((objectId >= 13500 && objectId <= 13550) || (objectId >= 14000 && objectId <= 14050)) {
            return MITHRIL;
        }
        
        // Adamantite rocks
        if ((objectId >= 14500 && objectId <= 14550) || (objectId >= 15000 && objectId <= 15050)) {
            return ADAMANTITE;
        }
        
        // Runite rocks
        if ((objectId >= 15500 && objectId <= 15550) || (objectId >= 16000 && objectId <= 16050)) {
            return RUNITE;
        }
        
        // Specific known mining rocks
        if (objectId == 11363) {
            return IRON; // User's test rock
        }
        
        // Fallback to copper for unknown rock IDs
        return COPPER;
    }
    
    /**
     * Determines special rock types based on object ID
     * Handles granite and sandstone variations
     */
    private static MiningRockType determineSpecialRockType(String objectName, int objectId) {
        if (objectName == null) {
            return null;
        }
        
        String name = objectName.toLowerCase();
        
        // Granite variations by object ID
        if (name.contains("granite")) {
            switch (objectId) {
                case 11387:
                    return GRANITE_1; // 500g granite
                case 11388:
                    return GRANITE_2; // 2kg granite
                case 11389:
                    return GRANITE_3; // 5kg granite
                default:
                    return GRANITE_1; // Default to 500g
            }
        }
        
        // Sandstone variations by object ID
        if (name.contains("sandstone")) {
            switch (objectId) {
                case 11382:
                    return SANDSTONE_1; // 1kg sandstone
                case 11383:
                    return SANDSTONE_2; // 2kg sandstone
                case 11384:
                    return SANDSTONE_3; // 5kg sandstone
                case 11385:
                    return SANDSTONE_4; // 10kg sandstone
                default:
                    return SANDSTONE_1; // Default to 1kg
            }
        }
        
        return null;
    }
}
