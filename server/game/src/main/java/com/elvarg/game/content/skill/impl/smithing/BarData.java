package com.elvarg.game.content.skill.impl.smithing;

import com.elvarg.game.model.Item;

/**
 * Represents different types of smithable bars with their requirements
 * 
 * @author Smithing System
 */
public enum BarData {
    
    BRONZE(2349, new Item[]{new Item(438), new Item(436)}, 1, 6.2, new int[]{110, 111, 112, 113, 114, 115, 116, 117, 118, 119}),
    IRON(2351, new Item[]{new Item(440)}, 15, 12.5, new int[]{120, 121, 122, 123, 124, 125, 126, 127, 128, 129}),
    STEEL(2353, new Item[]{new Item(440), new Item(453, 2)}, 30, 17.5, new int[]{130, 131, 132, 133, 134, 135, 136, 137, 138, 139}),
    MITHRIL(2359, new Item[]{new Item(447), new Item(453, 4)}, 50, 30.0, new int[]{140, 141, 142, 143, 144, 145, 146, 147, 148, 149}),
    ADAMANT(2361, new Item[]{new Item(449), new Item(453, 6)}, 70, 37.5, new int[]{150, 151, 152, 153, 154, 155, 156, 157, 158, 159}),
    RUNITE(2363, new Item[]{new Item(451), new Item(453, 8)}, 85, 50.0, new int[]{160, 161, 162, 163, 164, 165, 166, 167, 168, 169}),
    
    // Special bars
    SILVER(2355, new Item[]{new Item(442)}, 20, 13.6, new int[]{170, 171, 172, 173, 174, 175, 176, 177, 178, 179}),
    GOLD(2357, new Item[]{new Item(444)}, 40, 22.5, new int[]{180, 181, 182, 183, 184, 185, 186, 187, 188, 189}),
    
    // Elemental metals
    ELEMENTAL(2892, new Item[]{new Item(2892)}, 20, 7.5, new int[]{190, 191, 192, 193, 194, 195, 196, 197, 198, 199}),
    BLURITE(9468, new Item[]{new Item(9467)}, 8, 8.0, new int[]{200, 201, 202, 203, 204, 205, 206, 207, 208, 209}),
    
    // Special smelting - Cannonballs
    CANNONBALL(2353, new Item[]{new Item(2353), new Item(4)}, 35, 25.6, new int[]{210, 211, 212, 213, 214, 215, 216, 217, 218, 219});
    
    private final int barId;
    private final Item[] requiredOres;
    private final int levelRequirement;
    private final double experienceGained;
    private final int[] interfaceChildIds;
    
    BarData(int barId, Item[] requiredOres, int levelRequirement, double experienceGained, int[] interfaceChildIds) {
        this.barId = barId;
        this.requiredOres = requiredOres;
        this.levelRequirement = levelRequirement;
        this.experienceGained = experienceGained;
        this.interfaceChildIds = interfaceChildIds;
    }
    
    public int getBarId() {
        return barId;
    }
    
    public Item[] getRequiredOres() {
        return requiredOres;
    }
    
    public int getLevelRequirement() {
        return levelRequirement;
    }
    
    public double getExperienceGained() {
        return experienceGained;
    }
    
    public int[] getInterfaceChildIds() {
        return interfaceChildIds;
    }
    
    /**
     * Gets the BarData for a specific bar item ID
     * @param barId The bar item ID
     * @return The BarData or null if not found
     */
    public static BarData forBarId(int barId) {
        for (BarData bar : values()) {
            if (bar.barId == barId) {
                return bar;
            }
        }
        return null;
    }
    
    /**
     * Gets the BarData for a specific interface child ID
     * @param childId The interface child ID
     * @return The BarData or null if not found
     */
    public static BarData forChildId(int childId) {
        for (BarData bar : values()) {
            for (int id : bar.interfaceChildIds) {
                if (id == childId) {
                    return bar;
                }
            }
        }
        return null;
    }
    
    /**
     * Checks if the player has the required ores to smith this bar
     * @param player The player to check
     * @return true if player has required ores
     */
    public boolean hasRequiredOres(com.elvarg.game.entity.impl.player.Player player) {
        for (Item ore : requiredOres) {
            if (!player.getInventory().contains(ore)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Removes the required ores from player's inventory
     * @param player The player to remove ores from
     */
    public void removeRequiredOres(com.elvarg.game.entity.impl.player.Player player) {
        for (Item ore : requiredOres) {
            player.getInventory().delete(ore.getId(), ore.getAmount());
        }
    }
    
    /**
     * Gets the total amount of bars that can be made with current inventory
     * @param player The player to check
     * @return Maximum bars that can be made
     */
    public int getMaxBars(com.elvarg.game.entity.impl.player.Player player) {
        int maxBars = Integer.MAX_VALUE;
        
        for (Item ore : requiredOres) {
            int playerAmount = player.getInventory().getAmount(ore.getId());
            int requiredAmount = ore.getAmount();
            int possibleBars = playerAmount / requiredAmount;
            
            if (possibleBars < maxBars) {
                maxBars = possibleBars;
            }
        }
        
        return maxBars;
    }
}
