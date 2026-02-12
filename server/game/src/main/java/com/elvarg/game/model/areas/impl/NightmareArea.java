package com.elvarg.game.model.areas.impl;

import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Boundary;
import com.elvarg.game.model.Location;

import java.util.Arrays;
import java.util.Optional;

/**
 * Private area used for Nightmare encounters.
 */
public class NightmareArea extends PrivateArea {

    // Temporary stable arena coordinates on a known-good loaded region.
    public static final Boundary BOUNDARY = new Boundary(2262, 2285, 4041, 4074, 0);
    public static final Location PLAYER_START = new Location(2272, 4054, 0);
    public static final Location NIGHTMARE_SPAWN = new Location(2269, 4062, 0);
    public static final Location EXIT_LOCATION = new Location(2272, 4052, 0);

    public NightmareArea() {
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
    }

    @Override
    public boolean canTrade(Player player, Player target) {
        return false;
    }

    @Override
    public boolean isMulti(Mobile character) {
        return true;
    }

    @Override
    public boolean dropItemsOnDeath(Player player, Optional<Player> killer) {
        return true;
    }
}
