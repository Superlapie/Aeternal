package com.runescape.tools;

import com.runescape.cache.FileStore;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Generates placeholder models for moon equipment that won't crash the client
 */
public final class MoonModelPlaceholderGenerator {

    private MoonModelPlaceholderGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java com.runescape.tools.MoonModelPlaceholderGenerator <cacheDir> <modelId1,modelId2,...>");
            return;
        }

        String cacheDir = args[0];
        String[] modelIds = args[1].split(",");

        File datFile = new File(cacheDir + "main_file_cache.dat");
        if (!datFile.exists()) {
            datFile = new File(cacheDir + "main_file_cache.dat2");
        }

        File idxFile = new File(cacheDir + "main_file_cache.idx1");
        if (!datFile.exists() || !idxFile.exists()) {
            throw new IllegalStateException("Cache files not found");
        }

        try (RandomAccessFile datRaf = new RandomAccessFile(datFile, "rw");
             RandomAccessFile idxRaf = new RandomAccessFile(idxFile, "rw")) {

            FileStore store = new FileStore(datRaf, idxRaf, 2);

            for (String modelIdStr : modelIds) {
                int modelId = Integer.parseInt(modelIdStr.trim());
                
                // Create a simple placeholder model
                byte[] placeholderData = createPlaceholderModelData();
                
                boolean success = store.writeFile(placeholderData.length, placeholderData, modelId);
                if (success) {
                    System.out.println("Created placeholder model " + modelId + " (" + placeholderData.length + " bytes)");
                } else {
                    System.out.println("Failed to create placeholder model " + modelId);
                }
            }
        }
    }

    private static byte[] createPlaceholderModelData() {
        // Create a minimal old-format model that won't crash
        byte[] data = new byte[50]; // Very small model
        
        int offset = 0;
        
        // Vertex count (small)
        data[offset++] = 8; // 8 vertices
        data[offset++] = 0;
        
        // Face count (small)
        data[offset++] = 12; // 12 triangles
        data[offset++] = 0;
        
        // Minimal vertex data
        for (int i = 0; i < 8 * 3; i++) { // 8 vertices * 3 coords
            data[offset++] = 0; // x, y, z = 0
        }
        
        // Minimal face data
        for (int i = 0; i < 12 * 3; i++) { // 12 faces * 3 vertex indices
            data[offset++] = (byte) (i % 8); // vertex index
        }
        
        // Fill rest with zeros
        while (offset < data.length - 2) {
            data[offset++] = 0;
        }
        
        // Old format signature
        data[data.length - 2] = 0;
        data[data.length - 1] = 0;
        
        return data;
    }
}
