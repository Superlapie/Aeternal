import com.runescape.cache.FileStore;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayInputStream;

public class MapArchiveRepair {
    public static void main(String[] args) throws Exception {
        String clientCache = "client/Cache/";
        String serverMaps = "server/data/clipping/maps/";
        int wrote = 0;

        try (RandomAccessFile dat = new RandomAccessFile(clientCache + "main_file_cache.dat", "rw");
             RandomAccessFile idx4 = new RandomAccessFile(clientCache + "main_file_cache.idx4", "rw")) {
            FileStore store = new FileStore(dat, idx4, 5);

            for (int id = 6239; id <= 6268; id++) {
                byte[] src = Files.readAllBytes(Paths.get(serverMaps + id + ".dat"));
                // verify payload is readable gzip before writing
                try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(src))) {
                    while (gis.read() != -1) {
                        // consume
                    }
                }

                if (!store.writeFile(src.length, src, id)) {
                    throw new IllegalStateException("Failed to write map archive id=" + id);
                }
                wrote++;
            }
        }

        System.out.println("Repaired map archives written=" + wrote + " (6239..6268)");
    }
}
