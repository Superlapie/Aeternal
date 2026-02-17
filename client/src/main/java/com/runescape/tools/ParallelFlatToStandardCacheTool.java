package com.runescape.tools;

import com.runescape.cache.FileStore;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.util.stream.Collectors;

/**
 * Parallel version of FlatToStandardCacheTool for faster batch imports.
 * 
 * Usage:
 * java com.runescape.tools.ParallelFlatToStandardCacheTool
 *   <flatCacheDir> <targetCacheDir> <storeIndex> <archiveIdCsv> [threads]
 *   
 * Examples:
 *   - Import specific archives: "0,1,2,3,4,5"
 *   - Import range: "0-100"
 *   - Import all: "ALL"
 */
public final class ParallelFlatToStandardCacheTool {

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();
    
    private ParallelFlatToStandardCacheTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java com.runescape.tools.ParallelFlatToStandardCacheTool <flatCacheDir> <targetCacheDir> <storeIndex> <archiveIdCsv> [threads]");
            System.out.println("Examples:");
            System.out.println("  Import specific: 0,1,2,3,4,5");
            System.out.println("  Import range: 0-100");
            System.out.println("  Import all: ALL");
            System.out.println("  With custom threads: ... ALL 8");
            return;
        }

        String flatCacheDir = normalize(args[0]);
        String targetCacheDir = normalize(args[1]);
        int storeIndex = Integer.parseInt(args[2]);
        String archiveIdsStr = args[3];
        int threads = args.length > 4 ? Integer.parseInt(args[4]) : DEFAULT_THREADS;

        // Validate flat cache directory
        Path flatStoreDir = Paths.get(flatCacheDir, String.valueOf(storeIndex));
        if (!Files.exists(flatStoreDir) || !Files.isDirectory(flatStoreDir)) {
            throw new IllegalStateException("Flat cache store directory not found: " + flatStoreDir);
        }

        // Parse archive IDs
        List<Integer> archiveIds = parseArchiveIds(archiveIdsStr, flatStoreDir);
        
        System.out.println("Importing " + archiveIds.size() + " archives from store " + storeIndex + " using " + threads + " threads...");

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

        // Use thread pool for parallel processing
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<ImportResult>> futures = new ArrayList<>();
        
        // Keep files open during the entire operation
        RandomAccessFile targetDatRaf = new RandomAccessFile(targetDat, "rw");
        RandomAccessFile targetIdxRaf = new RandomAccessFile(targetIdx, "rw");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Submit tasks - each task gets its own FileStore instance
            for (int i = 0; i < archiveIds.size(); i++) {
                final int archiveId = archiveIds.get(i);
                
                futures.add(executor.submit(() -> {
                    // Create FileStore per task to avoid synchronization issues
                    FileStore store = new FileStore(targetDatRaf, targetIdxRaf, storeIndex + 1);
                    return importArchive(flatStoreDir, store, archiveId);
                }));
            }
        
        // Collect results
        int copied = 0;
        int missing = 0;
        int failed = 0;
        
        for (Future<ImportResult> future : futures) {
            try {
                ImportResult result = future.get();
                synchronized (System.out) {
                    if (result.success) {
                        System.out.println("OK   " + result.archiveId + " (" + result.size + " bytes)");
                        copied++;
                    } else if (result.missing) {
                        System.out.println("MISS " + result.archiveId + " (" + result.message + ")");
                        missing++;
                    } else {
                        System.out.println("FAIL " + result.archiveId + " (" + result.message + ")");
                        failed++;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing task: " + e.getMessage());
                failed++;
            }
        }
        
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0;
        
        System.out.println("\nDone. copied=" + copied + ", missing=" + missing + ", failed=" + failed);
        System.out.printf("Completed in %.2f seconds (%.2f archives/sec)\n", seconds, archiveIds.size() / seconds);
        
        } finally {
            // Clean up resources
            targetDatRaf.close();
            targetIdxRaf.close();
        }
    }
    
    private static ImportResult importArchive(Path flatStoreDir, FileStore store, int archiveId) {
        Path flatFile = flatStoreDir.resolve(archiveId + ".dat");
        
        if (!Files.exists(flatFile)) {
            return new ImportResult(archiveId, false, true, 0, "file not found");
        }
        
        try {
            byte[] payload = Files.readAllBytes(flatFile);
            if (payload.length == 0) {
                return new ImportResult(archiveId, false, true, 0, "empty file");
            }
            
            boolean ok = store.writeFile(payload.length, payload, archiveId);
            if (ok) {
                return new ImportResult(archiveId, true, false, payload.length, null);
            } else {
                return new ImportResult(archiveId, false, false, 0, "write failed");
            }
        } catch (Exception e) {
            return new ImportResult(archiveId, false, false, 0, e.getMessage());
        }
    }
    
    private static List<Integer> parseArchiveIds(String input, Path flatStoreDir) throws IOException {
        if ("ALL".equalsIgnoreCase(input.trim())) {
            // Find all .dat files in the directory
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(flatStoreDir, "*.dat")) {
                return StreamSupport.stream(stream.spliterator(), false)
                    .map(path -> path.getFileName().toString())
                    .map(name -> name.substring(0, name.length() - 4)) // Remove .dat extension
                    .map(Integer::parseInt)
                    .sorted()
                    .collect(Collectors.toList());
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
    
    private static class ImportResult {
        final int archiveId;
        final boolean success;
        final boolean missing;
        final int size;
        final String message;
        
        ImportResult(int archiveId, boolean success, boolean missing, int size, String message) {
            this.archiveId = archiveId;
            this.success = success;
            this.missing = missing;
            this.size = size;
            this.message = message;
        }
    }
}
