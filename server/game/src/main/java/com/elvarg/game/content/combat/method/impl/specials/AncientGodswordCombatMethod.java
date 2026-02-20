package com.elvarg.game.content.combat.method.impl.specials;

import com.elvarg.game.content.combat.CombatSpecial;
import com.elvarg.game.content.combat.hit.HitDamage;
import com.elvarg.game.content.combat.hit.HitMask;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.impl.MeleeCombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.Priority;
import com.elvarg.game.model.Skill;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;

public class AncientGodswordCombatMethod extends MeleeCombatMethod {

    private static final Animation ANIMATION = new Animation(9171, Priority.HIGH);
    private static final Graphic GRAPHIC = new Graphic(1211, Priority.HIGH);

    @Override
    public void start(Mobile character, Mobile target) {
        CombatSpecial.drain(character, CombatSpecial.ANCIENT_GODSWORD.getDrainAmount());
        character.performAnimation(ANIMATION);
        character.performGraphic(GRAPHIC);
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (hit.isAccurate()) {
            // These chat messages seem good enough - don't know the actual ones.
            hit.getTarget().sendMessage("You have been marked for blood sacrifice.");
            TaskManager.submit(new Task() {
                int processed = 0;

                @Override
                protected void execute() {
                    processed++;
                    if (hit.getAttacker().calculateDistance(hit.getTarget()) >= 5) {
                        hit.getTarget().sendMessage("You have escaped the blood sacrifice.");
                        stop();
                        return;
                    }
                    if (processed == 8) {
                        hit.getTarget().sendMessage("You have been sacrificed.");
                        hit.getTarget().performGraphic(new Graphic(377));
                        int maxSacrificeDamage;
                        if (hit.getTarget().isPlayer()) {
                            maxSacrificeDamage = (int) Math.floor(hit.getTarget().getAsPlayer().getSkillManager()
                                    .getMaxLevel(Skill.HITPOINTS) * 0.15);
                        } else if (hit.getTarget().isNpc()) {
                            maxSacrificeDamage = (int) Math.floor(((NPC) hit.getTarget()).getDefinition().getHitpoints() * 0.15);
                        } else {
                            maxSacrificeDamage = 0;
                        }
                        int sacrificeDamage = Math.min(25,
                                Math.min(Math.max(maxSacrificeDamage, 0), hit.getTarget().getHitpoints()));
                        hit.getAttacker().heal(sacrificeDamage);
                        hit.getTarget()
                           .getCombat()
                           .getHitQueue()
                           .addPendingDamage(new HitDamage(sacrificeDamage, HitMask.RED));
                        stop();
                        return;
                    }
                }
            });
        }
    }
}
