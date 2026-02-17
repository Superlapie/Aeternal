package com.runescape.tools;

import com.runescape.cache.FileStore;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts flat cache format to standard cache format.
 * 
 * Usage:
 * java com.runescape.tools.FlatToStandardCacheTool
 *   <flatCacheDir> <targetCacheDir> <storeIndex> <archiveIdCsv>
 */
public final class FlatToStandardCacheTool {

    private FlatToStandardCacheTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java com.runescape.tools.FlatToStandardCacheTool <flatCacheDir> <targetCacheDir> <storeIndex> <archiveIdCsv>");
            System.out.println("Example: java com.runescape.tools.FlatToStandardCacheTool D:/ext/openrs2-2446-flat/cache ./Cache 8 0,1,2,3,4,5");
            return;
        }

        String flatCacheDir = normalize(args[0]);
        String targetCacheDir = normalize(args[1]);
        int storeIndex = Integer.parseInt(args[2]);
        List<Integer> archiveIds = parseIds(args[3]);

        // Validate flat cache directory
        Path flatStoreDir = Paths.get(flatCacheDir, String.valueOf(storeIndex));
        if (!Files.exists(flatStoreDir) || !Files.isDirectory(flatStoreDir)) {
            throw new IllegalStateException("Flat cache store directory not found: " + flatStoreDir);
        }

        // Validate target cache
        File targetDat = resolveDatFile(targetCacheDir);
        File targetIdx = new File(targetCacheDir + "main_file_cache.idx" + storeIndex);
        
        if (!targetDat.exists()) {
            throw new IllegalStateException("Target cache missing .dat/.dat2 file: " + targetDat);
        }
        
        // Create idx file if it doesn't exist (for stores beyond 4)
        if (!targetIdx.exists()) {
            System.out.println("Creating new idx file: " + targetIdx);
            try (RandomAccessFile raf = new RandomAccessFile(targetIdx, "rw")) {
                // Initialize empty idx file
                raf.setLength(0);
            }
        }

        try (RandomAccessFile targetDatRaf = new RandomAccessFile(targetDat, "rw");
             RandomAccessFile targetIdxRaf = new RandomAccessFile(targetIdx, "rw")) {

            // Use FileStore to write to standard format
            FileStore targetStore = new FileStore(targetDatRaf, targetIdxRaf, storeIndex + 1);

            int copied = 0;
            int missing = 0;
            int failed = 0;
            
            for (int archiveId : archiveIds) {
                Path flatFile = flatStoreDir.resolve(archiveId + ".dat");
                
                if (!Files.exists(flatFile)) {
                    System.out.println("MISS " + archiveId + " (file not found: " + flatFile + ")");
                    missing++;
                    continue;
                }
                
                byte[] payload = Files.readAllBytes(flatFile);
                if (payload.length == 0) {
                    System.out.println("MISS " + archiveId + " (empty file)");
                    missing++;
                    continue;
                }
                
                boolean ok = targetStore.writeFile(payload.length, payload, archiveId);
                if (ok) {
                    System.out.println("OK   " + archiveId + " (" + payload.length + " bytes)");
                    copied++;
                } else {
                    System.out.println("FAIL " + archiveId);
                    failed++;
                }
            }
            
            System.out.println("Done. copied=" + copied + ", missing=" + missing + ", failed=" + failed);
        }
    }

    private static List<Integer> parseIds(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private static String normalize(String dir) {
        return dir.endsWith("/") || dir.endsWith("\\") ? dir : dir + File.separator;
    }

    private static File resolveDatFile(String cacheDir) {
        File dat = new File(cacheDir + "main_file_cache.dat");
        if (dat.exists()) {
            return dat;
        }
        return new File(cacheDir + "main_file_cache.dat2");
    }
}
