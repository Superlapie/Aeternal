import com.runescape.cache.FileStore;
import com.runescape.cache.anim.Frame;
import java.io.RandomAccessFile;

public class FrameSkinProbe {
  public static void main(String[] args) throws Exception {
    int[] groups={207,410,1390,1664,1243};
    try(RandomAccessFile dat=new RandomAccessFile("client/Cache/main_file_cache.dat","r");
        RandomAccessFile idx2=new RandomAccessFile("client/Cache/main_file_cache.idx2","r")) {
      FileStore s2=new FileStore(dat,idx2,3);
      Frame.animationlist=new Frame[30000][];
      for(int g:groups){
        Frame.load(g,s2.decompress(g));
        Frame fr=null;
        for(Frame f:Frame.animationlist[g]){ if(f!=null){ fr=f; break; } }
        if(fr==null){ System.out.println("group="+g+" no frame"); continue; }
        int max=-1; int min=99999; int count=0; int maxLen=0;
        for(int i=0;i<fr.base.skinList.length;i++){
          int[] sl=fr.base.skinList[i];
          if(sl==null) continue;
          if(sl.length>maxLen) maxLen=sl.length;
          for(int v:sl){ if(v>max)max=v; if(v<min)min=v; count++; }
        }
        System.out.println("group="+g+" skinVals count="+count+" min="+min+" max="+max+" maxLen="+maxLen);
      }
    }
  }
}
