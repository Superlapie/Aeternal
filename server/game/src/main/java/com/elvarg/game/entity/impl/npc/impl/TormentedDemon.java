package com.elvarg.game.entity.impl.npc.impl;

import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.magic.CombatSpell;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.content.combat.method.impl.npcs.TormentedDemonCombatMethod;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.model.Ids;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.GraphicHeight;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.Priority;
import com.elvarg.util.ItemIdentifiers;
import com.elvarg.util.Misc;

import java.util.ArrayList;
import java.util.List;

@Ids({13593, 13594, 13595, 13596, 13597})
public class TormentedDemon extends NPC {

    public static final int PROTECT_MELEE_ID = 13593;
    public static final int PROTECT_RANGED_ID = 13594;
    public static final int PROTECT_MAGIC_ID = 13595;
    public static final int SHIELD_DOWN_ID = 13596;
    public static final int SPECIAL_STATE_ID = 13597;

    private static final int[] PRAYER_SWAP_HP_THRESHOLDS = {450, 300, 100};
    private static final int PRAYER_SWAP_STALL_TICKS = 6;
    private static final int SHIELD_DOWN_TICKS = 10;
    private static final int SHIELD_BREAK_GFX = 2858;
    private static final int EMBERLIGHT = 29589;
    private static final int EMBERLIGHT_ALT = 29479;
    private static final int SCORCHING_BOW = 29591;
    private static final int SCORCHING_BOW_ALT = 29477;
    private static final int PURGING_STAFF = 29594;
    private static final int BURNING_CLAWS = 29577;
    private final CombatMethod combatMethod = new TormentedDemonCombatMethod();

    private CombatType lastDamageStyle = CombatType.MELEE;
    private int nextPrayerThresholdIndex;
    private int attackStallTicks;
    private int shieldDownTicksRemaining;
    private int shieldGraphicCooldown;

    public TormentedDemon(int id, Location position) {
        super(id, position);
        normalizeInitialState();
    }

    @Override
    public CombatMethod getCombatMethod() {
        return combatMethod;
    }

    @Override
    public int aggressionDistance() {
        return 16;
    }

    @Override
    public void process() {
        super.process();

        if (attackStallTicks > 0) {
            attackStallTicks--;
        }

        if (shieldDownTicksRemaining > 0 && --shieldDownTicksRemaining <= 0) {
            switchProtectionPrayerByLastStyle();
        }

        refreshShieldGraphic();
    }

    @Override
    public PendingHit manipulateHit(PendingHit hit) {
        PendingHit modified = super.manipulateHit(hit);

        if (modified == null || modified.getAttacker() == null || !modified.getAttacker().isPlayer()) {
            return modified;
        }

        if (modified.getTotalDamage() <= 0) {
            return modified;
        }

        Player attacker = modified.getAttacker().getAsPlayer();
        int finalDamage = modified.getTotalDamage();

        // Tormented Demon protection prayer blocks matching incoming styles.
        if (isProtectedState(getId()) && isBlockedByProtectionPrayer(modified.getCombatType(), getId())) {
            finalDamage = 0;
        }

        // Fire shield: non-demonbane/non-abyssal attacks are reduced by 20% while shield is active.
        if (finalDamage > 0 && !isShieldDownState() && !bypassesFireShield(attacker, modified.getCombatType())) {
            finalDamage = (int) Math.floor(finalDamage * 0.80);
        }

        if (isShieldDownState() && isSlowHeavyWeapon(attacker)) {
            finalDamage *= 2;
        }

        modified.setTotalDamage(finalDamage);
        if (finalDamage <= 0) {
            return modified;
        }

        lastDamageStyle = modified.getCombatType();
        checkPrayerSwapThresholds(finalDamage);
        return modified;
    }

    public void enterSpecialState() {
        setStateId(SPECIAL_STATE_ID);
    }

    public void enterShieldDownWindow() {
        shieldDownTicksRemaining = SHIELD_DOWN_TICKS;
        setStateId(SHIELD_DOWN_ID);
        shieldGraphicCooldown = 0;
        performGraphic(new Graphic(SHIELD_BREAK_GFX, GraphicHeight.LOW, Priority.HIGH));
    }

    public boolean isShieldDownState() {
        return getId() == SHIELD_DOWN_ID;
    }

    public static boolean isTormentedDemon(NPC npc) {
        return npc != null && isTormentedDemonId(npc.getId());
    }

    public static boolean isTormentedDemonId(int id) {
        return id >= PROTECT_MELEE_ID && id <= SPECIAL_STATE_ID;
    }

    public static boolean isShieldDown(NPC npc) {
        return npc != null && npc.getId() == SHIELD_DOWN_ID;
    }

    public boolean isAttackStalled() {
        return attackStallTicks > 0;
    }

    private void normalizeInitialState() {
        if (!isTormentedDemonId(getId()) || getId() == SHIELD_DOWN_ID || getId() == SPECIAL_STATE_ID) {
            setProtectionState(ProtectionState.MELEE);
        } else {
            setHeadIcon(headIconForState(getId()));
        }
        syncPrayerThresholdCursor();
    }

    private void switchProtectionPrayerByLastStyle() {
        shieldDownTicksRemaining = 0;
        ProtectionState nextState = protectionStateForCombatStyle(lastDamageStyle);
        setProtectionState(nextState);
        attackStallTicks = PRAYER_SWAP_STALL_TICKS;
    }

