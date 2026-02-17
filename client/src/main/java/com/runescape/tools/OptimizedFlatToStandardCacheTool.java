package com.runescape.tools;

import com.runescape.cache.FileStore;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Optimized version of FlatToStandardCacheTool with better performance reporting.
 * 
 * Usage:
 * java com.runescape.tools.OptimizedFlatToStandardCacheTool
 *   <flatCacheDir> <targetCacheDir> <storeIndex> <archiveIdCsv>
 *   
 * Examples:
 *   - Import specific archives: "0,1,2,3,4,5"
 *   - Import range: "0-100"
 *   - Import all: "ALL"
 */
public final class OptimizedFlatToStandardCacheTool {

    private OptimizedFlatToStandardCacheTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java com.runescape.tools.OptimizedFlatToStandardCacheTool <flatCacheDir> <targetCacheDir> <storeIndex> <archiveIdCsv>");
            System.out.println("Examples:");
            System.out.println("  Import specific: 0,1,2,3,4,5");
            System.out.println("  Import range: 0-100");
            System.out.println("  Import all: ALL");
            return;
        }

        String flatCacheDir = normalize(args[0]);
        String targetCacheDir = normalize(args[1]);
        int storeIndex = Integer.parseInt(args[2]);
        String archiveIdsStr = args[3];

        // Validate flat cache directory
        Path flatStoreDir = Paths.get(flatCacheDir, String.valueOf(storeIndex));
        if (!Files.exists(flatStoreDir) || !Files.isDirectory(flatStoreDir)) {
            throw new IllegalStateException("Flat cache store directory not found: " + flatStoreDir);
        }

        // Parse archive IDs
        List<Integer> archiveIds = parseArchiveIds(archiveIdsStr, flatStoreDir);
        
        System.out.println("Importing " + archiveIds.size() + " archives from store " + storeIndex + "...");

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
                raf.setLength(0);
            }
        }

        long startTime = System.currentTimeMillis();
        int copied = 0;
        int missing = 0;
        int failed = 0;
        long totalBytes = 0;
        
        // Process archives sequentially but with progress reporting
        try (RandomAccessFile targetDatRaf = new RandomAccessFile(targetDat, "rw");
             RandomAccessFile targetIdxRaf = new RandomAccessFile(targetIdx, "rw")) {
            
            FileStore targetStore = new FileStore(targetDatRaf, targetIdxRaf, storeIndex + 1);
            
            for (int i = 0; i < archiveIds.size(); i++) {
                int archiveId = archiveIds.get(i);
                
                // Progress reporting every 100 archives or 10% whichever is smaller
                int reportInterval = Math.max(1, Math.min(100, archiveIds.size() / 10));
                if (i % reportInterval == 0 || i == archiveIds.size() - 1) {
                    double percent = (i * 100.0) / archiveIds.size();
                    System.out.printf("Progress: %d/%d (%.1f%%)%n", i, archiveIds.size(), percent);
                }
                
                Path flatFile = flatStoreDir.resolve(archiveId + ".dat");
                
                if (!Files.exists(flatFile)) {
                    System.out.println("MISS " + archiveId + " (file not found)");
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
                    totalBytes += payload.length;
                    copied++;
                } else {
                    System.out.println("FAIL " + archiveId);
                    failed++;
                }
            }
        }
        
        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0;
        double mbPerSec = (totalBytes / (1024.0 * 1024.0)) / seconds;
        
        System.out.println("\nDone. copied=" + copied + ", missing=" + missing + ", failed=" + failed);
        System.out.printf("Total data: %.2f MB%n", totalBytes / (1024.0 * 1024.0));
        System.out.printf("Completed in %.2f seconds (%.2f archives/sec, %.2f MB/sec)%n", 
                         seconds, archiveIds.size() / seconds, mbPerSec);
    }
    
    private static List<Integer> parseArchiveIds(String input, Path flatStoreDir) throws IOException {
        if ("ALL".equalsIgnoreCase(input.trim())) {
            // Find all .dat files in the directory
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(flatStoreDir, "*.dat")) {
                List<Integer> ids = new ArrayList<>();
                for (Path path : stream) {
                    String name = path.getFileName().toString();
                    ids.add(Integer.parseInt(name.substring(0, name.length() - 4))); // Remove .dat extension
                }
                return ids.stream().sorted().collect(Collectors.toList());
            }
        } else if (input.contains("-")) {
            // Parse range like "0-100"
            String[] parts = input.split("-");
            int start = Integer.parseInt(parts[0].trim());
            int end = Integer.parseInt(parts[1].trim());
            return IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
        } else {
            // Parse comma-separated list
            return Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }
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
