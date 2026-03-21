package com.elvarg.game.content;

import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;
import com.elvarg.util.ItemIdentifiers;
import com.elvarg.util.Misc;

import java.util.*;

public final class ImplingJars {

    private static final int EMPTY_IMPLING_JAR = ItemIdentifiers.IMPLING_JAR;
    private static final int JAR_BREAK_CHANCE = 10;
    private static final Map<Integer, List<DropEntry>> TABLES = new HashMap<>();
    private static final Map<String, Integer> ITEM_BY_NAME = new HashMap<>();
    private static final Map<String, String> NAME_ALIASES = new HashMap<>();

    static {
        indexItems();
        initAliases();

        table(ItemIdentifiers.BABY_IMPLING_JAR,
                d("Chisel", "1", "1/10"),
                d("Thread", "1", "1/10"),
                d("Needle", "1", "1/10"),
                d("Knife", "1", "1/10"),
                d("Cheese", "1", "1/10"),
                d("Hammer", "1", "1/10"),
                d("Ball of wool", "1", "1/10"),
                d("Anchovies", "1", "1/10"),
                d("Nothing", "1", "1/10"),
                d("Spice", "1", "1/100"),
                d("Flax", "1", "1/100"),
                d("Mud pie", "1", "1/100"),
                d("Seaweed", "1", "1/100"),
                d("Air talisman", "1", "1/100"),
                d("Silver bar", "1", "1/100"),
                d("Sapphire", "1", "1/100"),
                d("Hard leather", "1", "1/100"),
                d("Lobster", "1", "1/100"),
                d("Soft clay", "1", "1/100"),
                d("Clue scroll (beginner)", "1", "1/50"),
                d("Clue scroll (easy)", "1", "1/100"));

        table(ItemIdentifiers.YOUNG_IMPLING_JAR,
                d("Steel nails", "5", "1/10"), d("Lockpick", "1", "1/10"), d("Pure essence", "1", "1/10"),
                d("Tuna", "1", "1/10"), d("Chocolate slice", "1", "1/10"), d("Steel axe", "1", "1/10"),
                d("Meat pizza", "1", "1/10"), d("Coal", "1", "1/10"), d("Bow string", "1", "1/10"),
                d("Snape grass", "1", "1/100"), d("Soft clay", "1", "1/100"), d("Studded chaps", "1", "1/100"),
                d("Steel full helm", "1", "1/100"), d("Oak plank", "1", "1/100"), d("Defence potion(3)", "1", "1/100"),
                d("Mithril bar", "1", "1/100"), d("Yew longbow", "1", "1/100"), d("Garden pie", "1", "1/100"),
                d("Jangerberries", "1", "1/100"), d("Clue scroll (beginner)", "1", "1/25"), d("Clue scroll (easy)", "1", "1/50"));

        table(ItemIdentifiers.GOURMET_IMPLING_JAR,
                d("Grubby key", "1", "1/500"), d("Tuna", "1", "1/5"), d("Bass", "1", "1/10"), d("Curry", "1", "1/10"),
                d("Meat pie", "1", "1/10"), d("Chocolate cake", "1", "1/10"), d("Frog spawn", "1", "1/10"),
                d("Spice", "1", "1/10"), d("Curry leaf", "1", "1/10"), d("Ugthanki kebab", "1", "1/100"),
                d("Lobster", "4 (noted)", "1/100"), d("Shark", "3 (noted)", "1/100"), d("Fish pie", "1", "1/100"),
                d("Chef's delight", "1", "1/100"), d("Rainbow fish", "5 (noted)", "1/100"), d("Garden pie", "6 (noted)", "1/100"),
                d("Swordfish", "3 (noted)", "1/100"), d("Strawberries(5)", "1", "1/100"), d("Cooked karambwan", "2 (noted)", "1/100"),
                d("Clue scroll (easy)", "1", "1/25"));

        table(ItemIdentifiers.EARTH_IMPLING_JAR,
                d("Fire talisman", "1", "1/10"), d("Earth talisman", "1", "1/10"), d("Earth tiara", "1", "1/10"),
                d("Earth rune", "32", "1/10"), d("Mithril ore", "1", "1/10"), d("Bucket of sand", "4 (noted)", "1/10"),
                d("Unicorn horn", "1", "1/10"), d("Compost", "6 (noted)", "1/10"), d("Gold ore", "1", "1/10"),
                d("Steel bar", "1", "1/100"), d("Mithril pickaxe", "1", "1/100"), d("Wildblood seed", "2", "1/100"),
                d("Jangerberry seed", "2", "1/100"), d("Supercompost", "2 (noted)", "1/100"), d("Mithril ore", "3 (noted)", "1/100"),
                d("Harralander seed", "2", "1/100"), d("Coal", "6 (noted)", "1/100"), d("Emerald", "2 (noted)", "1/100"),
                d("Ruby", "1", "1/100"), d("Clue scroll (medium)", "1", "1/100"));

        table(ItemIdentifiers.ESSENCE_IMPLING_JAR,
                d("Pure essence", "20 (noted)", "1/10"), d("Water rune", "30", "1/10"), d("Air rune", "30", "1/10"),
                d("Fire rune", "50", "1/10"), d("Mind rune", "25", "1/10"), d("Body rune", "28", "1/10"),
                d("Chaos rune", "4", "1/10"), d("Cosmic rune", "4", "1/10"), d("Mind talisman", "1", "1/10"),
                d("Pure essence", "35 (noted)", "1/100"), d("Lava rune", "4", "1/100"), d("Mud rune", "4", "1/100"),
                d("Smoke rune", "4", "1/100"), d("Steam rune", "4", "1/100"), d("Death rune", "13", "1/100"),
                d("Law rune", "13", "1/100"), d("Blood rune", "7", "1/100"), d("Soul rune", "11", "1/100"),
                d("Nature rune", "13", "1/100"), d("Clue scroll (medium)", "1", "1/50"));

        table(ItemIdentifiers.ECLECTIC_IMPLING_JAR,
                d("Mithril pickaxe", "1", "1/10"), d("Curry leaf", "1", "1/10"), d("Snape grass", "1", "1/10"),
                d("Air rune", "30-58", "1/10"), d("Oak plank", "4 (noted)", "1/10"), d("Empty candle lantern", "1", "1/10"),
                d("Gold ore", "1", "1/10"), d("Gold bar", "5 (noted)", "1/10"), d("Unicorn horn", "1", "1/10"),
                d("Adamant kiteshield", "1", "1/100"), d("Blue d'hide chaps", "1", "1/100"), d("Red spiky vambraces", "1", "1/100"),
                d("Rune dagger", "1", "1/100"), d("Battlestaff", "1", "1/100"), d("Adamantite ore", "10 (noted)", "1/100"),
                d("Slayer's respite", "2 (noted)", "1/100"), d("Wild pie", "1", "1/100"), d("Watermelon seed", "3", "1/100"),
                d("Diamond", "1", "1/100"), d("Clue scroll (medium)", "1", "1/25"));

        table(ItemIdentifiers.NATURE_IMPLING_JAR,
                d("Limpwurt seed", "1", "1/10"), d("Jangerberry seed", "1", "1/10"), d("Belladonna seed", "1", "1/10"),
                d("Harralander seed", "1", "1/10"), d("Cactus spine", "1", "1/10"), d("Magic logs", "1", "1/10"),
                d("Tarromin", "4 (noted)", "1/10"), d("Coconut", "1", "1/10"), d("Irit seed", "1", "1/10"),
                d("Curry tree seed", "1", "1/100"), d("Orange tree seed", "1", "1/100"), d("Snapdragon", "1", "1/100"),
                d("Kwuarm seed", "1", "1/100"), d("Avantoe seed", "5", "1/100"), d("Willow seed", "1", "1/100"),
                d("Torstol seed", "1", "1/100"), d("Ranarr seed", "1", "1/100"), d("Torstol", "2 (noted)", "1/100"),
                d("Dwarf weed seed", "1", "1/100"), d("Clue scroll (hard)", "1", "1/100"));

        table(ItemIdentifiers.MAGPIE_IMPLING_JAR,
                d("Black dragonhide", "6 (noted)", "2/21"), d("Diamond amulet", "3 (noted)", "1/21"), d("Amulet of power", "3 (noted)", "1/21"),
                d("Ring of forging", "3 (noted)", "1/21"), d("Splitbark gauntlets", "1", "1/21"), d("Mystic boots", "1", "1/21"),
                d("Mystic gloves", "1", "1/21"), d("Rune warhammer", "1", "1/21"), d("Ring of life", "4 (noted)", "1/21"),
                d("Rune sq shield", "1", "1/21"), d("Dragon dagger", "1", "1/21"), d("Nature tiara", "1", "1/21"),
                d("Runite bar", "2 (noted)", "1/21"), d("Diamond", "4 (noted)", "1/21"), d("Pineapple seed", "1", "1/21"),
                d("Ring of recoil", "3 (noted)", "1/21"), d("Loop half of key", "1", "1/21"), d("Tooth half of key", "1", "1/21"),
                d("Snapdragon seed", "1", "1/21"), d("Sinister key", "1", "1/21"), d("Clue scroll (hard)", "1", "1/50"));

        table(ItemIdentifiers.NINJA_IMPLING_JAR,
                d("Snakeskin boots", "1", "1/19"), d("Splitbark helm", "1", "1/19"), d("Mystic boots", "1", "1/19"),
                d("Rune chainbody", "1", "1/19"), d("Mystic gloves", "1", "1/19"), d("Opal machete", "1", "1/19"),
                d("Rune claws", "1", "1/19"), d("Rune scimitar", "1", "1/19"), d("Dragon dagger(p+)", "1", "1/19"),
                d("Rune arrow", "70", "1/19"), d("Rune dart", "70", "1/19"), d("Rune knife", "40", "1/19"),
                d("Rune thrownaxe", "50", "1/19"), d("Onyx bolts", "2", "1/19"), d("Onyx bolt tips", "4", "1/19"),
                d("Black dragonhide", "10 (noted)", "1/19"), d("Prayer potion(3)", "4 (noted)", "1/19"), d("Weapon poison(+)", "4 (noted)", "1/19"),
                d("Dagannoth hide", "3 (noted)", "1/19"), d("Clue scroll (hard)", "1", "1/25"));

        table(ItemIdentifiers.DRAGON_IMPLING_JAR,
                d("Dragonstone bolt tips", "10-30", "1/19"), d("Dragonstone bolt tips", "36", "1/19"), d("Mystic robe bottom", "1", "1/19"),
                d("Amulet of glory", "3 (noted)", "1/19"), d("Dragonstone amulet", "2 (noted)", "1/19"), d("Dragon arrow", "100-250", "1/19"),
                d("Dragonstone bolts", "10-40", "1/19"), d("Dragon longsword", "1", "1/19"), d("Dragon dagger(p++)", "3 (noted)", "1/19"),
                d("Dragon dart", "100-250", "1/19"), d("Dragonstone", "3 (noted)", "1/19"), d("Dragon dart tip", "100-350", "1/19"),
                d("Dragon arrowtips", "100-350", "1/19"), d("Dragon javelin tips", "25-35", "1/19"), d("Babydragon bones", "100-300 (noted)", "1/19"),
                d("Dragon bones", "50-100 (noted)", "1/19"), d("Magic seed", "1", "1/19"), d("Snapdragon seed", "6", "1/19"),
                d("Summer pie", "15 (noted)", "1/19"), d("Clue scroll (elite)", "1", "1/50"));

        table(ItemIdentifiers.CRYSTAL_IMPLING_JAR,
                d("Amulet of power", "5-7 (noted)", "1/18"), d("Crystal acorn", "1", "1/18"), d("Crystal shard", "30-40", "1/18"),
                d("Dragonstone amulet", "1", "1/18"), d("Dragonstone", "2 (noted)", "1/18"), d("Ruby bolt tips", "50-125", "1/18"),
                d("Onyx bolt tips", "6-10", "1/18"), d("Rune arrowtips", "150-250", "1/18"), d("Rune arrow", "400-750", "1/18"),
                d("Rune javelin tips", "20-60", "1/18"), d("Rune dart tip", "25-75", "1/18"), d("Rune dart", "50-100", "1/18"),
                d("Dragon dart tip", "10-15", "1/18"), d("Dragon dagger", "2 (noted)", "1/18"), d("Rune scimitar", "3-6 (noted)", "1/18"),
                d("Babydragon bones", "75-125 (noted)", "1/18"), d("Ranarr seed", "3-8", "1/18"), d("Yew seed", "1", "1/18"),
                d("Clue scroll (elite)", "1", "1/50"), d("Elven signet", "1", "1/128"));

        table(ItemIdentifiers.LUCKY_IMPLING_JAR,
                d("Clue scroll (easy)", "1", "1/5"), d("Clue scroll (medium)", "1", "1/5"),
                d("Clue scroll (hard)", "1", "1/5"), d("Clue scroll (elite)", "1", "1/5"),
                d("Clue scroll (master)", "1", "1/5"));
    }

