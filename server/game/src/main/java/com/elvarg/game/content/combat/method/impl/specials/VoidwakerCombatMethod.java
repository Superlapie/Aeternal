package com.elvarg.game.content.combat.method.impl.specials;

import com.elvarg.game.content.combat.CombatSpecial;
import com.elvarg.game.content.combat.formula.DamageFormulas;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.impl.MeleeCombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.Priority;
import com.elvarg.util.Misc;

/**
 * Voidwaker special attack (Disrupt): guaranteed hit that rolls 50%-150%
 * of melee max hit.
 */
public class VoidwakerCombatMethod extends MeleeCombatMethod {

    // 2446 Voidwaker IDs from animations export.
    private static final Animation ANIMATION = new Animation(11275, Priority.HIGH);
    private static final Graphic SPEC_CAST_GFX = new Graphic(3030, Priority.HIGH);
    private static final Graphic SPEC_IMPACT_GFX = new Graphic(3017, Priority.HIGH);

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        int maxHit = DamageFormulas.calculateMaxMeleeHit(character);
        int minRoll = Math.max(1, (int) Math.floor(maxHit * 0.5));
        int maxRoll = Math.max(minRoll, (int) Math.floor(maxHit * 1.5));
        int damage = Misc.random(minRoll, maxRoll);
        return new PendingHit[]{PendingHit.create(character, target, this, damage, true)};
    }

    @Override
    public void start(Mobile character, Mobile target) {
        CombatSpecial.drain(character, CombatSpecial.VOIDWAKER.getDrainAmount());
        character.performAnimation(ANIMATION);
        character.performGraphic(SPEC_CAST_GFX);
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        hit.getTarget().performGraphic(SPEC_IMPACT_GFX);
    }
}
