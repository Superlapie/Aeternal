package com.elvarg.game.content.combat.method.impl.specials;

import com.elvarg.game.content.combat.CombatSpecial;
import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.GraphicHeight;
import com.elvarg.game.model.Projectile;
import com.elvarg.game.model.Projectile.ProjectileBuilder;
import com.elvarg.game.model.Priority;
import com.elvarg.game.model.Skill;
import com.elvarg.game.model.equipment.BonusManager;
import com.elvarg.util.Misc;

import static com.elvarg.util.ItemIdentifiers.VOLATILE_NIGHTMARE_STAFF;
import static com.elvarg.util.ItemIdentifiers.VOLATILE_NIGHTMARE_STAFF_2;

public class VolatileNightmareStaffCombatMethod extends CombatMethod {

    private static final Animation CAST_ANIMATION = new Animation(8532, Priority.HIGH);
    private static final Graphic START_GFX = new Graphic(1757, GraphicHeight.HIGH);
    private static final Graphic HIT_GFX = new Graphic(1759, GraphicHeight.HIGH);
    private static final Projectile PROJECTILE = new ProjectileBuilder()
            .setId(1758)
            .setStart(43)
            .setEnd(31)
            .setDelay(51)
            .setAngle(16)
            .setDistanceOffset(64)
            .create();

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        final int delay = 1 + ((1 + character.getLocation().getDistance(target.getLocation())) / 3);
        final PendingHit damage = new PendingHit(character, target, this, delay);
        if (damage.isAccurate()) {
            final int maxHit = (int) Math.min(Math.floor(
                    character.getAsPlayer().getSkillManager().getCurrentLevel(Skill.MAGIC) * 263 / 449D + 1), 58);
            final double hitMultiplier =
                    1 + (character.getAsPlayer().getBonusManager().getOtherBonus()[BonusManager.MAGIC_STRENGTH] / 100D);
            final int hitRoll = Misc.random(1, maxHit);
            damage.setTotalDamage((int) Math.floor(hitRoll * hitMultiplier));
        }
        return new PendingHit[] { damage };
    }

    @Override
    public boolean canAttack(Mobile character, Mobile target) {
        final Player player = character.getAsPlayer();
        final int weaponId = player.getEquipment().getWeapon().getId();
        if (weaponId != VOLATILE_NIGHTMARE_STAFF && weaponId != VOLATILE_NIGHTMARE_STAFF_2) {
            return false;
        }
        return true;
    }

    @Override
    public CombatType type() {
        return CombatType.MAGIC;
    }

    @Override
    public void start(Mobile character, Mobile target) {
        final Player player = character.getAsPlayer();
        CombatSpecial.drain(player, CombatSpecial.VOLATILE_NIGHTMARE_STAFF.getDrainAmount());
        player.performAnimation(CAST_ANIMATION);
        player.performGraphic(START_GFX);
        Projectile.sendProjectile(player, target, PROJECTILE);
    }

    @Override
    public int attackSpeed(Mobile character) {
        // Like a regular spell
        return 5;
    }

    @Override
    public int attackDistance(Mobile character) {
        return 10;
    }

    @Override
    public void finished(Mobile character, Mobile target) {
        // Don't move up to the target or anything
        character.getCombat().reset();
        character.setMobileInteraction(target);
        character.getMovementQueue().reset();
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (hit.isAccurate()) {
            hit.getTarget().performGraphic(HIT_GFX);
        }
    }

}
