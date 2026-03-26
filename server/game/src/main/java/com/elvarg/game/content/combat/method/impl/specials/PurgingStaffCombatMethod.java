package com.elvarg.game.content.combat.method.impl.specials;

import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.CombatSpecial;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.magic.CombatSpell;
import com.elvarg.game.content.combat.magic.CombatSpells;
import com.elvarg.game.content.combat.magic.PlayerMagicStaff;
import com.elvarg.game.content.combat.method.impl.MagicCombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.MagicSpellbook;
import com.elvarg.game.model.Priority;
import com.elvarg.util.timers.TimerKey;

import java.util.Optional;

public class PurgingStaffCombatMethod extends MagicCombatMethod {

    private static final Animation ANIMATION = new Animation(8977, Priority.HIGH);
    private static final int INFERIOR_DEMONBANE_SPELL_ID = 30645;
    private static final int SUPERIOR_DEMONBANE_SPELL_ID = 30649;
    private static final int GREATER_DEMONBANE_SPELL_ID = 30653;

    @Override
    public boolean canAttack(Mobile character, Mobile target) {
        if (!character.isPlayer()) {
            return false;
        }

        Player player = character.getAsPlayer();
        if (player.getSpellbook() != MagicSpellbook.ARCEUUS) {
            player.getPacketSender().sendMessage("You need to be on the Arceuus spellbook to use Scatter ashes.");
            return false;
        }
        if (!CombatFactory.isDemonicTarget(target)) {
            player.getPacketSender().sendMessage("Scatter ashes can only be used on demonic targets.");
            return false;
        }

        CombatSpell spell = chooseBestDemonbaneSpell(player);
        if (spell == null) {
            player.getPacketSender().sendMessage("You do not have the runes or level to cast a demonbane spell.");
            return false;
        }

        player.getCombat().setCastSpell(spell);
        return spell.canCast(player, true);
    }

    @Override
    public void start(Mobile character, Mobile target) {
        CombatSpecial.drain(character, CombatSpecial.PURGING_STAFF.getDrainAmount());
        character.performAnimation(ANIMATION);
        super.start(character, target);
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        super.handleAfterHitEffects(hit);

        if (!hit.getAttacker().isPlayer()) {
            return;
        }
        if (!hit.getTarget().isNpc() || !CombatFactory.isDemonicTarget(hit.getTarget())) {
            return;
        }

        Player attacker = hit.getAttacker().getAsPlayer();
        if (hit.getTarget().getHitpoints() > 0) {
            return;
        }

        // Refund the special attack energy if Scatter ashes kills a demonic target.
        attacker.incrementSpecialPercentage(CombatSpecial.PURGING_STAFF.getDrainAmount());
        CombatSpecial.updateBar(attacker);

        // The next attack can be made 3 ticks earlier.
        int remainingTicks = attacker.getTimers().getTicks(TimerKey.COMBAT_ATTACK);
        if (remainingTicks > 0) {
            attacker.getTimers().register(TimerKey.COMBAT_ATTACK, Math.max(0, remainingTicks - 3));
        }
    }

    private static CombatSpell chooseBestDemonbaneSpell(Player player) {
        CombatSpell greater = CombatSpells.getCombatSpell(GREATER_DEMONBANE_SPELL_ID);
        if (canCastSpell(player, greater)) {
            return greater;
        }
        CombatSpell superior = CombatSpells.getCombatSpell(SUPERIOR_DEMONBANE_SPELL_ID);
        if (canCastSpell(player, superior)) {
            return superior;
        }
        CombatSpell inferior = CombatSpells.getCombatSpell(INFERIOR_DEMONBANE_SPELL_ID);
        if (canCastSpell(player, inferior)) {
            return inferior;
        }
        return null;
    }

    private static boolean canCastSpell(Player player, CombatSpell spell) {
        if (spell == null) {
            return false;
        }
        if (player.getSkillManager().getCurrentLevel(com.elvarg.game.model.Skill.MAGIC) < spell.levelRequired()) {
            return false;
        }
        Optional<Item[]> required = spell.itemsRequired(player);
        if (required.isEmpty()) {
            return true;
        }
        Item[] suppressed = PlayerMagicStaff.suppressRunes(player, required.get());
        return player.getInventory().containsAll(suppressed);
    }
}
