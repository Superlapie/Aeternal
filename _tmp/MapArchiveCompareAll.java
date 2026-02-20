import com.runescape.cache.FileStore;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MapArchiveCompareAll {
  public static void main(String[] args) throws Exception {
    try (RandomAccessFile dat = new RandomAccessFile("client/Cache/main_file_cache.dat", "r");
         RandomAccessFile idx4 = new RandomAccessFile("client/Cache/main_file_cache.idx4", "r")) {
      FileStore store = new FileStore(dat, idx4, 5);
      int mismatches = 0;
      for (int id = 6239; id <= 6268; id++) {
        byte[] c = store.decompress(id);
        byte[] s = Files.readAllBytes(Paths.get("server/data/clipping/maps/" + id + ".dat"));
        boolean eq = equals(c, s);
        if (!eq) {
          mismatches++;
          System.out.println("mismatch id=" + id + " clientLen=" + (c==null?-1:c.length) + " serverLen=" + s.length);
        }
      }
      System.out.println("mismatches=" + mismatches);
    }
  }

  private static boolean equals(byte[] a, byte[] b){
    if (a == b) return true;
    if (a == null || b == null) return false;
    if (a.length != b.length) return false;
    for(int i=0;i<a.length;i++) if(a[i]!=b[i]) return false;
    return true;
  }
}
