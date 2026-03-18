package com.elvarg.game.content.skill.impl.smithing;

import com.elvarg.game.model.Item;

/**
 * Represents individual smithable items with their requirements
 * 
 * @author Smithing System
 */
public enum SmithingItemData {
    
    // Bronze items (Bar ID: 2349)
    BRONZE_DAGGER(1205, 2349, 1, 12.5, 1, new int[]{110, 111, 112, 113, 114, 115, 116, 117, 118, 119}),
    BRONZE_AXE(1351, 2349, 1, 12.5, 2, new int[]{110, 111, 112, 113, 114, 115, 116, 117, 118, 119}),
    BRONZE_CHAIN_MAIL(1106, 2349, 1, 12.5, 3, new int[]{110, 111, 112, 113, 114, 115, 116, 117, 118, 119}),
    BRONZE_MED_HELM(1143, 2349, 1, 12.5, 4, new int[]{110, 111, 112, 113, 114, 115, 116, 117, 118, 119}),
    BRONZE_SWORD(1277, 2349, 1, 12.5, 5, new int[]{110, 111, 112, 113, 114, 115, 116, 117, 118, 119}),
    BRONZE_SCIMITAR(1321, 2349, 1, 12.5, 6, new int[]{110, 111, 112, 113, 114, 115, 116, 117, 118, 119}),
    BRONZE_LONGSWORD(1291, 2349, 1, 12.5, 7, new int[]{110, 111, 112, 113, 114, 115, 116, 117, 118, 119}),
    BRONZE_2H_SWORD(1307, 2349, 1, 12.5, 8, new int[]{110, 111, 112, 113, 114, 115, 116, 117, 118, 119}),
    BRONZE_SQ_SHIELD(1173, 2349, 1, 12.5, 9, new int[]{110, 111, 112, 113, 114, 115, 116, 117, 118, 119}),
    BRONZE_PLATEBODY(1117, 2349, 1, 12.5, 10, new int[]{110, 111, 112, 113, 114, 115, 116, 117, 118, 119}),
    
    // Iron items (Bar ID: 2351)
    IRON_DAGGER(1203, 2351, 1, 25.0, 15, new int[]{120, 121, 122, 123, 124, 125, 126, 127, 128, 129}),
    IRON_AXE(1349, 2351, 1, 25.0, 16, new int[]{120, 121, 122, 123, 124, 125, 126, 127, 128, 129}),
    IRON_CHAIN_MAIL(1101, 2351, 1, 25.0, 17, new int[]{120, 121, 122, 123, 124, 125, 126, 127, 128, 129}),
    IRON_MED_HELM(1141, 2351, 1, 25.0, 18, new int[]{120, 121, 122, 123, 124, 125, 126, 127, 128, 129}),
    IRON_SWORD(1279, 2351, 1, 25.0, 19, new int[]{120, 121, 122, 123, 124, 125, 126, 127, 128, 129}),
    IRON_SCIMITAR(1323, 2351, 1, 25.0, 20, new int[]{120, 121, 122, 123, 124, 125, 126, 127, 128, 129}),
    IRON_LONGSWORD(1293, 2351, 1, 25.0, 21, new int[]{120, 121, 122, 123, 124, 125, 126, 127, 128, 129}),
    IRON_2H_SWORD(1309, 2351, 1, 25.0, 22, new int[]{120, 121, 122, 123, 124, 125, 126, 127, 128, 129}),
    IRON_SQ_SHIELD(1175, 2351, 1, 25.0, 23, new int[]{120, 121, 122, 123, 124, 125, 126, 127, 128, 129}),
    IRON_PLATEBODY(1115, 2351, 1, 25.0, 24, new int[]{120, 121, 122, 123, 124, 125, 126, 127, 128, 129}),
    
