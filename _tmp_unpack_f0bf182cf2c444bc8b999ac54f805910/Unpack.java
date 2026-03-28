import java.nio.file.*;
import com.runescape.cache.bzip.BZip2Decompressor;

public class Unpack {
    public static void main(String[] args) throws Exception {
        byte[] input = Files.readAllBytes(Path.of(args[0]));
        int compressedLen = ((input[1] & 0xFF) << 24) | ((input[2] & 0xFF) << 16) | ((input[3] & 0xFF) << 8) | (input[4] & 0xFF);
        int outputLen = ((input[5] & 0xFF) << 24) | ((input[6] & 0xFF) << 16) | ((input[7] & 0xFF) << 8) | (input[8] & 0xFF);
        byte[] output = new byte[outputLen];
        BZip2Decompressor.decompress(output, outputLen, input, compressedLen, 6);
        Files.write(Path.of(args[1]), output);
        System.out.println("outlen=" + output.length);
        System.out.print("first16=");
        for (int i = 0; i < Math.min(16, output.length); i++) {
            System.out.printf("%02x", output[i]);
        }
        System.out.println();
        System.out.println("containsKings=" + new String(output).contains("Kings' ladder"));
        System.out.println("containsTunnel=" + new String(output).contains("Tunnel entrance"));
    }
}
