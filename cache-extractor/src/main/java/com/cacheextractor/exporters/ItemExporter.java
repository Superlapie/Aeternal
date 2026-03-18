package com.cacheextractor.exporters;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.openrs.cache.Cache;
import net.openrs.cache.type.TypeList;
import net.openrs.cache.type.items.ItemType;

/**
 * Exports item definitions from the OSRS cache to JSON format.
 * Includes all relevant gameplay fields for RSPS development.
 */
public class ItemExporter extends JSONExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(ItemExporter.class);
    
    public ItemExporter(ExtractionConfig config) {
        super(config);
    }
    
    /**
     * Exports all item definitions to JSON
     */
    public boolean export(Cache cache) {
        try {
            logger.info("Starting item export...");
            
            TypeList<ItemType> itemTypes = cache.getTypeList(ItemType.class);
            JsonObject root = new JsonObject();
            JsonArray items = new JsonArray();
            
            int count = 0;
            for (ItemType itemType : itemTypes) {
                if (itemType != null) {
                    JsonObject itemJson = serializeItem(itemType);
                    items.add(itemJson);
                    count++;
                }
            }
            
            root.add("items", items);
            root.add("metadata", createMetadataJson("items", count));
            
            Path outputPath = getOutputPath("items");
            writeJsonToFile(root, outputPath);
            
            logger.info("Exported {} items to {}", count, outputPath);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to export items: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Serializes an ItemType to JSON
     */
    private JsonObject serializeItem(ItemType item) {
        JsonObject json = new JsonObject();
        
        // Basic information
        json.addProperty("id", item.id);
        json.addProperty("name", item.name != null ? item.name : "");
        json.addProperty("description", item.description != null ? item.description : "");
        
        // Model information
        json.addProperty("modelId", item.modelId);
        json.addProperty("modelZoom", item.modelZoom);
        json.addProperty("modelRotationX", item.modelRotationX);
        json.addProperty("modelRotationY", item.modelRotationY);
        json.addProperty("modelOffsetX", item.modelOffsetX);
        json.addProperty("modelOffsetY", item.modelOffsetY);
        
        // Stackable and noted
        json.addProperty("stackable", item.stackable);
        json.addProperty("noted", item.noted);
        if (item.noteTemplateId != -1) {
            json.addProperty("noteTemplateId", item.noteTemplateId);
        }
        if (item.noteLinkedId != -1) {
            json.addProperty("noteLinkedId", item.noteLinkedId);
        }
        
        // Tradeable and members
        json.addProperty("tradeable", item.tradeable);
        json.addProperty("members", item.members);
        
        // Equipment information
        if (item.equipmentSlot != -1) {
            json.addProperty("equipmentSlot", item.equipmentSlot);
        }
        if (item.equipmentType != -1) {
            json.addProperty("equipmentType", item.equipmentType);
        }
        
        // Combat stats
        if (item.equipmentStats != null) {
            JsonObject stats = new JsonObject();
            for (int i = 0; i < item.equipmentStats.length; i++) {
                String statName = getStatName(i);
                if (statName != null) {
                    stats.addProperty(statName, item.equipmentStats[i]);
                }
            }
            json.add("equipmentStats", stats);
        }
        
        // Values
        json.addProperty("value", item.value);
        if (item.lowAlchValue != -1) {
            json.addProperty("lowAlchValue", item.lowAlchValue);
        }
        if (item.highAlchValue != -1) {
            json.addProperty("highAlchValue", item.highAlchValue);
        }
        
        // Stack sizes
        if (item.stackSizes != null) {
            JsonArray stackSizes = new JsonArray();
            for (int size : item.stackSizes) {
                stackSizes.add(size);
            }
            json.add("stackSizes", stackSizes);
        }
        
        // Colors and textures
        if (item.originalModelColors != null) {
            JsonArray colors = new JsonArray();
            for (int color : item.originalModelColors) {
                colors.add(color);
            }
            json.add("originalModelColors", colors);
        }
        
        if (item.modifiedModelColors != null) {
            JsonArray colors = new JsonArray();
            for (int color : item.modifiedModelColors) {
                colors.add(color);
            }
            json.add("modifiedModelColors", colors);
        }
        
        if (item.originalModelTextures != null) {
            JsonArray textures = new JsonArray();
            for (short texture : item.originalModelTextures) {
                textures.add(texture);
            }
            json.add("originalModelTextures", textures);
        }
        
        if (item.modifiedModelTextures != null) {
            JsonArray textures = new JsonArray();
            for (short texture : item.modifiedModelTextures) {
                textures.add(texture);
            }
            json.add("modifiedModelTextures", textures);
        }
        
        // Ground actions
        if (item.groundActions != null) {
            JsonArray actions = new JsonArray();
            for (String action : item.groundActions) {
                actions.add(action != null ? action : "");
            }
            json.add("groundActions", actions);
        }
        
        // Inventory actions
        if (item.inventoryActions != null) {
            JsonArray actions = new JsonArray();
            for (String action : item.inventoryActions) {
                actions.add(action != null ? action : "");
            }
            json.add("inventoryActions", actions);
        }
        
        // Team
        json.addProperty("team", item.team);
        
        // Buy/sell prices (if available)
        if (item.buyPrice != -1) {
            json.addProperty("buyPrice", item.buyPrice);
        }
        if (item.sellPrice != -1) {
            json.addProperty("sellPrice", item.sellPrice);
        }
        
        return json;
    }
    
    /**
     * Gets the name of an equipment stat by index
     */
    private String getStatName(int index) {
        switch (index) {
            case 0: return "stabAttack";
            case 1: return "slashAttack";
            case 2: return "crushAttack";
            case 3: return "magicAttack";
            case 4: return "rangeAttack";
            case 5: return "stabDefence";
            case 6: return "slashDefence";
            case 7: return "crushDefence";
            case 8: return "magicDefence";
            case 9: return "rangeDefence";
            case 10: return "strength";
            case 11: return "prayer";
            default: return null;
        }
    }
}
