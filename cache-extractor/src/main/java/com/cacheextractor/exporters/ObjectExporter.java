package com.cacheextractor.exporters;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.openrs.cache.Cache;
import net.openrs.cache.type.TypeList;
import net.openrs.cache.type.objects.ObjectType;

/**
 * Exports object definitions from the OSRS cache to JSON format.
 * Includes all relevant gameplay fields for RSPS development.
 */
public class ObjectExporter extends JSONExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(ObjectExporter.class);
    
    public ObjectExporter(ExtractionConfig config) {
        super(config);
    }
    
    /**
     * Exports all object definitions to JSON
     */
    public boolean export(Cache cache) {
        try {
            logger.info("Starting object export...");
            
            TypeList<ObjectType> objectTypes = cache.getTypeList(ObjectType.class);
            JsonObject root = new JsonObject();
            JsonArray objects = new JsonArray();
            
            int count = 0;
            for (ObjectType objectType : objectTypes) {
                if (objectType != null) {
                    JsonObject objectJson = serializeObject(objectType);
                    objects.add(objectJson);
                    count++;
                }
            }
            
            root.add("objects", objects);
            root.add("metadata", createMetadataJson("objects", count));
            
            Path outputPath = getOutputPath("objects");
            writeJsonToFile(root, outputPath);
            
            logger.info("Exported {} objects to {}", count, outputPath);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to export objects: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Serializes an ObjectType to JSON
     */
    private JsonObject serializeObject(ObjectType obj) {
        JsonObject json = new JsonObject();
        
        // Basic information
        json.addProperty("id", obj.id);
        json.addProperty("name", obj.name != null ? obj.name : "");
        json.addProperty("description", obj.description != null ? obj.description : "");
        
        // Model information
        if (obj.modelIds != null) {
            JsonArray models = new JsonArray();
            for (int modelId : obj.modelIds) {
                models.add(modelId);
            }
            json.add("models", models);
        }
        
        if (obj.modelTypes != null) {
            JsonArray modelTypes = new JsonArray();
            for (int modelType : obj.modelTypes) {
                modelTypes.add(modelType);
            }
            json.add("modelTypes", modelTypes);
        }
        
        // Actions
        if (obj.actions != null) {
            JsonArray actions = new JsonArray();
            for (String action : obj.actions) {
                actions.add(action != null ? action : "");
            }
            json.add("actions", actions);
        }
        
        // Size and positioning
        json.addProperty("sizeX", obj.sizeX);
        json.addProperty("sizeY", obj.sizeY);
        json.addProperty("height", obj.height);
        
        // Flags and properties
        json.addProperty("interactive", obj.interactive);
        json.addProperty("solid", obj.solid);
        json.addProperty("obstructsGround", obj.obstructsGround);
        json.addProperty("clipType", obj.clipType);
        json.addProperty("impenetrable", obj.impenetrable);
        json.addProperty("contouredGround", obj.contouredGround);
        json.addProperty("occludes", obj.occludes);
        json.addProperty("castsShadow", obj.castsShadow);
        json.addProperty("delayShading", obj.delayShading);
        json.addProperty("removeClipping", obj.removeClipping);
        
        // Animation and varbits
        json.addProperty("animationId", obj.animationId);
        json.addProperty("varbitId", obj.varbitId);
        json.addProperty("varpId", obj.varpId);
        
        // Colors and textures
        if (obj.originalModelColors != null) {
            JsonArray colors = new JsonArray();
            for (int color : obj.originalModelColors) {
                colors.add(color);
            }
            json.add("originalModelColors", colors);
        }
        
        if (obj.modifiedModelColors != null) {
            JsonArray colors = new JsonArray();
            for (int color : obj.modifiedModelColors) {
                colors.add(color);
            }
            json.add("modifiedModelColors", colors);
        }
        
        if (obj.originalModelTextures != null) {
            JsonArray textures = new JsonArray();
            for (short texture : obj.originalModelTextures) {
                textures.add(texture);
            }
            json.add("originalModelTextures", textures);
        }
        
        if (obj.modifiedModelTextures != null) {
            JsonArray textures = new JsonArray();
            for (short texture : obj.modifiedModelTextures) {
                textures.add(texture);
            }
            json.add("modifiedModelTextures", textures);
        }
        
        // Scale and translation
        json.addProperty("scaleX", obj.scaleX);
        json.addProperty("scaleY", obj.scaleY);
        json.addProperty("scaleZ", obj.scaleZ);
        json.addProperty("translateX", obj.translateX);
        json.addProperty("translateY", obj.translateY);
        json.addProperty("translateZ", obj.translateZ);
        
        // Lighting
        json.addProperty("ambientLighting", obj.ambientLighting);
        json.addProperty("lightDiffusion", obj.lightDiffusion);
        
        // Map and minimap
        json.addProperty("mapscene", obj.mapscene);
        json.addProperty("minimapFunction", obj.minimapFunction);
        
        // Support items
        json.addProperty("supportItems", obj.supportItems);
        
        // Children IDs
        if (obj.childrenIds != null) {
            JsonArray children = new JsonArray();
            for (int childId : obj.childrenIds) {
                children.add(childId);
            }
            json.add("childrenIds", children);
        }
        
        return json;
    }
}
