package com.elvarg.game.model.commands.impl;

import com.elvarg.game.World;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.rights.PlayerRights;

import java.util.Optional;

public class SetDropRateCommand implements Command {

    private static final double MIN_MULTIPLIER = 0.1;
    private static final double MAX_MULTIPLIER = 250.0;

    @Override
    public void execute(Player player, String command, String[] parts) {
        if (parts.length < 3) {
            player.getPacketSender().sendMessage("Usage: ::setdroprate <player> <multiplier>");
            return;
        }

        Optional<Player> target = World.getPlayerByName(parts[1]);
        if (target.isEmpty()) {
            player.getPacketSender().sendMessage("That player is not online.");
            return;
        }

        final double multiplier;
        try {
            multiplier = Double.parseDouble(parts[2]);
        } catch (NumberFormatException ex) {
            player.getPacketSender().sendMessage("Multiplier must be a number.");
            return;
        }

        if (multiplier < MIN_MULTIPLIER || multiplier > MAX_MULTIPLIER) {
            player.getPacketSender().sendMessage("Multiplier must be between " + MIN_MULTIPLIER + " and " + MAX_MULTIPLIER + ".");
            return;
        }

        Player targetPlayer = target.get();
        targetPlayer.setNpcDropRateMultiplier(multiplier);

        player.getPacketSender().sendMessage(
                String.format("Set %s's NPC drop-rate multiplier to x%.2f.", targetPlayer.getUsername(), targetPlayer.getNpcDropRateMultiplier())
        );
        targetPlayer.getPacketSender().sendMessage(
                String.format("Your NPC drop-rate multiplier was set to x%.2f.", targetPlayer.getNpcDropRateMultiplier())
        );
    }

    @Override
    public boolean canUse(Player player) {
        return player.getRights() == PlayerRights.DEVELOPER;
    }
}
