package com.cacheextractor.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for JSON operations.
 * Provides common JSON parsing and formatting functionality.
 */
public class JSONUtils {
    
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();
    
    /**
     * Parses JSON file to JsonElement
     */
    public static JsonElement parseJsonFile(Path filePath) throws IOException {
        try (FileReader reader = new FileReader(filePath.toFile())) {
            return JsonParser.parseReader(reader);
        }
    }
    
    /**
     * Converts object to JSON string
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
    
    /**
     * Converts JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
    
    /**
     * Validates JSON file syntax
     */
    public static boolean isValidJson(Path filePath) {
        if (!Files.exists(filePath)) {
            return false;
        }
        
        try {
            parseJsonFile(filePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets JSON file size in bytes
     */
    public static long getFileSize(Path filePath) throws IOException {
        return Files.size(filePath);
    }
    
    /**
     * Formats JSON string with pretty printing
     */
    public static String formatJson(String json) {
        JsonElement element = JsonParser.parseString(json);
        return GSON.toJson(element);
    }
}
