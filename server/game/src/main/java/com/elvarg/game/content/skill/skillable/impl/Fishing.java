package com.elvarg.game.content.skill.skillable.impl;

import com.elvarg.game.content.PetHandler;
import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.grounditem.ItemOnGroundManager;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.Projectile;
import com.elvarg.game.model.Skill;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;
import com.elvarg.util.Chance;
import com.elvarg.util.Misc;
import com.elvarg.util.NpcIdentifiers;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Represents the Fishing skill.
 *
 * @author Professor Oak
 * @author Lare96
 */
public class Fishing extends DefaultSkillable {
    private static final Set<Integer> NET_SPOTS = new HashSet<>(Arrays.asList(
            1497, 1498, 1499, 1500, 1518, 1519, 1521, 1523, 1524, 1525, 1528, 1530, 1542, 3913, 3914, 3915, 4316,
            4476, 4477, 4710, 4711, 4712, 5233, 5820, 5821, 6488, 7155, 7200, 7201, 7323, 7460, 7461, 7462, 7465,
            7730, 7731, 7732, 7733, 7946, 7947
    ));

    private static final Set<Integer> BAIT_SPOTS = new HashSet<>(Arrays.asList(
            1497, 1498, 1499, 1500, 1514, 1517, 1526, 1528, 1532, 1533, 1542, 7946, 7947
    ));

    private static final Set<Integer> LURE_SPOTS = new HashSet<>(Arrays.asList(
            1506, 1507, 1508, 1509, 1515, 1526, 1527, 3417, 6825, 7463, 7464, 7468, 7676
    ));

    private static final Set<Integer> CAGE_SPOTS = new HashSet<>(Arrays.asList(
            1510, 1511, 1519, 1520, 2653, 2654, 2655, 4713, 7466, 7467
    ));

    private static final Set<Integer> CAGE_HARPOON_SPOTS = new HashSet<>(Arrays.asList(
            1510, 1519, 1522, 3657, 3914, 5820, 7199, 7460, 7465, 7470, 7946
    ));

    private static final Set<Integer> BIG_NET_HARPOON_SPOTS = new HashSet<>(Arrays.asList(
            1520, 3419, 3915, 4476, 4477, 5233, 5234, 7200, 7461, 7466
    ));

    private static final Set<Integer> NET_HARPOON_SPOTS = new HashSet<>(Arrays.asList(
            1511, 4316, 5821
    ));

    private static final Set<Integer> BIG_NET_SPOTS = new HashSet<>(Arrays.asList(
            1511, 1512, 3913, 3914, 3915, 4079, 4080, 4081, 4082
    ));

    public static boolean handleNpcInteraction(Player player, NPC npc, int option) {
        FishingTool tool = getToolForSpotOption(npc.getId(), option);
        if (tool == null) {
            return false;
        }
        player.getSkillManager().startSkillable(new Fishing(npc, tool));
        return true;
    }

