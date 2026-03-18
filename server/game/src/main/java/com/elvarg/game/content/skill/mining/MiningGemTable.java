package com.elvarg.game.content.skill.mining;

/**
 * Enumeration representing different types of gems that can be obtained from mining.
 * Contains gem drop rates and item IDs for both uncut and cut gems.
 * 
 * @author Cache-driven Mining System
 */
public enum MiningGemTable {
    
    SAPPHIRE(1623, 1621, 1),
    EMERALD(1621, 1619, 1),
    RUBY(1619, 1617, 1),
    DIAMOND(1617, 1615, 1);
    
    private final int uncutId;
    private final int cutId;
    private final int weight;
    
    MiningGemTable(int uncutId, int cutId, int weight) {
        this.uncutId = uncutId;
        this.cutId = cutId;
        this.weight = weight;
    }
    
    public int getUncutId() {
        return uncutId;
    }
    
    public int getCutId() {
        return cutId;
    }
    
    public int getWeight() {
        return weight;
    }
    
    /**
     * Gets a random gem from the table
     * All gems have equal weight in the basic implementation
     */
    public static MiningGemTable getRandomGem() {
        int totalWeight = 0;
        for (MiningGemTable gem : values()) {
            totalWeight += gem.getWeight();
        }
        
        int random = com.elvarg.util.Misc.getRandom(totalWeight - 1);
        int currentWeight = 0;
        
        for (MiningGemTable gem : values()) {
            currentWeight += gem.getWeight();
            if (random < currentWeight) {
                return gem;
            }
        }
        
        return SAPPHIRE; // Fallback
    }
    
    /**
     * Gets the gem item ID (uncut by default)
     */
    public int getItemId(boolean cut) {
        return cut ? cutId : uncutId;
    }
    
    /**
     * Gets the gem name for display purposes
     */
    public String getName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
