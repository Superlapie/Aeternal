package com.runescape.tools;

import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.io.Buffer;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Locale;

/**
 * Probes object definitions from a 317-style cache.
 *
 * Usage:
 * java com.runescape.tools.ObjectDefProbe <cacheDir> id <objectId>
 * java com.runescape.tools.ObjectDefProbe <cacheDir> find <keyword> [limit]
 */
public final class ObjectDefProbe {

    private ObjectDefProbe() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java com.runescape.tools.ObjectDefProbe <cacheDir> id <objectId>");
            System.out.println("   or: java com.runescape.tools.ObjectDefProbe <cacheDir> find <keyword> [limit]");
            return;
        }

        String cacheDir = normalize(args[0]);
        String mode = args[1].toLowerCase(Locale.ROOT);

        CacheLocData data = loadLoc(cacheDir);
        if ("id".equals(mode)) {
            int objectId = Integer.parseInt(args[2]);
            if (objectId < 0 || objectId >= data.total) {
                throw new IllegalArgumentException("objectId out of range: " + objectId + " (0.." + (data.total - 1) + ")");
            }
            ProbeDef def = decodeAt(data.locDat, data.offsets[objectId]);
            printDef(objectId, def);
            return;
        }

        if ("find".equals(mode)) {
            String keyword = args[2].toLowerCase(Locale.ROOT);
            int limit = args.length >= 4 ? Integer.parseInt(args[3]) : 100;
            int printed = 0;
            for (int i = 0; i < data.total; i++) {
                ProbeDef def = decodeAt(data.locDat, data.offsets[i]);
                if (def.name != null && def.name.toLowerCase(Locale.ROOT).contains(keyword)) {
                    System.out.println("id=" + i + " name=" + def.name
                            + " interactType=" + def.interactType
                            + " solid=" + def.solid
                            + " actions=" + Arrays.toString(def.actions));
                    printed++;
                    if (printed >= limit) {
                        break;
                    }
                }
            }
            System.out.println("matches=" + printed);
            return;
        }

        throw new IllegalArgumentException("Unknown mode: " + mode);
    }

    private static CacheLocData loadLoc(String cacheDir) throws Exception {
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
        byte[] locDat = archive.readFile("loc.dat");
        byte[] locIdx = archive.readFile("loc.idx");
        if (locDat == null || locIdx == null) {
            throw new IllegalStateException("loc.dat or loc.idx missing from config archive");
        }

        Buffer idxBuf = new Buffer(locIdx);
        int total = idxBuf.readUShort();
        int[] offsets = new int[total];
        int offset = 2;
        for (int i = 0; i < total; i++) {
            offsets[i] = offset;
            offset += idxBuf.readUShort();
        }
        return new CacheLocData(locDat, offsets, total);
    }

    private static ProbeDef decodeAt(byte[] locDat, int offset) {
        Buffer buffer = new Buffer(locDat);
        buffer.currentPosition = offset;

        ProbeDef def = new ProbeDef();
        while (true) {
            int opcode = buffer.readUnsignedByte();
            if (opcode == 0) {
                break;
            } else if (opcode == 1 || opcode == 5) {
                int len = buffer.readUnsignedByte();
                if (opcode == 1) {
                    for (int i = 0; i < len; i++) {
                        buffer.readUShort();
                        buffer.readUnsignedByte();
                    }
                } else {
                    for (int i = 0; i < len; i++) {
                        buffer.readUShort();
                    }
                }
            } else if (opcode == 2) {
                def.name = buffer.readString();
            } else if (opcode == 14 || opcode == 15 || opcode == 19 || opcode == 21
                    || opcode == 22 || opcode == 23 || opcode == 24 || opcode == 28
                    || opcode == 64 || opcode == 73 || opcode == 74 || opcode == 75
                    || opcode == 92) {
                if (opcode == 24 || opcode == 92) {
                    buffer.readUShort();
                    if (opcode == 92) {
                        buffer.readUShort();
                        int n = buffer.readUnsignedByte();
                        for (int i = 0; i <= n; i++) {
                            buffer.readUShort();
                        }
                    }
                } else if (opcode == 28 || opcode == 75 || opcode == 19) {
                    if (opcode == 19) {
                        def.interactType = buffer.readUnsignedByte();
                    } else {
                        buffer.readUnsignedByte();
                    }
                } else if (opcode == 74) {
                    def.solid = false;
                }
            } else if (opcode >= 30 && opcode < 35) {
                def.actions[opcode - 30] = buffer.readString();
            } else if (opcode == 29 || opcode == 39) {
                buffer.readSignedByte();
            } else if (opcode == 40 || opcode == 41) {
                int len = buffer.readUnsignedByte();
                for (int i = 0; i < len; i++) {
                    buffer.readUShort();
                    buffer.readUShort();
                }
            } else if (opcode >= 65 && opcode <= 67) {
                buffer.readUShort();
            } else if (opcode == 68) {
                buffer.readUShort();
            } else if (opcode == 69) {
                buffer.readUnsignedByte();
            } else if (opcode >= 70 && opcode <= 72) {
                buffer.readShort();
            } else if (opcode == 77 || opcode == 78) {
                buffer.readUShort();
                buffer.readUShort();
                if (opcode == 78) {
                    buffer.readUShort();
                    buffer.readUnsignedByte();
                }
                int n = buffer.readUnsignedByte();
                for (int i = 0; i <= n; i++) {
                    buffer.readUShort();
                }
            } else if (opcode == 81) {
                buffer.readUnsignedByte();
            } else if (opcode == 82) {
                buffer.readUShort();
            } else if (opcode == 89 || opcode == 90 || opcode == 91) {
                // no payload
            } else if (opcode == 249) {
                int len = buffer.readUnsignedByte();
                for (int i = 0; i < len; i++) {
                    boolean stringVal = buffer.readUnsignedByte() == 1;
                    buffer.read24Int();
                    if (stringVal) {
                        buffer.readString();
                    } else {
                        buffer.readInt();
                    }
                }
            } else {
                // Mirror client behavior: stop decoding object on unknown opcode.
                break;
            }
        }
        return def;
    }

    private static void printDef(int objectId, ProbeDef def) {
        System.out.println("objectId=" + objectId);
        System.out.println("name=" + def.name);
        System.out.println("interactType=" + def.interactType + " solid=" + def.solid);
        System.out.println("actions=" + Arrays.toString(def.actions));
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

    private static final class CacheLocData {
        private final byte[] locDat;
        private final int[] offsets;
        private final int total;

        private CacheLocData(byte[] locDat, int[] offsets, int total) {
            this.locDat = locDat;
            this.offsets = offsets;
            this.total = total;
        }
    }

    private static final class ProbeDef {
        private String name = "null";
        private int interactType = -1;
        private boolean solid = true;
        private final String[] actions = new String[5];
    }
}
