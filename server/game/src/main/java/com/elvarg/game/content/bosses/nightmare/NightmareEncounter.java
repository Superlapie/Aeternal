package com.elvarg.game.content.bosses.nightmare;

import com.elvarg.game.World;
import com.elvarg.game.entity.impl.npc.impl.Nightmare;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.areas.impl.NightmareArea;
import com.elvarg.game.model.teleportation.TeleportHandler;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;

/**
 * Handles entering and leaving the Nightmare encounter.
 */
public final class NightmareEncounter {

    public static final int NIGHTMARE_NPC_ID = 9425;

    private NightmareEncounter() {
    }

    public static boolean enter(Player player) {
        if (player.getArea() instanceof NightmareArea) {
            player.getPacketSender().sendMessage("You are already in a Nightmare instance.");
            return false;
        }

        if (!TeleportHandler.checkReqs(player, NightmareArea.PLAYER_START)) {
            return false;
        }

        final NightmareArea area = new NightmareArea();
        player.moveTo(NightmareArea.PLAYER_START.clone());
        area.enter(player);

        TaskManager.submit(new Task(1, player, false) {
            @Override
            protected void execute() {
                if (!player.isRegistered() || area.isDestroyed() || player.getPrivateArea() != area) {
                    stop();
                    return;
                }

                final Nightmare nightmare = new Nightmare(player, area, NIGHTMARE_NPC_ID, NightmareArea.NIGHTMARE_SPAWN.clone());
                World.getAddNPCQueue().add(nightmare);
                player.getPacketSender().sendMessage("The Nightmare stirs...");
                stop();
            }
        });
        return true;
    }

    public static void leave(Player player) {
        if (!(player.getArea() instanceof NightmareArea)) {
            return;
        }
        player.getCombat().reset();
        player.moveTo(NightmareArea.EXIT_LOCATION.clone());
        player.getPacketSender().sendMessage("You leave the Nightmare lair.");
    }
}

