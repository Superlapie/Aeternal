package com.elvarg.game.model.commands.impl;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.commands.CommandInterface;

public class CommandsCommand implements Command {

    @Override
    public void execute(Player player, String command, String[] parts) {
        CommandInterface.open(player);
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }
}
