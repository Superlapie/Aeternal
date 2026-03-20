package com.runescape.sign;

import com.runescape.Configuration;

import java.applet.Applet;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SignLink {

    public static final RandomAccessFile[] indices = new RandomAccessFile[5];
    public static RandomAccessFile cache_dat = null;
    public static Applet mainapp = null;
    private static volatile String activeCacheDirectory = null;
    public static String os;
    public static String arch;
    public static EventQueue eventQueue;
    
    private SignLink() {
    }

    public static void init(Applet px) {

        System.setProperty("java.net.preferIPv4Stack", "true");

        mainapp = px;
       
        String directory = findcachedir();
        try {
            openCacheFiles(directory);
        } catch (Exception primaryException) {
            String fallbackDirectory = prepareFallbackCacheDir();
            if (!fallbackDirectory.equals(directory)) {
                try {
                    openCacheFiles(fallbackDirectory);
                } catch (Exception fallbackException) {
                    fallbackException.printStackTrace();
                }
            } else {
                primaryException.printStackTrace();
            }
        }
        
        try {
            eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        } catch (Throwable t) {
        }
        try {
            ThreadGroup t = Thread.currentThread().getThreadGroup();
            do {
                ThreadGroup t1 = t.getParent();
                if (t1 == null)
                    break;

                t = t1;
            } while (true);
            int n = t.activeCount();
            if (n > 0) {
                Thread[] h = new Thread[n];
                n = t.enumerate(h);
                if (n > 0)
                    for (int n1 = 0; n1 != n; ++n1) {
                        Thread r = h[n1];
                        if (r == null)
                            continue;

                        try {
                            String s = r.getName();
                            if (s != null && s.startsWith("AWT"))
                                r.setPriority(1);
                        } catch (Throwable w) {
                        }
                    }
            }
        } catch (Throwable t) {
        }
        os = null;
        try {
            os = System.getProperty("os.name").toLowerCase();
        } catch (Throwable ex) {
        }
        arch = null;
        try {
            arch = System.getProperty("os.arch").toLowerCase();
        } catch (Throwable ex) {
        }
    }

    public static String findcachedir() {
        if (activeCacheDirectory != null) {
            return activeCacheDirectory;
        }
        final File primary = new File(Configuration.CACHE_DIRECTORY);
        if (ensureWritableDirectory(primary)) {
            return withTrailingSeparator(primary);
        }

        return prepareFallbackCacheDir();
    }

    private static String getFallbackCacheDir() {
        return withTrailingSeparator(new File(System.getProperty("user.home"), "OSRSRSPS-Cache"));
    }

    private static String prepareFallbackCacheDir() {
        final File primary = new File(Configuration.CACHE_DIRECTORY);
        final File fallback = new File(getFallbackCacheDir());
        if (ensureWritableDirectory(fallback)) {
            // Copy core cache stores only when missing in fallback.
            // Replacing them on each boot can wipe runtime/imported cache edits.
            syncCacheFileIfMissing(primary.toPath(), fallback.toPath(), "main_file_cache.dat");
            for (int i = 0; i < 5; i++) {
                syncCacheFileIfMissing(primary.toPath(), fallback.toPath(), "main_file_cache.idx" + i);
            }
            syncCacheFileIfMissing(primary.toPath(), fallback.toPath(), "map_index");
            syncCacheFileIfMissing(primary.toPath(), fallback.toPath(), "flo.dat");
            syncCacheFileIfMissing(primary.toPath(), fallback.toPath(), "loc.dat");
            syncCacheFileIfMissing(primary.toPath(), fallback.toPath(), "seq.dat");
            syncCacheFileIfMissing(primary.toPath(), fallback.toPath(), "spotanim.dat");
        }
        return withTrailingSeparator(fallback);
    }

    private static void openCacheFiles(String directory) throws IOException {
        cache_dat = new RandomAccessFile(directory + "main_file_cache.dat", "rw");
        for (int index = 0; index < 5; index++) {
            indices[index] = new RandomAccessFile(directory + "main_file_cache.idx" + index, "rw");
        }
        activeCacheDirectory = directory;
        System.out.println("Active cache directory: " + directory);
    }

    private static boolean ensureWritableDirectory(File directory) {
        if (!directory.exists() && !directory.mkdirs()) {
            return false;
        }
        if (!directory.isDirectory()) {
            return false;
        }
        File probe = new File(directory, ".write_test");
        try {
            if (probe.exists() || probe.createNewFile()) {
                probe.delete();
                return true;
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private static void syncCacheFile(Path sourceDir, Path targetDir, String fileName) {
        try {
            Path source = sourceDir.resolve(fileName);
            Path target = targetDir.resolve(fileName);
            if (Files.exists(source)) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }

    private static void syncCacheFileIfMissing(Path sourceDir, Path targetDir, String fileName) {
        try {
            Path source = sourceDir.resolve(fileName);
            Path target = targetDir.resolve(fileName);
            if (Files.exists(source) && !Files.exists(target)) {
                Files.copy(source, target);
            }
        } catch (IOException ignored) {
        }
    }

    private static String withTrailingSeparator(File directory) {
        String path = directory.getPath();
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
    }
}
