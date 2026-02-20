import com.runescape.cache.FileStore;
import com.runescape.cache.anim.Frame;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

public class FrameBaseTypeProbe {
  public static void main(String[] args) throws Exception {
    int[] groups={207,410,1390,1664,1243};
    try(RandomAccessFile dat=new RandomAccessFile("client/Cache/main_file_cache.dat","r");
        RandomAccessFile idx2=new RandomAccessFile("client/Cache/main_file_cache.idx2","r")) {
      FileStore s2=new FileStore(dat,idx2,3);
      Frame.animationlist=new Frame[30000][];
      for(int g:groups){
        Frame.load(g,s2.decompress(g));
        Frame[] arr=Frame.animationlist[g];
        Frame fr=null;
        for(int i=0;i<arr.length;i++){ if(arr[i]!=null){ fr=arr[i]; break; } }
        if(fr==null){ System.out.println("group="+g+" no frames"); continue; }
        int[] t=fr.base.transformationType;
        int max=-1,min=9999; Set<Integer> u=new HashSet<>();
        for(int v:t){ if(v>max)max=v; if(v<min)min=v; u.add(v);} 
        System.out.println("group="+g+" baseCount="+t.length+" minType="+min+" maxType="+max+" uniq="+u);
      }
    }
  }
}
