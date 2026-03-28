import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import com.runescape.cache.bzip.BZip2Decompressor;

public class ProbeAny {
    public static void main(String[] args) throws Exception {
        byte[] input = Files.readAllBytes(Path.of(args[0]));
        byte[] output = unpack(input);
        boolean startsB84A = output.length >= 2 && (output[0] == (byte)0xB8 && output[1] == 0x4A);
        if (startsB84A || output.length < 200000) {
            System.out.printf("in=%d out=%d first=%s b84a=%s kings=%s tunnel=%s%n",
                    input.length, output.length, hex(output, 8), startsB84A,
                    new String(output, java.nio.charset.StandardCharsets.ISO_8859_1).contains("Kings' ladder"),
                    new String(output, java.nio.charset.StandardCharsets.ISO_8859_1).contains("Tunnel entrance"));
        }
    }

    private static byte[] unpack(byte[] input) throws Exception {
        if (input.length < 5) return input;
        int type = input[0] & 0xFF;
        int compressedLen = readInt(input, 1);
        if (type == 0) return Arrays.copyOfRange(input, 5, 5 + compressedLen);
        if (type == 2) {
            int start = 9;
            byte[] compressed = Arrays.copyOfRange(input, start, start + compressedLen);
            return gunzip(compressed);
        }
        if (type == 1) {
            int outputLen = readInt(input, 5);
            int start = 9;
            byte[] compressed = Arrays.copyOfRange(input, start, start + compressedLen);
            byte[] output = new byte[outputLen];
            BZip2Decompressor.decompress(output, outputLen, compressed, compressedLen, 0);
            return output;
        }
        return input;
    }

    private static byte[] gunzip(byte[] compressed) throws Exception {
        try (java.util.zip.GZIPInputStream in = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(compressed))) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private static int readInt(byte[] data, int offset) {
        if (offset + 3 >= data.length) return -1;
        return ((data[offset] & 0xFF) << 24) | ((data[offset+1] & 0xFF) << 16) | ((data[offset+2] & 0xFF) << 8) | (data[offset+3] & 0xFF);
    }

    private static String hex(byte[] data, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, data.length); i++) sb.append(String.format("%02x", data[i]));
        return sb.toString();
    }
}