    // Steel items (Bar ID: 2353)
    STEEL_DAGGER(1207, 2353, 1, 37.5, 30, new int[]{130, 131, 132, 133, 134, 135, 136, 137, 138, 139}),
    STEEL_AXE(1353, 2353, 1, 37.5, 31, new int[]{130, 131, 132, 133, 134, 135, 136, 137, 138, 139}),
    STEEL_CHAIN_MAIL(1103, 2353, 1, 37.5, 32, new int[]{130, 131, 132, 133, 134, 135, 136, 137, 138, 139}),
    STEEL_MED_HELM(1145, 2353, 1, 37.5, 33, new int[]{130, 131, 132, 133, 134, 135, 136, 137, 138, 139}),
    STEEL_SWORD(1281, 2353, 1, 37.5, 34, new int[]{130, 131, 132, 133, 134, 135, 136, 137, 138, 139}),
    STEEL_SCIMITAR(1325, 2353, 1, 37.5, 35, new int[]{130, 131, 132, 133, 134, 135, 136, 137, 138, 139}),
    STEEL_LONGSWORD(1295, 2353, 1, 37.5, 36, new int[]{130, 131, 132, 133, 134, 135, 136, 137, 138, 139}),
    STEEL_2H_SWORD(1311, 2353, 1, 37.5, 37, new int[]{130, 131, 132, 133, 134, 135, 136, 137, 138, 139}),
    STEEL_SQ_SHIELD(1177, 2353, 1, 37.5, 38, new int[]{130, 131, 132, 133, 134, 135, 136, 137, 138, 139}),
    STEEL_PLATEBODY(1119, 2353, 1, 37.5, 39, new int[]{130, 131, 132, 133, 134, 135, 136, 137, 138, 139}),
    
    // Mithril items (Bar ID: 2359)
    MITHRIL_DAGGER(1209, 2359, 1, 50.0, 50, new int[]{140, 141, 142, 143, 144, 145, 146, 147, 148, 149}),
    MITHRIL_AXE(1355, 2359, 1, 50.0, 51, new int[]{140, 141, 142, 143, 144, 145, 146, 147, 148, 149}),
    MITHRIL_CHAIN_MAIL(1105, 2359, 1, 50.0, 52, new int[]{140, 141, 142, 143, 144, 145, 146, 147, 148, 149}),
    MITHRIL_MED_HELM(1147, 2359, 1, 50.0, 53, new int[]{140, 141, 142, 143, 144, 145, 146, 147, 148, 149}),
    MITHRIL_SWORD(1285, 2359, 1, 50.0, 54, new int[]{140, 141, 142, 143, 144, 145, 146, 147, 148, 149}),
    MITHRIL_SCIMITAR(1329, 2359, 1, 50.0, 55, new int[]{140, 141, 142, 143, 144, 145, 146, 147, 148, 149}),
    MITHRIL_LONGSWORD(1297, 2359, 1, 50.0, 56, new int[]{140, 141, 142, 143, 144, 145, 146, 147, 148, 149}),
    MITHRIL_2H_SWORD(1313, 2359, 1, 50.0, 57, new int[]{140, 141, 142, 143, 144, 145, 146, 147, 148, 149}),
    MITHRIL_SQ_SHIELD(1179, 2359, 1, 50.0, 58, new int[]{140, 141, 142, 143, 144, 145, 146, 147, 148, 149}),
    MITHRIL_PLATEBODY(1121, 2359, 1, 50.0, 59, new int[]{140, 141, 142, 143, 144, 145, 146, 147, 148, 149}),
    
    // Adamant items (Bar ID: 2361)
    ADAMANT_DAGGER(1211, 2361, 1, 62.5, 70, new int[]{150, 151, 152, 153, 154, 155, 156, 157, 158, 159}),
    ADAMANT_AXE(1357, 2361, 1, 62.5, 71, new int[]{150, 151, 152, 153, 154, 155, 156, 157, 158, 159}),
    ADAMANT_CHAIN_MAIL(1109, 2361, 1, 62.5, 72, new int[]{150, 151, 152, 153, 154, 155, 156, 157, 158, 159}),
    ADAMANT_MED_HELM(1149, 2361, 1, 62.5, 73, new int[]{150, 151, 152, 153, 154, 155, 156, 157, 158, 159}),
    ADAMANT_SWORD(1287, 2361, 1, 62.5, 74, new int[]{150, 151, 152, 153, 154, 155, 156, 157, 158, 159}),
    ADAMANT_SCIMITAR(1331, 2361, 1, 62.5, 75, new int[]{150, 151, 152, 153, 154, 155, 156, 157, 158, 159}),
    ADAMANT_LONGSWORD(1299, 2361, 1, 62.5, 76, new int[]{150, 151, 152, 153, 154, 155, 156, 157, 158, 159}),
    ADAMANT_2H_SWORD(1315, 2361, 1, 62.5, 77, new int[]{150, 151, 152, 153, 154, 155, 156, 157, 158, 159}),
    ADAMANT_SQ_SHIELD(1181, 2361, 1, 62.5, 78, new int[]{150, 151, 152, 153, 154, 155, 156, 157, 158, 159}),
    ADAMANT_PLATEBODY(1123, 2361, 1, 62.5, 79, new int[]{150, 151, 152, 153, 154, 155, 156, 157, 158, 159}),
    