    private ImplingJars() {
    }

    public static boolean open(Player player, int jarItemId, int slot) {
        List<DropEntry> table = TABLES.get(jarItemId);
        if (table == null) {
            return false;
        }
        DropEntry reward = roll(table);
        player.getInventory().delete(jarItemId, 1);
        int rewardAmount = reward.rollAmount();
        if (reward.itemId > 0 && rewardAmount > 0) {
            player.getInventory().add(reward.itemId, rewardAmount);
            player.getPacketSender().sendMessage("You open the jar and find: " + reward.displayName + ".");
        } else {
            player.getPacketSender().sendMessage("You open the jar and find nothing.");
        }
        if (Misc.getRandom(99) >= JAR_BREAK_CHANCE) {
            player.getInventory().add(EMPTY_IMPLING_JAR, 1);
        } else {
            player.getPacketSender().sendMessage("The jar shatters as you open it.");
        }
        return true;
    }

    private static DropEntry roll(List<DropEntry> entries) {
        int total = 0;
        for (DropEntry e : entries) {
            total += e.weight;
        }
        int pick = Misc.getRandom(total - 1);
        int current = 0;
        for (DropEntry e : entries) {
            current += e.weight;
            if (pick < current) {
                return e;
            }
        }
        return entries.get(entries.size() - 1);
    }

