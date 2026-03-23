package com.elvarg.game.content.combat.method.impl.specials;

import com.elvarg.game.content.PrayerHandler;
import com.elvarg.game.content.combat.CombatConstants;
import com.elvarg.game.content.combat.CombatSpecial;
import com.elvarg.game.content.combat.formula.DamageFormulas;
import com.elvarg.game.content.combat.hit.HitDamage;
import com.elvarg.game.content.combat.hit.HitMask;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.impl.MeleeCombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.Priority;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;
import com.elvarg.util.Misc;

public class BurningClawCombatMethod extends MeleeCombatMethod {

    private static final Animation ANIMATION = new Animation(7527, Priority.HIGH);
    private static final Graphic GRAPHIC = new Graphic(1171, Priority.HIGH);

    private static final String BURN_TICKS_REMAINING_ATTR = "burning_claws_burn_ticks";
    private static final String BURN_TASK_ACTIVE_ATTR = "burning_claws_burn_task_active";
    private static final int BURN_TOTAL_TICKS = 10;
    private static final int BURN_DAMAGE_PER_TICK = 1;
    private static final int BURN_TICK_RATE = 4;

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        PendingHit hit = new PendingHit(character, target, this, true, 4, 0);

        int maxHit = DamageFormulas.calculateMaxMeleeHit(character);
        if (target.getPrayerActive()[PrayerHandler.PROTECT_FROM_MELEE]) {
            final double damageMultiplier = target.isNpc() ? CombatConstants.PRAYER_DAMAGE_REDUCTION_AGAINST_NPCS :
                    CombatConstants.PRAYER_DAMAGE_REDUCTION_AGAINST_PLAYERS;
            maxHit *= damageMultiplier;
        }

        final int first;
        final int second;
        final int third;
        final int fourth;
        if (hit.getHits()[0].getDamage() > 0) {
            first = Misc.randomInclusive((int) Math.round(maxHit * 0.5), maxHit - 1);
            second = first / 2;
            third = second / 2;
            fourth = third + Misc.random(1);
        } else if (hit.getHits()[1].getDamage() > 0) {
            first = 0;
            second = Misc.randomInclusive((int) Math.round(maxHit * (3 / 8D)), (int) Math.round(maxHit * (7 / 8D)));
            third = second / 2;
            fourth = third + Misc.random(1);
        } else if (hit.getHits()[2].getDamage() > 0) {
            first = 0;
            second = 0;
            third = Misc.randomInclusive((int) Math.round(maxHit * 0.25), (int) Math.round(maxHit * 0.75));
            fourth = third + Misc.random(1);
        } else if (hit.getHits()[3].getDamage() > 0) {
            first = 0;
            second = 0;
            third = 0;
            fourth = Misc.randomInclusive((int) Math.round(maxHit * 0.25), (int) Math.round(maxHit * 1.25));
        } else {
            first = 0;
            second = 0;
            third = Misc.random(1);
            fourth = third;
        }

        hit.getHits()[0].setDamage(first);
        hit.getHits()[1].setDamage(second);
        hit.getHits()[2].setDamage(third);
        hit.getHits()[3].setDamage(fourth);
        hit.updateTotalDamage();
        return new PendingHit[]{hit};
    }

    @Override
    public void start(Mobile character, Mobile target) {
        CombatSpecial.drain(character, CombatSpecial.BURNING_CLAWS.getDrainAmount());
        character.performAnimation(ANIMATION);
        character.performGraphic(GRAPHIC);
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        int burnChance = 0;
        if (hit.getHits()[0].getDamage() > 0) {
            burnChance = 15;
        } else if (hit.getHits()[1].getDamage() > 0) {
            burnChance = 30;
        } else if (hit.getHits()[2].getDamage() > 0) {
            burnChance = 45;
        }

        if (burnChance > 0 && Misc.getRandom(99) < burnChance) {
            applyOrRefreshBurn(hit.getTarget());
        }
    }

    private static void applyOrRefreshBurn(Mobile target) {
        target.setAttribute(BURN_TICKS_REMAINING_ATTR, BURN_TOTAL_TICKS);

        boolean taskActive = (boolean) target.getAttribute(BURN_TASK_ACTIVE_ATTR, false);
        if (!taskActive) {
            target.setAttribute(BURN_TASK_ACTIVE_ATTR, true);
            TaskManager.submit(new BurningClawBurnTask(target));
        }
    }

    private static final class BurningClawBurnTask extends Task {
        private final Mobile target;

        private BurningClawBurnTask(Mobile target) {
            super(BURN_TICK_RATE, target, false);
            this.target = target;
        }

        @Override
        protected void execute() {
            if (target == null || !target.isRegistered() || target.getHitpoints() <= 0) {
                stop();
                return;
            }

            int ticksRemaining = (int) target.getAttribute(BURN_TICKS_REMAINING_ATTR, 0);
            if (ticksRemaining <= 0) {
                stop();
                return;
            }

            target.getCombat().getHitQueue().addPendingDamage(new HitDamage(BURN_DAMAGE_PER_TICK, HitMask.YELLOW));
            target.setAttribute(BURN_TICKS_REMAINING_ATTR, ticksRemaining - 1);

            if (ticksRemaining - 1 <= 0) {
                stop();
            }
        }

        @Override
        public void stop() {
            target.setAttribute(BURN_TASK_ACTIVE_ATTR, false);
            super.stop();
        }
    }
}
