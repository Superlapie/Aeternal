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

    private static final Animation ANIMATION = new Animation(11140, Priority.HIGH);
    private static final Graphic GRAPHIC = new Graphic(2814, Priority.HIGH);

    private static final String LAST_SUCCESSFUL_ROLL_ATTR = "burning_claws_last_successful_roll";
    private static final String BURN_STACKS_ACTIVE_ATTR = "burning_claws_burn_stacks_active";
    private static final int MAX_BURN_STACKS = 5;
    private static final int BURN_TICKS_PER_STACK = 10;
    private static final int BURN_DAMAGE_PER_TICK = 1;
    private static final int BURN_TICK_RATE = 4;

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        PendingHit hit = new PendingHit(character, target, this, true, 3, 0);

        int maxHit = DamageFormulas.calculateMaxMeleeHit(character);
        if (target.getPrayerActive()[PrayerHandler.PROTECT_FROM_MELEE]) {
            final double damageMultiplier = target.isNpc() ? CombatConstants.PRAYER_DAMAGE_REDUCTION_AGAINST_NPCS :
                    CombatConstants.PRAYER_DAMAGE_REDUCTION_AGAINST_PLAYERS;
            maxHit *= damageMultiplier;
        }

        final int successfulRoll;
        final int first;
        final int second;
        final int third;
        if (hit.getHits()[0].getDamage() > 0) {
            successfulRoll = 0;
            int total = rollTotalDamage(maxHit, 0.75, 1.75);
            first = (int) Math.floor(total * 0.25);
            second = (int) Math.floor(total * 0.25);
            third = Math.max(0, total - first - second);
        } else if (hit.getHits()[1].getDamage() > 0) {
            successfulRoll = 1;
            int total = rollTotalDamage(maxHit, 0.50, 1.50);
            int firstBase = (int) Math.floor(total * 0.50);
            int secondBase = (int) Math.floor(total * 0.50);
            first = Math.max(0, firstBase - 1);
            second = Math.max(0, secondBase - 1);
            third = Math.max(0, total - first - second);
        } else if (hit.getHits()[2].getDamage() > 0) {
            successfulRoll = 2;
            int total = rollTotalDamage(maxHit, 0.25, 1.25);
            first = total >= 2 ? 1 : 0;
            second = total >= 2 ? 1 : 0;
            third = Math.max(0, total - first - second);
        } else {
            successfulRoll = -1;
            first = 0;
            second = 0;
            int chanceRoll = Misc.getRandom(99);
            if (chanceRoll < 20) {
                third = 0;
            } else if (chanceRoll < 60) {
                third = 1;
            } else {
                third = 2;
            }
        }

        character.setAttribute(LAST_SUCCESSFUL_ROLL_ATTR, successfulRoll);
        hit.getHits()[0].setDamage(first);
        hit.getHits()[1].setDamage(second);
        hit.getHits()[2].setDamage(third);
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
        int successfulRoll = (int) hit.getAttacker().getAttribute(LAST_SUCCESSFUL_ROLL_ATTR, -1);
        int burnChancePerHitsplat = getBurnChanceForRoll(successfulRoll);
        hit.getAttacker().setAttribute(LAST_SUCCESSFUL_ROLL_ATTR, -1);
        if (burnChancePerHitsplat <= 0) {
            return;
        }

        for (int i = 0; i < hit.getHits().length; i++) {
            if (Misc.getRandom(99) < burnChancePerHitsplat) {
                applyBurnStack(hit.getTarget());
            }
        }
    }

    private static int getBurnChanceForRoll(int successfulRoll) {
        if (successfulRoll == 0) {
            return 15;
        }
        if (successfulRoll == 1) {
            return 30;
        }
        if (successfulRoll == 2) {
            return 45;
        }
        return 0;
    }

    private static int rollTotalDamage(int maxHit, double minMultiplier, double maxMultiplier) {
        int min = (int) Math.floor(maxHit * minMultiplier);
        int max = (int) Math.floor(maxHit * maxMultiplier);
        if (max < min) {
            max = min;
        }
        return Misc.randomInclusive(min, max);
    }

    private static void applyBurnStack(Mobile target) {
        int activeStacks = (int) target.getAttribute(BURN_STACKS_ACTIVE_ATTR, 0);
        if (activeStacks >= MAX_BURN_STACKS) {
            return;
        }
        target.setAttribute(BURN_STACKS_ACTIVE_ATTR, activeStacks + 1);
        TaskManager.submit(new BurningClawBurnTask(target));
    }

    private static final class BurningClawBurnTask extends Task {
        private final Mobile target;
        private int ticksRemaining = BURN_TICKS_PER_STACK;

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
            if (ticksRemaining <= 0) {
                stop();
                return;
            }

            target.getCombat().getHitQueue().addPendingDamage(new HitDamage(BURN_DAMAGE_PER_TICK, HitMask.YELLOW));
            ticksRemaining--;
            if (ticksRemaining <= 0) {
                stop();
            }
        }

        @Override
        public void stop() {
            int activeStacks = (int) target.getAttribute(BURN_STACKS_ACTIVE_ATTR, 0);
            target.setAttribute(BURN_STACKS_ACTIVE_ATTR, Math.max(0, activeStacks - 1));
            super.stop();
        }
    }
}
