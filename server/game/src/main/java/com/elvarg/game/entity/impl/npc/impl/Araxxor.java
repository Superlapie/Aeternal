package com.elvarg.game.entity.impl.npc.impl;

import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.content.combat.method.impl.npcs.AraxxorCombatMethod;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.model.Ids;
import com.elvarg.game.model.Location;

@Ids({13668})
public class Araxxor extends NPC {

    private static final CombatMethod COMBAT_METHOD = new AraxxorCombatMethod();

    public Araxxor(int id, Location position) {
        super(id, position);
    }

    @Override
    public CombatMethod getCombatMethod() {
        return COMBAT_METHOD;
    }

    @Override
    public int aggressionDistance() {
        return 16;
    }
}

