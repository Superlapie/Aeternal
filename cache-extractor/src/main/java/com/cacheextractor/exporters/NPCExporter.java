package com.cacheextractor.exporters;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.openrs.cache.Cache;
import net.openrs.cache.type.TypeList;
import net.openrs.cache.type.npcs.NPCType;

/**
 * Exports NPC definitions from the OSRS cache to JSON format.
 * Includes all relevant gameplay fields for RSPS development.
 */
public class NPCExporter extends JSONExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(NPCExporter.class);
    
    public NPCExporter(ExtractionConfig config) {
        super(config);
    }
    
    /**
     * Exports all NPC definitions to JSON
     */
    public boolean export(Cache cache) {
        try {
            logger.info("Starting NPC export...");
            
            TypeList<NPCType> npcTypes = cache.getTypeList(NPCType.class);
            JsonObject root = new JsonObject();
            JsonArray npcs = new JsonArray();
            
            int count = 0;
            for (NPCType npcType : npcTypes) {
                if (npcType != null) {
                    JsonObject npcJson = serializeNPC(npcType);
                    npcs.add(npcJson);
                    count++;
                }
            }
            
            root.add("npcs", npcs);
            root.add("metadata", createMetadataJson("npcs", count));
            
            Path outputPath = getOutputPath("npcs");
            writeJsonToFile(root, outputPath);
            
            logger.info("Exported {} NPCs to {}", count, outputPath);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to export NPCs: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Serializes an NPCType to JSON
     */
    private JsonObject serializeNPC(NPCType npc) {
        JsonObject json = new JsonObject();
        
        // Basic information
        json.addProperty("id", npc.id);
        json.addProperty("name", npc.name != null ? npc.name : "");
        json.addProperty("description", npc.description != null ? npc.description : "");
        
        // Model information
        if (npc.modelIds != null) {
            JsonArray models = new JsonArray();
            for (int modelId : npc.modelIds) {
                models.add(modelId);
            }
            json.add("models", models);
        }
        
        // Size and positioning
        json.addProperty("sizeX", npc.sizeX);
        json.addProperty("sizeY", npc.sizeY);
        json.addProperty("height", npc.height);
        
        // Render animations
        if (npc.renderEmote != -1) {
            json.addProperty("renderEmote", npc.renderEmote);
        }
        if (npc.renderEmote2 != -1) {
            json.addProperty("renderEmote2", npc.renderEmote2);
        }
        
        // Stand animations
        if (npc.standAnimation != -1) {
            json.addProperty("standAnimation", npc.standAnimation);
        }
        if (npc.walkAnimation != -1) {
            json.addProperty("walkAnimation", npc.walkAnimation);
        }
        if (npc.walkBackAnimation != -1) {
            json.addProperty("walkBackAnimation", npc.walkBackAnimation);
        }
        if (npc.walkLeftAnimation != -1) {
            json.addProperty("walkLeftAnimation", npc.walkLeftAnimation);
        }
        if (npc.walkRightAnimation != -1) {
            json.addProperty("walkRightAnimation", npc.walkRightAnimation);
        }
        
        // Combat animations
        if (npc.attackAnimation != -1) {
            json.addProperty("attackAnimation", npc.attackAnimation);
        }
        if (npc.blockAnimation != -1) {
            json.addProperty("blockAnimation", npc.blockAnimation);
        }
        if (npc.deathAnimation != -1) {
            json.addProperty("deathAnimation", npc.deathAnimation);
        }
        
        // Combat stats
        if (npc.combatLevel != -1) {
            json.addProperty("combatLevel", npc.combatLevel);
        }
        if (npc.hitpoints != -1) {
            json.addProperty("hitpoints", npc.hitpoints);
        }
        if (npc.maxHit != -1) {
            json.addProperty("maxHit", npc.maxHit);
        }
        if (npc.attackSpeed != -1) {
            json.addProperty("attackSpeed", npc.attackSpeed);
        }
        if (npc.attackBonus != -1) {
            json.addProperty("attackBonus", npc.attackBonus);
        }
        if (npc.strengthBonus != -1) {
            json.addProperty("strengthBonus", npc.strengthBonus);
        }
        if (npc.defenceBonus != -1) {
            json.addProperty("defenceBonus", npc.defenceBonus);
        }
        if (npc.rangedBonus != -1) {
            json.addProperty("rangedBonus", npc.rangedBonus);
        }
        if (npc.magicBonus != -1) {
            json.addProperty("magicBonus", npc.magicBonus);
        }
        
        // Attack styles
        if (npc.attackStyles != null) {
            JsonArray styles = new JsonArray();
            for (int style : npc.attackStyles) {
                styles.add(style);
            }
            json.add("attackStyles", styles);
        }
        
        // Colors and textures
        if (npc.originalModelColors != null) {
            JsonArray colors = new JsonArray();
            for (int color : npc.originalModelColors) {
                colors.add(color);
            }
            json.add("originalModelColors", colors);
        }
        
        if (npc.modifiedModelColors != null) {
            JsonArray colors = new JsonArray();
            for (int color : npc.modifiedModelColors) {
                colors.add(color);
            }
            json.add("modifiedModelColors", colors);
        }
        
        if (npc.originalModelTextures != null) {
            JsonArray textures = new JsonArray();
            for (short texture : npc.originalModelTextures) {
                textures.add(texture);
            }
            json.add("originalModelTextures", textures);
        }
        
        if (npc.modifiedModelTextures != null) {
            JsonArray textures = new JsonArray();
            for (short texture : npc.modifiedModelTextures) {
                textures.add(texture);
            }
            json.add("modifiedModelTextures", textures);
        }
        
        // Chat head models
        if (npc.chatHeadModels != null) {
            JsonArray chatHeads = new JsonArray();
            for (int modelId : npc.chatHeadModels) {
                chatHeads.add(modelId);
            }
            json.add("chatHeadModels", chatHeads);
        }
        
        // Actions
        if (npc.actions != null) {
            JsonArray actions = new JsonArray();
            for (String action : npc.actions) {
                actions.add(action != null ? action : "");
            }
            json.add("actions", actions);
        }
        
        // Equipment
        if (npc.equipment != null) {
            JsonArray equipment = new JsonArray();
            for (int itemId : npc.equipment) {
                equipment.add(itemId);
            }
            json.add("equipment", equipment);
        }
        
        // Skill requirements
        if (npc.skillRequirements != null) {
            JsonObject requirements = new JsonObject();
            for (int i = 0; i < npc.skillRequirements.length; i++) {
                if (npc.skillRequirements[i] > 0) {
                    String skillName = getSkillName(i);
                    if (skillName != null) {
                        requirements.addProperty(skillName, npc.skillRequirements[i]);
                    }
                }
            }
            json.add("skillRequirements", requirements);
        }
        
        // Parameters
        if (npc.parameters != null) {
            JsonArray parameters = new JsonArray();
            for (int param : npc.parameters) {
                parameters.add(param);
            }
            json.add("parameters", parameters);
        }
        
        // Click area
        if (npc.clickArea != null) {
            JsonArray clickArea = new JsonArray();
            for (int coord : npc.clickArea) {
                clickArea.add(coord);
            }
            json.add("clickArea", clickArea);
        }
        
        // Flags
        json.addProperty("visible", npc.visible);
        json.addProperty("clickable", npc.clickable);
        json.addProperty("walkable", npc.walkable);
        json.addProperty("hasRenderPriority", npc.hasRenderPriority);
        
        return json;
    }
    
    /**
     * Gets the name of a skill by index
     */
    private String getSkillName(int index) {
        switch (index) {
            case 0: return "attack";
            case 1: return "defence";
            case 2: return "strength";
            case 3: return "hitpoints";
            case 4: return "ranged";
            case 5: return "prayer";
            case 6: return "magic";
            default: return null;
        }
    }
}
