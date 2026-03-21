package com.elvarg.game.content;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.Skill;
import com.elvarg.util.Misc;
import com.elvarg.util.timers.TimerKey;

public class ImbuedHeart {

    private static final int COOLDOWN_SECONDS = 7 * 60;
    private static final Graphic INVIGORATE_GFX = new Graphic(1316);

    private ImbuedHeart() {
    }

    public static boolean invigorate(Player player) {
        if (player.getArea() != null && !player.getArea().canDrink(player, com.elvarg.util.ItemIdentifiers.IMBUED_HEART)) {
            player.getPacketSender().sendMessage("You cannot use this here.");
            return true;
        }

        if (player.getTimers().has(TimerKey.STUN)) {
            player.getPacketSender().sendMessage("You're currently stunned and cannot use this.");
            return true;
        }

        if (player.getTimers().has(TimerKey.IMBUED_HEART)) {
            player.getPacketSender().sendMessage("The heart is still drained of its power.");
            player.getPacketSender().sendMessage("It will be ready in " + Misc.ticksToTime(player.getTimers().left(TimerKey.IMBUED_HEART)) + ".");
            return true;
        }

        int currentMagic = player.getSkillManager().getCurrentLevel(Skill.MAGIC);
        int baseMagic = player.getSkillManager().getMaxLevel(Skill.MAGIC);
        if (currentMagic > baseMagic) {
            player.getPacketSender().sendMessage("You cannot use this while your Magic level is already boosted.");
            return true;
        }

        int boost = 1 + (baseMagic / 10);
        int boostedMagic = Math.min(currentMagic + boost, baseMagic + boost);

        player.performGraphic(INVIGORATE_GFX);
        player.getSkillManager().setCurrentLevel(Skill.MAGIC, boostedMagic);
        player.getTimers().register(TimerKey.IMBUED_HEART, Misc.getTicks(COOLDOWN_SECONDS));
        return true;
    }
}
