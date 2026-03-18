package com.elvarg.game.model.commands.impl;

import com.elvarg.game.content.skill.impl.smithing.Smelting;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.rights.PlayerRights;

/**
 * Test command to verify smelting interface functionality
 */
public class TestSmeltingInterface implements Command {

    @Override
    public void execute(Player player, String command, String[] parts) {
        // Test opening the smelting interface
        player.getPacketSender().sendMessage("Testing smelting interface...");
        Smelting.openFurnaceInterface(player);
        player.getPacketSender().sendMessage("Smelting interface should now be visible with background.");
    }

    @Override
    public boolean canUse(Player player) {
        PlayerRights rights = player.getRights();
        return rights == PlayerRights.OWNER || rights == PlayerRights.DEVELOPER;
    }
}
