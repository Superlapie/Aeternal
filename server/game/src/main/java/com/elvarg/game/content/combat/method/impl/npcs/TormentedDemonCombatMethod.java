package com.elvarg.game.content.combat.method.impl.npcs;

import com.elvarg.game.content.PrayerHandler;
import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.npc.impl.TormentedDemon;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.Projectile;
import com.elvarg.util.Misc;

public class TormentedDemonCombatMethod extends CombatMethod {

    // Use the 2446 Tormented Demon body-sequence set (group 4109/4112/4113/4116/4117)
    // to avoid deformation from mismatched 67xx skeleton animations.
    private static final int MELEE_ATTACK_ANIMATION = 11387;
    private static final int RANGED_ATTACK_ANIMATION = 11389;
    private static final int MAGIC_ATTACK_ANIMATION = 11393;
    private static final int SPECIAL_ATTACK_ANIMATION = 11395;

    private static final Graphic MAGIC_IMPACT_GFX = new Graphic(2733);
    private static final Graphic RANGED_IMPACT_GFX = new Graphic(2731);

    private static final Projectile MAGIC_PROJECTILE = new Projectile(2732, 42, 31, 20, 70);
    private static final Projectile RANGED_PROJECTILE = new Projectile(2730, 42, 31, 20, 70);
    private static final Projectile SPECIAL_PROJECTILE = new Projectile(2735, 42, 31, 20, 70);

    private static final PendingHit[] NO_HITS = new PendingHit[0];

    private AttackStyle currentAttackStyle = AttackStyle.MAGIC;
    private CombatType currentAttackType = CombatType.MAGIC;
    private int currentHitDelay = 3;
    private int attacksUntilSpecial = 12;
    private boolean skipHit;
    private AttackStyle lastNonMeleeStyle = AttackStyle.RANGED;

    @Override
    public boolean canAttack(Mobile character, Mobile target) {
        if (!character.isNpc() || !target.isPlayer()) {
            return false;
        }
        NPC npc = character.getAsNpc();
        if (npc.getOwner() != null && npc.getOwner() != target.getAsPlayer()) {
            return false;
        }
        return true;
    }

    @Override
    public void start(Mobile character, Mobile target) {
        skipHit = false;

        if (!character.isNpc() || !target.isPlayer()) {
            skipHit = true;
            return;
        }

        NPC npc = character.getAsNpc();
        if (!(npc instanceof TormentedDemon demon)) {
            skipHit = true;
            return;
        }

        Player player = target.getAsPlayer();
        if (demon.isAttackStalled()) {
            skipHit = true;
            return;
        }

        if (attacksUntilSpecial <= 0 && !demon.isShieldDownState()) {
            performSpecialAttack(demon, player);
            attacksUntilSpecial = 11 + Misc.getRandom(2);
            return;
        }

        attacksUntilSpecial--;
        performRegularAttack(demon, player);
    }

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        if (skipHit) {
            return NO_HITS;
        }
        return new PendingHit[]{new PendingHit(character, target, this, currentHitDelay)};
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (!hit.getTarget().isPlayer()) {
            return;
        }

        Player target = hit.getTarget().getAsPlayer();

        if (currentAttackStyle == AttackStyle.MAGIC && hit.isAccurate()) {
            target.performGraphic(MAGIC_IMPACT_GFX);
            return;
        }

        if (currentAttackStyle == AttackStyle.RANGED && hit.isAccurate()) {
            target.performGraphic(RANGED_IMPACT_GFX);
            return;
        }

        if (currentAttackStyle == AttackStyle.SPECIAL) {
            target.setRunning(false);
            target.getPacketSender().sendRunStatus();
            CombatFactory.freeze(target, 5);
        }
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
    public CombatType type() {
        return currentAttackType;
    }

    private void performRegularAttack(TormentedDemon demon, Player target) {
        int distance = Math.max(1, demon.calculateDistance(target));
        AttackStyle chosenStyle = chooseRegularAttackStyle(target, distance);
        currentAttackStyle = chosenStyle;

        switch (chosenStyle) {
            case MELEE:
                currentAttackType = CombatType.MELEE;
                currentHitDelay = 1;
                demon.performAnimation(new Animation(MELEE_ATTACK_ANIMATION));
                break;
            case RANGED:
                currentAttackType = CombatType.RANGED;
                currentHitDelay = projectileHitDelay(distance);
                demon.performAnimation(new Animation(RANGED_ATTACK_ANIMATION));
                Projectile.sendProjectile(demon, target, RANGED_PROJECTILE);
                break;
            case MAGIC:
            default:
                currentAttackType = CombatType.MAGIC;
                currentHitDelay = projectileHitDelay(distance);
                demon.performAnimation(new Animation(MAGIC_ATTACK_ANIMATION));
                Projectile.sendProjectile(demon, target, MAGIC_PROJECTILE);
                break;
        }
    }

    private void performSpecialAttack(TormentedDemon demon, Player target) {
        int distance = Math.max(1, demon.calculateDistance(target));
        currentAttackStyle = AttackStyle.SPECIAL;
        currentAttackType = CombatType.MAGIC;
        currentHitDelay = projectileHitDelay(distance);

        demon.performAnimation(new Animation(SPECIAL_ATTACK_ANIMATION));
        Projectile.sendProjectile(demon, target, SPECIAL_PROJECTILE);
        demon.enterShieldDownWindow();
    }

    private AttackStyle chooseRegularAttackStyle(Player target, int distance) {
        boolean protectMelee = PrayerHandler.isActivated(target, PrayerHandler.PROTECT_FROM_MELEE);
        boolean protectRanged = PrayerHandler.isActivated(target, PrayerHandler.PROTECT_FROM_MISSILES);
        boolean protectMagic = PrayerHandler.isActivated(target, PrayerHandler.PROTECT_FROM_MAGIC);
        boolean meleeRange = distance <= 1;

        if (meleeRange && !protectMelee && Misc.getRandom(99) < 55) {
            return AttackStyle.MELEE;
        }

        if (protectMagic && !protectRanged) {
            lastNonMeleeStyle = AttackStyle.RANGED;
            return AttackStyle.RANGED;
        }

        if (protectRanged && !protectMagic) {
            lastNonMeleeStyle = AttackStyle.MAGIC;
            return AttackStyle.MAGIC;
        }

        if (meleeRange && Misc.getRandom(99) < 30) {
            return AttackStyle.MELEE;
        }

        // When not using melee, alternate magic/ranged to mimic TD non-melee cycling.
        if (lastNonMeleeStyle == AttackStyle.MAGIC) {
            lastNonMeleeStyle = AttackStyle.RANGED;
            return AttackStyle.RANGED;
        }
        lastNonMeleeStyle = AttackStyle.MAGIC;
        return AttackStyle.MAGIC;
    }

    private int projectileHitDelay(int distance) {
        return distance <= 3 ? 3 : 4;
    }

    private enum AttackStyle {
        MELEE,
        RANGED,
        MAGIC,
        SPECIAL
    }
}
