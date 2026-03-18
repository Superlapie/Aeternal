package com.cacheextractor;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the OSRS cache extraction tool.
 * Extracts cache definitions to JSON format for RSPS development.
 */
@Command(
    name = "cache-extractor",
    mixinStandardHelpOptions = true,
    version = "Cache Extractor 1.0.0",
    description = "Extracts OSRS cache data to JSON format for RSPS development"
)
public class CacheExtractor implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheExtractor.class);
    
    @Option(
        names = {"-c", "--cache"},
        description = "Cache directory path (auto-discovered if not specified)",
        paramLabel = "PATH"
    )
    private File cachePath;
    
    @Option(
        names = {"-o", "--output"},
        description = "Output directory path (default: ./export)",
        paramLabel = "PATH",
        defaultValue = "./export"
    )
    private File outputPath;
    
    @Option(
        names = {"-t", "--types"},
        description = "Export types (comma-separated): objects,items,npcs,animations,models",
        paramLabel = "TYPES",
        defaultValue = "objects,items,npcs,animations,models"
    )
    private String exportTypes;
    
    @Option(
        names = {"-a", "--actions"},
        description = "Include object actions scan",
        defaultValue = "true"
    )
    private boolean includeActions;
    
    @Option(
        names = {"-v", "--verbose"},
        description = "Verbose output"
    )
    private boolean verbose;
    
    @Option(
        names = {"-f", "--format"},
        description = "Output format (json, csv)",
        paramLabel = "FORMAT",
        defaultValue = "json"
    )
    private String format;
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CacheExtractor()).execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() throws Exception {
        logger.info("Starting OSRS Cache Extractor v1.0.0");
        
        try {
            // Validate and setup paths
            Path cacheDir = validateCachePath();
            Path outputDir = setupOutputDirectory();
            
            // Parse export types
            Set<String> types = parseExportTypes();
            
            // Create configuration
            ExtractionConfig config = new ExtractionConfig(
                cacheDir, outputDir, types, includeActions, verbose, format
            );
            
            // Execute extraction
            ExtractionEngine engine = new ExtractionEngine(config);
            boolean success = engine.extract();
            
            if (success) {
                logger.info("Extraction completed successfully!");
                logger.info("Output directory: {}", outputDir.toAbsolutePath());
                return 0;
            } else {
                logger.error("Extraction failed!");
                return 1;
            }
            
        } catch (Exception e) {
            logger.error("Extraction error: {}", e.getMessage(), e);
            return 1;
        }
    }
    
    /**
     * Validates and returns the cache directory path
     */
    private Path validateCachePath() throws Exception {
        if (cachePath != null) {
            if (!cachePath.exists()) {
                throw new Exception("Cache directory does not exist: " + cachePath);
            }
            if (!cachePath.isDirectory()) {
                throw new Exception("Cache path is not a directory: " + cachePath);
            }
            return cachePath.toPath();
        }
        
        // Auto-discover cache
        Path autoPath = CacheLoader.discoverCachePath();
        if (autoPath == null) {
            throw new Exception("Could not auto-discover cache path. Please specify with -c option.");
        }
        
        logger.info("Auto-discovered cache path: {}", autoPath);
        return autoPath;
    }
    
    /**
     * Sets up and returns the output directory
     */
    private Path setupOutputDirectory() throws Exception {
        Path outputDir = outputPath.toPath().toAbsolutePath();
        
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            logger.info("Created output directory: {}", outputDir);
        }
        
        return outputDir;
    }
    
    /**
     * Parses export types from string
     */
    private Set<String> parseExportTypes() {
        Set<String> types = new HashSet<>();
        String[] parts = exportTypes.split(",");
        
        for (String part : parts) {
            String type = part.trim().toLowerCase();
            if (isValidExportType(type)) {
                types.add(type);
            } else {
                logger.warn("Invalid export type: {} (valid: objects,items,npcs,animations,models)", type);
            }
        }
        
        if (types.isEmpty()) {
            throw new IllegalArgumentException("No valid export types specified");
        }
        
        logger.info("Export types: {}", String.join(", ", types));
        return types;
    }
    
    /**
     * Checks if export type is valid
     */
    private boolean isValidExportType(String type) {
        return type.equals("objects") || type.equals("items") || 
               type.equals("npcs") || type.equals("animations") || 
               type.equals("models");
    }
}
