package com.elvarg.game.content.skill.slayer;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Skill;

public enum SlayerMaster {
    TURAEL(401, 0, 0, 0, 40, new int[][] { { 10, 3 }, { 50, 10 }, { 100, 25 }, { 250, 50 }, { 1000, 75 } }),
    KRYSTILIA(7663, 0, 0, 25, 100, new int[][] { { 10, 75 }, { 50, 225 }, { 100, 375 }, { 250, 525 }, { 1000, 750 } }),
    MAZCHNA(402, 20, 0, 6, 50, new int[][] { { 10, 5 }, { 50, 15 }, { 100, 50 }, { 250, 70 }, { 1000, 100 } }),
    VANNAKA(403, 40, 0, 8, 60, new int[][] { { 10, 20 }, { 50, 60 }, { 100, 100 }, { 250, 140 }, { 1000, 200 } }),
    CHAELDAR(404, 70, 0, 10, 70, new int[][] { { 10, 50 }, { 50, 150 }, { 100, 250 }, { 250, 350 }, { 1000, 500 } }),
    KONAR(8623, 75, 0, 18, 80, new int[][] { { 10, 90 }, { 50, 270 }, { 100, 450 }, { 250, 630 }, { 1000, 900 } }),
    STEVE(6798, 85, 0, 12, 90, new int[][] { { 10, 60 }, { 50, 180 }, { 100, 300 }, { 250, 420 }, { 1000, 600 } }),
    KURADAL(9085, 100, 50, 15, 100, new int[][] { { 10, 75 }, { 50, 225 }, { 100, 375 }, { 250, 525 }, { 1000, 750 } });

    private final int npcId;
    private final int combatLevel;
    private final int slayerLevel;
    private final int basePoints;
    private final int blockCost;
    private final int[][] consecutiveTaskPoints;

    SlayerMaster(int npcId, int combatLevel, int slayerLevel, int basePoints, int blockCost, int[][] consecutiveTaskPoints) {
        this.npcId = npcId;
        this.combatLevel = combatLevel;
        this.slayerLevel = slayerLevel;
        this.basePoints = basePoints;
        this.blockCost = blockCost;
        this.consecutiveTaskPoints = consecutiveTaskPoints;
    }

    public int getNpcId() {
        return npcId;
    }

    public int getCombatLevel() {
        return combatLevel;
    }

    public int getSlayerLevel() {
        return slayerLevel;
    }

    public int getBasePoints() {
        return basePoints;
    }

    public int getBlockCost() {
        return blockCost;
    }

    public int[][] getConsecutiveTaskPoints() {
        return consecutiveTaskPoints;
    }
    
    public boolean canAssign(Player player) {
        if (player.getSkillManager().getCombatLevel() < combatLevel) {
            return false;
        }
        if (player.getSkillManager().getMaxLevel(Skill.SLAYER) < slayerLevel) {
            return false;
        }
        return true;
    }
    
    public static final SlayerMaster[] MASTERS = values();
}
