package com.elvarg.game.entity.impl.npc;

import com.elvarg.game.collision.RegionManager;
import com.elvarg.game.definition.NpcDropDefinition;
import com.elvarg.game.definition.NpcDropDefinition.DropTable;
import com.elvarg.game.definition.NpcDropDefinition.NPCDrop;
import com.elvarg.game.definition.NpcDropDefinition.RDT;
import com.elvarg.game.entity.impl.grounditem.ItemOnGroundManager;
import com.elvarg.game.entity.impl.npc.impl.TormentedDemon;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.container.impl.Equipment;
import com.elvarg.util.NpcIdentifiers;
import com.elvarg.util.RandomGen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class NPCDropGenerator {

    // Tormented Demon drop constants (existing server item ids only).
    private static final int INFERNAL_ASHES = 25778;
    private static final int TORMENTED_SYNAPSE = 29580;
    private static final int BURNING_CLAWS = 29577;
    private static final int RUNE_PLATEBODY = 1127;
    private static final int DRAGON_DAGGER = 1215;
    private static final int BATTLESTAFF_NOTED = 1392;
    private static final int RUNE_KITESHIELD = 1201;
    private static final int CHAOS_RUNE = 562;
    private static final int RUNE_ARROW = 892;
    private static final int SOUL_RUNE = 566;
    private static final int GRIMY_KWUARM = 213;
    private static final int GRIMY_DWARF_WEED = 217;
    private static final int GRIMY_CADANTINE = 215;
    private static final int GRIMY_LANTADYME = 2485;
    private static final int GRIMY_AVANTOE = 211;
    private static final int GRIMY_RANARR = 207;
    private static final int GRIMY_SNAPDRAGON = 3051;
    private static final int GRIMY_TORSTOL = 219;
    private static final int RANARR_SEED = 5295;
    private static final int SNAPDRAGON_SEED = 5300;
    private static final int TORSTOL_SEED = 5304;
    private static final int WATERMELON_SEED = 5321;
    private static final int WILLOW_SEED = 5313;
    private static final int MAHOGANY_SEED = 21488;
    private static final int MAPLE_SEED = 5314;
    private static final int TEAK_SEED = 21486;
    private static final int YEW_SEED = 5315;
    private static final int PAPAYA_TREE_SEED = 5288;
    private static final int MAGIC_SEED = 5316;
    private static final int PALM_TREE_SEED = 5289;
    private static final int SPIRIT_SEED = 5317;
    private static final int DRAGONFRUIT_TREE_SEED = 22877;
    private static final int CELASTRUS_SEED = 22869;
    private static final int REDWOOD_TREE_SEED = 22871;
    private static final int MANTA_RAY = 391;
    private static final int PRAYER_POTION_4 = 2434;
    private static final int PRAYER_POTION_2 = 141;
    private static final int MAGIC_SHORTBOW_U_NOTED = 73;
    private static final int MALICIOUS_ASHES = 25772;
    private static final int FIRE_ORB_NOTED = 570;
    private static final int DRAGON_ARROWTIPS = 11237;
    private static final int MAGIC_LONGBOW_U = 70;
    private static final int CLUE_SCROLL_ELITE = 12073;

    private final Player player;
    private final NpcDropDefinition def;

    public NPCDropGenerator(Player player, NpcDropDefinition def) {
        this.player = player;
        this.def = def;
    }

    public static void start(Player player, NPC npc) {
        if (TormentedDemon.isTormentedDemon(npc)) {
            List<Item> items = generateTormentedDemonDrops();
            Location dropLocation = resolveDropLocation(player, npc);
            for (Item item : items) {
                if (!item.getDefinition().isStackable()) {
                    for (int i = 0; i < item.getAmount(); i++) {
                        ItemOnGroundManager.register(player, new Item(item.getId(), 1), dropLocation);
                    }
                } else {
                    ItemOnGroundManager.register(player, item, dropLocation);
                }
            }
            return;
        }

        Optional<NpcDropDefinition> def = NpcDropDefinition.get(npc.getId());
        if (def.isPresent()) {
            NPCDropGenerator gen = new NPCDropGenerator(player, def.get());
            Location dropLocation = resolveDropLocation(player, npc);
            for (Item item : gen.getDropList()) {
                if (!item.getDefinition().isStackable()) {
                    for (int i = 0; i < item.getAmount(); i++) {
                        ItemOnGroundManager.register(player, new Item(item.getId(), 1), dropLocation);
                    }
                } else {
                    ItemOnGroundManager.register(player, item, dropLocation);
                }
            }
        }
    }

    private static List<Item> generateTormentedDemonDrops() {
        RandomGen random = new RandomGen();
        List<Item> items = new LinkedList<>();

        items.add(new Item(INFERNAL_ASHES, 1));

        // Exact unique chain from wiki notes.
        if (roll(random, 1, 500)) {
            items.add(new Item(TORMENTED_SYNAPSE, 1));
            return items;
        }

        if (roll(random, 499, 250000)) {
            items.add(new Item(BURNING_CLAWS, 1));
            return items;
        }

        // Smouldering-part phase consumed as null outcomes (user requested these drops removed).
        if (roll(random, 1, 25) || roll(random, 1, 25) || roll(random, 1, 125)) {
            return items;
        }

        // Standard table: literal per-item odds from provided OSRS wiki rates.
        maybeAdd(items, random, 4, 51, new Item(RUNE_PLATEBODY, 1));
        maybeAdd(items, random, 3, 51, new Item(DRAGON_DAGGER, 1));
        maybeAdd(items, random, 3, 51, new Item(BATTLESTAFF_NOTED, 1));
        maybeAdd(items, random, 2, 51, new Item(RUNE_KITESHIELD, 1));

        maybeAdd(items, random, 4, 51, new Item(CHAOS_RUNE, random.inclusive(25, 100)));
        maybeAdd(items, random, 4, 51, new Item(RUNE_ARROW, random.inclusive(65, 125)));
        maybeAdd(items, random, 2, 51, new Item(SOUL_RUNE, random.inclusive(50, 75)));

        maybeAdd(items, random, 10, 408, new Item(GRIMY_KWUARM, 1));
        maybeAdd(items, random, 8, 408, new Item(GRIMY_DWARF_WEED, 1));
        maybeAdd(items, random, 8, 408, new Item(GRIMY_CADANTINE, 1));
        maybeAdd(items, random, 6, 408, new Item(GRIMY_LANTADYME, 1));
        maybeAdd(items, random, 5, 408, new Item(GRIMY_AVANTOE, 1));
        maybeAdd(items, random, 4, 408, new Item(GRIMY_RANARR, 1));
        maybeAdd(items, random, 4, 408, new Item(GRIMY_SNAPDRAGON, 1));
        maybeAdd(items, random, 3, 408, new Item(GRIMY_TORSTOL, 1));

        maybeAdd(items, random, 1, 425, new Item(RANARR_SEED, 1));
        maybeAdd(items, random, 5, 2277, new Item(SNAPDRAGON_SEED, 1)); // ~1/455.4
        maybeAdd(items, random, 2, 1159, new Item(TORSTOL_SEED, 1)); // ~1/579.5
        maybeAdd(items, random, 10, 6071, new Item(WATERMELON_SEED, 15)); // ~1/607.1
        maybeAdd(items, random, 2, 1275, new Item(WILLOW_SEED, 1)); // ~1/637.5
        maybeAdd(items, random, 10, 7083, new Item(MAHOGANY_SEED, 1)); // ~1/708.3
        maybeAdd(items, random, 10, 7083, new Item(MAPLE_SEED, 1)); // ~1/708.3
        maybeAdd(items, random, 10, 7083, new Item(TEAK_SEED, 1)); // ~1/708.3
        maybeAdd(items, random, 10, 7083, new Item(YEW_SEED, 1)); // ~1/708.3
        maybeAdd(items, random, 10, 9107, new Item(PAPAYA_TREE_SEED, 1)); // ~1/910.7
        maybeAdd(items, random, 10, 11591, new Item(MAGIC_SEED, 1)); // ~1/1159.1
        maybeAdd(items, random, 1, 1275, new Item(PALM_TREE_SEED, 1));
        maybeAdd(items, random, 10, 15937, new Item(SPIRIT_SEED, 1)); // ~1/1593.7
        maybeAdd(items, random, 1, 2125, new Item(DRAGONFRUIT_TREE_SEED, 1));
        maybeAdd(items, random, 2, 6375, new Item(CELASTRUS_SEED, 1)); // ~1/3187.5
        maybeAdd(items, random, 2, 6375, new Item(REDWOOD_TREE_SEED, 1)); // ~1/3187.5

        maybeAdd(items, random, 4, 51, new Item(MANTA_RAY, random.inclusive(1, 2)));
        maybeAdd(items, random, 1, 51, new Item(PRAYER_POTION_4, 1));
        maybeAdd(items, random, 1, 51, new Item(PRAYER_POTION_2, 2));

        maybeAdd(items, random, 29, 255, new Item(MAGIC_SHORTBOW_U_NOTED, 1));
        // 1/12 Guthixian temple teleport intentionally skipped (not added per request).
        maybeAdd(items, random, 2, 51, new Item(MALICIOUS_ASHES, random.inclusive(2, 3)));
        maybeAdd(items, random, 2, 51, new Item(FIRE_ORB_NOTED, random.inclusive(5, 7)));
        maybeAdd(items, random, 1, 51, new Item(DRAGON_ARROWTIPS, random.inclusive(30, 40)));
        maybeAdd(items, random, 1, 255, new Item(MAGIC_LONGBOW_U, 1));

        // Tertiary.
        maybeAdd(items, random, 1, 128, new Item(CLUE_SCROLL_ELITE, 1));

        return items;
    }

    private static void maybeAdd(List<Item> items, RandomGen random, int numerator, int denominator, Item item) {
        if (roll(random, numerator, denominator)) {
            items.add(item);
        }
    }

    private static boolean roll(RandomGen random, int numerator, int denominator) {
        if (denominator <= 0 || numerator <= 0) {
            return false;
        }
        if (numerator >= denominator) {
            return true;
        }
        return random.get().nextInt(denominator) < numerator;
    }

    private static Location resolveDropLocation(Player player, NPC npc) {
        if (!requiresWalkableDropTile(npc)) {
            return npc.getLocation().clone();
        }

        final Location center = npc.getLocation();
        final var area = npc.getPrivateArea();

        if (!RegionManager.blocked(center, area)) {
            return center.clone();
        }

        Location best = null;
        int bestPlayerDistance = Integer.MAX_VALUE;

        for (int radius = 1; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) {
                        continue;
                    }

                    Location tile = center.transform(dx, dy);
                    if (RegionManager.blocked(tile, area)) {
                        continue;
                    }

                    int playerDistance = player.getLocation().getDistance(tile);
                    if (best == null || playerDistance < bestPlayerDistance) {
                        best = tile;
                        bestPlayerDistance = playerDistance;
                    }
                }
            }

            if (best != null) {
                return best;
            }
        }

        return center.clone();
    }

    private static boolean requiresWalkableDropTile(NPC npc) {
        int id = npc.getId();
        return id == NpcIdentifiers.ZULRAH
                || id == NpcIdentifiers.ZULRAH_2
                || id == NpcIdentifiers.ZULRAH_3
                || id == 8058
                || id == 8059
                || id == 8060
                || id == 8061;
    }

    public List<Item> getDropList() {
        RandomGen random = new RandomGen();
        List<Item> items = new LinkedList<>();
        List<DropTable> parsedTables = new ArrayList<>();

        if (def.getAlwaysDrops() != null) {
            for (NPCDrop drop : def.getAlwaysDrops()) {
                items.add(drop.toItem(random));
            }
        }

        if (def.getRdtChance() > 0 && rollWithMultiplier(random, def.getRdtChance())) {
            int rdtLength = RDT.values().length;
            int slots = wearingRingOfWealth() ? rdtLength : 128;
            int slot = random.get().nextInt(slots);
            if (slot < rdtLength) {
                RDT rdtDrop = RDT.values()[slot];
                if (rollWithMultiplier(random, rdtDrop.getChance())) {
                    items.add(new Item(rdtDrop.getItemId(), rdtDrop.getAmount()));
                    return items;
                }
            }
        }

        int rolls = 1 + random.get().nextInt(3);
        for (int i = 0; i < rolls; i++) {
            Optional<DropTable> table = Optional.empty();

            if (def.getSpecialDrops() != null && !parsedTables.contains(DropTable.SPECIAL)) {
                if (def.getSpecialDrops().length > 0) {
                    NPCDrop drop = def.getSpecialDrops()[random.get().nextInt(def.getSpecialDrops().length)];
                    if (drop.getChance() > 0 && rollWithMultiplier(random, drop.getChance())) {
                        items.add(drop.toItem(random));
                        parsedTables.add(DropTable.SPECIAL);
                        continue;
                    }
                }
            }

            if (!table.isPresent()) {
                double chance = random.get().nextDouble(100) / getDropRateMultiplier();
                if ((table = getDropTable(chance)).isPresent()) {
                    if (parsedTables.contains(table.get())) {
                        continue;
                    }
                    Optional<NPCDrop[]> dropTableItems = Optional.empty();
                    switch (table.get()) {
                        case COMMON:
                            if (def.getCommonDrops() != null) {
                                dropTableItems = Optional.of(def.getCommonDrops());
                            }
                            break;
                        case UNCOMMON:
                            if (def.getUncommonDrops() != null) {
                                dropTableItems = Optional.of(def.getUncommonDrops());
                            }
                            break;
                        case RARE:
                            if (def.getRareDrops() != null) {
                                dropTableItems = Optional.of(def.getRareDrops());
                            }
                            break;
                        case VERY_RARE:
                            if (def.getVeryRareDrops() != null) {
                                dropTableItems = Optional.of(def.getVeryRareDrops());
                            }
                            break;
                        default:
                            break;
                    }
                    if (!dropTableItems.isPresent()) {
                        continue;
                    }

                    NPCDrop npcDrop = dropTableItems.get()[random.get().nextInt(dropTableItems.get().length)];
                    items.add(npcDrop.toItem(random));
                    parsedTables.add(table.get());
                }
            }
        }
        return items;
    }

    public boolean wearingRingOfWealth() {
        return player.getEquipment().getItems()[Equipment.RING_SLOT].getId() == 2572;
    }

    public Optional<DropTable> getDropTable(double chance) {
        Optional<DropTable> table = Optional.empty();
        for (DropTable dropTable : DropTable.values()) {
            if (dropTable.getRandomRequired() >= 0) {
                if (chance <= dropTable.getRandomRequired()) {
                    table = Optional.of(dropTable);
                }
            }
        }
        return table;
    }

    private boolean rollWithMultiplier(RandomGen random, int baseChance) {
        int adjustedChance = (int) Math.ceil(baseChance / getDropRateMultiplier());
        if (adjustedChance < 1) {
            adjustedChance = 1;
        }
        return random.get().nextInt(adjustedChance) == 0;
    }

    private double getDropRateMultiplier() {
        return Math.max(0.1, player.getNpcDropRateMultiplier());
    }
}
