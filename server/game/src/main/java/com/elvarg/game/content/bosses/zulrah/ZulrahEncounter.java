package com.elvarg.game.content.bosses.zulrah;

import com.elvarg.game.World;
import com.elvarg.game.entity.impl.npc.impl.Zulrah;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.areas.impl.ZulrahArea;
import com.elvarg.game.model.teleportation.TeleportHandler;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;
import com.elvarg.util.NpcIdentifiers;

/**
 * Handles entering and leaving the Zulrah encounter.
 */
public final class ZulrahEncounter {

    private ZulrahEncounter() {
    }

    public static boolean enter(Player player) {
        if (player.getArea() instanceof ZulrahArea) {
            player.getPacketSender().sendMessage("You are already at Zulrah's shrine.");
            return false;
        }

        if (!TeleportHandler.checkReqs(player, ZulrahArea.PLAYER_START)) {
            return false;
        }

        final ZulrahArea area = new ZulrahArea();
        player.moveTo(ZulrahArea.PLAYER_START.clone());
        area.enter(player);

        TaskManager.submit(new Task(1, player, false) {
            @Override
            protected void execute() {
                if (!player.isRegistered() || area.isDestroyed() || player.getPrivateArea() != area) {
                    stop();
                    return;
                }

                final Zulrah zulrah = new Zulrah(player, area, NpcIdentifiers.ZULRAH, ZulrahArea.ZULRAH_SPAWN.clone());
                World.getAddNPCQueue().add(zulrah);
                stop();
            }
        });

        return true;
    }

    public static void leave(Player player) {
        if (!(player.getArea() instanceof ZulrahArea)) {
            return;
        }
        player.getCombat().reset();
        player.moveTo(ZulrahArea.EXIT_LOCATION.clone());
        player.getPacketSender().sendMessage("You leave Zulrah's shrine.");
    }
}
