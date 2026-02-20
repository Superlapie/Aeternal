import com.runescape.cache.FileStore;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MapArchiveProbe {
    public static void main(String[] args) throws Exception {
        int[] ids = {6241, 6251, 6254};
        String clientCache = "client/Cache/";
        String serverMaps = "server/data/clipping/maps/";

        try (RandomAccessFile dat = new RandomAccessFile(clientCache + "main_file_cache.dat", "r");
             RandomAccessFile idx4 = new RandomAccessFile(clientCache + "main_file_cache.idx4", "r")) {
            FileStore mapsStore = new FileStore(dat, idx4, 5);

            for (int id : ids) {
                byte[] client = mapsStore.decompress(id);
                byte[] server = Files.readAllBytes(Paths.get(serverMaps + id + ".dat"));

                System.out.println("id=" + id);
                System.out.println("  server len=" + server.length + " head=" + hexHead(server));
                System.out.println("  client len=" + (client == null ? -1 : client.length) + " head=" + hexHead(client));
                System.out.println("  equal=" + equals(server, client));
            }
        }
    }

    private static String hexHead(byte[] data) {
        if (data == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        int n = Math.min(8, data.length);
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", data[i] & 0xFF));
        }
        return sb.toString();
    }

    private static boolean equals(byte[] a, byte[] b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }
}
