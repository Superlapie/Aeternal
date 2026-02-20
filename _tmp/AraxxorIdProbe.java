import com.google.gson.Gson;
import java.nio.file.*;

public class AraxxorIdProbe {
  static class E { int id; }
  public static void main(String[] args) throws Exception {
    Path p = Paths.get("_ext/AraxxorAnimations.json");
    if (!Files.exists(p)) { System.out.println("missing"); return; }
    String json = new String(Files.readAllBytes(p));
    E[] arr = new Gson().fromJson(json, E[].class);
    int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE,low=0;
    for(E e:arr){ if(e==null) continue; if(e.id<min)min=e.id; if(e.id>max)max=e.id; if(e.id<9000)low++; }
    System.out.println("count="+arr.length+" min="+min+" max="+max+" low<9000="+low);
    for(E e:arr){ if(e!=null && e.id<9000) System.out.println("lowId="+e.id); }
  }
}
