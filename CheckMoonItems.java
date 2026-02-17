import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// Simple ItemDefinition class for testing
class TestItemDefinition {
    private int id;
    private String name;
    private String examine;
    
    public int getId() { return id; }
    public String getName() { return name; }
    public String getExamine() { return examine; }
}

public class CheckMoonItems {
    public static void main(String[] args) {
        try {
            System.out.println("Checking moon items in JSON...");
            
            // Read JSON file manually and look for moon items
            FileReader reader = new FileReader("server/data/definitions/items.json");
            StringBuilder content = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1) {
                content.append((char) ch);
            }
            reader.close();
            
            String json = content.toString();
            
            // Count moon items in JSON
            int moonCount = 0;
            String[] lines = json.split("\n");
            boolean inMoonItem = false;
            String currentMoonItem = "";
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                
                if (line.contains("\"id\": 3100")) {
                    inMoonItem = true;
                    currentMoonItem = "ID: " + line.split(":")[1].trim() + " - ";
                }
                
                if (inMoonItem && line.contains("\"name\":")) {
                    String name = line.split(":")[1].trim();
                    currentMoonItem += name;
                    if (name.toLowerCase().contains("moon")) {
                        moonCount++;
                        System.out.println("Found moon item: " + currentMoonItem);
                    }
                    inMoonItem = false;
                }
            }
            
            System.out.println("Total moon items found: " + moonCount);
            
            // Check if JSON is well-formed at the end
            System.out.println("JSON ends with: " + json.substring(json.length() - 50));
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
