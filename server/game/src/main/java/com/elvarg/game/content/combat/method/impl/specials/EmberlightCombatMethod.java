package com.elvarg.game.content.combat.method.impl.specials;

import com.elvarg.game.content.combat.CombatSpecial;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.impl.MeleeCombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.GraphicHeight;
import com.elvarg.game.model.Priority;
import com.elvarg.game.model.Skill;

public class EmberlightCombatMethod extends MeleeCombatMethod {

    private static final Animation ANIMATION = new Animation(11138, Priority.HIGH);
    private static final Graphic GRAPHIC = new Graphic(2810, GraphicHeight.HIGH, Priority.HIGH);

    @Override
    public void start(Mobile character, Mobile target) {
        CombatSpecial.drain(character, CombatSpecial.EMBERLIGHT.getDrainAmount());
        character.performAnimation(ANIMATION);
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (!hit.isAccurate() || !hit.getTarget().isPlayer()) {
            return;
        }

        hit.getTarget().performGraphic(GRAPHIC);
        Player target = hit.getTarget().getAsPlayer();
        drainCombatStats(target, 0.05);
    }

    private static void drainCombatStats(Player target, double drainMultiplier) {
        drainSkill(target, Skill.ATTACK, drainMultiplier);
        drainSkill(target, Skill.STRENGTH, drainMultiplier);
        drainSkill(target, Skill.DEFENCE, drainMultiplier);
    }

    private static void drainSkill(Player target, Skill skill, double drainMultiplier) {
        int baseLevel = target.getSkillManager().getMaxLevel(skill);
        int currentLevel = target.getSkillManager().getCurrentLevel(skill);
        int drain = (int) Math.floor(baseLevel * drainMultiplier) + 1;
        target.getSkillManager().setCurrentLevel(skill, Math.max(0, currentLevel - drain), true);
    }
}
