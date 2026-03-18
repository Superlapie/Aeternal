package com.elvarg.game.content.skill.mining;

import com.elvarg.game.entity.impl.object.GameObject;
import com.elvarg.game.entity.impl.object.MapObjects;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Skill;
import com.elvarg.game.task.Task;

/**
 * Task-based implementation of the mining skill loop.
 * Handles the continuous mining process including animations, success checks,
 * ore rewards, and rock depletion.
 * 
 * @author Cache-driven Mining System
 */
public class MiningTask extends Task {
    
    private final Player player;
    private final GameObject rockObject;
    private final MiningRockType rockType;
    private final PickaxeData pickaxe;
    
    public MiningTask(Player player, GameObject rockObject, MiningRockType rockType, PickaxeData pickaxe) {
        super(OSRSMiningFormula.getMiningDelay(pickaxe));
        
        this.player = player;
        this.rockObject = rockObject;
        this.rockType = rockType;
        this.pickaxe = pickaxe;
    }
    
    @Override
    public void execute() {
        // Check if player can continue mining
        if (!canContinueMining()) {
            stop();
            return;
        }
        
        // Perform mining animation
        if (pickaxe != null) {
            player.performAnimation(pickaxe.getAnimation());
        }
        
        // Check for successful mining attempt
        boolean success = OSRSMiningFormula.isMiningSuccessful(player, rockType, pickaxe);
        
        if (success) {
            mineOre();
            
            // Check if we should stop (inventory full or rock depleted)
            if (!canContinueMining()) {
                stop();
                return;
            }
        }
        
        // Random chance to stop mining (OSRS behavior)
        if (com.elvarg.util.Misc.random(100) < 10) { // 10% chance to stop each cycle
            player.getPacketSender().sendMessage("You stop mining.");
            stop();
        }
    }
    
    /**
     * Checks if the player can continue mining
     */
    private boolean canContinueMining() {
        // Check if player has inventory space
        if (player.getInventory().isFull()) {
            player.getPacketSender().sendMessage("Your inventory is full.");
            return false;
        }
        
        // Check if player still has the required pickaxe
        if (!playerHasPickaxe()) {
            player.getPacketSender().sendMessage("You don't have a pickaxe.");
            return false;
        }
        
        // Check if the rock still exists
        if (!MapObjects.exists(rockObject)) {
            player.getPacketSender().sendMessage("The rock has been depleted.");
            return false;
        }
        
        // Check if player meets level requirements
        if (player.getSkillManager().getCurrentLevel(Skill.MINING) < rockType.getLevelRequired()) {
            player.getPacketSender().sendMessage("You need a Mining level of at least " + 
                                                rockType.getLevelRequired() + " to mine this rock.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if player still has a usable pickaxe
     */
    private boolean playerHasPickaxe() {
        PickaxeData currentPickaxe = PickaxeData.getBestByMiningLevel(player);
        return currentPickaxe != null && 
               (player.getInventory().contains(currentPickaxe.getItemId()) ||
                player.getEquipment().getItems()[com.elvarg.game.model.container.impl.Equipment.WEAPON_SLOT].getId() == currentPickaxe.getItemId());
    }
    
    /**
     * Handles the ore reward logic
     */
    private void mineOre() {
        // Determine the actual ore to mine (for special rocks like sandstone/granite)
        MiningRockType actualRockType = determineActualRockType();
        
        // Add ore to inventory
        if (actualRockType.getOreItem() > 0) {
            player.getInventory().add(actualRockType.getOreItem(), 1);
            
            String oreName = getOreName(actualRockType);
            player.getPacketSender().sendMessage("You manage to mine some " + oreName + ".");
        }
        
        // Add experience
        player.getSkillManager().addExperience(Skill.MINING, (int) actualRockType.getExperience());
        
        // Check for gem drop
        checkGemDrop();
        
        // Check if rock should be depleted
        if (OSRSMiningFormula.shouldDepleteRock(rockType)) {
            // Use the proper depletion system
            MiningRespawnManager.depleteRock(rockObject, rockType);
            player.getPacketSender().sendMessage("You have exhausted this rock.");
            stop();
            return;
        }
    }
    
    /**
     * Determines the actual rock type for mining (handles sandstone/granite variations)
     */
    private MiningRockType determineActualRockType() {
        if (rockType == MiningRockType.SANDSTONE_1) {
            return MiningFormula.getSandstoneWeight();
        } else if (rockType == MiningRockType.GRANITE_1) {
            return MiningFormula.getGraniteSize();
        }
        
        return rockType;
    }
    
    /**
     * Gets the display name for an ore
     */
    private String getOreName(MiningRockType rockType) {
        switch (rockType) {
            case CLAY:
                return "clay";
            case COPPER:
                return "copper ore";
            case TIN:
                return "tin ore";
            case IRON:
                return "iron ore";
            case SILVER:
                return "silver ore";
            case COAL:
                return "coal";
            case GOLD:
                return "gold ore";
            case MITHRIL:
                return "mithril ore";
            case ADAMANTITE:
                return "adamantite ore";
            case RUNITE:
                return "runite ore";
            case GEM_ROCK:
                return "gems";
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
     * Checks for gem drops
     */
    private void checkGemDrop() {
        boolean hasGloryAmulet = hasGloryAmulet();
        
        if (MiningFormula.rollGemDrop(hasGloryAmulet)) {
            MiningGemTable gem = MiningGemTable.getRandomGem();
            player.getInventory().add(gem.getUncutId(), 1);
            player.getPacketSender().sendMessage("You find a " + gem.getName().toLowerCase() + "!");
        }
    }
    
    /**
     * Checks if player is wearing a glory amulet
     */
    private boolean hasGloryAmulet() {
        int[] gloryAmulets = {1712, 1713, 1714, 1715, 1716, 1717, 10354, 10355, 10356, 10357, 10358};
        
        for (int gloryId : gloryAmulets) {
            if (player.getEquipment().getItems()[com.elvarg.game.model.container.impl.Equipment.AMULET_SLOT].getId() == gloryId) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Determines if the rock should be depleted this mining attempt
     */
    private boolean shouldDepleteRock() {
        // Higher level rocks have higher depletion chance
        int depletionChance = Math.min(100, rockType.getLevelRequired() / 2);
        return com.elvarg.util.Misc.random(100) < depletionChance;
    }
    
    @Override
    public void stop() {
        // Reset animation
        player.performAnimation(new Animation(65535));
        
        // Call parent stop
        super.stop();
    }
}
