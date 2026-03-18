package com.elvarg.game.content.skill.mining;

import com.elvarg.game.entity.impl.object.GameObject;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Skill;

/**
 * Main entry point for the Mining skill system.
 * Handles the initiation of mining activities and integrates with the cache-driven
 * rock detection system to automatically support all mineable rocks.
 * 
 * @author Cache-driven Mining System
 */
public class Mining {
    
    /**
     * Starts mining a rock
     * @param player The player attempting to mine
     * @param object The rock object being mined
     * @return true if mining was started, false otherwise
     */
    public static boolean startMining(Player player, GameObject object) {
        // Get the rock type from our cache-driven registry
        MiningRockType rockType = MiningRockRegistry.getRockType(object.getId());
        
        if (rockType == null) {
            return false; // Not a mineable rock
        }
        
        // Check if player meets the mining level requirement
        if (player.getSkillManager().getCurrentLevel(Skill.MINING) < rockType.getLevelRequired()) {
            player.getPacketSender().sendMessage("You need a Mining level of at least " + 
                                                rockType.getLevelRequired() + " to mine this rock.");
            return false;
        }
        
        // Get the best pickaxe the player can use
        PickaxeData pickaxe = PickaxeData.getBest(player);
        
        if (pickaxe == null) {
            player.getPacketSender().sendMessage("You need a pickaxe to mine this rock.");
            return false;
        }
        
        // Check if player meets the pickaxe requirements
        if (player.getSkillManager().getCurrentLevel(Skill.MINING) < pickaxe.getMiningLevel()) {
            player.getPacketSender().sendMessage("You need a Mining level of at least " + 
                                                pickaxe.getMiningLevel() + " to use this pickaxe.");
            return false;
        }
        
        if (player.getSkillManager().getCurrentLevel(Skill.ATTACK) < pickaxe.getAttackLevel()) {
            player.getPacketSender().sendMessage("You need an Attack level of at least " + 
                                                pickaxe.getAttackLevel() + " to use this pickaxe.");
            return false;
        }
        
        // Check if inventory is full
        if (player.getInventory().isFull()) {
            player.getPacketSender().sendMessage("Your inventory is full.");
            return false;
        }
        
        // Stop any existing skill activity
        player.getSkillManager().stopSkillable();
        
        // Start the mining task
        MiningTask miningTask = new MiningTask(player, object, rockType, pickaxe);
        player.getSkillManager().startSkillable(new MiningSkillable(miningTask));
        
        return true;
    }
    
    /**
     * Handles prospecting a rock
     * @param player The player prospecting
     * @param objectId The object ID being prospected
     * @return true if prospecting was handled
     */
    public static boolean prospectRock(Player player, int objectId) {
        return ProspectService.prospectRock(player, objectId);
    }
    
    /**
     * Checks if an object is a mineable rock
     * @param objectId The object ID to check
     * @return true if the object is a mineable rock
     */
    public static boolean isMineableRock(int objectId) {
        return MiningRockRegistry.isMineableRock(objectId);
    }
    
    /**
     * Gets information about a mineable rock
     * @param objectId The object ID
     * @return Rock information or null if not mineable
     */
    public static String getRockInfo(int objectId) {
        MiningRockType rockType = MiningRockRegistry.getRockType(objectId);
        
        if (rockType == null) {
            return "This is not a mineable rock.";
        }
        
        return ProspectService.getProspectInfo(rockType);
    }
    
    /**
     * Skillable adapter for the mining task
     */
    private static class MiningSkillable implements com.elvarg.game.content.skill.skillable.Skillable {
        
        private final MiningTask miningTask;
        
        public MiningSkillable(MiningTask miningTask) {
            this.miningTask = miningTask;
        }
        
        @Override
        public void start(Player player) {
            // Submit the mining task
            com.elvarg.game.task.TaskManager.submit(miningTask);
        }
        
        @Override
        public void cancel(Player player) {
            miningTask.stop();
        }
        
        @Override
        public boolean hasRequirements(Player player) {
            // Requirements are checked before starting, so always return true here
            return true;
        }
        
        @Override
        public void startAnimationLoop(Player player) {
            // Animation is handled by the mining task
        }
        
        @Override
        public int cyclesRequired(Player player) {
            return 1; // Mining is instant with proper timing
        }
        
        @Override
        public void onCycle(Player player) {
            // Handled by the mining task
        }
        
        @Override
        public void finishedCycle(Player player) {
            // Handled by the mining task
        }
    }
}
