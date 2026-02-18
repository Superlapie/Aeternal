package com.elvarg.game.content.combat.method.impl.npcs;

import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.Projectile;
import com.elvarg.util.Misc;

public class YamaCombatMethod extends CombatMethod {

    private static final int MELEE_ANIM = 12146;
    private static final int MAGIC_ANIM = 12144;
    private static final int RANGED_ANIM = 12145;
    private static final int METEOR_ANIM = 12148;

    private static final int MAGIC_IMPACT_GFX = 2810;
    private static final int RANGED_IMPACT_GFX = 2812;
    private static final int METEOR_IMPACT_GFX = 2815;

    private static final Projectile MAGIC_PROJECTILE = new Projectile(2810, 45, 20, 20, 55);
    private static final Projectile RANGED_PROJECTILE = new Projectile(2812, 45, 20, 20, 55);

    private static final PendingHit[] NO_HITS = new PendingHit[0];

    private CombatType currentType = CombatType.MELEE;
    private int hitDelay = 1;
    private boolean skipHit;
    private int attackCounter;

    @Override
    public void start(Mobile character, Mobile target) {
        skipHit = false;
        if (!character.isNpc() || target == null || !target.isRegistered()) {
            skipHit = true;
            return;
        }

        NPC yama = character.getAsNpc();
        int distance = character.calculateDistance(target);

        // Meteor-like special every 7th attack.
        if (++attackCounter % 7 == 0) {
            currentType = CombatType.MAGIC;
            hitDelay = 3;
            yama.performAnimation(new Animation(METEOR_ANIM));
            target.performGraphic(new Graphic(METEOR_IMPACT_GFX));
            return;
        }

        if (distance <= 1 && Misc.getRandom(99) < 30) {
            currentType = CombatType.MELEE;
            hitDelay = 1;
            yama.performAnimation(new Animation(MELEE_ANIM));
            return;
        }

        if (Misc.getRandom(1) == 0) {
            currentType = CombatType.MAGIC;
            hitDelay = 2;
            yama.performAnimation(new Animation(MAGIC_ANIM));
            Projectile.sendProjectile(yama, target, MAGIC_PROJECTILE);
            target.performGraphic(new Graphic(MAGIC_IMPACT_GFX));
        } else {
            currentType = CombatType.RANGED;
            hitDelay = 2;
            yama.performAnimation(new Animation(RANGED_ANIM));
            Projectile.sendProjectile(yama, target, RANGED_PROJECTILE);
            target.performGraphic(new Graphic(RANGED_IMPACT_GFX));
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
        return 6;
    }

    @Override
    public int attackDistance(Mobile character) {
        return 12;
    }

    @Override
    public CombatType type() {
        return currentType;
    }
}
