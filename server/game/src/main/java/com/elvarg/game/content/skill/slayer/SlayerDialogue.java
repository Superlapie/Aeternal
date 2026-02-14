package com.elvarg.game.content.skill.slayer;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.dialogues.DialogueOption;
import com.elvarg.game.model.dialogues.builders.DynamicDialogueBuilder;
import com.elvarg.game.model.dialogues.entries.impl.ActionDialogue;
import com.elvarg.game.model.dialogues.entries.impl.EndDialogue;
import com.elvarg.game.model.dialogues.entries.impl.NpcDialogue;
import com.elvarg.game.model.dialogues.entries.impl.OptionDialogue;
import com.elvarg.game.model.dialogues.entries.impl.PlayerDialogue;

public class SlayerDialogue extends DynamicDialogueBuilder {

    private final SlayerMaster master;

    public SlayerDialogue(SlayerMaster master) {
        this.master = master;
    }

    @Override
    public void build(Player player) {
        int npcId = master.getNpcId();
        String greeting = "'Ello! I am " + master.name() + ". How can I help you?";
        if (master == SlayerMaster.KRYSTILIA) {
            greeting = "Welcome to the Wilderness Slayer master. I specialize in danger.";
        } else if (master == SlayerMaster.KONAR) {
            greeting = "I see your destiny. Are you ready for a challenge?";
        }

        add(new NpcDialogue(0, npcId, greeting));

        add(new OptionDialogue(1, (option) -> {
            switch (option) {
                case FIRST_OPTION:
                    player.getDialogueManager().start(2);
                    break;
                case SECOND_OPTION:
                    player.getDialogueManager().start(10);
                    break;
                case THIRD_OPTION:
                    player.getPacketSender().sendInterfaceRemoval();
                    break;
            }
        }, "I need a Slayer assignment.", "Rewards / Task Management", "Nothing, thanks."));

        // Assignment logic
        add(new ActionDialogue(2, () -> {
            if (player.getSlayerTask() == null) {
                if (Slayer.assign(player, master)) {
                    ActiveSlayerTask task = player.getSlayerTask();
                    String loc = task.getLocationName() != null ? " in " + task.getLocationName() : "";
                    add(new NpcDialogue(3, npcId, "Your new task is to kill " + task.getRemaining() + " " + task.getTask().toString() + loc + "."));
                    add(new PlayerDialogue(4, "Excellent. I'll get right on it."));
                    add(new EndDialogue(5));
                    player.getDialogueManager().start(this, 3);
                }
            } else {
                ActiveSlayerTask task = player.getSlayerTask();
                String loc = task.getLocationName() != null ? " in " + task.getLocationName() : "";
                add(new NpcDialogue(3, npcId, "You're currently assigned to kill " + task.getRemaining() + " " + task.getTask().toString() + loc + "."));
                
                if (master == SlayerMaster.TURAEL && task.getMaster() != SlayerMaster.TURAEL) {
                    add(new OptionDialogue(4, (option) -> {
                        if (option == DialogueOption.FIRST_OPTION) {
                            Slayer.assign(player, SlayerMaster.TURAEL); // Triggers reset logic
                        } else {
                            player.getPacketSender().sendInterfaceRemoval();
                        }
                    }, "Reset my task (Streak will be lost!)", "Nevermind."));
                    player.getDialogueManager().start(this, 3); // Start at the NPC dialogue, then options follow
                } else {
                    add(new PlayerDialogue(4, "Do you have any tips?"));
                    add(new NpcDialogue(5, npcId, task.getTask().getHint()));
                    add(new EndDialogue(6));
                    player.getDialogueManager().start(this, 3);
                }
            }
        }));

        // Rewards / Management
        add(new NpcDialogue(10, npcId, "What would you like to manage? You have " + player.getSlayerPoints() + " points."));
        add(new OptionDialogue(11, (option) -> {
            switch (option) {
                case FIRST_OPTION:
                    Slayer.cancelTask(player);
                    break;
                case SECOND_OPTION:
                    Slayer.blockTask(player);
                    break;
                case THIRD_OPTION:
                    if (player.getStoredSlayerTask() == null) {
                        Slayer.storeTask(player);
                    } else {
                        Slayer.unstoreTask(player);
                    }
                    break;
                case FOURTH_OPTION:
                    player.getPacketSender().sendInterfaceRemoval();
                    break;
            }
        }, "Cancel current task (30 pts)", "Block current task (" + master.getBlockCost() + " pts)", 
           player.getStoredSlayerTask() == null ? "Store current task (100 pts)" : "Unstore task (Free)", "Nevermind"));
    }
}
