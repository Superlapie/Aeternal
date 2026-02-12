package com.elvarg.game.entity.impl.npc.impl;

import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Ids;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.areas.impl.VorkathArea;

@Ids({8062, 8063})
public class ZombifiedSpawn extends NPC {

    public static final String SKIP_RESPAWN_ATTRIBUTE = "skip_respawn";

    public ZombifiedSpawn(int id, Location position) {
        super(id, position);
        setAttribute(SKIP_RESPAWN_ATTRIBUTE, true);
    }

    public ZombifiedSpawn(Player owner, VorkathArea area, int id, Location position) {
        this(id, position);
        setOwner(owner);
        if (area != null) {
            area.enter(this);
        }
    }

    @Override
    public boolean canUsePathFinding() {
        return true;
    }

    @Override
    public int aggressionDistance() {
        return 16;
    }
}
