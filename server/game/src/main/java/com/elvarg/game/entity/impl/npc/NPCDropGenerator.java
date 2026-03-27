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

    private final Player player;
    private final NpcDropDefinition def;

    public NPCDropGenerator(Player player, NpcDropDefinition def) {
        this.player = player;
        this.def = def;
    }

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
