import com.google.gson.Gson;
import java.io.FileReader;
import java.util.Map;

public class TestItemLoading {
    public static class ItemDefinition {
        public int id;
        public String name;
        public String examine;
        
        public int getId() { return id; }
        public String getName() { return name; }
    }
    
    public static void main(String[] args) {
        try {
            System.out.println("Testing item loading...");
            
            FileReader reader = new FileReader("server/data/definitions/items.json");
            ItemDefinition[] defs = new Gson().fromJson(reader, ItemDefinition[].class);
            reader.close();
            
            System.out.println("Total items parsed: " + defs.length);
            
            // Check for moon items
            int moonCount = 0;
            for (ItemDefinition def : defs) {
                if (def != null && def.getName() != null && def.getName().toLowerCase().contains("moon")) {
                    System.out.println("Found moon item: " + def.getName() + " (ID: " + def.getId() + ")");
                    moonCount++;
                }
            }
            
            System.out.println("Total moon items found: " + moonCount);
            
            // Check specific moon item IDs
            for (int id = 31000; id <= 31011; id++) {
                boolean found = false;
                for (ItemDefinition def : defs) {
                    if (def != null && def.getId() == id) {
                        System.out.println("Item " + id + ": " + def.getName() + " - Found!");
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.out.println("Item " + id + ": NOT FOUND");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
