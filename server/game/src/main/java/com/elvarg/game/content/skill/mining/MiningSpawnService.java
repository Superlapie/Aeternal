package com.elvarg.game.content.skill.mining;

import com.elvarg.game.entity.impl.object.GameObject;
import com.elvarg.game.entity.impl.object.ObjectManager;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Injects a baseline set of visible mining rocks for core low-level mines.
 * This is a compatibility layer for bases whose map/object cache does not
 * currently provide visible ore rocks in-world.
 */
public final class MiningSpawnService {

    private static boolean initialized = false;
    private static final List<GameObject> SPAWNED_ROCKS = new ArrayList<>();

    private static final int[][] ROCK_SPAWNS = {
            // Lumbridge south-east mine (classic footprint): compact copper/tin field east of the swamp path.
            {10943, 3145, 3147, 0}, {11161, 3147, 3147, 0}, {10943, 3149, 3148, 0},
            {11161, 3146, 3149, 0}, {10943, 3148, 3150, 0},
            {11360, 3146, 3146, 0}, {11361, 3148, 3146, 0}, {11360, 3150, 3147, 0},
            {11361, 3147, 3151, 0}, {11360, 3149, 3151, 0},

            // Lumbridge south-west mine (classic footprint): coal/iron just west of the south-east field.
            {11364, 3156, 3148, 0}, {11365, 3158, 3147, 0}, {11364, 3160, 3148, 0},
            {11366, 3157, 3151, 0}, {11367, 3159, 3151, 0}, {11366, 3161, 3150, 0},

            // Varrock south-east mine: 9 copper, 6 tin, 4 iron in a tight S-shaped field.
            {10943, 3283, 3363, 0}, {11161, 3285, 3363, 0}, {10943, 3287, 3364, 0},
            {11161, 3289, 3365, 0}, {10943, 3291, 3366, 0}, {11161, 3292, 3368, 0},
            {10943, 3291, 3370, 0}, {11161, 3289, 3371, 0}, {10943, 3287, 3372, 0},
            {11360, 3284, 3366, 0}, {11361, 3286, 3366, 0}, {11360, 3288, 3367, 0},
            {11361, 3290, 3368, 0}, {11360, 3288, 3370, 0}, {11361, 3286, 3371, 0},
            {11364, 3293, 3371, 0}, {11365, 3295, 3370, 0}, {11364, 3296, 3368, 0}, {11365, 3294, 3366, 0},

            // Varrock south-west mine: corrected back north; keep the eastward shift only.
            {11362, 3176, 3367, 0}, {11363, 3178, 3367, 0}, {11362, 3176, 3369, 0},
            {11360, 3179, 3368, 0}, {11361, 3181, 3368, 0}, {11360, 3183, 3369, 0}, {11361, 3184, 3371, 0},
            {11360, 3182, 3373, 0}, {11361, 3180, 3373, 0}, {11360, 3178, 3372, 0}, {11361, 3177, 3370, 0},
            {11364, 3180, 3371, 0}, {11365, 3182, 3372, 0}, {11364, 3183, 3374, 0},
            {11368, 3185, 3373, 0}, {11369, 3186, 3371, 0}, {11368, 3184, 3369, 0},

            // Al Kharid mine: rebuilt as a narrow north-to-south valley with tiers deepening eastward.
            {10943, 3294, 3318, 0}, {11161, 3296, 3317, 0}, {10943, 3297, 3315, 0}, {11360, 3298, 3313, 0},
            {11368, 3298, 3310, 0}, {11369, 3299, 3308, 0}, {11368, 3300, 3306, 0}, {11369, 3301, 3304, 0}, {11368, 3302, 3302, 0},
            {11364, 3301, 3314, 0}, {11365, 3302, 3312, 0}, {11364, 3303, 3310, 0}, {11365, 3304, 3308, 0}, {11364, 3305, 3306, 0},
            {11365, 3306, 3304, 0}, {11364, 3307, 3302, 0}, {11365, 3306, 3300, 0}, {11364, 3304, 3299, 0},
            {11366, 3308, 3313, 0}, {11367, 3309, 3310, 0}, {11366, 3310, 3307, 0},
            {11370, 3310, 3304, 0}, {11371, 3311, 3302, 0},
            {11372, 3309, 3299, 0}, {11373, 3311, 3298, 0},
            {11374, 3312, 3296, 0}, {11375, 3310, 3295, 0},

            // Rimmington mine: classic mixed ore site west of Port Sarim.
            {10943, 2971, 3240, 0}, {11161, 2973, 3239, 0}, {10943, 2975, 3238, 0}, {11161, 2977, 3238, 0}, {10943, 2979, 3239, 0},
            {11360, 2974, 3236, 0}, {11361, 2976, 3236, 0},
            {11362, 2969, 3241, 0}, {11363, 2968, 3243, 0},
            {11364, 2972, 3242, 0}, {11365, 2974, 3243, 0}, {11364, 2976, 3243, 0}, {11365, 2978, 3242, 0}, {11364, 2980, 3241, 0}, {11365, 2981, 3239, 0},
            {11370, 2982, 3237, 0}, {11371, 2983, 3239, 0},

            // Crafting Guild mine: compact clay/silver/gold crescent north of the guild.
            {11362, 2938, 3288, 0}, {11363, 2939, 3290, 0}, {11362, 2940, 3292, 0}, {11363, 2942, 3292, 0}, {11362, 2943, 3290, 0}, {11363, 2942, 3288, 0},
            {11368, 2944, 3287, 0}, {11369, 2945, 3289, 0}, {11368, 2946, 3291, 0}, {11369, 2947, 3290, 0}, {11368, 2946, 3288, 0}, {11369, 2945, 3286, 0},
            {11370, 2948, 3287, 0}, {11371, 2949, 3289, 0}, {11370, 2950, 3291, 0}, {11371, 2949, 3293, 0}, {11370, 2947, 3294, 0}, {11371, 2945, 3294, 0}, {11370, 2944, 3292, 0},

            // Dwarven Mine: low-level entrance loop plus deeper coal/gold/mithril/adamant cluster.
            {10943, 3019, 9831, 0}, {11161, 3021, 9831, 0}, {10943, 3023, 9830, 0}, {11161, 3025, 9830, 0},
            {11360, 3020, 9828, 0}, {11361, 3022, 9828, 0}, {11360, 3024, 9827, 0}, {11361, 3026, 9827, 0},
            {11364, 3028, 9828, 0}, {11365, 3030, 9828, 0}, {11364, 3032, 9829, 0}, {11365, 3034, 9830, 0},
            {11366, 3040, 9740, 0}, {11367, 3042, 9740, 0}, {11366, 3044, 9739, 0}, {11367, 3046, 9739, 0}, {11366, 3048, 9740, 0},
            {11366, 3038, 9735, 0}, {11367, 3040, 9734, 0}, {11366, 3042, 9734, 0},
            {11370, 3052, 9737, 0}, {11371, 3054, 9737, 0},
            {11372, 3031, 9736, 0}, {11373, 3033, 9736, 0},
            {11374, 3028, 9733, 0}, {11375, 3030, 9733, 0},

            // Mining Guild: core underground coal-heavy cluster with mithril, adamantite, and a runite pair.
            {11366, 3014, 9728, 0}, {11367, 3016, 9728, 0}, {11366, 3018, 9728, 0}, {11367, 3020, 9729, 0},
            {11366, 3022, 9730, 0}, {11367, 3020, 9732, 0}, {11366, 3018, 9732, 0}, {11367, 3016, 9731, 0},
            {11372, 3012, 9731, 0}, {11373, 3013, 9733, 0}, {11372, 3015, 9734, 0},
            {11374, 3010, 9734, 0}, {11375, 3011, 9736, 0},
            {11376, 3008, 9731, 0}, {11377, 3009, 9729, 0}
    };

