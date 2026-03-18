package com.cacheextractor.utils;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for reporting extraction progress.
 * Provides progress bars and status updates for long-running operations.
 */
public class ProgressReporter {
    
    private static final Logger logger = LoggerFactory.getLogger(ProgressReporter.class);
    
    /**
     * Creates a progress bar for extraction operations
     */
    public static ProgressBar createProgressBar(String taskName, int max) {
        return new ProgressBarBuilder()
            .setTaskName(taskName)
            .setInitialMax(max)
            .setUpdateIntervalMillis(100)
            .setStyle(ProgressBarStyle.UNICODE_BLOCK)
            .build();
    }
    
    /**
     * Logs progress update
     */
    public static void logProgress(String taskName, int current, int total) {
        double percent = (double) current / total * 100;
        logger.info("{}: {}/{} ({:.1f}%)", taskName, current, total, percent);
    }
    
    /**
     * Logs task completion
     */
    public static void logCompletion(String taskName, int processed, long timeMs) {
        double seconds = timeMs / 1000.0;
        double rate = processed / seconds;
        logger.info("{} completed: {} items in {:.2f}s ({:.1f} items/sec)", 
                   taskName, processed, seconds, rate);
    }
}
