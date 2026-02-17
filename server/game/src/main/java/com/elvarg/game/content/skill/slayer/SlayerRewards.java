package com.elvarg.game.content.skill.slayer;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.content.skill.slayer.Slayer;
import com.elvarg.game.content.skill.slayer.SlayerUnlock;
import com.elvarg.util.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SlayerRewards {

    // Interface IDs from SlayerRewardsWidget
    public static final int INTERFACE_ID = 56000;
    
    // Tabs
    public static final int UNLOCK_TAB = 56002;
    public static final int EXTEND_TAB = 56003;
    public static final int BUY_TAB = 56004;
    public static final int TASKS_TAB = 56005;
    public static final int COSMETICS_TAB = 56006;
    
    // Layers
    public static final int UNLOCK_LAYER = 56010;
    public static final int EXTEND_LAYER = 56020;
    public static final int BUY_LAYER = 56030;
    public static final int TASKS_LAYER = 56040;
    public static final int COSMETICS_LAYER = 56080;
    
    // Text
    public static final int POINTS_TEXT = 56007;
    public static final int CURRENT_TASK_TEXT = 56045;
    
    // Buttons
    public static final int CANCEL_TASK_BUTTON = 56047;
    public static final int BLOCK_TASK_BUTTON = 56048;
    public static final int SWAP_TASK_BUTTON = 56049;
    public static final int VIEW_LIST_BUTTON = 56058;
    
    // Ranges
    public static final int BLOCKED_TASK_START = 56050; // 7 slots
    public static final int UNBLOCK_BUTTON_START = 56060; // 7 buttons

    public static void openRewards(Player player) {
        openRewards(player, -1);
    }

    public static void openRewards(Player player, int npcId) {
        player.getPacketSender().sendInterface(INTERFACE_ID);
        updateInterface(player);
        // Default to Tasks tab as requested/detailed
        switchTab(player, TASKS_LAYER);
    }
    
    public static void updateInterface(Player player) {
        // Points
        player.getPacketSender().sendString(POINTS_TEXT, "Reward points: " + player.getSlayerPoints());
        
        // Current Task
        String taskInfo = "None";
        if (player.getSlayerTask() != null) {
            ActiveSlayerTask active = player.getSlayerTask();
            String name = active.getTask().toString();
            // Add location if Konar? (Not easily available in ActiveSlayerTask without parsing, but name is there)
            taskInfo = active.getRemaining() + " x " + name;
        }
        player.getPacketSender().sendString(CURRENT_TASK_TEXT, taskInfo);
        
        // Blocked Tasks
        List<SlayerTask> blockedTasks = getAllBlockedTasks(player);
        for(int i = 0; i < 7; i++) {
            if (i < blockedTasks.size()) {
                player.getPacketSender().sendString(BLOCKED_TASK_START + i, "Slot " + (i+1) + ": " + blockedTasks.get(i).toString());
            } else {
                player.getPacketSender().sendString(BLOCKED_TASK_START + i, "Slot " + (i+1) + ": Empty");
            }
        }
    }
    
    private static void switchTab(Player player, int layerId) {
        int[] layers = {UNLOCK_LAYER, EXTEND_LAYER, BUY_LAYER, TASKS_LAYER, COSMETICS_LAYER};
        for (int layer : layers) {
            player.getPacketSender().sendInterfaceDisplayState(layer, layer != layerId);
        }
    }
    
    private static List<SlayerTask> getAllBlockedTasks(Player player) {
        List<SlayerTask> all = new ArrayList<>();
        for (List<SlayerTask> list : player.getBlockedSlayerTasks().values()) {
            all.addAll(list);
        }
        return all;
    }

    public static boolean handleButton(Player player, int buttonId) {
        // Tab Switching
        if (buttonId == UNLOCK_TAB) { switchTab(player, UNLOCK_LAYER); return true; }
        if (buttonId == EXTEND_TAB) { switchTab(player, EXTEND_LAYER); return true; }
        if (buttonId == BUY_TAB) { switchTab(player, BUY_LAYER); return true; }
        if (buttonId == TASKS_TAB) { switchTab(player, TASKS_LAYER); return true; }
        if (buttonId == COSMETICS_TAB) { switchTab(player, COSMETICS_LAYER); return true; }
        
        // Task Actions
        if (buttonId == CANCEL_TASK_BUTTON) {
            Slayer.cancelTask(player);
            updateInterface(player);
            return true;
        }
        
        if (buttonId == BLOCK_TASK_BUTTON) {
            Slayer.blockTask(player);
            updateInterface(player);
            return true;
        }
        
        if (buttonId == SWAP_TASK_BUTTON) {
            handleSwap(player);
            updateInterface(player);
            return true;
        }
        
        // Unblock Buttons
        if (buttonId >= UNBLOCK_BUTTON_START && buttonId < UNBLOCK_BUTTON_START + 7) {
            int index = buttonId - UNBLOCK_BUTTON_START;
            handleUnblock(player, index);
            updateInterface(player);
            return true;
        }
        
        return false;
    }
    
    private static void handleSwap(Player player) {
        ActiveSlayerTask current = player.getSlayerTask();
        ActiveSlayerTask stored = player.getStoredSlayerTask();
        
        if (current != null && stored == null) {
            Slayer.storeTask(player);
        } else if (current == null && stored != null) {
            Slayer.unstoreTask(player);
        } else if (current != null && stored != null) {
            // Direct Swap
            player.setSlayerTask(stored);
            player.setStoredSlayerTask(current);
            player.getPacketSender().sendMessage("You have swapped your current task with your stored task.");
        } else {
            player.getPacketSender().sendMessage("You have no tasks to swap.");
        }
    }
    
    private static void handleUnblock(Player player, int index) {
        List<SlayerTask> all = getAllBlockedTasks(player);
        if (index >= all.size()) {
            return;
        }
        
        SlayerTask toRemove = all.get(index);
        
        // Remove from map
        for (Map.Entry<SlayerMaster, List<SlayerTask>> entry : player.getBlockedSlayerTasks().entrySet()) {
            if (entry.getValue().remove(toRemove)) {
                player.getPacketSender().sendMessage("You have unblocked " + toRemove.toString() + ".");
                break;
            }
        }
    }
}
