package com.runescape.tools;

import com.runescape.cache.bzip.BZip2Decompressor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Probes NPC config files directly from an OpenRS2 flat cache.
 *
 * Usage:
 * java com.runescape.tools.FlatNpcConfigProbeTool <flatCacheRoot> <npcIdCsv>
 *
 * Example:
 * java com.runescape.tools.FlatNpcConfigProbeTool ./_ext/openrs2-2446-flat/cache 13593,13594,13595,13596,13597
 */
public final class FlatNpcConfigProbeTool {

    private FlatNpcConfigProbeTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java com.runescape.tools.FlatNpcConfigProbeTool <flatCacheRoot> <npcIdCsv>");
            return;
        }

        Path flatRoot = resolveFlatRoot(args[0]);
        List<Integer> npcIds = Arrays.stream(args[1].split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        Map<Integer, int[]> filesByArchive = loadIndexReference(flatRoot, 2);
        int[] fileIds = filesByArchive.get(9); // 9 = NPC configs in archive 2.
        if (fileIds == null) {
            throw new IllegalStateException("Archive 9 not found in index 2 reference table.");
        }

        Path groupPath = flatRoot.resolve("2").resolve("9.dat");
        if (!Files.exists(groupPath)) {
            throw new IllegalStateException("Missing NPC config group: " + groupPath);
        }

        byte[] groupData = unpackJs5Container(Files.readAllBytes(groupPath));
        Map<Integer, byte[]> files = splitGroup(groupData, fileIds);

        for (int npcId : npcIds) {
            byte[] payload = files.get(npcId);
            if (payload == null) {
                System.out.println("npc " + npcId + " -> missing from group");
                continue;
            }
            ProbeNpc def = decodeNpc(payload);
            System.out.println("npc=" + npcId
                    + " name=\"" + def.name + "\""
                    + " level=" + def.combatLevel
                    + " stand=" + def.standAnim
                    + " walk=" + def.walkAnim
                    + " turn180=" + def.turn180Anim
                    + " turn90cw=" + def.turn90CWAnim
                    + " turn90ccw=" + def.turn90CCWAnim
                    + " models=" + Arrays.toString(def.models));
        }
    }

    private static ProbeNpc decodeNpc(byte[] payload) {
        ProbeNpc out = new ProbeNpc();
        BufferReader reader = new BufferReader(payload);

        while (reader.hasRemaining()) {
            int opcode = reader.readUByte();
            if (opcode == 0) {
                break;
            }
            switch (opcode) {
                case 1: {
                    int count = reader.readUByte();
                    out.models = new int[count];
                    for (int i = 0; i < count; i++) {
                        out.models[i] = reader.readUShort();
                    }
                    break;
                }
                case 2:
                    out.name = reader.readString();
                    break;
                case 12:
                    reader.readUByte();
                    break;
                case 13:
                    out.standAnim = reader.readUShort();
                    break;
                case 14:
                    out.walkAnim = reader.readUShort();
                    break;
                case 15:
                case 16:
                    reader.readUShort();
                    break;
                case 17:
                    out.walkAnim = reader.readUShort();
                    out.turn180Anim = reader.readUShort();
                    out.turn90CWAnim = reader.readUShort();
                    out.turn90CCWAnim = reader.readUShort();
                    break;
                default:
                    if (opcode >= 30 && opcode < 35) {
                        reader.readString();
                        break;
                    }
                    if (opcode == 40 || opcode == 41) {
                        int count = reader.readUByte();
                        for (int i = 0; i < count; i++) {
                            reader.readUShort();
                            reader.readUShort();
                        }
                        break;
                    }
                    if (opcode == 60) {
                        int count = reader.readUByte();
                        for (int i = 0; i < count; i++) {
                            reader.readUShort();
                        }
                        break;
                    }
                    if (opcode == 95) {
                        out.combatLevel = reader.readUShort();
                        break;
                    }
                    if (opcode == 97 || opcode == 98 || opcode == 102 || opcode == 103 || opcode == 114 || opcode == 116) {
                        reader.readUShort();
                        break;
                    }
                    if (opcode == 124) {
                        reader.readUShort();
                        break;
                    }
                    if (opcode == 100 || opcode == 101) {
                        reader.readUByte();
                        break;
                    }
                    if (opcode == 106 || opcode == 118) {
                        reader.readUShort();
                        reader.readUShort();
                        if (opcode == 118) {
                            reader.readUShort();
                        }
                        int count = reader.readUByte();
                        for (int i = 0; i <= count; i++) {
                            reader.readUShort();
                        }
                        break;
                    }
                    if (opcode == 115) {
                        reader.readUShort();
                        reader.readUShort();
                        reader.readUShort();
                        reader.readUShort();
                        break;
                    }
                    if (opcode == 117) {
                        reader.readUShort();
                        reader.readUShort();
                        reader.readUShort();
                        break;
                    }
                    if (opcode == 93 || opcode == 99 || opcode == 107 || opcode == 109 || opcode == 111) {
                        break;
                    }
                    if (opcode == 249) {
                        int count = reader.readUByte();
                        for (int i = 0; i < count; i++) {
                            boolean string = reader.readUByte() == 1;
                            reader.readUByte();
                            reader.readUByte();
                            reader.readUByte();
                            if (string) {
                                reader.readString();
                            } else {
                                reader.readInt();
                            }
                        }
                        break;
                    }
                    throw new IllegalStateException("Unsupported NPC opcode " + opcode + " at pos " + (reader.position() - 1));
            }
        }

        return out;
    }

    private static Path resolveFlatRoot(String rootArg) {
        Path root = Paths.get(rootArg).toAbsolutePath().normalize();
        Path cache = root.resolve("cache");
        return Files.isDirectory(cache) ? cache : root;
    }

    private static Map<Integer, int[]> loadIndexReference(Path flatRoot, int indexId) throws Exception {
        Path referencePath = flatRoot.resolve("255").resolve(indexId + ".dat");
        if (!Files.exists(referencePath)) {
            throw new IllegalStateException("Missing reference table: " + referencePath);
        }

        byte[] referencePayload = unpackJs5Container(Files.readAllBytes(referencePath));
        BufferReader reader = new BufferReader(referencePayload);

        int protocol = reader.readUByte();
        if (protocol < 5 || protocol > 7) {
            throw new IllegalStateException("Unsupported reference protocol: " + protocol);
        }
        if (protocol >= 6) {
            reader.readInt();
        }

        int flags = reader.readUByte();
        boolean named = (flags & 1) != 0;
        boolean sized = (flags & 4) != 0;

        int archiveCount = protocol >= 7 ? reader.readBigSmart() : reader.readUShort();
        int[] archiveIds = new int[archiveCount];
        int lastArchive = 0;
        for (int i = 0; i < archiveCount; i++) {
            int delta = protocol >= 7 ? reader.readBigSmart() : reader.readUShort();
            lastArchive += delta;
            archiveIds[i] = lastArchive;
        }

        if (named) {
            for (int i = 0; i < archiveCount; i++) {
                reader.readInt();
            }
        }

        for (int i = 0; i < archiveCount; i++) {
            reader.readInt();
        }
        if (sized) {
            for (int i = 0; i < archiveCount; i++) {
                reader.readInt();
                reader.readInt();
            }
        }
        for (int i = 0; i < archiveCount; i++) {
            reader.readInt();
        }

        int[] fileCounts = new int[archiveCount];
        for (int i = 0; i < archiveCount; i++) {
            fileCounts[i] = protocol >= 7 ? reader.readBigSmart() : reader.readUShort();
        }

        Map<Integer, int[]> filesByArchive = new HashMap<>();
        for (int i = 0; i < archiveCount; i++) {
            int[] fileIds = new int[fileCounts[i]];
            int lastFile = 0;
            for (int j = 0; j < fileIds.length; j++) {
                int delta = protocol >= 7 ? reader.readBigSmart() : reader.readUShort();
                lastFile += delta;
                fileIds[j] = lastFile;
            }
            filesByArchive.put(archiveIds[i], fileIds);
        }

        return filesByArchive;
    }

    private static Map<Integer, byte[]> splitGroup(byte[] groupData, int[] fileIds) {
        Map<Integer, byte[]> result = new HashMap<>();
        if (fileIds.length == 1) {
            result.put(fileIds[0], groupData);
            return result;
        }

        int chunks = groupData[groupData.length - 1] & 0xFF;
        int fileCount = fileIds.length;
        int tablePos = groupData.length - 1 - chunks * fileCount * 4;
        BufferReader table = new BufferReader(Arrays.copyOfRange(groupData, tablePos, groupData.length - 1));

        int[][] chunkSizes = new int[fileCount][chunks];
        int[] fileSizes = new int[fileCount];
        for (int c = 0; c < chunks; c++) {
            int cumulative = 0;
            for (int f = 0; f < fileCount; f++) {
                cumulative += table.readInt();
                chunkSizes[f][c] = cumulative;
                fileSizes[f] += cumulative;
            }
        }

        byte[][] files = new byte[fileCount][];
        int[] offsets = new int[fileCount];
        for (int i = 0; i < fileCount; i++) {
            files[i] = new byte[fileSizes[i]];
        }

        int pos = 0;
        for (int c = 0; c < chunks; c++) {
            for (int f = 0; f < fileCount; f++) {
                int size = chunkSizes[f][c];
                System.arraycopy(groupData, pos, files[f], offsets[f], size);
                offsets[f] += size;
                pos += size;
            }
        }

        for (int i = 0; i < fileCount; i++) {
            result.put(fileIds[i], files[i]);
        }
        return result;
    }

    private static byte[] unpackJs5Container(byte[] input) throws Exception {
        if (input.length < 5) {
            return input;
        }

        int type = input[0] & 0xFF;
        int compressedLen = readInt(input, 1);
        if (compressedLen < 0) {
            return input;
        }

        if (type == 0) {
            int start = 5;
            if (start + compressedLen > input.length) {
                return input;
            }
            return Arrays.copyOfRange(input, start, start + compressedLen);
        }

        if (input.length < 9) {
            return input;
        }

        if (type == 1) {
            int uncompressedLen = readInt(input, 5);
            int start = 9;
            if (start + compressedLen > input.length || uncompressedLen <= 0) {
                return input;
            }
            byte[] compressed = Arrays.copyOfRange(input, start, start + compressedLen);
            byte[] output = new byte[uncompressedLen];
            BZip2Decompressor.decompress(output, uncompressedLen, compressed, compressedLen, 0);
            return output;
        }

        if (type == 2) {
            int start = 9;
            if (start + compressedLen > input.length) {
                return input;
            }
            return gunzip(Arrays.copyOfRange(input, start, start + compressedLen));
        }

        return input;
    }

    private static byte[] gunzip(byte[] compressed) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 2);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static int readInt(byte[] data, int offset) {
        if (offset + 3 >= data.length) {
            return -1;
        }
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static final class ProbeNpc {
        String name = "";
        int combatLevel = -1;
        int standAnim = -1;
        int walkAnim = -1;
        int turn180Anim = -1;
        int turn90CWAnim = -1;
        int turn90CCWAnim = -1;
        int[] models = new int[0];
    }

    private static final class BufferReader {
        private final byte[] data;
        private int pos;

        private BufferReader(byte[] data) {
            this.data = data;
        }

        private boolean hasRemaining() {
            return pos < data.length;
        }

        private int position() {
            return pos;
        }

        private int readUByte() {
            return data[pos++] & 0xFF;
        }

        private int readUShort() {
            int value = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;
            return value;
        }

        private int readInt() {
            int value = ((data[pos] & 0xFF) << 24)
                    | ((data[pos + 1] & 0xFF) << 16)
                    | ((data[pos + 2] & 0xFF) << 8)
                    | (data[pos + 3] & 0xFF);
            pos += 4;
            return value;
        }

        private int readBigSmart() {
            if ((data[pos] & 0xFF) < 128) {
                return readUShort();
            }
            return readInt() & 0x7FFFFFFF;
        }

        private String readString() {
            int start = pos;
            while (pos < data.length && data[pos] != 0) {
                pos++;
            }
            String out = new String(data, start, Math.max(0, pos - start), StandardCharsets.ISO_8859_1);
            if (pos < data.length) {
                pos++;
            }
            return out;
        }
    }
}
