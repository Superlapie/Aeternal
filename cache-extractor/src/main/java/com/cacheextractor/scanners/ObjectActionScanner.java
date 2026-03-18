package com.cacheextractor.scanners;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cacheextractor.ExtractionConfig;
import com.cacheextractor.exporters.JSONExporter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.openrs.cache.Cache;
import net.openrs.cache.type.TypeList;
import net.openrs.cache.type.objects.ObjectType;

/**
 * Scans object definitions for skill-related actions and exports them to JSON.
 * Groups objects by their actions for automatic skill system registration.
 */
public class ObjectActionScanner extends JSONExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(ObjectActionScanner.class);
    
    // Skill-related action keywords
    private static final Set<String> SKILL_ACTIONS = new HashSet<>();
    
    static {
        // Mining
        SKILL_ACTIONS.add("Mine");
        SKILL_ACTIONS.add("Prospect");
        
        // Woodcutting
        SKILL_ACTIONS.add("Chop down");
        SKILL_ACTIONS.add("Woodcut");
        
        // Fishing
        SKILL_ACTIONS.add("Net");
        SKILL_ACTIONS.add("Bait");
        SKILL_ACTIONS.add("Lure");
        SKILL_ACTIONS.add("Harpoon");
        SKILL_ACTIONS.add("Cage");
        SKILL_ACTIONS.add("Big net");
        
        // Cooking
        SKILL_ACTIONS.add("Cook");
        SKILL_ACTIONS.add("Range");
        
        // Smithing
        SKILL_ACTIONS.add("Smelt");
        SKILL_ACTIONS.add("Smith");
        SKILL_ACTIONS.add("Forge");
        
        // Crafting
        SKILL_ACTIONS.add("Craft");
        SKILL_ACTIONS.add("Spin");
        SKILL_ACTIONS.add("Weave");
        SKILL_ACTIONS.add("Tan");
        
        // Herblore
        SKILL_ACTIONS.add("Clean");
        SKILL_ACTIONS.add("Grind");
        SKILL_ACTIONS.add("Potion");
        
        // Farming
        SKILL_ACTIONS.add("Pick");
        SKILL_ACTIONS.add("Harvest");
        SKILL_ACTIONS.add("Water");
        SKILL_ACTIONS.add("Inspect");
        
        // Construction
        SKILL_ACTIONS.add("Build");
        SKILL_ACTIONS.add("Remove");
        
        // Other common actions
        SKILL_ACTIONS.add("Search");
        SKILL_ACTIONS.add("Open");
        SKILL_ACTIONS.add("Close");
        SKILL_ACTIONS.add("Bank");
        SKILL_ACTIONS.add("Deposit");
        SKILL_ACTIONS.add("Withdraw");
        SKILL_ACTIONS.add("Trade");
        SKILL_ACTIONS.add("Shop");
    }
    
    public ObjectActionScanner(ExtractionConfig config) {
        super(config);
    }
    
    /**
     * Scans objects and exports action mappings
     */
    public boolean scanAndExport(Cache cache) {
        try {
            logger.info("Starting object action scan...");
            
            TypeList<ObjectType> objectTypes = cache.getTypeList(ObjectType.class);
            Map<String, List<Integer>> actionMappings = new HashMap<>();
            Map<String, List<ObjectInfo>> objectDetails = new HashMap<>();
            
            int scannedCount = 0;
            int actionCount = 0;
            
            for (ObjectType objectType : objectTypes) {
                if (objectType != null && objectType.actions != null) {
                    scannedCount++;
                    
                    for (String action : objectType.actions) {
                        if (action != null && !action.trim().isEmpty()) {
                            String normalizedAction = normalizeAction(action.trim());
                            
                            if (SKILL_ACTIONS.contains(normalizedAction)) {
                                actionMappings.computeIfAbsent(normalizedAction, k -> new ArrayList<>())
                                              .add(objectType.id);
                                
                                objectDetails.computeIfAbsent(normalizedAction, k -> new ArrayList<>())
                                           .add(new ObjectInfo(objectType.id, objectType.name, action));
                                
                                actionCount++;
                            }
                        }
                    }
                }
            }
            
            // Create JSON output
            JsonObject root = new JsonObject();
            
            // Action mappings
            JsonObject actions = new JsonObject();
            for (Map.Entry<String, List<Integer>> entry : actionMappings.entrySet()) {
                JsonArray objectIds = new JsonArray();
                for (int id : entry.getValue()) {
                    objectIds.add(id);
                }
                actions.add(entry.getKey(), objectIds);
            }
            root.add("actions", actions);
            
            // Object details
            JsonObject details = new JsonObject();
            for (Map.Entry<String, List<ObjectInfo>> entry : objectDetails.entrySet()) {
                JsonArray objects = new JsonArray();
                for (ObjectInfo info : entry.getValue()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", info.id);
                    obj.addProperty("name", info.name != null ? info.name : "");
                    obj.addProperty("action", info.action);
                    objects.add(obj);
                }
                details.add(entry.getKey(), objects);
            }
            root.add("objectDetails", details);
            
            // Metadata
            JsonObject metadata = new JsonObject();
            metadata.addProperty("type", "object_actions");
            metadata.addProperty("scannedObjects", scannedCount);
            metadata.addProperty("totalActions", actionCount);
            metadata.addProperty("uniqueActions", actionMappings.size());
            metadata.addProperty("extractedAt", System.currentTimeMillis());
            metadata.addProperty("version", "1.0.0");
            root.add("metadata", metadata);
            
            // Write to file
            Path outputPath = getOutputPath("object_actions");
            writeJsonToFile(root, outputPath);
            
            logger.info("Scanned {} objects, found {} action mappings with {} unique actions", 
                       scannedCount, actionCount, actionMappings.size());
            logger.info("Exported object actions to {}", outputPath);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to scan object actions: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Normalizes action strings for consistent grouping
     */
    private String normalizeAction(String action) {
        // Convert to title case for consistency
        if (action.length() == 0) {
            return action;
        }
        
        String normalized = action.toLowerCase();
        
        // Handle common variations
        switch (normalized) {
            case "chop":
            case "woodcut":
                return "Chop down";
                
            case "fish":
                return "Net";
                
            case "cook":
                return "Cook";
                
            case "mine":
                return "Mine";
                
            case "smith":
                return "Smith";
                
            case "craft":
                return "Craft";
                
            default:
                // Return title case
                return action.substring(0, 1).toUpperCase() + action.substring(1);
        }
    }
    
    /**
     * Simple data holder for object information
     */
    private static class ObjectInfo {
        final int id;
        final String name;
        final String action;
        
        ObjectInfo(int id, String name, String action) {
            this.id = id;
            this.name = name;
            this.action = action;
        }
    }
}
