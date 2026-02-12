package com.elvarg.game.model.commands.impl;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.commands.CommandManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommandsCommand implements Command {

    private static final int COMMANDS_PER_LINE = 8;

    @Override
    public void execute(Player player, String command, String[] parts) {
        List<String> availableCommands = new ArrayList<>();

        for (Map.Entry<String, Command> entry : CommandManager.commands.entrySet()) {
            Command mappedCommand = entry.getValue();
            if (mappedCommand != null && mappedCommand.canUse(player)) {
                availableCommands.add(entry.getKey().toLowerCase());
            }
        }

        Collections.sort(availableCommands);

        player.getPacketSender().sendMessage("Available commands (" + availableCommands.size() + "):");

        if (availableCommands.isEmpty()) {
            player.getPacketSender().sendMessage("No commands available.");
            return;
        }

        StringBuilder lineBuilder = new StringBuilder();
        int lineCount = 0;

        for (String alias : availableCommands) {
            if (lineBuilder.length() > 0) {
                lineBuilder.append(", ");
            }
            lineBuilder.append("::").append(alias);
            lineCount++;

            if (lineCount == COMMANDS_PER_LINE) {
                player.getPacketSender().sendMessage(lineBuilder.toString());
                lineBuilder.setLength(0);
                lineCount = 0;
            }
        }

        if (lineBuilder.length() > 0) {
            player.getPacketSender().sendMessage(lineBuilder.toString());
        }
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }
}
