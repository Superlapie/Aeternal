package com.runescape.graphics.widget;

import com.runescape.graphics.GameFont;
import com.runescape.graphics.sprite.Sprite;

public class SlayerRewardsWidget extends Widget {

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
    
    // Task Layer Components
    public static final int POINTS_TEXT = 56007;
    public static final int CURRENT_TASK_TEXT = 56045;
    public static final int BLOCKED_TASK_START = 56050; // 56050-56056 (7 slots)
    public static final int UNBLOCK_BUTTON_START = 56060; // 56060-56066 (7 buttons)
    
    public static void widget() {
        Widget tab = addTabInterface(INTERFACE_ID);

        // Background (Using Spawn Tab background for clean look)
        addSpriteLoader(56001, 196); 
        
        // Close Button
        closeButton(56008, 142, 143); 

        // Title
        addText(56009, "Slayer Rewards", fonts, 2, 0xff981f, true, true);
        
        // Points display (Top Right area)
        addText(POINTS_TEXT, "Reward points: 0", fonts, 1, 0xff981f, false, true);

        // Tabs
        String[] tabNames = {"Unlock", "Extend", "Buy", "Tasks", "Cosmetics"};
        int[] tabIds = {UNLOCK_TAB, EXTEND_TAB, BUY_TAB, TASKS_TAB, COSMETICS_TAB};
        
        for(int i = 0; i < tabNames.length; i++) {
            addHoverButton(tabIds[i], 164, 60, 20, tabNames[i], -1, tabIds[i] + 100, 1); // +100 for text ID placeholder
            addText(tabIds[i] + 100, tabNames[i], fonts, 0, 0xffffff, true, true);
        }

        // Initialize Layers
        tasksLayer();
        // Placeholder layers for others (empty for now)
        addLayer(UNLOCK_LAYER);
        addLayer(EXTEND_LAYER);
        addLayer(BUY_LAYER);
        addLayer(COSMETICS_LAYER);

        tab.totalChildren(20); // Count carefully!
        int childNum = 0;

        // Background - Centered roughly (Screen is 512x334)
        // Sprite 196 is approx 496x305 (Spawn Tab). Let's place it to center.
        // Screen Center (256, 167). Image Center (248, 152).
        // 256 - 248 = 8. 167 - 152 = 15.
        int bgX = 8;
        int bgY = 15;
        
        setBounds(56001, bgX, bgY, childNum++, tab);
        
        // Close Button (Top Right of BG) - Adjust relative to new BG
        setBounds(56008, bgX + 465, bgY + 8, childNum++, tab);
        
        // Title (Centered Top)
        setBounds(56009, bgX + 248, bgY + 12, childNum++, tab);
        
        // Tabs Row (Below Title)
        int tabStartX = bgX + 60;
        int tabY = bgY + 40;
        for(int i = 0; i < tabNames.length; i++) {
            setBounds(tabIds[i], tabStartX + (i * 75), tabY, childNum++, tab);
            setBounds(tabIds[i] + 100, tabStartX + (i * 75) + 30, tabY + 4, childNum++, tab); // Text centered on button
        }
        
        // Points Text (Right of tabs - wait, tabs take up most space now)
        // Move points text to below title or top right corner
        setBounds(POINTS_TEXT, bgX + 380, bgY + 15, childNum++, tab);
        
        // Layers (Below Tabs)
        // Only showing Tasks layer for now
        setBounds(TASKS_LAYER, bgX + 15, tabY + 30, childNum++, tab);
        
        // Placeholders (Hidden by default server-side)
        setBounds(UNLOCK_LAYER, bgX + 15, tabY + 30, childNum++, tab);
        setBounds(EXTEND_LAYER, bgX + 15, tabY + 30, childNum++, tab);
        setBounds(BUY_LAYER, bgX + 15, tabY + 30, childNum++, tab);
        setBounds(COSMETICS_LAYER, bgX + 15, tabY + 30, childNum++, tab);
    }
    
    private static void addLayer(int id) {
        Widget layer = addTabInterface(id);
        layer.totalChildren(0);
    }

