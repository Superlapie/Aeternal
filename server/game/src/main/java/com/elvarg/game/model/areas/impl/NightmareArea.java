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

    // Sisterhood Sanctuary / Nightmare lair region block.
    public static final Boundary BOUNDARY = new Boundary(3784, 3835, 9748, 9800, 1);
    // Entry platform in Sisterhood Sanctuary where the suspended Nightmare is clicked.
    public static final Location SANCTUARY_WAKE_TILE = new Location(3806, 9758, 1);
    // Fight box around the wake platform in the sanctuary instance.
    public static final Boundary FIGHT_BOUNDARY = new Boundary(3798, 3818, 9748, 9768, 1);
    // Keep player close to the wake location during the fade transition.
    public static final Location PLAYER_START = new Location(3808, 9756, 1);
    // Instanced Nightmare now spawns at the exact same tile as the suspended wake NPC.
    public static final Location NIGHTMARE_SPAWN = SANCTUARY_WAKE_TILE.clone();
    public static final Location EXIT_LOCATION = new Location(3808, 9756, 1);
    public static final int FADE_SOFT_INTERFACE_ID = 56951;
    public static final int FADE_MID_INTERFACE_ID = 56952;
    public static final int FADE_FULL_INTERFACE_ID = 56953;

    public NightmareArea() {
        super(Arrays.asList(BOUNDARY));
    }

    public static boolean inFightBox(Location location) {
        return FIGHT_BOUNDARY.inside(location);
    }

    public static Location mapWakeTileToFight(Location wakeTile) {
        if (wakeTile == null) {
            return NIGHTMARE_SPAWN.clone();
        }
        return wakeTile.clone();
    }

    @Override
    public void postLeave(Mobile mobile, boolean logout) {
        if (mobile.isPlayer()) {
            mobile.getAsPlayer().getPacketSender().sendWalkableInterface(-1);
        }
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
