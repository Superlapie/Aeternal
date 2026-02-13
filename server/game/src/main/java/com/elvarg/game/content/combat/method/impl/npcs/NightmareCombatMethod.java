package com.elvarg.game.content.combat.method.impl.npcs;

import com.elvarg.game.content.PrayerHandler;
import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.model.areas.impl.NightmareArea;
import com.elvarg.game.model.Animation;
import com.elvarg.util.Misc;

/**
 * Core Nightmare combat loop:
 * - Rotates attack styles/animations to avoid static melee-only behavior.
 * - Keeps a stable visual form during combat to prevent incorrect suspended-state morphs.
 */
public class NightmareCombatMethod extends CombatMethod {

    private static final int ATTACK_MELEE = 8594;
    private static final int ATTACK_MAGIC = 8595;
    private static final int ATTACK_RANGED = 8596;
    private static final PendingHit[] NO_HITS = new PendingHit[0];

    private CombatType currentAttackType = CombatType.MAGIC;
    private int currentHitDelay = 2;
    private boolean skipHit;
    private int attackCounter;

    @Override
    public boolean canAttack(Mobile character, Mobile target) {
        if (!character.isNpc() || target == null || !target.isRegistered()) {
            return false;
        }
        NPC npc = character.getAsNpc();
        if (!NightmareArea.inFightBox(npc.getLocation())) {
            return false;
        }
        if (!NightmareArea.inFightBox(target.getLocation())) {
            return false;
        }
        if (npc.getOwner() != null && target.isPlayer() && npc.getOwner() != target.getAsPlayer()) {
            return false;
        }
        return true;
    }

    @Override
    public void onTick(NPC npc, Mobile target) {
        // Force stable form while fighting.
        // Some imported Nightmare variants map to suspended visuals in this cache.
        if (npc.getNpcTransformationId() != -1) {
            npc.setNpcTransformationId(-1);
        }
        if (!NightmareArea.inFightBox(npc.getLocation())) {
            npc.getMovementQueue().reset();
            npc.getCombat().reset();
            npc.moveTo(NightmareArea.NIGHTMARE_SPAWN.clone());
            return;
        }
        if (target == null || !NightmareArea.inFightBox(target.getLocation())) {
            npc.getCombat().reset();
        }
    }

    @Override
    public void start(Mobile character, Mobile target) {
        skipHit = false;

        if (!character.isNpc() || target == null) {
            skipHit = true;
            return;
        }

        NPC npc = character.getAsNpc();
        int distance = character.calculateDistance(target);
        int roll = Misc.getRandom(99);
        int selector = attackCounter++ % 5;

        if (distance <= 1 && (selector == 0 || roll < 20)) {
            currentAttackType = CombatType.MELEE;
            currentHitDelay = 1;
            npc.performAnimation(new Animation(ATTACK_MELEE));
            return;
        }

        if (selector % 2 == 0) {
            currentAttackType = CombatType.MAGIC;
            currentHitDelay = 2;
            npc.performAnimation(new Animation(ATTACK_MAGIC));
        } else {
            currentAttackType = CombatType.RANGED;
            currentHitDelay = 2;
            npc.performAnimation(new Animation(ATTACK_RANGED));
            if (target.isPlayer() && Misc.getRandom(99) < 15) {
                target.getAsPlayer().getSkillManager().decreaseCurrentLevel(
                        com.elvarg.game.model.Skill.PRAYER, 2, 0);
            }
        }
    }

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        if (skipHit) {
            return NO_HITS;
        }
        return new PendingHit[]{new PendingHit(character, target, this, currentHitDelay)};
    }

    @Override
    public int attackSpeed(Mobile character) {
        return 5;
    }

    @Override
    public int attackDistance(Mobile character) {
        return 20;
    }

    @Override
    public CombatType type() {
        return currentAttackType;
    }
}
