package com.elvarg.game.model.commands.impl;

import com.elvarg.game.definition.NpcDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.rights.PlayerRights;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NpcidCommand implements Command {

    private static final int MAX_RESULTS = 50;

    @Override
    public void execute(Player player, String command, String[] parts) {
        if (parts.length < 2) {
            player.getPacketSender().sendMessage("Usage: ::npcid <npc name>");
            return;
        }

        String query = command.length() > 5 ? command.substring(6).trim() : "";
        if (query.isEmpty()) {
            player.getPacketSender().sendMessage("Usage: ::npcid <npc name>");
            return;
        }

        String normalized = query.toLowerCase(Locale.ROOT);
        List<NpcDefinition> matches = new ArrayList<>();
        for (Map.Entry<Integer, NpcDefinition> entry : NpcDefinition.definitions.entrySet()) {
            NpcDefinition definition = entry.getValue();
            if (definition == null || definition.getName() == null || definition.getName().isEmpty()) {
                continue;
            }

            if (definition.getName().toLowerCase(Locale.ROOT).contains(normalized)) {
                matches.add(definition);
            }
        }

        matches.sort(Comparator.comparingInt(NpcDefinition::getId));

        if (matches.isEmpty()) {
            player.getPacketSender().sendMessage("No npcs found for: " + query);
            return;
        }

        player.getPacketSender().sendMessage("Found " + matches.size() + " npc(s) for: " + query);
        int shown = 0;
        for (NpcDefinition match : matches) {
            player.getPacketSender().sendMessage(match.getName() + " - " + match.getId());
            shown++;
            if (shown >= MAX_RESULTS && matches.size() > MAX_RESULTS) {
                player.getPacketSender().sendMessage("Showing first " + MAX_RESULTS + " results.");
                break;
            }
        }
    }

    @Override
    public boolean canUse(Player player) {
        PlayerRights rights = player.getRights();
        return rights == PlayerRights.OWNER || rights == PlayerRights.DEVELOPER;
    }
}
