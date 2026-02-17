import com.runescape.cache.anim.Frame;

public class TmpFrameCheck {
    public static void main(String[] args) {
        Frame.animationlist = new Frame[20000][0];
        int[] frames = {
            709296129,709296141,709296130, //11057
            508362753,508362758,           //11058
            508297217,508297222,           //11059
            709361665,709361687,           //11060
            644415489,644415511,           //11061
            643760129,643760149,           //11062
            520159233,520159244,           //11063
            520224769,520224787            //11064
        };
        for (int f : frames) {
            Frame fr = Frame.method531(f);
            int group = f >>> 16;
            int file = f & 0xFFFF;
            System.out.println("frame=" + f + " group=" + group + " file=" + file + " ok=" + (fr != null));
        }
    }
}
