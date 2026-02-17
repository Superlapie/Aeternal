package com.runescape.cache.anim;

import com.runescape.Client;
import com.runescape.cache.bzip.BZip2Decompressor;
import com.runescape.io.Buffer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public final class Frame {
    private static Path flatCacheRoot;
    public static Frame[][] animationlist;
    private static final Set<Integer> failedFrameFiles = new HashSet<>();
    private static final Set<Integer> attempted2446Groups = new HashSet<>();
    private static final Set<Integer> allowed2446Groups = new HashSet<>(Arrays.asList(
            7756, // 11059
            7757, // 11058
            7857, // 10819
            7866, // 10815
            7937, // 11063
            7938, // 11064
            9823, // 11062
            9833, // 11061
            9918, // 11051
            10823, // 11057
            10824, // 11060
            4172, // 11464 (voidwaker impact vfx)
            4173, // 11463 (voidwaker special vfx)
            4275, // 11275 (voidwaker special anim)
            4276, // 11240 (voidwaker alt special anim)
            // Yama combat body animation groups (2446 seq ids 11338+)
            6715, 6718, 6719, 6720, 6722, 6725, 6727, 6730, 6731, 6733,
            // Keep only high, non-colliding groups from 2446.
            11307
    ));
    private static final Map<Integer, int[]> animArchiveFiles = new HashMap<>();
    private static final Map<Integer, int[]> skeletonArchiveFiles = new HashMap<>();
    private static final Map<Integer, FrameBase> skeletonBaseCache = new HashMap<>();
    private static boolean animReferenceLoaded = false;
    private static boolean skeletonReferenceLoaded = false;
    public int duration;
    public FrameBase base;
    public int transformationCount;
    public int[] transformationIndices;
    public int[] transformX;
    public int[] transformY;
    public int[] transformZ;

    public static void load(int file, byte[] array) {
        try {
            if (animationlist == null || file < 0) {
                return;
            }
            if (file >= animationlist.length) {
                animationlist = Arrays.copyOf(animationlist, file + 1);
            }
            final Buffer ay = new Buffer(array);
            final FrameBase b2 = new FrameBase(ay);
            final int n = ay.readUShort();
            animationlist[file] = new Frame[n * 3];
            final int transformCapacity = Math.max(1, b2.transformationType.length);
            final int[] array2 = new int[transformCapacity];
            final int[] array3 = new int[transformCapacity];
            final int[] array4 = new int[transformCapacity];
            final int[] array5 = new int[transformCapacity];
            for (int j = 0; j < n; ++j) {
                final int k = ay.readUShort();
                final Frame[] array6 = animationlist[file];
                final int n2 = k;
                final Frame q = new Frame();
                array6[n2] = q;
                final Frame q2 = q;
                q.base = b2;
                final int f = ay.readUnsignedByte();
                int c2 = 0;
                int n3 = -1;
                final int limit = Math.min(f, b2.transformationType.length);
                for (int l = 0; l < limit; ++l) {
                    final int f2;
                    if ((f2 = ay.readUnsignedByte()) > 0) {
                        if (c2 >= transformCapacity) {
                            break;
                        }
                        if (b2.transformationType[l] != 0) {
                            for (int n4 = l - 1; n4 > n3; --n4) {
                                if (b2.transformationType[n4] == 0) {
                                    if (c2 >= transformCapacity) {
                                        break;
                                    }
                                    array2[c2] = n4;
                                    array3[c2] = 0;
                                    array5[c2] = (array4[c2] = 0);
                                    ++c2;
                                    break;
                                }
                            }
                        }
                        array2[c2] = l;
                        int n4 = 0;
                        if (b2.transformationType[l] == 3) {
                            n4 = 128;
                        }
                        if ((f2 & 0x1) != 0x0) {
                            array3[c2] = ay.readShort2();
                        } else {
                            array3[c2] = n4;
                        }
                        if ((f2 & 0x2) != 0x0) {
                            array4[c2] = ay.readShort2();
                        } else {
                            array4[c2] = n4;
                        }
                        if ((f2 & 0x4) != 0x0) {
                            array5[c2] = ay.readShort2();
                        } else {
                            array5[c2] = n4;
                        }
                        n3 = l;
                        ++c2;
                    }
                }
                // Consume any trailing flag bytes if malformed/extended data advertises more transforms.
                for (int l = limit; l < f; ++l) {
                    final int f2 = ay.readUnsignedByte();
                    if ((f2 & 0x1) != 0) {
                        ay.readShort2();
                    }
                    if ((f2 & 0x2) != 0) {
                        ay.readShort2();
                    }
                    if ((f2 & 0x4) != 0) {
                        ay.readShort2();
                    }
                }
                q2.transformationCount = c2;
                q2.transformationIndices = new int[c2];
                q2.transformX = new int[c2];
                q2.transformY = new int[c2];
                q2.transformZ = new int[c2];
                for (int l = 0; l < c2; ++l) {
                    q2.transformationIndices[l] = array2[l];
                    q2.transformX[l] = array3[l];
                    q2.transformY[l] = array4[l];
                    q2.transformZ[l] = array5[l];
                }
            }
            failedFrameFiles.remove(file);
        } catch (Exception ex) {
            failedFrameFiles.add(file);
            System.err.println("Frame.load: skipped invalid frame file " + file + " (" + ex.getClass().getSimpleName() + ")");
        }
    }

    public static Frame method531(int frame) {
        try {

            int file = frame >> 16;
            int k = frame & 0xffff;

            if (animationlist == null || file < 0) {
                return null;
            }
            if (file >= animationlist.length) {
                animationlist = Arrays.copyOf(animationlist, file + 1);
            }

            if (failedFrameFiles.contains(file)) {
                return null;
            }

            if (animationlist[file] == null || animationlist[file].length == 0) {
                if (tryLoad2446FrameGroup(file) && animationlist[file] != null && animationlist[file].length > 0) {
                    if (k < 0 || k >= animationlist[file].length) {
                        return null;
                    }
                    return animationlist[file][k];
                }
                Client.instance.resourceProvider.provide(1, file);
                return null;
            }

            if (k < 0 || k >= animationlist[file].length) {
                return null;
            }
            return animationlist[file][k];
        } catch (Exception ex) {
            return null;
        }
    }
    
    public static boolean noAnimationInProgress(int frame) {
        return frame == -1;
    }

    public static void clear() {
        animationlist = null;
        failedFrameFiles.clear();
        attempted2446Groups.clear();
        animArchiveFiles.clear();
        skeletonArchiveFiles.clear();
        skeletonBaseCache.clear();
        animReferenceLoaded = false;
        skeletonReferenceLoaded = false;
        flatCacheRoot = null;
    }

    private static Path resolveFlatCacheRoot() {
        if (flatCacheRoot != null) {
            return flatCacheRoot;
        }

        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path[] candidates = new Path[]{
                cwd.resolve("_ext/openrs2-2446-flat/cache"),
                cwd.resolve("../_ext/openrs2-2446-flat/cache"),
                cwd.resolve("../../_ext/openrs2-2446-flat/cache")
        };

        for (Path candidate : candidates) {
            Path normalized = candidate.normalize();
            if (Files.exists(normalized.resolve("0")) && Files.exists(normalized.resolve("1")) && Files.exists(normalized.resolve("255"))) {
                flatCacheRoot = normalized;
                System.out.println("Using 2446 flat cache root: " + flatCacheRoot);
                return flatCacheRoot;
            }
        }

        return null;
    }

    private static boolean tryLoad2446FrameGroup(int groupId) {
        // TEMP SAFETY MODE: disable all 2446 runtime frame group injection while
        // isolating animation corruption.
        return false;
        /*
        // Never override low legacy group ids; they collide with native 317 animation groups.
        if (groupId < 4000) {
            return false;
        }
        if (!allowed2446Groups.contains(groupId)) {
            return false;
        }
        Path root = resolveFlatCacheRoot();
        if (root == null) {
            return false;
        }
        if (!attempted2446Groups.add(groupId)) {
            return animationlist[groupId] != null && animationlist[groupId].length > 0;
        }
        try {
            Path groupPath = root.resolve("0").resolve(groupId + ".dat");
            if (!Files.exists(groupPath)) {
                return false;
            }
            byte[] container = Files.readAllBytes(groupPath);
            byte[] groupData = unpackJs5Container(container);
            if (groupData == null || groupData.length == 0) {
                return false;
            }

            int[] fileIds = getArchiveFileIds(0, groupId);
            if (fileIds == null || fileIds.length == 0) {
                return false;
            }
            Map<Integer, byte[]> files = splitGroupFiles(groupData, fileIds);
            if (files.isEmpty()) {
                return false;
            }

            int maxFileId = 0;
            for (int id : fileIds) {
                if (id > maxFileId) {
                    maxFileId = id;
                }
            }
            Frame[] frames = new Frame[maxFileId + 1];
            int loaded = 0;
            for (Map.Entry<Integer, byte[]> entry : files.entrySet()) {
                int frameId = entry.getKey();
                byte[] payload = entry.getValue();
                if (payload == null || payload.length < 3) {
                    continue;
                }
                int skeletonId = readUShort(payload, 0);
                FrameBase base = loadSkeletonBase(skeletonId);
                if (base == null) {
                    continue;
                }
                Frame frame = decode2446Frame(base, payload);
                if (frame == null) {
                    continue;
                }
                if (frameId >= frames.length) {
                    frames = Arrays.copyOf(frames, frameId + 1);
                }
                frames[frameId] = frame;
                loaded++;
            }

            if (loaded == 0) {
                failedFrameFiles.add(groupId);
                return false;
            }

            if (groupId >= animationlist.length) {
                animationlist = Arrays.copyOf(animationlist, groupId + 1);
            }
            animationlist[groupId] = frames;
            failedFrameFiles.remove(groupId);
            System.out.println("Loaded 2446 frame group " + groupId + " (" + loaded + " frames)");
            return true;
        } catch (Exception ex) {
            failedFrameFiles.add(groupId);
            System.err.println("Frame.load2446: failed group " + groupId + " (" + ex.getClass().getSimpleName() + ")");
            return false;
        }
        */
    }

    private static FrameBase loadSkeletonBase(int skeletonId) {
        Path root = resolveFlatCacheRoot();
        if (root == null) {
            return null;
        }
        FrameBase cached = skeletonBaseCache.get(skeletonId);
        if (cached != null) {
            return cached;
        }
        try {
            Path path = root.resolve("1").resolve(skeletonId + ".dat");
            if (!Files.exists(path)) {
                return null;
            }
            byte[] container = Files.readAllBytes(path);
            byte[] groupData = unpackJs5Container(container);
            if (groupData == null || groupData.length == 0) {
                return null;
            }
            int[] fileIds = getArchiveFileIds(1, skeletonId);
            if (fileIds == null || fileIds.length == 0) {
                return null;
            }
            Map<Integer, byte[]> files = splitGroupFiles(groupData, fileIds);
            if (files.isEmpty()) {
                return null;
            }
            byte[] payload = files.get(0);
            if (payload == null) {
                payload = files.values().iterator().next();
            }
            if (payload == null || payload.length == 0) {
                return null;
            }
            int pos = 0;
            int count = payload[pos++] & 0xFF;
            int[] types = new int[count];
            int[][] skinList = new int[count][];

            for (int i = 0; i < count; i++) {
                types[i] = payload[pos++] & 0xFF;
            }
            for (int i = 0; i < count; i++) {
                int len = payload[pos++] & 0xFF;
                skinList[i] = new int[len];
            }
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < skinList[i].length; j++) {
                    skinList[i][j] = payload[pos++] & 0xFF;
                }
            }

            FrameBase base = new FrameBase(types, skinList);
            skeletonBaseCache.put(skeletonId, base);
            return base;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Frame decode2446Frame(FrameBase base, byte[] payload) {
        try {
            int len = payload[2] & 0xFF;
            int inPos = 3;
            int dataPos = 3 + len;
            if (dataPos > payload.length) {
                return null;
            }

            int[] indexFrameIds = new int[500];
            int[] translatorX = new int[500];
            int[] translatorY = new int[500];
            int[] translatorZ = new int[500];

            int lastI = -1;
            int index = 0;
            for (int i = 0; i < len; i++) {
                int flags = payload[inPos++] & 0xFF;
                if (flags <= 0) {
                    continue;
                }
                if (i >= base.transformationType.length) {
                    return null;
                }

                if (base.transformationType[i] != 0) {
                    for (int j = i - 1; j > lastI; --j) {
                        if (base.transformationType[j] == 0) {
                            indexFrameIds[index] = j;
                            translatorX[index] = 0;
                            translatorY[index] = 0;
                            translatorZ[index] = 0;
                            ++index;
                            break;
                        }
                    }
                }

                indexFrameIds[index] = i;
                int defaultValue = base.transformationType[i] == 3 ? 128 : 0;

                if ((flags & 1) != 0) {
                    translatorX[index] = readShortSmart(payload, dataPos);
                    dataPos += shortSmartLength(payload, dataPos);
                } else {
                    translatorX[index] = defaultValue;
                }

                if ((flags & 2) != 0) {
                    translatorY[index] = readShortSmart(payload, dataPos);
                    dataPos += shortSmartLength(payload, dataPos);
                } else {
                    translatorY[index] = defaultValue;
                }

                if ((flags & 4) != 0) {
                    translatorZ[index] = readShortSmart(payload, dataPos);
                    dataPos += shortSmartLength(payload, dataPos);
                } else {
                    translatorZ[index] = defaultValue;
                }

                lastI = i;
                ++index;
            }

            Frame frame = new Frame();
            frame.base = base;
            frame.transformationCount = index;
            frame.transformationIndices = Arrays.copyOf(indexFrameIds, index);
            frame.transformX = Arrays.copyOf(translatorX, index);
            frame.transformY = Arrays.copyOf(translatorY, index);
            frame.transformZ = Arrays.copyOf(translatorZ, index);
            return frame;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int[] getArchiveFileIds(int indexId, int archiveId) {
        Map<Integer, int[]> map = indexId == 0 ? animArchiveFiles : skeletonArchiveFiles;
        if ((indexId == 0 && !animReferenceLoaded) || (indexId == 1 && !skeletonReferenceLoaded)) {
            loadReferenceMap(indexId, map);
            if (indexId == 0) {
                animReferenceLoaded = true;
            } else {
                skeletonReferenceLoaded = true;
            }
        }
        return map.get(archiveId);
    }

    private static void loadReferenceMap(int indexId, Map<Integer, int[]> output) {
        Path root = resolveFlatCacheRoot();
        if (root == null) {
            return;
        }
        output.clear();
        try {
            Path refPath = root.resolve("255").resolve(indexId + ".dat");
            if (!Files.exists(refPath)) {
                return;
            }
            byte[] refContainer = Files.readAllBytes(refPath);
            byte[] ref = unpackJs5Container(refContainer);
            if (ref == null || ref.length == 0) {
                return;
            }

            IntReader r = new IntReader(ref);
            int protocol = r.readUByte();
            if (protocol >= 6) {
                r.readInt();
            }
            int flags = r.readUByte();
            boolean named = (flags & 1) != 0;
            boolean sized = (flags & 4) != 0;

            int archiveCount = protocol >= 7 ? r.readBigSmart() : r.readUShort();
            int[] archiveIds = new int[archiveCount];
            int lastArchive = 0;
            for (int i = 0; i < archiveCount; i++) {
                lastArchive += protocol >= 7 ? r.readBigSmart() : r.readUShort();
                archiveIds[i] = lastArchive;
            }

            if (named) {
                for (int i = 0; i < archiveCount; i++) {
                    r.readInt();
                }
            }
            for (int i = 0; i < archiveCount; i++) {
                r.readInt(); // CRC
            }
            if (sized) {
                for (int i = 0; i < archiveCount; i++) {
                    r.readInt();
                    r.readInt();
                }
            }
            for (int i = 0; i < archiveCount; i++) {
                r.readInt(); // revision
            }

            int[] fileCounts = new int[archiveCount];
            for (int i = 0; i < archiveCount; i++) {
                fileCounts[i] = protocol >= 7 ? r.readBigSmart() : r.readUShort();
            }

            for (int i = 0; i < archiveCount; i++) {
                int count = fileCounts[i];
                int[] fileIds = new int[count];
                int lastFile = 0;
                for (int j = 0; j < count; j++) {
                    lastFile += protocol >= 7 ? r.readBigSmart() : r.readUShort();
                    fileIds[j] = lastFile;
                }
                output.put(archiveIds[i], fileIds);
            }
        } catch (Exception ignored) {
        }
    }

    private static Map<Integer, byte[]> splitGroupFiles(byte[] groupData, int[] fileIds) {
        Map<Integer, byte[]> files = new HashMap<>();
        if (fileIds.length == 1) {
            files.put(fileIds[0], groupData);
            return files;
        }

        int fileCount = fileIds.length;
        int chunks = groupData[groupData.length - 1] & 0xFF;
        int tablePos = groupData.length - 1 - chunks * fileCount * 4;
        if (chunks <= 0 || tablePos < 0) {
            return files;
        }

        IntReader table = new IntReader(groupData, tablePos);
        int[][] chunkSizes = new int[fileCount][chunks];
        int[] fileSizes = new int[fileCount];

        for (int c = 0; c < chunks; c++) {
            int cumulative = 0;
            for (int f = 0; f < fileCount; f++) {
                cumulative += table.readInt();
                chunkSizes[f][c] = cumulative;
                fileSizes[f] += cumulative;
            }
        }

        byte[][] out = new byte[fileCount][];
        int[] offsets = new int[fileCount];
        for (int f = 0; f < fileCount; f++) {
            out[f] = new byte[fileSizes[f]];
        }

        int pos = 0;
        for (int c = 0; c < chunks; c++) {
            for (int f = 0; f < fileCount; f++) {
                int size = chunkSizes[f][c];
                System.arraycopy(groupData, pos, out[f], offsets[f], size);
                offsets[f] += size;
                pos += size;
            }
        }

        for (int f = 0; f < fileCount; f++) {
            files.put(fileIds[f], out[f]);
        }
        return files;
    }

    private static byte[] unpackJs5Container(byte[] input) {
        if (input == null || input.length < 5) {
            return null;
        }
        int type = input[0] & 0xFF;
        int compressedLength = readInt(input, 1);
        if (compressedLength < 0) {
            return null;
        }
        if (type == 0) {
            if (5 + compressedLength > input.length) {
                return null;
            }
            return Arrays.copyOfRange(input, 5, 5 + compressedLength);
        }
        if (input.length < 9) {
            return null;
        }
        int decompressedLength = readInt(input, 5);
        if (decompressedLength < 0 || 9 + compressedLength > input.length) {
            return null;
        }
        byte[] out = new byte[decompressedLength];
        if (type == 1) {
            BZip2Decompressor.decompress(out, decompressedLength, input, compressedLength, 9);
            return out;
        }
        if (type == 2) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(input, 9, compressedLength))) {
                int read = 0;
                while (read < decompressedLength) {
                    int count = gis.read(out, read, decompressedLength - read);
                    if (count == -1) {
                        break;
                    }
                    read += count;
                }
                if (read == decompressedLength) {
                    return out;
                }
            } catch (IOException ignored) {
                return null;
            }
        }
        return null;
    }

    private static int readUShort(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static int shortSmartLength(byte[] data, int offset) {
        return (data[offset] & 0xFF) < 128 ? 1 : 2;
    }

    private static int readShortSmart(byte[] data, int offset) {
        int peek = data[offset] & 0xFF;
        if (peek < 128) {
            return (data[offset] & 0xFF) - 64;
        }
        return readUShort(data, offset) - 0xC000;
    }

    private static final class IntReader {
        private final byte[] data;
        private int pos;

        private IntReader(byte[] data) {
            this(data, 0);
        }

        private IntReader(byte[] data, int start) {
            this.data = data;
            this.pos = start;
        }

        private int readUByte() {
            return data[pos++] & 0xFF;
        }

        private int readUShort() {
            int v = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;
            return v;
        }

        private int readInt() {
            int v = ((data[pos] & 0xFF) << 24)
                    | ((data[pos + 1] & 0xFF) << 16)
                    | ((data[pos + 2] & 0xFF) << 8)
                    | (data[pos + 3] & 0xFF);
            pos += 4;
            return v;
        }

        private int readBigSmart() {
            if ((data[pos] & 0xFF) < 128) {
                return readUShort();
            }
            return readInt() & Integer.MAX_VALUE;
        }
    }
}
