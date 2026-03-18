package com.elvarg.game.content.skill.mining;

import com.elvarg.util.Misc;

/**
 * Utility class containing mining success calculations based on OSRS formulas.
 * Provides accurate mining success rates that scale with player level and rock difficulty.
 * 
 * @author Cache-driven Mining System
 */
public class MiningFormula {
    
    /**
     * Calculates whether a mining attempt is successful based on player level
     * and rock level requirements. This formula approximates OSRS mining success rates.
     * 
     * @param playerLevel The player's current mining level
     * @param rockLevel The level required to mine the rock
     * @param pickaxeSpeed The speed bonus from the pickaxe (lower is better)
     * @return true if mining attempt is successful
     */
    public static boolean isSuccess(int playerLevel, int rockLevel, int pickaxeSpeed) {
        // Base success chance calculation
        // Higher player level and lower rock level = higher success chance
        double baseChance = calculateBaseSuccessChance(playerLevel, rockLevel);
        
        // Pickaxe speed modifier (faster pickaxes = better success chance)
        double pickaxeModifier = calculatePickaxeModifier(pickaxeSpeed);
        
        // Final success chance
        double finalChance = baseChance * pickaxeModifier;
        
        // Ensure chance is within valid bounds (5% to 95%)
        finalChance = Math.max(0.05, Math.min(0.95, finalChance));
        
        return Misc.getRandomDouble() < finalChance;
    }
    
    /**
     * Calculates the base success chance before pickaxe modifiers
     */
    private static double calculateBaseSuccessChance(int playerLevel, int rockLevel) {
        // OSRS formula approximation: (playerLevel * 2.0) - (rockLevel * 1.5)
        double chance = (playerLevel * 2.0) - (rockLevel * 1.5);
        chance /= 100.0;
        
        // Add minimum base chance
        chance += 0.1;
        
        return chance;
    }
    
    /**
     * Calculates the pickaxe speed modifier
     * Faster pickaxes (lower tick speed) provide better success rates
     */
    private static double calculatePickaxeModifier(int pickaxeSpeed) {
        // Base modifier is 1.0, faster pickaxes get bonus
        switch (pickaxeSpeed) {
            case 2: // Third age
                return 1.15;
            case 3: // Dragon, Rune, Infernal
                return 1.10;
            case 4: // Adamant
                return 1.05;
            case 5: // Mithril, Black
                return 1.0;
            case 6: // Steel
                return 0.95;
            case 7: // Iron
                return 0.90;
            case 8: // Bronze
                return 0.85;
            default:
                return 1.0;
        }
    }
    
    /**
     * Calculates the mining cycle time (in ticks) based on player stats and equipment
     */
    public static int calculateMiningCycleTime(int playerLevel, MiningRockType rockType, PickaxeData pickaxe) {
        if (pickaxe == null || rockType == null) {
            return 10; // Default fallback
        }
        
        // Base cycle time from pickaxe
        int cycleTime = pickaxe.getTickSpeed();
        
        // Player level bonus (higher level = slightly faster)
        int levelBonus = Math.min(3, playerLevel / 30);
        cycleTime -= levelBonus;
        
        // Rock difficulty modifier (harder rocks take longer)
        if (rockType.getLevelRequired() >= 70) {
            cycleTime += 1; // Adamantite and Runite
        } else if (rockType.getLevelRequired() >= 50) {
            cycleTime += 0; // Mithril (no additional penalty)
        }
        
        // Add random variance (0-2 ticks) for realistic timing
        cycleTime += Misc.random(3);
        
        // Ensure minimum cycle time
        return Math.max(2, cycleTime);
    }
    
    /**
     * Calculates if a gem should be dropped from mining
     */
    public static boolean rollGemDrop(boolean hasGloryAmulet) {
        // Base chance: 1/256
        double chance = 1.0 / 256.0;
        
        // Glory amulet increases chance to approximately 1/86
        if (hasGloryAmulet) {
            chance = 1.0 / 86.0;
        }
        
        return Misc.getRandomDouble() < chance;
    }
    
    /**
     * Determines the weight of sandstone when mining sandstone rocks
     */
    public static MiningRockType getSandstoneWeight() {
        // Weight distribution: 1kg (12.5%), 2kg (25%), 3kg (37.5%), 4kg (18.75%), 5kg (6.25%)
        int roll = Misc.random(100);
        
        if (roll < 13) return MiningRockType.SANDSTONE_1; // 1kg
        else if (roll < 38) return MiningRockType.SANDSTONE_2; // 2kg
        else if (roll < 75) return MiningRockType.SANDSTONE_3; // 3kg
        else if (roll < 94) return MiningRockType.SANDSTONE_4; // 4kg
        else return MiningRockType.SANDSTONE_5; // 5kg
    }
    
    /**
     * Determines the size of granite when mining granite rocks
     */
    public static MiningRockType getGraniteSize() {
        // Size distribution: 500g (60%), 2kg (30%), 5kg (10%)
        int roll = Misc.random(100);
        
        if (roll < 60) return MiningRockType.GRANITE_1; // 500g
        else if (roll < 90) return MiningRockType.GRANITE_2; // 2kg
        else return MiningRockType.GRANITE_3; // 5kg
    }
}
