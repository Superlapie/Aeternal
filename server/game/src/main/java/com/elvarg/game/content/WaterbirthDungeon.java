package com.elvarg.game.content;

import com.elvarg.game.World;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.Boundary;
import com.elvarg.game.entity.impl.player.Player;

public final class WaterbirthDungeon {

    public static final Location KINGS_LADDER_ENTRANCE_LOCATION = new Location(1912, 4367, 0);
    public static final Location KINGS_LAIR_LOCATION = new Location(2900, 4385, 0);
    public static final Boundary KINGS_LAIR_BOUNDARY = new Boundary(2888, 2931, 4378, 4408, 0);

    private WaterbirthDungeon() {
    }

    public static void initialize() {
        // The entrance ladder is part of the map data in the live cache.
    }

    public static int countPlayersInKingsLair() {
        int count = 0;
        for (Player player : World.getPlayers()) {
            if (player != null && KINGS_LAIR_BOUNDARY.inside(player.getLocation())) {
                count++;
            }
        }
        return count;
    }
}
