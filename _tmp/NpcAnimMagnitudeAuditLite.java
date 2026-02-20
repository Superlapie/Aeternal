import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.anim.Frame;
import com.runescape.io.Buffer;

import java.io.File;
import java.io.RandomAccessFile;

public class NpcAnimMagnitudeAuditLite {
    static class NpcDef { int stand=-1, walk=-1; }
    static class SeqDef { int[] primary; }

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

            byte[] npcDat = ar.readFile("npc.dat");
            byte[] npcIdx = ar.readFile("npc.idx");
            byte[] seq = ar.readFile("seq.dat");
            java.nio.file.Path ext = java.nio.file.Paths.get(cacheDir, "seq.dat");
            if (java.nio.file.Files.exists(ext)) seq = java.nio.file.Files.readAllBytes(ext);

            NpcDef[] npcs = readNpcs(npcDat, npcIdx);
            SeqDef[] seqs = readSeqs(seq);

            Frame.clear();
            int checked=0, unresolved=0, outliers=0;
            for (int id=0; id<npcs.length; id++) {
                NpcDef n=npcs[id];
                int[] ss={n.stand,n.walk};
                for (int sid: ss) {
                    if (sid<0 || sid>=seqs.length) continue;
                    SeqDef sd = seqs[sid];
                    if (sd==null || sd.primary==null || sd.primary.length==0 || sd.primary[0]<0) continue;
                    int frameId = sd.primary[0];
                    int group = frameId>>>16;
                    byte[] payload = s2.decompress(group);
                    if (payload != null) Frame.load(group,payload);
                    Frame f = Frame.method531(frameId);
                    checked++;
                    if (f==null) {unresolved++; continue;}
                    int maxAbs=0;
                    for (int v: f.transformX) maxAbs=Math.max(maxAbs,Math.abs(v));
                    for (int v: f.transformY) maxAbs=Math.max(maxAbs,Math.abs(v));
                    for (int v: f.transformZ) maxAbs=Math.max(maxAbs,Math.abs(v));
                    if (maxAbs>2000) {
                        outliers++;
                        if (outliers<=20) {
                            System.out.println("OUTLIER npc="+id+" seq="+sid+" frame="+frameId+" maxAbs="+maxAbs);
                        }
                    }
                }
            }
            System.out.println("MAG_LITE checked="+checked+" unresolved="+unresolved+" outlierOver2000="+outliers);
        }
    }

    static NpcDef[] readNpcs(byte[] npcDat, byte[] npcIdx){
        Buffer idx=new Buffer(npcIdx); int total=idx.readUShort(); int[] off=new int[total]; int p=2;
        for(int i=0;i<total;i++){off[i]=p;p+=idx.readUShort();}
        Buffer d=new Buffer(npcDat); NpcDef[] out=new NpcDef[total];
        for(int i=0;i<total;i++){
            d.currentPosition=off[i]; NpcDef n=new NpcDef();
            while(true){int op=d.readUnsignedByte(); if(op==0)break;
                if(op==13)n.stand=fix(d.readUShort());
                else if(op==14)n.walk=fix(d.readUShort());
                else if(op==17){n.walk=fix(d.readUShort()); d.readUShort(); d.readUShort(); d.readUShort();}
                else skipNpc(d,op);
            }
            out[i]=n;
        }
        return out;
    }
    static int fix(int v){return v==65535?-1:v;}
    static void skipNpc(Buffer b,int op){
        if(op==1){int l=b.readUnsignedByte(); for(int i=0;i<l;i++) b.readUShort();}
        else if(op==2)b.readString();
        else if(op==12)b.readUnsignedByte();
        else if(op==15||op==16||op==95||op==97||op==98||op==102||op==103)b.readUShort();
        else if(op>=30&&op<35)b.readString();
        else if(op==40||op==41){int l=b.readUnsignedByte(); for(int i=0;i<l;i++){b.readUShort(); b.readUShort();}}
        else if(op==60){int l=b.readUnsignedByte(); for(int i=0;i<l;i++) b.readUShort();}
        else if(op==93||op==99||op==107||op==109||op==111){}
        else if(op==100||op==101)b.readSignedByte();
        else if(op==106||op==118){b.readUShort(); b.readUShort(); if(op==118)b.readUShort(); int l=b.readUnsignedByte(); for(int i=0;i<=l;i++) b.readUShort();}
    }

    static SeqDef[] readSeqs(byte[] seqData){
        Buffer s=new Buffer(seqData); int total=s.readUShort(); SeqDef[] out=new SeqDef[total];
        for(int id=0; id<total && s.currentPosition<s.payload.length; id++){
            SeqDef d=new SeqDef();
            while(true){int op=s.readUnsignedByte(); if(op==0) break;
                if(op==1){int fc=s.readUShort(); for(int i=0;i<fc;i++) s.readUShort(); int[] p=new int[fc]; for(int i=0;i<fc;i++) p[i]=s.readUShort(); for(int i=0;i<fc;i++) p[i]+=s.readUShort()<<16; d.primary=p;}
                else if(op==2){s.readUShort();}
                else if(op==3){int l=s.readUnsignedByte(); for(int i=0;i<l;i++) s.readUnsignedByte();}
                else if(op==4||op==18||op==19||op==35){}
                else if(op==5||op==8||op==9||op==10||op==11){s.readUnsignedByte();}
                else if(op==6||op==7){s.readUShort();}
                else if(op==12){int l=s.readUnsignedByte(); for(int i=0;i<l;i++) s.readUShort(); for(int i=0;i<l;i++) s.readUShort();}
                else if(op==13){int l=s.readUnsignedByte(); for(int i=0;i<l;i++) s.read24Int();}
                else if(op==14){s.readInt();}
                else if(op==15){int c=s.readUShort(); for(int i=0;i<c;i++){s.readUShort(); s.read24Int();}}
                else if(op==16){s.readUShort(); s.readUShort();}
                else if(op==17){int c=s.readUnsignedByte(); for(int i=0;i<c;i++) s.readUnsignedByte();}
                else {while(s.currentPosition<s.payload.length && s.readUnsignedByte()!=0){} break;}
            }
            out[id]=d;
        }
        return out;
    }
}
