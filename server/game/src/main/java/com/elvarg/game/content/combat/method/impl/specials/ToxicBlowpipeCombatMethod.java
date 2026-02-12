package com.elvarg.game.content.combat.method.impl.specials;

import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.CombatSpecial;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.impl.RangedCombatMethod;
import com.elvarg.game.content.combat.ranged.RangedData;
import com.elvarg.game.content.combat.ranged.RangedData.Ammunition;
import com.elvarg.game.content.combat.ranged.RangedData.RangedWeapon;
import com.elvarg.game.content.sound.Sound;
import com.elvarg.game.content.sound.SoundManager;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Priority;
import com.elvarg.game.model.Projectile;

/**
 * Toxic blowpipe special attack (Toxic Siphon).
 */
public class ToxicBlowpipeCombatMethod extends RangedCombatMethod {

    private static final Animation ANIMATION = new Animation(5061, Priority.HIGH);

    @Override
    public boolean canAttack(Mobile character, Mobile target) {
        Player player = character.getAsPlayer();
        if (player.getCombat().getRangedWeapon() != RangedWeapon.TOXIC_BLOWPIPE) {
            return false;
        }
        return super.canAttack(character, target);
    }

    @Override
    public void start(Mobile character, Mobile target) {
        final Player player = character.getAsPlayer();
        final Ammunition ammo = player.getCombat().getAmmunition();

        CombatSpecial.drain(player, CombatSpecial.TOXIC_BLOWPIPE.getDrainAmount());

        player.performAnimation(ANIMATION);

        if (ammo != null) {
            Projectile.sendProjectile(character, target, new Projectile(ammo.getProjectileId(), 40, 35, 40, 60));
        }

        SoundManager.sendSound(player, Sound.SHOOT_ARROW);
        CombatFactory.decrementAmmo(player, target.getLocation(), 1);
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (hit.isAccurate() && hit.getTotalDamage() > 0) {
            hit.getAttacker().heal(hit.getTotalDamage() / 2);
        }
    }
}

