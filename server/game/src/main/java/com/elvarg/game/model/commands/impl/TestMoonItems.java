package com.elvarg.game.model.commands.impl;

import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.rights.PlayerRights;

public class TestMoonItems implements Command {
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
        player.getPacketSender().sendMessage("Testing moon item definitions...");
        
        for (int id : MOON_ITEM_IDS) {
            ItemDefinition def = ItemDefinition.definitions.get(id);
            if (def != null) {
                player.getPacketSender().sendMessage("Item " + id + ": " + def.getName() + " - Found!");
            } else {
                player.getPacketSender().sendMessage("Item " + id + ": NOT FOUND");
            }
        }
        
        player.getPacketSender().sendMessage("Total item definitions loaded: " + ItemDefinition.definitions.size());
    }

    @Override
    public boolean canUse(Player player) {
        PlayerRights rights = player.getRights();
        return rights == PlayerRights.OWNER || rights == PlayerRights.DEVELOPER;
    }
}
