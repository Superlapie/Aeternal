package com.elvarg.game.model.commands.impl;

import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.rights.PlayerRights;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IdCommand implements Command {

    private static final int MAX_RESULTS = 50;

    @Override
    public void execute(Player player, String command, String[] parts) {
        if (parts.length < 2) {
            player.getPacketSender().sendMessage("Usage: ::id <item name>");
            return;
        }

        String query = command.length() > 2 ? command.substring(3).trim() : "";
        if (query.isEmpty()) {
            player.getPacketSender().sendMessage("Usage: ::id <item name>");
            return;
        }

        String normalized = query.toLowerCase(Locale.ROOT);
        List<ItemDefinition> matches = new ArrayList<>();
        for (Map.Entry<Integer, ItemDefinition> entry : ItemDefinition.definitions.entrySet()) {
            ItemDefinition definition = entry.getValue();
            if (definition == null || definition.getName() == null || definition.getName().isEmpty()) {
                continue;
            }

            if (definition.getName().toLowerCase(Locale.ROOT).contains(normalized)) {
                matches.add(definition);
            }
        }

        matches.sort(Comparator.comparingInt(ItemDefinition::getId));

        if (matches.isEmpty()) {
            player.getPacketSender().sendMessage("No items found for: " + query);
            return;
        }

        player.getPacketSender().sendMessage("Found " + matches.size() + " item(s) for: " + query);
        int shown = 0;
        for (ItemDefinition match : matches) {
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