    private MiningSpawnService() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        for (int[] spawn : ROCK_SPAWNS) {
            GameObject rock = new GameObject(spawn[0], new Location(spawn[1], spawn[2], spawn[3]), 10, 0, null);
            ObjectManager.register(rock, false);
            SPAWNED_ROCKS.add(rock);
        }

        initialized = true;
        System.out.println("MiningSpawnService initialized with " + ROCK_SPAWNS.length + " visible rock spawns.");
    }

    public static void onRegionChange(Player player) {
        if (!initialized) {
            return;
        }

        int nearby = 0;
        for (GameObject rock : SPAWNED_ROCKS) {
            if (player.getPrivateArea() != rock.getPrivateArea()) {
                continue;
            }
            if (!player.getLocation().isWithinDistance(rock.getLocation(), 64)) {
                continue;
            }
            player.getPacketSender().sendObject(rock);
            nearby++;
        }

        if (nearby > 0) {
            System.out.println("MiningSpawnService resent " + nearby + " nearby rocks to " + player.getUsername() + ".");
        }
    }

    public static GameObject findNearbyRock(int objectId, Location location, int maxDistance) {
        if (!initialized) {
            return null;
        }

        GameObject best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (GameObject rock : SPAWNED_ROCKS) {
            if (rock.getId() != objectId) {
                continue;
            }
            if (rock.getLocation().getZ() != location.getZ()) {
                continue;
            }

            int dx = Math.abs(rock.getLocation().getX() - location.getX());
            int dy = Math.abs(rock.getLocation().getY() - location.getY());
            int distance = Math.max(dx, dy);
            if (distance > maxDistance) {
                continue;
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                best = rock;
            }
        }

        return best;
    }

    public static boolean isInjectedRock(GameObject object) {
        if (object == null) {
            return false;
        }
        return SPAWNED_ROCKS.contains(object);
    }
}
