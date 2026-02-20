import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.io.Buffer;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SeqCompare {
  static int firstFrame(byte[] seqData, int id){
    Buffer s=new Buffer(seqData); int total=s.readUShort();
    for(int i=0;i<total && s.currentPosition<s.payload.length;i++){
      int ff=-1;
      while(true){int op=s.readUnsignedByte(); if(op==0) break;
        if(op==1){int fc=s.readUShort(); for(int j=0;j<fc;j++) s.readUShort(); int[] low=new int[fc]; for(int j=0;j<fc;j++) low[j]=s.readUShort(); for(int j=0;j<fc;j++) low[j]+=s.readUShort()<<16; if(fc>0) ff=low[0];}
        else if(op==2){s.readUShort();}
        else if(op==3){int l=s.readUnsignedByte(); for(int j=0;j<l;j++) s.readUnsignedByte();}
        else if(op==4||op==18||op==19||op==35){}
        else if(op==5||op==8||op==9||op==10||op==11){s.readUnsignedByte();}
        else if(op==6||op==7){s.readUShort();}
        else if(op==12){int l=s.readUnsignedByte(); for(int j=0;j<l;j++) s.readUShort(); for(int j=0;j<l;j++) s.readUShort();}
        else if(op==13){int l=s.readUnsignedByte(); for(int j=0;j<l;j++) s.read24Int();}
        else if(op==14){s.readInt();}
        else if(op==15){int c=s.readUShort(); for(int j=0;j<c;j++){s.readUShort(); s.read24Int();}}
        else if(op==16){s.readUShort(); s.readUShort();}
        else if(op==17){int c=s.readUnsignedByte(); for(int j=0;j<c;j++) s.readUnsignedByte();}
        else { while(s.currentPosition<s.payload.length && s.readUnsignedByte()!=0){} break; }
      }
      if(i==id) return ff;
    }
    return -2;
  }

  public static void main(String[] a) throws Exception {
    String cacheDir = a.length>0 ? a[0] : "client/Cache";
    if(!cacheDir.endsWith("/") && !cacheDir.endsWith("\\")) cacheDir += File.separator;
    File dat=new File(cacheDir+"main_file_cache.dat"); if(!dat.exists()) dat=new File(cacheDir+"main_file_cache.dat2");
    File idx0=new File(cacheDir+"main_file_cache.idx0");
    byte[] cfgSeq;
    try(RandomAccessFile dr=new RandomAccessFile(dat,"r"); RandomAccessFile i0=new RandomAccessFile(idx0,"r")){
      FileStore s0=new FileStore(dr,i0,1); byte[] cfg=s0.decompress(2); FileArchive ar=new FileArchive(cfg); cfgSeq=ar.readFile("seq.dat");
    }
    byte[] extSeq=Files.readAllBytes(Paths.get(cacheDir,"seq.dat"));
    int[] ids={5317,5318,808,819,98,101,11473,11474};
    for(int id:ids){
      int c=firstFrame(cfgSeq,id), e=firstFrame(extSeq,id);
      System.out.println("seq="+id+" cfg="+c+" ext="+e+" same="+(c==e));
    }
  }
}
