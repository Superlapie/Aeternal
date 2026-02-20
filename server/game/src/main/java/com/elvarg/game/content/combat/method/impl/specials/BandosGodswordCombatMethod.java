package com.elvarg.game.content.combat.method.impl.specials;

import com.elvarg.game.content.combat.CombatSpecial;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.impl.MeleeCombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.Priority;
import com.elvarg.game.model.Skill;

public class BandosGodswordCombatMethod extends MeleeCombatMethod {

    private static final Animation ANIMATION = new Animation(7642, Priority.HIGH);
    private static final Graphic GRAPHIC = new Graphic(1212, Priority.HIGH);

    @Override
    public void start(Mobile character, Mobile target) {
        CombatSpecial.drain(character, CombatSpecial.BANDOS_GODSWORD.getDrainAmount());
        character.performAnimation(ANIMATION);
        character.performGraphic(GRAPHIC);
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (hit.isAccurate() && hit.getTarget().isPlayer()) {
            int remainingDrain = hit.getTotalDamage();
            if (remainingDrain <= 0) {
                return;
            }
            Player target = hit.getTarget().getAsPlayer();
            Skill[] drainOrder = {
                    Skill.DEFENCE,
                    Skill.STRENGTH,
                    Skill.PRAYER,
                    Skill.ATTACK,
                    Skill.MAGIC,
                    Skill.RANGED
            };
            for (Skill skill : drainOrder) {
                if (remainingDrain <= 0) {
                    break;
                }
                int currentLevel = target.getSkillManager().getCurrentLevel(skill);
                if (currentLevel <= 0) {
                    continue;
                }
                int drainAmount = Math.min(currentLevel, remainingDrain);
                target.getSkillManager().setCurrentLevel(skill, currentLevel - drainAmount);
                remainingDrain -= drainAmount;
            }
        }
    }
}
