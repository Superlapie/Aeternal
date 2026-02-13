package com.runescape.tools;

import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.io.Buffer;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scans donor map object archives (idx4) for specific object ids and prints matching regions.
 *
 * Usage:
 * java com.runescape.tools.MapObjectSearchTool <cacheDir> <objectIdCsv>
 */
public final class MapObjectSearchTool {

    private MapObjectSearchTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java com.runescape.tools.MapObjectSearchTool <cacheDir> <objectIdCsv>");
            return;
        }

        String cacheDir = normalize(args[0]);
        Set<Integer> targetObjectIds = Arrays.stream(args[1].split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());

        File dat = resolveDatFile(cacheDir);
        File idx0 = new File(cacheDir + "main_file_cache.idx0");
        File idx4 = new File(cacheDir + "main_file_cache.idx4");
        if (!dat.exists() || !idx0.exists() || !idx4.exists()) {
            throw new IllegalStateException("Missing cache files (.dat/.dat2, idx0, idx4) in " + cacheDir);
        }

        Map<Integer, RegionEntry> regions;
        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "r");
             RandomAccessFile idx0Raf = new RandomAccessFile(idx0, "r")) {
            FileStore store0 = new FileStore(datRaf, idx0Raf, 1);
            byte[] versionListArchive = store0.decompress(5);
            if (versionListArchive == null) {
                throw new IllegalStateException("Could not read versionlist archive.");
            }
            FileArchive versionList = new FileArchive(versionListArchive);
            byte[] mapIndex = versionList.readFile("map_index");
            if (mapIndex == null) {
                throw new IllegalStateException("No map_index in versionlist.");
            }
            regions = parseMapIndex(mapIndex);
        }

        List<String> hits = new ArrayList<>();
        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "r");
             RandomAccessFile idx4Raf = new RandomAccessFile(idx4, "r")) {
            FileStore mapStore = new FileStore(datRaf, idx4Raf, 5);
            for (RegionEntry region : regions.values()) {
                byte[] objectPayload = mapStore.decompress(region.objectArchive);
                if (objectPayload == null || objectPayload.length == 0) {
                    continue;
                }

                if (containsAnyObjectId(objectPayload, targetObjectIds)) {
                    hits.add(region.regionId + " terrain=" + region.terrainArchive + " object=" + region.objectArchive);
                }
            }
        }

        for (String hit : hits) {
            System.out.println(hit);
        }
        System.out.println("matches=" + hits.size());
    }

    private static boolean containsAnyObjectId(byte[] objectData, Set<Integer> targets) {
        Buffer stream = new Buffer(objectData);
        int objectId = -1;
        while (true) {
            int incr = stream.readUSmart();
            if (incr == 0) {
                break;
            }
            objectId += incr;

            int location = 0;
            while (true) {
                int incr2 = stream.readUSmart();
                if (incr2 == 0) {
                    break;
                }
                location += incr2 - 1;
                stream.readUnsignedByte(); // type/rotation hash
            }

            if (targets.contains(objectId)) {
                return true;
            }
        }
        return false;
    }

    private static Map<Integer, RegionEntry> parseMapIndex(byte[] bytes) {
        Map<Integer, RegionEntry> map = new LinkedHashMap<>();
        Buffer buffer = new Buffer(bytes);
        int size = buffer.readUShort();
        for (int i = 0; i < size; i++) {
            int regionId = buffer.readUShort();
            int terrain = buffer.readUShort();
            int object = buffer.readUShort();
            map.put(regionId, new RegionEntry(regionId, terrain, object));
        }
        return map;
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

    private static final class RegionEntry {
        private final int regionId;
        private final int terrainArchive;
        private final int objectArchive;

        private RegionEntry(int regionId, int terrainArchive, int objectArchive) {
            this.regionId = regionId;
            this.terrainArchive = terrainArchive;
            this.objectArchive = objectArchive;
        }
    }
}
