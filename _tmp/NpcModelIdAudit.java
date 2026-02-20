import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.io.Buffer;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.*;

public class NpcModelIdAudit {
    static class Def { int[] models; }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: <cacheDir> <changedIdsCsv>");
            return;
        }
        String cacheDir = args[0];
        if (!cacheDir.endsWith("\\") && !cacheDir.endsWith("/")) cacheDir += File.separator;
        Set<Integer> changed = new HashSet<>();
        for (String s : args[1].split(",")) {
            s = s.trim();
            if (!s.isEmpty()) changed.add(Integer.parseInt(s));
        }

        File dat = new File(cacheDir + "main_file_cache.dat");
        if (!dat.exists()) dat = new File(cacheDir + "main_file_cache.dat2");
        File idx0 = new File(cacheDir + "main_file_cache.idx0");

        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "r");
             RandomAccessFile idxRaf = new RandomAccessFile(idx0, "r")) {
            FileStore store0 = new FileStore(datRaf, idxRaf, 1);
            byte[] config = store0.decompress(2);
            FileArchive archive = new FileArchive(config);
            byte[] npcDat = archive.readFile("npc.dat");
            byte[] npcIdx = archive.readFile("npc.idx");

            Buffer idx = new Buffer(npcIdx);
            int total = idx.readUShort();
            int[] offsets = new int[total];
            int off = 2;
            for (int i = 0; i < total; i++) { offsets[i] = off; off += idx.readUShort(); }

            Buffer datBuf = new Buffer(npcDat);
            Map<Integer, List<Integer>> idToNpcs = new TreeMap<>();
            for (int id : changed) idToNpcs.put(id, new ArrayList<>());

            for (int npc = 0; npc < total; npc++) {
                datBuf.currentPosition = offsets[npc];
                int[] models = readModels(datBuf);
                if (models == null) continue;
                for (int m : models) {
                    List<Integer> lst = idToNpcs.get(m);
                    if (lst != null) lst.add(npc);
                }
            }

            int used = 0;
            for (Map.Entry<Integer,List<Integer>> e : idToNpcs.entrySet()) {
                if (!e.getValue().isEmpty()) {
                    used++;
                    System.out.println("model=" + e.getKey() + " npcs=" + e.getValue().size() + " sampleNpc=" + e.getValue().get(0));
                }
            }
            System.out.println("changed_models=" + changed.size() + " used_by_base_npcs=" + used);
        }
    }

    private static int[] readModels(Buffer b) {
        int[] models = null;
        while (true) {
            int op = b.readUnsignedByte();
            if (op == 0) break;
            if (op == 1) {
                int len = b.readUnsignedByte();
                models = new int[len];
                for (int i = 0; i < len; i++) models[i] = b.readUShort();
            } else if (op == 2) {
                b.readString();
            } else if (op == 12) {
                b.readUnsignedByte();
            } else if (op == 13 || op == 14 || op == 15 || op == 16 || op == 95 || op == 97 || op == 98 || op == 100 || op == 101 || op == 102 || op == 103) {
                if (op == 100 || op == 101) b.readSignedByte(); else b.readUShort();
            } else if (op == 17) {
                b.readUShort(); b.readUShort(); b.readUShort(); b.readUShort();
            } else if (op >= 30 && op < 35) {
                b.readString();
            } else if (op == 40 || op == 41) {
                int len = b.readUnsignedByte();
                for (int i = 0; i < len; i++) { b.readUShort(); b.readUShort(); }
            } else if (op == 60) {
                int len = b.readUnsignedByte();
                for (int i = 0; i < len; i++) b.readUShort();
            } else if (op == 93 || op == 99 || op == 107 || op == 109 || op == 111) {
                // no payload
            } else if (op == 106 || op == 118) {
                b.readUShort(); b.readUShort();
                if (op == 118) b.readUShort();
                int len = b.readUnsignedByte();
                for (int i = 0; i <= len; i++) b.readUShort();
            } else {
                // best-effort abort
                break;
            }
        }
        return models;
    }
}
