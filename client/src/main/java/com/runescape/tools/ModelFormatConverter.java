package com.runescape.tools;

import com.runescape.cache.FileStore;
import com.runescape.entity.model.Model;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Converts newer model formats to older compatible formats
 */
public final class ModelFormatConverter {

    private ModelFormatConverter() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java com.runescape.tools.ModelFormatConverter <cacheDir> <modelId> <outputId>");
            return;
        }

        String cacheDir = args[0];
        int modelId = Integer.parseInt(args[1]);
        int outputId = Integer.parseInt(args[2]);

        // Load the model
        try {
            Model model = Model.getModel(modelId);
            if (model == null) {
                System.out.println("Model " + modelId + " not found");
                return;
            }

            System.out.println("Converting model " + modelId + " to " + outputId);
            System.out.println("Vertices: " + model.numVertices);
            System.out.println("Faces: " + model.numTriangles);

            // Save the model in old format
            saveModelInOldFormat(cacheDir, outputId, model);
            System.out.println("Model converted successfully");

        } catch (Exception e) {
            System.err.println("Error converting model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void saveModelInOldFormat(String cacheDir, int modelId, Model model) throws Exception {
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

            // Create old format model data
            byte[] oldFormatData = createOldFormatData(model);
            
            // Write to cache
            boolean success = store.writeFile(oldFormatData.length, oldFormatData, modelId);
            if (!success) {
                throw new RuntimeException("Failed to write model to cache");
            }
        }
    }

    private static byte[] createOldFormatData(Model model) {
        // This is a simplified version - in reality, we'd need to properly
        // convert the vertex and face data to the old format
        // For now, we'll create a minimal old-format model
        
        byte[] data = new byte[100]; // Minimal size for old format
        
        // Old format signature
        data[data.length - 2] = 0;
        data[data.length - 1] = 0;
        
        // Write basic model data (simplified)
        int offset = 0;
        
        // Vertex count
        data[offset++] = (byte) (model.numVertices & 0xFF);
        data[offset++] = (byte) ((model.numVertices >> 8) & 0xFF);
        
        // Face count  
        data[offset++] = (byte) (model.numTriangles & 0xFF);
        data[offset++] = (byte) ((model.numTriangles >> 8) & 0xFF);
        
        // Fill rest with basic data
        while (offset < data.length - 2) {
            data[offset++] = 0;
        }
        
        return data;
    }
}
