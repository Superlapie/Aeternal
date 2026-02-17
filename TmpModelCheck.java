import com.runescape.Client;
import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.anim.Animation;
import com.runescape.cache.anim.Frame;
import com.runescape.cache.anim.Graphic;
import com.runescape.entity.model.Model;

import java.io.RandomAccessFile;
import java.lang.reflect.Field;

public class TmpModelCheck {
  public static void main(String[] args) throws Exception {
    Client c = new Client();
    Client.instance = c;

    String cacheDir = "client/Cache/";
    RandomAccessFile dat = new RandomAccessFile(cacheDir + "main_file_cache.dat", "r");
    for (int i = 0; i < c.indices.length; i++) {
      c.indices[i] = new FileStore(dat, new RandomAccessFile(cacheDir + "main_file_cache.idx" + i, "r"), i + 1);
    }

    Model.init();
    Frame.animationlist = new Frame[20000][0];

    FileArchive cfg = new FileArchive(c.indices[0].decompress(2));
    Animation.init(cfg);
    Graphic.init(cfg);

    int[] mids = {51234,51237,35394,52263};
    for (int mid : mids) {
      Model m = Model.getModel(mid);
      if (m == null) {
        System.out.println("model " + mid + " -> null");
        continue;
      }
      Field nv = Model.class.getDeclaredField("numVertices"); nv.setAccessible(true);
      Field nt = Model.class.getDeclaredField("numTriangles"); nt.setAccessible(true);
      int v = (Integer) nv.get(m);
      int t = (Integer) nt.get(m);
      System.out.println("model "+mid+" vertices="+v+" triangles="+t);
    }

    int[] gfxIds = {2709,2710,2712};
    for (int gid : gfxIds) {
      Graphic g = Graphic.cache[gid];
      System.out.println("gfx " + gid + " animSeq=" + (g.animationSequence == null ? -1 : 1) + " model=" + (g.getModel()==null?"null":"ok"));
    }
  }
}
