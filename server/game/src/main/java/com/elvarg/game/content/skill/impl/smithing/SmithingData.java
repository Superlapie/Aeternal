package com.elvarg.game.content.skill.impl.smithing;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * OSRS accurate smithing data enum
 * Contains all items that can be smithed at anvils
 * 
 * @author Smithing System
 */
public enum SmithingData {
    
    // Bronze items (Base level 1)
    BRONZE_DAGGER(1205, 2349, 1, 1, 12.5, 1119, "Dagger"),
    BRONZE_AXE(1351, 2349, 1, 2, 12.5, 1083, "Axe"),
    BRONZE_MACE(1422, 2349, 1, 3, 12.5, 1085, "Mace"),
    BRONZE_MED_HELM(1139, 2349, 1, 4, 12.5, 1099, "Medium helm"),
    BRONZE_SWORD(1277, 2349, 1, 5, 12.5, 1087, "Sword"),
    BRONZE_DART_TIPS(819, 2349, 1, 5, 12.5, 1104, "Dart tips"),
    BRONZE_NAILS(4819, 2349, 1, 5, 12.5, 1098, "Nails"),
    BRONZE_ARROWTIPS(39, 2349, 1, 6, 12.5, 1106, "Arrowtips"),
    BRONZE_SCIMITAR(1321, 2349, 2, 6, 25.0, 1091, "Scimitar"),
    BRONZE_LONGSWORD(1291, 2349, 2, 7, 25.0, 1086, "Longsword"),
    BRONZE_CROSSBOW_LIMBS(9420, 2349, 1, 7, 12.5, 11459, "Crossbow limbs"),
    BRONZE_FULL_HELM(1155, 2349, 2, 8, 25.0, 1101, "Full helm"),
    BRONZE_THROWING_KNIVES(864, 2349, 1, 8, 12.5, 1107, "Throwing knives"),
    BRONZE_SQUARE_SHIELD(1173, 2349, 2, 9, 25.0, 1102, "Square shield"),
    BRONZE_WARHAMMER(1337, 2349, 3, 10, 37.5, 1089, "Warhammer"),
    BRONZE_BATTLEAXE(1375, 2349, 3, 11, 37.5, 1092, "Battleaxe"),
    BRONZE_CHAINBODY(1103, 2349, 3, 12, 37.5, 1093, "Chainbody"),
    BRONZE_KITESHIELD(1189, 2349, 3, 13, 37.5, 1103, "Kiteshield"),
    BRONZE_CLAWS(3095, 2349, 2, 14, 25.0, 8428, "Claws"),
    BRONZE_2H_SWORD(1307, 2349, 3, 15, 37.5, 1088, "2h sword"),
    BRONZE_PLATELEGS(1075, 2349, 3, 17, 37.5, 1099, "Platelegs"),
    BRONZE_PLATESKIRT(1087, 2349, 3, 17, 37.5, 1100, "Plateskirt"),
    BRONZE_PLATEBODY(1117, 2349, 5, 19, 62.5, 1101, "Platebody"),
    
