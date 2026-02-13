package com.runescape.tools;

import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

/**
 * Prints basic config archive stats for a cache directory.
 *
 * Usage:
 * java com.runescape.tools.ConfigProbeTool <cacheDir>
 */
public final class ConfigProbeTool {

    private ConfigProbeTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java com.runescape.tools.ConfigProbeTool <cacheDir>");
            return;
        }

        String cacheDir = normalize(args[0]);
        File dat = resolveDatFile(cacheDir);
        File idx0 = new File(cacheDir + "main_file_cache.idx0");
        if (!dat.exists() || !idx0.exists()) {
            throw new IllegalStateException("Missing cache .dat/.dat2 or idx0 in " + cacheDir);
        }

        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "r");
             RandomAccessFile idx0Raf = new RandomAccessFile(idx0, "r")) {
            FileStore store0 = new FileStore(datRaf, idx0Raf, 1);
            byte[] configArchivePayload = store0.decompress(2);
            if (configArchivePayload == null) {
                throw new IllegalStateException("Could not read idx0 archive 2 (config).");
            }

            System.out.println("config archive bytes=" + configArchivePayload.length + " sha256=" + sha256(configArchivePayload));
            FileArchive archive = new FileArchive(configArchivePayload);

            dumpLoc(archive.readFile("loc.dat"), archive.readFile("loc.idx"));
            dumpFlo(archive.readFile("flo.dat"));
        }
    }

    private static void dumpLoc(byte[] locDat, byte[] locIdx) {
        if (locDat == null || locIdx == null || locIdx.length < 2) {
            System.out.println("loc.dat/loc.idx missing");
            return;
        }
        int total = ((locIdx[0] & 0xFF) << 8) | (locIdx[1] & 0xFF);
        System.out.println("loc.dat bytes=" + locDat.length + " sha256=" + sha256(locDat));
        System.out.println("loc.idx bytes=" + locIdx.length + " sha256=" + sha256(locIdx));
        System.out.println("loc totalObjects=" + total);
    }

    private static void dumpFlo(byte[] floDat) {
        if (floDat == null || floDat.length < 4) {
            System.out.println("flo.dat missing");
            return;
        }
        ByteBuffer bb = ByteBuffer.wrap(floDat);
        int underlays = bb.getShort() & 0xFFFF;
        // This client format stores overlays immediately after underlays in same file.
        int cursor = 2;
        for (int i = 0; i < underlays && cursor < floDat.length; i++) {
            while (cursor < floDat.length) {
                int opcode = floDat[cursor++] & 0xFF;
                if (opcode == 0) {
                    break;
                }
                if (opcode == 1) {
                    cursor += 3;
                }
            }
        }
        int overlays = -1;
        if (cursor + 1 < floDat.length) {
            overlays = ((floDat[cursor] & 0xFF) << 8) | (floDat[cursor + 1] & 0xFF);
        }
        System.out.println("flo.dat bytes=" + floDat.length + " sha256=" + sha256(floDat));
        System.out.println("flo underlays=" + underlays + " overlays=" + overlays);
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "n/a";
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