    private static FishingTool getToolForSpotOption(int npcId, int option) {
        if (option == 1) {
            if (BIG_NET_SPOTS.contains(npcId)) return FishingTool.BIG_NET;
            if (CAGE_SPOTS.contains(npcId)) return FishingTool.LOBSTER_POT;
            if (LURE_SPOTS.contains(npcId)) return FishingTool.FLY_FISHING_ROD;
            if (BAIT_SPOTS.contains(npcId)) return FishingTool.FISHING_ROD;
            if (NET_SPOTS.contains(npcId)) return FishingTool.NET;
        } else if (option >= 2) {
            if (CAGE_HARPOON_SPOTS.contains(npcId)) return FishingTool.HARPOON;
            if (BIG_NET_HARPOON_SPOTS.contains(npcId)) return FishingTool.SHARK_HARPOON;
            if (NET_HARPOON_SPOTS.contains(npcId)) return FishingTool.SHARK_HARPOON;
            if (BAIT_SPOTS.contains(npcId)) return FishingTool.FISHING_ROD;
        }

        switch (npcId) {
            // Shrimp / Anchovies
            case NpcIdentifiers.FISHING_SPOT_6: // 1506
            case NpcIdentifiers.FISHING_SPOT_30: // 1530
            case NpcIdentifiers.FISHING_SPOT_31: // 1531
                if (option == 1) {
                    return FishingTool.NET;
                }
                break;

            // Sardine / Herring (bait) + Pike (bait)
            case NpcIdentifiers.FISHING_SPOT_2: // 1497
            case NpcIdentifiers.FISHING_SPOT_3: // 1498
            case NpcIdentifiers.FISHING_SPOT_4: // 1499
            case NpcIdentifiers.FISHING_SPOT_5: // 1500
                if (option == 1 || option == 2) {
                    return FishingTool.FISHING_ROD;
                }
                break;

            // Trout / Salmon (lure) + pike/bait at some spots
            case NpcIdentifiers.FISHING_SPOT_8: // 1508
            case NpcIdentifiers.FISHING_SPOT_9: // 1509
                if (option == 1) {
                    return FishingTool.FLY_FISHING_ROD;
                }
                if (option == 2) {
                    return FishingTool.FISHING_ROD;
                }
                break;

            // Lobster / Swordfish / Tuna
            case NpcIdentifiers.FISHING_SPOT_11: // 1511
            case NpcIdentifiers.FISHING_SPOT_12: // 1512
            case NpcIdentifiers.FISHING_SPOT_40: // 2653
            case NpcIdentifiers.FISHING_SPOT_41: // 2654
            case NpcIdentifiers.FISHING_SPOT_42: // 2655
                if (option == 1) {
                    return FishingTool.LOBSTER_POT;
                }
                if (option == 2) {
                    return FishingTool.HARPOON;
                }
                break;

            // Big net / shark + harpoon variants (e.g. Catherby)
            case NpcIdentifiers.FISHING_SPOT_48: // 3913
            case NpcIdentifiers.FISHING_SPOT_49: // 3914
            case NpcIdentifiers.FISHING_SPOT_50: // 3915
            case NpcIdentifiers.FISHING_SPOT_51: // 4079
            case NpcIdentifiers.FISHING_SPOT_52: // 4080
            case NpcIdentifiers.FISHING_SPOT_53: // 4081
            case NpcIdentifiers.FISHING_SPOT_54: // 4082
                if (option == 1) {
                    return FishingTool.BIG_NET;
                }
                if (option == 2) {
                    return FishingTool.SHARK_HARPOON;
                }
                break;
        }
        return null;
    }

    /**
     * All of the possible items you can get from a casket.
     */
    public static final Item[] CASKET_ITEMS = {new Item(1061), new Item(592), new Item(1059), new Item(995, 100000), new Item(4212), new Item(995, 50000), new Item(401), new Item(995, 150000), new Item(407)};
    /**
     * The fish spot we're fishing from.
     */
    private final NPC fishSpot;

    /**
     * The {@link FishingTool} we're using
     * to fish.
     */
    private final FishingTool tool;

    /**
     * Constructs a new fishing instance.
     *
     * @param fishSpot The fish spot we're using to fish from.
     */
    public Fishing(NPC fishSpot, FishingTool tool) {
        this.fishSpot = fishSpot;
        this.tool = tool;
    }

    @Override
    public boolean hasRequirements(Player player) {
        //Make sure player has tool..
        if (!player.getInventory().contains(tool.getId())) {
            player.getPacketSender().sendMessage("You need a " + ItemDefinition.forId(tool.getId()).getName().toLowerCase() + " to do this.");
            return false;
        }

        //Check fishing level for tool..
        if (player.getSkillManager().getCurrentLevel(Skill.FISHING) < tool.getLevel()) {
            player.getPacketSender().sendMessage("You need a Fishing level of at least " + tool.getLevel() + " to do this.");
            return false;
        }

        //Make sure player has required bait..
        if (tool.getNeeded() > 0) {
            if (!player.getInventory().contains(tool.getNeeded())) {
                player.getPacketSender().sendMessage("You do not have any " + ItemDefinition.forId(tool.getNeeded()).getName().toLowerCase() + "(s).");
                return false;
            }
        }

        return super.hasRequirements(player);
    }

