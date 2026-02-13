package com.runescape.tools;

import com.runescape.cache.FileStore;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Copies archive ids between cache stores.
 *
 * Usage:
 * java com.runescape.tools.CacheArchiveCopyTool
 *   <donorCacheDir> <targetCacheDir> <storeIndex> <archiveIdCsv>
 *
 * storeIndex: 0..4 for idx0..idx4
 */
public final class CacheArchiveCopyTool {

    private CacheArchiveCopyTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java com.runescape.tools.CacheArchiveCopyTool <donorCacheDir> <targetCacheDir> <storeIndex> <archiveIdCsv>");
            return;
        }

        String donorDir = normalize(args[0]);
        String targetDir = normalize(args[1]);
        int storeIndex = Integer.parseInt(args[2]);
        List<Integer> archiveIds = parseIds(args[3]);

        if (storeIndex < 0 || storeIndex > 4) {
            throw new IllegalArgumentException("storeIndex must be between 0 and 4");
        }
        if (archiveIds.isEmpty()) {
            throw new IllegalArgumentException("No archive ids supplied.");
        }

        File donorDat = resolveDatFile(donorDir);
        File donorIdx = new File(donorDir + "main_file_cache.idx" + storeIndex);
        File targetDat = resolveDatFile(targetDir);
        File targetIdx = new File(targetDir + "main_file_cache.idx" + storeIndex);
        if (!donorDat.exists() || !donorIdx.exists()) {
            throw new IllegalStateException("Donor cache missing .dat/.dat2 or idx" + storeIndex);
        }
        if (!targetDat.exists() || !targetIdx.exists()) {
            throw new IllegalStateException("Target cache missing .dat/.dat2 or idx" + storeIndex);
        }

        try (RandomAccessFile donorDatRaf = new RandomAccessFile(donorDat, "r");
             RandomAccessFile donorIdxRaf = new RandomAccessFile(donorIdx, "r");
             RandomAccessFile targetDatRaf = new RandomAccessFile(targetDat, "rw");
             RandomAccessFile targetIdxRaf = new RandomAccessFile(targetIdx, "rw")) {

            FileStore donorStore = new FileStore(donorDatRaf, donorIdxRaf, storeIndex + 1);
            FileStore targetStore = new FileStore(targetDatRaf, targetIdxRaf, storeIndex + 1);

            int copied = 0;
            int missing = 0;
            int failed = 0;
            for (int archiveId : archiveIds) {
                byte[] payload = donorStore.decompress(archiveId);
                if (payload == null || payload.length == 0) {
                    System.out.println("MISS " + archiveId);
                    missing++;
                    continue;
                }
                boolean ok = targetStore.writeFile(payload.length, payload, archiveId);
                if (ok) {
                    System.out.println("OK   " + archiveId + " (" + payload.length + " bytes)");
                    copied++;
                } else {
                    System.out.println("FAIL " + archiveId);
                    failed++;
                }
            }
            System.out.println("Done. copied=" + copied + ", missing=" + missing + ", failed=" + failed);
        }
    }

    private static List<Integer> parseIds(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
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
}
