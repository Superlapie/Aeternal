package com.elvarg.game.model.commands.impl;

import com.elvarg.game.GameConstants;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.teleportation.TeleportHandler;

public class BossTeleportCommand implements Command {

    private static final Location HOME = GameConstants.DEFAULT_LOCATION;
    private static final Location ZULRAH = new Location(2202, 3056, 0);
    private static final Location DAGANNOTH_KINGS = new Location(1912, 4367, 0);
    private static final Location VORKATH = new Location(2272, 4052, 0);
    private static final Location BANDOS = new Location(2864, 5354, 2);
    private static final Location SARADOMIN = new Location(2908, 5265, 0);
    private static final Location ZAMORAK = new Location(2925, 5334, 2);
    private static final Location ARMADYL = new Location(2839, 5296, 2);

    @Override
    public void execute(Player player, String command, String[] parts) {
        Location destination = switch (command.toLowerCase()) {
            case "home" -> HOME;
            case "zulrah" -> ZULRAH;
            case "dks" -> DAGANNOTH_KINGS;
            case "vork", "vorkath" -> VORKATH;
            case "bandos" -> BANDOS;
            case "sara" -> SARADOMIN;
            case "zammy", "zamorak" -> ZAMORAK;
            case "arma", "armadyl" -> ARMADYL;
            default -> null;
        };

        if (destination == null) {
            return;
        }
        if (!TeleportHandler.checkReqs(player, destination)) {
            return;
        }
        TeleportHandler.teleport(player, destination, player.getSpellbook().getTeleportType(), false);
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }
}
