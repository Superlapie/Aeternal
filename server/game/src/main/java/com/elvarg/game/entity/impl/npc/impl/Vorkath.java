package com.elvarg.game.entity.impl.npc.impl;

import com.elvarg.game.World;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.content.combat.method.impl.npcs.VorkathCombatMethod;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.npc.NPCInteraction;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Ids;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.areas.impl.VorkathArea;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;

@Ids({8058, 8059, 8060, 8061})
public class Vorkath extends NPC implements NPCInteraction {

    public static final String SKIP_RESPAWN_ATTRIBUTE = "skip_respawn";
    private static final int RESPAWN_TICKS = 16;
    private static final int SLEEPING_ID = 8059;
    private static final int AWAKE_ID = 8061;
    private static final int WAKE_ANIMATION = 7950;

    private final CombatMethod combatMethod = new VorkathCombatMethod();
    private boolean asleep;
    private boolean respawnQueued;

    public Vorkath(int id, Location position) {
        super(id, position);
        initialise();
    }

    public Vorkath(Player owner, VorkathArea area, int id, Location position) {
        this(id, position);
        setOwner(owner);
        if (area != null) {
            area.enter(this);
        }
    }

    private void initialise() {
        asleep = getRealId() != AWAKE_ID;
        setAttribute(SKIP_RESPAWN_ATTRIBUTE, true);
        getMovementQueue().setBlockMovement(true);
        getMovementCoordinator().setRadius(0);

        if (asleep && getRealId() != SLEEPING_ID) {
            setNpcTransformationId(SLEEPING_ID);
        }
    }

    @Override
    public int aggressionDistance() {
        return 32;
    }

    @Override
    public boolean useProjectileClipping() {
        // Vorkath attacks across his arena and should not deadlock on LoS clips.
        return false;
    }

    @Override
    public CombatMethod getCombatMethod() {
        return combatMethod;
    }

    public boolean isAsleep() {
        return asleep;
    }

    public void queueSleepingRespawn() {
        if (respawnQueued) {
            return;
        }
        respawnQueued = true;

        Player owner = getOwner();
        if (owner == null || !(owner.getPrivateArea() instanceof VorkathArea area)) {
            return;
        }

        TaskManager.submit(new Task(RESPAWN_TICKS, this, false) {
            @Override
            protected void execute() {
                if (!owner.isRegistered() || owner.getPrivateArea() != area || area.isDestroyed()) {
                    stop();
                    return;
                }

                boolean vorkathAlive = area.getNpcs().stream()
                        .anyMatch(n -> n != null && n.isRegistered() && (n.getId() == 8058 || n.getId() == 8059 || n.getId() == 8060 || n.getId() == 8061));

                if (!vorkathAlive) {
                    Vorkath respawned = new Vorkath(owner, area, SLEEPING_ID, VorkathArea.VORKATH_SPAWN.clone());
                    World.getAddNPCQueue().add(respawned);
                    owner.getPacketSender().sendMessage("Vorkath has returned to his slumber.");
                }
                stop();
            }
        });
    }

    private void wake(Player player) {
        if (!asleep) {
            return;
        }
        asleep = false;
        setNpcTransformationId(AWAKE_ID);
        performAnimation(new Animation(WAKE_ANIMATION));

        TaskManager.submit(new Task(2, this, false) {
            @Override
            protected void execute() {
                if (isRegistered() && player.isRegistered() && player.getPrivateArea() == getPrivateArea()) {
                    getCombat().attack(player);
                }
                stop();
            }
        });
    }

    @Override
    public void firstOptionClick(Player player, NPC npc) {
        if (asleep) {
            player.getPacketSender().sendMessage("You poke Vorkath...");
            wake(player);
            return;
        }
        player.getPacketSender().sendMessage("Vorkath is already awake.");
    }

    @Override
    public void secondOptionClick(Player player, NPC npc) {
        if (asleep) {
            firstOptionClick(player, npc);
        }
    }

    @Override
    public void thirdOptionClick(Player player, NPC npc) {
        if (asleep) {
            firstOptionClick(player, npc);
        }
    }

    @Override
    public void forthOptionClick(Player player, NPC npc) {
        if (asleep) {
            firstOptionClick(player, npc);
        }
    }

    @Override
    public void useItemOnNpc(Player player, NPC npc, int itemId, int slot) {
    }
}
