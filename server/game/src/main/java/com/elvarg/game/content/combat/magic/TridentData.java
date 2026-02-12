package com.elvarg.game.content.combat.magic;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Skill;
import com.elvarg.util.ItemIdentifiers;

import java.util.Set;

/**
 * Trident helpers for powered-staff combat routing and formulas.
 */
public final class TridentData {

    private static final Set<Integer> SEAS_TRIDENT_IDS = Set.of(
            ItemIdentifiers.TRIDENT_OF_THE_SEAS_FULL_,
            ItemIdentifiers.TRIDENT_OF_THE_SEAS,
            ItemIdentifiers.TRIDENT_OF_THE_SEAS_E_,
            ItemIdentifiers.TRIDENT_OF_THE_SEAS_E_2
    );

    private static final Set<Integer> SWAMP_TRIDENT_IDS = Set.of(
            ItemIdentifiers.TRIDENT_OF_THE_SWAMP,
            ItemIdentifiers.TRIDENT_OF_THE_SWAMP_E_,
            ItemIdentifiers.TRIDENT_OF_THE_SWAMP_E_2
    );

    private TridentData() {
    }

    public static boolean isTridentWeapon(int weaponId) {
        return isSeasTrident(weaponId) || isSwampTrident(weaponId);
    }

    public static boolean isSeasTrident(int weaponId) {
        return SEAS_TRIDENT_IDS.contains(weaponId);
    }

    public static boolean isSwampTrident(int weaponId) {
        return SWAMP_TRIDENT_IDS.contains(weaponId);
    }

    public static CombatSpell spellForWeapon(int weaponId) {
        if (isSwampTrident(weaponId)) {
            return CombatSpells.TRIDENT_OF_THE_SWAMP.getSpell();
        }
        if (isSeasTrident(weaponId)) {
            return CombatSpells.TRIDENT_OF_THE_SEAS.getSpell();
        }
        return null;
    }

    public static boolean isTridentSpell(CombatSpell spell) {
        return spell == CombatSpells.TRIDENT_OF_THE_SEAS.getSpell()
                || spell == CombatSpells.TRIDENT_OF_THE_SWAMP.getSpell();
    }

    public static int getMaxHit(Player player, CombatSpell spell) {
        final int magicLevel = player.getSkillManager().getCurrentLevel(Skill.MAGIC);

        if (spell == CombatSpells.TRIDENT_OF_THE_SWAMP.getSpell()) {
            // OSRS base: floor(Magic / 3) - 2, min 4.
            return Math.max(4, (magicLevel / 3) - 2);
        }

        // OSRS base: floor(Magic / 3) - 5, min 1.
        return Math.max(1, (magicLevel / 3) - 5);
    }
}

