package com.elvarg.game.definition.loader.impl;

import java.io.FileReader;

import com.elvarg.game.GameConstants;
import com.elvarg.game.content.npc.NpcSpawnService;
import com.elvarg.game.definition.NpcDefinition;
import com.elvarg.game.definition.NpcSpawnDefinition;
import com.elvarg.game.definition.loader.DefinitionLoader;
import com.google.gson.Gson;

public class NpcSpawnDefinitionLoader extends DefinitionLoader {

    @Override
    public void load() throws Throwable {
        FileReader reader = new FileReader(file());
        NpcSpawnDefinition[] defs = new Gson().fromJson(reader, NpcSpawnDefinition[].class);
        if (defs != null) {
            for (NpcSpawnDefinition def : defs) {
                int radius = def.getRadius();
                if (radius <= 0) {
                    NpcDefinition definition = NpcDefinition.forId(def.getId());
                    if (definition.getWalkRadius() > 0) {
                        radius = definition.getWalkRadius();
                    } else if (definition.isAttackable()) {
                        radius = 2;
                    }
                }
                // Normalize imported spawn radius before region activation.
                def.setRadius(Math.max(0, Math.min(radius, 5)));
            }
        }
        NpcSpawnService.initialize(defs);
        reader.close();
    }

    @Override
    public String file() {
        return GameConstants.DEFINITIONS_DIRECTORY + "npc_spawns.json";
    }

}
