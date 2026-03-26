package com.elvarg.game.content.combat.formula;

import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Skill;
import com.elvarg.game.model.areas.Area;
import com.elvarg.game.model.equipment.BonusManager;
import com.elvarg.util.ItemIdentifiers;

final class TwistedBowData {

    private static final int DEFAULT_MAGIC_CAP = 250;
    private static final int BOOSTED_MAGIC_CAP = 350;

    private TwistedBowData() {
    }

    static boolean isTwistedBowEquipped(Player player) {
        return player != null && player.getEquipment().hasAt(com.elvarg.game.model.container.impl.Equipment.WEAPON_SLOT,
                ItemIdentifiers.TWISTED_BOW);
    }

    static double rangedAccuracyMultiplier(Player attacker, Mobile target) {
        return 1.0 + twistedBowAccuracyBonus(attacker, target) / 100.0;
    }

    static double rangedDamageMultiplier(Player attacker, Mobile target) {
        return 1.0 + twistedBowDamageBonus(attacker, target) / 100.0;
    }

    static int twistedBowAccuracyBonus(Player attacker, Mobile target) {
        int magic = twistedBowMagicLevel(attacker, target);
        double bonus = 140.0 + ((3.0 * magic - 10.0) / 100.0)
                - Math.pow(((3.0 * magic / 10.0) - 100.0), 2) / 100.0;
        return (int) Math.min(140, Math.max(0, Math.floor(bonus)));
    }

    static int twistedBowDamageBonus(Player attacker, Mobile target) {
        int magic = twistedBowMagicLevel(attacker, target);
        double bonus = 250.0 + ((3.0 * magic - 14.0) / 100.0)
                - Math.pow(((3.0 * magic / 10.0) - 140.0), 2) / 100.0;
        return (int) Math.max(0, Math.floor(bonus));
    }

    private static int twistedBowMagicLevel(Player attacker, Mobile target) {
        int magic = 0;

        if (target != null) {
            if (target.isPlayer()) {
                Player victim = target.getAsPlayer();
                magic = Math.max(victim.getSkillManager().getCurrentLevel(Skill.MAGIC),
                        victim.getBonusManager().getAttackBonus()[BonusManager.ATTACK_MAGIC]);
            } else if (target.isNpc()) {
                magic = target.getAsNpc().getCurrentDefinition().getStats()[4];
            }
        }

        int cap = twistedBowMagicCap(attacker);
        return Math.max(0, Math.min(magic, cap));
    }

    private static int twistedBowMagicCap(Player attacker) {
        if (attacker == null) {
            return DEFAULT_MAGIC_CAP;
        }

        Area area = attacker.getArea();
        if (area == null) {
            return DEFAULT_MAGIC_CAP;
        }

        String areaName = area.getName().toLowerCase();
        if (areaName.contains("chambers")
                || areaName.contains("xeric")
                || areaName.contains("tombs")
                || areaName.contains("amascut")
                || areaName.contains("nightmarezone")
                || areaName.contains("nmz")) {
            return BOOSTED_MAGIC_CAP;
        }

        return DEFAULT_MAGIC_CAP;
    }
}
