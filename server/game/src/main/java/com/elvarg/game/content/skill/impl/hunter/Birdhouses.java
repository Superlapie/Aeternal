package com.elvarg.game.content.skill.impl.hunter;

import com.elvarg.game.content.skill.SkillManager;
import com.elvarg.game.content.Looting;
import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.object.GameObject;
import com.elvarg.game.entity.impl.object.ObjectManager;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.Skill;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class Birdhouses {

    private Birdhouses() {
    }

    private static final int CLOCKWORK_ID = 8792;
    private static final int HAMMER_ID = 2347;
    private static final int CHISEL_ID = 1755;

    private static final int RAW_BIRD_MEAT_ID = 9978;
    private static final int FEATHER_ID = 314;
    private static final int[] BIRD_NESTS = {5070, 5071, 5072, 5073, 5074, 5075};

    private static final Animation CRAFT_ANIM = new Animation(1248);
    private static final Animation INTERACT_ANIM = new Animation(827);
    private static final int BIRDHOUSE_GROWTH_TICKS = (50 * 60 * 1000) / 600;
    private static final long BIRDHOUSE_GROWTH_MS = 50L * 60L * 1000L;
    private static final Path SAVE_PATH = Path.of("../data/birdhouses.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SAVE_LIST_TYPE = new TypeToken<List<BirdhouseSave>>() {}.getType();

    private static final int SPACE_OBJECT_ID = 30565;
    private static final Set<Integer> SPACE_IDS = Set.of(30565, 30566, 30567, 30568);
    private static final Location[] BIRDHOUSE_SPOTS = {
            new Location(5835, 5940, 0),
            new Location(5837, 5940, 0),
            new Location(5835, 5938, 0),
            new Location(5837, 5938, 0)
    };

    private static final Set<String> HERB_NAMES = Set.of(
            "guam", "marrentill", "tarromin", "harralander", "ranarr",
            "toadflax", "irit", "avantoe", "kwuarm", "snapdragon",
            "cadantine", "lantadyme", "dwarf weed", "torstol"
    );
    private static final Set<String> HOP_NAMES = Set.of(
            "barley", "hammerstone", "asgarnian", "jute", "yanillian", "krandorian", "wildblood"
    );

    private static final Map<Location, ActiveBirdhouse> ACTIVE = new ConcurrentHashMap<>();

    private enum State {
        BUILT, SEEDED, READY
    }

    private static final class ActiveBirdhouse {
        private final Tier tier;
        private final String owner;
        private volatile long seededAtMs;
        private volatile State state;
        private final int face;
        private final int type;

        private ActiveBirdhouse(Tier tier, String owner, State state, int face, int type) {
            this.tier = tier;
            this.owner = owner;
            this.state = state;
            this.face = face;
            this.type = type;
        }
    }

    private static final class BirdhouseSave {
        private int x;
        private int y;
        private int z;
        private String owner;
        private String tier;
        private String state;
        private long seededAtMs;
        private int face;
        private int type;
    }

    private enum Tier {
        REGULAR(1511, 21507, 30553, 30554, 30555, 5, 5, 280, 280),
        OAK(1521, 21509, 30556, 30557, 30558, 15, 14, 420, 420),
        WILLOW(1519, 21511, 30559, 30560, 30561, 25, 24, 560, 560),
        TEAK(6333, 21513, 30562, 30563, 30564, 35, 34, 700, 700),
        MAPLE(1517, 21515, 31827, 31828, 31829, 45, 44, 820, 820),
        MAHOGANY(6332, 21517, 31830, 31831, 31832, 50, 49, 960, 960),
        YEW(1515, 21519, 31833, 31834, 31835, 60, 59, 1020, 1020),
        MAGIC(1513, 21521, 31836, 31837, 31838, 75, 74, 1140, 1140),
        REDWOOD(19669, 22192, 31839, 31840, 31841, 90, 89, 1200, 1200);

        private final int logId;
        private final int itemId;
        private final int builtObjectId;
        private final int seededObjectId;
        private final int readyObjectId;
        private final int craftingLevel;
        private final int hunterLevel;
        private final int craftXp;
        private final int hunterXp;

        Tier(int logId, int itemId, int builtObjectId, int seededObjectId, int readyObjectId, int craftingLevel, int hunterLevel, int craftXp, int hunterXp) {
            this.logId = logId;
            this.itemId = itemId;
            this.builtObjectId = builtObjectId;
            this.seededObjectId = seededObjectId;
            this.readyObjectId = readyObjectId;
            this.craftingLevel = craftingLevel;
            this.hunterLevel = hunterLevel;
            this.craftXp = craftXp;
            this.hunterXp = hunterXp;
        }

        private static Tier fromLog(int logId) {
            return Arrays.stream(values()).filter(t -> t.logId == logId).findFirst().orElse(null);
        }

        private static Tier fromBirdhouseItem(int itemId) {
            return Arrays.stream(values()).filter(t -> t.itemId == itemId).findFirst().orElse(null);
        }

        private static Tier fromObjectState(int objectId) {
            return Arrays.stream(values())
                    .filter(t -> t.builtObjectId == objectId || t.seededObjectId == objectId || t.readyObjectId == objectId)
                    .findFirst()
                    .orElse(null);
        }
    }

    public static void initialize() {
        for (Location spot : BIRDHOUSE_SPOTS) {
            ObjectManager.register(new GameObject(SPACE_OBJECT_ID, spot, 10, 0, null), true);
        }
        load();
    }

    public static boolean handleItemOnItem(Player player, int firstItemId, int secondItemId) {
        final boolean hasClockwork = firstItemId == CLOCKWORK_ID || secondItemId == CLOCKWORK_ID;
        if (!hasClockwork) {
            return false;
        }

        final int otherItem = (firstItemId == CLOCKWORK_ID) ? secondItemId : firstItemId;
        final Tier tier = Tier.fromLog(otherItem);
        if (tier == null) {
            return false;
        }

        if (!player.getInventory().contains(HAMMER_ID) || !player.getInventory().contains(CHISEL_ID)) {
            player.getPacketSender().sendMessage("You need a hammer and chisel to make a birdhouse.");
            return true;
        }

        if (player.getSkillManager().getCurrentLevel(Skill.CRAFTING) < tier.craftingLevel) {
            player.getPacketSender().sendMessage("You need a Crafting level of " + tier.craftingLevel + " to make this birdhouse.");
            return true;
        }

        if (!player.getInventory().contains(otherItem)) {
            return true;
        }

        if (player.getInventory().isFull() && !player.getInventory().contains(otherItem)) {
            player.getPacketSender().sendMessage("You do not have enough inventory space.");
            return true;
        }

        player.performAnimation(CRAFT_ANIM);
        player.getInventory().delete(otherItem, 1).delete(CLOCKWORK_ID, 1).add(tier.itemId, 1);
        player.getSkillManager().addExperience(Skill.CRAFTING, tier.craftXp);
        player.getPacketSender().sendMessage("You carefully craft a " + ItemDefinition.forId(tier.itemId).getName().toLowerCase() + ".");
        return true;
    }

    public static boolean handleItemOnObject(Player player, Item item, GameObject object) {
        final Tier placementTier = Tier.fromBirdhouseItem(item.getId());
        if (placementTier != null && SPACE_IDS.contains(object.getId())) {
            return placeBirdhouse(player, placementTier, object);
        }

        final Tier objectTier = Tier.fromObjectState(object.getId());
        if (objectTier == null) {
            return false;
        }

        final ActiveBirdhouse active = ACTIVE.get(object.getLocation());
        if (active == null) {
            return false;
        }

        if (!active.owner.equalsIgnoreCase(player.getUsername())) {
            player.getPacketSender().sendMessage("This isn't your birdhouse.");
            return true;
        }

        if (active.state != State.BUILT || object.getId() != active.tier.builtObjectId) {
            return false;
        }

        return seedBirdhouse(player, object, active, item.getId());
    }

    public static boolean handleObjectClick(Player player, GameObject object) {
        final Tier tier = Tier.fromObjectState(object.getId());
        if (tier == null) {
            return false;
        }

        final ActiveBirdhouse active = ACTIVE.get(object.getLocation());
        if (active == null) {
            return false;
        }

        if (!active.owner.equalsIgnoreCase(player.getUsername())) {
            player.getPacketSender().sendMessage("This isn't your birdhouse.");
            return true;
        }

        if (active.state == State.SEEDED && System.currentTimeMillis() - active.seededAtMs >= 50L * 60L * 1000L) {
            setReadyState(object.getLocation(), active);
        }

        if (active.state != State.READY || object.getId() != active.tier.readyObjectId) {
            player.getPacketSender().sendMessage("This birdhouse isn't ready yet.");
            return true;
        }

        player.performAnimation(INTERACT_ANIM);
        player.getSkillManager().addExperience(Skill.HUNTER, active.tier.hunterXp);
        Looting.addOrDrop(player, CLOCKWORK_ID, 1);
        Looting.addOrDrop(player, RAW_BIRD_MEAT_ID, 10);
        Looting.addOrDrop(player, FEATHER_ID, ThreadLocalRandom.current().nextInt(40, 61));
        if (ThreadLocalRandom.current().nextInt(100) < 20) {
            int nestId = BIRD_NESTS[ThreadLocalRandom.current().nextInt(BIRD_NESTS.length)];
            Looting.addOrDrop(player, nestId, 1);
        }

        ObjectManager.register(new GameObject(SPACE_OBJECT_ID, object.getLocation(), object.getType(), object.getFace(), object.getPrivateArea()), true);
        ACTIVE.remove(object.getLocation());
        save();
        player.getPacketSender().sendMessage("You dismantle the birdhouse and collect your loot.");
        return true;
    }

    private static boolean placeBirdhouse(Player player, Tier tier, GameObject spaceObject) {
        if (player.getSkillManager().getCurrentLevel(Skill.HUNTER) < tier.hunterLevel) {
            player.getPacketSender().sendMessage("You need a Hunter level of " + tier.hunterLevel + " to place this birdhouse.");
            return true;
        }

        if (!player.getInventory().contains(tier.itemId)) {
            return true;
        }

        player.performAnimation(INTERACT_ANIM);
        player.getInventory().delete(tier.itemId, 1);

        ActiveBirdhouse active = new ActiveBirdhouse(tier, player.getUsername(), State.BUILT, spaceObject.getFace(), spaceObject.getType());
        ACTIVE.put(spaceObject.getLocation().clone(), active);
        ObjectManager.register(new GameObject(tier.builtObjectId, spaceObject.getLocation(), spaceObject.getType(), spaceObject.getFace(), spaceObject.getPrivateArea()), true);
        save();
        player.getPacketSender().sendMessage("You set up the birdhouse frame.");
        return true;
    }

    private static boolean seedBirdhouse(Player player, GameObject object, ActiveBirdhouse active, int seedItemId) {
        final SeedType seedType = getSeedType(seedItemId);
        if (seedType == null) {
            return false;
        }

        if (player.getInventory().getAmount(seedItemId) < seedType.requiredAmount) {
            player.getPacketSender().sendMessage("You need at least " + seedType.requiredAmount + " of that seed.");
            return true;
        }

        player.performAnimation(INTERACT_ANIM);
        player.getInventory().delete(seedItemId, seedType.requiredAmount);
        active.seededAtMs = System.currentTimeMillis();
        active.state = State.SEEDED;
        ObjectManager.register(new GameObject(active.tier.seededObjectId, object.getLocation(), object.getType(), object.getFace(), object.getPrivateArea()), true);
        scheduleReadyTransition(object.getLocation().clone(), active);
        save();
        player.getPacketSender().sendMessage("You fill the birdhouse with seeds.");
        return true;
    }

    private static void scheduleReadyTransition(Location location, ActiveBirdhouse active) {
        TaskManager.submit(new Task(BIRDHOUSE_GROWTH_TICKS) {
            @Override
            protected void execute() {
                ActiveBirdhouse current = ACTIVE.get(location);
                if (current == null || current != active || current.state != State.SEEDED) {
                    stop();
                    return;
                }
                setReadyState(location, current);
                stop();
            }
        });
    }

    private static void setReadyState(Location location, ActiveBirdhouse active) {
        active.state = State.READY;
        ObjectManager.register(new GameObject(active.tier.readyObjectId, location, active.type, active.face, null), true);
        save();
    }

    private static SeedType getSeedType(int itemId) {
        String name = ItemDefinition.forId(itemId).getName();
        if (name == null) {
            return null;
        }
        String lower = name.toLowerCase();
        if (!lower.contains("seed")) {
            return null;
        }

        for (String herb : HERB_NAMES) {
            if (lower.contains(herb)) {
                return new SeedType(5);
            }
        }
        for (String hop : HOP_NAMES) {
            if (lower.contains(hop)) {
                return new SeedType(10);
            }
        }
        return null;
    }

    private static final class SeedType {
        private final int requiredAmount;

        private SeedType(int requiredAmount) {
            this.requiredAmount = requiredAmount;
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(SAVE_PATH.getParent());
            List<BirdhouseSave> list = new ArrayList<>();
            for (Map.Entry<Location, ActiveBirdhouse> entry : ACTIVE.entrySet()) {
                Location location = entry.getKey();
                ActiveBirdhouse active = entry.getValue();
                BirdhouseSave save = new BirdhouseSave();
                save.x = location.getX();
                save.y = location.getY();
                save.z = location.getZ();
                save.owner = active.owner;
                save.tier = active.tier.name();
                save.state = active.state.name();
                save.seededAtMs = active.seededAtMs;
                save.face = active.face;
                save.type = active.type;
                list.add(save);
            }
            try (Writer writer = Files.newBufferedWriter(SAVE_PATH)) {
                GSON.toJson(list, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized void load() {
        if (!Files.exists(SAVE_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(SAVE_PATH)) {
            List<BirdhouseSave> saves = GSON.fromJson(reader, SAVE_LIST_TYPE);
            if (saves == null) {
                return;
            }

            ACTIVE.clear();
            long now = System.currentTimeMillis();
            for (BirdhouseSave save : saves) {
                Tier tier = Tier.valueOf(save.tier);
                State state = State.valueOf(save.state);
                Location location = new Location(save.x, save.y, save.z);
                ActiveBirdhouse active = new ActiveBirdhouse(tier, save.owner, state, save.face, save.type);
                active.seededAtMs = save.seededAtMs;
                ACTIVE.put(location, active);

                if (state == State.BUILT) {
                    ObjectManager.register(new GameObject(tier.builtObjectId, location, active.type, active.face, null), true);
                } else if (state == State.SEEDED) {
                    long elapsed = now - active.seededAtMs;
                    if (elapsed >= BIRDHOUSE_GROWTH_MS) {
                        setReadyState(location, active);
                    } else {
                        ObjectManager.register(new GameObject(tier.seededObjectId, location, active.type, active.face, null), true);
                        scheduleReadyTransition(location.clone(), active);
                    }
                } else {
                    ObjectManager.register(new GameObject(tier.readyObjectId, location, active.type, active.face, null), true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
