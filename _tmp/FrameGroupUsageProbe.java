import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.io.Buffer;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FrameGroupUsageProbe {
  static class NpcDef { int stand=-1, walk=-1; }
  static class SeqDef { int[] primary; }

  public static void main(String[] args) throws Exception {
    String cacheDir = args.length>0 ? args[0] : "client/Cache";
    int targetGroup = args.length>1 ? Integer.parseInt(args[1]) : 1034;
    if (!cacheDir.endsWith("/") && !cacheDir.endsWith("\\")) cacheDir += File.separator;

    File dat = new File(cacheDir + "main_file_cache.dat");
    if (!dat.exists()) dat = new File(cacheDir + "main_file_cache.dat2");
    File idx0 = new File(cacheDir + "main_file_cache.idx0");

    try (RandomAccessFile dr = new RandomAccessFile(dat, "r");
         RandomAccessFile i0 = new RandomAccessFile(idx0, "r")) {
      FileStore s0 = new FileStore(dr, i0, 1);
      byte[] cfg = s0.decompress(2);
      FileArchive ar = new FileArchive(cfg);

      byte[] seqData = ar.readFile("seq.dat");
      Path ext = Paths.get(cacheDir, "seq.dat");
      if (Files.exists(ext)) seqData = Files.readAllBytes(ext);
      SeqDef[] seqs = readSeqs(seqData);

      byte[] npcDat = ar.readFile("npc.dat");
      byte[] npcIdx = ar.readFile("npc.idx");
      NpcDef[] npcs = readNpcs(npcDat, npcIdx);

      int seqRefs = 0;
      List<Integer> seqIds = new ArrayList<>();
      for (int i = 0; i < seqs.length; i++) {
        SeqDef s = seqs[i];
        if (s == null || s.primary == null) continue;
        for (int f : s.primary) {
          if (f < 0) continue;
          if ((f >>> 16) == targetGroup) { seqRefs++; if (seqIds.size() < 30) seqIds.add(i); break; }
        }
      }

      int npcStandWalkRefs = 0;
      List<String> npcSamples = new ArrayList<>();
      for (int id = 0; id < npcs.length; id++) {
        NpcDef n = npcs[id];
        if (usesGroup(seqs, n.stand, targetGroup) || usesGroup(seqs, n.walk, targetGroup)) {
          npcStandWalkRefs++;
          if (npcSamples.size() < 30) npcSamples.add("npc=" + id + " stand=" + n.stand + " walk=" + n.walk);
        }
      }

      System.out.println("group=" + targetGroup + " seqRefs=" + seqRefs + " npcStandWalkRefs=" + npcStandWalkRefs);
      System.out.println("seqSamples=" + seqIds);
      for (String s : npcSamples) System.out.println(s);
    }
  }

  static boolean usesGroup(SeqDef[] seqs, int seqId, int group) {
    if (seqId < 0 || seqId >= seqs.length) return false;
    SeqDef s = seqs[seqId];
    if (s == null || s.primary == null) return false;
    for (int f : s.primary) if (f >= 0 && (f >>> 16) == group) return true;
    return false;
  }

  static NpcDef[] readNpcs(byte[] npcDat, byte[] npcIdx) {
    Buffer idx = new Buffer(npcIdx); int total = idx.readUShort(); int[] off = new int[total]; int p = 2;
    for (int i = 0; i < total; i++) { off[i] = p; p += idx.readUShort(); }
    Buffer d = new Buffer(npcDat); NpcDef[] out = new NpcDef[total];
    for (int i = 0; i < total; i++) {
      d.currentPosition = off[i]; NpcDef n = new NpcDef();
      while (true) {
        int op = d.readUnsignedByte(); if (op == 0) break;
        if (op == 13) n.stand = fix(d.readUShort());
        else if (op == 14) n.walk = fix(d.readUShort());
        else if (op == 17) { n.walk = fix(d.readUShort()); d.readUShort(); d.readUShort(); d.readUShort(); }
        else skipNpc(d, op);
      }
      out[i] = n;
    }
    return out;
  }

  static int fix(int v){ return v == 65535 ? -1 : v; }
  static void skipNpc(Buffer b, int op) {
    if (op == 1) { int l = b.readUnsignedByte(); for (int i = 0; i < l; i++) b.readUShort(); }
    else if (op == 2) b.readString();
    else if (op == 12) b.readUnsignedByte();
    else if (op == 15 || op == 16 || op == 95 || op == 97 || op == 98 || op == 102 || op == 103) b.readUShort();
    else if (op >= 30 && op < 35) b.readString();
    else if (op == 40 || op == 41) { int l = b.readUnsignedByte(); for (int i = 0; i < l; i++) { b.readUShort(); b.readUShort(); } }
    else if (op == 60) { int l = b.readUnsignedByte(); for (int i = 0; i < l; i++) b.readUShort(); }
    else if (op == 93 || op == 99 || op == 107 || op == 109 || op == 111) { }
    else if (op == 100 || op == 101) b.readSignedByte();
    else if (op == 106 || op == 118) { b.readUShort(); b.readUShort(); if (op == 118) b.readUShort(); int l = b.readUnsignedByte(); for (int i = 0; i <= l; i++) b.readUShort(); }
  }

  static SeqDef[] readSeqs(byte[] seqData) {
    Buffer s = new Buffer(seqData); int total = s.readUShort(); SeqDef[] out = new SeqDef[total];
    for (int id = 0; id < total && s.currentPosition < s.payload.length; id++) {
      SeqDef d = new SeqDef();
      while (true) {
        int op = s.readUnsignedByte(); if (op == 0) break;
        if (op == 1) {
          int fc = s.readUShort(); for (int i = 0; i < fc; i++) s.readUShort();
          int[] p = new int[fc]; for (int i = 0; i < fc; i++) p[i] = s.readUShort();
          for (int i = 0; i < fc; i++) p[i] += s.readUShort() << 16;
          d.primary = p;
        } else if (op == 2) s.readUShort();
        else if (op == 3) { int l = s.readUnsignedByte(); for (int i = 0; i < l; i++) s.readUnsignedByte(); }
        else if (op == 4 || op == 18 || op == 19 || op == 35) {}
        else if (op == 5 || op == 8 || op == 9 || op == 10 || op == 11) s.readUnsignedByte();
        else if (op == 6 || op == 7) s.readUShort();
        else if (op == 12) { int l = s.readUnsignedByte(); for (int i = 0; i < l; i++) s.readUShort(); for (int i = 0; i < l; i++) s.readUShort(); }
        else if (op == 13) { int l = s.readUnsignedByte(); for (int i = 0; i < l; i++) s.read24Int(); }
        else if (op == 14) s.readInt();
        else if (op == 15) { int c = s.readUShort(); for (int i = 0; i < c; i++) { s.readUShort(); s.read24Int(); } }
        else if (op == 16) { s.readUShort(); s.readUShort(); }
        else if (op == 17) { int c = s.readUnsignedByte(); for (int i = 0; i < c; i++) s.readUnsignedByte(); }
        else { while (s.currentPosition < s.payload.length && s.readUnsignedByte() != 0) {} break; }
      }
      out[id] = d;
    }
    return out;
  }
}
