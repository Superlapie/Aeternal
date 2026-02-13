package com.runescape.tools;

import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts selected files from cache idx0 archive 2 (config) to an output directory.
 *
 * Usage:
 * java com.runescape.tools.ConfigExtractTool <cacheDir> <fileCsv> <outDir>
 */
public final class ConfigExtractTool {

    private ConfigExtractTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java com.runescape.tools.ConfigExtractTool <cacheDir> <fileCsv> <outDir>");
            return;
        }

        String cacheDir = normalize(args[0]);
        List<String> files = Arrays.stream(args[1].split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        Path outDir = Paths.get(args[2]).toAbsolutePath().normalize();
        Files.createDirectories(outDir);

        File dat = resolveDatFile(cacheDir);
        File idx0 = new File(cacheDir + "main_file_cache.idx0");
        if (!dat.exists() || !idx0.exists()) {
            throw new IllegalStateException("Missing cache .dat/.dat2 or idx0 in " + cacheDir);
        }

        byte[] configArchivePayload;
        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "r");
             RandomAccessFile idx0Raf = new RandomAccessFile(idx0, "r")) {
            FileStore store0 = new FileStore(datRaf, idx0Raf, 1);
            configArchivePayload = store0.decompress(2);
        }
        if (configArchivePayload == null || configArchivePayload.length == 0) {
            throw new IllegalStateException("Could not read idx0 archive 2 (config).");
        }

        FileArchive archive = new FileArchive(configArchivePayload);
        int extracted = 0;
        for (String file : files) {
            byte[] data = archive.readFile(file);
            if (data == null) {
                System.out.println("MISS " + file);
                continue;
            }
            Path out = outDir.resolve(file);
            Files.write(out, data);
            System.out.println("OK   " + out + " (" + data.length + " bytes)");
            extracted++;
        }
        System.out.println("Done. extracted=" + extracted + "/" + files.size());
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
