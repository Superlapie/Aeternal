import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.def.ObjectDefinition;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class ObjRuntimeProbe {
  public static void main(String[] args) throws Exception {
    String cacheDir = args.length>0 ? args[0] : "client/Cache";
    File dat = new File(cacheDir, "main_file_cache.dat");
    if(!dat.exists()) dat = new File(cacheDir, "main_file_cache.dat2");
    File idx0 = new File(cacheDir, "main_file_cache.idx0");
    try(RandomAccessFile datRaf = new RandomAccessFile(dat,"r"); RandomAccessFile idx0Raf = new RandomAccessFile(idx0,"r")) {
      FileStore store0 = new FileStore(datRaf, idx0Raf, 1);
      byte[] config = store0.decompress(2);
      FileArchive archive = new FileArchive(config);
      ObjectDefinition.init(archive);
    }
    int[] ids = {7454,7455,7456,7457,7486,7492,7460,9720,14859,11360,11361,11364,11366,11370,11372};
    for(int id: ids){
      ObjectDefinition d = ObjectDefinition.lookup(id);
      System.out.println("id="+id+" name="+d.name+" actions="+Arrays.toString(d.interactions)+" modelIds="+(d.modelIds==null?"null":Arrays.toString(d.modelIds))+" modelTypes="+(d.modelTypes==null?"null":Arrays.toString(d.modelTypes)));
    }
  }
}
