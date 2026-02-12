package com.elvarg.game.entity.impl.npc.impl;

import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.content.combat.method.impl.npcs.ZulrahSnakelingCombatMethod;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Ids;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.areas.impl.ZulrahArea;

import static com.elvarg.util.NpcIdentifiers.SNAKELING;
import static com.elvarg.util.NpcIdentifiers.SNAKELING_2;
import static com.elvarg.util.NpcIdentifiers.SNAKELING_3;

@Ids({SNAKELING, SNAKELING_2, SNAKELING_3})
public class ZulrahSnakeling extends NPC {

    private final CombatMethod combatMethod = new ZulrahSnakelingCombatMethod();

    public ZulrahSnakeling(int id, Location position) {
        super(id, position);
        setAttribute(Zulrah.SKIP_RESPAWN_ATTRIBUTE, true);
    }

    public ZulrahSnakeling(Player owner, ZulrahArea area, int id, Location position) {
        this(id, position);
        setOwner(owner);
        if (area != null) {
            area.enter(this);
        }
    }

    @Override
    public void onAdd() {
        super.onAdd();
        if (getOwner() != null && getOwner().isRegistered() && getOwner().getPrivateArea() == getPrivateArea()) {
            getCombat().attack(getOwner());
        }
    }

    @Override
    public CombatMethod getCombatMethod() {
        return combatMethod;
    }
}
