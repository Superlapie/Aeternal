package com.elvarg.game.content.skill.mining;

import com.elvarg.game.model.Animation;

/**
 * Enumeration representing different types of pickaxes available for mining.
 * Contains OSRS-accurate data for each pickaxe including required level,
 * animation ID, mining speed (tick delay), and speed modifier for success calculations.
 * 
 * @author Cache-driven Mining System
 */
public enum PickaxeData {
    
    // OSRS-accurate pickaxe data with correct mining speeds and animations
    BRONZE(1265, 1, 1, new Animation(625), 5, 1.0),      // 5 ticks = 3.0s, 1.0x speed modifier
    IRON(1267, 1, 1, new Animation(626), 4, 1.1),         // 4 ticks = 2.4s, 1.1x speed modifier
    STEEL(1269, 6, 5, new Animation(627), 3, 1.2),         // 3 ticks = 1.8s, 1.2x speed modifier
    BLACK(12297, 11, 10, new Animation(3873), 3, 1.25),    // 3 ticks = 1.8s, 1.25x speed modifier
    MITHRIL(1273, 21, 20, new Animation(629), 2, 1.3),      // 2 ticks = 1.2s, 1.3x speed modifier
    ADAMANT(1271, 31, 30, new Animation(628), 2, 1.35),     // 2 ticks = 1.2s, 1.35x speed modifier
    RUNE(1275, 41, 40, new Animation(624), 1, 1.4),         // 1 tick = 0.6s, 1.4x speed modifier
    DRAGON(11920, 61, 60, new Animation(7139), 1, 1.5),     // 1 tick = 0.6s, 1.5x speed modifier
    THIRD_AGE(20014, 61, 60, new Animation(7139), 1, 1.7),  // 1 tick = 0.6s, 1.7x speed modifier
    INFERNAL(13243, 61, 60, new Animation(7139), 1, 1.8),    // 1 tick = 0.6s, 1.8x speed modifier
    CRYSTAL(23681, 71, 70, new Animation(7284), 1, 1.6);     // 1 tick = 0.6s, 1.6x speed modifier
    
    private final int itemId;
    private final int miningLevel;
    private final int attackLevel;
    private final Animation animation;
    private final int tickSpeed;
    private final double speedModifier;
    
    PickaxeData(int itemId, int miningLevel, int attackLevel, Animation animation, int tickSpeed, double speedModifier) {
        this.itemId = itemId;
        this.miningLevel = miningLevel;
        this.attackLevel = attackLevel;
        this.animation = animation;
        this.tickSpeed = tickSpeed;
        this.speedModifier = speedModifier;
    }
    
    public int getItemId() {
        return itemId;
    }
    
    public int getMiningLevel() {
        return miningLevel;
    }
    
    public int getAttackLevel() {
        return attackLevel;
    }
    
    public Animation getAnimation() {
        return animation;
    }
    
    public int getTickSpeed() {
        return tickSpeed;
    }
    
    public double getSpeedModifier() {
        return speedModifier;
    }
    
    /**
     * Gets the best pickaxe a player can use based on their stats and inventory
     */
    public static PickaxeData getBest(com.elvarg.game.entity.impl.player.Player player) {
        PickaxeData best = null;
        
        for (PickaxeData pickaxe : values()) {
            // Check if player has this pickaxe
            boolean hasPickaxe = player.getInventory().contains(pickaxe.getItemId()) ||
                               player.getEquipment().getItems()[com.elvarg.game.model.container.impl.Equipment.WEAPON_SLOT].getId() == pickaxe.getItemId();
            
            if (!hasPickaxe) {
                continue;
            }
            
            // Check if player meets requirements
            boolean meetsRequirements = player.getSkillManager().getCurrentLevel(com.elvarg.game.model.Skill.MINING) >= pickaxe.getMiningLevel() &&
                                      player.getSkillManager().getCurrentLevel(com.elvarg.game.model.Skill.ATTACK) >= pickaxe.getAttackLevel();
            
            if (!meetsRequirements) {
                continue;
            }
            
            // This pickaxe is usable, check if it's better than current best
            if (best == null || pickaxe.getMiningLevel() > best.getMiningLevel()) {
                best = pickaxe;
            }
        }
        
        return best;
    }
    
    /**
     * Gets the best pickaxe a player can use based only on mining level
     * Used for requirement checking
     */
    public static PickaxeData getBestByMiningLevel(com.elvarg.game.entity.impl.player.Player player) {
        PickaxeData best = null;
        int miningLevel = player.getSkillManager().getCurrentLevel(com.elvarg.game.model.Skill.MINING);
        
        for (PickaxeData pickaxe : values()) {
            // Check if player has this pickaxe
            boolean hasPickaxe = player.getInventory().contains(pickaxe.getItemId()) ||
                               player.getEquipment().getItems()[com.elvarg.game.model.container.impl.Equipment.WEAPON_SLOT].getId() == pickaxe.getItemId();
            
            if (!hasPickaxe) {
                continue;
            }
            
            // Check mining level requirement
            if (miningLevel < pickaxe.getMiningLevel()) {
                continue;
            }
            
            // This pickaxe is usable, check if it's better than current best
            if (best == null || pickaxe.getMiningLevel() > best.getMiningLevel()) {
                best = pickaxe;
            }
        }
        
        return best;
    }
}
