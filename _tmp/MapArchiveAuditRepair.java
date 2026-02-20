import com.runescape.cache.FileStore;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class MapArchiveAuditRepair {
    public static void main(String[] args) throws Exception {
        String clientCache = "client/Cache/";
        String serverMaps = "server/data/clipping/maps/";
        byte[] mapIndex = Files.readAllBytes(Paths.get(clientCache + "map_index"));

        int count = u16(mapIndex, 0);
        int off = 2;
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < count; i++) {
            int terrain = u16(mapIndex, off + 2);
            int object = u16(mapIndex, off + 4);
            if (terrain >= 0) ids.add(terrain);
            if (object >= 0) ids.add(object);
            off += 6;
        }

        int missingBefore = 0;
        int corruptBefore = 0;
        int repaired = 0;
        int missingAfter = 0;
        int corruptAfter = 0;

        try (RandomAccessFile dat = new RandomAccessFile(clientCache + "main_file_cache.dat", "rw");
             RandomAccessFile idx4 = new RandomAccessFile(clientCache + "main_file_cache.idx4", "rw")) {
            FileStore store = new FileStore(dat, idx4, 5);

            for (int id : ids) {
                byte[] data = store.decompress(id);
                Status before = check(data);
                if (before == Status.MISSING) missingBefore++;
                if (before == Status.CORRUPT) corruptBefore++;

                if (before != Status.OK) {
                    Path serverFile = Paths.get(serverMaps + id + ".dat");
                    if (Files.exists(serverFile)) {
                        byte[] src = Files.readAllBytes(serverFile);
                        if (check(src) == Status.OK && store.writeFile(src.length, src, id)) {
                            repaired++;
                        }
                    }
                }

                byte[] afterData = store.decompress(id);
                Status after = check(afterData);
                if (after == Status.MISSING) missingAfter++;
                if (after == Status.CORRUPT) corruptAfter++;
            }
        }

        System.out.println("mapIndexEntries=" + count + " uniqueArchiveIds=" + ids.size());
        System.out.println("before missing=" + missingBefore + " corrupt=" + corruptBefore);
        System.out.println("repairedFromServerMaps=" + repaired);
        System.out.println("after missing=" + missingAfter + " corrupt=" + corruptAfter);
    }

    private enum Status { OK, MISSING, CORRUPT }

    private static Status check(byte[] data) {
        if (data == null || data.length == 0) {
            return Status.MISSING;
        }
        if (isGzip(data)) {
            return Status.OK;
        }
        if (isJs5Container(data)) {
            return Status.OK;
        }
        return Status.CORRUPT;
    }

    private static boolean isGzip(byte[] data) {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] buf = new byte[1024];
            while (gis.read(buf) != -1) {
                // consume
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean isJs5Container(byte[] data) {
        if (data.length < 5) return false;
        int type = data[0] & 0xFF;
        int compLen = readInt(data, 1);
        if (compLen < 0) return false;
        if (type == 0) {
            return data.length >= 5 + compLen;
        }
        if (data.length < 9) return false;
        int decompLen = readInt(data, 5);
        if (decompLen < 0 || data.length < 9 + compLen) return false;
        return type == 1 || type == 2;
    }

    private static int readInt(byte[] data, int offset) {
        if (offset + 4 > data.length) return -1;
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static int u16(byte[] data, int off) {
        return ((data[off] & 0xFF) << 8) | (data[off + 1] & 0xFF);
    }
}
