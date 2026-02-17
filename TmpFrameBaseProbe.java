import com.runescape.cache.anim.Frame;

public class TmpFrameBaseProbe {
  public static void main(String[] args) {
    Frame.animationlist = new Frame[20000][0];
    int[] frames = {709296129,709296141,709361665,709361687};
    for (int f : frames) {
      Frame fr = Frame.method531(f);
      if (fr == null || fr.base == null) {
        System.out.println("frame="+f+" null");
        continue;
      }
      int tlen = fr.base.transformationType == null ? -1 : fr.base.transformationType.length;
      int sllen = fr.base.skinList == null ? -1 : fr.base.skinList.length;
      System.out.println("frame="+f+" baseTypes="+tlen+" skinList="+sllen+" tCount="+fr.transformationCount);
    }
  }
}
