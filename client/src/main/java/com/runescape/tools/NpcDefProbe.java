package com.runescape.tools;

import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.io.Buffer;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Probes npc definitions from a 317-style cache for a given npc id.
 *
 * Usage:
 * java com.runescape.tools.NpcDefProbe <cacheDir> <npcId>
 */
public final class NpcDefProbe {

    private NpcDefProbe() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java com.runescape.tools.NpcDefProbe <cacheDir> <npcId>");
            return;
        }

        String cacheDir = normalize(args[0]);
        int npcId = Integer.parseInt(args[1]);

        File dat = resolveDatFile(cacheDir);
        File idx0 = new File(cacheDir + "main_file_cache.idx0");
        if (!dat.exists() || !idx0.exists()) {
            throw new IllegalStateException("Missing main_file_cache.dat/.dat2 or idx0 in " + cacheDir);
        }

        byte[] configArchive;
        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "r");
             RandomAccessFile idxRaf = new RandomAccessFile(idx0, "r")) {
            FileStore store0 = new FileStore(datRaf, idxRaf, 1);
            configArchive = store0.decompress(2);
        }

        if (configArchive == null || configArchive.length == 0) {
            throw new IllegalStateException("Could not read config archive (file 2) from idx0");
        }

        FileArchive archive = new FileArchive(configArchive);
        byte[] npcDat = archive.readFile("npc.dat");
        byte[] npcIdx = archive.readFile("npc.idx");
        if (npcDat == null || npcIdx == null) {
            throw new IllegalStateException("npc.dat or npc.idx missing from config archive");
        }

        Buffer idxBuf = new Buffer(npcIdx);
        int total = idxBuf.readUShort();
        if (npcId < 0 || npcId >= total) {
            throw new IllegalArgumentException("npcId " + npcId + " out of range (0.." + (total - 1) + ")");
        }

        int offset = 2;
        for (int i = 0; i < npcId; i++) {
            offset += idxBuf.readUShort();
        }

        Buffer datBuf = new Buffer(npcDat);
        datBuf.currentPosition = offset;
        ProbeDef def = decode(datBuf);

        System.out.println("npcId=" + npcId + " totalNpcDefs=" + total);
        System.out.println("name=" + def.name);
        System.out.println("size=" + def.size + " combatLevel=" + def.combatLevel + " clickable=" + def.clickable);
        System.out.println("stand=" + def.standAnim + " walk=" + def.walkAnim
                + " turn180=" + def.turn180 + " turn90cw=" + def.turn90cw + " turn90ccw=" + def.turn90ccw);
        System.out.println("models=" + Arrays.toString(def.models));
        System.out.println("actions=" + Arrays.toString(def.actions));
    }

    private static ProbeDef decode(Buffer buffer) {
        ProbeDef def = new ProbeDef();

        while (true) {
            int opcode = buffer.readUnsignedByte();
            if (opcode == 0) {
                break;
            } else if (opcode == 1) {
                int len = buffer.readUnsignedByte();
                def.models = new int[len];
                for (int i = 0; i < len; i++) {
                    def.models[i] = buffer.readUShort();
                }
            } else if (opcode == 2) {
                def.name = buffer.readString();
            } else if (opcode == 12) {
                def.size = buffer.readUnsignedByte();
            } else if (opcode == 13) {
                def.standAnim = buffer.readUShort();
            } else if (opcode == 14) {
                def.walkAnim = buffer.readUShort();
            } else if (opcode == 15 || opcode == 16) {
                buffer.readUShort();
            } else if (opcode == 17) {
                def.walkAnim = buffer.readUShort();
                def.turn180 = buffer.readUShort();
                def.turn90cw = buffer.readUShort();
                def.turn90ccw = buffer.readUShort();
            } else if (opcode >= 30 && opcode < 35) {
                if (def.actions == null) {
                    def.actions = new String[5];
                }
                def.actions[opcode - 30] = buffer.readString();
            } else if (opcode == 40) {
                int len = buffer.readUnsignedByte();
                for (int i = 0; i < len; i++) {
                    buffer.readUShort();
                    buffer.readUShort();
                }
            } else if (opcode == 41) {
                int len = buffer.readUnsignedByte();
                for (int i = 0; i < len; i++) {
                    buffer.readUShort();
                    buffer.readUShort();
                }
            } else if (opcode == 60) {
                int len = buffer.readUnsignedByte();
                for (int i = 0; i < len; i++) {
                    buffer.readUShort();
                }
            } else if (opcode == 93) {
                // no-op
            } else if (opcode == 95) {
                def.combatLevel = buffer.readUShort();
            } else if (opcode == 97 || opcode == 98 || opcode == 102 || opcode == 103) {
                buffer.readUShort();
            } else if (opcode == 99) {
                // no-op
            } else if (opcode == 100 || opcode == 101) {
                buffer.readSignedByte();
            } else if (opcode == 106 || opcode == 118) {
                int varBit = buffer.readUShort();
                int setting = buffer.readUShort();
                if (opcode == 118) {
                    buffer.readUShort();
                }
                int len = buffer.readUnsignedByte();
                for (int i = 0; i <= len; i++) {
                    buffer.readUShort();
                }
                // keep compiler happy
                if (varBit == -2 || setting == -2) {
                    throw new IllegalStateException("unreachable");
                }
            } else if (opcode == 107 || opcode == 111) {
                // no-op
            } else if (opcode == 109) {
                def.clickable = false;
            } else {
                throw new IllegalStateException("Unsupported npc opcode " + opcode + " at pos " + buffer.currentPosition);
            }
        }

        if (def.models == null) {
            def.models = new int[0];
        }
        if (def.actions == null) {
            def.actions = new String[0];
        }
        return def;
    }

    private static String normalize(String dir) {
        return dir.endsWith("/") || dir.endsWith("\\") ? dir : dir + File.separator;
    }

    private static File resolveDatFile(String cacheDir) {
        File dat = new File(cacheDir + "main_file_cache.dat");
        if (dat.exists()) {
            return dat;
        }
        return new File(cacheDir + "main_file_cache.dat2");
    }

    private static final class ProbeDef {
        String name = "null";
        int size = 1;
        int combatLevel = -1;
        boolean clickable = true;
        int standAnim = -1;
        int walkAnim = -1;
        int turn180 = -1;
        int turn90cw = -1;
        int turn90ccw = -1;
        int[] models;
        String[] actions;
    }
}

