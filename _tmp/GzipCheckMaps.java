import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class GzipCheckMaps {
  public static void main(String[] args) throws Exception {
    for (int id = 6239; id <= 6268; id++) {
      byte[] data = Files.readAllBytes(Paths.get("server/data/clipping/maps/" + id + ".dat"));
      try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data));
           ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        byte[] buf = new byte[4096];
        int n;
        while ((n = gis.read(buf)) != -1) out.write(buf, 0, n);
        System.out.println(id + " ok decLen=" + out.size());
      } catch (Exception ex) {
        System.out.println(id + " FAIL " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
      }
    }
  }
}
