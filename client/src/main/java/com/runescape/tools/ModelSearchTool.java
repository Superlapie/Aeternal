package com.runescape.tools;

import com.runescape.cache.FileStore;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Searches for models in a cache
 */
public final class ModelSearchTool {

    private ModelSearchTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java com.runescape.tools.ModelSearchTool <cacheDir> <startId> <endId>");
            return;
        }

        String cacheDir = args[0];
        int startId = Integer.parseInt(args[1]);
        int endId = Integer.parseInt(args[2]);

        File datFile = new File(cacheDir + "main_file_cache.dat");
        if (!datFile.exists()) {
            datFile = new File(cacheDir + "main_file_cache.dat2");
        }

        File idxFile = new File(cacheDir + "main_file_cache.idx1");
        if (!datFile.exists() || !idxFile.exists()) {
            throw new IllegalStateException("Cache files not found");
        }

        try (RandomAccessFile datRaf = new RandomAccessFile(datFile, "r");
             RandomAccessFile idxRaf = new RandomAccessFile(idxFile, "r")) {

            FileStore store = new FileStore(datRaf, idxRaf, 2);

            System.out.println("Searching for models from " + startId + " to " + endId + "...");
            
            int found = 0;
            for (int modelId = startId; modelId <= endId; modelId++) {
                byte[] data = store.decompress(modelId);
                if (data != null && data.length > 0) {
                    found++;
                    System.out.println("Found model " + modelId + " (" + data.length + " bytes)");
                    
                    // Check if it might be a moon model by looking at size
                    if (data.length > 10000) {
                        System.out.println("  -> Large model, possible moon model: " + modelId);
                    }
                }
            }
            
            System.out.println("Found " + found + " models in range " + startId + "-" + endId);
        }
    }
}
