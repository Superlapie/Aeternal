package com.elvarg.game.model.commands.impl;

import com.elvarg.game.definition.ObjectDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.rights.PlayerRights;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ObidCommand implements Command {

    private static final int MAX_RESULTS = 50;

    @Override
    public void execute(Player player, String command, String[] parts) {
        if (parts.length < 2) {
            player.getPacketSender().sendMessage("Usage: ::obid <object name>");
            return;
        }

        String query = command.length() > 4 ? command.substring(5).trim() : "";
        if (query.isEmpty()) {
            player.getPacketSender().sendMessage("Usage: ::obid <object name>");
            return;
        }

        String normalized = query.toLowerCase(Locale.ROOT);
        List<ObjectMatch> matches = new ArrayList<>();

        for (int i = 0; i < ObjectDefinition.totalObjects; i++) {
            ObjectDefinition definition = ObjectDefinition.forId(i);
            if (definition == null || definition.getName() == null || definition.getName().isEmpty()) {
                continue;
            }
            String name = definition.getName();
            if (name.equalsIgnoreCase("null")) {
                continue;
            }
            if (name.toLowerCase(Locale.ROOT).contains(normalized)) {
                // Snapshot now because ObjectDefinition.forId() reuses a small rotating cache.
                matches.add(new ObjectMatch(i, name));
            }
        }

        matches.sort(Comparator.comparingInt(match -> match.id));

        if (matches.isEmpty()) {
            player.getPacketSender().sendMessage("No objects found for: " + query);
            return;
        }

        player.getPacketSender().sendMessage("Found " + matches.size() + " object(s) for: " + query);
        int shown = 0;
        for (ObjectMatch match : matches) {
            player.getPacketSender().sendMessage(match.name + " - " + match.id);
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

    private static final class ObjectMatch {
        private final int id;
        private final String name;

        private ObjectMatch(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
