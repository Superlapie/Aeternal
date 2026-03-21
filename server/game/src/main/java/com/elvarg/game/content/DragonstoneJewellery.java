package com.elvarg.game.content;

import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.container.impl.Equipment;
import com.elvarg.game.model.dialogues.builders.DynamicDialogueBuilder;
import com.elvarg.game.model.dialogues.entries.impl.OptionDialogue;
import com.elvarg.game.model.teleportation.TeleportHandler;
import com.elvarg.game.model.teleportation.TeleportType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DragonstoneJewellery {

    private static final Pattern CHARGE_PATTERN = Pattern.compile("\\((\\d+)\\)");
    private static final Map<Integer, JewelleryData> JEWELLERY_BY_ID = new HashMap<>();
    private static final Map<String, Integer> ID_BY_NAME = new HashMap<>();
    private static final Map<String, Integer> UNNOTED_ID_BY_NAME = new HashMap<>();
    private static final Map<String, List<ChargeVariant>> VARIANTS_BY_BASE = new HashMap<>();

    private static final Location EDGEVILLE = new Location(3087, 3496);
    private static final Location KARAMJA = new Location(2918, 3176);
    private static final Location DRAYNOR = new Location(3105, 3251);
    private static final Location AL_KHARID = new Location(3293, 3163);
    private static final Location FISHING_GUILD = new Location(2612, 3390);
    private static final Location MINING_GUILD = new Location(3046, 9763);
    private static final Location CRAFTING_GUILD = new Location(2933, 3288);
    private static final Location COOKING_GUILD = new Location(3143, 3440);
    private static final Location WOODCUTTING_GUILD = new Location(1662, 3505);
    private static final Location FARMING_GUILD = new Location(1249, 3726);
    private static final Location WARRIORS_GUILD = new Location(2882, 3549);
    private static final Location CHAMPIONS_GUILD = new Location(3191, 3367);
    private static final Location MONASTERY = new Location(3053, 3486);
    private static final Location RANGING_GUILD = new Location(2656, 3440);
    private static final Location GRAND_EXCHANGE = new Location(3164, 3487);
    private static final Location FALADOR_PARK = new Location(2994, 3377);
    private static final Location MISCELLANIA = new Location(2525, 3861);

    static {
        for (ItemDefinition def : ItemDefinition.definitions.values()) {
            if (def == null || def.getName() == null || def.getName().isEmpty()) {
                continue;
            }
            ID_BY_NAME.put(def.getName(), def.getId());
            if (!def.isNoted()) {
                UNNOTED_ID_BY_NAME.put(def.getName(), def.getId());
            }
            String baseName = baseName(def.getName());
            int charges = parseCharges(def.getName());
            VARIANTS_BY_BASE.computeIfAbsent(baseName, k -> new ArrayList<>())
                    .add(new ChargeVariant(def.getName(), def.getId(), charges, def.isNoted()));
        }
        for (List<ChargeVariant> variants : VARIANTS_BY_BASE.values()) {
            variants.sort(Comparator.comparingInt(v -> -v.charges));
        }

        for (ItemDefinition def : ItemDefinition.definitions.values()) {
            if (def == null || def.getName() == null || def.getName().isEmpty()) {
                continue;
            }
            JewelleryType type = typeForName(def.getName());
            if (type == null) {
                continue;
            }
            String lower = def.getName().toLowerCase();
            boolean eternal = lower.contains("eternal");
            int charges = parseCharges(def.getName());
            int nextId = eternal ? -1 : getNextUnnotedId(def.getName(), charges);
            JEWELLERY_BY_ID.put(def.getId(), new JewelleryData(type, eternal, nextId));
        }
    }

    private DragonstoneJewellery() {
    }

    public static boolean rubFromInventory(Player player, int itemId, int slot) {
        JewelleryData data = JEWELLERY_BY_ID.get(itemId);
        if (data == null) {
            return false;
        }
        openMenu(player, data.type, () -> {
            if (slot < 0 || slot >= player.getInventory().capacity()) {
                return false;
            }
            return player.getInventory().getItems()[slot].getId() == itemId;
        }, () -> {
            if (!data.eternal && data.nextId > 0) {
                player.getInventory().setItem(slot, new Item(data.nextId, 1));
                player.getInventory().refreshItems();
            }
        });
        return true;
    }

    public static boolean rubFromEquipment(Player player, int itemId, int slot) {
        JewelleryData data = JEWELLERY_BY_ID.get(itemId);
        if (data == null) {
            return false;
        }
        openMenu(player, data.type, () -> {
            if (slot != Equipment.AMULET_SLOT && slot != Equipment.RING_SLOT && slot != Equipment.HANDS_SLOT) {
                return false;
            }
            return player.getEquipment().getItems()[slot].getId() == itemId;
        }, () -> {
            if (!data.eternal && data.nextId > 0) {
                player.getEquipment().setItem(slot, new Item(data.nextId, 1));
                player.getEquipment().refreshItems();
            }
        });
        return true;
    }

    private static void openMenu(Player player, JewelleryType type, ItemStillPresentCheck presentCheck, ChargeConsumer chargeConsumer) {
        switch (type) {
            case GLORY:
                openGloryMenu(player, presentCheck, chargeConsumer);
                break;
            case SKILLS_NECKLACE:
                openSkillsNecklaceMenu(player, presentCheck, chargeConsumer, false);
                break;
            case COMBAT_BRACELET:
                openCombatBraceletMenu(player, presentCheck, chargeConsumer);
                break;
            case RING_OF_WEALTH:
                openRingOfWealthMenu(player, presentCheck, chargeConsumer);
                break;
        }
    }

    private static void openGloryMenu(Player player, ItemStillPresentCheck presentCheck, ChargeConsumer chargeConsumer) {
        player.getDialogueManager().start(new DynamicDialogueBuilder() {
            @Override
            public void build(Player p) {
                add(new OptionDialogue(0, "Where would you like to teleport to?", option -> {
                    Location destination;
                    switch (option) {
                        case FIRST_OPTION:
                            destination = EDGEVILLE;
                            break;
                        case SECOND_OPTION:
                            destination = KARAMJA;
                            break;
                        case THIRD_OPTION:
                            destination = DRAYNOR;
                            break;
                        case FOURTH_OPTION:
                            destination = AL_KHARID;
                            break;
                        default:
                            p.getPacketSender().sendInterfaceRemoval();
                            return;
                    }
                    if (!presentCheck.isStillPresent()) {
                        p.getPacketSender().sendMessage("You can't do that right now.");
                        p.getPacketSender().sendInterfaceRemoval();
                        return;
                    }
                    if (!TeleportHandler.checkReqs(p, destination)) {
                        return;
                    }
                    chargeConsumer.consume();
                    TeleportHandler.teleport(p, destination, TeleportType.TELE_TAB, false);
                    p.getPacketSender().sendInterfaceRemoval();
                }, "Edgeville", "Karamja", "Draynor Village", "Al Kharid", "Nowhere"));
            }
        });
    }

    private static void openSkillsNecklaceMenu(Player player, ItemStillPresentCheck presentCheck, ChargeConsumer chargeConsumer, boolean secondPage) {
        player.getDialogueManager().start(new DynamicDialogueBuilder() {
            @Override
            public void build(Player p) {
                if (!secondPage) {
                    add(new OptionDialogue(0, "Where would you like to teleport to?", option -> {
                        switch (option) {
                            case FIRST_OPTION:
                                doTeleport(p, FISHING_GUILD, presentCheck, chargeConsumer);
                                break;
                            case SECOND_OPTION:
                                doTeleport(p, MINING_GUILD, presentCheck, chargeConsumer);
                                break;
                            case THIRD_OPTION:
                                doTeleport(p, CRAFTING_GUILD, presentCheck, chargeConsumer);
                                break;
                            case FOURTH_OPTION:
                                doTeleport(p, COOKING_GUILD, presentCheck, chargeConsumer);
                                break;
                            case FIFTH_OPTION:
                                openSkillsNecklaceMenu(p, presentCheck, chargeConsumer, true);
                                break;
                        }
                    }, "Fishing Guild", "Mining Guild", "Crafting Guild", "Cooking Guild", "More options"));
                } else {
                    add(new OptionDialogue(0, "Where would you like to teleport to?", option -> {
                        switch (option) {
                            case FIRST_OPTION:
                                doTeleport(p, WOODCUTTING_GUILD, presentCheck, chargeConsumer);
                                break;
                            case SECOND_OPTION:
                                doTeleport(p, FARMING_GUILD, presentCheck, chargeConsumer);
                                break;
                            case THIRD_OPTION:
                                openSkillsNecklaceMenu(p, presentCheck, chargeConsumer, false);
                                break;
                            default:
                                p.getPacketSender().sendInterfaceRemoval();
                                break;
                        }
                    }, "Woodcutting Guild", "Farming Guild", "Back", "Nowhere"));
                }
            }
        });
    }

    private static void openCombatBraceletMenu(Player player, ItemStillPresentCheck presentCheck, ChargeConsumer chargeConsumer) {
        player.getDialogueManager().start(new DynamicDialogueBuilder() {
            @Override
            public void build(Player p) {
                add(new OptionDialogue(0, "Where would you like to teleport to?", option -> {
                    switch (option) {
                        case FIRST_OPTION:
                            doTeleport(p, WARRIORS_GUILD, presentCheck, chargeConsumer);
                            break;
                        case SECOND_OPTION:
                            doTeleport(p, CHAMPIONS_GUILD, presentCheck, chargeConsumer);
                            break;
                        case THIRD_OPTION:
                            doTeleport(p, MONASTERY, presentCheck, chargeConsumer);
                            break;
                        case FOURTH_OPTION:
                            doTeleport(p, RANGING_GUILD, presentCheck, chargeConsumer);
                            break;
                        default:
                            p.getPacketSender().sendInterfaceRemoval();
                            break;
                    }
                }, "Warriors' Guild", "Champions' Guild", "Monastery", "Ranging Guild", "Nowhere"));
            }
        });
    }

    private static void openRingOfWealthMenu(Player player, ItemStillPresentCheck presentCheck, ChargeConsumer chargeConsumer) {
        player.getDialogueManager().start(new DynamicDialogueBuilder() {
            @Override
            public void build(Player p) {
                add(new OptionDialogue(0, "Where would you like to teleport to?", option -> {
                    switch (option) {
                        case FIRST_OPTION:
                            doTeleport(p, GRAND_EXCHANGE, presentCheck, chargeConsumer);
                            break;
                        case SECOND_OPTION:
                            doTeleport(p, FALADOR_PARK, presentCheck, chargeConsumer);
                            break;
                        case THIRD_OPTION:
                            doTeleport(p, MISCELLANIA, presentCheck, chargeConsumer);
                            break;
                        default:
                            p.getPacketSender().sendInterfaceRemoval();
                            break;
                    }
                }, "Grand Exchange", "Falador Park", "Miscellania", "Nowhere"));
            }
        });
    }

    private static void doTeleport(Player player, Location destination, ItemStillPresentCheck presentCheck, ChargeConsumer chargeConsumer) {
        if (!presentCheck.isStillPresent()) {
            player.getPacketSender().sendMessage("You can't do that right now.");
            player.getPacketSender().sendInterfaceRemoval();
            return;
        }
        if (!TeleportHandler.checkReqs(player, destination)) {
            return;
        }
        chargeConsumer.consume();
        TeleportHandler.teleport(player, destination, TeleportType.TELE_TAB, false);
        player.getPacketSender().sendInterfaceRemoval();
    }

    private static int parseCharges(String itemName) {
        Matcher matcher = CHARGE_PATTERN.matcher(itemName);
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static String baseName(String itemName) {
        Matcher matcher = CHARGE_PATTERN.matcher(itemName);
        return matcher.replaceAll("").trim();
    }

    private static int getNextUnnotedId(String itemName, int charges) {
        String base = baseName(itemName);
        List<ChargeVariant> variants = VARIANTS_BY_BASE.get(base);
        if (variants == null || variants.isEmpty()) {
            return -1;
        }
        if (charges > 1) {
            for (ChargeVariant variant : variants) {
                if (!variant.noted && variant.charges == charges - 1) {
                    return variant.id;
                }
            }
        } else if (charges == 1) {
            for (ChargeVariant variant : variants) {
                if (!variant.noted && variant.charges == 0) {
                    return variant.id;
                }
            }
        }
        String nextName = charges > 1 ? itemName.replace("(" + charges + ")", "(" + (charges - 1) + ")") : base;
        return UNNOTED_ID_BY_NAME.getOrDefault(nextName, ID_BY_NAME.getOrDefault(nextName, -1));
    }

    private static JewelleryType typeForName(String itemName) {
        String lower = itemName.toLowerCase();
        if (lower.startsWith("amulet of glory")) {
            return JewelleryType.GLORY;
        }
        if (lower.startsWith("skills necklace")) {
            return JewelleryType.SKILLS_NECKLACE;
        }
        if (lower.startsWith("combat bracelet")) {
            return JewelleryType.COMBAT_BRACELET;
        }
        if (lower.startsWith("ring of wealth")) {
            return JewelleryType.RING_OF_WEALTH;
        }
        return null;
    }

    private interface ItemStillPresentCheck {
        boolean isStillPresent();
    }

    private interface ChargeConsumer {
        void consume();
    }

    private static final class JewelleryData {
        private final JewelleryType type;
        private final boolean eternal;
        private final int nextId;

        private JewelleryData(JewelleryType type, boolean eternal, int nextId) {
            this.type = type;
            this.eternal = eternal;
            this.nextId = nextId;
        }
    }

    private static final class ChargeVariant {
        private final String name;
        private final int id;
        private final int charges;
        private final boolean noted;

        private ChargeVariant(String name, int id, int charges, boolean noted) {
            this.name = name;
            this.id = id;
            this.charges = charges;
            this.noted = noted;
        }
    }

    private enum JewelleryType {
        GLORY,
        SKILLS_NECKLACE,
        COMBAT_BRACELET,
        RING_OF_WEALTH
    }
}
