package com.elvarg.game.content;

import com.elvarg.game.definition.ItemDefinition;
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
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;
import com.elvarg.util.ItemIdentifiers;
import com.elvarg.util.Misc;
import com.elvarg.util.NpcIdentifiers;
import com.elvarg.util.ObjectIdentifiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ClueScrolls {

    public static final int BEGINNER_CLUE_TEXT_INTERFACE = 6965;
    public static final int BEGINNER_CLUE_TITLE_LINE = 6967;
    public static final int BEGINNER_CLUE_BODY_START = 6968;
    private static final int EASY_CLUE_MEMORY_INTERFACE = 56700;
    private static final int EASY_CLUE_MEMORY_BACKGROUND = 56701;
    private static final int EASY_CLUE_MEMORY_BORDER = 56702;
    private static final int EASY_CLUE_MEMORY_TITLE = 56703;
    private static final int EASY_CLUE_MEMORY_TEXT = 56704;
    private static final int EASY_CLUE_MEMORY_ROUND_TEXT = 56705;
    private static final int EASY_CLUE_MEMORY_STATUS_TEXT = 56706;
    private static final int EASY_CLUE_MEMORY_FLASH_SLOT = 56710;
    private static final int[] EASY_CLUE_MEMORY_SELECTION_SLOTS = {
            56711, 56712, 56713, 56714
    };
    private static final int[] EASY_CLUE_MEMORY_SELECTION_BUTTONS = {
            56740, 56741, 56742, 56743
    };
    private static final int[] EASY_CLUE_MEMORY_SUCCESS_OVERLAYS = {
            56750, 56751, 56752, 56753
    };
    private static final int[] EASY_CLUE_MEMORY_FAILURE_OVERLAYS = {
            56754, 56755, 56756, 56757
    };
    private static final int EASY_CLUE_MEMORY_FLASH_DELAY = 2;
    private static final int EASY_CLUE_MEMORY_CLOSE = 56720;
    private static final Location SARADOMIN_STATUE_LOCATION = new Location(2967, 3416, 0);
    private static final Location AUBURY_LOCATION = new Location(3253, 3402, 0);
    private static final int SARADOMIN_STATUE_OBJECT_ID = ObjectIdentifiers.STATUE_OF_SARADOMIN_7;
    private static final BeginnerClueStep[] ACTIVE_BEGINNER_STEPS = {
            BeginnerClueStep.WISE_OLD_MAN,
            BeginnerClueStep.FRED_THE_FARMER,
            BeginnerClueStep.SARADOMIN_STATUE
    };
    private static final int[] EASY_CLUE_RUNE_POOL = {
            ItemIdentifiers.FIRE_RUNE,
            ItemIdentifiers.WATER_RUNE,
            ItemIdentifiers.AIR_RUNE,
            ItemIdentifiers.EARTH_RUNE,
            ItemIdentifiers.MIND_RUNE,
            ItemIdentifiers.NATURE_RUNE,
            ItemIdentifiers.CHAOS_RUNE
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

    private static final Reward[] EASY_REWARDS = {
            new Reward(ItemIdentifiers.COINS, 2_500, 10_000),
            new Reward(ItemIdentifiers.EMERALD, 1, 1),
            new Reward(ItemIdentifiers.LEATHER_BOOTS, 1, 1),
            new Reward(ItemIdentifiers.WILLOW_SHORTBOW, 1, 1),
            new Reward(ItemIdentifiers.WILLOW_LONGBOW, 1, 1),
            new Reward(ItemIdentifiers.AIR_RUNE, 25, 75),
            new Reward(ItemIdentifiers.WATER_RUNE, 25, 75),
            new Reward(ItemIdentifiers.NATURE_RUNE, 5, 15),
            new Reward(ItemIdentifiers.LAW_RUNE, 5, 15),
            new Reward(ItemIdentifiers.COSMIC_RUNE, 10, 25)
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

    public enum EasyCluePhase {
        FLASHING,
        SELECTING
    }

    public static boolean isBeginnerClue(int itemId) {
        return itemId == ItemIdentifiers.CLUE_SCROLL_BEGINNER_;
    }

    public static boolean isBeginnerRewardCasket(int itemId) {
        return itemId == ItemIdentifiers.REWARD_CASKET_BEGINNER_;
    }

    public static boolean isEasyClue(int itemId) {
        return itemId == ItemIdentifiers.CLUE_SCROLL_EASY_
                || isClueDefinitionNamed(itemId, "Clue scroll (easy)");
    }

    public static boolean isEasyRewardCasket(int itemId) {
        return itemId == ItemIdentifiers.REWARD_CASKET_EASY_
                || isClueDefinitionNamed(itemId, "Reward casket (easy)");
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

    public static boolean readEasyClue(Player player, int itemId) {
        if (!isEasyClue(itemId) || findClueItemId(player, "clue scroll (easy)") == -1) {
            return false;
        }

        showEasyClue(player);
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

    public static boolean handleEasyThinkEmote(Player player, int button) {
        if (button != 162) {
            return false;
        }

        if (player.getEasyClueSession() != null) {
            return true;
        }

        if (findClueItemId(player, "clue scroll (easy)") == -1) {
            return false;
        }

        if (!player.getLocation().isWithinDistance(AUBURY_LOCATION, 5)) {
            return false;
        }

        startAuburyMemoryGame(player);
        return true;
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

    public static boolean handleEasyMemoryBoardClick(Player player, int interfaceId, int itemId, int slot) {
        EasyClueSession session = player.getEasyClueSession();
        if (session == null || !isEasyMemoryWidget(interfaceId)) {
            return false;
        }

        if (interfaceId == EASY_CLUE_MEMORY_FLASH_SLOT) {
            return true;
        }

        if (findClueItemId(player, "clue scroll (easy)") == -1) {
            cancelEasyClueSession(player);
            return true;
        }

        if (session.getPhase() != EasyCluePhase.SELECTING || session.isResolvingSelection()) {
            return true;
        }

        if (itemId <= 0) {
            return true;
        }

        int slotIndex = resolveEasyMemorySlotIndex(interfaceId);
        if (slotIndex == -1) {
            return true;
        }

        handleEasyMemorySelection(player, session, slotIndex, itemId);
        return true;
    }

    public static boolean handleEasyMemoryButtonClick(Player player, int button) {
        EasyClueSession session = player.getEasyClueSession();
        if (session == null || session.getPhase() != EasyCluePhase.SELECTING || session.isResolvingSelection()) {
            return false;
        }

        int slotIndex = -1;
        for (int i = 0; i < EASY_CLUE_MEMORY_SELECTION_BUTTONS.length; i++) {
            if (EASY_CLUE_MEMORY_SELECTION_BUTTONS[i] == button) {
                slotIndex = i;
                break;
            }
        }

        if (slotIndex == -1) {
            return false;
        }

        if (findClueItemId(player, "clue scroll (easy)") == -1) {
            cancelEasyClueSession(player);
            return true;
        }

        int selectedRune = session.getBoardItems()[slotIndex];
        if (selectedRune <= 0) {
            return true;
        }

        handleEasyMemorySelection(player, session, slotIndex, selectedRune);
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

    public static boolean openEasyRewardCasket(Player player, int itemId) {
        if (!isEasyRewardCasket(itemId) || !player.getInventory().contains(itemId)) {
            return false;
        }

        Item reward = rollEasyReward();
        player.getInventory().delete(itemId, 1);
        Looting.addOrDrop(player, reward.getId(), reward.getAmount());

        player.getPacketSender().sendInterfaceRemoval();
        player.getPacketSender().sendInterface(6960);
        player.getPacketSender().sendItemOnInterface(6963, reward.getId(), reward.getAmount());
        player.getPacketSender().sendMessage("You open the easy reward casket.");
        return true;
    }

    private static void showEasyClue(Player player) {
        player.getPacketSender().sendInterfaceRemoval();
        player.getPacketSender().sendInterface(BEGINNER_CLUE_TEXT_INTERFACE);
        player.getPacketSender().sendString(BEGINNER_CLUE_TITLE_LINE, "Easy Clue Scroll");

        String[] body = {
                "Think real hard at",
                "the place of runes and",
                "magic, the man who runs",
                "the places name is as",
                "beautiful as an auburn",
                "sky.",
                "",
                ""
        };

        for (int i = 0; i < 8; i++) {
            player.getPacketSender().sendString(BEGINNER_CLUE_BODY_START + i, body[i]);
        }
    }

    private static void startAuburyMemoryGame(Player player) {
        EasyClueSession existing = player.getEasyClueSession();
        if (existing != null) {
            TaskManager.cancelTasks(existing);
        }

        EasyClueSession session = new EasyClueSession();
        player.getPacketSender().sendInterfaceRemoval();
        player.setEasyClueSession(session);
        player.getPacketSender().sendInterface(EASY_CLUE_MEMORY_INTERFACE);
        player.getPacketSender().sendString(EASY_CLUE_MEMORY_TITLE, "Aubury's Rune Memory");
        player.getPacketSender().sendString(EASY_CLUE_MEMORY_TEXT, "Watch the runes, then repeat the order.");

        startEasyClueRound(player, session, 0);
    }

    private static void startEasyClueRound(Player player, EasyClueSession session, int roundIndex) {
        TaskManager.cancelTasks(session);

        session.setRoundIndex(roundIndex);
        session.resetProgress();
        session.setPhase(EasyCluePhase.FLASHING);
        hideEasyMemoryFeedback(player);

        player.getPacketSender().sendString(EASY_CLUE_MEMORY_ROUND_TEXT,
                "Round " + (roundIndex + 1) + " of " + session.getRoundCount());
        player.getPacketSender().sendString(EASY_CLUE_MEMORY_STATUS_TEXT, "Memorize the runes.");

        clearEasyClueBoard(player);

        TaskManager.submit(new Task(3, session, true) {
            @Override
            protected void execute() {
                EasyClueSession active = player.getEasyClueSession();
                if (active != session || player.getSession() == null || player.getHitpoints() <= 0) {
                    stop();
                    return;
                }

                int[] sequence = session.getCurrentSequence();
                if (session.getFlashIndex() >= sequence.length) {
                    stop();
                    beginEasyClueSelectionPhase(player, session);
                    return;
                }

                int rune = sequence[session.getFlashIndex()];
                session.incrementFlashIndex();
                player.getPacketSender().sendItemOnInterface(EASY_CLUE_MEMORY_FLASH_SLOT, rune, 1);
            }

            @Override
            public void onTick() {
                if (player.getEasyClueSession() != session || player.getSession() == null || player.getHitpoints() <= 0) {
                    stop();
                }
            }
        });
    }

    private static void beginEasyClueSelectionPhase(Player player, EasyClueSession session) {
        session.setPhase(EasyCluePhase.SELECTING);
        session.resetSelectionIndex();
        session.setResolvingSelection(false);
        player.getPacketSender().clearItemOnInterface(EASY_CLUE_MEMORY_FLASH_SLOT);
        player.getPacketSender().sendString(EASY_CLUE_MEMORY_STATUS_TEXT, "Select the runes in order.");

        int[] board = buildEasyClueBoard(session.getCurrentSequence());
        session.setBoardItems(board);
        for (int i = 0; i < EASY_CLUE_MEMORY_SELECTION_SLOTS.length; i++) {
            int slotId = EASY_CLUE_MEMORY_SELECTION_SLOTS[i];
            int itemId = board[i];
            if (itemId > 0) {
                player.getPacketSender().sendItemOnInterface(slotId, itemId, 1);
            } else {
                player.getPacketSender().clearItemOnInterface(slotId);
            }
        }
    }

    private static void handleEasyMemorySelection(Player player, EasyClueSession session, int slotIndex, int selectedRune) {
        if (session.isResolvingSelection()) {
            return;
        }

        session.setResolvingSelection(true);

        int expected = session.getCurrentSequence()[session.getSelectionIndex()];
        boolean correct = selectedRune == expected;

        flashEasyMemoryTile(player, slotIndex, correct);

        if (correct) {
            session.incrementSelectionIndex();
        } else {
            player.getPacketSender().sendMessage("That was the wrong rune. The sequence resets.");
        }

        TaskManager.submit(new Task(EASY_CLUE_MEMORY_FLASH_DELAY, session, false) {
            @Override
            protected void execute() {
                clearEasyMemoryFeedback(player);

                if (!correct) {
                    session.setResolvingSelection(false);
                    startEasyClueRound(player, session, session.getRoundIndex());
                    stop();
                    return;
                }

                if (session.getSelectionIndex() >= session.getCurrentSequence().length) {
                    if (session.hasMoreRounds()) {
                        player.getPacketSender().sendMessage("Correct. The next sequence is harder.");
                        session.advanceRound();
                        session.setResolvingSelection(false);
                        startEasyClueRound(player, session, session.getRoundIndex());
                    } else {
                        completeEasyClue(player);
                    }
                } else {
                    session.setResolvingSelection(false);
                    player.getPacketSender().sendString(EASY_CLUE_MEMORY_STATUS_TEXT, "Select the runes in order.");
                }

                stop();
            }
        });
    }

    private static void completeEasyClue(Player player) {
        EasyClueSession session = player.getEasyClueSession();
        if (session != null) {
            TaskManager.cancelTasks(session);
        }

        int clueItem = findClueItemId(player, "clue scroll (easy)");
        if (clueItem != -1) {
            player.getInventory().delete(clueItem, 1);
        }
        player.setEasyClueSession(null);
        player.getInventory().forceAdd(player, new Item(ItemIdentifiers.REWARD_CASKET_EASY_, 1));
        player.getPacketSender().sendMessage("You receive an easy reward casket.");
        player.getPacketSender().sendInterfaceRemoval();
    }

    private static void clearEasyClueBoard(Player player) {
        player.getPacketSender().clearItemOnInterface(EASY_CLUE_MEMORY_FLASH_SLOT);
        for (int slotId : EASY_CLUE_MEMORY_SELECTION_SLOTS) {
            player.getPacketSender().clearItemOnInterface(slotId);
        }
    }

    private static void clearEasyMemoryFeedback(Player player) {
        hideEasyMemoryFeedback(player);
    }

    private static void hideEasyMemoryFeedback(Player player) {
        for (int overlayId : EASY_CLUE_MEMORY_SUCCESS_OVERLAYS) {
            player.getPacketSender().sendInterfaceDisplayState(overlayId, true);
        }
        for (int overlayId : EASY_CLUE_MEMORY_FAILURE_OVERLAYS) {
            player.getPacketSender().sendInterfaceDisplayState(overlayId, true);
        }
    }

    private static void flashEasyMemoryTile(Player player, int slotIndex, boolean success) {
        int overlayId = success ? EASY_CLUE_MEMORY_SUCCESS_OVERLAYS[slotIndex] : EASY_CLUE_MEMORY_FAILURE_OVERLAYS[slotIndex];
        int otherOverlayId = success ? EASY_CLUE_MEMORY_FAILURE_OVERLAYS[slotIndex] : EASY_CLUE_MEMORY_SUCCESS_OVERLAYS[slotIndex];

        player.getPacketSender().sendInterfaceDisplayState(otherOverlayId, true);
        player.getPacketSender().sendInterfaceDisplayState(overlayId, false);
    }

    private static int resolveEasyMemorySlotIndex(int interfaceId) {
        for (int i = 0; i < EASY_CLUE_MEMORY_SELECTION_SLOTS.length; i++) {
            if (EASY_CLUE_MEMORY_SELECTION_SLOTS[i] == interfaceId) {
                return i;
            }
        }
        return -1;
    }

    private static int[] buildEasyClueBoard(int[] sequence) {
        List<Integer> items = new ArrayList<>();
        for (int rune : sequence) {
            items.add(rune);
        }
        while (items.size() < EASY_CLUE_MEMORY_SELECTION_SLOTS.length) {
            items.add(-1);
        }
        Collections.shuffle(items);

        int[] board = new int[EASY_CLUE_MEMORY_SELECTION_SLOTS.length];
        for (int i = 0; i < board.length; i++) {
            board[i] = items.get(i);
        }
        return board;
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

    private static boolean isClueDefinitionNamed(int itemId, String clueName) {
        ItemDefinition definition = ItemDefinition.forId(itemId);
        String name = definition.getName();
        if (name == null || name.isEmpty()) {
            return false;
        }
        return name.toLowerCase(Locale.ROOT).startsWith(clueName.toLowerCase(Locale.ROOT));
    }

    private static int findClueItemId(Player player, String clueName) {
        for (Item item : player.getInventory().getItems()) {
            if (item == null || item.getId() <= 0) {
                continue;
            }

            if (isClueDefinitionNamed(item.getId(), clueName)) {
                return item.getId();
            }
        }
        return -1;
    }

    private static boolean isEasyMemoryWidget(int interfaceId) {
        if (interfaceId == EASY_CLUE_MEMORY_FLASH_SLOT) {
            return true;
        }
        for (int slotId : EASY_CLUE_MEMORY_SELECTION_SLOTS) {
            if (interfaceId == slotId) {
                return true;
            }
        }
        return false;
    }

    public static void cancelEasyClueSession(Player player) {
        EasyClueSession session = player.getEasyClueSession();
        if (session != null) {
            TaskManager.cancelTasks(session);
            player.setEasyClueSession(null);
        }
    }

    private static Item rollBeginnerReward() {
        Reward reward = BEGINNER_REWARDS[Misc.getRandom(BEGINNER_REWARDS.length - 1)];
        int amount = reward.minAmount == reward.maxAmount
                ? reward.minAmount
                : Misc.getRandom(reward.maxAmount - reward.minAmount) + reward.minAmount;
        return new Item(reward.itemId, amount);
    }

    private static Item rollEasyReward() {
        Reward reward = EASY_REWARDS[Misc.getRandom(EASY_REWARDS.length - 1)];
        int amount = reward.minAmount == reward.maxAmount
                ? reward.minAmount
                : Misc.getRandom(reward.maxAmount - reward.minAmount) + reward.minAmount;
        return new Item(reward.itemId, amount);
    }

    private static int[] generateDistinctRunes(int amount) {
        List<Integer> pool = new ArrayList<>();
        for (int rune : EASY_CLUE_RUNE_POOL) {
            pool.add(rune);
        }

        int[] sequence = new int[amount];
        for (int i = 0; i < amount; i++) {
            int index = Misc.getRandom(pool.size() - 1);
            sequence[i] = pool.remove(index);
        }
        return sequence;
    }

    public static final class EasyClueSession {
        private final int[][] rounds = new int[][] {
                generateDistinctRunes(3),
                generateDistinctRunes(4)
        };
        private int roundIndex = 0;
        private int flashIndex = 0;
        private int selectionIndex = 0;
        private int[] boardItems = new int[EASY_CLUE_MEMORY_SELECTION_SLOTS.length];
        private EasyCluePhase phase = EasyCluePhase.FLASHING;
        private boolean resolvingSelection = false;

        public int[] getCurrentSequence() {
            return rounds[roundIndex];
        }

        public int getRoundIndex() {
            return roundIndex;
        }

        public int getRoundCount() {
            return rounds.length;
        }

        public boolean hasMoreRounds() {
            return roundIndex + 1 < rounds.length;
        }

        public void advanceRound() {
            roundIndex++;
            resetProgress();
        }

        public int getFlashIndex() {
            return flashIndex;
        }

        public void incrementFlashIndex() {
            flashIndex++;
        }

        public int getSelectionIndex() {
            return selectionIndex;
        }

        public void incrementSelectionIndex() {
            selectionIndex++;
        }

        public void resetProgress() {
            flashIndex = 0;
            selectionIndex = 0;
            boardItems = new int[EASY_CLUE_MEMORY_SELECTION_SLOTS.length];
            resolvingSelection = false;
        }

        public int[] getBoardItems() {
            return boardItems;
        }

        public void setBoardItems(int[] boardItems) {
            this.boardItems = boardItems;
        }

        public EasyCluePhase getPhase() {
            return phase;
        }

        public void setPhase(EasyCluePhase phase) {
            this.phase = phase;
        }

        public void setRoundIndex(int roundIndex) {
            this.roundIndex = roundIndex;
        }

        public void resetSelectionIndex() {
            this.selectionIndex = 0;
        }

        public boolean isResolvingSelection() {
            return resolvingSelection;
        }

        public void setResolvingSelection(boolean resolvingSelection) {
            this.resolvingSelection = resolvingSelection;
        }
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
