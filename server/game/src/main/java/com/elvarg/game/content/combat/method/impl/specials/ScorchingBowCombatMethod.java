package com.elvarg.game.content.combat.method.impl.specials;

import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.CombatSpecial;
import com.elvarg.game.content.combat.hit.HitDamage;
import com.elvarg.game.content.combat.hit.HitMask;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.impl.RangedCombatMethod;
import com.elvarg.game.content.combat.ranged.RangedData.RangedWeapon;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.Priority;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;

public class ScorchingBowCombatMethod extends RangedCombatMethod {

    private static final Animation ANIMATION = new Animation(11133, Priority.HIGH);
    private static final Graphic IMPACT_GRAPHIC = new Graphic(2908, Priority.HIGH);
    private static final int BIND_TICKS = 20;
    private static final int BURN_DAMAGE_TOTAL = 5;
    private static final int BURN_DAMAGE_PER_TICK = 1;
    private static final int BURN_TICK_RATE = 4;
    private static final String BURN_TICKS_REMAINING_ATTR = "scorching_bow_burn_ticks";
    private static final String BURN_TASK_ACTIVE_ATTR = "scorching_bow_burn_task_active";

    @Override
    public boolean canAttack(Mobile character, Mobile target) {
        if (!super.canAttack(character, target)) {
            return false;
        }
        Player player = character.getAsPlayer();
        if (player.getCombat().getRangedWeapon() != RangedWeapon.SCORCHING_BOW) {
            return false;
        }
        if (!CombatFactory.isDemonicTarget(target)) {
            player.getPacketSender().sendMessage("Scorching shackles won't work against a non-demon enemy.");
            return false;
        }
        return true;
    }

    @Override
    public void start(Mobile character, Mobile target) {
        CombatSpecial.drain(character, CombatSpecial.SCORCHING_BOW.getDrainAmount());
        character.performAnimation(ANIMATION);
        super.start(character, target);
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        Mobile target = hit.getTarget();
        if (!CombatFactory.isDemonicTarget(target)) {
            return;
        }

        target.performGraphic(IMPACT_GRAPHIC);
        if (target.isPlayer()) {
            Player player = target.getAsPlayer();
            player.setRunning(false);
            player.getPacketSender().sendRunStatus();
        }

        CombatFactory.freeze(target, BIND_TICKS);
        applyOrRefreshBurn(target);
    }

    private static void applyOrRefreshBurn(Mobile target) {
        target.setAttribute(BURN_TICKS_REMAINING_ATTR, BURN_DAMAGE_TOTAL);
        boolean taskActive = (boolean) target.getAttribute(BURN_TASK_ACTIVE_ATTR, false);
        if (!taskActive) {
            target.setAttribute(BURN_TASK_ACTIVE_ATTR, true);
            TaskManager.submit(new ScorchingBurnTask(target));
        }
    }

    private static final class ScorchingBurnTask extends Task {
        private final Mobile target;

        private ScorchingBurnTask(Mobile target) {
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
