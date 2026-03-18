package com.cacheextractor.exporters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Base class for JSON exporters.
 * Provides common JSON serialization functionality.
 */
public abstract class JSONExporter {
    
    protected final ExtractionConfig config;
    protected final Gson gson;
    
    public JSONExporter(ExtractionConfig config) {
        this.config = config;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    }
    
    /**
     * Writes JSON object to file
     */
    protected void writeJsonToFile(JsonObject json, Path outputPath) throws IOException {
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            gson.toJson(json, writer);
        }
    }
    
    /**
     * Writes JSON array to file
     */
    protected void writeJsonToFile(JsonArray json, Path outputPath) throws IOException {
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            gson.toJson(json, writer);
        }
    }
    
    /**
     * Creates a JSON object with metadata
     */
    protected JsonObject createMetadataJson(String type, int count) {
        JsonObject metadata = new JsonObject();
        metadata.addProperty("type", type);
        metadata.addProperty("count", count);
        metadata.addProperty("extractedAt", System.currentTimeMillis());
        metadata.addProperty("version", "1.0.0");
        return metadata;
    }
    
    /**
     * Gets output file path for a specific data type
     */
    protected Path getOutputPath(String filename) {
        return config.getOutputPath().resolve(filename + ".json");
    }
}