    @Override
    public void start(Player player) {
        player.getPacketSender().sendMessage("You begin to fish..");
        super.start(player);
    }

    @Override
    public void startAnimationLoop(Player player) {
        Task animLoop = new Task(4, player, true) {
            @Override
            protected void execute() {
                player.performAnimation(new Animation(tool.getAnimation()));
            }
        };
        TaskManager.submit(animLoop);
        getTasks().add(animLoop);
    }

    @Override
    public void onCycle(Player player) {
        PetHandler.onSkill(player, Skill.FISHING);

        //Handle random event..
        if (Misc.getRandom(1400) == 1) {
            AttackToolRandomEvent attackTool = new AttackToolRandomEvent(player, tool, fishSpot);
            TaskManager.submit(attackTool);
            cancel(player);
        }
    }

    @Override
    public void finishedCycle(Player player) {
        /** Random stop for that 'old school' rs feel :) */
        if (Misc.getRandom(90) == 0) {
            cancel(player);
        }

        /** Catch multiple fish with a big net. */
        int amount = 1;
        if (tool == FishingTool.BIG_NET) {
            amount = Math.min(Misc.getRandom(4) + 1, player.getInventory().getFreeSlots());
        }

        int fishingLevel = player.getSkillManager().getCurrentLevel(Skill.FISHING);
        for (int i = 0; i < amount; i++) {
            Fish caught = determineFish(player, tool);

            int levelDiff = fishingLevel - caught.getLevel();
            Chance chance = Chance.SOMETIMES;
            if (levelDiff >= 15) chance = Chance.COMMON;
            if (levelDiff >= 25) chance = Chance.VERY_COMMON;

            if (chance.success()) {
                player.getPacketSender().sendMessage(
                        "You catch a " + caught.name().toLowerCase().replace("_", " ") + ".");
                player.getInventory().add(new Item(caught.getId()));
                player.getSkillManager().addExperience(Skill.FISHING, caught.getExperience());
            }

            if (tool.getNeeded() > 0) {
                player.getInventory().delete(new Item(tool.getNeeded()));
            }
        }

    }

    @Override
    public boolean loopRequirements() {
        return true;
    }

    @Override
    public boolean allowFullInventory() {
        return false;
    }

    @Override
    public int cyclesRequired(Player player) {
        float cycles = 4 + Misc.getRandom(2);
        cycles -= player.getSkillManager().getCurrentLevel(Skill.FISHING) * 0.03;

        return Math.max(3, (int) cycles);
    }

    /**
     * Gets a random fish to be caught for the player based on fishing level and
     * rarity.
     *
     * @param player the player that needs a fish.
     * @param tool   the tool this player is fishing with.
     */
    private Fish determineFish(Player player, FishingTool tool) {
        List<Fish> fishList = new ArrayList<Fish>();

        /** Determine which fish are able to be caught. */
        for (Fish fish : tool.getFish()) {
            if (fish.getLevel() <= player.getSkillManager().getCurrentLevel(Skill.FISHING)) {
                fishList.add(fish);
            }
        }

        /** Filter the fish based on rarity. */
        for (Iterator<Fish> iterator = fishList.iterator(); iterator.hasNext(); ) {
            Fish fish = iterator.next();

            if (fishList.size() == 1) {
                /** Return this fish if it's the only one left in the list. */
                return fish;
            }

            if (!fish.getChance().success()) {
                iterator.remove();
            }
        }

        /** Return a random fish from the list. */
        return Misc.randomElement(fishList);
    }

