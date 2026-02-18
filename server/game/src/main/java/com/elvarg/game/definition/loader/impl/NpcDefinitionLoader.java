package com.elvarg.game.definition.loader.impl;

import com.elvarg.game.GameConstants;
import com.elvarg.game.definition.NpcDefinition;
import com.elvarg.game.definition.loader.DefinitionLoader;
import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NpcDefinitionLoader extends DefinitionLoader {

    @Override
    public void load() throws Throwable {
        NpcDefinition.definitions.clear();
        Path path = Paths.get(file()).toAbsolutePath().normalize();
        String json = Files.readString(path, StandardCharsets.UTF_8);
        if (!json.isEmpty() && json.charAt(0) == '\uFEFF') {
            json = json.substring(1);
        }
        NpcDefinition[] defs = new Gson().fromJson(json, NpcDefinition[].class);
        for (NpcDefinition def : defs) {
            NpcDefinition.definitions.put(def.getId(), def);
        }
        ensureAraxxorDefinition();
    }

    @Override
    public String file() {
        return GameConstants.DEFINITIONS_DIRECTORY + "npc_defs.json";
    }

    private void ensureAraxxorDefinition() {
        final int araxxorId = 13668;
        if (NpcDefinition.definitions.containsKey(araxxorId)) {
            return;
        }

        try {
            NpcDefinition def = new NpcDefinition();
            set(def, "id", araxxorId);
            set(def, "name", "Araxxor");
            set(def, "examine", "A towering araxyte colossus.");
            set(def, "size", 7);
            set(def, "walkRadius", 6);
            set(def, "attackable", true);
            set(def, "retreats", false);
            set(def, "aggressive", true);
            set(def, "aggressiveTolerance", false);
            set(def, "poisonous", true);
            set(def, "fightsBack", true);
            set(def, "respawn", 30);
            set(def, "maxHit", 45);
            set(def, "hitpoints", 1300);
            set(def, "attackSpeed", 5);
            set(def, "attackAnim", 11480);
            // Use idle as defence fallback; 11485 is a death-loop pose and causes visual oddities when hit.
            set(def, "defenceAnim", 11473);
            // 11481 is Araxxor death; 11482 is spawn.
            set(def, "deathAnim", 11481);
            set(def, "combatLevel", 890);
            set(def, "stats", new int[]{320, 320, 320, 1020, 210, 190, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
            set(def, "slayerLevel", 0);
            set(def, "combatFollowDistance", 16);
            NpcDefinition.definitions.put(araxxorId, def);
            System.out.println("Installed fallback NPC definition for Araxxor (13668).");
        } catch (Exception e) {
            System.err.println("Failed to install fallback Araxxor NPC definition: " + e.getMessage());
        }
    }

    private static void set(NpcDefinition def, String fieldName, Object value) throws Exception {
        Field f = NpcDefinition.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(def, value);
    }

}
