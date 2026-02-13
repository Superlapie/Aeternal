package com.elvarg.game.definition.loader.impl;

import com.elvarg.game.GameConstants;
import com.elvarg.game.definition.NpcDefinition;
import com.elvarg.game.definition.loader.DefinitionLoader;
import com.google.gson.Gson;

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
    }

    @Override
    public String file() {
        return GameConstants.DEFINITIONS_DIRECTORY + "npc_defs.json";
    }

}
