import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.anim.Animation;
import com.runescape.cache.anim.Frame;
import java.io.RandomAccessFile;

public class FrameValueProbe {
  public static void main(String[] args) throws Exception {
    String cacheDir="client/Cache/";
    try (RandomAccessFile dat = new RandomAccessFile(cacheDir+"main_file_cache.dat","r");
         RandomAccessFile idx0 = new RandomAccessFile(cacheDir+"main_file_cache.idx0","r");
         RandomAccessFile idx2 = new RandomAccessFile(cacheDir+"main_file_cache.idx2","r")) {
      FileStore s0 = new FileStore(dat, idx0, 1);
      FileStore s2 = new FileStore(dat, idx2, 3);
      Animation.init(new FileArchive(s0.decompress(2)));
      Frame.animationlist = new Frame[30000][];

      int[] seqs = {808,819,5318,5317,98,101};
      for (int seq : seqs) {
        int pf = Animation.animations[seq].primaryFrames[0];
        int g = pf>>>16; int f=pf&0xFFFF;
        Frame.load(g, s2.decompress(g));
        Frame fr = Frame.method531(pf);
        System.out.println("seq="+seq+" group="+g+" file="+f+" frame="+(fr!=null));
        if (fr==null) continue;
        System.out.println(" tCount="+fr.transformationCount+" baseTypes="+fr.base.transformationType.length);
        int lim = Math.min(10, fr.transformationCount);
        for(int i=0;i<lim;i++){
          int idx=fr.transformationIndices[i];
          int t=fr.base.transformationType[idx];
          System.out.println("  i="+i+" idx="+idx+" type="+t+" x="+fr.transformX[i]+" y="+fr.transformY[i]+" z="+fr.transformZ[i]);
        }
      }
    }
  }
}
