import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.def.ObjectDefinition;
import java.io.File;
import java.io.RandomAccessFile;

public class ObjectProbe {
    public static void main(String[] args) throws Exception {
        String cacheDir = args[0];
        if (!cacheDir.endsWith("\\") && !cacheDir.endsWith("/")) cacheDir += File.separator;
        File dat = new File(cacheDir + "main_file_cache.dat");
        if (!dat.exists()) dat = new File(cacheDir + "main_file_cache.dat2");
        File idx0 = new File(cacheDir + "main_file_cache.idx0");
        try (RandomAccessFile datRaf = new RandomAccessFile(dat, "r"); RandomAccessFile idx0Raf = new RandomAccessFile(idx0, "r")) {
            FileStore store0 = new FileStore(datRaf, idx0Raf, 1);
            byte[] configArchivePayload = store0.decompress(2);
            FileArchive archive = new FileArchive(configArchivePayload);
            ObjectDefinition.init(archive);
            for (int id = 7448; id <= 7495; id++) {
                ObjectDefinition def = ObjectDefinition.lookup(id);
                if (def == null || def.name == null) continue;
                String actions = "";
                if (def.interactions != null) {
                    for (int i = 0; i < def.interactions.length; i++) {
                        if (def.interactions[i] != null) actions += " [" + i + ":" + def.interactions[i] + "]";
                    }
                }
                System.out.println(id + " | " + def.name + actions);
            }
        }
    }
}
