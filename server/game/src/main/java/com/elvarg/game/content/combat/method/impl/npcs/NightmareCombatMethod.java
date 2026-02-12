package com.elvarg.game.content.combat.method.impl.npcs;

import com.elvarg.game.content.PrayerHandler;
import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.model.Animation;
import com.elvarg.util.Misc;

/**
 * Core Nightmare combat loop:
 * - Rotates attack styles/animations to avoid static melee-only behavior.
 * - Applies lightweight phase transforms for core Nightmare variants.
 */
public class NightmareCombatMethod extends CombatMethod {

    private static final int ATTACK_MELEE = 8594;
    private static final int ATTACK_MAGIC = 8595;
    private static final int ATTACK_RANGED = 8596;
    private static final int ATTACK_SURGE = 8597;

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
        if (npc.getOwner() != null && target.isPlayer() && npc.getOwner() != target.getAsPlayer()) {
            return false;
        }
        return true;
    }

    @Override
    public void onTick(NPC npc, Mobile target) {
        updatePhaseTransform(npc);
    }

    @Override
    public void start(Mobile character, Mobile target) {
        skipHit = false;

        if (!character.isNpc() || target == null) {
            skipHit = true;
            return;
        }

        NPC npc = character.getAsNpc();
        updatePhaseTransform(npc);

        int distance = character.calculateDistance(target);
        int roll = Misc.getRandom(99);
        int selector = attackCounter++ % 6;

        if (distance <= 1 && (selector == 0 || roll < 20)) {
            currentAttackType = CombatType.MELEE;
            currentHitDelay = 1;
            npc.performAnimation(new Animation(ATTACK_MELEE));
            return;
        }

        if (selector == 3 || roll < 25) {
            currentAttackType = CombatType.MAGIC;
            currentHitDelay = 2;
            npc.performAnimation(new Animation(ATTACK_SURGE));
            if (target.isPlayer() && Misc.getRandom(99) < 18) {
                CombatFactory.disableProtectionPrayers(target.getAsPlayer());
            }
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

    private void updatePhaseTransform(NPC npc) {
        int hp = npc.getHitpoints();
        int max = npc.getDefinition().getHitpoints();
        if (max <= 0) {
            return;
        }

        int percent = (hp * 100) / max;
        int transform;
        if (percent > 66) {
            transform = 9425;
        } else if (percent > 33) {
            transform = 9426;
        } else {
            transform = 9427;
        }

        if (npc.getRealId() == transform) {
            npc.setNpcTransformationId(-1);
        } else {
            npc.setNpcTransformationId(transform);
        }
    }
}

