package com.elvarg.game.content.combat.method.impl.npcs;

import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.task.impl.CombatPoisonEffect.PoisonType;
import com.elvarg.util.Misc;

public class AraxxorCombatMethod extends CombatMethod {

    private static final int MELEE_ANIM = 11480;
    private static final int MAGIC_ANIM = 11479;
    private static final int RANGED_ANIM = 11476;
    private static final int ACID_LEAK_ANIM = 11477;
    private static final int ACID_SPRAY_ANIM = 11478;
    private static final int SLOW_MELEE_ANIM = 11483;
    private static final int SLOW_RANGED_ANIM = 11484;
    private static final int ENRAGED_MELEE_ANIM = 11487;
    private static final int ENRAGE_TRANSITION_ANIM = 11488;
    private static final int ENRAGE_TRANSITION_2_ANIM = 11489;
    private static final int ACID_CANNON_ANIM = 11493;
    private static final int ATTACK_DISTANCE = 12;

    private static final PendingHit[] NO_HITS = new PendingHit[0];
    private static final int SPECIAL_INTERVAL = 5;

    private CombatType currentType = CombatType.MELEE;
    private int hitDelay = 1;
    private boolean skipHit;
    private int attackCounter;
    private boolean enraged;
    private int specialCycle;
    private boolean playedEnrageTransition;

    @Override
    public void onTick(NPC npc, Mobile target) {
        if (npc == null || !npc.isRegistered() || npc.isDying()) {
            return;
        }
        if (target != null && target.isRegistered()) {
            return;
        }
        for (Player player : npc.getPlayersWithinDistance(16)) {
            if (player == null || !player.isRegistered() || player.isDying()) {
                continue;
            }
            if (player.getPrivateArea() != npc.getPrivateArea()) {
                continue;
            }
            npc.getCombat().attack(player);
            break;
        }
    }

    @Override
    public void start(Mobile character, Mobile target) {
        skipHit = false;
        if (!character.isNpc() || target == null || !target.isRegistered()) {
            skipHit = true;
            return;
        }

        NPC araxxor = character.getAsNpc();
        int distance = character.calculateDistance(target);

        if (!enraged && araxxor.getHitpoints() > 0
                && araxxor.getDefinition() != null
                && araxxor.getHitpoints() <= araxxor.getDefinition().getHitpoints() / 3) {
            enraged = true;
        }

        if (enraged && !playedEnrageTransition) {
            // Trigger once, not twice in the same tick.
            araxxor.performAnimation(new Animation(ENRAGE_TRANSITION_ANIM));
            playedEnrageTransition = true;
            skipHit = true;
            return;
        }

        attackCounter++;
        boolean forceSpecialCycle = (attackCounter % SPECIAL_INTERVAL) == 0;

        if (forceSpecialCycle) {
            performSpecial(araxxor, distance);
            return;
        }

        if (distance <= 1 && Misc.getRandom(99) < (enraged ? 65 : 45)) {
            currentType = CombatType.MELEE;
            hitDelay = 1;
            araxxor.performAnimation(new Animation(enraged ? ENRAGED_MELEE_ANIM : MELEE_ANIM));
            return;
        }

        if (Misc.getRandom(1) == 0) {
            currentType = CombatType.MAGIC;
            hitDelay = 2;
            araxxor.performAnimation(new Animation(MAGIC_ANIM));
        } else {
            currentType = CombatType.RANGED;
            hitDelay = 2;
            araxxor.performAnimation(new Animation(RANGED_ANIM));
        }
    }

    private void performSpecial(NPC araxxor, int distance) {
        // Rotating special cadence:
        // acid leak -> acid spray -> acid cannon -> heavy melee/ranged.
        specialCycle = (specialCycle + 1) % 4;
        switch (specialCycle) {
            case 0:
                currentType = CombatType.MAGIC;
                hitDelay = 3;
                araxxor.performAnimation(new Animation(ACID_LEAK_ANIM));
                break;
            case 1:
                currentType = CombatType.RANGED;
                hitDelay = 3;
                araxxor.performAnimation(new Animation(ACID_SPRAY_ANIM));
                break;
            case 2:
                currentType = CombatType.MAGIC;
                hitDelay = 3;
                araxxor.performAnimation(new Animation(ACID_CANNON_ANIM));
                break;
            default:
                if (distance <= 1) {
                    currentType = CombatType.MELEE;
                    hitDelay = 2;
                    araxxor.performAnimation(new Animation(enraged ? ENRAGED_MELEE_ANIM : SLOW_MELEE_ANIM));
                } else {
                    currentType = CombatType.RANGED;
                    hitDelay = 3;
                    araxxor.performAnimation(new Animation(SLOW_RANGED_ANIM));
                }
                break;
        }
    }

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        if (skipHit) {
            return NO_HITS;
        }
        return new PendingHit[]{new PendingHit(character, target, this, hitDelay)};
    }

    @Override
    public int attackSpeed(Mobile character) {
        return enraged ? 4 : 5;
    }

    @Override
    public int attackDistance(Mobile character) {
        return ATTACK_DISTANCE;
    }

    @Override
    public CombatType type() {
        return currentType;
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (!hit.isAccurate() || !hit.getTarget().isPlayer()) {
            return;
        }
        int roll = Misc.getRandom(99);
        if (currentType == CombatType.MAGIC && roll < 35) {
            CombatFactory.poisonEntity(hit.getTarget(), PoisonType.VENOM);
        } else if (currentType == CombatType.RANGED && roll < 20) {
            CombatFactory.poisonEntity(hit.getTarget(), PoisonType.SUPER);
        }
    }
}
