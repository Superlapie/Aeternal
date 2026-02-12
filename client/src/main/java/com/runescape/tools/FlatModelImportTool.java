package com.runescape.tools;

import com.runescape.cache.FileStore;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Imports model payloads from an extracted OpenRS2 flat-file cache path (cache/7/<id>.dat)
 * into a target 317-style cache idx1 store.
 *
 * Usage:
 * java com.runescape.tools.FlatModelImportTool <flatCacheRoot> <targetCacheDir> <id1,id2,id3...|*|ALL>
 *
 * flatCacheRoot examples:
 * - .../flat/cache
 * - .../flat  (tool will append /cache automatically if present)
 */
public final class FlatModelImportTool {

    private FlatModelImportTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java com.runescape.tools.FlatModelImportTool <flatCacheRoot> <targetCacheDir> <id1,id2,id3...|*|ALL>");
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
        if (args.length < 3 || isAllToken(args[2])) {
            ids = collectAllModelIds(modelsDir);
            if (ids.isEmpty()) {
                System.out.println("No model files found in " + modelsDir.getAbsolutePath());
                return;
            }
            System.out.println("Discovered " + ids.size() + " model ids from flat-file donor.");
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

                byte[] rawContainer = Files.readAllBytes(modelFile.toPath());
                if (rawContainer.length == 0) {
                    System.out.println("MISS " + id + " (empty flat file)");
                    missing++;
                    continue;
                }

                byte[] raw = unpackJs5Container(rawContainer);
                byte[] data = gzip(raw);
                boolean ok = target.writeFile(data.length, data, id);
                if (ok) {
                    System.out.println("OK   " + id + " (" + raw.length + " raw, " + data.length + " gz bytes)");
                    copied++;
                } else {
                    System.out.println("FAIL " + id + " (write error)");
                    failed++;
                }
            }

            System.out.println("Done. copied=" + copied + ", missing=" + missing + ", failed=" + failed);
        }
    }

    private static boolean isAllToken(String token) {
        String t = token == null ? "" : token.trim();
        return "*".equals(t) || "ALL".equalsIgnoreCase(t);
    }

    private static File resolveFlatRoot(String rootArg) {
        File root = new File(rootArg);
        if (!root.exists()) {
            throw new IllegalStateException("Flat cache root does not exist: " + rootArg);
        }
        File cache = new File(root, "cache");
        if (cache.exists() && cache.isDirectory()) {
            return cache;
        }
        return root;
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

    private static List<Integer> parseIds(String csv) {
        List<Integer> ids = new ArrayList<>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ids.add(Integer.parseInt(trimmed));
        }
        return ids;
    }

    private static List<Integer> collectAllModelIds(File modelsDir) {
        List<Integer> ids = new ArrayList<>();
        File[] files = modelsDir.listFiles((dir, name) -> name.endsWith(".dat"));
        if (files == null) {
            return ids;
        }
        for (File file : files) {
            String name = file.getName();
            String idPart = name.substring(0, name.length() - 4);
            try {
                ids.add(Integer.parseInt(idPart));
            } catch (NumberFormatException ignored) {
            }
        }
        ids.sort(Integer::compareTo);
        return ids;
    }

    private static byte[] gzip(byte[] input) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length + 64);
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(input);
        }
        return baos.toByteArray();
    }

    /**
     * OpenRS2 flat-file groups are JS5 containers:
     * type(1) + compressedLen(4) + [uncompressedLen(4)] + payload (+optional version).
     * We need the unpacked group payload before writing to 317 idx1.
     */
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
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }
}
