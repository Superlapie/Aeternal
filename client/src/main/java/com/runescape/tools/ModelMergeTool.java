package com.runescape.tools;

import com.runescape.cache.FileStore;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges selected model ids (idx1 store) from a donor 317-style cache into a target cache.
 *
 * Usage:
 * java com.runescape.tools.ModelMergeTool <donorCacheDir> <targetCacheDir> <id1,id2,id3...> [donorIndex]
 *
 * donorIndex defaults to 1 (317-style model index).
 * For OSRS dat2 caches from OpenRS2, models are in idx7.
 */
public final class ModelMergeTool {

    private ModelMergeTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java com.runescape.tools.ModelMergeTool <donorCacheDir> <targetCacheDir> <id1,id2,id3...> [donorIndex]");
            return;
        }

        String donorDir = normalize(args[0]);
        String targetDir = normalize(args[1]);
        List<Integer> ids = parseIds(args[2]);
        int donorIndex = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
        if (donorIndex < 0) {
            throw new IllegalArgumentException("donorIndex must be >= 0");
        }
        if (ids.isEmpty()) {
            System.out.println("No model ids provided.");
            return;
        }

        File donorDat = resolveDatFile(donorDir);
        File donorIdx = new File(donorDir + "main_file_cache.idx" + donorIndex);
        File targetDat = resolveDatFile(targetDir);
        File targetIdx1 = new File(targetDir + "main_file_cache.idx1");

        if (!donorDat.exists() || !donorIdx.exists()) {
            throw new IllegalStateException("Donor cache missing main_file_cache.dat/.dat2 or idx" + donorIndex + " in " + donorDir);
        }
        if (!targetDat.exists() || !targetIdx1.exists()) {
            throw new IllegalStateException("Target cache missing main_file_cache.dat/.dat2 or idx1 in " + targetDir);
        }

        try (RandomAccessFile donorDatRaf = new RandomAccessFile(donorDat, "r");
             RandomAccessFile donorIdxRaf = new RandomAccessFile(donorIdx, "r");
             RandomAccessFile targetDatRaf = new RandomAccessFile(targetDat, "rw");
             RandomAccessFile targetIdxRaf = new RandomAccessFile(targetIdx1, "rw")) {

            int donorStoreType = donorIndex + 1;
            FileStore donor = new FileStore(donorDatRaf, donorIdxRaf, donorStoreType);
            FileStore target = new FileStore(targetDatRaf, targetIdxRaf, 2);

            int copied = 0;
            int missing = 0;
            int failed = 0;

            for (int id : ids) {
                byte[] data = donor.decompress(id);
                if (data == null || data.length == 0) {
                    System.out.println("MISS " + id + " (not found in donor idx1)");
                    missing++;
                    continue;
                }

                boolean ok = target.writeFile(data.length, data, id);
                if (ok) {
                    System.out.println("OK   " + id + " (" + data.length + " bytes)");
                    copied++;
                } else {
                    System.out.println("FAIL " + id + " (write error)");
                    failed++;
                }
            }

            System.out.println("Done. copied=" + copied + ", missing=" + missing + ", failed=" + failed);
        }
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
}
