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
    BRONZE(1265, 1, 1, new Animation(625), 8, 1.0),       // OSRS Wiki: 8 ticks between rolls
    IRON(1267, 1, 1, new Animation(626), 7, 1.1),         // OSRS Wiki: 7 ticks between rolls
    STEEL(1269, 6, 5, new Animation(627), 6, 1.2),        // OSRS Wiki: 6 ticks between rolls
    BLACK(12297, 11, 10, new Animation(3873), 5, 1.25),   // OSRS Wiki: 5 ticks between rolls
    MITHRIL(1273, 21, 20, new Animation(629), 5, 1.3),    // OSRS Wiki: 5 ticks between rolls
    ADAMANT(1271, 31, 30, new Animation(628), 4, 1.35),   // OSRS Wiki: 4 ticks between rolls
    RUNE(1275, 41, 40, new Animation(624), 3, 1.4),       // OSRS Wiki: 3 ticks between rolls
    DRAGON(11920, 61, 60, new Animation(7139), 3, 1.5),   // OSRS Wiki: 3 ticks by default
    THIRD_AGE(20014, 61, 60, new Animation(7139), 3, 1.7),
    INFERNAL(13243, 61, 60, new Animation(7139), 3, 1.8),
    CRYSTAL(23681, 71, 70, new Animation(7284), 3, 1.6);
    
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
