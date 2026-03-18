package com.cacheextractor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.openrs.cache.Cache;
import net.openrs.cache.type.TypeList;
import net.openrs.cache.type.objects.ObjectType;
import net.openrs.cache.type.items.ItemType;
import net.openrs.cache.type.npcs.NPCType;

/**
 * Utility class for loading and validating OSRS cache files.
 * Handles cache discovery and basic validation.
 */
public class CacheLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheLoader.class);
    
    // Standard OSRS cache locations - prioritize local cache first
    private static final List<String> CACHE_PATHS = Arrays.asList(
        "../client/Cache",                    // Local Elvarg cache
        "./client/Cache",                    // Alternative local path
        "../../client/Cache",                // Another local path
        System.getProperty("user.home") + "/jagexcache/oldschool/LIVE",
        System.getProperty("user.home") + "/.jagex_cache_32/oldschool/LIVE",
        System.getProperty("user.home") + "/.jagex_cache_64/oldschool/LIVE",
        "./cache",
        "../cache",
        "./repository/cache"
    );
    
    // Required cache files
    private static final List<String> REQUIRED_FILES = Arrays.asList(
        "main_file_cache.dat0",
        "main_file_cache.dat1",
        "main_file_cache.dat2"
    );
    
    /**
     * Discovers cache path from standard locations
     */
    public static Path discoverCachePath() {
        for (String pathStr : CACHE_PATHS) {
            Path path = Paths.get(pathStr);
            if (isValidCacheDirectory(path)) {
                return path;
            }
        }
        return null;
    }
    
    /**
     * Validates if directory contains valid cache files
     */
    public static boolean isValidCacheDirectory(Path path) {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return false;
        }
        
        File dir = path.toFile();
        File[] files = dir.listFiles((file, name) -> name.startsWith("main_file_cache"));
        
        return files != null && files.length >= 3;
    }
    
    /**
     * Loads cache from specified path
     */
    public static Cache loadCache(Path cachePath) throws Exception {
        logger.info("Loading cache from: {}", cachePath);
        
        if (!isValidCacheDirectory(cachePath)) {
            throw new IllegalArgumentException("Invalid cache directory: " + cachePath);
        }
        
        try {
            Cache cache = new Cache(cachePath.toFile());
            logger.info("Cache loaded successfully");
            return cache;
        } catch (Exception e) {
            throw new Exception("Failed to load cache: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets cache statistics
     */
    public static CacheStats getCacheStats(Cache cache) {
        CacheStats stats = new CacheStats();
        
        try {
            TypeList<ObjectType> objects = cache.getTypeList(ObjectType.class);
            TypeList<ItemType> items = cache.getTypeList(ItemType.class);
            TypeList<NPCType> npcs = cache.getTypeList(NPCType.class);
            
            stats.objectCount = objects.size();
            stats.itemCount = items.size();
            stats.npcCount = npcs.size();
            
        } catch (Exception e) {
            logger.warn("Could not get cache stats: {}", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Cache statistics holder
     */
    public static class CacheStats {
        public int objectCount;
        public int itemCount;
        public int npcCount;
        
        @Override
        public String toString() {
            return String.format("Objects: %d, Items: %d, NPCs: %d", 
                               objectCount, itemCount, npcCount);
        }
    }
}
