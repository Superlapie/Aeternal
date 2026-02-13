package com.runescape.tools;

import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.io.Buffer;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Imports map regions from a donor cache into target cache idx4 and server clipping map_index/maps.
 *
 * Usage:
 * java com.runescape.tools.MapRegionImportTool
 *   <donorCacheDir> <targetCacheDir> <serverMapIndexPath> <serverMapsDir> <regionIdCsv>
 */
public final class MapRegionImportTool {

    private MapRegionImportTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("Usage: java com.runescape.tools.MapRegionImportTool <donorCacheDir> <targetCacheDir> <serverMapIndexPath> <serverMapsDir> <regionIdCsv>");
            return;
        }

        String donorDir = normalize(args[0]);
        String targetDir = normalize(args[1]);
        Path serverMapIndex = Paths.get(args[2]);
        Path serverMapsDir = Paths.get(args[3]);
        Set<Integer> regionIds = parseIds(args[4]);
        if (regionIds.isEmpty()) {
            throw new IllegalArgumentException("No region ids supplied.");
        }

        Path clientExternalMapIndex = Paths.get(targetDir, "map_index");

        File donorDat = resolveDatFile(donorDir);
        File donorIdx0 = new File(donorDir + "main_file_cache.idx0");
        File donorIdx4 = new File(donorDir + "main_file_cache.idx4");
        File targetDat = resolveDatFile(targetDir);
        File targetIdx4 = new File(targetDir + "main_file_cache.idx4");

        if (!donorDat.exists() || !donorIdx0.exists() || !donorIdx4.exists()) {
            throw new IllegalStateException("Donor cache missing required files (.dat/.dat2, idx0, idx4): " + donorDir);
        }
        if (!targetDat.exists() || !targetIdx4.exists()) {
            throw new IllegalStateException("Target cache missing required files (.dat/.dat2, idx4): " + targetDir);
        }
        if (!Files.exists(serverMapIndex)) {
            throw new IllegalStateException("Missing server map_index: " + serverMapIndex);
        }
        if (!Files.isDirectory(serverMapsDir)) {
            throw new IllegalStateException("Missing server maps dir: " + serverMapsDir);
        }

        byte[] donorMapIndexBytes;
        try (RandomAccessFile donorDatRaf = new RandomAccessFile(donorDat, "r");
             RandomAccessFile donorIdx0Raf = new RandomAccessFile(donorIdx0, "r")) {
            FileStore donorIndex0 = new FileStore(donorDatRaf, donorIdx0Raf, 1);
            byte[] versionListArchive = donorIndex0.decompress(5);
            if (versionListArchive == null) {
                throw new IllegalStateException("Failed to read donor versionlist (idx0 archive id 5).");
            }
            FileArchive archive = new FileArchive(versionListArchive);
            donorMapIndexBytes = archive.readFile("map_index");
            if (donorMapIndexBytes == null) {
                throw new IllegalStateException("Donor versionlist has no map_index.");
            }
        }

        Map<Integer, RegionEntry> donorEntries = parseMapIndex(donorMapIndexBytes);
        List<RegionEntry> selected = new ArrayList<>();
        for (int regionId : regionIds) {
            RegionEntry entry = donorEntries.get(regionId);
            if (entry == null) {
                throw new IllegalStateException("Donor map_index missing region " + regionId);
            }
            selected.add(entry);
        }

        Set<Integer> archiveIds = new LinkedHashSet<>();
        for (RegionEntry entry : selected) {
            archiveIds.add(entry.terrain);
            archiveIds.add(entry.object);
        }

        try (RandomAccessFile donorDatRaf = new RandomAccessFile(donorDat, "r");
             RandomAccessFile donorIdx4Raf = new RandomAccessFile(donorIdx4, "r");
             RandomAccessFile targetDatRaf = new RandomAccessFile(targetDat, "rw");
             RandomAccessFile targetIdx4Raf = new RandomAccessFile(targetIdx4, "rw")) {

            FileStore donorMaps = new FileStore(donorDatRaf, donorIdx4Raf, 5);
            FileStore targetMaps = new FileStore(targetDatRaf, targetIdx4Raf, 5);

            int copiedToCache = 0;
            int copiedToServer = 0;
            for (int archiveId : archiveIds) {
                byte[] payload = donorMaps.decompress(archiveId);
                if (payload == null || payload.length == 0) {
                    throw new IllegalStateException("Donor idx4 missing archive id " + archiveId);
                }

                if (!targetMaps.writeFile(payload.length, payload, archiveId)) {
                    throw new IllegalStateException("Failed writing archive " + archiveId + " to target idx4.");
                }
                copiedToCache++;

                Files.write(serverMapsDir.resolve(archiveId + ".dat"), payload);
                copiedToServer++;
            }

            Map<Integer, RegionEntry> serverEntries = parseMapIndex(Files.readAllBytes(serverMapIndex));
            for (RegionEntry entry : selected) {
                serverEntries.put(entry.regionId, entry);
            }
            byte[] merged = serializeMapIndex(serverEntries);
            Files.write(serverMapIndex, merged);
            Files.write(clientExternalMapIndex, merged);

            System.out.println("Imported regions: " + selected.size());
            System.out.println("Archives copied to target idx4: " + copiedToCache);
            System.out.println("Archives copied to server maps/: " + copiedToServer);
            System.out.println("Updated server map_index: " + serverMapIndex);
            System.out.println("Wrote client external map_index: " + clientExternalMapIndex);
        }
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

    private static byte[] serializeMapIndex(Map<Integer, RegionEntry> entries) {
        List<RegionEntry> sorted = new ArrayList<>(entries.values());
        sorted.sort(Comparator.comparingInt(a -> a.regionId));

        byte[] out = new byte[2 + sorted.size() * 6];
        out[0] = (byte) (sorted.size() >> 8);
        out[1] = (byte) sorted.size();
        int off = 2;
        for (RegionEntry e : sorted) {
            out[off++] = (byte) (e.regionId >> 8);
            out[off++] = (byte) e.regionId;
            out[off++] = (byte) (e.terrain >> 8);
            out[off++] = (byte) e.terrain;
            out[off++] = (byte) (e.object >> 8);
            out[off++] = (byte) e.object;
        }
        return out;
    }

    private static Set<Integer> parseIds(String csv) {
        Set<Integer> ids = new LinkedHashSet<>();
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt)
                .forEach(ids::add);
        return ids;
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
        private final int terrain;
        private final int object;

        private RegionEntry(int regionId, int terrain, int object) {
            this.regionId = regionId;
            this.terrain = terrain;
            this.object = object;
        }
    }
}
