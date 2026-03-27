package com.elvarg.game.model.commands;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.rights.PlayerRights;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CommandInterface {

    public static final int INTERFACE_ID = 60100;
    private static final int TITLE_ID = 60101;
    private static final int SUBTITLE_ID = 60102;
    private static final int COUNT_ID = 60103;
    private static final int GROUP_ID = 60104;
    private static final int GROUP_BUTTON_START = 60105;
    private static final int ROW_START_ID = 60120;
    private static final int MAX_ROWS = 128;

    private CommandInterface() {
    }

    public enum CommandGroup {
        ALL("All", "Everything you can currently use."),
        GENERAL("General", "Core player commands and account utilities."),
        TRAVEL("Travel", "Teleport and movement commands."),
        DONATOR("Donator", "Commands for donators and yell access."),
        STAFF("Staff", "Moderation and support tools."),
        OWNER("Owner", "Owner-level management commands."),
        DEVELOPER("Dev", "Developer and testing tools.");

        private final String label;
        private final String description;

        CommandGroup(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    private static final class CommandEntry {
        private final String primaryAlias;
        private final CommandGroup group;

        private CommandEntry(String primaryAlias, CommandGroup group) {
            this.primaryAlias = primaryAlias;
            this.group = group;
        }

        private String displayText() {
            return "@or1@::" + primaryAlias;
        }
    }

    public static void open(Player player) {
        open(player, CommandGroup.ALL);
    }

    public static void open(Player player, CommandGroup group) {
        player.setCommandsInterfaceGroup(group);
        List<CommandEntry> entries = getEntries(player, group);

        player.getPacketSender().sendInterfaceRemoval();
        player.getPacketSender().sendInterface(INTERFACE_ID);
        player.getPacketSender().sendString(TITLE_ID, "@or1@Command Guide");
        player.getPacketSender().sendString(SUBTITLE_ID, "Simple access to every command you can use.");
        player.getPacketSender().sendString(COUNT_ID, "@yel@" + entries.size() + " commands available");
        player.getPacketSender().sendString(GROUP_ID, "@whi@Selected group: @or1@" + group.getLabel() + "@whi@ - " + group.getDescription());

        int buttonId = GROUP_BUTTON_START;
        for (CommandGroup commandGroup : CommandGroup.values()) {
            String label = commandGroup == group
                    ? "@or1@" + commandGroup.getLabel()
                    : "@whi@" + commandGroup.getLabel();
            player.getPacketSender().sendString(buttonId++, label);
        }

        for (int i = 0; i < MAX_ROWS; i++) {
            int rowId = ROW_START_ID + i;
            if (i < entries.size()) {
                player.getPacketSender().sendString(rowId, entries.get(i).displayText());
            } else {
                player.getPacketSender().sendString(rowId, "");
            }
        }
    }

    public static boolean handleButton(Player player, int button) {
        if (button >= GROUP_BUTTON_START && button < GROUP_BUTTON_START + CommandGroup.values().length) {
            int index = button - GROUP_BUTTON_START;
            CommandGroup[] values = CommandGroup.values();
            if (index >= 0 && index < values.length) {
                open(player, values[index]);
                return true;
            }
        }

        if (button >= ROW_START_ID && button < ROW_START_ID + MAX_ROWS) {
            int index = button - ROW_START_ID;
            List<CommandEntry> entries = getEntries(player, player.getCommandsInterfaceGroup());
            if (index >= 0 && index < entries.size()) {
                CommandEntry entry = entries.get(index);
                player.getPacketSender().sendMessage("@or1@::" + entry.primaryAlias);
            }
            return true;
        }

        return false;
    }

    private static List<CommandEntry> getEntries(Player player, CommandGroup group) {
        List<CommandEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Command> entry : CommandManager.commands.entrySet()) {
            String alias = entry.getKey().toLowerCase();
            if ("commands".equals(alias)) {
                continue;
            }

            Command command = entry.getValue();
            if (command == null || !command.canUse(player)) {
                continue;
            }

            CommandGroup commandGroup = resolveGroup(alias, player);
            if (group == CommandGroup.ALL || commandGroup == group) {
                entries.add(new CommandEntry(alias, commandGroup));
            }
        }

        entries.sort(Comparator.comparing(entry -> entry.primaryAlias));
        return entries;
    }

    private static CommandGroup resolveGroup(String alias, Player player) {
        String key = alias.toLowerCase();
        switch (key) {
            case "changepassword":
            case "claim":
            case "creationdate":
            case "kdr":
            case "players":
            case "thread":
            case "timeplayed":
            case "ground":
            case "store":
            case "donate":
            case "maxhit":
            case "mh":
            case "droprate":
            case "smelt":
            case "ge":
            case "lockxp":
            case "skull":
            case "redskull":
                return CommandGroup.GENERAL;
            case "home":
            case "zulrah":
            case "dks":
            case "vork":
            case "vorkath":
            case "bandos":
            case "sara":
            case "zammy":
            case "zamorak":
            case "arma":
            case "armadyl":
            case "dg":
            case "demonic":
                return CommandGroup.TRAVEL;
            case "yell":
            case "dzone":
            case "donatorzone":
                return CommandGroup.DONATOR;
            case "mute":
            case "unmute":
            case "ipmute":
            case "ban":
            case "ipban":
            case "unban":
            case "unipmute":
            case "teleto":
            case "exit":
            case "kick":
            case "music":
            case "reloaditemdefs":
            case "reloadnpcdefs":
            case "reloadnpcspawns":
            case "reloaddrops":
            case "reloadshops":
            case "reloadpunishments":
            case "reloadcommands":
            case "teletome":
            case "tele":
            case "item":
            case "pickup":
            case "empty":
            case "unlockprayers":
            case "saveall":
                return CommandGroup.STAFF;
            case "copybank":
            case "bank":
            case "title":
            case "runes":
            case "barrage":
            case "donator":
            case "givedonator":
            case "save":
                return CommandGroup.OWNER;
            case "dialogue":
            case "flood":
            case "master":
            case "reset":
            case "pnpc":
            case "npc":
            case "n":
            case "object":
            case "coords":
            case "config":
            case "spec":
            case "gfx":
            case "sound":
            case "anim":
            case "interface":
            case "chatboxinterface":
            case "update":
            case "area":
            case "infhp":
            case "onehit":
            case "oneshot":
            case "taskdebug":
            case "noclip":
            case "up":
            case "down":
            case "cwar":
            case "listsizes":
            case "atkrange":
            case "attackrange":
            case "id":
            case "npcid":
            case "obid":
            case "oid":
            case "objectid":
            case "testmoon":
            case "debugitems":
            case "t":
                return CommandGroup.DEVELOPER;
            default:
                if (player.getRights() == PlayerRights.DEVELOPER) {
                    return CommandGroup.DEVELOPER;
                }
                if (player.getRights() == PlayerRights.OWNER) {
                    return CommandGroup.OWNER;
                }
                return CommandGroup.STAFF;
        }
    }
}
