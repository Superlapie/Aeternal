public class ExtractMaps {
    public static void main(String[] args) {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", "bin", "com.cacheextractor.CacheExtractor", "--cache", "../client/Cache", "-o", "../client/Cache", "-t", "map");
            Process process = pb.start();
            process.waitFor();
            System.out.println("Map extraction completed!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
