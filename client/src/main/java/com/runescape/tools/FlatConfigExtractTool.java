package com.runescape.tools;

import com.runescape.cache.FileArchive;
import com.runescape.cache.bzip.BZip2Decompressor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Extracts selected config files from a flat cache's store 0 archive.
 *
 * Usage:
 * java com.runescape.tools.FlatConfigExtractTool <flatCacheDir> <archiveId> <fileCsv> <outDir>
 *
 * Example:
 * java com.runescape.tools.FlatConfigExtractTool ./_ext/openrs2-2446-flat/cache 2 seq.dat,spotanim.dat ./client/Cache
 */
public final class FlatConfigExtractTool {

    private FlatConfigExtractTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java com.runescape.tools.FlatConfigExtractTool <flatCacheDir> <archiveId> <fileCsv> <outDir>");
            return;
        }

        Path flatCacheDir = Paths.get(args[0]).toAbsolutePath().normalize();
        int archiveId = Integer.parseInt(args[1]);
        List<String> files = Arrays.stream(args[2].split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        Path outDir = Paths.get(args[3]).toAbsolutePath().normalize();
        Files.createDirectories(outDir);

        Path archivePath = flatCacheDir.resolve("0").resolve(archiveId + ".dat");
        if (!Files.exists(archivePath)) {
            throw new IllegalStateException("Missing flat archive: " + archivePath);
        }

        byte[] archiveContainer = Files.readAllBytes(archivePath);
        if (archiveContainer.length == 0) {
            throw new IllegalStateException("Flat archive is empty: " + archivePath);
        }

        byte[] archivePayload = unpackJs5Container(archiveContainer);
        FileArchive archive;
        try {
            archive = new FileArchive(archivePayload);
        } catch (Exception first) {
            if (archivePayload.length <= 2) {
                throw first;
            }
            byte[] trimmed = Arrays.copyOf(archivePayload, archivePayload.length - 2);
            archive = new FileArchive(trimmed);
            System.out.println("Retried archive parse after trimming JS5 version footer.");
        }
        int extracted = 0;
        for (String file : files) {
            byte[] data = archive.readFile(file);
            if (data == null) {
                System.out.println("MISS " + file);
                continue;
            }

            Path out = outDir.resolve(file);
            Files.write(out, data);
            System.out.println("OK   " + out + " (" + data.length + " bytes)");
            extracted++;
        }

        System.out.println("Done. extracted=" + extracted + "/" + files.size());
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

        if (type == 2) {
            if (input.length < 9) {
                return input;
            }
            int start = 9;
            if (start + compressedLen > input.length) {
                return input;
            }
            byte[] compressed = Arrays.copyOfRange(input, start, start + compressedLen);
            return gunzip(compressed);
        }

        if (type == 1) {
            if (input.length < 9) {
                return input;
            }
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

        // Keep unsupported types unchanged.
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
}
