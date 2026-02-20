package com.runescape.tools;

import com.runescape.cache.FileStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Generates brand-new map regions (terrain + empty object archives) and wires them into
 * server/client map_index.
 *
 * Terrain is synthetic:
 * - plane 0: underlay + explicit flat height
 * - planes 1..3: empty
 *
 * Usage:
 * java com.runescape.tools.CustomBlankMapRegionTool
 *   <targetCacheDir> <serverMapIndexPath> <serverMapsDir> <startRegionX> <startRegionY> <widthRegions> <heightRegions>
 *   [baseUnderlayId] [pathUnderlayId] [graveUnderlayId] [marshUnderlayId] [ritualUnderlayId]
 */
public final class CustomBlankMapRegionTool {

    private CustomBlankMapRegionTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 7) {
            System.out.println("Usage: java com.runescape.tools.CustomBlankMapRegionTool <targetCacheDir> <serverMapIndexPath> <serverMapsDir> <startRegionX> <startRegionY> <widthRegions> <heightRegions> [baseUnderlayId] [pathUnderlayId] [graveUnderlayId] [marshUnderlayId] [ritualUnderlayId]");
            return;
        }

        String targetDir = normalize(args[0]);
        Path serverMapIndex = Paths.get(args[1]).toAbsolutePath().normalize();
        Path serverMapsDir = Paths.get(args[2]).toAbsolutePath().normalize();
        int startRegionX = Integer.parseInt(args[3]);
        int startRegionY = Integer.parseInt(args[4]);
        int width = Integer.parseInt(args[5]);
        int height = Integer.parseInt(args[6]);
        int baseUnderlayId = args.length >= 8 ? Integer.parseInt(args[7]) : 1;
        int pathUnderlayId = args.length >= 9 ? Integer.parseInt(args[8]) : -1;
        int graveUnderlayId = args.length >= 10 ? Integer.parseInt(args[9]) : -1;
        int marshUnderlayId = args.length >= 11 ? Integer.parseInt(args[10]) : -1;
        int ritualUnderlayId = args.length >= 12 ? Integer.parseInt(args[11]) : -1;

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width/height must be > 0");
        }
        if (!Files.exists(serverMapIndex)) {
            throw new IllegalStateException("Missing server map_index: " + serverMapIndex);
        }
        if (!Files.isDirectory(serverMapsDir)) {
            throw new IllegalStateException("Missing server maps dir: " + serverMapsDir);
        }

        Path clientExternalMapIndex = Paths.get(targetDir, "map_index");
        File targetDat = resolveDatFile(targetDir);
        File targetIdx4 = new File(targetDir + "main_file_cache.idx4");
        if (!targetDat.exists() || !targetIdx4.exists()) {
            throw new IllegalStateException("Target cache missing required files (.dat/.dat2, idx4): " + targetDir);
        }

        Map<Integer, Entry> indexEntries = parseMapIndex(Files.readAllBytes(serverMapIndex));
        Map<Integer, Boolean> usedArchiveIds = new LinkedHashMap<>();
        for (Entry e : indexEntries.values()) {
            usedArchiveIds.put(e.terrain, Boolean.TRUE);
            usedArchiveIds.put(e.object, Boolean.TRUE);
        }

        int nextClassicArchiveId = findNextFreeArchiveId(usedArchiveIds, 1, 3535);
        int nextArchiveId = findNextFreeArchiveId(usedArchiveIds, 3536, 65535);
        List<Entry> generated = new ArrayList<>();
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                int rx = startRegionX + dx;
                int ry = startRegionY + dy;
                int regionId = (rx << 8) | ry;
                Entry existing = indexEntries.get(regionId);
                Entry entry;
                if (existing != null) {
                    int terrainArchive = existing.terrain;
                    int objectArchive = existing.object;
                    boolean needsClassicRemap = terrainArchive > 3535 || objectArchive > 3535;
                    if (needsClassicRemap) {
                        terrainArchive = nextClassicArchiveId;
                        usedArchiveIds.put(terrainArchive, Boolean.TRUE);
                        nextClassicArchiveId = findNextFreeArchiveId(usedArchiveIds, nextClassicArchiveId + 1, 3535);

                        objectArchive = nextClassicArchiveId;
                        usedArchiveIds.put(objectArchive, Boolean.TRUE);
                        nextClassicArchiveId = findNextFreeArchiveId(usedArchiveIds, nextClassicArchiveId + 1, 3535);
                    }
                    entry = new Entry(regionId, terrainArchive, objectArchive);
                } else {
                    int terrainArchive;
                    int objectArchive;
                    if (nextClassicArchiveId > 0) {
                        terrainArchive = nextClassicArchiveId;
                        usedArchiveIds.put(terrainArchive, Boolean.TRUE);
                        nextClassicArchiveId = findNextFreeArchiveId(usedArchiveIds, nextClassicArchiveId + 1, 3535);

                        objectArchive = nextClassicArchiveId;
                        usedArchiveIds.put(objectArchive, Boolean.TRUE);
                        nextClassicArchiveId = findNextFreeArchiveId(usedArchiveIds, nextClassicArchiveId + 1, 3535);
                    } else {
                        terrainArchive = nextArchiveId;
                        usedArchiveIds.put(terrainArchive, Boolean.TRUE);
                        nextArchiveId = findNextFreeArchiveId(usedArchiveIds, nextArchiveId + 1, 65535);

                        objectArchive = nextArchiveId;
                        usedArchiveIds.put(objectArchive, Boolean.TRUE);
                        nextArchiveId = findNextFreeArchiveId(usedArchiveIds, nextArchiveId + 1, 65535);
                    }
                    entry = new Entry(regionId, terrainArchive, objectArchive);
                }
                indexEntries.put(regionId, entry);
                generated.add(entry);
            }
        }

        byte[] terrainRaw = generateTerrainRaw(baseUnderlayId, pathUnderlayId, graveUnderlayId, marshUnderlayId, ritualUnderlayId);
        byte[] terrainGzip = gzip(terrainRaw);
        byte[] objectGzip = gzip(new byte[]{0}); // Empty delta object stream

        try (RandomAccessFile datRaf = new RandomAccessFile(targetDat, "rw");
             RandomAccessFile idx4Raf = new RandomAccessFile(targetIdx4, "rw")) {
            FileStore mapsStore = new FileStore(datRaf, idx4Raf, 5);

            for (Entry e : generated) {
                if (!mapsStore.writeFile(terrainGzip.length, terrainGzip, e.terrain)) {
                    throw new IllegalStateException("Failed writing terrain archive " + e.terrain);
                }
                if (!mapsStore.writeFile(objectGzip.length, objectGzip, e.object)) {
                    throw new IllegalStateException("Failed writing object archive " + e.object);
                }

                Files.write(serverMapsDir.resolve(e.terrain + ".dat"), terrainGzip);
                Files.write(serverMapsDir.resolve(e.object + ".dat"), objectGzip);
            }
        }

        byte[] merged = serializeMapIndex(indexEntries);
        Files.write(serverMapIndex, merged);
        Files.write(clientExternalMapIndex, merged);

        System.out.println("Generated regions: " + generated.size());
        System.out.println("Region block: rx=" + startRegionX + ".." + (startRegionX + width - 1)
                + ", ry=" + startRegionY + ".." + (startRegionY + height - 1));
        System.out.println("Tile bounds: x=" + (startRegionX * 64) + ".." + ((startRegionX + width) * 64 - 1)
                + ", y=" + (startRegionY * 64) + ".." + ((startRegionY + height) * 64 - 1));
        System.out.println("Updated server map_index: " + serverMapIndex);
        System.out.println("Wrote client external map_index: " + clientExternalMapIndex);
    }

    private static byte[] generateTerrainRaw(int baseUnderlayId, int pathUnderlayId, int graveUnderlayId, int marshUnderlayId, int ritualUnderlayId) {
        int baseOpcode = toUnderlayOpcode(baseUnderlayId, "baseUnderlayId");
        int pathOpcode = pathUnderlayId > 0 ? toUnderlayOpcode(pathUnderlayId, "pathUnderlayId") : -1;
        int graveOpcode = graveUnderlayId > 0 ? toUnderlayOpcode(graveUnderlayId, "graveUnderlayId") : -1;
        int marshOpcode = marshUnderlayId > 0 ? toUnderlayOpcode(marshUnderlayId, "marshUnderlayId") : -1;
        int ritualOpcode = ritualUnderlayId > 0 ? toUnderlayOpcode(ritualUnderlayId, "ritualUnderlayId") : -1;

        ByteArrayOutputStream out = new ByteArrayOutputStream(22000);
        for (int plane = 0; plane < 4; plane++) {
            for (int x = 0; x < 64; x++) {
                for (int y = 0; y < 64; y++) {
                    if (plane == 0) {
                        int opcode = baseOpcode;
                        if (graveOpcode > 0 && isGraveTile(x, y)) {
                            opcode = graveOpcode;
                        }
                        if (marshOpcode > 0 && isMarshTile(x, y)) {
                            opcode = marshOpcode;
                        }
                        if (ritualOpcode > 0 && isRitualTile(x, y)) {
                            opcode = ritualOpcode;
                        }
                        if (pathOpcode > 0 && isPathTile(x, y)) {
                            opcode = pathOpcode;
                        }
                        out.write(opcode); // underlay
                        out.write(1);              // explicit height
                        out.write(0);              // flat height sample
                    } else {
                        out.write(0);              // end tile data
                    }
                }
            }
        }
        return out.toByteArray();
    }

    private static int toUnderlayOpcode(int underlayId, String field) {
        int opcode = 81 + underlayId;
        if (opcode < 82 || opcode > 255) {
            throw new IllegalArgumentException(field + " out of supported range: " + underlayId);
        }
        return opcode;
    }

    private static boolean isPathTile(int x, int y) {
        // Broad cross-road and ring so blank regions remain readable and navigable.
        boolean verticalRoad = (x >= 29 && x <= 34);
        boolean horizontalRoad = (y >= 29 && y <= 34);
        boolean ring =
                ((x >= 18 && x <= 45) && (y == 18 || y == 45))
                        || ((y >= 18 && y <= 45) && (x == 18 || x == 45));
        return verticalRoad || horizontalRoad || ring;
    }

    private static boolean isGraveTile(int x, int y) {
        // North cemetery strips and scattered patches.
        boolean northBand = y >= 48 && ((x >= 8 && x <= 22) || (x >= 42 && x <= 56));
        boolean centerIsles = (x >= 24 && x <= 28 && y >= 50) || (x >= 36 && x <= 40 && y >= 50);
        return northBand || centerIsles;
    }

    private static boolean isMarshTile(int x, int y) {
        // Southern marsh with rough edge variation.
        boolean southBand = y <= 15;
        boolean fingers = (y <= 22) && ((x >= 6 && x <= 14) || (x >= 26 && x <= 31) || (x >= 49 && x <= 58));
        boolean noisyPocket = ((x * 13 + y * 7) % 19 == 0) && y <= 24;
        return southBand || fingers || noisyPocket;
    }

    private static boolean isRitualTile(int x, int y) {
        // Mid-map ritual circle + diagonal scar.
        int dx = x - 32;
        int dy = y - 32;
        int distSq = dx * dx + dy * dy;
        boolean ring = distSq >= 144 && distSq <= 196;
        boolean diagonal = Math.abs((x - y) - 2) <= 1 && y >= 22 && y <= 42;
        return ring || diagonal;
    }

    private static byte[] gzip(byte[] data) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(128, data.length / 2));
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(data);
            gz.finish();
        }
        return out.toByteArray();
    }

    private static Map<Integer, Entry> parseMapIndex(byte[] bytes) {
        int count = ((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff);
        int off = 2;
        Map<Integer, Entry> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            int region = ((bytes[off] & 0xff) << 8) | (bytes[off + 1] & 0xff);
            int terrain = ((bytes[off + 2] & 0xff) << 8) | (bytes[off + 3] & 0xff);
            int object = ((bytes[off + 4] & 0xff) << 8) | (bytes[off + 5] & 0xff);
            map.put(region, new Entry(region, terrain, object));
            off += 6;
        }
        return map;
    }

    private static byte[] serializeMapIndex(Map<Integer, Entry> entries) {
        List<Entry> sorted = new ArrayList<>(entries.values());
        sorted.sort(Comparator.comparingInt(a -> a.region));
        byte[] out = new byte[2 + sorted.size() * 6];
        out[0] = (byte) (sorted.size() >> 8);
        out[1] = (byte) sorted.size();
        int off = 2;
        for (Entry e : sorted) {
            out[off++] = (byte) (e.region >> 8);
            out[off++] = (byte) e.region;
            out[off++] = (byte) (e.terrain >> 8);
            out[off++] = (byte) e.terrain;
            out[off++] = (byte) (e.object >> 8);
            out[off++] = (byte) e.object;
        }
        return out;
    }

    private static String normalize(String dir) {
        return dir.endsWith("/") || dir.endsWith("\\") ? dir : dir + File.separator;
    }

    private static int findNextFreeArchiveId(Map<Integer, Boolean> used, int startInclusive, int endInclusive) {
        for (int id = Math.max(1, startInclusive); id <= endInclusive; id++) {
            if (!used.containsKey(id)) {
                return id;
            }
        }
        return -1;
    }

    private static File resolveDatFile(String cacheDir) {
        File dat = new File(cacheDir + "main_file_cache.dat");
        if (dat.exists()) {
            return dat;
        }
        return new File(cacheDir + "main_file_cache.dat2");
    }

    private static final class Entry {
        private final int region;
        private final int terrain;
        private final int object;

        private Entry(int region, int terrain, int object) {
            this.region = region;
            this.terrain = terrain;
            this.object = object;
        }
    }
}
