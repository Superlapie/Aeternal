package com.runescape.tools;

import com.runescape.cache.FileStore;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

/**
 * Corrected import tool for moon models that doesn't double-gzip the data
 */
public final class CorrectedMoonModelImportTool {

    private CorrectedMoonModelImportTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java com.runescape.tools.CorrectedMoonModelImportTool <flatCacheRoot> <targetCacheDir> <id1,id2,id3...>");
            return;
        }

        String flatRootArg = normalize(args[0]);
        String targetDir = normalize(args[1]);
        File flatRoot = resolveFlatRoot(flatRootArg);
        File modelsDir = new File(flatRoot, "7");
        if (!modelsDir.exists() || !modelsDir.isDirectory()) {
            throw new IllegalStateException("Missing flat-file models directory: " + modelsDir.getAbsolutePath());
        }

        List<Integer> ids;
        if (args.length < 3) {
            System.out.println("No model ids provided.");
            return;
        } else {
            ids = parseIds(args[2]);
            if (ids.isEmpty()) {
                System.out.println("No model ids provided.");
                return;
            }
        }

        File targetDat = resolveDatFile(targetDir);
        File targetIdx1 = new File(targetDir + "main_file_cache.idx1");
        if (!targetDat.exists() || !targetIdx1.exists()) {
            throw new IllegalStateException("Target cache missing main_file_cache.dat/.dat2 or idx1 in " + targetDir);
        }

        try (RandomAccessFile targetDatRaf = new RandomAccessFile(targetDat, "rw");
             RandomAccessFile targetIdxRaf = new RandomAccessFile(targetIdx1, "rw")) {
            FileStore target = new FileStore(targetDatRaf, targetIdxRaf, 2);

            int copied = 0;
            int missing = 0;
            int failed = 0;

            for (int id : ids) {
                File modelFile = new File(modelsDir, id + ".dat");
                if (!modelFile.exists()) {
                    System.out.println("MISS " + id + " (missing flat file)");
                    missing++;
                    continue;
                }

                // Unpack JS5 container and get the raw model data
                byte[] rawContainer = Files.readAllBytes(modelFile.toPath());
                if (rawContainer.length == 0) {
                    System.out.println("MISS " + id + " (empty flat file)");
                    missing++;
                    continue;
                }

                byte[] raw = unpackJs5Container(rawContainer);
                
                // Check if the unpacked data is still compressed (starts with GZIP signature)
                if (raw.length >= 2 && (raw[0] & 0xFF) == 0x1F && (raw[1] & 0xFF) == 0x8B) {
                    // It's still compressed, decompress it
                    raw = gunzip(raw);
                }
                
                boolean ok = target.writeFile(raw.length, raw, id);
                if (ok) {
                    System.out.println("OK   " + id + " (" + raw.length + " raw bytes)");
                    copied++;
                } else {
                    System.out.println("FAIL " + id + " (write error)");
                    failed++;
                }
            }

            System.out.println("Done. copied=" + copied + ", missing=" + missing + ", failed=" + failed);
        }
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

        // Unsupported container type (e.g., bzip2). Keep original bytes.
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
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }

    private static List<Integer> parseIds(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private static String normalize(String dir) {
        return dir.endsWith("/") || dir.endsWith("\\") ? dir : dir + File.separator;
    }

    private static File resolveFlatRoot(String path) {
        File f = new File(path);
        // If we're already pointing to the cache directory, use it directly
        if (f.getName().equals("cache")) {
            return f;
        }
        return f;
    }

    private static File resolveDatFile(String cacheDir) {
        File dat = new File(cacheDir + "main_file_cache.dat");
        if (dat.exists()) {
            return dat;
        }
        return new File(cacheDir + "main_file_cache.dat2");
    }
}
