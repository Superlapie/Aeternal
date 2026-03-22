package com.runescape.tools;

import com.runescape.cache.FileStore;
import com.runescape.io.Buffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Copies a source rectangle (terrain + objects) and stamps it 4 times as a 2x2 stitched block.
 *
 * Usage:
 * java com.runescape.tools.MapRectStitchCopyTool
 *   <cacheDir> <serverMapIndexPath> <serverMapsDir>
 *   <srcX> <srcY> <width> <height> <dstX> <dstY>
 */
public final class MapRectStitchCopyTool {

    private MapRectStitchCopyTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 9) {
            System.out.println("Usage: java com.runescape.tools.MapRectStitchCopyTool <cacheDir> <serverMapIndexPath> <serverMapsDir> <srcX> <srcY> <width> <height> <dstX> <dstY>");
            return;
        }

        String cacheDir = normalize(args[0]);
        Path serverMapIndex = Paths.get(args[1]).toAbsolutePath().normalize();
        Path serverMapsDir = Paths.get(args[2]).toAbsolutePath().normalize();
        int srcX = Integer.parseInt(args[3]);
        int srcY = Integer.parseInt(args[4]);
        int width = Integer.parseInt(args[5]);
        int height = Integer.parseInt(args[6]);
        int dstX = Integer.parseInt(args[7]);
        int dstY = Integer.parseInt(args[8]);

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width/height must be > 0");
        }
        if (!Files.exists(serverMapIndex)) {
            throw new IllegalStateException("Missing server map_index: " + serverMapIndex);
        }
        if (!Files.isDirectory(serverMapsDir)) {
            throw new IllegalStateException("Missing server maps dir: " + serverMapsDir);
        }

        File dat = resolveDatFile(cacheDir);
        File idx4 = new File(cacheDir + "main_file_cache.idx4");
        if (!dat.exists() || !idx4.exists()) {
            throw new IllegalStateException("Missing cache dat/idx4 in " + cacheDir);
        }

        Map<Integer, Entry> mapIndex = parseServerMapIndex(Files.readAllBytes(serverMapIndex));
        Map<Integer, RegionData> loaded = new HashMap<>();

        byte[][][][] sourceTiles = new byte[width][height][4][];
        List<ObjPlacement> sourceObjects = new ArrayList<>();

        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "rw");
             RandomAccessFile idx4Raf = new RandomAccessFile(idx4, "rw")) {
            FileStore mapsStore = new FileStore(datRaf, idx4Raf, 5);

            for (int dx = 0; dx < width; dx++) {
                for (int dy = 0; dy < height; dy++) {
                    int wx = srcX + dx;
                    int wy = srcY + dy;
                    TileRef ref = toTileRef(wx, wy);
                    RegionData rd = loadRegionData(mapIndex, mapsStore, loaded, ref.regionId);
                    for (int plane = 0; plane < 4; plane++) {
                        sourceTiles[dx][dy][plane] = Arrays.copyOf(rd.terrain[plane][ref.localX][ref.localY],
                                rd.terrain[plane][ref.localX][ref.localY].length);
                    }
                }
            }

            for (ObjPlacement p : collectObjectsInRect(mapIndex, mapsStore, loaded, srcX, srcY, width, height)) {
                sourceObjects.add(p);
            }

            int[][] offsets = new int[][]{
                    {0, 0},
                    {width, 0},
                    {0, height},
                    {width, height}
            };

            for (int[] off : offsets) {
                int ox = off[0];
                int oy = off[1];

                int blockX = dstX + ox;
                int blockY = dstY + oy;

                for (int dx = 0; dx < width; dx++) {
                    for (int dy = 0; dy < height; dy++) {
                        int tx = blockX + dx;
                        int ty = blockY + dy;
                        TileRef tref = toTileRef(tx, ty);
                        RegionData trd = loadRegionData(mapIndex, mapsStore, loaded, tref.regionId);
                        for (int plane = 0; plane < 4; plane++) {
                            trd.terrain[plane][tref.localX][tref.localY] = Arrays.copyOf(
                                    sourceTiles[dx][dy][plane], sourceTiles[dx][dy][plane].length);
                        }
                        trd.terrainDirty = true;
                    }
                }

                clearObjectsInRect(mapIndex, mapsStore, loaded, blockX, blockY, width, height);
                for (ObjPlacement src : sourceObjects) {
                    int relX = src.x - srcX;
                    int relY = src.y - srcY;
                    int nx = blockX + relX;
                    int ny = blockY + relY;
                    putObject(mapIndex, mapsStore, loaded, new ObjPlacement(src.id, src.plane, nx, ny, src.type, src.rotation));
                }
            }

            int terrainWrites = 0;
            int objectWrites = 0;
            for (Map.Entry<Integer, RegionData> e : loaded.entrySet()) {
                RegionData rd = e.getValue();
                if (rd.terrainDirty) {
                    byte[] terrainRaw = encodeTerrain(rd.terrain);
                    byte[] terrainGz = gzip(terrainRaw);
                    if (!mapsStore.writeFile(terrainGz.length, terrainGz, rd.terrainArchiveId)) {
                        throw new IllegalStateException("Failed writing terrain archive " + rd.terrainArchiveId);
                    }
                    Files.write(serverMapsDir.resolve(rd.terrainArchiveId + ".dat"), terrainGz);
                    terrainWrites++;
                }
                if (rd.objectsDirty) {
                    byte[] objRaw = encodeObjects(rd.objects);
                    byte[] objGz = gzip(objRaw);
                    if (!mapsStore.writeFile(objGz.length, objGz, rd.objectArchiveId)) {
                        throw new IllegalStateException("Failed writing object archive " + rd.objectArchiveId);
                    }
                    Files.write(serverMapsDir.resolve(rd.objectArchiveId + ".dat"), objGz);
                    objectWrites++;
                }
            }

            System.out.println("Copied source rect " + width + "x" + height + " from (" + srcX + "," + srcY + ")");
            System.out.println("Stamped at (" + dstX + "," + dstY + ") as 2x2 stitched block.");
            System.out.println("Modified terrain archives: " + terrainWrites);
            System.out.println("Modified object archives: " + objectWrites);
        }
    }

    private static List<ObjPlacement> collectObjectsInRect(Map<Integer, Entry> mapIndex,
                                                           FileStore mapsStore,
                                                           Map<Integer, RegionData> loaded,
                                                           int minX, int minY, int width, int height) throws Exception {
        int maxX = minX + width - 1;
        int maxY = minY + height - 1;
        List<ObjPlacement> out = new ArrayList<>();
        int minRegionX = minX >> 6;
        int maxRegionX = maxX >> 6;
        int minRegionY = minY >> 6;
        int maxRegionY = maxY >> 6;
        for (int rx = minRegionX; rx <= maxRegionX; rx++) {
            for (int ry = minRegionY; ry <= maxRegionY; ry++) {
                int regionId = (rx << 8) | ry;
                RegionData rd = loadRegionData(mapIndex, mapsStore, loaded, regionId);
                for (ObjPlacement p : rd.objects) {
                    if (p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY) {
                        out.add(p);
                    }
                }
            }
        }
        return out;
    }

    private static void clearObjectsInRect(Map<Integer, Entry> mapIndex,
                                           FileStore mapsStore,
                                           Map<Integer, RegionData> loaded,
                                           int minX, int minY, int width, int height) throws Exception {
        int maxX = minX + width - 1;
        int maxY = minY + height - 1;
        int minRegionX = minX >> 6;
        int maxRegionX = maxX >> 6;
        int minRegionY = minY >> 6;
        int maxRegionY = maxY >> 6;
        for (int rx = minRegionX; rx <= maxRegionX; rx++) {
            for (int ry = minRegionY; ry <= maxRegionY; ry++) {
                int regionId = (rx << 8) | ry;
                RegionData rd = loadRegionData(mapIndex, mapsStore, loaded, regionId);
                int before = rd.objects.size();
                rd.objects.removeIf(p -> p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY);
                if (rd.objects.size() != before) {
                    rd.objectsDirty = true;
                }
            }
        }
    }

    private static void putObject(Map<Integer, Entry> mapIndex,
                                  FileStore mapsStore,
                                  Map<Integer, RegionData> loaded,
                                  ObjPlacement placement) throws Exception {
        int regionId = ((placement.x >> 6) << 8) | (placement.y >> 6);
        RegionData rd = loadRegionData(mapIndex, mapsStore, loaded, regionId);
        rd.objects.add(placement);
        rd.objectsDirty = true;
    }

    private static RegionData loadRegionData(Map<Integer, Entry> mapIndex,
                                             FileStore mapsStore,
                                             Map<Integer, RegionData> loaded,
                                             int regionId) throws Exception {
        RegionData existing = loaded.get(regionId);
        if (existing != null) {
            return existing;
        }
        Entry entry = mapIndex.get(regionId);
        if (entry == null) {
            throw new IllegalStateException("Region missing in server map_index: " + regionId);
        }

        byte[] terrainGz = mapsStore.decompress(entry.terrainArchiveId);
        if (terrainGz == null || terrainGz.length == 0) {
            throw new IllegalStateException("Missing terrain archive " + entry.terrainArchiveId + " for region " + regionId);
        }
        byte[] terrainRaw = gunzipOrRaw(terrainGz);
        byte[][][][] terrain = decodeTerrain(terrainRaw);

        byte[] objGz = mapsStore.decompress(entry.objectArchiveId);
        if (objGz == null || objGz.length == 0) {
            throw new IllegalStateException("Missing object archive " + entry.objectArchiveId + " for region " + regionId);
        }
        byte[] objRaw = gunzipOrRaw(objGz);
        List<ObjPlacement> objects = decodeObjects(objRaw, regionId);

        RegionData rd = new RegionData(regionId, entry.terrainArchiveId, entry.objectArchiveId, terrain, objects);
        loaded.put(regionId, rd);
        return rd;
    }

    private static byte[][][][] decodeTerrain(byte[] raw) {
        byte[][][][] out = new byte[4][64][64][];
        Buffer b = new Buffer(raw);
        for (int plane = 0; plane < 4; plane++) {
            for (int x = 0; x < 64; x++) {
                for (int y = 0; y < 64; y++) {
                    ByteArrayOutputStream tile = new ByteArrayOutputStream(8);
                    while (true) {
                        int opcode = b.readUnsignedByte();
                        tile.write(opcode);
                        if (opcode == 0) {
                            break;
                        }
                        if (opcode == 1) {
                            tile.write(b.readUnsignedByte());
                            break;
                        }
                    }
                    out[plane][x][y] = tile.toByteArray();
                }
            }
        }
        return out;
    }

    private static byte[] encodeTerrain(byte[][][][] tiles) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(22000);
        for (int plane = 0; plane < 4; plane++) {
            for (int x = 0; x < 64; x++) {
                for (int y = 0; y < 64; y++) {
                    out.writeBytes(tiles[plane][x][y]);
                }
            }
        }
        return out.toByteArray();
    }

    private static List<ObjPlacement> decodeObjects(byte[] raw, int regionId) {
        Buffer stream = new Buffer(raw);
        List<ObjPlacement> out = new ArrayList<>();
        int baseX = (regionId >> 8) * 64;
        int baseY = (regionId & 0xFF) * 64;

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
                int localY = location & 0x3F;
                int localX = (location >> 6) & 0x3F;
                int plane = location >> 12;
                int hash = stream.readUnsignedByte();
                int type = hash >> 2;
                int rotation = hash & 0x3;
                out.add(new ObjPlacement(objectId, plane, baseX + localX, baseY + localY, type, rotation));
            }
        }
        return out;
    }

    private static byte[] encodeObjects(List<ObjPlacement> objects) {
        List<ObjPlacement> sorted = new ArrayList<>(objects);
        sorted.sort(Comparator
                .comparingInt((ObjPlacement p) -> p.id)
                .thenComparingInt(p -> p.plane)
                .thenComparingInt(p -> p.x)
                .thenComparingInt(p -> p.y)
                .thenComparingInt(p -> p.type)
                .thenComparingInt(p -> p.rotation));

        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, sorted.size() * 5));
        int lastObjectId = -1;
        int idx = 0;
        while (idx < sorted.size()) {
            ObjPlacement first = sorted.get(idx);
            writeUSmart(out, first.id - lastObjectId);
            lastObjectId = first.id;

            int lastLocation = 0;
            while (idx < sorted.size() && sorted.get(idx).id == first.id) {
                ObjPlacement p = sorted.get(idx);
                int localX = p.x & 0x3F;
                int localY = p.y & 0x3F;
                int location = (p.plane << 12) | (localX << 6) | localY;
                writeUSmart(out, (location - lastLocation) + 1);
                out.write((p.type << 2) | (p.rotation & 0x3));
                lastLocation = location;
                idx++;
            }
            writeUSmart(out, 0);
        }
        writeUSmart(out, 0);
        return out.toByteArray();
    }

    private static void writeUSmart(ByteArrayOutputStream out, int value) {
        if (value < 128) {
            out.write(value);
        } else {
            int v = value + 32768;
            out.write((v >> 8) & 0xFF);
            out.write(v & 0xFF);
        }
    }

    private static Map<Integer, Entry> parseServerMapIndex(byte[] bytes) {
        Map<Integer, Entry> out = new LinkedHashMap<>();
        int start = 0;
        int count;
        if (bytes.length % 6 == 0) {
            count = bytes.length / 6;
        } else if ((bytes.length - 2) % 6 == 0) {
            count = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
            start = 2;
        } else {
            throw new IllegalStateException("Invalid map_index length: " + bytes.length);
        }
        for (int i = 0; i < count; i++) {
            int off = start + i * 6;
            int region = ((bytes[off] & 0xFF) << 8) | (bytes[off + 1] & 0xFF);
            int terrain = ((bytes[off + 2] & 0xFF) << 8) | (bytes[off + 3] & 0xFF);
            int object = ((bytes[off + 4] & 0xFF) << 8) | (bytes[off + 5] & 0xFF);
            out.put(region, new Entry(region, terrain, object));
        }
        return out;
    }

    private static TileRef toTileRef(int worldX, int worldY) {
        int regionX = worldX >> 6;
        int regionY = worldY >> 6;
        int regionId = (regionX << 8) | regionY;
        return new TileRef(regionId, worldX & 63, worldY & 63);
    }

    private static byte[] gunzipOrRaw(byte[] data) throws Exception {
        try {
            return gunzip(data);
        } catch (Exception ignored) {
            return data;
        }
    }

    private static byte[] gunzip(byte[] gz) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gz));
             ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(4096, gz.length * 2))) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static byte[] gzip(byte[] raw) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(4096, raw.length / 2));
             GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(raw);
            gz.finish();
            return out.toByteArray();
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

    private static final class Entry {
        private final int regionId;
        private final int terrainArchiveId;
        private final int objectArchiveId;

        private Entry(int regionId, int terrainArchiveId, int objectArchiveId) {
            this.regionId = regionId;
            this.terrainArchiveId = terrainArchiveId;
            this.objectArchiveId = objectArchiveId;
        }
    }

    private static final class TileRef {
        private final int regionId;
        private final int localX;
        private final int localY;

        private TileRef(int regionId, int localX, int localY) {
            this.regionId = regionId;
            this.localX = localX;
            this.localY = localY;
        }
    }

    private static final class ObjPlacement {
        private final int id;
        private final int plane;
        private final int x;
        private final int y;
        private final int type;
        private final int rotation;

        private ObjPlacement(int id, int plane, int x, int y, int type, int rotation) {
            this.id = id;
            this.plane = plane;
            this.x = x;
            this.y = y;
            this.type = type;
            this.rotation = rotation;
        }
    }

    private static final class RegionData {
        private final int regionId;
        private final int terrainArchiveId;
        private final int objectArchiveId;
        private final byte[][][][] terrain;
        private final List<ObjPlacement> objects;
        private boolean terrainDirty;
        private boolean objectsDirty;

        private RegionData(int regionId, int terrainArchiveId, int objectArchiveId, byte[][][][] terrain, List<ObjPlacement> objects) {
            this.regionId = regionId;
            this.terrainArchiveId = terrainArchiveId;
            this.objectArchiveId = objectArchiveId;
            this.terrain = terrain;
            this.objects = objects;
        }
    }
}
