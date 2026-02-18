package com.runescape.tools;

import com.runescape.cache.FileStore;
import com.runescape.cache.bzip.BZip2Decompressor;
import com.runescape.io.Buffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Imports raw map archive containers from a flat OpenRS2 cache into client idx4.
 *
 * OpenRS2 map archives live in flat store 5 and some object archives are XTEA-encrypted.
 * This tool decrypts (when keys are available) and rewrites archives in classic gzip payload
 * format expected by the client/server map loaders.
 *
 * Usage:
 * java com.runescape.tools.FlatMapArchiveImportTool <flatCacheRoot> <targetCacheDir> <idCsv|MAPINDEX:<path>> [keysJson]
 */
public final class FlatMapArchiveImportTool {

    private FlatMapArchiveImportTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java com.runescape.tools.FlatMapArchiveImportTool <flatCacheRoot> <targetCacheDir> <idCsv|MAPINDEX:<path>> [keysJson]");
            return;
        }

        Path flatRoot = resolveFlatRoot(args[0]);
        Path donorMaps = flatRoot.resolve("5");
        if (!Files.isDirectory(donorMaps)) {
            throw new IllegalStateException("Missing donor map directory: " + donorMaps);
        }

        Path keysPath;
        if (args.length >= 4) {
            keysPath = Paths.get(args[3]).toAbsolutePath().normalize();
        } else {
            keysPath = Paths.get("_ext", "openrs2-2446-keys.json").toAbsolutePath().normalize();
        }
        Map<Integer, int[]> keysByGroup = loadXteaKeys(keysPath);
        if (!keysByGroup.isEmpty()) {
            System.out.println("Loaded XTEA keys: " + keysByGroup.size() + " map groups");
        } else {
            System.out.println("No XTEA key file loaded (or no keys found): " + keysPath);
        }

        String targetDir = normalize(args[1]);
        File targetDat = resolveDatFile(targetDir);
        File targetIdx4 = new File(targetDir + "main_file_cache.idx4");
        if (!targetDat.exists() || !targetIdx4.exists()) {
            throw new IllegalStateException("Target cache missing .dat/.dat2 or idx4 in " + targetDir);
        }

        Set<Integer> ids = new LinkedHashSet<>();
        if (args[2].startsWith("MAPINDEX:")) {
            Path mapIndexPath = Paths.get(args[2].substring("MAPINDEX:".length())).toAbsolutePath().normalize();
            if (!Files.exists(mapIndexPath)) {
                throw new IllegalStateException("map_index not found: " + mapIndexPath);
            }
            byte[] mapIndexData = Files.readAllBytes(mapIndexPath);
            Buffer stream = new Buffer(mapIndexData);
            int count = stream.readUShort();
            for (int i = 0; i < count; i++) {
                stream.readUShort(); // region id
                ids.add(stream.readUShort()); // terrain archive
                ids.add(stream.readUShort()); // object archive
            }
            ids.remove(-1);
            ids.remove(65535);
            System.out.println("Resolved " + ids.size() + " archive ids from map_index " + mapIndexPath);
        } else {
            String[] idParts = args[2].split(",");
            for (String part : idParts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    ids.add(Integer.parseInt(trimmed));
                }
            }
        }

        int copied = 0;
        int missing = 0;

        try (RandomAccessFile datRaf = new RandomAccessFile(targetDat, "rw");
             RandomAccessFile idxRaf = new RandomAccessFile(targetIdx4, "rw")) {
            FileStore targetStore = new FileStore(datRaf, idxRaf, 5);

            for (int id : ids) {
                Path src = donorMaps.resolve(id + ".dat");
                if (!Files.exists(src)) {
                    missing++;
                    continue;
                }
                byte[] payload = Files.readAllBytes(src);
                if (payload.length == 0) {
                    System.out.println("MISS " + id + " (empty donor)");
                    missing++;
                    continue;
                }
                byte[] converted = convertMapArchive(id, payload, keysByGroup.get(id));
                boolean ok = targetStore.writeFile(converted.length, converted, id);
                if (ok) {
                    copied++;
                    System.out.println("OK   " + id + " (" + payload.length + " -> " + converted.length + " bytes)");
                } else {
                    System.out.println("FAIL " + id + " (write failed)");
                }
            }
        }

        System.out.println("Done. copied=" + copied + ", missing=" + missing);
    }

    private static Path resolveFlatRoot(String arg) {
        Path in = Paths.get(arg).toAbsolutePath().normalize();
        if (Files.isDirectory(in.resolve("cache"))) {
            return in.resolve("cache");
        }
        return in;
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

    private static Map<Integer, int[]> loadXteaKeys(Path keysPath) {
        Map<Integer, int[]> keys = new HashMap<>();
        if (!Files.exists(keysPath)) {
            return keys;
        }
        try {
            String json = new String(Files.readAllBytes(keysPath), StandardCharsets.UTF_8);
            Pattern p = Pattern.compile("\\{[^\\}]*\"archive\"\\s*:\\s*(\\d+)[^\\}]*\"group\"\\s*:\\s*(\\d+)[^\\}]*\"key\"\\s*:\\s*\\[\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\]", Pattern.DOTALL);
            Matcher m = p.matcher(json);
            while (m.find()) {
                int archive = Integer.parseInt(m.group(1));
                if (archive != 5) {
                    continue;
                }
                int group = Integer.parseInt(m.group(2));
                int[] key = new int[]{
                        Integer.parseInt(m.group(3)),
                        Integer.parseInt(m.group(4)),
                        Integer.parseInt(m.group(5)),
                        Integer.parseInt(m.group(6))
                };
                keys.put(group, key);
            }
        } catch (Exception ex) {
            System.out.println("WARN: Failed to load keys file " + keysPath + ": " + ex.getMessage());
        }
        return keys;
    }

    private static byte[] convertMapArchive(int groupId, byte[] payload, int[] xteaKey) {
        if (startsWithGzip(payload)) {
            return payload;
        }
        if (payload.length < 5) {
            return payload;
        }

        int type = payload[0] & 0xff;
        int compressedLength = readInt(payload, 1);
        if (compressedLength < 0) {
            return payload;
        }
        if (type == 0 && payload.length < 5 + compressedLength) {
            return payload;
        }
        if ((type == 1 || type == 2) && payload.length < 9 + compressedLength) {
            return payload;
        }

        byte[] container = Arrays.copyOf(payload, payload.length);
        boolean decrypted = false;
        if (xteaKey != null && shouldAttemptDecrypt(container, type, compressedLength)) {
            xteaDecryptInPlace(container, 5, container.length - 5, xteaKey);
            decrypted = true;
        }

        try {
            byte[] decoded;
            if (type == 0) {
                decoded = Arrays.copyOfRange(container, 5, 5 + compressedLength);
            } else if (type == 1) {
                int decompressedLength = readInt(container, 5);
                if (decompressedLength < 0) {
                    return payload;
                }
                decoded = new byte[decompressedLength];
                BZip2Decompressor.decompress(decoded, decompressedLength, container, compressedLength, 9);
            } else if (type == 2) {
                int decompressedLength = readInt(container, 5);
                if (decompressedLength < 0) {
                    return payload;
                }
                byte[] compressed = Arrays.copyOfRange(container, 9, 9 + compressedLength);
                decoded = gunzip(compressed, decompressedLength);
            } else {
                return payload;
            }

            byte[] out = gzip(decoded);
            if (decrypted) {
                System.out.println("Decrypted map archive " + groupId + " using XTEA key.");
            }
            return out;
        } catch (Exception ex) {
            if (decrypted) {
                System.out.println("WARN: Decrypt/convert failed for map archive " + groupId + ", keeping original payload. " + ex.getMessage());
            }
            return payload;
        }
    }

    private static boolean shouldAttemptDecrypt(byte[] container, int type, int compressedLength) {
        if (type == 2 && container.length >= 11 && (container[9] & 0xff) == 0x1f && (container[10] & 0xff) == 0x8b) {
            return false;
        }
        if (type == 0) {
            return false;
        }
        return compressedLength > 0;
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

    private static boolean startsWithGzip(byte[] data) {
        return data.length >= 2 && (data[0] & 0xff) == 0x1f && (data[1] & 0xff) == 0x8b;
    }

    private static byte[] gunzip(byte[] compressed, int expectedLength) throws Exception {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, expectedLength))) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = gis.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static byte[] gzip(byte[] plain) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(128, plain.length / 2));
             GZIPOutputStream gos = new GZIPOutputStream(out)) {
            gos.write(plain);
            gos.finish();
            return out.toByteArray();
        }
    }

    private static void xteaDecryptInPlace(byte[] data, int offset, int length, int[] key) {
        final int rounds = 32;
        final int delta = 0x9E3779B9;
        int end = offset + (length / 8) * 8;
        for (int pos = offset; pos < end; pos += 8) {
            int v0 = readInt(data, pos);
            int v1 = readInt(data, pos + 4);
            int sum = delta * rounds;
            for (int i = 0; i < rounds; i++) {
                v1 -= (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (sum + key[(sum >>> 11) & 3]);
                sum -= delta;
                v0 -= (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (sum + key[sum & 3]);
            }
            writeInt(data, pos, v0);
            writeInt(data, pos + 4, v1);
        }
    }

    private static void writeInt(byte[] data, int off, int value) {
        data[off] = (byte) (value >> 24);
        data[off + 1] = (byte) (value >> 16);
        data[off + 2] = (byte) (value >> 8);
        data[off + 3] = (byte) value;
    }
}
