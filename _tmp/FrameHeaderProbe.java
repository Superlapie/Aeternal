import com.runescape.cache.FileStore;
import java.io.ByteArrayInputStream;
import java.io.RandomAccessFile;
import java.util.zip.GZIPInputStream;

public class FrameHeaderProbe {
  public static void main(String[] args) throws Exception {
    int[] groups = {207, 410, 1390, 1664, 1243, 6088};
    try (RandomAccessFile dat = new RandomAccessFile("client/Cache/main_file_cache.dat", "r");
         RandomAccessFile idx2 = new RandomAccessFile("client/Cache/main_file_cache.idx2", "r")) {
      FileStore store = new FileStore(dat, idx2, 3);
      for (int g : groups) {
        byte[] raw = store.decompress(g);
        if (raw == null) {
          System.out.println("group=" + g + " raw=null");
          continue;
        }
        byte[] data = gunzip(raw);
        if (data == null) {
          System.out.println("group=" + g + " gunzip failed");
          continue;
        }
        int b0 = data.length > 0 ? (data[0] & 0xFF) : -1;
        int b1 = data.length > 1 ? (data[1] & 0xFF) : -1;
        int u16 = (b0 << 8) | b1;
        System.out.println("group=" + g + " decLen=" + data.length + " b0=" + b0 + " b1=" + b1 + " asU16=" + u16);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(24, data.length); i++) {
          if (i > 0) sb.append(' ');
          sb.append(String.format("%02X", data[i] & 0xFF));
        }
        System.out.println("  head=" + sb);
      }
    }
  }

  private static byte[] gunzip(byte[] in) {
    try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(in))) {
      byte[] out = new byte[1 << 20];
      int pos = 0;
      while (true) {
        int n = gis.read(out, pos, out.length - pos);
        if (n == -1) break;
        pos += n;
        if (pos == out.length) return null;
      }
      byte[] copy = new byte[pos];
      System.arraycopy(out, 0, copy, 0, pos);
      return copy;
    } catch (Exception ex) {
      return null;
    }
  }
}
