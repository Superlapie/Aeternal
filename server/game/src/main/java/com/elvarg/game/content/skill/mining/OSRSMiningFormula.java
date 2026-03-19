package com.elvarg.game.content.skill.mining;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Skill;
import com.elvarg.util.Misc;

/**
 * OSRS-accurate mining formula calculations.
 * Based on OSRS Wiki research and community data for exact mining mechanics.
 * 
 * Calculates mining success rates, speed, and probability based on:
 * - Player's mining level
 * - Pickaxe type and speed modifier
 * - Rock type and difficulty
 * - Random chance per mining attempt
 */
public class OSRSMiningFormula {
    
    /**
     * Calculates the success chance for a mining attempt
     * Formula: Base success + Level bonus + Pickaxe bonus
     * 
     * @param player The player attempting to mine
     * @param rockType The type of rock being mined
     * @param pickaxe The pickaxe being used
     * @return Success chance as a percentage (0.0 to 1.0)
     */
    public static double calculateSuccessChance(Player player, MiningRockType rockType, PickaxeData pickaxe) {
        if (player == null || rockType == null || pickaxe == null) {
            return 0.0;
        }
        
        int playerLevel = player.getSkillManager().getCurrentLevel(Skill.MINING);
        int rockLevel = rockType.getLevelRequired();
        
        // Base success rate: 1 / (Rock level * 2)
        double baseSuccess = 1.0 / (rockLevel * 2.0);
        
        // Level bonus: (Player level - Rock level) / 100
        double levelBonus = Math.max(0, (playerLevel - rockLevel)) / 100.0;
        
        // Pickaxe bonus: (Speed modifier - 1.0)
        double pickaxeBonus = pickaxe.getSpeedModifier() - 1.0;
        
        // Final success rate
        double successRate = baseSuccess + levelBonus + pickaxeBonus;
        
        // Cap at 100% (always success)
        return Math.min(1.0, Math.max(0.0, successRate));
    }
    
    /**
     * Determines if a mining attempt is successful
     * 
     * @param player The player attempting to mine
     * @param rockType The type of rock being mined
     * @param pickaxe The pickaxe being used
     * @return true if the mining attempt succeeds, false otherwise
     */
    public static boolean isMiningSuccessful(Player player, MiningRockType rockType, PickaxeData pickaxe) {
        double successChance = calculateSuccessChance(player, rockType, pickaxe);
        return Misc.getRandomDouble() < successChance;
    }
    
    /**
     * Calculates the average mining speed in seconds per ore
     * Based on pickaxe speed and success rate
     * 
     * @param player The player attempting to mine
     * @param rockType The type of rock being mined
     * @param pickaxe The pickaxe being used
     * @return Average time in seconds to get one ore
     */
    public static double calculateMiningSpeed(Player player, MiningRockType rockType, PickaxeData pickaxe) {
        double successChance = calculateSuccessChance(player, rockType, pickaxe);
        double tickSpeed = pickaxe.getTickSpeed(); // ticks between attempts
        
        // Average attempts needed = 1 / success chance
        double averageAttempts = successChance > 0 ? 1.0 / successChance : Double.MAX_VALUE;
        
        // Average time = attempts * tick speed * 0.6 (tick duration in seconds)
        return averageAttempts * tickSpeed * 0.6;
    }
    
    /**
     * Gets the mining delay in ticks for the next mining attempt
     * 
     * @param pickaxe The pickaxe being used
     * @return Delay in ticks
     */
    public static int getMiningDelay(PickaxeData pickaxe) {
        return pickaxe != null ? pickaxe.getTickSpeed() : 5; // Default to bronze pickaxe speed
    }
    
    /**
     * Calculates the respawn time for a depleted rock
     * Returns the correct respawn time based on rock type
     * 
     * @param rockType The type of rock that was depleted
     * @return Respawn time in game ticks
     */
    public static int getRockRespawnTime(MiningRockType rockType) {
        if (rockType == null || rockType.isInfiniteRock()) {
            return 0; // Infinite rocks don't respawn
        }
        
        return rockType.getRespawnTicks();
    }
    
