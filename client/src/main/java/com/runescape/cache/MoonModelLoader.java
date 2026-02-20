package com.runescape.cache;

import com.runescape.entity.model.Model;
import com.runescape.cache.FileStore;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Handles loading of moon equipment models from external cache sources
 * when they're not available in the main cache.
 */
public final class MoonModelLoader {
    private static final boolean VERBOSE_MOON_LOGS = Boolean.getBoolean("client.verboseMoonLogs");

    // Moon equipment model mappings (item ID -> model ID)
    private static final Map<Integer, Integer> MOON_MODEL_MAPPINGS = new HashMap<>();
    private static final String EXTERNAL_CACHE_PATH = "./_ext/openrs2-2446-flat/cache";
    
    // Fallback models for moon equipment (if moon models fail to load)
    private static final Map<Integer, Integer> MOON_FALLBACK_MODELS = new HashMap<>();
    
    static {
        // Eclipse Moon set (2446 IDs)
        MOON_MODEL_MAPPINGS.put(29010, 52255); // Eclipse moon helm (inventory model)
        MOON_MODEL_MAPPINGS.put(29004, 52238); // Eclipse moon chestplate (inventory model)
        MOON_MODEL_MAPPINGS.put(29007, 52245); // Eclipse moon tassets (inventory model)
        MOON_MODEL_MAPPINGS.put(29000, 52263); // Eclipse atlatl (inventory model)
        MOON_MODEL_MAPPINGS.put(28991, 52270); // Atlatl dart (inventory model)
        
        // Blood Moon set (2446 IDs)
        MOON_MODEL_MAPPINGS.put(29013, 39574); // Blood moon helm
        MOON_MODEL_MAPPINGS.put(29016, 39575); // Blood moon chestplate
        MOON_MODEL_MAPPINGS.put(29019, 39317); // Blood moon tassets
        MOON_MODEL_MAPPINGS.put(29022, 39316); // Dual macuahuitl

        // Blue Moon set (2446 IDs)
        MOON_MODEL_MAPPINGS.put(29025, 39447); // Blue moon helm
        MOON_MODEL_MAPPINGS.put(29028, 39309); // Blue moon chestplate
        MOON_MODEL_MAPPINGS.put(29031, 39512); // Blue moon tassets
        MOON_MODEL_MAPPINGS.put(29034, 39511); // Blue moon spear
        
        // Fallback models (using existing models that look similar)
        MOON_FALLBACK_MODELS.put(29010, 2665); // Dragon med helm
        MOON_FALLBACK_MODELS.put(29004, 2660); // Dragon chainbody
        MOON_FALLBACK_MODELS.put(29007, 2655); // Dragon platelegs
        MOON_FALLBACK_MODELS.put(29000, 841);  // Magic longbow
        MOON_FALLBACK_MODELS.put(28991, 806);  // Bronze dart
        MOON_FALLBACK_MODELS.put(29013, 2665); // Dragon med helm
        MOON_FALLBACK_MODELS.put(29016, 2660); // Dragon chainbody
        MOON_FALLBACK_MODELS.put(29019, 2655); // Dragon platelegs
        MOON_FALLBACK_MODELS.put(29022, 4587); // Dragon scimitar
        MOON_FALLBACK_MODELS.put(29025, 2665); // Dragon med helm
        MOON_FALLBACK_MODELS.put(29028, 2660); // Dragon chainbody
        MOON_FALLBACK_MODELS.put(29031, 2655); // Dragon platelegs
        MOON_FALLBACK_MODELS.put(29034, 1249); // Dragon spear
    }

