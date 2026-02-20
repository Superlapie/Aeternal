import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.anim.Frame;
import com.runescape.io.Buffer;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NpcAnimAudit {
    static class NpcDef { int stand=-1, walk=-1; }
    static class SeqDef { int[] primary; }

    public static void main(String[] args) throws Exception {
        String cacheDir = args.length > 0 ? args[0] : "client/Cache";
        if (!cacheDir.endsWith("\\") && !cacheDir.endsWith("/")) cacheDir += File.separator;

        File dat = new File(cacheDir + "main_file_cache.dat");
        if (!dat.exists()) dat = new File(cacheDir + "main_file_cache.dat2");
        File idx0 = new File(cacheDir + "main_file_cache.idx0");
        File idx2 = new File(cacheDir + "main_file_cache.idx2");

        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "r");
             RandomAccessFile idx0Raf = new RandomAccessFile(idx0, "r");
             RandomAccessFile idx2Raf = new RandomAccessFile(idx2, "r")) {

            FileStore store0 = new FileStore(datRaf, idx0Raf, 1);
            FileStore store2 = new FileStore(datRaf, idx2Raf, 3);

            byte[] config = store0.decompress(2);
            FileArchive archive = new FileArchive(config);

            byte[] npcDat = archive.readFile("npc.dat");
            byte[] npcIdx = archive.readFile("npc.idx");

            byte[] seqData = archive.readFile("seq.dat");
            Path extSeq = Paths.get(cacheDir, "seq.dat").toAbsolutePath().normalize();
            if (Files.exists(extSeq)) seqData = Files.readAllBytes(extSeq);

            NpcDef[] npcs = readNpcs(npcDat, npcIdx);
            SeqDef[] seqs = readSeqs(seqData);

            Map<Integer, List<Integer>> groupToNpc = new HashMap<>();
            Map<Integer, List<Integer>> groupToSeq = new HashMap<>();

            for (int npcId = 0; npcId < npcs.length; npcId++) {
                NpcDef n = npcs[npcId];
                add(n.stand, npcId, seqs, groupToNpc, groupToSeq);
                add(n.walk, npcId, seqs, groupToNpc, groupToSeq);
            }

            Set<Integer> groups = new TreeSet<>(groupToNpc.keySet());
            int maxGroup = groups.isEmpty() ? 0 : groups.stream().max(Integer::compareTo).get();
            Frame.clear();
            Frame.animationlist = new Frame[maxGroup + 1][];

            List<Integer> failed = new ArrayList<>();
            for (int g : groups) {
                byte[] payload = store2.decompress(g);
                if (payload == null || payload.length == 0) {
                    failed.add(g);
                    continue;
                }
                try {
                    Frame.load(g, payload);
                } catch (Throwable t) {
                    failed.add(g);
                    continue;
                }
                Frame[] arr = Frame.animationlist[g];
                if (arr == null) {
                    failed.add(g);
                    continue;
                }
                boolean any = false;
                for (Frame f : arr) if (f != null) { any = true; break; }
                if (!any) failed.add(g);
            }

            System.out.println("npcCount=" + npcs.length + " seqCount=" + seqs.length + " groupsUsed=" + groups.size());
            System.out.println("failedGroups=" + failed.size());
            for (int i = 0; i < Math.min(25, failed.size()); i++) {
                int g = failed.get(i);
                List<Integer> sn = groupToSeq.getOrDefault(g, Collections.emptyList());
                List<Integer> nn = groupToNpc.getOrDefault(g, Collections.emptyList());
                int seqSample = sn.isEmpty() ? -1 : sn.get(0);
                int npcSample = nn.isEmpty() ? -1 : nn.get(0);
                System.out.println("group=" + g + " seqRefs=" + sn.size() + " npcRefs=" + nn.size() + " sampleSeq=" + seqSample + " sampleNpc=" + npcSample);
            }
        }
    }

    static void add(int seqId, int npcId, SeqDef[] seqs, Map<Integer,List<Integer>> groupToNpc, Map<Integer,List<Integer>> groupToSeq) {
        if (seqId < 0 || seqId >= seqs.length) return;
        SeqDef s = seqs[seqId];
        if (s == null || s.primary == null || s.primary.length == 0) return;
        int pf = s.primary[0];
        if (pf < 0) return;
        int g = pf >>> 16;
        groupToNpc.computeIfAbsent(g, k -> new ArrayList<>()).add(npcId);
        groupToSeq.computeIfAbsent(g, k -> new ArrayList<>()).add(seqId);
    }

    static NpcDef[] readNpcs(byte[] npcDat, byte[] npcIdx) {
        Buffer idx = new Buffer(npcIdx);
        int total = idx.readUShort();
        int[] off = new int[total];
        int p = 2;
        for (int i=0;i<total;i++){ off[i]=p; p += idx.readUShort(); }
        Buffer d = new Buffer(npcDat);
        NpcDef[] out = new NpcDef[total];
        for (int i=0;i<total;i++) {
            d.currentPosition = off[i];
            NpcDef n = new NpcDef();
            while (true) {
                int op = d.readUnsignedByte();
                if (op==0) break;
                if (op==13) { n.stand = fix(d.readUShort()); }
                else if (op==14) { n.walk = fix(d.readUShort()); }
                else if (op==17) { n.walk=fix(d.readUShort()); d.readUShort(); d.readUShort(); d.readUShort(); }
                else skipNpcOp(d, op);
            }
            out[i]=n;
        }
        return out;
    }

    static int fix(int v){ return v==65535 ? -1 : v; }

    static void skipNpcOp(Buffer b, int op){
        if (op==1){ int len=b.readUnsignedByte(); for(int i=0;i<len;i++) b.readUShort(); }
        else if (op==2){ b.readString(); }
        else if (op==12){ b.readUnsignedByte(); }
        else if (op==15 || op==16 || op==95 || op==97 || op==98 || op==102 || op==103){ b.readUShort(); }
        else if (op>=30 && op<35){ b.readString(); }
        else if (op==40 || op==41){ int len=b.readUnsignedByte(); for(int i=0;i<len;i++){ b.readUShort(); b.readUShort(); } }
        else if (op==60){ int len=b.readUnsignedByte(); for(int i=0;i<len;i++) b.readUShort(); }
        else if (op==93 || op==99 || op==107 || op==109 || op==111){ }
        else if (op==100 || op==101){ b.readSignedByte(); }
        else if (op==106 || op==118){ b.readUShort(); b.readUShort(); if(op==118)b.readUShort(); int len=b.readUnsignedByte(); for(int i=0;i<=len;i++) b.readUShort(); }
    }

    static SeqDef[] readSeqs(byte[] seqData) {
        Buffer s = new Buffer(seqData);
        int total = s.readUShort();
        SeqDef[] out = new SeqDef[total];
        for (int id=0; id<total && s.currentPosition < s.payload.length; id++) {
            SeqDef d = new SeqDef();
            while (true) {
                int op = s.readUnsignedByte();
                if (op==0) break;
                if (op==1){
                    int fc = s.readUShort();
                    int[] dur = new int[fc];
                    int[] pri = new int[fc];
                    for(int i=0;i<fc;i++) dur[i]=s.readUShort();
                    for(int i=0;i<fc;i++) pri[i]=s.readUShort();
                    for(int i=0;i<fc;i++) pri[i]+= (s.readUShort()<<16);
                    d.primary = pri;
                } else if (op==2){ s.readUShort(); }
                else if (op==3){ int len=s.readUnsignedByte(); for(int i=0;i<len;i++) s.readUnsignedByte(); }
                else if (op==4 || op==18 || op==19 || op==35){ }
                else if (op==5 || op==8 || op==9 || op==10 || op==11){ s.readUnsignedByte(); }
                else if (op==6 || op==7 || op==16){ s.readUShort(); if (op==16) s.readUShort(); }
                else if (op==12){ int len=s.readUnsignedByte(); for(int i=0;i<len;i++) s.readUShort(); for(int i=0;i<len;i++) s.readUShort(); }
                else if (op==13){ int len=s.readUnsignedByte(); for(int i=0;i<len;i++) s.read24Int(); }
                else if (op==14){ s.readInt(); }
                else if (op==15){ int c=s.readUShort(); for(int i=0;i<c;i++){ s.readUShort(); s.read24Int(); } }
                else if (op==17){ int c=s.readUnsignedByte(); for(int i=0;i<c;i++) s.readUnsignedByte(); }
                else { // resync
                    while (s.currentPosition < s.payload.length && s.readUnsignedByte() != 0) {}
                    break;
                }
            }
            out[id]=d;
        }
        return out;
    }
}
