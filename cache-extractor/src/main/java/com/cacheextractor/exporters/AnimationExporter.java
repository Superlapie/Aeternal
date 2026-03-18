package com.cacheextractor.exporters;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.openrs.cache.Cache;
import net.openrs.cache.type.TypeList;
import net.openrs.cache.type.sequences.SequenceType;

/**
 * Exports animation definitions from the OSRS cache to JSON format.
 * Includes frame data and animation metadata for RSPS development.
 */
public class AnimationExporter extends JSONExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(AnimationExporter.class);
    
    public AnimationExporter(ExtractionConfig config) {
        super(config);
    }
    
    /**
     * Exports all animation definitions to JSON
     */
    public boolean export(Cache cache) {
        try {
            logger.info("Starting animation export...");
            
            TypeList<SequenceType> sequenceTypes = cache.getTypeList(SequenceType.class);
            JsonObject root = new JsonObject();
            JsonArray animations = new JsonArray();
            
            int count = 0;
            for (SequenceType sequenceType : sequenceTypes) {
                if (sequenceType != null) {
                    JsonObject animationJson = serializeAnimation(sequenceType);
                    animations.add(animationJson);
                    count++;
                }
            }
            
            root.add("animations", animations);
            root.add("metadata", createMetadataJson("animations", count));
            
            Path outputPath = getOutputPath("animations");
            writeJsonToFile(root, outputPath);
            
            logger.info("Exported {} animations to {}", count, outputPath);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to export animations: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Serializes a SequenceType to JSON
     */
    private JsonObject serializeAnimation(SequenceType seq) {
        JsonObject json = new JsonObject();
        
        // Basic information
        json.addProperty("id", seq.id);
        
        // Frame IDs
        if (seq.frameIds != null) {
            JsonArray frameIds = new JsonArray();
            for (int frameId : seq.frameIds) {
                frameIds.add(frameId);
            }
            json.add("frameIds", frameIds);
        }
        
        // Frame lengths
        if (seq.frameLengths != null) {
            JsonArray frameLengths = new JsonArray();
            for (int length : seq.frameLengths) {
                frameLengths.add(length);
            }
            json.add("frameLengths", frameLengths);
        }
        
        // Frame sounds
        if (seq.frameSounds != null) {
            JsonArray frameSounds = new JsonArray();
            for (int sound : seq.frameSounds) {
                frameSounds.add(sound);
            }
            json.add("frameSounds", frameSounds);
        }
        
        // Frame chat head models
        if (seq.frameChatHeadModels != null) {
            JsonArray chatHeadModels = new JsonArray();
            for (int modelId : seq.frameChatHeadModels) {
                chatHeadModels.add(modelId);
            }
            json.add("frameChatHeadModels", chatHeadModels);
        }
        
        // Frame priorities
        if (seq.framePriorities != null) {
            JsonArray priorities = new JsonArray();
            for (int priority : seq.framePriorities) {
                priorities.add(priority);
            }
            json.add("framePriorities", priorities);
        }
        
        // Frame main hand models
        if (seq.frameMainHandModels != null) {
            JsonArray mainHandModels = new JsonArray();
            for (int modelId : seq.frameMainHandModels) {
                mainHandModels.add(modelId);
            }
            json.add("frameMainHandModels", mainHandModels);
        }
        
        // Frame off hand models
        if (seq.frameOffHandModels != null) {
            JsonArray offHandModels = new JsonArray();
            for (int modelId : seq.frameOffHandModels) {
                offHandModels.add(modelId);
            }
            json.add("frameOffHandModels", offHandModels);
        }
        
        // Loop frame
        if (seq.loopFrame != -1) {
            json.addProperty("loopFrame", seq.loopFrame);
        }
        
        // Interleave offset
        if (seq.interleaveOffset != -1) {
            json.addProperty("interleaveOffset", seq.interleaveOffset);
        }
        
        // Stretches
        if (seq.stretches) {
            json.addProperty("stretches", true);
        }
        
        // Forced priority
        if (seq.forcedPriority != -1) {
            json.addProperty("forcedPriority", seq.forcedPriority);
        }
        
        // Priority
        if (seq.priority != -1) {
            json.addProperty("priority", seq.priority);
        }
        
        // Replay mode
        if (seq.replayMode != -1) {
            json.addProperty("replayMode", seq.replayMode);
        }
        
        // Flags
        json.addProperty("playerSequence", seq.playerSequence);
        json.addProperty("reset", seq.reset);
        
        // Calculate total duration
        int totalDuration = 0;
        if (seq.frameLengths != null) {
            for (int length : seq.frameLengths) {
                totalDuration += length;
            }
        }
        json.addProperty("totalDuration", totalDuration);
        
        // Frame count
        int frameCount = seq.frameIds != null ? seq.frameIds.length : 0;
        json.addProperty("frameCount", frameCount);
        
        return json;
    }
}
