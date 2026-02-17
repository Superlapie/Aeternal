import com.runescape.cache.anim.Frame;

public class TmpFrameDetails {
  public static void main(String[] args) {
    Frame.animationlist = new Frame[20000][0];
    int[] frames = {
      709296129,709296141,709296130, // 11057
      709361665,709361687,           // 11060
      508362753,508362758            // 11058
    };
    for (int f : frames) {
      Frame fr = Frame.method531(f);
      int g = f >>> 16;
      int i = f & 0xFFFF;
      if (fr == null) {
        System.out.println("frame="+f+" group="+g+" file="+i+" NULL");
        continue;
      }
      System.out.println("frame="+f+" group="+g+" file="+i+" tCount="+fr.transformationCount);
      if (fr.transformationIndices != null && fr.transformationIndices.length > 0) {
        int limit = Math.min(5, fr.transformationIndices.length);
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < limit; x++) {
          if (x > 0) sb.append(',');
          sb.append(fr.transformationIndices[x]).append(':')
            .append(fr.transformX[x]).append('/')
            .append(fr.transformY[x]).append('/')
            .append(fr.transformZ[x]);
        }
        System.out.println(" first=" + sb);
      }
    }
  }
}
