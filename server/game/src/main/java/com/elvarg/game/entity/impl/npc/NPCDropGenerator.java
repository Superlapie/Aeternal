package com.elvarg.game.entity.impl.npc;

import com.elvarg.game.collision.RegionManager;
import com.elvarg.game.definition.NpcDropDefinition;
import com.elvarg.game.definition.NpcDropDefinition.DropTable;
import com.elvarg.game.definition.NpcDropDefinition.NPCDrop;
import com.elvarg.game.definition.NpcDropDefinition.RDT;
import com.elvarg.game.entity.impl.grounditem.ItemOnGroundManager;
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

    /**
     * The {@link Player} whose generating a drop.
     */
    private final Player player;
    /**
     * The {@link NpcDropDefinition} this drop is for.
     */
    private final NpcDropDefinition def;

    /**
     * Constructor
     *
     * @param player
     * @param def
     */
    public NPCDropGenerator(Player player, NpcDropDefinition def) {
        this.player = player;
        this.def = def;
    }

    /**
     * Attempts to start a new generator using the given entities.
     *
     * @param player
     * @param npc
     */
    public static void start(Player player, NPC npc) {
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

    /**
     * Generates a list of items from the drop definition that will be dropped for a
     * player.
     *
     * @return
     */
    public List<Item> getDropList() {
        // The {@RandomGen} which will help us randomize drops..
        RandomGen random = new RandomGen();

        // The list containing the {@link Item} that will be dropped for the player.
        List<Item> items = new LinkedList<>();

        // The list containing the drop tables which we've gone through.
        List<DropTable> parsedTables = new ArrayList<DropTable>();

        // Drop "always" items..
        if (def.getAlwaysDrops() != null) {
            for (NPCDrop drop : def.getAlwaysDrops()) {
                items.add(drop.toItem(random));
            }
        }

        // Handle RDT.. If a drop is generated from RDT, no further items should be
        // given.
        // There are 128 slots in the rdt, many empty. When a player is wearing ring of
        // wealth, the empty slots are not counted.
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

        // Handle unique drops..
        // The amount of items the player will receive from the unique drop tables.
        // Note: A player cannot receive multiple drops from the same drop table.
        int rolls = 1 + random.get().nextInt(3);
        for (int i = 0; i < rolls; i++) {
            Optional<DropTable> table = Optional.empty();

            // Check if we should access the special drop table..
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

            // If we didn't get a special drop, attempt to find a different table..
            if (!table.isPresent()) {
                double chance = random.get().nextDouble(100) / getDropRateMultiplier();
                if ((table = getDropTable(chance)).isPresent()) {
                    // Make sure we haven't already parsed this table.
                    if (parsedTables.contains(table.get())) {
                        continue;
                    }
                    // Get the items related to this drop table..
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
                    // Get a random drop from the table..
                    NPCDrop npcDrop = dropTableItems.get()[random.get().nextInt(dropTableItems.get().length)];

                    // Add the drop to the drop list.
                    items.add(npcDrop.toItem(random));

                    // Flag this table as visited..
                    parsedTables.add(table.get());
                }
            }
        }
        return items;
    }

    /**
     * Checks if the player is wearing a ring of wealth which will increase the
     * chances for getting a good drop.
     *
     * @return
     */
    public boolean wearingRingOfWealth() {
        return player.getEquipment().getItems()[Equipment.RING_SLOT].getId() == 2572;
    }

    /**
     * Attempts to fetch the drop table for the given chance.
     *
     * @param drop
     * @return
     */
    public Optional<DropTable> getDropTable(double chance) {
        Optional<DropTable> table = Optional.empty();
        // Fetch one of the ordinary drop tables
        // based on our chance.
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
