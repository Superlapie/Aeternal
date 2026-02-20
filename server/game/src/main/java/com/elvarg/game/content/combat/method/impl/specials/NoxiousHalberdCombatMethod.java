package com.elvarg.game.content.combat.method.impl.specials;

import com.elvarg.game.content.combat.CombatSpecial;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.impl.MeleeCombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.GraphicHeight;
import com.elvarg.game.model.Priority;

public class NoxiousHalberdCombatMethod extends MeleeCombatMethod {

    // 11516/11587 from 2446 are not a stable human-rig sequence in this client.
    // Use a compatible halberd special anim to avoid warped playback/stuck idle.
    private static final Animation ANIMATION = new Animation(1203, Priority.HIGH);
    private static final Graphic IMPACT_GFX = new Graphic(2914, GraphicHeight.HIGH, Priority.HIGH);

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        return new PendingHit[]{new PendingHit(character, target, this)};
    }

    @Override
    public void start(Mobile character, Mobile target) {
        CombatSpecial.drain(character, CombatSpecial.NOXIOUS_HALBERD.getDrainAmount());
        character.performAnimation(ANIMATION);
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        hit.getTarget().performGraphic(IMPACT_GFX);
    }
}
