package com.elvarg.game.entity.impl.npc.impl;

import com.elvarg.game.World;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Ids;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.areas.impl.NightmareArea;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;

/**
 * Minimal Nightmare boss scaffold for private instances.
 */
@Ids({9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433})
public class Nightmare extends NPC {

    public static final String SKIP_RESPAWN_ATTRIBUTE = "skip_respawn";
    private static final int RESPAWN_TICKS = 30;
    private boolean respawnQueued;

    public Nightmare(int id, Location position) {
        super(id, position);
        initialise();
    }

    public Nightmare(Player owner, NightmareArea area, int id, Location position) {
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
    public void appendDeath() {
        queueRespawn();
        super.appendDeath();
    }

    private void queueRespawn() {
        if (respawnQueued) {
            return;
        }
        respawnQueued = true;

        Player owner = getOwner();
        if (owner == null || !(owner.getPrivateArea() instanceof NightmareArea area)) {
            return;
        }

        TaskManager.submit(new Task(RESPAWN_TICKS, this, false) {
            @Override
            protected void execute() {
                if (!owner.isRegistered() || owner.getPrivateArea() != area || area.isDestroyed()) {
                    stop();
                    return;
                }

                boolean nightmareAlive = area.getNpcs().stream()
                        .anyMatch(n -> n != null && n.isRegistered()
                                && n.getId() >= 9425 && n.getId() <= 9433);

                if (!nightmareAlive) {
                    Nightmare respawned = new Nightmare(owner, area, 9425, NightmareArea.NIGHTMARE_SPAWN.clone());
                    World.getAddNPCQueue().add(respawned);
                    owner.getPacketSender().sendMessage("The Nightmare reforms in the darkness.");
                }
                stop();
            }
        });
    }
}

