package com.elvarg.game.model.commands.impl;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.rights.PlayerRights;
import com.elvarg.game.model.teleportation.TeleportHandler;

public class DonatorZoneCommand implements Command {

    private static final Location DONATOR_ZONE = new Location(5856, 5920, 0);

    @Override
    public void execute(Player player, String command, String[] parts) {
        if (!TeleportHandler.checkReqs(player, DONATOR_ZONE)) {
            return;
        }
        TeleportHandler.teleport(player, DONATOR_ZONE, player.getSpellbook().getTeleportType(), false);
    }

    @Override
    public boolean canUse(Player player) {
        PlayerRights rights = player.getRights();
        return player.isDonator()
                || rights == PlayerRights.MODERATOR
                || rights == PlayerRights.ADMINISTRATOR
                || rights == PlayerRights.DEVELOPER
                || rights == PlayerRights.OWNER;
    }
}
