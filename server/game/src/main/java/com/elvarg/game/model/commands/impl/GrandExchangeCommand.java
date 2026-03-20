package com.elvarg.game.model.commands.impl;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.teleportation.TeleportHandler;

public class GrandExchangeCommand implements Command {

    private static final Location GRAND_EXCHANGE = new Location(3163, 3479, 0);

    @Override
    public void execute(Player player, String command, String[] parts) {
        if (!TeleportHandler.checkReqs(player, GRAND_EXCHANGE)) {
            return;
        }
        TeleportHandler.teleport(player, GRAND_EXCHANGE, player.getSpellbook().getTeleportType(), false);
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }
}
