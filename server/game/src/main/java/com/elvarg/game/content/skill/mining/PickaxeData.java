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
    BRONZE(new int[]{1265, 1266}, 1, 1, new Animation(625), 8, 1.0),
    IRON(new int[]{1267, 1268, 11721}, 1, 1, new Animation(626), 7, 1.1),
    STEEL(new int[]{1269, 1270}, 6, 5, new Animation(627), 6, 1.2),
    BLACK(new int[]{12297, 12298}, 11, 10, new Animation(3873), 5, 1.25),
    MITHRIL(new int[]{1273, 1274, 11720}, 21, 20, new Animation(629), 5, 1.3),
    ADAMANT(new int[]{1271, 1272}, 31, 30, new Animation(628), 4, 1.35),
    RUNE(new int[]{1275, 1276, 11719}, 41, 40, new Animation(624), 3, 1.4),
    DRAGON(new int[]{11920, 11921, 12797, 15259, 23677, 23678, 25376, 25377}, 61, 60, new Animation(7139), 3, 1.5),
    THIRD_AGE(new int[]{20014, 20015}, 61, 60, new Animation(7139), 3, 1.7),
    INFERNAL(new int[]{13243, 13244, 25063, 25065, 25369, 25370}, 61, 60, new Animation(7139), 3, 1.8),
    CRYSTAL(new int[]{23680, 23681, 23682, 23683, 23863}, 71, 70, new Animation(7284), 3, 1.6);
    
    private final int[] itemIds;
    private final int miningLevel;
    private final int attackLevel;
    private final Animation animation;
    private final int tickSpeed;
    private final double speedModifier;
    
    PickaxeData(int[] itemIds, int miningLevel, int attackLevel, Animation animation, int tickSpeed, double speedModifier) {
        this.itemIds = itemIds;
        this.miningLevel = miningLevel;
        this.attackLevel = attackLevel;
        this.animation = animation;
        this.tickSpeed = tickSpeed;
        this.speedModifier = speedModifier;
    }
    
    public int getItemId() {
        return itemIds[0];
    }

    public int[] getItemIds() {
        return itemIds;
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
            if (!playerHasPickaxe(player, pickaxe)) {
                continue;
            }
            
            // This RSPS uses Mining level as the gate for tool usability.
            boolean meetsRequirements = player.getSkillManager().getCurrentLevel(com.elvarg.game.model.Skill.MINING) >= pickaxe.getMiningLevel();
            
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
            if (!playerHasPickaxe(player, pickaxe)) {
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

    private static boolean playerHasPickaxe(com.elvarg.game.entity.impl.player.Player player, PickaxeData pickaxe) {
        int equippedId = player.getEquipment().getItems()[com.elvarg.game.model.container.impl.Equipment.WEAPON_SLOT].getId();
        for (int itemId : pickaxe.getItemIds()) {
            if (player.getInventory().contains(itemId) || equippedId == itemId) {
                return true;
            }
        }
        return false;
    }
}
