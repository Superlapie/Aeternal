package com.elvarg.game.content.bosses.nightmare;

import com.elvarg.game.World;
import com.elvarg.game.entity.impl.npc.impl.Nightmare;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.areas.impl.NightmareArea;
import com.elvarg.game.model.teleportation.TeleportHandler;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;

/**
 * Handles entering and leaving the Nightmare encounter.
 */
public final class NightmareEncounter {

    public static final int NIGHTMARE_NPC_ID = 9425;
    private static final int TRANSITION_TOTAL_TICKS = 5;

    private NightmareEncounter() {
    }

    public static boolean enter(Player player) {
        return enter(player, NightmareArea.SANCTUARY_WAKE_TILE);
    }

    public static boolean enter(Player player, Location wakeTile) {
        if (player.getArea() instanceof NightmareArea) {
            player.getPacketSender().sendMessage("You are already in a Nightmare instance.");
            return false;
        }

        if (!TeleportHandler.checkReqs(player, NightmareArea.PLAYER_START)) {
            return false;
        }

        final NightmareArea area = new NightmareArea();
        final Location mappedSpawn = NightmareArea.mapWakeTileToFight(wakeTile);

        TaskManager.submit(new Task(1, player, false) {
            int tick = 0;

            @Override
            protected void execute() {
                if (!player.isRegistered() || area.isDestroyed()) {
                    cleanup();
                    stop();
                    return;
                }

                switch (tick) {
                    case 0 -> {
                        player.getPacketSender().sendWalkableInterface(NightmareArea.FADE_SOFT_INTERFACE_ID);
                        player.getMovementQueue().setBlockMovement(true).reset();
                        player.setUntargetable(true);
                        player.setTeleporting(true);
                    }
                    case 1 -> player.getPacketSender().sendWalkableInterface(NightmareArea.FADE_FULL_INTERFACE_ID);
                    case 2 -> {
                        player.moveTo(NightmareArea.PLAYER_START.clone());
                        area.enter(player);
                        final Nightmare nightmare = new Nightmare(player, area, NIGHTMARE_NPC_ID, mappedSpawn);
                        World.getAddNPCQueue().add(nightmare);
                        player.getPacketSender().sendMessage("You shake The Nightmare awake...");
                    }
                    case 3 -> player.getPacketSender().sendWalkableInterface(NightmareArea.FADE_MID_INTERFACE_ID);
                    case 4 -> {
                        player.getPacketSender().sendWalkableInterface(-1);
                        cleanup();
                    }
                }

                tick++;
                if (tick >= TRANSITION_TOTAL_TICKS) {
                    stop();
                }
            }

            private void cleanup() {
                player.getPacketSender().sendWalkableInterface(-1);
                player.getMovementQueue().setBlockMovement(false).reset();
                player.setUntargetable(false);
                player.setTeleporting(false);
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
