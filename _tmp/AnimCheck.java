import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.anim.Animation;
import com.runescape.io.Buffer;
import java.io.RandomAccessFile;

public class AnimCheck {
  public static void main(String[] args) throws Exception {
    String cacheDir = "client/Cache/";
    RandomAccessFile dat = new RandomAccessFile(cacheDir + "main_file_cache.dat", "r");
    RandomAccessFile idx0 = new RandomAccessFile(cacheDir + "main_file_cache.idx0", "r");
    FileStore store0 = new FileStore(dat, idx0, 1);
    byte[] configArchive = store0.decompress(2);
    FileArchive archive = new FileArchive(configArchive);
    Animation.init(archive);
    int[] ids = {5317,5318,5322,5323,5324,5325,5326,5327,6610};
    for (int id : ids) {
      if (id < 0 || id >= Animation.animations.length || Animation.animations[id] == null) {
        System.out.println(id + " -> missing");
        continue;
      }
      Animation a = Animation.animations[id];
      int fc = a.primaryFrames == null ? -1 : a.primaryFrames.length;
      int pf = (a.primaryFrames != null && a.primaryFrames.length > 0) ? a.primaryFrames[0] : -1;
      System.out.println(id + " skeletal=" + a.isSkeletalSequence() + " classic=" + a.hasClassicFrames() + " frameCount=" + a.frameCount + " pf0=" + pf + " pfLen=" + fc);
    }
  }
}