    public static void tasksLayer() {
        Widget layer = addTabInterface(TASKS_LAYER);
        
        // Description Text
        int colorOrange = 0x804000;
        int colorRed = 0x800000;
        
        addText(56041, "You may spend points to Cancel or Block your current task", fonts, 0, colorOrange, false, true);
        addText(56042, "If you cancel it, you may be assigned that target again in the future (30 points)", fonts, 0, colorOrange, false, true); // Partial red handled via string code usually, or split. Using orange for now.
        addText(56043, "If you block it, you will not get that assignment again. (80 points)", fonts, 0, colorOrange, false, true);
        addText(56044, "You may also store your current task for later.", fonts, 0, colorOrange, false, true);
        
        // Current Assignment Box
        addText(56046, "Current assignment:", fonts, 1, colorOrange, false, true);
        addText(CURRENT_TASK_TEXT, "None", fonts, 1, 0xffffff, false, true);
        
        // Buttons
        addHoverButton(56047, 60, 20, 20, "Cancel", -1, 56047+50, 1); // Cancel
        addText(56047+50, "Cancel", fonts, 0, 0xffffff, true, true);
        
        addHoverButton(56048, 60, 20, 20, "Block", -1, 56048+50, 1); // Block
        addText(56048+50, "Block", fonts, 0, 0xffffff, true, true);
        
        addHoverButton(56049, 60, 20, 20, "Swap", -1, 56049+50, 1); // Swap
        addText(56049+50, "Swap", fonts, 0, 0xffffff, true, true);
        
        addHoverButton(56058, 60, 20, 20, "View List", -1, 56058+50, 1); // View List
        addText(56058+50, "List", fonts, 0, 0xffffff, true, true);
        
        // Blocked Tasks Title
        addText(56059, "Blocked Tasks:", fonts, 1, 0xff981f, true, true);
        
        // Blocked Tasks List (7 rows)
        int childrenCount = 4 + 2 + 8 + 1 + (7 * 3); // Text(4) + Current(2) + Buttons(8) + Title(1) + List(7*3) = 36
        layer.totalChildren(childrenCount);
        int child = 0;
        
        // Description
        setBounds(56041, 5, 0, child++, layer);
        setBounds(56042, 5, 12, child++, layer);
        setBounds(56043, 5, 24, child++, layer);
        setBounds(56044, 5, 36, child++, layer);
        
        // Current Assignment
        setBounds(56046, 5, 60, child++, layer);
        setBounds(CURRENT_TASK_TEXT, 5, 75, child++, layer);
        
        // Buttons (Right of Assignment)
        int btnX = 140;
        int btnY = 60;
        setBounds(56047, btnX, btnY, child++, layer); // Cancel
        setBounds(56047+50, btnX + 30, btnY+4, child++, layer);
        
        setBounds(56048, btnX + 65, btnY, child++, layer); // Block
        setBounds(56048+50, btnX + 95, btnY+4, child++, layer);
        
        setBounds(56049, btnX, btnY + 25, child++, layer); // Swap
        setBounds(56049+50, btnX + 30, btnY + 29, child++, layer);
        
        setBounds(56058, btnX + 65, btnY + 25, child++, layer); // List
        setBounds(56058+50, btnX + 95, btnY + 29, child++, layer);
        
        // Blocked Tasks
        setBounds(56059, 135, 110, child++, layer); // Title
        
        for(int i = 0; i < 7; i++) {
            int yPos = 130 + (i * 20);
            addText(BLOCKED_TASK_START + i, "Slot " + (i+1) + ": Empty", fonts, 0, 0xffffff, false, true);
            addHoverButton(UNBLOCK_BUTTON_START + i, 50, 16, 20, "Unblock", -1, UNBLOCK_BUTTON_START + i + 100, 1);
            addText(UNBLOCK_BUTTON_START + i + 100, "Unblock", fonts, 0, 0xffffff, true, true);
            
            setBounds(BLOCKED_TASK_START + i, 20, yPos, child++, layer);
            setBounds(UNBLOCK_BUTTON_START + i, 200, yPos, child++, layer);
            setBounds(UNBLOCK_BUTTON_START + i + 100, 225, yPos + 3, child++, layer);
        }
    }
}