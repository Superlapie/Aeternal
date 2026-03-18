package com.elvarg.game.model.commands.impl;

import com.elvarg.game.content.skill.impl.smithing.Smelting;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.rights.PlayerRights;

public class SmeltCommand implements Command {

    @Override
    public void execute(Player player, String command, String[] parts) {
        if (parts.length < 2) {
            player.getPacketSender().sendMessage("Usage: ::smelt [bar]");
            player.getPacketSender().sendMessage("Available bars: bronze, iron, steel, gold, mithril, adamantite, runite");
            return;
        }
        
        String barName = parts[1];
        Smelting.handleSmeltCommand(player, barName);
    }

    @Override
    public boolean canUse(Player player) {
        PlayerRights rights = player.getRights();
        return (rights == PlayerRights.OWNER || rights == PlayerRights.DEVELOPER || rights == PlayerRights.ADMINISTRATOR || rights == PlayerRights.MODERATOR || rights == PlayerRights.NONE);
    }

}
