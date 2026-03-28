import java.nio.file.*;
import com.runescape.cache.bzip.BZip2Decompressor;

public class Unpack {
    public static void main(String[] args) throws Exception {
        byte[] input = Files.readAllBytes(Path.of(args[0]));
        int compressedLen = ((input[1] & 0xFF) << 24) | ((input[2] & 0xFF) << 16) | ((input[3] & 0xFF) << 8) | (input[4] & 0xFF);
        int outputLen = ((input[5] & 0xFF) << 24) | ((input[6] & 0xFF) << 16) | ((input[7] & 0xFF) << 8) | (input[8] & 0xFF);
        byte[] compressed = new byte[compressedLen];
        System.arraycopy(input, 9, compressed, 0, compressedLen);
        byte[] output = new byte[outputLen];
        BZip2Decompressor.decompress(output, outputLen, compressed, compressedLen, 0);
        Files.write(Path.of(args[1]), output);
        System.out.println("outlen=" + output.length);
        System.out.print("first16=");
        for (int i = 0; i < Math.min(16, output.length); i++) {
            System.out.printf("%02x", output[i]);
        }
        System.out.println();
        String s = new String(output);
        System.out.println("containsKings=" + s.contains("Kings' ladder"));
        System.out.println("containsTunnel=" + s.contains("Tunnel entrance"));
        System.out.println("containsStandard=" + s.contains("Standard"));
        System.out.println("containsSlayer=" + s.contains("Slayer"));
        System.out.println("containsPeek=" + s.contains("Peek"));
    }
}
