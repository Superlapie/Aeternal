package com.elvarg.game.content;

import com.elvarg.game.entity.impl.object.GameObject;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.content.Looting;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.dialogues.DialogueExpression;
import com.elvarg.game.model.dialogues.builders.DynamicDialogueBuilder;
import com.elvarg.game.model.dialogues.entries.impl.NpcDialogue;
import com.elvarg.util.ItemIdentifiers;
import com.elvarg.util.Misc;
import com.elvarg.util.NpcIdentifiers;
import com.elvarg.util.ObjectIdentifiers;

public final class ClueScrolls {

    public static final int BEGINNER_CLUE_TEXT_INTERFACE = 6965;
    public static final int BEGINNER_CLUE_TITLE_LINE = 6967;
    public static final int BEGINNER_CLUE_BODY_START = 6968;
    private static final Location SARADOMIN_STATUE_LOCATION = new Location(2967, 3416, 0);
    private static final int SARADOMIN_STATUE_OBJECT_ID = ObjectIdentifiers.STATUE_OF_SARADOMIN_7;
    private static final BeginnerClueStep[] ACTIVE_BEGINNER_STEPS = {
            BeginnerClueStep.WISE_OLD_MAN,
            BeginnerClueStep.FRED_THE_FARMER,
            BeginnerClueStep.SARADOMIN_STATUE
    };

    private static final Reward[] BEGINNER_REWARDS = {
            new Reward(ItemIdentifiers.COINS, 1_500, 5_000),
            new Reward(ItemIdentifiers.LEATHER_GLOVES, 1, 1),
            new Reward(ItemIdentifiers.LEATHER_BOOTS, 1, 1),
            new Reward(ItemIdentifiers.BRONZE_SWORD, 1, 1),
            new Reward(ItemIdentifiers.IRON_SWORD, 1, 1),
            new Reward(ItemIdentifiers.CLAY, 15, 40),
            new Reward(ItemIdentifiers.BEER, 4, 8),
            new Reward(ItemIdentifiers.EMERALD, 1, 1),
            new Reward(ItemIdentifiers.WILLOW_SHORTBOW, 1, 1),
            new Reward(ItemIdentifiers.WILLOW_LONGBOW, 1, 1)
    };

    private ClueScrolls() {
    }

    public enum BeginnerClueStep {
        NONE,
        STATUE_BECKON,
        GOSSIP_HINT,
        GOSSIP_HORVIK,
        WISE_OLD_MAN,
        FRED_THE_FARMER,
        SARADOMIN_STATUE
    }

    public static boolean isBeginnerClue(int itemId) {
        return itemId == ItemIdentifiers.CLUE_SCROLL_BEGINNER_;
    }

    public static boolean isBeginnerRewardCasket(int itemId) {
        return itemId == ItemIdentifiers.REWARD_CASKET_BEGINNER_;
    }

    public static boolean readBeginnerClue(Player player, int itemId) {
        if (!isBeginnerClue(itemId) || !player.getInventory().contains(itemId)) {
            return false;
        }

        BeginnerClueStep step = player.getBeginnerClueStep();
        if (step == null || step == BeginnerClueStep.NONE) {
            step = ACTIVE_BEGINNER_STEPS[Misc.getRandom(ACTIVE_BEGINNER_STEPS.length - 1)];
            player.setBeginnerClueStep(step);
        }

        showBeginnerClue(player, step);
        return true;
    }

    public static boolean handleNpcInteraction(Player player, NPC npc) {
        BeginnerClueStep step = player.getBeginnerClueStep();
        if (step == null || step == BeginnerClueStep.NONE) {
            return false;
        }

        if (!player.getInventory().contains(ItemIdentifiers.CLUE_SCROLL_BEGINNER_)) {
            player.setBeginnerClueStep(BeginnerClueStep.NONE);
            return false;
        }

        if (step == BeginnerClueStep.WISE_OLD_MAN && npc.getId() == NpcIdentifiers.WISE_OLD_MAN) {
            startWiseOldManDialogue(player);
            return true;
        }

        if (step == BeginnerClueStep.FRED_THE_FARMER && npc.getId() == NpcIdentifiers.FRED_THE_FARMER) {
            startFredDialogue(player);
            return true;
        }

        return false;
    }

    public static boolean handleBeginnerEmote(Player player, int button) {
        if (button != 13383) {
            return false;
        }

        if (player.getBeginnerClueStep() != BeginnerClueStep.SARADOMIN_STATUE) {
            return false;
        }

        if (!player.getInventory().contains(ItemIdentifiers.CLUE_SCROLL_BEGINNER_)) {
            player.setBeginnerClueStep(BeginnerClueStep.NONE);
            return false;
        }

        if (!player.getLocation().isWithinDistance(SARADOMIN_STATUE_LOCATION, 2)) {
            return false;
        }

        GameObject statue = new GameObject(SARADOMIN_STATUE_OBJECT_ID, SARADOMIN_STATUE_LOCATION, 10, 0, null);
        statue.performGraphic(new Graphic(110));
        player.getPacketSender().sendMessage("The gods are pleased, take your reward");
        completeBeginnerClueAndClose(player);
        return true;
    }

