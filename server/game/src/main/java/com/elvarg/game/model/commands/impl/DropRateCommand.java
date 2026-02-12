package com.elvarg.game.model.commands.impl;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;

public class DropRateCommand implements Command {

    @Override
    public void execute(Player player, String command, String[] parts) {
        player.getPacketSender().sendMessage(
                String.format("Your NPC drop-rate multiplier is currently x%.2f.", player.getNpcDropRateMultiplier())
        );
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }
}

