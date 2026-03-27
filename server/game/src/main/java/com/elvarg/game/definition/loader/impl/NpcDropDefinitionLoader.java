package com.elvarg.game.definition.loader.impl;

import com.elvarg.game.GameConstants;
import com.elvarg.game.definition.NpcDropDefinition;
import com.elvarg.game.definition.loader.DefinitionLoader;
import com.google.gson.Gson;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NpcDropDefinitionLoader extends DefinitionLoader {

    @Override
    public void load() throws Throwable {
    	NpcDropDefinition.definitions.clear();
        loadDefinitions(Paths.get(file()));
    }

    @Override
    public String file() {
        return GameConstants.DEFINITIONS_DIRECTORY + "npc_drops.json";
    }

    private void loadDefinitions(Path path) throws Throwable {
        if (!Files.exists(path)) {
            return;
        }

        try (FileReader reader = new FileReader(path.toFile())) {
            NpcDropDefinition[] defs = new Gson().fromJson(reader, NpcDropDefinition[].class);
            if (defs == null) {
                return;
            }

            for (NpcDropDefinition def : defs) {
                if (def == null || def.getNpcIds() == null) {
                    continue;
                }
                for (int npcId : def.getNpcIds()) {
                    NpcDropDefinition.definitions.put(npcId, def);
                }
            }
        }
    }
}
