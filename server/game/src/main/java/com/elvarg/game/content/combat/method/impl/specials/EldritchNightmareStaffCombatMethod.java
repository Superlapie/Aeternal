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

import static com.elvarg.util.ItemIdentifiers.ELDRITCH_NIGHTMARE_STAFF;
import static com.elvarg.util.ItemIdentifiers.ELDRITCH_NIGHTMARE_STAFF_2;

public class EldritchNightmareStaffCombatMethod extends CombatMethod {

    private static final Animation CAST_ANIMATION = new Animation(8531, Priority.HIGH);
    private static final Graphic START_GFX = new Graphic(1760, GraphicHeight.HIGH);
    private static final Graphic HIT_GFX = new Graphic(1762, GraphicHeight.HIGH);
    private static final Projectile PROJECTILE = new ProjectileBuilder()
            .setId(1761)
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
                    character.getAsPlayer().getSkillManager().getCurrentLevel(Skill.MAGIC) * 4 / 9D + 1), 44);
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
        return weaponId == ELDRITCH_NIGHTMARE_STAFF || weaponId == ELDRITCH_NIGHTMARE_STAFF_2;
    }

    @Override
    public CombatType type() {
        return CombatType.MAGIC;
    }

    @Override
    public void start(Mobile character, Mobile target) {
        final Player player = character.getAsPlayer();
        CombatSpecial.drain(player, CombatSpecial.ELDRITCH_NIGHTMARE_STAFF.getDrainAmount());
        player.performAnimation(CAST_ANIMATION);
        player.performGraphic(START_GFX);
        Projectile.sendProjectile(player, target, PROJECTILE);
    }

    @Override
    public int attackSpeed(Mobile character) {
        return 5;
    }

    @Override
    public int attackDistance(Mobile character) {
        return 10;
    }

    @Override
    public void finished(Mobile character, Mobile target) {
        character.getCombat().reset();
        character.setMobileInteraction(target);
        character.getMovementQueue().reset();
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (!hit.isAccurate()) {
            return;
        }

        hit.getTarget().performGraphic(HIT_GFX);
        if (!hit.getAttacker().isPlayer()) {
            return;
        }

        final Player player = hit.getAttacker().getAsPlayer();
        final int damage = Math.max(0, hit.getTotalDamage());
        if (damage <= 0) {
            return;
        }

        // OSRS: Eldritch special restores prayer points equal to 50% of damage dealt.
        final int prayerRestore = damage / 2;
        final int currentPrayer = player.getSkillManager().getCurrentLevel(Skill.PRAYER);
        final int maxPrayer = player.getSkillManager().getMaxLevel(Skill.PRAYER);
        final int updatedPrayer = Math.min(maxPrayer, currentPrayer + prayerRestore);
        final int restoredPrayer = Math.max(0, updatedPrayer - currentPrayer);
        player.getSkillManager().setCurrentLevel(Skill.PRAYER, updatedPrayer);

        // Eldritch also restores run energy by 25% of damage dealt.
        final int currentRun = player.getRunEnergy();
        final int runRestore = damage / 4;
        final int updatedRun = Math.min(100, currentRun + runRestore);
        final int restoredRun = Math.max(0, updatedRun - currentRun);
        player.setRunEnergy(updatedRun);
        player.getPacketSender().sendRunEnergy();

        if (restoredPrayer > 0 || restoredRun > 0) {
            player.getPacketSender().sendMessage(
                    "Your eldritch staff restores " + restoredPrayer + " Prayer and " + restoredRun + "% run energy.");
        }
    }
}
