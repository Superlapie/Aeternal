package com.elvarg.game.entity.impl.npc.impl;

import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.content.combat.method.impl.npcs.DemonicGorillaCombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Ids;
import com.elvarg.game.model.Location;

@Ids({7144, 7145, 7146, 7147, 7148, 7149, 7152})
public class DemonicGorilla extends NPC {

    public static final int PROTECT_MELEE_ID = 7144;
    public static final int PROTECT_RANGED_ID = 7145;
    public static final int PROTECT_MAGIC_ID = 7146;
    public static final int NO_PRAYER_ID = 7147;

    private static final int PRAYER_DAMAGE_THRESHOLD = 50;

    private final CombatMethod combatMethod = new DemonicGorillaCombatMethod();
    private ProtectionState protectionState = ProtectionState.NONE;
    private final int[] damageByStyle = new int[3];

    public DemonicGorilla(int id, Location position) {
        super(id, position);
        initialiseState(id);
    }

    @Override
    public CombatMethod getCombatMethod() {
        return combatMethod;
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

        CombatType combatType = modified.getCombatType();
        if (isProtectedAgainst(combatType)) {
            modified.setTotalDamage(0);
            return modified;
        }

        int damage = modified.getTotalDamage();
        damageByStyle[indexFor(combatType)] += damage;

        if (damageByStyle[indexFor(combatType)] >= PRAYER_DAMAGE_THRESHOLD) {
            damageByStyle[indexFor(combatType)] = 0;
            switchProtectionPrayer(combatType);
        }

        return modified;
    }

    private void initialiseState(int id) {
        switch (id) {
            case PROTECT_MELEE_ID -> setProtectionState(ProtectionState.MELEE);
            case PROTECT_RANGED_ID -> setProtectionState(ProtectionState.RANGED);
            case PROTECT_MAGIC_ID -> setProtectionState(ProtectionState.MAGIC);
            default -> setProtectionState(ProtectionState.NONE);
        }
    }

    private void switchProtectionPrayer(CombatType combatType) {
        switch (combatType) {
            case MELEE -> setProtectionState(ProtectionState.MELEE);
            case RANGED -> setProtectionState(ProtectionState.RANGED);
            case MAGIC -> setProtectionState(ProtectionState.MAGIC);
        }
    }

    private void setProtectionState(ProtectionState state) {
        protectionState = state;
        switch (state) {
            case MELEE -> setStateId(PROTECT_MELEE_ID, 0);
            case RANGED -> setStateId(PROTECT_RANGED_ID, 1);
            case MAGIC -> setStateId(PROTECT_MAGIC_ID, 2);
            case NONE -> setStateId(NO_PRAYER_ID, -1);
        }
    }

    private void setStateId(int id, int headIcon) {
        setNpcTransformationId(id);
        setHeadIcon(headIcon);
    }

    private boolean isProtectedAgainst(CombatType combatType) {
        return switch (protectionState) {
            case MELEE -> combatType == CombatType.MELEE;
            case RANGED -> combatType == CombatType.RANGED;
            case MAGIC -> combatType == CombatType.MAGIC;
            case NONE -> false;
        };
    }

    private int indexFor(CombatType combatType) {
        return switch (combatType) {
            case MELEE -> 0;
            case RANGED -> 1;
            case MAGIC -> 2;
        };
    }

    private enum ProtectionState {
        NONE,
        MELEE,
        RANGED,
        MAGIC
    }
}
