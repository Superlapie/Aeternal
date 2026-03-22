import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.def.ObjectDefinition;
import java.io.File;import java.io.RandomAccessFile;import java.util.Arrays;
public class ObjRuntimeProbe2 {
  public static void main(String[] a) throws Exception {
    String c=a.length>0?a[0]:"Cache";
    File dat=new File(c,"main_file_cache.dat"); if(!dat.exists()) dat=new File(c,"main_file_cache.dat2");
    try(RandomAccessFile d=new RandomAccessFile(dat,"r"); RandomAccessFile i0=new RandomAccessFile(new File(c,"main_file_cache.idx0"),"r")){
      byte[] cfg=new FileStore(d,i0,1).decompress(2); ObjectDefinition.init(new FileArchive(cfg));
    }
    int[] ids={10943,11161,11360,11361,11364,11365,11366,11367,11370,11371,11372,11373,11374,11375,11376,11377,7453,7454,7455,7456,7457,7486,7492,7460,7461};
    for(int id:ids){ObjectDefinition d=ObjectDefinition.lookup(id); System.out.println(id+"|"+d.name+"|"+Arrays.toString(d.interactions)+"|"+(d.modelIds==null?"null":Arrays.toString(d.modelIds)));}
  }
}
