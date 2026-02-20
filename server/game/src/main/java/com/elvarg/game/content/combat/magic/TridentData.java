package com.elvarg.game.content.combat.magic;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.GraphicHeight;
import com.elvarg.game.model.Skill;
import com.elvarg.game.model.container.impl.Equipment;
import com.elvarg.util.ItemIdentifiers;
import com.elvarg.util.Misc;

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

    private static final Set<Integer> SANGUINESTI_STAFF_IDS = Set.of(
            ItemIdentifiers.SANGUINESTI_STAFF,
            ItemIdentifiers.SANGUINESTI_STAFF_2,
            ItemIdentifiers.HOLY_SANGUINESTI_STAFF,
            ItemIdentifiers.HOLY_SANGUINESTI_STAFF_2
    );

    private TridentData() {
    }

    public static boolean isTridentWeapon(int weaponId) {
        return isSeasTrident(weaponId) || isSwampTrident(weaponId) || isSanguinestiWeapon(weaponId);
    }

    public static boolean isSeasTrident(int weaponId) {
        return SEAS_TRIDENT_IDS.contains(weaponId);
    }

    public static boolean isSwampTrident(int weaponId) {
        return SWAMP_TRIDENT_IDS.contains(weaponId);
    }

    public static boolean isSanguinestiWeapon(int weaponId) {
        return SANGUINESTI_STAFF_IDS.contains(weaponId);
    }

    public static CombatSpell spellForWeapon(int weaponId) {
        if (isSanguinestiWeapon(weaponId)) {
            return CombatSpells.SANGUINESTI_STAFF.getSpell();
        }
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
                || spell == CombatSpells.TRIDENT_OF_THE_SWAMP.getSpell()
                || spell == CombatSpells.SANGUINESTI_STAFF.getSpell();
    }

    public static int getMaxHit(Player player, CombatSpell spell) {
        final int magicLevel = player.getSkillManager().getCurrentLevel(Skill.MAGIC);
        final int weaponId = player.getEquipment().get(Equipment.WEAPON_SLOT).getId();

        if (isSanguinestiWeapon(weaponId)) {
            // OSRS: floor(Magic level / 3) - 1, minimum 5.
            return Math.max(5, (magicLevel / 3) - 1);
        }

        if (spell == CombatSpells.TRIDENT_OF_THE_SWAMP.getSpell()) {
            // OSRS base: floor(Magic / 3) - 2, min 4.
            return Math.max(4, (magicLevel / 3) - 2);
        }

        // OSRS base: floor(Magic / 3) - 5, min 1.
        return Math.max(1, (magicLevel / 3) - 5);
    }

    public static void trySanguinestiHeal(Player player, boolean accurate, int damage) {
        if (!accurate || damage <= 0) {
            return;
        }

        int weaponId = player.getEquipment().get(Equipment.WEAPON_SLOT).getId();
        if (!isSanguinestiWeapon(weaponId)) {
            return;
        }

        // OSRS: 1/6 chance to heal for 50% of damage dealt.
        if (Misc.getRandom(5) == 0) {
            int healAmount = damage / 2;
            if (healAmount > 0) {
                player.heal(healAmount);
                player.performGraphic(new Graphic(1542, GraphicHeight.HIGH));
            }
        }
    }
}