    private static void table(int jarId, DropEntry... drops) {
        TABLES.put(jarId, Arrays.asList(drops));
    }

    private static DropEntry d(String name, String qty, String rarity) {
        if ("Nothing".equalsIgnoreCase(name)) {
            return new DropEntry(name, -1, 0, 0, weight(rarity));
        }
        boolean noted = qty.contains("(noted)");
        int[] amount = parseAmount(qty);
        int id = resolveItemId(name, noted);
        return new DropEntry(name, id, amount[0], amount[1], weight(rarity));
    }

    private static int resolveItemId(String name, boolean noted) {
        String key = normalizeName(name);
        key = NAME_ALIASES.getOrDefault(key, key);
        int id = ITEM_BY_NAME.getOrDefault((noted ? "noted:" : "base:") + key, -1);
        if (id > 0) {
            return id;
        }
        return ITEM_BY_NAME.getOrDefault("base:" + key, -1);
    }

    private static int[] parseAmount(String qtyText) {
        String cleaned = qtyText.replace("(noted)", "").trim();
        if (cleaned.contains("-")) {
            String[] split = cleaned.split("-");
            return new int[]{Integer.parseInt(split[0].trim()), Integer.parseInt(split[1].trim())};
        }
        return new int[]{Integer.parseInt(cleaned), Integer.parseInt(cleaned)};
    }

