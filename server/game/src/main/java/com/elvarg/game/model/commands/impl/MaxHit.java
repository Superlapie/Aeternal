package com.elvarg.game.model.commands.impl;

import com.elvarg.game.World;
import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.content.combat.formula.DamageFormulas;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.util.Misc;

import java.util.Optional;

public class MaxHit implements Command {

    /**
     * This command can be used
     *
     * ::maxhit/mh {?player}
     *
     * This command will show you your max hit, or if you provide a player's name it will try to get their max hit.
     *
     * @param player
     * @param command
     * @param parts
     */
    @Override
    public void execute(Player player, String command, String[] parts) {
        String playerName = parts.length == 2 ? parts[1] : null;
        if (playerName != null) {
            Optional<Player> p2 = World.getPlayerByName(playerName);
            if (p2.isPresent()) {
                Player otherPlayer = p2.get();
                CombatType type = getCombatType(otherPlayer);
                int maxHit = getMaxHit(otherPlayer, type);

                player.getPacketSender().sendMessage(playerName + "'s current " + formatType(type)
                        + " max hit is: " + maxHit);
            } else {
                player.getPacketSender().sendMessage("Cannot find player: " + playerName);
            }

            return;
        }

        CombatType type = getCombatType(player);
        int maxHit = getMaxHit(player, type);

        player.getPacketSender().sendMessage("Your current " + formatType(type) + " max hit is: " + maxHit);
    }

    private static CombatType getCombatType(Player player) {
        CombatMethod method = CombatFactory.getMethod(player);
        return method.type();
    }

    private static int getMaxHit(Player player, CombatType type) {
        switch (type) {
            case RANGED:
                return DamageFormulas.calculateMaxRangedHit(player);
            case MAGIC:
                return DamageFormulas.getMagicMaxhit(player);
            case MELEE:
            default:
                return DamageFormulas.calculateMaxMeleeHit(player);
        }
    }

    private static String formatType(CombatType type) {
        return Misc.capitalize(type.name().toLowerCase());
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }
}