    // Iron items (Base level 15)
    IRON_DAGGER(1203, 2351, 1, 15, 25.0, 1119, "Dagger"),
    IRON_AXE(1349, 2351, 1, 16, 25.0, 1083, "Axe"),
    IRON_MACE(1420, 2351, 1, 17, 25.0, 1085, "Mace"),
    IRON_MED_HELM(1137, 2351, 1, 18, 25.0, 1099, "Medium helm"),
    IRON_SWORD(1279, 2351, 1, 19, 25.0, 1087, "Sword"),
    IRON_DART_TIPS(820, 2351, 1, 19, 25.0, 1104, "Dart tips"),
    IRON_NAILS(4820, 2351, 1, 19, 25.0, 1098, "Nails"),
    IRON_ARROWTIPS(40, 2351, 1, 20, 25.0, 1106, "Arrowtips"),
    IRON_SCIMITAR(1323, 2351, 2, 20, 50.0, 1091, "Scimitar"),
    IRON_LONGSWORD(1293, 2351, 2, 21, 50.0, 1086, "Longsword"),
    IRON_CROSSBOW_LIMBS(9422, 2351, 1, 21, 25.0, 11459, "Crossbow limbs"),
    IRON_FULL_HELM(1153, 2351, 2, 22, 50.0, 1101, "Full helm"),
    IRON_THROWING_KNIVES(863, 2351, 1, 22, 25.0, 1107, "Throwing knives"),
    IRON_SQUARE_SHIELD(1175, 2351, 2, 23, 50.0, 1102, "Square shield"),
    IRON_WARHAMMER(1335, 2351, 3, 24, 75.0, 1089, "Warhammer"),
    IRON_BATTLEAXE(1363, 2351, 3, 25, 75.0, 1092, "Battleaxe"),
    IRON_CHAINBODY(1101, 2351, 3, 26, 75.0, 1093, "Chainbody"),
    IRON_KITESHIELD(1191, 2351, 3, 27, 75.0, 1103, "Kiteshield"),
    IRON_CLAWS(3096, 2351, 2, 28, 50.0, 8428, "Claws"),
    IRON_2H_SWORD(1309, 2351, 3, 29, 75.0, 1088, "2h sword"),
    IRON_PLATELEGS(1067, 2351, 3, 31, 75.0, 1099, "Platelegs"),
    IRON_PLATESKIRT(1081, 2351, 3, 31, 75.0, 1100, "Plateskirt"),
    IRON_PLATEBODY(1115, 2351, 5, 33, 125.0, 1101, "Platebody"),
    
    // Steel items (Base level 30)
    STEEL_DAGGER(1207, 2353, 1, 30, 37.5, 1119, "Dagger"),
    STEEL_AXE(1353, 2353, 1, 31, 37.5, 1083, "Axe"),
    STEEL_MACE(1424, 2353, 1, 32, 37.5, 1085, "Mace"),
    STEEL_MED_HELM(1141, 2353, 1, 33, 37.5, 1099, "Medium helm"),
    STEEL_SWORD(1281, 2353, 1, 34, 37.5, 1087, "Sword"),
    STEEL_DART_TIPS(821, 2353, 1, 34, 37.5, 1104, "Dart tips"),
    STEEL_NAILS(1539, 2353, 1, 34, 37.5, 1098, "Nails"),
    STEEL_ARROWTIPS(41, 2353, 1, 35, 37.5, 1106, "Arrowtips"),
    STEEL_SCIMITAR(1325, 2353, 2, 35, 75.0, 1091, "Scimitar"),
    STEEL_LONGSWORD(1295, 2353, 2, 36, 75.0, 1086, "Longsword"),
    STEEL_CROSSBOW_LIMBS(9424, 2353, 1, 36, 37.5, 11459, "Crossbow limbs"),
    STEEL_FULL_HELM(1157, 2353, 2, 37, 75.0, 1101, "Full helm"),
    STEEL_THROWING_KNIVES(865, 2353, 1, 37, 37.5, 1107, "Throwing knives"),
    STEEL_SQUARE_SHIELD(1177, 2353, 2, 38, 75.0, 1102, "Square shield"),
    STEEL_WARHAMMER(1339, 2353, 3, 39, 112.5, 1089, "Warhammer"),
    STEEL_BATTLEAXE(1365, 2353, 3, 40, 112.5, 1092, "Battleaxe"),
    STEEL_CHAINBODY(1105, 2353, 3, 41, 112.5, 1093, "Chainbody"),
    STEEL_KITESHIELD(1193, 2353, 3, 42, 112.5, 1103, "Kiteshield"),
    STEEL_CLAWS(3097, 2353, 2, 43, 75.0, 8428, "Claws"),
    STEEL_2H_SWORD(1311, 2353, 3, 44, 112.5, 1088, "2h sword"),
    STEEL_PLATELEGS(1069, 2353, 3, 46, 112.5, 1099, "Platelegs"),
    STEEL_PLATESKIRT(1083, 2353, 3, 46, 112.5, 1100, "Plateskirt"),
    STEEL_PLATEBODY(1119, 2353, 5, 48, 187.5, 1101, "Platebody"),
    
