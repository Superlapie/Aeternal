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
        ensureThrallDefinitions();
        ensureTormentedDemonDefinitions();
        ensureDemonicGorillaDefinitions();
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

    private void ensureThrallDefinitions() {
        ensureThrallDefinition(10884, "Ghostly thrall", 5540, 5541, 5542);
        ensureThrallDefinition(10885, "Skeletal thrall", 5485, 5482, 5491);
        ensureThrallDefinition(10886, "Zombie thrall", 5567, 5568, 5569);
    }

    private void ensureTormentedDemonDefinitions() {
        ensureTormentedDemonDefinition(13593);
        ensureTormentedDemonDefinition(13594);
        ensureTormentedDemonDefinition(13595);
        ensureTormentedDemonDefinition(13596);
        ensureTormentedDemonDefinition(13597);
    }

    private void ensureDemonicGorillaDefinitions() {
        int[] ids = {7144, 7145, 7146, 7147, 7148, 7149, 7152};
        for (int id : ids) {
            NpcDefinition def = NpcDefinition.definitions.get(id);
            if (def == null) {
                continue;
            }

            try {
                set(def, "defenceAnim", 7224);
                set(def, "deathAnim", 7229);
            } catch (Exception e) {
                System.err.println("Failed to correct demonic gorilla NPC definition (" + id + "): " + e.getMessage());
            }
        }
    }

    private void ensureTormentedDemonDefinition(int id) {
        if (NpcDefinition.definitions.containsKey(id)) {
            return;
        }
        try {
            NpcDefinition def = new NpcDefinition();
            set(def, "id", id);
            set(def, "name", "Tormented Demon");
            set(def, "examine", "A demon wracked with infernal torment.");
            set(def, "size", 2);
            set(def, "walkRadius", 6);
            set(def, "attackable", true);
            set(def, "retreats", true);
            set(def, "aggressive", true);
            set(def, "aggressiveTolerance", false);
            set(def, "poisonous", false);
            set(def, "fightsBack", true);
            set(def, "respawn", 35);
            set(def, "maxHit", 40);
            set(def, "hitpoints", 600);
            set(def, "attackSpeed", 5);
            set(def, "attackAnim", 11387);
            set(def, "defenceAnim", 11388);
            set(def, "deathAnim", 11366);
            set(def, "combatLevel", 450);
            set(def, "stats", new int[]{290, 290, 285, 600, 285, 0, 0, 0, 0, 0, 250, 250, 250, 250, 250, 0, 0, 0});
            set(def, "slayerLevel", 0);
            set(def, "combatFollowDistance", 16);
            NpcDefinition.definitions.put(id, def);
            System.out.println("Installed fallback NPC definition for Tormented Demon (" + id + ").");
        } catch (Exception e) {
            System.err.println("Failed to install fallback Tormented Demon NPC definition (" + id + "): " + e.getMessage());
        }
    }

    private void ensureThrallDefinition(int id, String name, int attackAnim, int defenceAnim, int deathAnim) {
        if (NpcDefinition.definitions.containsKey(id)) {
            return;
        }
        try {
            NpcDefinition def = new NpcDefinition();
            set(def, "id", id);
            set(def, "name", name);
            set(def, "examine", "A summoned undead servant.");
            set(def, "size", 1);
            set(def, "walkRadius", 0);
            set(def, "attackable", false);
            set(def, "retreats", true);
            set(def, "aggressive", false);
            set(def, "aggressiveTolerance", false);
            set(def, "poisonous", false);
            set(def, "fightsBack", true);
            set(def, "respawn", 0);
            set(def, "maxHit", 1);
            set(def, "hitpoints", 18);
            set(def, "attackSpeed", 4);
            set(def, "attackAnim", attackAnim);
            set(def, "defenceAnim", defenceAnim);
            set(def, "deathAnim", deathAnim);
            set(def, "combatLevel", 0);
            set(def, "stats", new int[]{1, 1, 1, 18, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1});
            set(def, "slayerLevel", 0);
            set(def, "combatFollowDistance", 7);
            NpcDefinition.definitions.put(id, def);
            System.out.println("Installed fallback NPC definition for thrall (" + id + ").");
        } catch (Exception e) {
            System.err.println("Failed to install fallback thrall NPC definition (" + id + "): " + e.getMessage());
        }
    }
}
