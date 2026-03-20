package com.elvarg.game.model.commands.impl;

import com.elvarg.game.definition.ObjectDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.rights.PlayerRights;
import com.elvarg.util.ObjectIdentifiers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ObidCommand implements Command {

    private static final int MAX_RESULTS = 50;
    private static final Map<Integer, String> OBJECT_CONSTANT_NAMES = loadObjectConstantNames();

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
        Integer numericQuery = parseNumericQuery(normalized);

        for (int i = 0; i < ObjectDefinition.totalObjects; i++) {
            ObjectDefinition definition = ObjectDefinition.forId(i);
            String defName = extractDefinitionName(definition);
            String constantName = OBJECT_CONSTANT_NAMES.get(i);
            String normalizedConstant = normalizeConstantName(constantName);

            boolean idMatch = numericQuery != null && i == numericQuery;
            boolean defMatch = defName != null && defName.toLowerCase(Locale.ROOT).contains(normalized);
            boolean constMatch = normalizedConstant != null && normalizedConstant.contains(normalized);

            if (idMatch || defMatch || constMatch) {
                String displayName = defName != null ? defName : prettifyConstantName(constantName);
                if (displayName == null) {
                    displayName = "Object";
                }
                matches.add(new ObjectMatch(i, displayName));
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

    private static String extractDefinitionName(ObjectDefinition definition) {
        if (definition == null) {
            return null;
        }
        String name = definition.getName();
        if (name == null || name.isEmpty() || name.equalsIgnoreCase("null")) {
            return null;
        }
        return name;
    }

    private static Integer parseNumericQuery(String query) {
        try {
            return Integer.parseInt(query);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String normalizeConstantName(String constantName) {
        if (constantName == null || constantName.isEmpty()) {
            return null;
        }
        return constantName.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static String prettifyConstantName(String constantName) {
        if (constantName == null || constantName.isEmpty()) {
            return null;
        }
        String cleaned = constantName.replace('_', ' ').toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(cleaned.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                out.append(c);
            } else if (capitalizeNext) {
                out.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static Map<Integer, String> loadObjectConstantNames() {
        Map<Integer, String> map = new HashMap<>();
        for (Field field : ObjectIdentifiers.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || field.getType() != int.class) {
                continue;
            }
            try {
                int id = field.getInt(null);
                map.putIfAbsent(id, field.getName());
            } catch (IllegalAccessException ignored) {
            }
        }
        return map;
    }
}
