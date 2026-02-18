package com.runescape.cache;

import com.runescape.Client;
import com.runescape.cache.bzip.BZip2Decompressor;
import com.runescape.collection.Deque;
import com.runescape.collection.Queue;
import com.runescape.io.Buffer;
import com.runescape.sign.SignLink;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public final class ResourceProvider implements Runnable {

    private final Deque requested;
    private final byte[] payload;
    private final byte[][] fileStatus;
    private final Deque extras;
    private final Deque complete;
    private final byte[] gzipInputBuffer;
    private final Queue requests;
    private final int[][] versions;
    private final Deque unrequested;
    private final Deque mandatoryRequests;
    private final String[] crcNames = {"model_crc", "anim_crc", "midi_crc", "map_crc"};
    private final int[][] crcs = new int[crcNames.length][];
    public int tick;
    public int[] file_amounts = new int[4];
    int[] cheapHaxValues = new int[]{
            3627, 3628,
            3655, 3656,
            3625, 3626,
            3629, 3630,
            4071, 4072,
            5253, 1816,
            1817, 3653,
            3654, 4067,
            4068, 3639,
            3640, 1976,
            1977, 3571,
            3572, 5129,
            5130, 2066,
            2067, 3545,
            3546, 3559,
            3560, 3569,
            3570, 3551,
            3552, 3579,
            3580, 3575,
            3576, 1766,
            1767, 3547,
            3548, 3682,
            3683, 3696,
            3697, 3692,
            3693, 4013,
            4079, 4080,
            4082, 3996,
            4083, 4084,
            4075, 4076,
            3664, 3993,
            3994, 3995,
            4077, 4078,
            4073, 4074,
            4011, 4012,
            3998, 3999,
            4081,
    };
    private int totalFiles;
    private int maximumPriority;
    private int[] landscapes;
    private Client clientInstance;
    private int completedSize;
    private int remainingData;
    private int[] musicPriorities;
    private int[] mapFiles;
    private int filesLoaded;
    private boolean running;
    private boolean expectingData;
    private InputStream inputStream;
    private Socket socket;
    private int uncompletedCount;
    private int completedCount;
    private Resource current;
    private int[] areas;
    private int idleTime;
    private final Set<Integer> missingMapArchives = new HashSet<>();
    private final Set<Long> rawArchiveFallbacks = new HashSet<>();
    private final Set<Long> staleMandatoryWarnings = new HashSet<>();

    public ResourceProvider() {
        requested = new Deque();
        payload = new byte[500];
        fileStatus = new byte[4][];
        extras = new Deque();
        running = true;
        expectingData = false;
        complete = new Deque();
        gzipInputBuffer = new byte[0x71868];
        requests = new Queue();
        versions = new int[4][];
        unrequested = new Deque();
        mandatoryRequests = new Deque();
    }

    private void respond() {
        try {
            int available = inputStream.available();
            if (remainingData == 0 && available >= 10) {
                expectingData = true;
                for (int skip = 0; skip < 10; skip += inputStream.read(payload, skip, 10 - skip))
                    ;
                int type = payload[0] & 0xff;
                int file = ((payload[1] & 0xff) << 16) + ((payload[2] & 0xff) << 8) + (payload[3] & 0xff);
                int length = ((payload[4] & 0xff) << 32) + ((payload[5] & 0xff) << 16) + ((payload[6] & 0xff) << 8) + (payload[7] & 0xff);
                int sector = ((payload[8] & 0xff) << 8) + (payload[9] & 0xff);
                current = null;
                for (Resource resource = (Resource) requested.reverseGetFirst(); resource != null; resource = (Resource) requested.reverseGetNext()) {
                    if (resource.dataType == type && resource.ID == file)
                        current = resource;
                    if (current != null)
                        resource.loopCycle = 0;
                }

                if (current != null) {
                    idleTime = 0;
                    if (length == 0) {
                        System.out.println("Rej: " + type + "," + file);
                        current.buffer = null;
                        if (current.incomplete)
                            synchronized (complete) {
                                complete.insertHead(current);
                            }
                        else {
                            current.unlink();
                        }
                        current = null;
                    } else {
                        if (current.buffer == null && sector == 0)
                            current.buffer = new byte[length];
                        if (current.buffer == null && sector != 0)
                            throw new IOException("missing start of file");
                    }
                }
                completedSize = sector * 500;
                remainingData = 500;
                if (remainingData > length - sector * 500)
                    remainingData = length - sector * 500;
            }
            if (remainingData > 0 && available >= remainingData) {
                expectingData = true;
                byte[] data = payload;
                int read = 0;
                if (current != null) {
                    data = current.buffer;
                    read = completedSize;
                }
                for (int skip = 0; skip < remainingData; skip += inputStream.read(data, skip + read, remainingData - skip))
                    ;
                if (remainingData + completedSize >= data.length && current != null) {
                    if (clientInstance.indices[0] != null)
                        clientInstance.indices[current.dataType + 1].writeFile(data.length, data, current.ID);
                    if (!current.incomplete && current.dataType == 3) {
                        current.incomplete = true;
                        current.dataType = 93;
                    }
                    if (current.incomplete)
                        synchronized (complete) {
                            complete.insertHead(current);
                        }
                    else {
                        current.unlink();
                    }
                }
                remainingData = 0;
            }
        } catch (IOException ex) {
            try {
                socket.close();
            } catch (Exception _ex) {
                _ex.printStackTrace();
            }
            socket = null;
            inputStream = null;
            remainingData = 0;
        }
    }

    public void initialize(FileArchive archive, Client client) {

        for (int i = 0; i < crcNames.length; i++) {
            byte[] crc_file = archive.readFile(crcNames[i]);
            int length = 0;

            if (crc_file != null) {
                length = crc_file.length / 4;
                Buffer crcStream = new Buffer(crc_file);
                crcs[i] = new int[length];
                fileStatus[i] = new byte[length];
                for (int ptr = 0; ptr < length; ptr++) {
                    crcs[i][ptr] = crcStream.readInt();
                }
            }
        }


        byte[] data = archive.readFile("map_index");
        Path externalMapIndex = Paths.get(SignLink.findcachedir(), "map_index");
        if (Files.exists(externalMapIndex)) {
            try {
                data = Files.readAllBytes(externalMapIndex);
                System.out.println("Loaded external map_index: " + externalMapIndex.toAbsolutePath());
            } catch (IOException ex) {
                System.out.println("Failed reading external map_index, falling back to archive copy.");
            }
        }
        Buffer stream = new Buffer(data);
        int j1 = stream.readUShort();//data.length / 6;
        areas = new int[j1];
        mapFiles = new int[j1];
        landscapes = new int[j1];
        file_amounts[3] = j1;
        for (int i2 = 0; i2 < j1; i2++) {
            areas[i2] = stream.readUShort();
            mapFiles[i2] = stream.readUShort();
            landscapes[i2] = stream.readUShort();
        }
        addMapBridgeEntry(14744, 4227, 4228);

        System.out.println("Loaded: " + file_amounts[3] + " maps");

        data = archive.readFile("midi_index");
        stream = new Buffer(data);
        j1 = data.length;
        file_amounts[2] = j1;
        musicPriorities = new int[j1];
        for (int k2 = 0; k2 < j1; k2++)
            musicPriorities[k2] = stream.readUnsignedByte();
        System.out.println("Loaded: " + file_amounts[2] + " sounds");
        //For some reason, model_index = anim_index and vice versa
        data = archive.readFile("model_index");
        file_amounts[1] = data.length;

        data = archive.readFile("anim_index");
        file_amounts[0] = data.length;
        System.out.println("Loaded: " + file_amounts[0] + " models");

        clientInstance = client;
        running = true;
        clientInstance.startRunnable(this, 2);
    }

    public void disable() {
        running = false;
    }

    public int getVersionCount(int index) {
        return versions[index].length;
    }

    private void request(Resource resource) {
 /*       try {

            if (socket == null || !socket.isConnected()) {
                socket = Client.instance.openSocket(JagGrabConstants.FILE_SERVER_PORT);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            }

            //Store opcode
            payload[0] = JagGrabConstants.ONDEMAND_REQUEST_OPCODE;

            //Store data type as byte
            payload[1] = (byte) resource.dataType;

            //Store file id as int
            payload[2] = (byte) (resource.ID >> 24);
            payload[3] = (byte) (resource.ID >> 16);
            payload[4] = (byte) (resource.ID >> 8);
            payload[5] = (byte) resource.ID;

            //Write the buffer
            outputStream.write(payload, 0, 6);

            deadTime = 0;
            errors = -10000;
            return;

        } catch (IOException ex) {
            //ex.printStackTrace();
        }
        try {
            socket.close();
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
        socket = null;
        inputStream = null;
        outputStream = null;
        remainingData = 0;
        errors++;*/
    }

    public void provide(int type, int file) {
        if (type < 0 || file < 0)
            return;
        if (type == 3 && missingMapArchives.contains(file)) {
            return;
        }
        synchronized (requests) {
            for (Resource resource = (Resource) requests.reverseGetFirst(); resource != null; resource = (Resource) requests.reverseGetNext())
                if (resource.dataType == type && resource.ID == file) {
                    return;
                }

            Resource resource = new Resource();
            resource.dataType = type;
            resource.ID = file;
            resource.incomplete = true;
            synchronized (mandatoryRequests) {
                mandatoryRequests.insertHead(resource);
            }
            requests.insertHead(resource);
        }
    }

    public int getModelIndex(int i) {
        return 0;
    }

    public void run() {
        try {
            while (running) {
                tick++;
                int sleepTime = 20;
                if (maximumPriority == 0 && clientInstance.indices[0] != null)
                    sleepTime = 50;
                try {
                    Thread.sleep(sleepTime);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                expectingData = true;
                for (int index = 0; index < 100; index++) {
                    if (!expectingData)
                        break;
                    expectingData = false;
                    loadMandatory();
                    requestMandatory();
                    if (uncompletedCount == 0 && index >= 5)
                        break;
                    loadExtra();
                    if (inputStream != null)
                        respond();
                }

                boolean idle = false;
                for (Resource resource = (Resource) requested.reverseGetFirst(); resource != null; resource = (Resource) requested.reverseGetNext())
                    if (resource.incomplete) {
                        idle = true;
                        resource.loopCycle++;
                        if (resource.loopCycle > 50) {
                            resource.loopCycle = 0;
                            request(resource);
                        }
                    }

                if (!idle) {
                    for (Resource resource = (Resource) requested.reverseGetFirst(); resource != null; resource = (Resource) requested.reverseGetNext()) {
                        idle = true;
                        resource.loopCycle++;
                        if (resource.loopCycle > 50) {
                            resource.loopCycle = 0;
                            request(resource);
                        }
                    }

                }
                if (idle) {
                    idleTime++;
                    if (idleTime > 750) {
                        try {
                            socket.close();
                        } catch (Exception _ex) {
                        }
                        socket = null;
                        inputStream = null;
                        remainingData = 0;
                    }
                } else {
                    idleTime = 0;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("od_ex " + exception.getMessage());
        }
    }

    public void loadExtra(int type, int file) {
        if (clientInstance.indices[0] == null) {
            return;
        } else if (maximumPriority == 0) {
            return;
        }
        Resource resource = new Resource();
        resource.dataType = file;
        resource.ID = type;
        resource.incomplete = false;
        synchronized (extras) {
            extras.insertHead(resource);
        }
    }

    public Resource next() {
        Resource resource;
        synchronized (complete) {
            resource = (Resource) complete.popHead();
        }
        if (resource == null)
            return null;
        synchronized (requests) {
            resource.unlinkCacheable();
        }
        if (resource.buffer == null)
            return resource;
        byte[] originalData = resource.buffer;
        int read = 0;
        try {
            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(resource.buffer));
            do {
                if (read == gzipInputBuffer.length)
                    throw new RuntimeException("buffer overflow!");
                int in = gis.read(gzipInputBuffer, read, gzipInputBuffer.length - read);
                if (in == -1)
                    break;
                read += in;
            } while (true);
        } catch (IOException _ex) {
            byte[] unpacked = unpackJs5Container(originalData);
            if (unpacked != null) {
                resource.buffer = unpacked;
                return resource;
            }
            if (resource.dataType == 3 && missingMapArchives.add(resource.ID)) {
                System.out.println("Failed to unzip archive [" + resource.ID + "] type = " + resource.dataType);
                System.out.println("Corrupt map archive skipped [id = " + resource.ID + "]");
                return null;
            }
            // Compatibility path: some imported caches store a subset of archives already
            // decoded (non-gzip). Let downstream loaders decide if the payload is usable.
            long key = (((long) resource.dataType) << 32) | (resource.ID & 0xffffffffL);
            if (rawArchiveFallbacks.add(key)) {
                System.out.println("Using raw archive fallback [type=" + resource.dataType + ", id=" + resource.ID + "]");
            }
            resource.buffer = originalData;
            return resource;
        }
        resource.buffer = new byte[read];
        System.arraycopy(gzipInputBuffer, 0, resource.buffer, 0, read);

        return resource;
    }

    private static byte[] unpackJs5Container(byte[] data) {
        if (data == null || data.length < 5) {
            return null;
        }
        int type = data[0] & 0xff;
        int compressedLength = readInt(data, 1);
        if (compressedLength < 0) {
            return null;
        }
        if (type == 0) {
            if (data.length < 5 + compressedLength) {
                return null;
            }
            byte[] out = new byte[compressedLength];
            System.arraycopy(data, 5, out, 0, compressedLength);
            return out;
        }
        if (data.length < 9) {
            return null;
        }
        int decompressedLength = readInt(data, 5);
        if (decompressedLength < 0 || data.length < 9 + compressedLength) {
            return null;
        }
        byte[] out = new byte[decompressedLength];
        if (type == 1) {
            BZip2Decompressor.decompress(out, decompressedLength, data, compressedLength, 9);
            return out;
        }
        if (type == 2) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data, 9, compressedLength))) {
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

    private static int readInt(byte[] data, int offset) {
        if (offset + 4 > data.length) {
            return -1;
        }
        return ((data[offset] & 0xff) << 24)
                | ((data[offset + 1] & 0xff) << 16)
                | ((data[offset + 2] & 0xff) << 8)
                | (data[offset + 3] & 0xff);
    }

    public int resolve(int landscapeOrObject, int regionY, int regionX) {
        int regionId = (regionX << 8) + regionY;
        for (int j1 = 0; j1 < areas.length; j1++) {
            if (areas[j1] == regionId) {
                if (landscapeOrObject == 0) {
                    return mapFiles[j1];
                } else {
                    return landscapes[j1];
                }
            }
        }
        return -1;
    }

    public boolean landscapePresent(int landscape) {
        for (int index = 0; index < areas.length; index++)
            if (landscapes[index] == landscape)
                return true;
        return false;
    }

    private void addMapBridgeEntry(int regionId, int terrainArchive, int objectArchive) {
        for (int i = 0; i < areas.length; i++) {
            if (areas[i] == regionId) {
                mapFiles[i] = terrainArchive;
                landscapes[i] = objectArchive;
                return;
            }
        }
        int newLen = areas.length + 1;
        areas = java.util.Arrays.copyOf(areas, newLen);
        mapFiles = java.util.Arrays.copyOf(mapFiles, newLen);
        landscapes = java.util.Arrays.copyOf(landscapes, newLen);
        areas[newLen - 1] = regionId;
        mapFiles[newLen - 1] = terrainArchive;
        landscapes[newLen - 1] = objectArchive;
        file_amounts[3] = newLen;
        System.out.println("Added map bridge region " + regionId + " -> " + terrainArchive + "," + objectArchive);
    }

    private void requestMandatory() {
        uncompletedCount = 0;
        completedCount = 0;
        for (Resource resource = (Resource) requested.reverseGetFirst(); resource != null; resource = (Resource) requested.reverseGetNext())
            if (resource.incomplete) {
                uncompletedCount++;
                if (resource.loopCycle > 50) {
                    long key = (((long) resource.dataType) << 32) | (resource.ID & 0xffffffffL);
                    if (staleMandatoryWarnings.add(key)) {
                        System.out.println("Stale archive request [type=" + resource.dataType + " (" + resourceTypeName(resource.dataType) + "), id=" + resource.ID + "]");
                    }
                }
            } else
                completedCount++;

        while (uncompletedCount < 10) { // 10
            Resource request = (Resource) unrequested.popHead();
            if (request == null) {
                break;
            }
            try {
                if (hasFileStatusEntry(request.dataType, request.ID)) {
                    if (fileStatus[request.dataType][request.ID] != 0) {
                        filesLoaded++;
                    }
                    fileStatus[request.dataType][request.ID] = 0;
                }
                requested.insertHead(request);
                uncompletedCount++;
                request(request);
                expectingData = true;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static String resourceTypeName(int dataType) {
        switch (dataType) {
            case 0:
                return "model";
            case 1:
                return "animation";
            case 2:
                return "midi";
            case 3:
                return "map";
            case 93:
                return "map-passive";
            default:
                return "unknown";
        }
    }

    public void clearExtras() {
        synchronized (extras) {
            extras.clear();
        }
    }

    private void loadMandatory() {
        Resource resource;
        synchronized (mandatoryRequests) {
            resource = (Resource) mandatoryRequests.popHead();
        }
        while (resource != null) {
            expectingData = true;
            byte[] data = null;

            if (clientInstance.indices[0] != null)
                data = clientInstance.indices[resource.dataType + 1].decompress(resource.ID);

            //CRC MATCHING
            /*if (Configuration.JAGCACHED_ENABLED) {
                if (!crcMatches(crcs[resource.dataType][resource.ID], data)) {
                    data = null;
                }
            }*/

            synchronized (mandatoryRequests) {
                if (data == null) {
                    if (resource.dataType == 3) {
                        if (missingMapArchives.add(resource.ID)) {
                            System.out.println("Missing map archive skipped [id = " + resource.ID + "]");
                        }
                        synchronized (requests) {
                            resource.unlinkCacheable();
                        }
                        resource.unlink();
                    } else {
                        unrequested.insertHead(resource);
                    }
                } else {
                    resource.buffer = data;
                    synchronized (complete) {
                        complete.insertHead(resource);
                    }
                }
                resource = (Resource) mandatoryRequests.popHead();
            }
        }
    }


    private void loadExtra() {
        while (uncompletedCount == 0 && completedCount < 10) {
            if (maximumPriority == 0)
                break;
            Resource resource;
            synchronized (extras) {
                resource = (Resource) extras.popHead();
            }
            while (resource != null) {
                if (hasFileStatusEntry(resource.dataType, resource.ID)
                        && fileStatus[resource.dataType][resource.ID] != 0) {
                    fileStatus[resource.dataType][resource.ID] = 0;
                    requested.insertHead(resource);
                    request(resource);
                    expectingData = true;
                    if (filesLoaded < totalFiles)
                        filesLoaded++;
                    completedCount++;
                    if (completedCount == 10)
                        return;
                }
                synchronized (extras) {
                    resource = (Resource) extras.popHead();
                }
            }
            for (int type = 0; type < 4; type++) {
                byte[] data = fileStatus[type];
                int size = data.length;
                for (int file = 0; file < size; file++)
                    if (data[file] == maximumPriority) {
                        data[file] = 0;
                        Resource newResource = new Resource();
                        newResource.dataType = type;
                        newResource.ID = file;
                        newResource.incomplete = false;
                        requested.insertHead(newResource);
                        request(newResource);
                        expectingData = true;
                        if (filesLoaded < totalFiles)
                            filesLoaded++;
                        completedCount++;
                        if (completedCount == 10)
                            return;
                    }
            }
            maximumPriority--;
        }
    }

    private boolean hasFileStatusEntry(int type, int id) {
        if (type < 0 || type >= fileStatus.length || id < 0) {
            return false;
        }
        byte[] status = fileStatus[type];
        return status != null && id < status.length;
    }

}

