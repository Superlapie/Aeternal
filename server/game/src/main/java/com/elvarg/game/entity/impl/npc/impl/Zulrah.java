package com.elvarg.game.entity.impl.npc.impl;

import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.content.combat.method.impl.npcs.ZulrahCombatMethod;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Ids;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.areas.impl.ZulrahArea;

import static com.elvarg.util.NpcIdentifiers.ZULRAH;
import static com.elvarg.util.NpcIdentifiers.ZULRAH_2;
import static com.elvarg.util.NpcIdentifiers.ZULRAH_3;

@Ids({ZULRAH, ZULRAH_2, ZULRAH_3})
public class Zulrah extends NPC {

    public static final String SKIP_RESPAWN_ATTRIBUTE = "skip_respawn";

    private final CombatMethod combatMethod = new ZulrahCombatMethod();

    public Zulrah(int id, Location position) {
        super(id, position);
        initialise();
    }

    public Zulrah(Player owner, ZulrahArea area, int id, Location position) {
        this(id, position);
        setOwner(owner);
        if (area != null) {
            area.enter(this);
        }
    }

    private void initialise() {
        setAttribute(SKIP_RESPAWN_ATTRIBUTE, true);
        getMovementQueue().setBlockMovement(true);
        getMovementCoordinator().setRadius(0);
    }

    @Override
    public int aggressionDistance() {
        return 32;
    }

    @Override
    public boolean useProjectileClipping() {
        // Zulrah attacks over the shrine water and should not deadlock on map LoS clipping.
        return false;
    }

    @Override
    public CombatMethod getCombatMethod() {
        return combatMethod;
    }
}
