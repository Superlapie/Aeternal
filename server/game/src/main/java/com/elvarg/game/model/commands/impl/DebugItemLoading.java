package com.elvarg.game.model.commands.impl;

import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.rights.PlayerRights;

public class DebugItemLoading implements Command {
    private static final int[] MOON_ITEM_IDS = {
            28991, // Atlatl dart
            29000, // Eclipse atlatl
            29004, // Eclipse moon chestplate
            29007, // Eclipse moon tassets
            29010, // Eclipse moon helm
            29013, // Blood moon helm
            29016, // Blood moon chestplate
            29019, // Blood moon tassets
            29022, // Dual macuahuitl
            29025, // Blue moon helm
            29028, // Blue moon chestplate
            29031, // Blue moon tassets
            29034  // Blue moon spear
    };

    @Override
    public void execute(Player player, String command, String[] parts) {
        player.getPacketSender().sendMessage("Debugging item loading...");
        
        // Check total items loaded
        int totalItems = ItemDefinition.definitions.size();
        player.getPacketSender().sendMessage("Total items loaded: " + totalItems);
        
        player.getPacketSender().sendMessage("Checking moon item IDs:");
        for (int id : MOON_ITEM_IDS) {
            ItemDefinition def = ItemDefinition.definitions.get(id);
            if (def != null && def.getName() != null) {
                player.getPacketSender().sendMessage("ID " + id + ": " + def.getName());
            } else {
                player.getPacketSender().sendMessage("ID " + id + ": NOT FOUND");
            }
        }
        
        // Test search for "eclipse" 
        player.getPacketSender().sendMessage("Testing search for 'eclipse':");
        int eclipseCount = 0;
        for (ItemDefinition def : ItemDefinition.definitions.values()) {
            if (def != null && def.getName() != null && def.getName().toLowerCase().contains("eclipse")) {
                player.getPacketSender().sendMessage("Found: " + def.getName() + " (ID: " + def.getId() + ")");
                eclipseCount++;
            }
        }
        player.getPacketSender().sendMessage("Total eclipse items: " + eclipseCount);
        
        // Test search for "moon"
        player.getPacketSender().sendMessage("Testing search for 'moon':");
        int moonCount = 0;
        for (ItemDefinition def : ItemDefinition.definitions.values()) {
            if (def != null && def.getName() != null && def.getName().toLowerCase().contains("moon")) {
                player.getPacketSender().sendMessage("Found: " + def.getName() + " (ID: " + def.getId() + ")");
                moonCount++;
            }
        }
        player.getPacketSender().sendMessage("Total moon items: " + moonCount);
    }

    @Override
    public boolean canUse(Player player) {
        PlayerRights rights = player.getRights();
        return rights == PlayerRights.OWNER || rights == PlayerRights.DEVELOPER;
    }
}