    /**
     * All of the tools that can be used to catch fish.
     *
     * @author lare96
     */
    public enum FishingTool {
        NET(303, 1, -1, 3, 621, Fish.SHRIMP, Fish.ANCHOVY),
        BIG_NET(305, 16, -1, 3, 620, Fish.MACKEREL, Fish.OYSTER, Fish.COD, Fish.BASS, Fish.CASKET),
        FISHING_ROD(307, 5, 313, 1, 622, Fish.SARDINE, Fish.HERRING, Fish.PIKE, Fish.SLIMY_EEL, Fish.CAVE_EEL, Fish.LAVA_EEL),
        FLY_FISHING_ROD(309, 20, 314, 1, 622, Fish.TROUT, Fish.SALMON),
        HARPOON(311, 35, -1, 4, 618, Fish.TUNA, Fish.SWORDFISH),
        SHARK_HARPOON(311, 76, -1, 6, 618, Fish.SHARK),
        LOBSTER_POT(301, 40, -1, 4, 619, Fish.LOBSTER);

        /**
         * The item id of the tool.
         */
        private int id;

        /**
         * The level you need to be to use this tool.
         */
        private int level;

        /**
         * The id of an item needed to use this tool.
         */
        private int needed;

        /**
         * The speed of this tool.
         */
        private int speed;

        /**
         * The animation performed when using this tool.
         */
        private int animation;

        /**
         * All of the fish you can catch with this tool.
         */
        private Fish[] fish;

        /**
         * Creates a new {@link Tool}.
         *
         * @param id        the item id of the tool.
         * @param level     the level you need to be to use this tool.
         * @param needed    the id of an item needed to use this tool.
         * @param speed     the speed of this tool.
         * @param animation the animation performed when using this tool.
         * @param fish      the fish you can catch with this tool.
         */
        private FishingTool(int id, int level, int needed, int speed, int animation, Fish... fish) {
            this.id = id;
            this.level = level;
            this.needed = needed;
            this.speed = speed;
            this.animation = animation;
            this.fish = fish;
        }

        /**
         * Gets the item id of this tool.
         *
         * @return the item id.
         */
        public int getId() {
            return id;
        }

        /**
         * Gets the level you need to be to use this tool.
         *
         * @return the level needed.
         */
        public int getLevel() {
            return level;
        }

        /**
         * Gets the id of an item needed to use this tool.
         *
         * @return the item needed.
         */
        public int getNeeded() {
            return needed;
        }

        /**
         * Gets the speed of this tool.
         *
         * @return the speed.
         */
        public int getSpeed() {
            return speed;
        }

        /**
         * Gets the animation performed when using this tool.
         *
         * @return the animation.
         */
        public int getAnimation() {
            return animation;
        }

        /**
         * Gets the fish you can catch with this tool.
         *
         * @return the fish available.
         */
        public Fish[] getFish() {
            return fish;
        }
    }

    /**
     * All of the fish that can be caught while fishing.
     *
     * @author lare96
     */
    public enum Fish {
        SHRIMP(317, 1, Chance.VERY_COMMON, 10),
        SARDINE(327, 5, Chance.VERY_COMMON, 20),
        HERRING(345, 10, Chance.VERY_COMMON, 30),
        ANCHOVY(321, 15, Chance.SOMETIMES, 40),
        MACKEREL(353, 16, Chance.VERY_COMMON, 20),
        CASKET(405, 16, Chance.ALMOST_IMPOSSIBLE, 100),
        OYSTER(407, 16, Chance.EXTREMELY_RARE, 80),
        TROUT(335, 20, Chance.VERY_COMMON, 50),
        COD(341, 23, Chance.VERY_COMMON, 45),
        PIKE(349, 25, Chance.VERY_COMMON, 60),
        SLIMY_EEL(3379, 28, Chance.EXTREMELY_RARE, 65),
        SALMON(331, 30, Chance.VERY_COMMON, 70),
        TUNA(359, 35, Chance.VERY_COMMON, 80),
        CAVE_EEL(5001, 38, Chance.SOMETIMES, 80),
        LOBSTER(377, 40, Chance.VERY_COMMON, 90),
        BASS(363, 46, Chance.SOMETIMES, 100),
        SWORDFISH(371, 50, Chance.COMMON, 100),
        LAVA_EEL(2148, 53, Chance.VERY_COMMON, 60),
        SHARK(383, 76, Chance.COMMON, 110);

