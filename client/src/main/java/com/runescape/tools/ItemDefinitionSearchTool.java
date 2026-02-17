package com.runescape.tools;

import com.runescape.cache.FileStore;
import com.runescape.cache.def.ItemDefinition;
import com.runescape.io.Buffer;
import com.runescape.util.FileUtils;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Searches item definitions for moon equipment
 */
public final class ItemDefinitionSearchTool {

    private ItemDefinitionSearchTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java com.runescape.tools.ItemDefinitionSearchTool <cacheDir> <searchTerm>");
            return;
        }

        String cacheDir = args[0];
        String searchTerm = args[1].toLowerCase();

        // Initialize ItemDefinition cache
        File datFile = new File(cacheDir + "main_file_cache.dat");
        if (!datFile.exists()) {
            datFile = new File(cacheDir + "main_file_cache.dat2");
        }

        File idxFile = new File(cacheDir + "main_file_cache.idx2");
        if (!datFile.exists() || !idxFile.exists()) {
            throw new IllegalStateException("Cache files not found");
        }

        // Load item definitions
        try (RandomAccessFile datRaf = new RandomAccessFile(datFile, "r");
             RandomAccessFile idxRaf = new RandomAccessFile(idxFile, "r")) {

            FileStore store = new FileStore(datRaf, idxRaf, 3);
            
            // Get total items
            Buffer idxStream = new Buffer(FileUtils.readFile(cacheDir + "main_file_cache.idx2"));
            int totalItems = idxStream.readUShort();
            
            System.out.println("Searching " + totalItems + " items for: " + searchTerm);
            System.out.println();
            
            int found = 0;
            for (int itemId = 0; itemId < totalItems; itemId++) {
                byte[] data = store.decompress(itemId);
                if (data != null && data.length > 0) {
                    try {
                        Buffer itemStream = new Buffer(data);
                        ItemDefinition itemDef = ItemDefinition.lookup(itemId);
                        
                        if (itemDef.name != null && itemDef.name.toLowerCase().contains(searchTerm)) {
                            found++;
                            System.out.println("Item " + itemId + ": " + itemDef.name);
                            System.out.println("  Inventory model: " + itemDef.inventory_model);
                            System.out.println("  Equipped model (male): " + itemDef.equipped_model_male_1);
                            System.out.println("  Equipped model (male 2): " + itemDef.equipped_model_male_2);
                            System.out.println("  Zoom: " + itemDef.modelZoom);
                            System.out.println("  Actions: " + java.util.Arrays.toString(itemDef.actions));
                            System.out.println();
                        }
                    } catch (Exception e) {
                        // Skip invalid items
                    }
                }
            }
            
            System.out.println("Found " + found + " items containing '" + searchTerm + "'");
        }
    }
}
