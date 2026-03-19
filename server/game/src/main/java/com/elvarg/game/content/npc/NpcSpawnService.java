package com.elvarg.game.content.npc;

import com.elvarg.game.World;
import com.elvarg.game.definition.NpcSpawnDefinition;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Direction;

import java.util.*;

/**
 * Region-based NPC activation so we can keep a large spawn dataset on disk
 * while staying inside the 14-bit protocol NPC index limits at runtime.
 */
public final class NpcSpawnService {

    private static final int ACTIVE_REGION_RADIUS = 1; // 3x3 around each player
    private static final String SKIP_RESPAWN_ATTRIBUTE = "skip_respawn";
    private static final long TICK_MS = 600L;

    private static final Map<Integer, List<NpcSpawnDefinition>> spawnsByRegion = new HashMap<>();
    private static final Map<Long, NPC> activeBySpawnKey = new HashMap<>();
    private static final Map<Long, Long> respawnAt = new HashMap<>();
    private static final Set<Long> managedSpawnKeys = new HashSet<>();
    private static volatile boolean initialized = false;

    private NpcSpawnService() {
    }

    public static void initialize(NpcSpawnDefinition[] defs) {
        spawnsByRegion.clear();
        activeBySpawnKey.clear();
        respawnAt.clear();
        managedSpawnKeys.clear();
        if (defs == null) {
            initialized = true;
            return;
        }
        for (NpcSpawnDefinition def : defs) {
            int region = regionId(def.getPosition().getX(), def.getPosition().getY());
            spawnsByRegion.computeIfAbsent(region, r -> new ArrayList<>()).add(def);
            managedSpawnKeys.add(spawnKey(def));
        }
        initialized = true;
    }

    public static void process() {
        if (!initialized) {
            return;
        }

        Set<Integer> requiredRegions = getRequiredRegions();
        activateRequired(requiredRegions);
        deactivateOutOfRange(requiredRegions);
    }

    private static Set<Integer> getRequiredRegions() {
        Set<Integer> regions = new HashSet<>();
        for (Player player : World.getPlayers()) {
            if (player == null) {
                continue;
            }
            int baseX = player.getLocation().getX() >> 6;
            int baseY = player.getLocation().getY() >> 6;
            for (int dx = -ACTIVE_REGION_RADIUS; dx <= ACTIVE_REGION_RADIUS; dx++) {
                for (int dy = -ACTIVE_REGION_RADIUS; dy <= ACTIVE_REGION_RADIUS; dy++) {
                    int rx = baseX + dx;
                    int ry = baseY + dy;
                    if (rx < 0 || ry < 0 || rx > 255 || ry > 255) {
                        continue;
                    }
                    regions.add((rx << 8) | ry);
                }
            }
        }
        return regions;
    }

    private static void activateRequired(Set<Integer> requiredRegions) {
        long now = System.currentTimeMillis();
        for (Integer region : requiredRegions) {
            List<NpcSpawnDefinition> spawns = spawnsByRegion.get(region);
            if (spawns == null || spawns.isEmpty()) {
                continue;
            }
            for (NpcSpawnDefinition def : spawns) {
                if (World.getNpcs().isFull()) {
                    return;
                }
                long key = spawnKey(def);
                if (activeBySpawnKey.containsKey(key)) {
                    continue;
                }
                Long readyAt = respawnAt.get(key);
                if (readyAt != null && now < readyAt) {
                    continue;
                }
                respawnAt.remove(key);
                NPC npc = NPC.create(def.getId(), def.getPosition());
                npc.getMovementCoordinator().setRadius(def.getRadius());
                npc.setFace(def.getFacing() == null ? Direction.SOUTH : def.getFacing());
                npc.setAttribute(SKIP_RESPAWN_ATTRIBUTE, true);
                if (World.getNpcs().add(npc)) {
                    activeBySpawnKey.put(key, npc);
                }
            }
        }
    }

    private static void deactivateOutOfRange(Set<Integer> requiredRegions) {
        Iterator<Map.Entry<Long, NPC>> it = activeBySpawnKey.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, NPC> entry = it.next();
            NPC npc = entry.getValue();
            if (npc == null || !npc.isRegistered()) {
                it.remove();
                continue;
            }
            int region = regionId(npc.getLocation().getX(), npc.getLocation().getY());
            if (requiredRegions.contains(region)) {
                continue;
            }
            // Keep active if in combat with a player.
            if (npc.getCombat().getTarget() != null) {
                continue;
            }
            World.getNpcs().remove(npc);
            it.remove();
        }
    }

    private static int regionId(int x, int y) {
        return ((x >> 6) << 8) | (y >> 6);
    }

    private static long spawnKey(NpcSpawnDefinition def) {
        long id = def.getId() & 0x3FFFL;
        long x = def.getPosition().getX() & 0x3FFFL;
        long y = def.getPosition().getY() & 0x3FFFL;
        long z = def.getPosition().getZ() & 0x3L;
        return (id << 32) | (x << 18) | (y << 4) | z;
    }

    public static void onManagedNpcDeath(NPC npc, int respawnTicks) {
        if (!initialized || npc == null || npc.getSpawnPosition() == null) {
            return;
        }
        long key = spawnKey(npc.getRealId(), npc.getSpawnPosition().getX(), npc.getSpawnPosition().getY(), npc.getSpawnPosition().getZ());
        if (!managedSpawnKeys.contains(key)) {
            return;
        }
        activeBySpawnKey.remove(key);
        long cooldown = Math.max(1L, (long) respawnTicks) * TICK_MS;
        respawnAt.put(key, System.currentTimeMillis() + cooldown);
    }

    private static long spawnKey(int id, int x, int y, int z) {
        long idPart = id & 0x3FFFL;
        long xPart = x & 0x3FFFL;
        long yPart = y & 0x3FFFL;
        long zPart = z & 0x3L;
        return (idPart << 32) | (xPart << 18) | (yPart << 4) | zPart;
    }
}
