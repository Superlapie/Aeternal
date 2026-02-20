import com.runescape.cache.FileStore;
import java.io.RandomAccessFile;

public class FrameRawProbe {
  public static void main(String[] args) throws Exception {
    int group = Integer.parseInt(args[0]);
    String cacheDir = "client/Cache/";
    try (RandomAccessFile dat = new RandomAccessFile(cacheDir + "main_file_cache.dat", "r");
         RandomAccessFile idx2 = new RandomAccessFile(cacheDir + "main_file_cache.idx2", "r")) {
      FileStore store2 = new FileStore(dat, idx2, 3);
      byte[] data = store2.decompress(group);
      if (data == null) {
        System.out.println("no data");
        return;
      }
      System.out.println("len=" + data.length);
      int b0 = data.length > 0 ? (data[0] & 0xFF) : -1;
      int b1 = data.length > 1 ? (data[1] & 0xFF) : -1;
      int ushort = ((b0 << 8) | b1);
      System.out.println("countByte=" + b0 + " countUShort=" + ushort + " secondByte=" + b1);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < Math.min(32, data.length); i++) {
        if (i > 0) sb.append(' ');
        sb.append(String.format("%02X", data[i] & 0xFF));
      }
      System.out.println(sb.toString());
    }
  }
}
