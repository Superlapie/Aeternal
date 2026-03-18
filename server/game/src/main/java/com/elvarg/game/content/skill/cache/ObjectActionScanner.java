package com.elvarg.game.content.skill.cache;

import java.util.HashMap;
import java.util.Map;

import com.elvarg.game.definition.ObjectDefinition;

/**
 * Universal scanner for detecting skill-related objects from the cache.
 * Automatically registers objects based on their interaction types.
 * This system can be extended to support woodcutting, fishing, farming, etc.
 * 
 * @author Cache-driven Skill System
 */
public class ObjectActionScanner {
    
    // Map of action keywords to object IDs
    private static final Map<String, Map<Integer, String>> skillObjects = new HashMap<>();
    
    static {
        // Initialize skill action mappings
        skillObjects.put("Mine", new HashMap<>());
        skillObjects.put("Chop down", new HashMap<>());
        skillObjects.put("Net", new HashMap<>());
        skillObjects.put("Harpoon", new HashMap<>());
        skillObjects.put("Pick", new HashMap<>());
        skillObjects.put("Fish", new HashMap<>());
    }
    
    /**
     * Scans all ObjectDefinitions and registers skill objects by their actions
     */
    public static void initialize() {
        for (int i = 0; i < ObjectDefinition.totalObjects; i++) {
            ObjectDefinition def = ObjectDefinition.forId(i);
            
            if (def == null || def.getName() == null || def.interactions == null) {
                continue;
            }
            
            // Check each interaction for skill-related actions
            for (String action : def.interactions) {
                if (action != null) {
                    registerSkillObject(action, i, def.getName());
                }
            }
        }
        
        // Print registration summary
        System.out.println("ObjectActionScanner initialized:");
        for (Map.Entry<String, Map<Integer, String>> entry : skillObjects.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue().size() + " objects");
        }
    }
    
    /**
     * Registers an object for a specific skill action
     */
    private static void registerSkillObject(String action, int objectId, String objectName) {
        for (String skillAction : skillObjects.keySet()) {
            if (action.equalsIgnoreCase(skillAction)) {
                skillObjects.get(skillAction).put(objectId, objectName);
                break;
            }
        }
    }
    
    /**
     * Gets all objects for a specific skill action
     */
    public static Map<Integer, String> getObjectsByAction(String action) {
        return skillObjects.getOrDefault(action, new HashMap<>());
    }
    
    /**
     * Checks if an object has a specific action
     */
    public static boolean hasAction(int objectId, String action) {
        Map<Integer, String> objects = skillObjects.get(action);
        return objects != null && objects.containsKey(objectId);
    }
    
    /**
     * Gets the object name for a specific action
     */
    public static String getObjectName(int objectId, String action) {
        Map<Integer, String> objects = skillObjects.get(action);
        return objects != null ? objects.get(objectId) : null;
    }
    
    /**
     * Gets all registered skill objects
     */
    public static Map<String, Map<Integer, String>> getAllSkillObjects() {
        return new HashMap<>(skillObjects);
    }
    
    /**
     * Gets statistics about registered objects
     */
    public static void printStatistics() {
        System.out.println("=== Skill Object Statistics ===");
        int total = 0;
        
        for (Map.Entry<String, Map<Integer, String>> entry : skillObjects.entrySet()) {
            int count = entry.getValue().size();
            total += count;
            System.out.println(entry.getKey() + ": " + count + " objects");
        }
        
        System.out.println("Total skill objects: " + total);
        System.out.println("============================");
    }
}