    private static int weight(String rarity) {
        String[] split = rarity.split("/");
        int numerator = Integer.parseInt(split[0].trim());
        int denominator = Integer.parseInt(split[1].trim());
        return Math.max(1, (numerator * 10000) / denominator);
    }

    private static String normalizeName(String name) {
        return name.toLowerCase()
                .replace("'", "")
                .replace(".", "")
                .replace("(", "")
                .replace(")", "")
                .replace("+", "")
                .replace("-", "")
                .replace(" ", "")
                .trim();
    }

    private static void indexItems() {
        for (ItemDefinition def : ItemDefinition.definitions.values()) {
            if (def == null || def.getName() == null || def.getName().isEmpty()) {
                continue;
            }
            String key = normalizeName(def.getName());
            ITEM_BY_NAME.putIfAbsent("base:" + key, def.getId());
            if (def.isNoted()) {
                ITEM_BY_NAME.put("noted:" + key, def.getId());
            }
        }
    }

    private static void initAliases() {
        NAME_ALIASES.put(normalizeName("Dragonstone bolt tips"), normalizeName("Dragon bolt tips"));
        NAME_ALIASES.put(normalizeName("Dragonstone bolts"), normalizeName("Dragonstone dragon bolts"));
        NAME_ALIASES.put(normalizeName("Red spiky vambraces"), normalizeName("Red spiky vambs"));
        NAME_ALIASES.put(normalizeName("Rune javelin tips"), normalizeName("Rune javelin heads"));
        NAME_ALIASES.put(normalizeName("Dragon javelin tips"), normalizeName("Dragon javelin heads"));
    }

    private static final class DropEntry {
        private final String displayName;
        private final int itemId;
        private final int minAmount;
        private final int maxAmount;
        private final int weight;

        private DropEntry(String displayName, int itemId, int minAmount, int maxAmount, int weight) {
            this.displayName = displayName;
            this.itemId = itemId;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.weight = weight;
        }

        private int rollAmount() {
            return minAmount == maxAmount ? minAmount : Misc.inclusive(minAmount, maxAmount);
        }
    }
}
