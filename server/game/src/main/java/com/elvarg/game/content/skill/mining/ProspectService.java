package com.elvarg.game.content.skill.mining;

import com.elvarg.game.entity.impl.player.Player;

/**
 * Service for handling the prospect action on mining rocks.
 * Provides rock identification messages and level requirement checks.
 * 
 * @author Cache-driven Mining System
 */
public class ProspectService {
    
    /**
     * Handles prospecting a rock
     * @param player The player prospecting
     * @param objectId The object ID being prospected
     * @return true if prospecting was handled, false if not a mineable rock
     */
    public static boolean prospectRock(Player player, int objectId) {
        MiningRockType rockType = MiningRockRegistry.getRockType(objectId);
        
        if (rockType == null) {
            return false; // Not a mineable rock
        }
        
        // Check if player meets the requirements to identify this rock
        int playerLevel = player.getSkillManager().getCurrentLevel(com.elvarg.game.model.Skill.MINING);
        int requiredLevel = rockType.getLevelRequired();
        
        // Send appropriate message based on player's level
        if (playerLevel < requiredLevel) {
            player.getPacketSender().sendMessage("You need a Mining level of at least " + requiredLevel + 
                                                " to prospect this rock.");
        } else {
            String rockName = getRockName(rockType);
            player.getPacketSender().sendMessage("This rock contains " + rockName + " ore.");
            
            // Add additional information for special rocks
            if (rockType == MiningRockType.GEM_ROCK) {
                player.getPacketSender().sendMessage("You may find various gems when mining this rock.");
            } else if (rockType == MiningRockType.AMETHYST) {
                player.getPacketSender().sendMessage("This crystal contains amethyst.");
            } else if (rockType.isInfiniteRock()) {
                player.getPacketSender().sendMessage("This rock has an unlimited supply of essence.");
            }
        }
        
        return true;
    }
    
    /**
     * Gets the display name for a rock type
     * @param rockType The rock type
     * @return The formatted rock name
     */
    private static String getRockName(MiningRockType rockType) {
        switch (rockType) {
            case CLAY:
                return "clay";
            case COPPER:
                return "copper";
            case TIN:
                return "tin";
            case IRON:
                return "iron";
            case SILVER:
                return "silver";
            case COAL:
                return "coal";
            case GOLD:
                return "gold";
            case MITHRIL:
                return "mithril";
            case ADAMANTITE:
                return "adamantite";
            case RUNITE:
                return "runite";
            case GEM_ROCK:
                return "gem";
            case AMETHYST:
                return "amethyst";
            case RUNE_ESSENCE:
                return "rune essence";
            case PURE_ESSENCE:
                return "pure essence";
            case SANDSTONE_1:
            case SANDSTONE_2:
            case SANDSTONE_3:
            case SANDSTONE_4:
            case SANDSTONE_5:
                return "sandstone";
            case GRANITE_1:
            case GRANITE_2:
            case GRANITE_3:
                return "granite";
            default:
                return rockType.name().toLowerCase().replace("_", " ");
        }
    }
    
    /**
     * Checks if a player can prospect a specific rock type
     * @param player The player to check
     * @param rockType The rock type to check
     * @return true if the player can prospect this rock
     */
    public static boolean canProspect(Player player, MiningRockType rockType) {
        if (rockType == null) {
            return false;
        }
        
        int playerLevel = player.getSkillManager().getCurrentLevel(com.elvarg.game.model.Skill.MINING);
        return playerLevel >= rockType.getLevelRequired();
    }
    
    /**
     * Gets prospect information for a rock type
     * @param rockType The rock type
     * @return Formatted prospect information
     */
    public static String getProspectInfo(MiningRockType rockType) {
        if (rockType == null) {
            return "Unknown rock type.";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("This rock contains ").append(getRockName(rockType)).append(" ore.");
        info.append(" Requires Mining level ").append(rockType.getLevelRequired()).append(".");
        info.append(" Awards ").append(rockType.getExperience()).append(" experience.");
        
        if (rockType.isInfiniteRock()) {
            info.append(" This rock has an unlimited supply.");
        }
        
        return info.toString();
    }
}