    // Mithril items (Base level 50)
    MITHRIL_DAGGER(1209, 2359, 1, 50, 50.0, 1119, "Dagger"),
    MITHRIL_AXE(1355, 2359, 1, 51, 50.0, 1083, "Axe"),
    MITHRIL_MACE(1428, 2359, 1, 52, 50.0, 1085, "Mace"),
    MITHRIL_MED_HELM(1143, 2359, 1, 53, 50.0, 1099, "Medium helm"),
    MITHRIL_SWORD(1285, 2359, 1, 54, 50.0, 1087, "Sword"),
    MITHRIL_DART_TIPS(822, 2359, 1, 54, 50.0, 1104, "Dart tips"),
    MITHRIL_NAILS(4822, 2359, 1, 54, 50.0, 1098, "Nails"),
    MITHRIL_ARROWTIPS(42, 2359, 1, 55, 50.0, 1106, "Arrowtips"),
    MITHRIL_SCIMITAR(1329, 2359, 2, 55, 100.0, 1091, "Scimitar"),
    MITHRIL_LONGSWORD(1299, 2359, 2, 56, 100.0, 1086, "Longsword"),
    MITHRIL_CROSSBOW_LIMBS(9426, 2359, 1, 56, 50.0, 11459, "Crossbow limbs"),
    MITHRIL_FULL_HELM(1159, 2359, 2, 57, 100.0, 1101, "Full helm"),
    MITHRIL_THROWING_KNIVES(866, 2359, 1, 57, 50.0, 1107, "Throwing knives"),
    MITHRIL_SQUARE_SHIELD(1181, 2359, 2, 58, 100.0, 1102, "Square shield"),
    MITHRIL_WARHAMMER(1343, 2359, 3, 59, 150.0, 1089, "Warhammer"),
    MITHRIL_BATTLEAXE(1369, 2359, 3, 60, 150.0, 1092, "Battleaxe"),
    MITHRIL_CHAINBODY(1109, 2359, 3, 61, 150.0, 1093, "Chainbody"),
    MITHRIL_KITESHIELD(1197, 2359, 3, 62, 150.0, 1103, "Kiteshield"),
    MITHRIL_CLAWS(3099, 2359, 2, 63, 100.0, 8428, "Claws"),
    MITHRIL_2H_SWORD(1315, 2359, 3, 64, 150.0, 1088, "2h sword"),
    MITHRIL_PLATELEGS(1071, 2359, 3, 66, 150.0, 1099, "Platelegs"),
    MITHRIL_PLATESKIRT(1085, 2359, 3, 66, 150.0, 1100, "Plateskirt"),
    MITHRIL_PLATEBODY(1121, 2359, 5, 68, 250.0, 1101, "Platebody"),
    
    // Adamant items (Base level 70)
    ADAMANT_DAGGER(1211, 2361, 1, 70, 62.5, 1119, "Dagger"),
    ADAMANT_AXE(1357, 2361, 1, 71, 62.5, 1083, "Axe"),
    ADAMANT_MACE(1430, 2361, 1, 72, 62.5, 1085, "Mace"),
    ADAMANT_MED_HELM(1145, 2361, 1, 73, 62.5, 1099, "Medium helm"),
    ADAMANT_SWORD(1287, 2361, 1, 74, 62.5, 1087, "Sword"),
    ADAMANT_DART_TIPS(823, 2361, 1, 74, 62.5, 1104, "Dart tips"),
    ADAMANT_NAILS(4823, 2361, 1, 74, 62.5, 1098, "Nails"),
    ADAMANT_ARROWTIPS(43, 2361, 1, 75, 62.5, 1106, "Arrowtips"),
    ADAMANT_SCIMITAR(1331, 2361, 2, 75, 125.0, 1091, "Scimitar"),
    ADAMANT_LONGSWORD(1301, 2361, 2, 76, 125.0, 1086, "Longsword"),
    ADAMANT_CROSSBOW_LIMBS(9428, 2361, 1, 76, 62.5, 11459, "Crossbow limbs"),
    ADAMANT_FULL_HELM(1161, 2361, 2, 77, 125.0, 1101, "Full helm"),
    ADAMANT_THROWING_KNIVES(867, 2361, 1, 77, 62.5, 1107, "Throwing knives"),
    ADAMANT_SQUARE_SHIELD(1183, 2361, 2, 78, 125.0, 1102, "Square shield"),
    ADAMANT_WARHAMMER(1345, 2361, 3, 79, 187.5, 1089, "Warhammer"),
    ADAMANT_BATTLEAXE(1371, 2361, 3, 80, 187.5, 1092, "Battleaxe"),
    ADAMANT_CHAINBODY(1111, 2361, 3, 81, 187.5, 1093, "Chainbody"),
    ADAMANT_KITESHIELD(1199, 2361, 3, 82, 187.5, 1103, "Kiteshield"),
    ADAMANT_CLAWS(3100, 2361, 2, 83, 125.0, 8428, "Claws"),
    ADAMANT_2H_SWORD(1317, 2361, 3, 84, 187.5, 1088, "2h sword"),
    ADAMANT_PLATELEGS(1073, 2361, 3, 86, 187.5, 1099, "Platelegs"),
    ADAMANT_PLATESKIRT(1091, 2361, 3, 86, 187.5, 1100, "Plateskirt"),
    ADAMANT_PLATEBODY(1123, 2361, 5, 88, 312.5, 1101, "Platebody"),
    
