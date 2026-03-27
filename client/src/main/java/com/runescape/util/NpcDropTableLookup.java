package com.runescape.util;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public final class NpcDropTableLookup {

    private static final Set<Integer> DROP_TABLE_NPC_IDS = new HashSet<>();
    private static boolean loaded = false;

    private NpcDropTableLookup() {
    }

    public static boolean hasDropTable(int npcId) {
        ensureLoaded();
        return DROP_TABLE_NPC_IDS.contains(npcId);
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;

        Path[] candidates = new Path[] {
                Paths.get("server", "data", "definitions", "npc_drops.json"),
                Paths.get("..", "server", "data", "definitions", "npc_drops.json"),
                Paths.get("data", "definitions", "npc_drops.json")
        };

        for (Path path : candidates) {
            if (Files.exists(path)) {
                loadFromFile(path);
                return;
            }
        }

        try (InputStream in = NpcDropTableLookup.class.getResourceAsStream("/npc_drops.json")) {
            if (in != null) {
                loadFromStream(in);
                return;
            }
        } catch (IOException ignored) {
        }

        System.out.println("NpcDropTableLookup: unable to locate npc_drops.json, drop-table menu will be hidden.");
    }

    private static void loadFromFile(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            loadFromReader(reader);
            System.out.println("NpcDropTableLookup: loaded " + DROP_TABLE_NPC_IDS.size() + " NPC ids from " + path);
        } catch (IOException e) {
            System.out.println("NpcDropTableLookup: failed to read " + path + ", drop-table menu will be hidden.");
        }
    }

    private static void loadFromStream(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            loadFromReader(reader);
            System.out.println("NpcDropTableLookup: loaded " + DROP_TABLE_NPC_IDS.size() + " NPC ids from classpath resource.");
        }
    }

    private static void loadFromReader(BufferedReader reader) {
        RawDropDefinition[] definitions = new Gson().fromJson(reader, RawDropDefinition[].class);
        if (definitions == null) {
            return;
        }

        for (RawDropDefinition definition : definitions) {
            if (definition == null || definition.npcIds == null) {
                continue;
            }
            for (int npcId : definition.npcIds) {
                DROP_TABLE_NPC_IDS.add(npcId);
            }
        }
    }

    private static final class RawDropDefinition {
        private int[] npcIds;
    }
}
