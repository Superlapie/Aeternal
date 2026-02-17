package com.runescape.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Searches flat cache format for moon equipment items
 */
public final class FlatCacheItemSearchTool {

    private FlatCacheItemSearchTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java com.runescape.tools.FlatCacheItemSearchTool <cacheDir> <searchTerm>");
            return;
        }

        String cacheDir = args[0];
        String searchTerm = args[1].toLowerCase();

        // Search for item definitions in cache directory 2 (items)
        File itemCacheDir = new File(cacheDir + "2");
        if (!itemCacheDir.exists()) {
            System.out.println("Item cache directory not found: " + itemCacheDir.getAbsolutePath());
            return;
        }

        System.out.println("Searching flat cache for items containing: " + searchTerm);
        System.out.println();

        int found = 0;
        File[] itemFiles = itemCacheDir.listFiles();
        if (itemFiles != null) {
            for (File itemFile : itemFiles) {
                if (!itemFile.getName().endsWith(".dat")) {
                    continue;
                }

                try {
                    int itemId = Integer.parseInt(itemFile.getName().replace(".dat", ""));
                    byte[] data = readFile(itemFile);
                    
                    if (data != null && data.length > 0) {
                        String itemName = extractItemName(data);
                        if (itemName != null && itemName.toLowerCase().contains(searchTerm)) {
                            found++;
                            System.out.println("Item " + itemId + ": " + itemName);
                            
                            // Extract model information
                            int inventoryModel = extractInventoryModel(data);
                            int equippedModelMale = extractEquippedModelMale(data);
                            
                            System.out.println("  Inventory model: " + inventoryModel);
                            System.out.println("  Equipped model (male): " + equippedModelMale);
                            System.out.println();
                        }
                    }
                } catch (Exception e) {
                    // Skip invalid files
                }
            }
        }

        System.out.println("Found " + found + " items containing '" + searchTerm + "'");
    }

    private static String extractItemName(byte[] data) {
        if (data.length < 10) return null;
        
        try {
            // Skip first few bytes to find name string
            int pos = 0;
            while (pos < data.length - 1) {
                if (data[pos] == 0 && data[pos + 1] > 0) {
                    pos++;
                    break;
                }
                pos++;
            }
            
            if (pos >= data.length) return null;
            
            // Read string until null terminator
            StringBuilder name = new StringBuilder();
            while (pos < data.length && data[pos] != 0) {
                name.append((char) data[pos]);
                pos++;
            }
            
            String result = name.toString();
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            return null;
        }
    }

    private static int extractInventoryModel(byte[] data) {
        try {
            // Look for inventory model ID (usually near the beginning)
            if (data.length >= 6) {
                // Try different positions where inventory model might be
                for (int i = 0; i < Math.min(10, data.length - 1); i++) {
                    if (data[i] != 0) {
                        int modelId = ByteBuffer.wrap(data, i, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
                        if (modelId > 0 && modelId < 100000) {
                            return modelId;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return -1;
    }

    private static int extractEquippedModelMale(byte[] data) {
        try {
            // Look for equipped model ID (usually after inventory model)
            if (data.length >= 8) {
                for (int i = 2; i < Math.min(20, data.length - 1); i++) {
                    if (data[i] != 0) {
                        int modelId = ByteBuffer.wrap(data, i, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
                        if (modelId > 0 && modelId < 100000) {
                            return modelId;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return -1;
    }

    private static byte[] readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        }
    }
}
