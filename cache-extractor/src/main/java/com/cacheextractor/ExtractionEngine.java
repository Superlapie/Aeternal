package com.cacheextractor;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cacheextractor.exporters.*;
import com.cacheextractor.scanners.ObjectActionScanner;

import net.openrs.cache.Cache;

/**
 * Main extraction engine that coordinates the cache extraction process.
 * Manages exporters and handles the overall extraction workflow.
 */
public class ExtractionEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(ExtractionEngine.class);
    
    private final ExtractionConfig config;
    private final ExecutorService executor;
    
    public ExtractionEngine(ExtractionConfig config) {
        this.config = config;
        this.executor = Executors.newFixedThreadPool(4);
    }
    
    /**
     * Executes the complete extraction process
     */
    public boolean extract() {
        try {
            // Load cache
            Cache cache = CacheLoader.loadCache(config.getCachePath());
            
            // Print cache statistics
            CacheLoader.CacheStats stats = CacheLoader.getCacheStats(cache);
            logger.info("Cache statistics: {}", stats);
            
            // Create exporters
            ObjectExporter objectExporter = new ObjectExporter(config);
            ItemExporter itemExporter = new ItemExporter(config);
            NPCExporter npcExporter = new NPCExporter(config);
            AnimationExporter animationExporter = new AnimationExporter(config);
            ModelExporter modelExporter = new ModelExporter(config);
            
            // Execute extractions
            boolean success = true;
            
            if (config.shouldExportObjects()) {
                success &= objectExporter.export(cache);
            }
            
            if (config.shouldExportItems()) {
                success &= itemExporter.export(cache);
            }
            
            if (config.shouldExportNPCs()) {
                success &= npcExporter.export(cache);
            }
            
            if (config.shouldExportAnimations()) {
                success &= animationExporter.export(cache);
            }
            
            if (config.shouldExportModels()) {
                success &= modelExporter.export(cache);
            }
            
            // Extract object actions if requested
            if (config.shouldIncludeActions()) {
                ObjectActionScanner scanner = new ObjectActionScanner(config);
                success &= scanner.scanAndExport(cache);
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Extraction failed: {}", e.getMessage(), e);
            return false;
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * Executes extraction asynchronously
     */
    public CompletableFuture<Boolean> extractAsync() {
        return CompletableFuture.supplyAsync(this::extract, executor);
    }
}