    /**
     * Attempts to load a moon model from external cache if available
     * @param itemId The item ID requesting the model
     * @param fallbackModelId The fallback model ID to use if external model fails
     * @return The model ID to use, either external or fallback
     */
    public static int getMoonModel(int itemId, int fallbackModelId) {
        Integer externalModelId = MOON_MODEL_MAPPINGS.get(itemId);
        if (externalModelId == null) {
            return fallbackModelId;
        }

        try {
            // Check if model is already in our main cache store 7 (from import)
            if (Model.isCached(externalModelId)) {
                // Try to get model to test if it's compatible
                Model testModel = Model.getModel(externalModelId);
                if (testModel != null) {
                    if (VERBOSE_MOON_LOGS) {
                        System.out.println("SUCCESS: Using moon model " + externalModelId + " for item " + itemId);
                    }
                    return externalModelId;
                } else {
                    if (VERBOSE_MOON_LOGS) {
                        System.err.println("FAILED: Model.getModel returned null for " + externalModelId);
                    }
                }
            } else {
                if (VERBOSE_MOON_LOGS) {
                    System.err.println("FAILED: Model.isCached returned false for " + externalModelId);
                }
            }
        } catch (Exception e) {
            if (VERBOSE_MOON_LOGS) {
                System.err.println("FAILED: Moon model " + externalModelId + " crashed for item " + itemId + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        // Final fallback to provided model
        if (VERBOSE_MOON_LOGS) {
            System.out.println("FALLBACK: Using model " + fallbackModelId + " for item " + itemId);
        }
        return fallbackModelId;
    }

    /**
     * Attempts to load a model from the external cache
     * @param modelId The model ID to load
     * @return true if successful, false otherwise
     */
    private static boolean loadExternalModel(int modelId) {
        Path externalCacheDir = Paths.get(EXTERNAL_CACHE_PATH);
        if (!Files.exists(externalCacheDir)) {
            return false;
        }

        try {
            // Check if model is already in our main cache store 7 (from import)
            if (Model.isCached(modelId)) {
                if (VERBOSE_MOON_LOGS) {
                    System.out.println("Model " + modelId + " already cached");
                }
                return true;
            }
            
            // Models are in store 7 (imported from external cache)
            // Store 1 typically contains direct models but our moon models are in 7
            Path modelFile7 = externalCacheDir.resolve("7/" + modelId + ".dat");
            Path modelFile1 = externalCacheDir.resolve("1/" + modelId + ".dat");
            
            byte[] modelData = null;
            int targetStore = 7; // Default to store 7 for moon models
            
            // Try store 7 first (where moon models are located)
            if (Files.exists(modelFile7)) {
                modelData = Files.readAllBytes(modelFile7);
                if (VERBOSE_MOON_LOGS) {
                    System.out.println("Found model " + modelId + " in store 7");
                }
            }
            // Try store 1 as fallback
            else if (Files.exists(modelFile1)) {
                modelData = Files.readAllBytes(modelFile1);
                targetStore = 1;
                if (VERBOSE_MOON_LOGS) {
                    System.out.println("Found model " + modelId + " in store 1");
                }
            }
            
            if (modelData != null && modelData.length > 0) {
                // Add the model to the appropriate cache store
                return addToMainCache(modelId, modelData, targetStore);
            }
            
        } catch (Exception e) {
            if (VERBOSE_MOON_LOGS) {
                System.err.println("Error loading external model " + modelId + ": " + e.getMessage());
            }
        }
        
        return false;
    }

    /**
     * Attempts to decompress a model from store 7 archive format
     */
    private static byte[] decompressModelArchive(byte[] archivedData, int modelId) {
        try {
            // Store 7 contains archived models that need decompression
            // We need to use the same decompression as FileStore but for a single model
            
            // Create a temporary FileStore to decompress the data
            // Since we can't easily replicate the exact FileStore decompression here,
            // let's try a simpler approach - treat the archived data as compressed
            
            // For now, return the raw data and see if the model loader can handle it
            // If not, we'll need to implement proper decompression
            return archivedData;
            
        } catch (Exception e) {
            if (VERBOSE_MOON_LOGS) {
                System.err.println("Failed to decompress model archive " + modelId + ": " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Attempts to add a model to the main cache
     * @param modelId The model ID
     * @param modelData The model data
     * @param targetStoreIndex The target store index (1 or 7)
     * @return true if successful
     */
    private static boolean addToMainCache(int modelId, byte[] modelData, int targetStoreIndex) {
        try {
            String cacheDir = "./Cache/";
            File datFile = new File(cacheDir + "main_file_cache.dat");
            if (!datFile.exists()) {
                datFile = new File(cacheDir + "main_file_cache.dat2");
            }
            
            // Use the specified store index
            File idxFile = new File(cacheDir + "main_file_cache.idx" + targetStoreIndex);
            
            // Create idx file if it doesn't exist
            if (!idxFile.exists()) {
                idxFile.createNewFile();
            }
            
            try (RandomAccessFile datRaf = new RandomAccessFile(datFile, "rw");
                 RandomAccessFile idxRaf = new RandomAccessFile(idxFile, "rw")) {
                
                FileStore store = new FileStore(datRaf, idxRaf, targetStoreIndex);
                boolean success = store.writeFile(modelData.length, modelData, modelId);
                
                if (success) {
                    // Clear the model cache to force reload
                    Model.clear();
                    if (VERBOSE_MOON_LOGS) {
                        System.out.println("Successfully added model " + modelId + " to store " + targetStoreIndex);
                    }
                    return true;
                }
            }
            
        } catch (Exception e) {
            if (VERBOSE_MOON_LOGS) {
                System.err.println("Error adding model " + modelId + " to main cache: " + e.getMessage());
            }
        }
        
        return false;
    }

    /**
     * Preloads all available moon models (no longer needed since models are imported)
     */
    public static void preloadMoonModels() {
        if (VERBOSE_MOON_LOGS) {
            System.out.println("Moon models already imported to idx1 - skipping preload");
        }
    }

    /**
     * Check if an item uses moon equipment models
     */
    public static boolean isMoonEquipment(int itemId) {
        return MOON_MODEL_MAPPINGS.containsKey(itemId);
    }
}
