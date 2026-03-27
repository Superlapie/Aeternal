package com.elvarg.game.content;

import com.elvarg.game.definition.NpcDropDefinition;
import com.elvarg.game.definition.NpcDropDefinition.DropTable;
import com.elvarg.game.definition.NpcDropDefinition.NPCDrop;
import com.elvarg.game.definition.NpcDropDefinition.RDT;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.container.impl.Equipment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NpcDropTableViewer {

    private static final int INTERFACE_ID = 62000;
    private static final int SCROLL_ID = 62010;
    private static final int ROW_ITEM_START = 62011;
    private static final int ROW_NAME_START = 62111;
    private static final int ROW_BASE_RATE_START = 62211;
    private static final int ROW_CURRENT_RATE_START = 62311;
    private static final int MAX_ROWS = 100;
    private static final int ROW_HEIGHT = 30;

    public static void open(Player player, NPC npc) {
        int definitionId = npc.getCurrentDefinition() != null ? npc.getCurrentDefinition().getId() : npc.getId();
        NpcDropDefinition definition = NpcDropDefinition.get(definitionId).orElse(null);
        if (definition == null) {
            player.getPacketSender().sendMessage("This NPC does not have a drop table.");
            return;
        }

        double baseMultiplier = 1.0D;
        double currentMultiplier = Math.max(0.1D, player.getNpcDropRateMultiplier());
        boolean wearingRingOfWealth = isWearingRingOfWealth(player);

        ExactDropChanceCalculator baseCalculator = new ExactDropChanceCalculator(definition, baseMultiplier, wearingRingOfWealth);
        List<DropRow> rows = buildRows(definition, baseCalculator, currentMultiplier);

        player.getPacketSender().sendInterfaceRemoval();
        player.getPacketSender().sendInterface(INTERFACE_ID);
        player.getPacketSender().sendString(INTERFACE_ID + 4, String.format(Locale.US, "Your drop-rate multiplier: x%.2f", currentMultiplier));
        player.getPacketSender().sendString(INTERFACE_ID + 5, "Base roll and your effective roll are shown below.");
        player.getPacketSender().sendString(INTERFACE_ID + 6, "Item");
        player.getPacketSender().sendString(INTERFACE_ID + 7, "Base roll");
        player.getPacketSender().sendString(INTERFACE_ID + 8, "Current roll");

        int visibleRows = Math.min(rows.size(), MAX_ROWS);
        for (int row = 0; row < visibleRows; row++) {
            DropRow entry = rows.get(row);
            int itemFrame = ROW_ITEM_START + row;
            int nameFrame = ROW_NAME_START + row;
            int baseFrame = ROW_BASE_RATE_START + row;
            int currentFrame = ROW_CURRENT_RATE_START + row;

            player.getPacketSender().sendItemOnInterface(itemFrame, entry.itemId, 0, 1);
            player.getPacketSender().sendString(nameFrame, entry.name);
            player.getPacketSender().sendString(baseFrame, formatProbability(entry.baseProbability));
            player.getPacketSender().sendString(currentFrame, formatProbability(entry.currentProbability));
        }

        for (int row = visibleRows; row < MAX_ROWS; row++) {
            int itemFrame = ROW_ITEM_START + row;
            int nameFrame = ROW_NAME_START + row;
            int baseFrame = ROW_BASE_RATE_START + row;
            int currentFrame = ROW_CURRENT_RATE_START + row;

            player.getPacketSender().clearItemOnInterface(itemFrame);
            player.getPacketSender().sendString(nameFrame, "");
            player.getPacketSender().sendString(baseFrame, "");
            player.getPacketSender().sendString(currentFrame, "");
        }

        if (rows.size() > MAX_ROWS) {
            player.getPacketSender().sendMessage("Drop table truncated to the first " + MAX_ROWS + " rows.");
        }
    }

    private static List<DropRow> buildRows(NpcDropDefinition definition,
                                           ExactDropChanceCalculator baseCalculator,
                                           double currentMultiplier) {
        List<DropRow> rows = new ArrayList<>();

        appendAlwaysDrops(rows, definition.getAlwaysDrops(), baseCalculator, currentMultiplier);
        if (definition.getRdtChance() > 0) {
            appendRdtDrops(rows, definition, baseCalculator, currentMultiplier);
        }
        appendSpecialDrops(rows, definition.getSpecialDrops(), baseCalculator, currentMultiplier);
        appendTableDrops(rows, definition.getCommonDrops(), DropTable.COMMON, baseCalculator, currentMultiplier);
        appendTableDrops(rows, definition.getUncommonDrops(), DropTable.UNCOMMON, baseCalculator, currentMultiplier);
        appendTableDrops(rows, definition.getRareDrops(), DropTable.RARE, baseCalculator, currentMultiplier);
        appendTableDrops(rows, definition.getVeryRareDrops(), DropTable.VERY_RARE, baseCalculator, currentMultiplier);

        return rows;
    }

    private static void appendAlwaysDrops(List<DropRow> rows,
                                          NPCDrop[] drops,
                                          ExactDropChanceCalculator baseCalculator,
                                          double currentMultiplier) {
        if (drops == null) {
            return;
        }

        for (NPCDrop drop : drops) {
            rows.add(new DropRow(drop.getItemId(), itemName(drop.getItemId()), 1.0D, 1.0D));
        }
    }

    private static void appendRdtDrops(List<DropRow> rows,
                                       NpcDropDefinition definition,
                                       ExactDropChanceCalculator baseCalculator,
                                       double currentMultiplier) {
        int rdtLength = RDT.values().length;
        for (int slot = 0; slot < rdtLength; slot++) {
            RDT rdt = RDT.values()[slot];
            double base = baseCalculator.probabilityForRdt(rdt);
            rows.add(new DropRow(
                    rdt.getItemId(),
                    itemName(rdt.getItemId()),
                    base,
                    scaleByMultiplier(base, currentMultiplier)));
        }
    }

    private static void appendSpecialDrops(List<DropRow> rows,
                                           NPCDrop[] drops,
                                           ExactDropChanceCalculator baseCalculator,
                                           double currentMultiplier) {
        if (drops == null) {
            return;
        }

        for (int index = 0; index < drops.length; index++) {
            NPCDrop drop = drops[index];
            double base = baseCalculator.probabilityForSpecial(index);
            rows.add(new DropRow(
                    drop.getItemId(),
                    itemName(drop.getItemId()),
                    base,
                    scaleByMultiplier(base, currentMultiplier)));
        }
    }

    private static void appendTableDrops(List<DropRow> rows,
                                         NPCDrop[] drops,
                                         DropTable table,
                                         ExactDropChanceCalculator baseCalculator,
                                         double currentMultiplier) {
        if (drops == null) {
            return;
        }

        for (int index = 0; index < drops.length; index++) {
            NPCDrop drop = drops[index];
            double base = baseCalculator.probabilityForTable(table, index);
            rows.add(new DropRow(
                    drop.getItemId(),
                    itemName(drop.getItemId()),
                    base,
                    scaleByMultiplier(base, currentMultiplier)));
        }
    }

    private static String itemName(int itemId) {
        Item item = new Item(itemId, 1);
        if (item.getDefinition() == null) {
            return "Item " + itemId;
        }
        return item.getDefinition().getName();
    }

    private static boolean isWearingRingOfWealth(Player player) {
        return player.getEquipment().getItems()[Equipment.RING_SLOT] != null
                && player.getEquipment().getItems()[Equipment.RING_SLOT].getId() == 2572;
    }

    private static String formatProbability(double probability) {
        if (probability >= 0.999999999D) {
            return "Always";
        }
        if (probability <= 0.0D) {
            return "Never";
        }

        long oneIn = Math.max(1L, (long) Math.ceil(1.0D / probability));
        return String.format(Locale.US, "1/%,d (%s)", oneIn, formatPercent(probability));
    }

    private static String formatPercent(double probability) {
        double percent = probability * 100.0D;
        if (percent >= 1.0D) {
            return String.format(Locale.US, "%.2f%%", percent);
        }
        if (percent >= 0.1D) {
            return String.format(Locale.US, "%.3f%%", percent);
        }
        return String.format(Locale.US, "%.4f%%", percent);
    }

    private static double scaleByMultiplier(double baseProbability, double multiplier) {
        if (baseProbability <= 0.0D) {
            return 0.0D;
        }
        if (baseProbability >= 1.0D) {
            return 1.0D;
        }
        return Math.min(1.0D, baseProbability * Math.max(0.1D, multiplier));
    }

    private static final class DropRow {
        private final int itemId;
        private final String name;
        private final double baseProbability;
        private final double currentProbability;

        private DropRow(int itemId, String name, double baseProbability, double currentProbability) {
            this.itemId = itemId;
            this.name = name;
            this.baseProbability = baseProbability;
            this.currentProbability = currentProbability;
        }
    }

    private enum TargetType {
        ALWAYS,
        SPECIAL,
        TABLE,
        RDT
    }

    private enum TableKind {
        COMMON(1 << 0),
        UNCOMMON(1 << 1),
        RARE(1 << 2),
        VERY_RARE(1 << 3),
        SPECIAL(1 << 4);

        private final int bit;

        TableKind(int bit) {
            this.bit = bit;
        }
    }

    private static final class ExactDropChanceCalculator {

        private final NpcDropDefinition definition;
        private final double multiplier;
        private final boolean wearingRingOfWealth;
        private final double pCommon;
        private final double pUncommon;
        private final double pRare;
        private final double pVeryRare;
        private final double pNone;
        private final double[] specialSuccessProbabilities;
        private final double pRdtTrigger;
        private final int rdtSlots;

        private final Map<Long, Double> memo = new HashMap<>();

        private TargetType targetType;
        private TableKind targetTableKind;
        private int targetTableSize;
        private int targetIndex;
        private int targetRdtChance;
        private int targetSpecialCount;
        private double targetSpecialSuccessChance;
        private int targetRdtItemId;

        private ExactDropChanceCalculator(NpcDropDefinition definition, double multiplier, boolean wearingRingOfWealth) {
            this.definition = definition;
            this.multiplier = Math.max(0.1D, multiplier);
            this.wearingRingOfWealth = wearingRingOfWealth;
            this.pCommon = tableProbability(90.0D, 40.0D);
            this.pUncommon = tableProbability(40.0D, 6.0D);
            this.pRare = tableProbability(6.0D, 0.6D);
            this.pVeryRare = tableProbability(0.6D, 0.0D);
            this.pNone = Math.max(0.0D, 1.0D - (pCommon + pUncommon + pRare + pVeryRare));
            this.specialSuccessProbabilities = buildSpecialSuccessProbabilities(definition.getSpecialDrops());
            this.pRdtTrigger = rollProbability(definition.getRdtChance());
            this.rdtSlots = wearingRingOfWealth ? RDT.values().length : 128;
        }

        private double probabilityForRdt(RDT rdt) {
            if (definition.getRdtChance() <= 0) {
                return 0.0D;
            }

            double itemChance = rollProbability(rdt.getChance());
            if (itemChance <= 0.0D) {
                return 0.0D;
            }

            double slotChance = 1.0D / rdtSlots;
            return pRdtTrigger * slotChance * itemChance;
        }

        private double probabilityForSpecial(int index) {
            if (definition.getSpecialDrops() == null || index < 0 || index >= definition.getSpecialDrops().length) {
                return 0.0D;
            }

            setSpecialTarget(index);
            memo.clear();
            double probability = chance(0, 3);
            return applyRdtGate(probability);
        }

        private double probabilityForTable(DropTable table, int index) {
            NPCDrop[] drops = dropsForTable(table);
            if (drops == null || index < 0 || index >= drops.length) {
                return 0.0D;
            }

            setTableTarget(table, index);
            memo.clear();
            double probability = chance(0, 3);
            return applyRdtGate(probability);
        }

        private double probabilityForRdt(int index) {
            if (index < 0 || index >= RDT.values().length) {
                return 0.0D;
            }

            return probabilityForRdt(RDT.values()[index]);
        }

        private double probabilityForAlways() {
            return 1.0D;
        }

        private void setSpecialTarget(int index) {
            this.targetType = TargetType.SPECIAL;
            this.targetTableKind = TableKind.SPECIAL;
            this.targetTableSize = definition.getSpecialDrops().length;
            this.targetIndex = index;
            this.targetSpecialCount = targetTableSize;
            this.targetSpecialSuccessChance = specialSuccessProbabilities[index];
        }

        private void setTableTarget(DropTable table, int index) {
            this.targetType = TargetType.TABLE;
            this.targetTableKind = switch (table) {
                case COMMON -> TableKind.COMMON;
                case UNCOMMON -> TableKind.UNCOMMON;
                case RARE -> TableKind.RARE;
                case VERY_RARE -> TableKind.VERY_RARE;
                default -> throw new IllegalStateException("Unexpected table: " + table);
            };
            this.targetIndex = index;
            this.targetTableSize = dropsForTable(table).length;
            this.targetSpecialCount = 0;
            this.targetSpecialSuccessChance = 0.0D;
        }

        private NPCDrop[] dropsForTable(DropTable table) {
            return switch (table) {
                case COMMON -> definition.getCommonDrops();
                case UNCOMMON -> definition.getUncommonDrops();
                case RARE -> definition.getRareDrops();
                case VERY_RARE -> definition.getVeryRareDrops();
                default -> null;
            };
        }

        private double chance(int mask, int rollsRemaining) {
            if (rollsRemaining <= 0) {
                return 0.0D;
            }

            if (targetType == TargetType.SPECIAL && (mask & TableKind.SPECIAL.bit) != 0) {
                return 0.0D;
            }

            if (targetType == TargetType.TABLE && (mask & targetTableKind.bit) != 0) {
                return 0.0D;
            }

            if (targetType == TargetType.RDT) {
                return 0.0D;
            }

            long key = (((long) mask) << 3) | rollsRemaining;
            Double cached = memo.get(key);
            if (cached != null) {
                return cached;
            }

            double result;
            if (isSpecialAvailable(mask)) {
                result = specialPhase(mask, rollsRemaining);
            } else {
                result = mainRoll(mask, rollsRemaining - 1);
            }

            memo.put(key, result);
            return result;
        }

        private double specialPhase(int mask, int rollsRemaining) {
            NPCDrop[] specialDrops = definition.getSpecialDrops();
            if (specialDrops == null || specialDrops.length == 0) {
                return mainRoll(mask, rollsRemaining - 1);
            }

            double totalSpecialSuccess = 0.0D;
            double result = 0.0D;

            for (int i = 0; i < specialDrops.length; i++) {
                double successProbability = specialSuccessProbabilities[i];
                if (successProbability <= 0.0D) {
                    continue;
                }

                totalSpecialSuccess += successProbability;
                if (targetType == TargetType.SPECIAL && i == targetIndex) {
                    result += successProbability;
                } else {
                    result += successProbability * chance(mask | TableKind.SPECIAL.bit, rollsRemaining - 1);
                }
            }

            double specialFail = Math.max(0.0D, 1.0D - totalSpecialSuccess);
            result += specialFail * mainRoll(mask, rollsRemaining - 1);
            return result;
        }

        private double mainRoll(int mask, int futureRolls) {
            double result = 0.0D;

            result += pCommon * tableOutcome(mask, futureRolls, TableKind.COMMON, definition.getCommonDrops());
            result += pUncommon * tableOutcome(mask, futureRolls, TableKind.UNCOMMON, definition.getUncommonDrops());
            result += pRare * tableOutcome(mask, futureRolls, TableKind.RARE, definition.getRareDrops());
            result += pVeryRare * tableOutcome(mask, futureRolls, TableKind.VERY_RARE, definition.getVeryRareDrops());
            result += pNone * chance(mask, futureRolls);

            return result;
        }

        private double tableOutcome(int mask, int futureRolls, TableKind tableKind, NPCDrop[] drops) {
            if (drops == null || drops.length == 0) {
                return chance(mask, futureRolls);
            }

            if ((mask & tableKind.bit) != 0) {
                return chance(mask, futureRolls);
            }

            if (targetType == TargetType.TABLE && targetTableKind == tableKind) {
                double success = 1.0D / targetTableSize;
                double other = 1.0D - success;
                return success + other * chance(mask | tableKind.bit, futureRolls);
            }

            return chance(mask | tableKind.bit, futureRolls);
        }

        private boolean isSpecialAvailable(int mask) {
            return definition.getSpecialDrops() != null
                    && definition.getSpecialDrops().length > 0
                    && (mask & TableKind.SPECIAL.bit) == 0;
        }

        private double applyRdtGate(double probability) {
            if (definition.getRdtChance() <= 0) {
                return probability;
            }
            return (1.0D - pRdtTrigger) * probability;
        }

        private double[] buildSpecialSuccessProbabilities(NPCDrop[] specialDrops) {
            if (specialDrops == null || specialDrops.length == 0) {
                return new double[0];
            }

            double[] values = new double[specialDrops.length];
            for (int i = 0; i < specialDrops.length; i++) {
                values[i] = rollProbability(specialDrops[i].getChance()) / specialDrops.length;
            }
            return values;
        }

        private double rollProbability(int baseChance) {
            if (baseChance <= 0) {
                return 0.0D;
            }
            int adjustedChance = (int) Math.ceil(baseChance / multiplier);
            if (adjustedChance < 1) {
                adjustedChance = 1;
            }
            return 1.0D / adjustedChance;
        }

        private double tableProbability(double upperBound, double lowerBound) {
            double upper = Math.min(upperBound * multiplier, 100.0D);
            double lower = Math.min(lowerBound * multiplier, 100.0D);
            return Math.max(0.0D, (upper - lower) / 100.0D);
        }
    }
}
