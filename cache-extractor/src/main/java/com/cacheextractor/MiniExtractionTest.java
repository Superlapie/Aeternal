package com.cacheextractor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mini extraction test to demonstrate cache loading with the local Elvarg cache
 */
public class MiniExtractionTest {
    
    private static final Logger logger = LoggerFactory.getLogger(MiniExtractionTest.class);
    
    public static void main(String[] args) {
        System.out.println("=== Mini Cache Extraction Test ===");
        System.out.println("Testing with local Elvarg cache...\n");
        
        try {
            // Test cache discovery
            Path cachePath = testLocalCacheDiscovery();
            
            if (cachePath != null) {
                System.out.println("✅ Successfully found local cache!");
                System.out.println("   Path: " + cachePath.toAbsolutePath());
                
                // Test cache validation
                boolean isValid = CacheLoader.isValidCacheDirectory(cachePath);
                System.out.println("   Valid cache: " + (isValid ? "✅ YES" : "❌ NO"));
                
                // List cache files
                listCacheFiles(cachePath);
                
                // Demonstrate extraction configuration
                demonstrateExtractionConfig(cachePath);
                
            } else {
                System.out.println("❌ Could not find local cache");
                System.out.println("   Expected: ../client/Cache");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== Next Steps ===");
        System.out.println("1. Build the complete tool: ./gradlew build");
        System.out.println("2. Run full extraction: java -jar build/libs/cache-extractor.jar");
        System.out.println("3. Check output in ./export directory");
    }
    
    private static Path testLocalCacheDiscovery() {
        // Test the same paths as CacheLoader
        String[] cachePaths = {
            "../client/Cache",                    // Local Elvarg cache
            "./client/Cache",                    // Alternative local path
            "../../client/Cache",                // Another local path
        };
        
        for (String path : cachePaths) {
            Path cachePath = Paths.get(path);
            if (CacheLoader.isValidCacheDirectory(cachePath)) {
                return cachePath;
            }
        }
        
        return null;
    }
    
    private static void listCacheFiles(Path cachePath) {
        System.out.println("\nCache files:");
        File cacheDir = cachePath.toFile();
        File[] files = cacheDir.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("main_file_cache")) {
                    long size = file.length();
                    System.out.println("   " + file.getName() + " (" + size + " bytes)");
                }
            }
        }
        
        // Check for other important files
        String[] importantFiles = {"obj.dat", "obj.idx", "loc.dat", "loc.idx", "seq.dat", "spotanim.dat"};
        for (String fileName : importantFiles) {
            File file = new File(cacheDir, fileName);
            if (file.exists()) {
                System.out.println("   " + fileName + " (" + file.length() + " bytes)");
            }
        }
    }
    
    private static void demonstrateExtractionConfig(Path cachePath) {
        System.out.println("\nExtraction configuration:");
        System.out.println("   Cache path: " + cachePath);
        System.out.println("   Output path: ./export");
        System.out.println("   Export types: objects,items,npcs,animations,models");
        System.out.println("   Include actions: true");
        System.out.println("   Format: json");
        
        // Create a sample configuration
        try {
            ExtractionConfig config = new ExtractionConfig(
                cachePath,
                Paths.get("./export"),
                java.util.Set.of("objects", "items", "npcs", "animations", "models"),
                true,
                false,
                "json"
            );
            
            System.out.println("   Config created: ✅");
            System.out.println("   Ready for extraction: ✅");
            
        } catch (Exception e) {
            System.out.println("   Config created: ❌ (" + e.getMessage() + ")");
        }
    }
}
