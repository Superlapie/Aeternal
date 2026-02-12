package com.elvarg.game.model.areas.impl;

import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Boundary;
import com.elvarg.game.model.Location;

import java.util.Arrays;

/**
 * Private area used for Zulrah encounters.
 */
public class ZulrahArea extends PrivateArea {

    public static final Boundary BOUNDARY = new Boundary(2255, 2280, 3060, 3085, 0);
    public static final Location PLAYER_START = new Location(2268, 3069, 0);
    public static final Location ZULRAH_SPAWN = new Location(2267, 3072, 0);
    public static final Location EXIT_OBJECT_LOCATION = new Location(2269, 3069, 0);
    public static final Location EXIT_LOCATION = new Location(2202, 3056, 0);

    public ZulrahArea() {
        super(Arrays.asList(BOUNDARY));
    }

    @Override
    public void postLeave(Mobile mobile, boolean logout) {
        if (logout && mobile.isPlayer()) {
            mobile.moveTo(EXIT_LOCATION.clone());
        }
        super.postLeave(mobile, logout);
    }

    @Override
    public void process(Mobile mobile) {
        // No per-tick area processing required.
    }

    @Override
    public boolean canTrade(Player player, Player target) {
        return false;
    }
}
