package com.elvarg.game.content.npc;

import com.elvarg.game.World;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.model.Location;
import com.elvarg.util.NpcIdentifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Maintains a fixed population of implings inside the custom fenced habitat.
 * Rules:
 * - 30 implings active when possible.
 * - On catch/despawn: 1 minute respawn cooldown.
 * - Natural despawn if not caught: random 3-4 minutes.
 */
public final class AreaImplingService {

    private static final int TARGET_POPULATION = 30;
    private static final long RESPAWN_DELAY_MS = 60_000L;
    private static final long DESPAWN_MIN_MS = 180_000L;
    private static final long DESPAWN_MAX_MS = 240_000L;

    // Interior of enclosure around 5855,5886.
    private static final int MIN_X = 5849;
    private static final int MAX_X = 5861;
    private static final int MIN_Y = 5880;
    private static final int MAX_Y = 5892;
    private static final int Z = 0;

    // Excludes dragon and lucky implings as requested.
    private static final int[] IMPLING_IDS = {
            NpcIdentifiers.BABY_IMPLING,
            NpcIdentifiers.YOUNG_IMPLING,
            NpcIdentifiers.GOURMET_IMPLING,
            NpcIdentifiers.EARTH_IMPLING,
            NpcIdentifiers.ESSENCE_IMPLING,
            NpcIdentifiers.ECLECTIC_IMPLING,
            NpcIdentifiers.NATURE_IMPLING,
            NpcIdentifiers.MAGPIE_IMPLING,
            NpcIdentifiers.NINJA_IMPLING
    };

    private static final List<Slot> SLOTS = new ArrayList<>(TARGET_POPULATION);
    private static volatile boolean initialized;

    private AreaImplingService() {
    }

    public static void initialize() {
        SLOTS.clear();
        for (int i = 0; i < TARGET_POPULATION; i++) {
            Slot slot = new Slot();
            slot.location = randomLocation();
            slot.respawnAt = 0L;
            SLOTS.add(slot);
        }
        initialized = true;
    }

    public static void process() {
        if (!initialized) {
            initialize();
        }

        long now = System.currentTimeMillis();
        for (Slot slot : SLOTS) {
            // Active slot handling.
            if (slot.npc != null) {
                if (!slot.npc.isRegistered()) {
                    // Caught or otherwise removed.
                    slot.npc = null;
                    slot.respawnAt = now + RESPAWN_DELAY_MS;
                    continue;
                }

                if (now >= slot.despawnAt) {
                    World.getRemoveNPCQueue().add(slot.npc);
                    slot.npc = null;
                    slot.respawnAt = now + RESPAWN_DELAY_MS;
                    continue;
                }
            }

            // Respawn handling.
            if (slot.npc == null && now >= slot.respawnAt && !World.getNpcs().isFull()) {
                slot.location = randomLocation();
                int npcId = IMPLING_IDS[ThreadLocalRandom.current().nextInt(IMPLING_IDS.length)];
                NPC npc = NPC.create(npcId, slot.location.clone());
                npc.getMovementCoordinator().setRadius(2);
                World.getAddNPCQueue().add(npc);
                slot.npc = npc;
                slot.despawnAt = now + randomDespawnDuration();
            }
        }
    }

    private static long randomDespawnDuration() {
        return ThreadLocalRandom.current().nextLong(DESPAWN_MIN_MS, DESPAWN_MAX_MS + 1);
    }

    private static Location randomLocation() {
        int x = ThreadLocalRandom.current().nextInt(MIN_X, MAX_X + 1);
        int y = ThreadLocalRandom.current().nextInt(MIN_Y, MAX_Y + 1);
        return new Location(x, y, Z);
    }

    private static final class Slot {
        private Location location;
        private NPC npc;
        private long respawnAt;
        private long despawnAt;
    }
}

