import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.anim.Animation;
import com.runescape.cache.anim.Frame;
import java.io.RandomAccessFile;

public class SeqGroupProbe {
  public static void main(String[] args) throws Exception {
    String cacheDir = "client/Cache/";
    try (RandomAccessFile dat = new RandomAccessFile(cacheDir + "main_file_cache.dat", "r");
         RandomAccessFile idx0 = new RandomAccessFile(cacheDir + "main_file_cache.idx0", "r");
         RandomAccessFile idx2 = new RandomAccessFile(cacheDir + "main_file_cache.idx2", "r")) {
      FileStore store0 = new FileStore(dat, idx0, 1);
      FileStore store2 = new FileStore(dat, idx2, 3);
      FileArchive archive = new FileArchive(store0.decompress(2));
      Animation.init(archive);
      Frame.animationlist = new Frame[30000][];

      int[] seqIds = {808,819,5318,5317,6610,98,101};
      for (int seqId : seqIds) {
        Animation a = Animation.animations[seqId];
        int pf = a.primaryFrames[0];
        int g = pf >>> 16;
        int f = pf & 0xFFFF;
        byte[] data = store2.decompress(g);
        if (data != null) {
          Frame.load(g, data);
        }
        Frame[] arr = (g >= 0 && g < Frame.animationlist.length) ? Frame.animationlist[g] : null;
        int nonNull = 0;
        int first = -1;
        int last = -1;
        if (arr != null) {
          for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null) {
              nonNull++;
              if (first == -1) first = i;
              last = i;
            }
          }
        }
        System.out.println("seq=" + seqId + " pf0=" + pf + " group=" + g + " file=" + f + " arr=" + (arr==null?"null":arr.length) + " nonNull=" + nonNull + " first=" + first + " last=" + last + " target=" + (arr!=null && f<arr.length ? (arr[f]!=null) : false));
      }
    }
  }
}