    // Rune items (Base level 85)
    RUNE_DAGGER(1213, 2363, 1, 85, 75.0, 1119, "Dagger"),
    RUNE_AXE(1359, 2363, 1, 86, 75.0, 1083, "Axe"),
    RUNE_MACE(1432, 2363, 1, 87, 75.0, 1085, "Mace"),
    RUNE_MED_HELM(1147, 2363, 1, 88, 75.0, 1099, "Medium helm"),
    RUNE_SWORD(1289, 2363, 1, 89, 75.0, 1087, "Sword"),
    RUNE_DART_TIPS(824, 2363, 1, 89, 75.0, 1104, "Dart tips"),
    RUNE_NAILS(4824, 2363, 1, 89, 75.0, 1098, "Nails"),
    RUNE_ARROWTIPS(44, 2363, 1, 90, 75.0, 1106, "Arrowtips"),
    RUNE_SCIMITAR(1333, 2363, 2, 90, 150.0, 1091, "Scimitar"),
    RUNE_LONGSWORD(1303, 2363, 2, 91, 150.0, 1086, "Longsword"),
    RUNE_CROSSBOW_LIMBS(9430, 2363, 1, 91, 75.0, 11459, "Crossbow limbs"),
    RUNE_FULL_HELM(1163, 2363, 2, 92, 150.0, 1101, "Full helm"),
    RUNE_THROWING_KNIVES(868, 2363, 1, 92, 75.0, 1107, "Throwing knives"),
    RUNE_SQUARE_SHIELD(1185, 2363, 2, 93, 150.0, 1102, "Square shield"),
    RUNE_WARHAMMER(1347, 2363, 3, 94, 225.0, 1089, "Warhammer"),
    RUNE_BATTLEAXE(1373, 2363, 3, 95, 225.0, 1092, "Battleaxe"),
    RUNE_CHAINBODY(1113, 2363, 3, 96, 225.0, 1093, "Chainbody"),
    RUNE_KITESHIELD(1201, 2363, 3, 97, 225.0, 1103, "Kiteshield"),
    RUNE_CLAWS(3101, 2363, 2, 98, 150.0, 8428, "Claws"),
    RUNE_2H_SWORD(1319, 2363, 3, 99, 225.0, 1088, "2h sword"),
    RUNE_PLATELEGS(1079, 2363, 3, 99, 225.0, 1099, "Platelegs"),
    RUNE_PLATESKIRT(1093, 2363, 3, 99, 225.0, 1100, "Plateskirt"),
    RUNE_PLATEBODY(1127, 2363, 5, 99, 375.0, 1101, "Platebody");
    
    private final int itemId;
    private final int barRequired;
    private final int barsUsed;
    private final int levelReq;
    private final double experience;
    private final int interfaceChildId;
    private final String name;
    
    // Static mapping for quick lookups
    private static final Map<Integer, SmithingData> ITEM_MAP = new HashMap<>();
    private static final Map<Integer, SmithingData> INTERFACE_MAP = new HashMap<>();
    
    static {
        for (SmithingData data : values()) {
            ITEM_MAP.put(data.itemId, data);
            INTERFACE_MAP.put(data.interfaceChildId, data);
        }
    }
    