        /**
         * The item id of the fish.
         */
        private int id;

        /**
         * The level needed to be able to catch the fish.
         */
        private int level;

        /**
         * The chance of catching this fish (when grouped with other fishes).
         */
        private Chance chance;

        /**
         * The experience gained from catching this fish.
         */
        private int experience;

        /**
         * Creates a new {@link Fish}.
         *
         * @param id         the item id of the fish.
         * @param level      the level needed to be able to catch the fish.
         * @param chance     the chance of catching this fish (when grouped with other
         *                   fishes).
         * @param experience the experience gained from catching this fish.
         */
        private Fish(int id, int level, Chance chance, int experience) {
            this.id = id;
            this.level = level;
            this.chance = chance;
            this.experience = experience;
        }

        /**
         * Gets the item id of the fish.
         *
         * @return the item id.
         */
        public int getId() {
            return id;
        }

        /**
         * Gets the level needed to be able to catch the fish.
         *
         * @return the level.
         */
        public int getLevel() {
            return level;
        }

        /**
         * Gets the chance of catching this fish (when grouped with other
         * fishes).
         *
         * @return the chance.
         */
        public Chance getChance() {
            return chance;
        }

        /**
         * Gets the experience gained from catching this fish.
         *
         * @return the experience.
         */
        public int getExperience() {
            return experience;
        }
    }

    /**
     * Represents a random event which attacks a player's tool, forcing it
     * to drop onto the ground.
     * <p>
     * This is a custom version of the OSRS "Big fish random event", which was
     * deleted in a update. The NPC "big fish" was deleted along with it,
     * so we simply shoot the projectile from the fish spot.
     *
     * @author Professor Oak
     * @author Lare96
     */
    private static final class AttackToolRandomEvent extends Task {
        /**
         * The defence animation the player will perform
         * when attacked.
         */
        private static final Animation DEFENCE_ANIM = new Animation(404);

        /**
         * The projectile which will be fired towards
         * the player from the fishing spot.
         */
        private static final Projectile PROJECTILE = new Projectile(94, 31, 33, 40, 70);

        /**
         * The player being attacked by a fish.
         */
        private final Player player;

        /**
         * The tool the player is using when being
         * attacked by a fish. This will be removed
         * from inventory and put on the ground.
         */
        private final FishingTool tool;

        /**
         * The fishing spot's position. The
         * attacking npc.
         */
        private final NPC fishSpot;

        /**
         * This {@link Task}'s current tick.
         */
        private int tick;

        /**
         * Did we delete the player's tool?
         */
        private boolean deletedTool;

        /**
         * Creates this task.
         *
         * @param player
         * @param tool
         * @param fishSpot
         */
        public AttackToolRandomEvent(Player player, FishingTool tool, NPC fishSpot) {
            super(1, player, true);
            this.player = player;
            this.tool = tool;
            this.fishSpot = fishSpot;
        }

        @Override
        protected void execute() {
            switch (tick) {
                case 0:
                    //Fire projectile at player.
                    Projectile.sendProjectile(fishSpot, player, PROJECTILE);
                    break;
                case 2:
                    //Defence animation..
                    player.performAnimation(DEFENCE_ANIM);
                    break;
                case 3:
                    //Delete tool from inventory and put on ground..
                    if (player.getInventory().contains(tool.getId())) {
                        player.getInventory().delete(tool.getId(), 1);
                        deletedTool = true;
                    }
                    break;
                case 4:
                    //Spawn tool on ground if it was deleted from inventory..
                    if (deletedTool) {
                        ItemOnGroundManager.register(player, new Item(tool.getId()));
                        player.getPacketSender().sendMessage("A big fish attacked and you were forced to drop your " + ItemDefinition.forId(tool.getId()).getName().toLowerCase() + ".");
                    }

                    //Stop task..
                    stop();
                    break;
            }
            tick++;
        }
    }
}
