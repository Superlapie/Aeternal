package com.elvarg.game.content.combat.method.impl.npcs;

import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Projectile;
import com.elvarg.game.task.impl.CombatPoisonEffect.PoisonType;
import com.elvarg.util.Misc;

/**
 * Snakelings use either magic or melee and can inflict venom.
 */
public class ZulrahSnakelingCombatMethod extends CombatMethod {

    private static final Projectile MAGIC_PROJECTILE = new Projectile(1044, 25, 31, 8, 45);

    private final CombatType attackType;

    public ZulrahSnakelingCombatMethod() {
        this.attackType = Misc.getRandom(1) == 0 ? CombatType.MAGIC : CombatType.MELEE;
    }

    @Override
    public void start(Mobile character, Mobile target) {
        character.performAnimation(new Animation(character.getAttackAnim()));
        if (attackType == CombatType.MAGIC) {
            Projectile.sendProjectile(character, target, MAGIC_PROJECTILE);
        }
    }

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        int delay = attackType == CombatType.MAGIC ? 2 : 0;
        return new PendingHit[]{new PendingHit(character, target, this, delay)};
    }

    @Override
    public int attackDistance(Mobile character) {
        return attackType == CombatType.MAGIC ? 8 : 1;
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (hit.getTarget() != null && hit.getTarget().isPlayer() && Misc.getRandom(99) < 15) {
            CombatFactory.poisonEntity(hit.getTarget(), PoisonType.VENOM);
        }
    }

    @Override
    public CombatType type() {
        return attackType;
    }
}
