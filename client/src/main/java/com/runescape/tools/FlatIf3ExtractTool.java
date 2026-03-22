package com.runescape.tools;

import com.runescape.cache.bzip.BZip2Decompressor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Extracts IF3 interface group children from an OpenRS2 flat cache.
 *
 * Usage:
 * java com.runescape.tools.FlatIf3ExtractTool <flatCacheRoot> <interfaceId> <outDir>
 *
 * Example:
 * java com.runescape.tools.FlatIf3ExtractTool ./_ext/openrs2-2446-flat/cache 218 ./_tmp/if3/218
 */
public final class FlatIf3ExtractTool {

    private FlatIf3ExtractTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java com.runescape.tools.FlatIf3ExtractTool <flatCacheRoot> <interfaceId> <outDir>");
            return;
        }

        Path flatRoot = resolveFlatRoot(args[0]);
        int interfaceId = Integer.parseInt(args[1]);
        Path outDir = Paths.get(args[2]).toAbsolutePath().normalize();
        Files.createDirectories(outDir);

        Map<Integer, int[]> reference = loadIndexReference(flatRoot, 3);
        int[] fileIds = reference.get(interfaceId);
        if (fileIds == null) {
            throw new IllegalStateException("Interface group " + interfaceId + " not found in index 3 reference.");
        }

        Path groupPath = flatRoot.resolve("3").resolve(interfaceId + ".dat");
        if (!Files.exists(groupPath)) {
            throw new IllegalStateException("Missing interface group payload: " + groupPath);
        }

        byte[] groupContainer = Files.readAllBytes(groupPath);
        byte[] groupData = unpackJs5Container(groupContainer);
        Map<Integer, byte[]> files = splitGroup(groupData, fileIds);

        StringBuilder summary = new StringBuilder();
        summary.append("interface=").append(interfaceId).append('\n');
        summary.append("children=").append(fileIds.length).append('\n');

        for (int fileId : fileIds) {
            byte[] payload = files.get(fileId);
            if (payload == null) {
                continue;
            }
            Path out = outDir.resolve("if3_" + interfaceId + "_" + fileId + ".dat");
            Files.write(out, payload);
            summary.append(fileId).append('=').append(payload.length).append('\n');
            System.out.println("OK   " + out + " (" + payload.length + " bytes)");
        }

        Path summaryPath = outDir.resolve("if3_" + interfaceId + "_summary.txt");
        Files.writeString(summaryPath, summary.toString());
        System.out.println("Wrote summary: " + summaryPath);
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
            reader.readInt(); // crc
        }
        if (sized) {
            for (int i = 0; i < archiveCount; i++) {
                reader.readInt(); // compressed size
                reader.readInt(); // uncompressed size
            }
        }
        for (int i = 0; i < archiveCount; i++) {
            reader.readInt(); // revision
        }

        int[] fileCounts = new int[archiveCount];
        for (int i = 0; i < archiveCount; i++) {
            fileCounts[i] = protocol >= 7 ? reader.readBigSmart() : reader.readUShort();
        }

        Map<Integer, int[]> filesByArchive = new HashMap<>();
        for (int i = 0; i < archiveCount; i++) {
            int[] fileIds = new int[fileCounts[i]];
            int lastFile = 0;
            for (int j = 0; j < fileCounts[i]; j++) {
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

    private static final class BufferReader {
        private final byte[] data;
        private int pos;

        private BufferReader(byte[] data) {
            this.data = data;
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
    }
}