    /**
     * Gets the empty rock object ID for a depleted rock
     * Based on OSRS empty rock IDs from research
     * 
     * @param rockType The type of rock that was depleted
     * @return Empty rock object ID
     */
    public static int getEmptyRockId(MiningRockType rockType) {
        if (rockType == null) {
            return 450;
        }

        switch (rockType) {
            case CLAY:
            case COPPER:
            case TIN:
                return 450;
            case IRON:
            case SILVER:
            case COAL:
            case GOLD:
            case GRANITE_1:
            case GRANITE_2:
            case GRANITE_3:
            case SANDSTONE_1:
            case SANDSTONE_2:
            case SANDSTONE_3:
            case SANDSTONE_4:
            case SANDSTONE_5:
                return 451;
            case MITHRIL:
            case GEM_ROCK:
                return 452;
            case ADAMANTITE:
            case RUNITE:
            case AMETHYST:
                return 453;
            default:
                return 450;
        }
    }
    
    /**
     * Checks if a rock should be depleted based on random chance
     * Some rocks have depletion chance per ore obtained
     * 
     * @param rockType The type of rock being mined
     * @return true if the rock should be depleted, false otherwise
     */
    public static boolean shouldDepleteRock(MiningRockType rockType) {
        if (rockType == null || rockType.isInfiniteRock()) {
            return false; // Infinite rocks never deplete
        }
        
        // Most rocks deplete after one ore in OSRS
        // Some rocks like gem rocks have different depletion rates
        if (rockType == MiningRockType.GEM_ROCK) {
            return Misc.getRandomDouble() < 0.5; // 50% chance to deplete
        }
        
        // Standard rocks have depletion chance based on rock type
        // Higher-tier rocks are more likely to deplete
        double depletionChance = 0.8; // 80% base depletion chance
        
        // Adjust based on rock tier
        if (rockType == MiningRockType.CLAY || rockType == MiningRockType.COPPER || rockType == MiningRockType.TIN) {
            depletionChance = 0.6; // Lower tier rocks less likely to deplete
        } else if (rockType == MiningRockType.ADAMANTITE || rockType == MiningRockType.RUNITE) {
            depletionChance = 0.9; // Higher tier rocks more likely to deplete
        }
        
        return Misc.getRandomDouble() < depletionChance;
    }
    
    /**
     * Gets mining statistics for debugging/display purposes
     * 
     * @param player The player attempting to mine
     * @param rockType The type of rock being mined
     * @param pickaxe The pickaxe being used
     * @return Formatted string with mining statistics
     */
    public static String getMiningStats(Player player, MiningRockType rockType, PickaxeData pickaxe) {
        if (player == null || rockType == null || pickaxe == null) {
            return "Invalid mining parameters";
        }
        
        int playerLevel = player.getSkillManager().getCurrentLevel(Skill.MINING);
        double successChance = calculateSuccessChance(player, rockType, pickaxe) * 100;
        double miningSpeed = calculateMiningSpeed(player, rockType, pickaxe);
        int respawnTime = getRockRespawnTime(rockType);
        
        return String.format(
            "Mining Stats: Level %d | %s | %s | Success: %.1f%% | Speed: %.1fs/ore | Respawn: %.1fs",
            playerLevel,
            rockType.name(),
            pickaxe.name(),
            successChance,
            miningSpeed,
            respawnTime * 0.6
        );
    }
    
    /**
     * Calculates the estimated time to mine a certain number of ores
     * 
     * @param player The player attempting to mine
     * @param rockType The type of rock being mined
     * @param pickaxe The pickaxe being used
     * @param oreCount Number of ores to mine
     * @return Estimated time in seconds
     */
    public static double estimateMiningTime(Player player, MiningRockType rockType, PickaxeData pickaxe, int oreCount) {
        double oreSpeed = calculateMiningSpeed(player, rockType, pickaxe);
        return oreSpeed * oreCount;
    }
}