    private void setProtectionState(ProtectionState state) {
        int targetId;
        switch (state) {
            case MELEE:
                targetId = PROTECT_MELEE_ID;
                break;
            case RANGED:
                targetId = PROTECT_RANGED_ID;
                break;
            case MAGIC:
            default:
                targetId = PROTECT_MAGIC_ID;
                break;
        }
        setStateId(targetId);
        shieldGraphicCooldown = 0;
        refreshShieldGraphic();
    }

    private void setStateId(int id) {
        // Keep TD in explicit transformed state for all phase/prayer ids to avoid
        // base<->transformed flicker transitions.
        setNpcTransformationId(id);
        setHeadIcon(headIconForState(id));
    }

    private void refreshShieldGraphic() {
        if (!isProtectedState(getId())) {
            return;
        }
        if (shieldGraphicCooldown > 0) {
            shieldGraphicCooldown--;
            return;
        }
        int shieldGfx = shieldLoopGfxForState(getId());
        if (shieldGfx != -1) {
            performGraphic(new Graphic(shieldGfx, GraphicHeight.LOW, Priority.HIGH));
        }
        shieldGraphicCooldown = 4;
    }

    private void checkPrayerSwapThresholds(int damageApplied) {
        if (damageApplied <= 0) {
            return;
        }
        int projectedHp = Math.max(0, getHitpointsAfterPendingDamage() - damageApplied);
        boolean crossed = false;
        while (nextPrayerThresholdIndex < PRAYER_SWAP_HP_THRESHOLDS.length
                && projectedHp <= PRAYER_SWAP_HP_THRESHOLDS[nextPrayerThresholdIndex]) {
            nextPrayerThresholdIndex++;
            crossed = true;
        }
        if (crossed) {
            switchProtectionPrayerByLastStyle();
        }
    }

    private void syncPrayerThresholdCursor() {
        int hp = getHitpoints();
        nextPrayerThresholdIndex = 0;
        while (nextPrayerThresholdIndex < PRAYER_SWAP_HP_THRESHOLDS.length
                && hp <= PRAYER_SWAP_HP_THRESHOLDS[nextPrayerThresholdIndex]) {
            nextPrayerThresholdIndex++;
        }
    }

    private static ProtectionState protectionStateForCombatStyle(CombatType style) {
        if (style == CombatType.RANGED) {
            return ProtectionState.RANGED;
        }
        if (style == CombatType.MAGIC) {
            return ProtectionState.MAGIC;
        }
        return ProtectionState.MELEE;
    }

    private static int headIconForState(int stateId) {
        switch (stateId) {
            case PROTECT_MELEE_ID:
                return 0;
            case PROTECT_RANGED_ID:
                return 1;
            case PROTECT_MAGIC_ID:
                return 2;
            default:
                return -1;
        }
    }

    private static int shieldLoopGfxForState(int stateId) {
        switch (stateId) {
            case PROTECT_MELEE_ID:
                return 2847;
            case PROTECT_RANGED_ID:
                return 2848;
            case PROTECT_MAGIC_ID:
                return 2849;
            default:
                return -1;
        }
    }

    private static boolean isProtectedState(int stateId) {
        return stateId == PROTECT_MELEE_ID
                || stateId == PROTECT_RANGED_ID
                || stateId == PROTECT_MAGIC_ID;
    }

    private static boolean isBlockedByProtectionPrayer(CombatType incomingStyle, int currentStateId) {
        switch (currentStateId) {
            case PROTECT_MELEE_ID:
                return incomingStyle == CombatType.MELEE;
            case PROTECT_RANGED_ID:
                return incomingStyle == CombatType.RANGED;
            case PROTECT_MAGIC_ID:
                return incomingStyle == CombatType.MAGIC;
            default:
                return false;
        }
    }

    private static boolean bypassesFireShield(Player attacker, CombatType style) {
        int weaponId = attacker.getEquipment().getWeapon().getId();
        if (weaponId == EMBERLIGHT || weaponId == EMBERLIGHT_ALT
                || weaponId == SCORCHING_BOW || weaponId == SCORCHING_BOW_ALT
                || weaponId == PURGING_STAFF || weaponId == BURNING_CLAWS) {
            return true;
        }

        String weaponName = attacker.getEquipment().getWeapon().getDefinition().getName();
        if (weaponName != null && weaponName.toLowerCase().contains("abyssal")) {
            return true;
        }

        if (style == CombatType.MAGIC) {
            CombatSpell spell = attacker.getCombat().getCastSpell();
            if (spell != null) {
                int spellId = spell.spellId();
                if (spellId == 30645 || spellId == 30649 || spellId == 30653) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isSlowHeavyWeapon(Player attacker) {
        int weaponId = attacker.getEquipment().getWeapon().getId();
        if (weaponId == ItemIdentifiers.ELDER_MAUL
                || weaponId == ItemIdentifiers.ELDER_MAUL_2
                || weaponId == ItemIdentifiers.ELDER_MAUL_3
                || weaponId == ItemIdentifiers.ELDER_MAUL_4
                || weaponId == ItemIdentifiers.HEAVY_BALLISTA
                || weaponId == ItemIdentifiers.HEAVY_BALLISTA_2) {
            return true;
        }
        return attacker.getBaseAttackSpeed() >= 6;
    }

    private enum ProtectionState {
        MELEE,
        RANGED,
        MAGIC
    }
}
