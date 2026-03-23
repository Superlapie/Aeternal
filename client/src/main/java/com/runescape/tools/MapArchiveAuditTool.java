package com.runescape.tools;

import com.runescape.cache.FileStore;
import com.runescape.cache.bzip.BZip2Decompressor;
import com.runescape.io.Buffer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Audits map archive readability in idx4 for ids referenced by map_index.
 *
 * Usage:
 * java com.runescape.tools.MapArchiveAuditTool <cacheDir> [mapIndexPath]
 */
public final class MapArchiveAuditTool {

    private MapArchiveAuditTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java com.runescape.tools.MapArchiveAuditTool <cacheDir> [mapIndexPath]");
            return;
        }

        String cacheDir = normalize(args[0]);
        Path mapIndexPath;
        if (args.length >= 2) {
            mapIndexPath = Paths.get(args[1]).toAbsolutePath().normalize();
        } else {
            mapIndexPath = Paths.get(cacheDir, "map_index").toAbsolutePath().normalize();
        }
        if (!Files.exists(mapIndexPath)) {
            throw new IllegalStateException("map_index not found: " + mapIndexPath);
        }

        File dat = resolveDatFile(cacheDir);
        File idx4 = new File(cacheDir + "main_file_cache.idx4");
        if (!dat.exists() || !idx4.exists()) {
            throw new IllegalStateException("Missing cache dat/idx4 in " + cacheDir);
        }

        Set<Integer> ids = parseMapIndexIds(mapIndexPath);
        List<Integer> missing = new ArrayList<>();
        List<Integer> unreadable = new ArrayList<>();

        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "r");
             RandomAccessFile idx4Raf = new RandomAccessFile(idx4, "r")) {
            FileStore mapStore = new FileStore(datRaf, idx4Raf, 5);
            for (int id : ids) {
                byte[] payload = mapStore.decompress(id);
                if (payload == null || payload.length == 0) {
                    missing.add(id);
                    continue;
                }
                if (!isReadableMapArchive(payload)) {
                    unreadable.add(id);
                }
            }
        }

        System.out.println("Checked ids: " + ids.size());
        System.out.println("Missing ids: " + missing.size());
        if (!missing.isEmpty()) {
            System.out.println("Missing list: " + joinCsv(missing));
        }
        System.out.println("Unreadable ids: " + unreadable.size());
        if (!unreadable.isEmpty()) {
            System.out.println("Unreadable list: " + joinCsv(unreadable));
        }
    }

    private static Set<Integer> parseMapIndexIds(Path mapIndexPath) throws Exception {
        byte[] bytes = Files.readAllBytes(mapIndexPath);
        Buffer stream = new Buffer(bytes);
        int count = stream.readUShort();
        Set<Integer> ids = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            stream.readUShort(); // region id
            ids.add(stream.readUShort()); // terrain
            ids.add(stream.readUShort()); // objects
        }
        ids.remove(-1);
        ids.remove(65535);
        return ids;
    }

    private static boolean isReadableMapArchive(byte[] payload) {
        if (canGunzip(payload)) {
            return true;
        }
        byte[] unpacked = unpackJs5Container(payload);
        return unpacked != null;
    }

    private static boolean canGunzip(byte[] payload) {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(payload))) {
            byte[] tmp = new byte[4096];
            while (gis.read(tmp) != -1) {
                // consume
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] unpackJs5Container(byte[] data) {
        if (data == null || data.length < 5) {
            return null;
        }
        int type = data[0] & 0xff;
        int compressedLength = readInt(data, 1);
        if (compressedLength < 0) {
            return null;
        }
        if (type == 0) {
            if (data.length < 5 + compressedLength) {
                return null;
            }
            byte[] out = new byte[compressedLength];
            System.arraycopy(data, 5, out, 0, compressedLength);
            return out;
        }
        if (data.length < 9) {
            return null;
        }
        int decompressedLength = readInt(data, 5);
        if (decompressedLength < 0 || data.length < 9 + compressedLength) {
            return null;
        }
        byte[] out = new byte[decompressedLength];
        if (type == 1) {
            try {
                BZip2Decompressor.decompress(out, decompressedLength, data, compressedLength, 9);
                return out;
            } catch (Exception ex) {
                return null;
            }
        } else if (type == 2) {
            byte[] compressed = new byte[compressedLength];
            System.arraycopy(data, 9, compressed, 0, compressedLength);
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                int read = 0;
                while (read < decompressedLength) {
                    int in = gis.read(out, read, decompressedLength - read);
                    if (in == -1) {
                        break;
                    }
                    read += in;
                }
                if (read == decompressedLength) {
                    return out;
                }
                return null;
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    private static int readInt(byte[] data, int off) {
        if (off + 4 > data.length) {
            return -1;
        }
        return ((data[off] & 0xff) << 24)
                | ((data[off + 1] & 0xff) << 16)
                | ((data[off + 2] & 0xff) << 8)
                | (data[off + 3] & 0xff);
    }

    private static String joinCsv(List<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(ids.get(i));
        }
        return sb.toString();
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

