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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Copies real terrain tiles (all planes) from one world rectangle to another.
 *
 * Usage:
 * java com.runescape.tools.TerrainTileCopyTool
 *   <cacheDir> <serverMapIndexPath> <serverMapsDir>
 *   <srcX> <srcY> <width> <height> <dstX> <dstY>
 */
public final class TerrainTileCopyTool {

    private TerrainTileCopyTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 9) {
            System.out.println("Usage: java com.runescape.tools.TerrainTileCopyTool <cacheDir> <serverMapIndexPath> <serverMapsDir> <srcX> <srcY> <width> <height> <dstX> <dstY>");
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

        Map<Integer, Entry> mapIndex = parseMapIndex(Files.readAllBytes(serverMapIndex));
        Map<Integer, TileData> loaded = new LinkedHashMap<>();

        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "rw");
             RandomAccessFile idx4Raf = new RandomAccessFile(idx4, "rw")) {
            FileStore mapsStore = new FileStore(datRaf, idx4Raf, 5);

            for (int dx = 0; dx < width; dx++) {
                for (int dy = 0; dy < height; dy++) {
                    int sx = srcX + dx;
                    int sy = srcY + dy;
                    int tx = dstX + dx;
                    int ty = dstY + dy;

                    byte[] srcTile = readTile(mapIndex, mapsStore, loaded, sx, sy);
                    writeTile(mapIndex, mapsStore, loaded, tx, ty, srcTile);
                }
            }

            int written = 0;
            for (Map.Entry<Integer, TileData> e : loaded.entrySet()) {
                if (!e.getValue().dirty) {
                    continue;
                }
                byte[] raw = encodeTerrain(e.getValue().tiles);
                byte[] gz = gzip(raw);
                if (!mapsStore.writeFile(gz.length, gz, e.getValue().terrainArchiveId)) {
                    throw new IllegalStateException("Failed writing terrain archive " + e.getValue().terrainArchiveId);
                }
                Files.write(serverMapsDir.resolve(e.getValue().terrainArchiveId + ".dat"), gz);
                written++;
            }

            System.out.println("Copied terrain rectangle " + width + "x" + height
                    + " from (" + srcX + "," + srcY + ") to (" + dstX + "," + dstY + ").");
            System.out.println("Modified terrain archives: " + written);
        }
    }

    private static byte[] readTile(Map<Integer, Entry> mapIndex, FileStore mapsStore, Map<Integer, TileData> loaded, int worldX, int worldY) throws Exception {
        TileRef ref = toTileRef(worldX, worldY);
        TileData td = loadRegionTerrain(mapIndex, mapsStore, loaded, ref.regionId);
        return Arrays.copyOf(td.tiles[ref.plane][ref.localX][ref.localY], td.tiles[ref.plane][ref.localX][ref.localY].length);
    }

    private static void writeTile(Map<Integer, Entry> mapIndex, FileStore mapsStore, Map<Integer, TileData> loaded,
                                  int worldX, int worldY, byte[] tileData) throws Exception {
        TileRef ref = toTileRef(worldX, worldY);
        TileData td = loadRegionTerrain(mapIndex, mapsStore, loaded, ref.regionId);
        td.tiles[ref.plane][ref.localX][ref.localY] = Arrays.copyOf(tileData, tileData.length);
        td.dirty = true;
    }

    private static TileData loadRegionTerrain(Map<Integer, Entry> mapIndex, FileStore mapsStore,
                                              Map<Integer, TileData> loaded, int regionId) throws Exception {
        TileData existing = loaded.get(regionId);
        if (existing != null) {
            return existing;
        }
        Entry entry = mapIndex.get(regionId);
        if (entry == null) {
            throw new IllegalStateException("Region missing in map_index: " + regionId);
        }
        byte[] gz = mapsStore.decompress(entry.terrain);
        if (gz == null || gz.length == 0) {
            throw new IllegalStateException("Missing terrain archive id " + entry.terrain + " for region " + regionId);
        }
        byte[] raw = gunzip(gz);
        byte[][][][] tiles = decodeTerrain(raw);
        TileData td = new TileData(entry.terrain, tiles);
        loaded.put(regionId, td);
        return td;
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

    private static Map<Integer, Entry> parseMapIndex(byte[] bytes) {
        Buffer stream = new Buffer(bytes);
        int count = stream.readUShort();
        Map<Integer, Entry> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            int region = stream.readUShort();
            int terrain = stream.readUShort();
            int object = stream.readUShort();
            map.put(region, new Entry(region, terrain, object));
        }
        return map;
    }

    private static TileRef toTileRef(int worldX, int worldY) {
        int regionX = worldX >> 6;
        int regionY = worldY >> 6;
        int regionId = (regionX << 8) | regionY;
        int localX = worldX & 63;
        int localY = worldY & 63;
        return new TileRef(regionId, 0, localX, localY);
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
        private final int terrain;
        private final int object;

        private Entry(int regionId, int terrain, int object) {
            this.regionId = regionId;
            this.terrain = terrain;
            this.object = object;
        }
    }

    private static final class TileRef {
        private final int regionId;
        private final int plane;
        private final int localX;
        private final int localY;

        private TileRef(int regionId, int plane, int localX, int localY) {
            this.regionId = regionId;
            this.plane = plane;
            this.localX = localX;
            this.localY = localY;
        }
    }

    private static final class TileData {
        private final int terrainArchiveId;
        private final byte[][][][] tiles;
        private boolean dirty;

        private TileData(int terrainArchiveId, byte[][][][] tiles) {
            this.terrainArchiveId = terrainArchiveId;
            this.tiles = tiles;
        }
    }
}

