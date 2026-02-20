import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.anim.Animation;
import com.runescape.cache.anim.Frame;
import com.runescape.cache.def.NpcDefinition;
import com.runescape.io.Buffer;
import java.io.*;

public class NpcAnimMagnitudeAudit {
  public static void main(String[] args) throws Exception {
    String cacheDir = "client/Cache";
    File dat = new File(cacheDir + "/main_file_cache.dat");
    if (!dat.exists()) dat = new File(cacheDir + "/main_file_cache.dat2");
    File idx0 = new File(cacheDir + "/main_file_cache.idx0");
    File idx2 = new File(cacheDir + "/main_file_cache.idx2");

    try (RandomAccessFile dr = new RandomAccessFile(dat, "r");
         RandomAccessFile i0 = new RandomAccessFile(idx0, "r");
         RandomAccessFile i2 = new RandomAccessFile(idx2, "r")) {
      FileStore s0 = new FileStore(dr, i0, 1);
      FileStore s2 = new FileStore(dr, i2, 3);
      byte[] cfg = s0.decompress(2);
      FileArchive ar = new FileArchive(cfg);

      Animation.init(ar);
      NpcDefinition.init(ar);

      Frame.clear();
      int hi = 0, checked = 0, unresolved = 0;
      for (int id = 0; id < NpcDefinition.offsets.length; id++) {
        NpcDefinition n = NpcDefinition.lookup(id);
        if (n == null) continue;
        int[] seqs = { n.standAnim, n.walkAnim };
        for (int seqId : seqs) {
          if (seqId < 0 || seqId >= Animation.animations.length) continue;
          Animation a = Animation.animations[seqId];
          if (a == null || a.primaryFrames == null || a.primaryFrames.length == 0 || a.primaryFrames[0] < 0) continue;
          int frameId = a.primaryFrames[0];
          int group = frameId >>> 16;
          byte[] payload = s2.decompress(group);
          if (payload != null) Frame.load(group, payload);
          Frame f = Frame.method531(frameId);
          checked++;
          if (f == null) { unresolved++; continue; }
          int maxAbs = 0;
          for (int v : f.transformX) maxAbs = Math.max(maxAbs, Math.abs(v));
          for (int v : f.transformY) maxAbs = Math.max(maxAbs, Math.abs(v));
          for (int v : f.transformZ) maxAbs = Math.max(maxAbs, Math.abs(v));
          if (maxAbs > 2000) {
            hi++;
            if (hi <= 20) {
              System.out.println("OUTLIER npc=" + id + " seq=" + seqId + " frame=" + frameId + " maxAbs=" + maxAbs);
            }
          }
        }
      }
      System.out.println("MAG_AUDIT checked=" + checked + " unresolved=" + unresolved + " outlierOver2000=" + hi);
    }
  }
}
