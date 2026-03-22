package com.elvarg.game.content.combat.method.impl.npcs;

import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.model.Animation;
import com.elvarg.util.Misc;

public class ThrallMagicCombatMethod extends CombatMethod {
    private static final String THRALL_MAX_HIT_ATTR = "thrall_max_hit";
    private static final int LESSER_THRALL_MAX_HIT = 1;

    @Override
    public void start(Mobile character, Mobile target) {
        int animation = character.getAttackAnim();
        if (animation != -1) {
            character.performAnimation(new Animation(animation));
        }
    }

    @Override
    public CombatType type() {
        return CombatType.MAGIC;
    }

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        int distance = character.getLocation().getDistance(target.getLocation());
        int delay = 1 + ((1 + distance) / 3);
        int maxHit = (int) character.getAttribute(THRALL_MAX_HIT_ATTR, LESSER_THRALL_MAX_HIT);
        PendingHit hit = new PendingHit(character, target, this, false, delay);
        hit.setTotalDamage(Misc.inclusive(0, maxHit));
        return new PendingHit[]{hit};
    }

    @Override
    public int attackDistance(Mobile character) {
        return 6;
    }
}
