package com.runescape.tools;

import com.runescape.cache.FileStore;
import com.runescape.entity.model.Model;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.RandomAccessFile;
import java.util.zip.GZIPInputStream;

/**
 * Analyzes the format of specific models to understand compatibility issues
 */
public final class ModelFormatAnalyzer {

    private ModelFormatAnalyzer() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("  java com.runescape.tools.ModelFormatAnalyzer <cacheDir> <modelId>");
            System.out.println("  java com.runescape.tools.ModelFormatAnalyzer flat <flatCacheRoot> <modelId>");
            return;
        }

        if ("flat".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                System.out.println("Usage: java com.runescape.tools.ModelFormatAnalyzer flat <flatCacheRoot> <modelId>");
                return;
            }
            analyzeFlat(args[1], Integer.parseInt(args[2]));
            return;
        }

        String cacheDir = args[0];
        int modelId = Integer.parseInt(args[1]);

        // Try to load the model from idx1
        File datFile = new File(cacheDir + "main_file_cache.dat");
        if (!datFile.exists()) {
            datFile = new File(cacheDir + "main_file_cache.dat2");
        }

        File idxFile = new File(cacheDir + "main_file_cache.idx1");
        if (!datFile.exists() || !idxFile.exists()) {
            System.out.println("Cache files not found");
            return;
        }

        try (RandomAccessFile datRaf = new RandomAccessFile(datFile, "r");
             RandomAccessFile idxRaf = new RandomAccessFile(idxFile, "r")) {

            FileStore store = new FileStore(datRaf, idxRaf, 2); // Store 1 for models, +1 for FileStore
            byte[] modelData = store.decompress(modelId);

            if (modelData == null) {
                System.out.println("Model " + modelId + " not found");
                return;
            }

            System.out.println("Model " + modelId + " analysis:");
            System.out.println("Size: " + modelData.length + " bytes");
            
            if (modelData.length >= 2) {
                int lastByte = modelData[modelData.length - 1] & 0xFF;
                int secondLastByte = modelData[modelData.length - 2] & 0xFF;
                
                System.out.println("Last two bytes: " + secondLastByte + ", " + lastByte);
                
                if (lastByte == -3 && secondLastByte == -1) {
                    System.out.println("Format: Type 3 (newer format)");
                } else if (lastByte == -2 && secondLastByte == -1) {
                    System.out.println("Format: Type 2");
                } else if (lastByte == -1 && secondLastByte == -1) {
                    System.out.println("Format: New");
                } else {
                    System.out.println("Format: Old (legacy)");
                }
                
                // Show first few bytes for additional analysis
                System.out.print("First 10 bytes: ");
                for (int i = 0; i < Math.min(10, modelData.length); i++) {
                    System.out.print((modelData[i] & 0xFF) + " ");
                }
                System.out.println();
            }

            // Decode probe: emulate client load path and verify vertex/triangle counts.
            try {
                byte[] decoded = maybeGunzip(modelData);
                Model.init();
                Model.method460(decoded, modelId);
                Model m = Model.getModel(modelId);
                if (m == null) {
                    System.out.println("Decode probe: NULL model");
                } else {
                    System.out.println("Decode probe: vertices=" + m.numVertices + ", triangles=" + m.numTriangles);
                }
            } catch (Throwable t) {
                System.out.println("Decode probe failed: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        }
    }

    private static void analyzeFlat(String flatCacheRoot, int modelId) throws Exception {
        Path root = Path.of(flatCacheRoot);
        Path models = root.getFileName().toString().equals("cache") ? root.resolve("7") : root.resolve("cache").resolve("7");
        Path file = models.resolve(modelId + ".dat");
        if (!Files.exists(file)) {
            System.out.println("Flat model not found: " + file);
            return;
        }
        byte[] container = Files.readAllBytes(file);
        byte[] unpacked = unpackJs5Container(container);
        if (unpacked == null) {
            unpacked = container;
        }

        System.out.println("Flat model " + modelId + " analysis:");
        System.out.println("Container size: " + container.length + " bytes");
        System.out.println("Unpacked size: " + unpacked.length + " bytes");

        decodeProbe(modelId, unpacked);
    }

    private static void decodeProbe(int modelId, byte[] modelData) {
        try {
            byte[] decoded = maybeGunzip(modelData);
            Model.init();
            Model.method460(decoded, modelId);
            Model m = Model.getModel(modelId);
            if (m == null) {
                System.out.println("Decode probe: NULL model");
            } else {
                System.out.println("Decode probe: vertices=" + m.numVertices + ", triangles=" + m.numTriangles);
            }
        } catch (Throwable t) {
            System.out.println("Decode probe failed: " + t.getClass().getSimpleName() + " - " + t.getMessage());
        }
    }

    private static byte[] unpackJs5Container(byte[] data) {
        if (data == null || data.length < 5) {
            return null;
        }
        int type = data[0] & 0xFF;
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
        if (type != 2 || data.length < 9 || data.length < 9 + compressedLength) {
            return null;
        }
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data, 9, compressedLength));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = gis.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] maybeGunzip(byte[] data) throws Exception {
        if (data == null || data.length < 2) {
            return data;
        }
        int offset = 0;
        if (data.length > 12 && data[0] == 2 && (data[9] & 0xFF) == 0x1F && (data[10] & 0xFF) == 0x8B) {
            offset = 9;
        }
        if ((data[offset] & 0xFF) == 0x1F && (data[offset + 1] & 0xFF) == 0x8B) {
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data, offset, data.length - offset));
                 ByteArrayOutputStream out = new ByteArrayOutputStream(data.length * 2)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            }
        }
        return data;
    }

    private static int readInt(byte[] data, int offset) {
        if (offset + 4 > data.length) {
            return -1;
        }
        return ((data[offset] & 0xff) << 24)
                | ((data[offset + 1] & 0xff) << 16)
                | ((data[offset + 2] & 0xff) << 8)
                | (data[offset + 3] & 0xff);
    }
}