    public static boolean openBeginnerRewardCasket(Player player, int itemId) {
        if (!isBeginnerRewardCasket(itemId) || !player.getInventory().contains(itemId)) {
            return false;
        }

        Item reward = rollBeginnerReward();
        player.getInventory().delete(itemId, 1);
        Looting.addOrDrop(player, reward.getId(), reward.getAmount());

        player.getPacketSender().sendInterfaceRemoval();
        player.getPacketSender().sendInterface(6960);
        player.getPacketSender().sendItemOnInterface(6963, reward.getId(), reward.getAmount());
        player.getPacketSender().sendMessage("You open the beginner reward casket.");
        return true;
    }

    private static void completeBeginnerClue(Player player) {
        player.getInventory().delete(ItemIdentifiers.CLUE_SCROLL_BEGINNER_, 1);
        player.setBeginnerClueStep(BeginnerClueStep.NONE);
        player.getInventory().forceAdd(player, new Item(ItemIdentifiers.REWARD_CASKET_BEGINNER_, 1));
        player.getPacketSender().sendMessage("You receive a beginner reward casket.");
    }

    private static void completeBeginnerClueAndClose(Player player) {
        completeBeginnerClue(player);
        player.getPacketSender().sendInterfaceRemoval();
    }

    private static void startWiseOldManDialogue(Player player) {
        player.getDialogueManager().start(new DynamicDialogueBuilder() {
            @Override
            public void build(Player player) {
                String username = player.getUsername();
                add(
                        new NpcDialogue(0, NpcIdentifiers.WISE_OLD_MAN,
                                "Hello Traveller, not here to steal my hat are ya?",
                                DialogueExpression.CALM),
                        new NpcDialogue(1, NpcIdentifiers.WISE_OLD_MAN,
                                "Hahaha, just kidding. Technically it's not mine anyways, although THEY can't prove that.",
                                DialogueExpression.CALM),
                        new NpcDialogue(2, NpcIdentifiers.WISE_OLD_MAN,
                                "Oh, heres your reward " + username + ", old men ramble sometimes..",
                                DialogueExpression.CALM, ClueScrolls::completeBeginnerClueAndClose)
                );
            }
        });
    }

    private static void startFredDialogue(Player player) {
        player.getDialogueManager().start(new DynamicDialogueBuilder() {
            @Override
            public void build(Player player) {
                add(
                        new NpcDialogue(0, NpcIdentifiers.FRED_THE_FARMER,
                                "I can't talk right now.. they are listening... they are ALWAYS listening.",
                                DialogueExpression.CALM),
                        new NpcDialogue(1, NpcIdentifiers.FRED_THE_FARMER,
                                "Take this and run, run fast!!!",
                                DialogueExpression.CALM, ClueScrolls::completeBeginnerClueAndClose)
                );
            }
        });
    }

    private static void showBeginnerClue(Player player, BeginnerClueStep step) {
        player.getPacketSender().sendInterfaceRemoval();
        player.getPacketSender().sendInterface(BEGINNER_CLUE_TEXT_INTERFACE);
        player.getPacketSender().sendString(BEGINNER_CLUE_TITLE_LINE, "Beginner Clue Scroll");

        String[] body = switch (step) {
            case WISE_OLD_MAN -> new String[] {
                    "The Wise Old Man",
                    "Speak to the sage who's party hat",
                    "we all admired, he rests across a",
                    "bank wearing old man attire.",
                    "",
                    "",
                    "",
                    ""
            };
            case FRED_THE_FARMER -> new String[] {
                    "Diary of an estranged farmer,",
                    "I can't prove it, but I swear",
                    "one of my sheep isn't really a sheep,",
                    "but PENGUINS!!!",
                    "I can't tell anyone though,",
                    "who would believe me?",
                    "I should sleep...",
                    ""
            };
            case SARADOMIN_STATUE -> new String[] {
                    "The marbled Saradomin points south,",
                    "icy hills to the north,",
                    "to appease the gods",
                    "bow forth.",
                    "",
                    "",
                    "",
                    ""
            };
            default -> new String[] {
                    "Beginner Clue Scroll",
                    "Read the clue again to",
                    "start a fresh beginner trail.",
                    "",
                    "",
                    "",
                    "",
                    ""
            };
        };

        for (int i = 0; i < 8; i++) {
            player.getPacketSender().sendString(BEGINNER_CLUE_BODY_START + i, body[i]);
        }
    }

    private static boolean isGenericGossipNpc(NPC npc) {
        if (npc.getCurrentDefinition() == null) {
            return false;
        }

        String name = npc.getCurrentDefinition().getName();
        return "Man".equalsIgnoreCase(name) || "Woman".equalsIgnoreCase(name);
    }

    private static Item rollBeginnerReward() {
        Reward reward = BEGINNER_REWARDS[Misc.getRandom(BEGINNER_REWARDS.length - 1)];
        int amount = reward.minAmount == reward.maxAmount
                ? reward.minAmount
                : Misc.getRandom(reward.maxAmount - reward.minAmount) + reward.minAmount;
        return new Item(reward.itemId, amount);
    }

    private static final class Reward {
        private final int itemId;
        private final int minAmount;
        private final int maxAmount;

        private Reward(int itemId, int minAmount, int maxAmount) {
            this.itemId = itemId;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }
    }
}
