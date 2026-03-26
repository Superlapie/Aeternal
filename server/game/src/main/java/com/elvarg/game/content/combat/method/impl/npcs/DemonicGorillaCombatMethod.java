package com.elvarg.game.content.combat.method.impl.npcs;

import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.npc.impl.DemonicGorilla;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Projectile;
import com.elvarg.util.Misc;

public class DemonicGorillaCombatMethod extends CombatMethod {

    private static final int MELEE_ATTACK_ANIMATION = 7226;
    private static final int RANGED_ATTACK_ANIMATION = 7227;
    private static final int MAGIC_ATTACK_ANIMATION = 7225;
    private static final int ROAR_ANIMATION = 7222;

    private static final Projectile RANGED_PROJECTILE = new Projectile(1302, 43, 31, 20, 75);
    private static final Projectile MAGIC_PROJECTILE = new Projectile(1304, 43, 31, 20, 75);

    private AttackStyle currentAttackStyle = AttackStyle.random();
    private int currentHitDelay = 2;
    private int consecutiveZeroHits;

    @Override
    public boolean canAttack(Mobile character, Mobile target) {
        if (!character.isNpc() || !target.isPlayer()) {
            return false;
        }
        NPC npc = character.getAsNpc();
        return npc.getOwner() == null || npc.getOwner() == target.getAsPlayer();
    }

    @Override
    public void start(Mobile character, Mobile target) {
        if (!character.isNpc() || !target.isPlayer()) {
            return;
        }

        NPC npc = character.getAsNpc();
        if (!(npc instanceof DemonicGorilla gorilla)) {
            return;
        }

        Player player = target.getAsPlayer();
        int distance = Math.max(1, gorilla.calculateDistance(player));
        AttackStyle style = currentAttackStyle;

        currentAttackStyle = style;
        currentHitDelay = style.hitDelay(distance);

        gorilla.performAnimation(style.animation);
        if (style.projectile != null) {
            Projectile.sendProjectile(gorilla, player, style.projectile);
        }
    }

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        return new PendingHit[]{new PendingHit(character, target, this, currentHitDelay)};
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (hit.getTarget() == null || !hit.getTarget().isPlayer()) {
            return;
        }

        if (hit.getTotalDamage() <= 0) {
            consecutiveZeroHits++;
            if (consecutiveZeroHits >= 3) {
                consecutiveZeroHits = 0;
                currentAttackStyle = currentAttackStyle.next();
            }
            return;
        }

        consecutiveZeroHits = 0;
    }

    @Override
    public int attackSpeed(Mobile character) {
        return 4;
    }

    @Override
    public int attackDistance(Mobile character) {
        return currentAttackStyle == AttackStyle.MELEE ? 1 : 10;
    }

    @Override
    public CombatType type() {
        return currentAttackStyle.combatType;
    }

    private enum AttackStyle {
        MELEE(CombatType.MELEE, new Animation(MELEE_ATTACK_ANIMATION), null),
        RANGED(CombatType.RANGED, new Animation(RANGED_ATTACK_ANIMATION), RANGED_PROJECTILE),
        MAGIC(CombatType.MAGIC, new Animation(MAGIC_ATTACK_ANIMATION), MAGIC_PROJECTILE);

        private final CombatType combatType;
        private final Animation animation;
        private final Projectile projectile;

        AttackStyle(CombatType combatType, Animation animation, Projectile projectile) {
            this.combatType = combatType;
            this.animation = animation;
            this.projectile = projectile;
        }

        private int hitDelay(int distance) {
            if (this == MELEE) {
                return 1;
            }
            return distance <= 3 ? 3 : 4;
        }

        private AttackStyle next() {
            return switch (this) {
                case MELEE -> RANGED;
                case RANGED -> MAGIC;
                case MAGIC -> MELEE;
            };
        }

        private static AttackStyle random() {
            return switch (Misc.getRandom(2)) {
                case 0 -> MELEE;
                case 1 -> RANGED;
                default -> MAGIC;
            };
        }
    }
}
