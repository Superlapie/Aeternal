import java.nio.file.*;
import com.runescape.cache.bzip.BZip2Decompressor;

public class ProbeFlat {
    public static void main(String[] args) throws Exception {
        byte[] input = Files.readAllBytes(Path.of(args[0]));
        if (input.length < 9) {
            System.out.println("too small");
            return;
        }
        int type = input[0] & 0xFF;
        int compressedLen = ((input[1] & 0xFF) << 24) | ((input[2] & 0xFF) << 16) | ((input[3] & 0xFF) << 8) | (input[4] & 0xFF);
        int outputLen = ((input[5] & 0xFF) << 24) | ((input[6] & 0xFF) << 16) | ((input[7] & 0xFF) << 8) | (input[8] & 0xFF);
        byte[] compressed = new byte[compressedLen];
        System.arraycopy(input, 9, compressed, 0, compressedLen);
        byte[] output = new byte[outputLen];
        BZip2Decompressor.decompress(output, outputLen, compressed, compressedLen, 0);
        System.out.printf("type=%d in=%d out=%d first=%02x%02x hasKings=%s hasTunnel=%s startsB84A=%s%n",
                type, input.length, output.length, output.length > 0 ? output[0] : 0, output.length > 1 ? output[1] : 0,
                new String(output).contains("Kings' ladder"), new String(output).contains("Tunnel entrance"),
                output.length > 1 && output[0] == (byte)0xB8 && output[1] == 0x4A);
    }
}
