package com.runescape.tools;

import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.io.Buffer;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads map_index from cache idx0 archive id 5 ("versionlist") and prints entries.
 *
 * Usage:
 * java com.runescape.tools.MapIndexProbeTool <cacheDir> [regionIdCsv]
 */
public final class MapIndexProbeTool {

    private MapIndexProbeTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java com.runescape.tools.MapIndexProbeTool <cacheDir> [regionIdCsv]");
            return;
        }

        String cacheDir = normalize(args[0]);
        Set<Integer> filter = new HashSet<>();
        if (args.length >= 2 && !args[1].trim().isEmpty()) {
            Arrays.stream(args[1].split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .forEach(filter::add);
        }

        File dat = resolveDatFile(cacheDir);
        File idx0 = new File(cacheDir + "main_file_cache.idx0");
        if (!dat.exists() || !idx0.exists()) {
            throw new IllegalStateException("Missing cache files in " + cacheDir);
        }

        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "r");
             RandomAccessFile idx0Raf = new RandomAccessFile(idx0, "r")) {
            FileStore index0 = new FileStore(datRaf, idx0Raf, 1);
            byte[] versionListArchive = index0.decompress(5);
            if (versionListArchive == null) {
                throw new IllegalStateException("Failed to read idx0 archive id 5 (versionlist).");
            }

            FileArchive archive = new FileArchive(versionListArchive);
            byte[] mapIndex = archive.readFile("map_index");
            if (mapIndex == null) {
                throw new IllegalStateException("versionlist archive has no map_index.");
            }

            Buffer buffer = new Buffer(mapIndex);
            int count = buffer.readUShort();
            int matches = 0;
            for (int i = 0; i < count; i++) {
                int regionId = buffer.readUShort();
                int terrain = buffer.readUShort();
                int object = buffer.readUShort();
                if (filter.isEmpty() || filter.contains(regionId)) {
                    System.out.println(regionId + "," + terrain + "," + object);
                    matches++;
                }
            }
            System.out.println("entries=" + count + ", matched=" + matches);
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
}
