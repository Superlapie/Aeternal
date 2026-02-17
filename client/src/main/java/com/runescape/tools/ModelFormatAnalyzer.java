package com.runescape.tools;

import com.runescape.cache.FileStore;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Analyzes the format of specific models to understand compatibility issues
 */
public final class ModelFormatAnalyzer {

    private ModelFormatAnalyzer() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java com.runescape.tools.ModelFormatAnalyzer <cacheDir> <modelId>");
            return;
        }

        String cacheDir = args[0];
        int modelId = Integer.parseInt(args[1]);

        // Try to load the model from idx1
        File datFile = new File(cacheDir + "main_file_cache.dat");
        if (!datFile.exists()) {
            datFile = new File(cacheDir + "main_file_cache.dat2");
        }

        File idxFile = new File(cacheDir + "main_file_cache.idx1");
        if (!datFile.exists() || !idxFile.exists()) {
            System.out.println("Cache files not found");
            return;
        }

        try (RandomAccessFile datRaf = new RandomAccessFile(datFile, "r");
             RandomAccessFile idxRaf = new RandomAccessFile(idxFile, "r")) {

            FileStore store = new FileStore(datRaf, idxRaf, 2); // Store 1 for models, +1 for FileStore
            byte[] modelData = store.decompress(modelId);

            if (modelData == null) {
                System.out.println("Model " + modelId + " not found");
                return;
            }

            System.out.println("Model " + modelId + " analysis:");
            System.out.println("Size: " + modelData.length + " bytes");
            
            if (modelData.length >= 2) {
                int lastByte = modelData[modelData.length - 1] & 0xFF;
                int secondLastByte = modelData[modelData.length - 2] & 0xFF;
                
                System.out.println("Last two bytes: " + secondLastByte + ", " + lastByte);
                
                if (lastByte == -3 && secondLastByte == -1) {
                    System.out.println("Format: Type 3 (newer format)");
                } else if (lastByte == -2 && secondLastByte == -1) {
                    System.out.println("Format: Type 2");
                } else if (lastByte == -1 && secondLastByte == -1) {
                    System.out.println("Format: New");
                } else {
                    System.out.println("Format: Old (legacy)");
                }
                
                // Show first few bytes for additional analysis
                System.out.print("First 10 bytes: ");
                for (int i = 0; i < Math.min(10, modelData.length); i++) {
                    System.out.print((modelData[i] & 0xFF) + " ");
                }
                System.out.println();
            }
        }
    }
}
