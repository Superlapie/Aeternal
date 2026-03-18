package com.cacheextractor;

import java.nio.file.Path;
import java.util.Set;

/**
 * Configuration for the cache extraction process.
 * Contains all settings and paths needed for extraction.
 */
public class ExtractionConfig {
    
    private final Path cachePath;
    private final Path outputPath;
    private final Set<String> exportTypes;
    private final boolean includeActions;
    private final boolean verbose;
    private final String format;
    
    public ExtractionConfig(Path cachePath, Path outputPath, Set<String> exportTypes, 
                           boolean includeActions, boolean verbose, String format) {
        this.cachePath = cachePath;
        this.outputPath = outputPath;
        this.exportTypes = exportTypes;
        this.includeActions = includeActions;
        this.verbose = verbose;
        this.format = format;
    }
    
    public Path getCachePath() {
        return cachePath;
    }
    
    public Path getOutputPath() {
        return outputPath;
    }
    
    public Set<String> getExportTypes() {
        return exportTypes;
    }
    
    public boolean shouldIncludeActions() {
        return includeActions;
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public String getFormat() {
        return format;
    }
    
    public boolean shouldExportObjects() {
        return exportTypes.contains("objects");
    }
    
    public boolean shouldExportItems() {
        return exportTypes.contains("items");
    }
    
    public boolean shouldExportNPCs() {
        return exportTypes.contains("npcs");
    }
    
    public boolean shouldExportAnimations() {
        return exportTypes.contains("animations");
    }
    
    public boolean shouldExportModels() {
        return exportTypes.contains("models");
    }
}