    SmithingData(int itemId, int barRequired, int barsUsed, int levelReq, double experience, int interfaceChildId, String name) {
        this.itemId = itemId;
        this.barRequired = barRequired;
        this.barsUsed = barsUsed;
        this.levelReq = levelReq;
        this.experience = experience;
        this.interfaceChildId = interfaceChildId;
        this.name = name;
    }
    
    /**
     * Gets the item ID produced
     * @return item ID
     */
    public int getItemId() {
        return itemId;
    }
    
    /**
     * Gets the bar type required
     * @return bar ID
     */
    public int getBarRequired() {
        return barRequired;
    }
    
    /**
     * Gets the number of bars required per item
     * @return bars used
     */
    public int getBarsUsed() {
        return barsUsed;
    }
    
    /**
     * Gets the Smithing level requirement
     * @return level requirement
     */
    public int getLevelRequirement() {
        return levelReq;
    }
    
    /**
     * Gets the experience gained per item
     * @return experience amount
     */
    public double getExperience() {
        return experience;
    }
    
    /**
     * Gets the interface child ID for this item
     * @return interface child ID
     */
    public int getInterfaceChildId() {
        return interfaceChildId;
    }
    
    /**
     * Gets the item name
     * @return item name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the amount of items produced (for items like darts, nails, etc.)
     * @return amount produced
     */
    public int getAmountProduced() {
        switch (this) {
            case BRONZE_DART_TIPS:
            case IRON_DART_TIPS:
            case STEEL_DART_TIPS:
            case MITHRIL_DART_TIPS:
            case ADAMANT_DART_TIPS:
            case RUNE_DART_TIPS:
                return 10;
            case BRONZE_NAILS:
            case IRON_NAILS:
            case STEEL_NAILS:
            case MITHRIL_NAILS:
            case ADAMANT_NAILS:
            case RUNE_NAILS:
                return 15;
            case BRONZE_ARROWTIPS:
            case IRON_ARROWTIPS:
            case STEEL_ARROWTIPS:
            case MITHRIL_ARROWTIPS:
            case ADAMANT_ARROWTIPS:
            case RUNE_ARROWTIPS:
                return 15;
            case BRONZE_THROWING_KNIVES:
            case IRON_THROWING_KNIVES:
            case STEEL_THROWING_KNIVES:
            case MITHRIL_THROWING_KNIVES:
            case ADAMANT_THROWING_KNIVES:
            case RUNE_THROWING_KNIVES:
                return 5;
            default:
                return 1;
        }
    }
    
    /**
     * Checks if the player has the required bars to smith this item
     * @param player the player to check
     * @return true if player has required bars
     */
    public boolean hasRequiredBars(Player player) {
        return player.getInventory().getAmount(barRequired) >= barsUsed;
    }
    
    /**
     * Removes the required bars from player's inventory
     * @param player the player to remove bars from
     */
    public void removeRequiredBars(Player player) {
        player.getInventory().delete(new Item(barRequired, barsUsed));
    }
    
    /**
     * Gets the maximum number of items the player can smith with current bars
     * @param player the player to check
     * @return maximum items that can be smithed
     */
    public int getMaxItems(Player player) {
        int barAmount = player.getInventory().getAmount(barRequired);
        return barAmount / barsUsed;
    }
    
    /**
     * Gets SmithingData by item ID
     * @param itemId the item ID
     * @return SmithingData or null if not found
     */
    public static SmithingData forItemId(int itemId) {
        return ITEM_MAP.get(itemId);
    }
    
    /**
     * Gets SmithingData by interface child ID
     * @param childId the interface child ID
     * @return SmithingData or null if not found
     */
    public static SmithingData forChildId(int childId) {
        return INTERFACE_MAP.get(childId);
    }
    
    /**
     * Gets all smithing items for a specific bar type
     * @param barId the bar ID
     * @return array of SmithingData for that bar
     */
    public static SmithingData[] forBarId(int barId) {
        java.util.List<SmithingData> result = new java.util.ArrayList<>();
        for (SmithingData data : values()) {
            if (data.barRequired == barId) {
                result.add(data);
            }
        }
        return result.toArray(new SmithingData[0]);
    }
}