    // Runite items (Bar ID: 2363)
    RUNITE_DAGGER(1213, 2363, 1, 75.0, 85, new int[]{160, 161, 162, 163, 164, 165, 166, 167, 168, 169}),
    RUNITE_AXE(1359, 2363, 1, 75.0, 86, new int[]{160, 161, 162, 163, 164, 165, 166, 167, 168, 169}),
    RUNITE_CHAIN_MAIL(1111, 2363, 1, 75.0, 87, new int[]{160, 161, 162, 163, 164, 165, 166, 167, 168, 169}),
    RUNITE_MED_HELM(1151, 2363, 1, 75.0, 88, new int[]{160, 161, 162, 163, 164, 165, 166, 167, 168, 169}),
    RUNITE_SWORD(1289, 2363, 1, 75.0, 89, new int[]{160, 161, 162, 163, 164, 165, 166, 167, 168, 169}),
    RUNITE_SCIMITAR(1333, 2363, 1, 75.0, 90, new int[]{160, 161, 162, 163, 164, 165, 166, 167, 168, 169}),
    RUNITE_LONGSWORD(1301, 2363, 1, 75.0, 91, new int[]{160, 161, 162, 163, 164, 165, 166, 167, 168, 169}),
    RUNITE_2H_SWORD(1317, 2363, 1, 75.0, 92, new int[]{160, 161, 162, 163, 164, 165, 166, 167, 168, 169}),
    RUNITE_SQ_SHIELD(1183, 2363, 1, 75.0, 93, new int[]{160, 161, 162, 163, 164, 165, 166, 167, 168, 169}),
    RUNITE_PLATEBODY(1125, 2363, 1, 75.0, 94, new int[]{160, 161, 162, 163, 164, 165, 166, 167, 168, 169});
    
    private final int itemId;
    private final int barId;
    private final int barsRequired;
    private final double experience;
    private final int levelRequirement;
    private final int[] interfaceChildIds;
    
    SmithingItemData(int itemId, int barId, int barsRequired, double experience, int levelRequirement, int[] interfaceChildIds) {
        this.itemId = itemId;
        this.barId = barId;
        this.barsRequired = barsRequired;
        this.experience = experience;
        this.levelRequirement = levelRequirement;
        this.interfaceChildIds = interfaceChildIds;
    }
    
    public int getItemId() {
        return itemId;
    }
    
    public int getBarId() {
        return barId;
    }
    
    public int getBarsRequired() {
        return barsRequired;
    }
    
    public double getExperience() {
        return experience;
    }
    
    public int getLevelRequirement() {
        return levelRequirement;
    }
    
    public int[] getInterfaceChildIds() {
        return interfaceChildIds;
    }
    
    /**
     * Gets the SmithingItemData for a specific item ID
     * @param itemId The item ID
     * @return The SmithingItemData or null if not found
     */
    public static SmithingItemData forItemId(int itemId) {
        for (SmithingItemData item : values()) {
            if (item.itemId == itemId) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * Gets the SmithingItemData for a specific interface child ID
     * @param childId The interface child ID
     * @return The SmithingItemData or null if not found
     */
    public static SmithingItemData forChildId(int childId) {
        for (SmithingItemData item : values()) {
            for (int id : item.interfaceChildIds) {
                if (id == childId) {
                    return item;
                }
            }
        }
        return null;
    }
    
    /**
     * Gets all smithable items for a specific bar type
     * @param barId The bar ID
     * @return Array of smithable items
     */
    public static SmithingItemData[] forBarId(int barId) {
        java.util.List<SmithingItemData> items = new java.util.ArrayList<>();
        for (SmithingItemData item : values()) {
            if (item.barId == barId) {
                items.add(item);
            }
        }
        return items.toArray(new SmithingItemData[0]);
    }
    
    /**
     * Checks if the player has the required bars to smith this item
     * @param player The player to check
     * @return true if player has required bars
     */
    public boolean hasRequiredBars(com.elvarg.game.entity.impl.player.Player player) {
        return player.getInventory().getAmount(barId) >= barsRequired;
    }
    
    /**
     * Removes the required bars from player's inventory
     * @param player The player to remove bars from
     */
    public void removeRequiredBars(com.elvarg.game.entity.impl.player.Player player) {
        player.getInventory().delete(barId, barsRequired);
    }
    
    /**
     * Gets the total amount of items that can be made with current inventory
     * @param player The player to check
     * @return Maximum items that can be made
     */
    public int getMaxItems(com.elvarg.game.entity.impl.player.Player player) {
        int playerBars = player.getInventory().getAmount(barId);
        return playerBars / barsRequired;
    }
}
