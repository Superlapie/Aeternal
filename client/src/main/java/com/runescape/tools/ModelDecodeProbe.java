package com.runescape.tools;

import com.runescape.cache.FileStore;
import com.runescape.entity.model.Model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.zip.GZIPInputStream;

/**
 * Probes whether specific model IDs can be decoded by this client build.
 *
 * Usage:
 * java com.runescape.tools.ModelDecodeProbe <cacheDir> <id1,id2,id3...>
 */
public final class ModelDecodeProbe {

    private ModelDecodeProbe() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java com.runescape.tools.ModelDecodeProbe <cacheDir> <id1,id2,id3...>");
            return;
        }
        String cacheDir = normalize(args[0]);
        int[] ids = parseIds(args[1]);

        File dat = new File(cacheDir + "main_file_cache.dat");
        File idx1 = new File(cacheDir + "main_file_cache.idx1");
        if (!dat.exists() || !idx1.exists()) {
            throw new IllegalStateException("Missing main_file_cache.dat or idx1 in " + cacheDir);
        }

        Model.init();

        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "r");
             RandomAccessFile idxRaf = new RandomAccessFile(idx1, "r")) {
            FileStore models = new FileStore(datRaf, idxRaf, 2);

            for (int id : ids) {
                try {
                    byte[] gz = models.decompress(id);
                    if (gz == null) {
                        System.out.println("MISS " + id + " (no cache payload)");
                        continue;
                    }
                    byte[] raw = gunzip(gz);
                    Model.method460(raw, id);
                    Model model = Model.getModel(id);
                    if (model == null) {
                        System.out.println("NULL " + id + " (header loaded but model not built)");
                        continue;
                    }
                    int draw0 = 0, draw1 = 0, draw2 = 0, draw3 = 0, drawOther = 0;
                    if (model.faceDrawType != null) {
                        for (int tVal : model.faceDrawType) {
                            int v = tVal & 0x3;
                            if (v == 0) draw0++;
                            else if (v == 1) draw1++;
                            else if (v == 2) draw2++;
                            else if (v == 3) draw3++;
                            else drawOther++;
                        }
                    }

                    int alphaZero = 0, alphaFull = 0, alphaOther = 0;
                    if (model.face_alpha != null) {
                        for (int a : model.face_alpha) {
                            if (a <= 0) alphaZero++;
                            else if (a >= 255) alphaFull++;
                            else alphaOther++;
                        }
                    }

                    int colorZero = 0, colorNegative = 0;
                    int colorMin = Integer.MAX_VALUE, colorMax = Integer.MIN_VALUE;
                    if (model.triangleColours != null) {
                        for (short cVal : model.triangleColours) {
                            int c = cVal;
                            if (c < 0) colorNegative++;
                            if (c == 0) colorZero++;
                            if (c < colorMin) colorMin = c;
                            if (c > colorMax) colorMax = c;
                        }
                    }

                    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
                    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
                    for (int i = 0; i < model.numVertices; i++) {
                        int x = model.vertexX[i];
                        int y = model.vertexY[i];
                        int z = model.vertexZ[i];
                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (z < minZ) minZ = z;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;
                        if (z > maxZ) maxZ = z;
                    }

                    System.out.println("OK   " + id
                            + " v=" + model.numVertices
                            + " t=" + model.numTriangles
                            + " tex=" + model.numberOfTexturesFaces
                            + " draw[0/1/2/3]=" + draw0 + "/" + draw1 + "/" + draw2 + "/" + draw3
                            + " alpha[z/f/o]=" + alphaZero + "/" + alphaFull + "/" + alphaOther
                            + " colors[z/neg/min/max]=" + colorZero + "/" + colorNegative + "/"
                            + (colorMin == Integer.MAX_VALUE ? "n/a" : colorMin) + "/"
                            + (colorMax == Integer.MIN_VALUE ? "n/a" : colorMax)
                            + " bounds=(" + minX + "," + minY + "," + minZ + ")->(" + maxX + "," + maxY + "," + maxZ + ")");
                } catch (Throwable t) {
                    System.out.println("FAIL " + id + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage());
                    t.printStackTrace(System.out);
                }
            }
        }
    }

    private static byte[] gunzip(byte[] gz) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
            byte[] out = new byte[1024 * 1024 * 8];
            int off = 0;
            int n;
            while ((n = in.read(out, off, out.length - off)) != -1) {
                off += n;
                if (off == out.length) {
                    throw new IllegalStateException("Model too large for probe buffer");
                }
            }
            byte[] exact = new byte[off];
            System.arraycopy(out, 0, exact, 0, off);
            return exact;
        }
    }

    private static int[] parseIds(String csv) {
        String[] parts = csv.split(",");
        int[] ids = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ids[i] = Integer.parseInt(parts[i].trim());
        }
        return ids;
    }

    private static String normalize(String dir) {
        return dir.endsWith("/") || dir.endsWith("\\") ? dir : dir + File.separator;
    }
}
