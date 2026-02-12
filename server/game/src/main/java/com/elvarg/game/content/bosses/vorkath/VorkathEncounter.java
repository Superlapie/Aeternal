package com.elvarg.game.content.bosses.vorkath;

import com.elvarg.game.World;
import com.elvarg.game.entity.impl.npc.impl.Vorkath;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.areas.impl.VorkathArea;
import com.elvarg.game.model.teleportation.TeleportHandler;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;

/**
 * Handles entering and leaving the Vorkath encounter.
 */
public final class VorkathEncounter {

    private static final int VORKATH_SLEEPING_NPC_ID = 8059;

    private VorkathEncounter() {
    }

    public static boolean enter(Player player) {
        if (player.getArea() instanceof VorkathArea) {
            player.getPacketSender().sendMessage("You are already in a Vorkath instance.");
            return false;
        }

        if (!TeleportHandler.checkReqs(player, VorkathArea.PLAYER_START)) {
            return false;
        }

        final VorkathArea area = new VorkathArea();
        player.moveTo(VorkathArea.PLAYER_START.clone());
        area.enter(player);

        TaskManager.submit(new Task(1, player, false) {
            @Override
            protected void execute() {
                if (!player.isRegistered() || area.isDestroyed() || player.getPrivateArea() != area) {
                    stop();
                    return;
                }

                final Vorkath vorkath = new Vorkath(player, area, VORKATH_SLEEPING_NPC_ID, VorkathArea.VORKATH_SPAWN.clone());
                World.getAddNPCQueue().add(vorkath);
                stop();
            }
        });
        return true;
    }

    public static void leave(Player player) {
        if (!(player.getArea() instanceof VorkathArea)) {
            return;
        }
        player.getCombat().reset();
        player.moveTo(VorkathArea.EXIT_LOCATION.clone());
        player.getPacketSender().sendMessage("You leave Vorkath's island.");
    }
}
