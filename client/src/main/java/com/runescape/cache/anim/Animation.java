package com.runescape.cache.anim;

import com.runescape.cache.FileArchive;
import com.runescape.io.Buffer;
import com.runescape.sign.SignLink;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Animation {
    private static final Set<Integer> ALLOWED_2446_OVERRIDES = new HashSet<>(Arrays.asList(
            10815, 10818, 10819,
            11051, 11052, 11053, 11055,
            11057, 11058, 11059, 11060, 11061, 11062, 11063, 11064,
            11240, 11275, 11463, 11464
    ));
    private static final Set<Integer> loggedUnknownOpcodes = new HashSet<>();

    public static Animation[] animations;
    public int frameCount;
    public int[] primaryFrames;
    public int[] secondaryFrames;
    public int[] durations;
    public int loopOffset;
    public int[] interleaveOrder;
    public boolean stretches;
    public int forcedPriority;
    public int playerOffhand;
    public int playerMainhand;
    public int maximumLoops;
    public int animatingPrecedence;
    public int priority;
    public int replayMode;
    private int skeletalRangeBegin;
    private int skeletalRangeEnd;
    private int skeletalId;
    private boolean[] masks;
    public Map<Integer, Integer> skeletalSounds;

    private Animation() {
        loopOffset = -1;
        skeletalId = -1;
        skeletalRangeEnd = -1;
        skeletalRangeBegin = -1;
        stretches = false;
        forcedPriority = 5;
        playerOffhand = -1; //Removes shield
        playerMainhand = -1; //Removes weapon
        maximumLoops = 99;
        animatingPrecedence = -1; //Stops character from moving
        priority = -1;
        replayMode = 1;
    }

    public static void init(FileArchive archive) {
        byte[] seqData = archive.readFile("seq.dat");
        Path extSeqDat = Paths.get(SignLink.findcachedir(), "seq.dat");
        if (Files.exists(extSeqDat)) {
            try {
                seqData = Files.readAllBytes(extSeqDat);
                System.out.println("Loaded external animations: " + extSeqDat.toAbsolutePath());
            } catch (Exception ignored) {
            }
        }
        Buffer stream = new Buffer(seqData);
        int length = stream.readUShort();
        if (animations == null)
            animations = new Animation[length];
        for (int j = 0; j < length; j++) {
            if (stream.currentPosition >= stream.payload.length) {
                System.out.println("Animation.init: reached end of seq.dat at entry " + j + "/" + length);
                break;
            }
            if (animations[j] == null) {
                animations[j] = new Animation();
            }
            int startPos = stream.currentPosition;
            try {
                animations[j].decode(stream);
            } catch (Exception ex) {
                // Recover by seeking to the next entry terminator.
                int pos = Math.max(startPos, 0);
                while (pos < stream.payload.length && (stream.payload[pos] & 0xFF) != 0) {
                    pos++;
                }
                stream.currentPosition = Math.min(pos + 1, stream.payload.length);
            }

        }

        load2446Overrides();
        ensureEclipseSlots();

        System.out.println("Loaded: " + length + " animations");
        dumpEclipseAnimationCandidates();
    }

    private static void load2446Overrides() {
        Path cacheDir = Paths.get(SignLink.findcachedir());
        int loaded = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(cacheDir, "e2446_seq_*.dat")) {
            for (Path path : ds) {
                String name = path.getFileName().toString();
                int idStart = "e2446_seq_".length();
                int idEnd = name.lastIndexOf(".dat");
                if (idEnd <= idStart) {
                    continue;
                }

                int id;
                try {
                    id = Integer.parseInt(name.substring(idStart, idEnd));
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (!ALLOWED_2446_OVERRIDES.contains(id)) {
                    continue;
                }

                byte[] data = Files.readAllBytes(path);
                Animation override = new Animation();
                try {
                    decode2446Sequence(override, new Buffer(data));
                } catch (Exception ex) {
                    System.out.println("Failed loading 2446 sequence override " + id + ": " + ex.getMessage());
                    continue;
                }

                if (id >= animations.length) {
                    animations = Arrays.copyOf(animations, id + 1);
                }
                animations[id] = override;
                loaded++;
            }
        } catch (IOException ignored) {
        }

        if (loaded > 0) {
            System.out.println("Loaded 2446 sequence overrides: " + loaded);
        }
    }

    private static void decode2446Sequence(Animation out, Buffer buffer) {
        out.decode(buffer);
    }

    private static void ensureEclipseSlots() {
        final int[] eclipseIds = {10815, 10818, 10819, 11051, 11052, 11053, 11055, 11057, 11058, 11059, 11060, 11061, 11062, 11063, 11064, 11240, 11275, 11463, 11464};
        int max = 0;
        for (int id : eclipseIds) {
            if (id > max) {
                max = id;
            }
        }
        if (animations.length <= max) {
            animations = Arrays.copyOf(animations, max + 1);
        }
        for (int id : eclipseIds) {
            if (animations[id] == null) {
                Animation noop = new Animation();
                noop.frameCount = 1;
                noop.primaryFrames = new int[]{-1};
                noop.secondaryFrames = new int[]{-1};
                noop.durations = new int[]{1};
                animations[id] = noop;
            }
        }
    }

    private static void dumpEclipseAnimationCandidates() {
        // Eclipse atlatl worn model ids from 2446 item defs.
        final int[] eclipseMainhandModelIds = {51175, 51138, 52263};
        int explicitHandAnimations = 0;
        for (int i = 0; i < animations.length; i++) {
            Animation a = animations[i];
            if (a == null) {
                continue;
            }
            for (int modelId : eclipseMainhandModelIds) {
                if (a.playerMainhand == modelId || a.playerOffhand == modelId) {
                    System.out.println("ECLIPSE_ANIM_CANDIDATE seq=" + i
                            + " frames=" + a.frameCount
                            + " mainhand=" + a.playerMainhand
                            + " offhand=" + a.playerOffhand
                            + " priority=" + a.priority
                            + " forcedPriority=" + a.forcedPriority);
                    break;
                }
            }
            if (a.playerMainhand != -1 || a.playerOffhand != -1) {
                explicitHandAnimations++;
                if (a.playerMainhand >= 50000 || a.playerOffhand >= 50000) {
                    System.out.println("HAND_ANIM_HIGH_MODEL seq=" + i
                            + " mainhand=" + a.playerMainhand
                            + " offhand=" + a.playerOffhand
                            + " frames=" + a.frameCount);
                }
            }
        }
        System.out.println("HAND_ANIM_TOTAL=" + explicitHandAnimations);
    }

    public int duration(int i) {
        int j = durations[i];
        if (j == 0) {
            Frame frame = Frame.method531(primaryFrames[i]);
            if (frame != null) {
                j = durations[i] = frame.duration;
            }
        }
        if (j == 0)
            j = 1;
        return j;
    }

    private void decode(Buffer buffer) {        
        while(true) {
            final int opcode = buffer.readUnsignedByte();

            if (opcode == 0) {
                break;
            } else if (opcode == 1) {
                frameCount = buffer.readUShort();
                primaryFrames = new int[frameCount];
                secondaryFrames = new int[frameCount];
                durations = new int[frameCount];

                for (int i = 0; i < frameCount; i++) {
                    durations[i] = buffer.readUShort();
                }

                for (int i = 0; i < frameCount; i++) {
                    primaryFrames[i] = buffer.readUShort();
                    secondaryFrames[i] = -1;
                }

                for (int i = 0; i < frameCount; i++) {
                    primaryFrames[i] += buffer.readUShort() << 16;
                }
            } else if (opcode == 2) {
                loopOffset = buffer.readUShort();
            } else if (opcode == 3) {
                int len = buffer.readUnsignedByte();
                interleaveOrder = new int[len + 1];
                for (int i = 0; i < len; i++) {
                    interleaveOrder[i] = buffer.readUnsignedByte();
                }
                interleaveOrder[len] = 9999999;
            } else if (opcode == 4) {
                stretches = true;
            } else if (opcode == 5) {
                forcedPriority = buffer.readUnsignedByte();
            } else if (opcode == 6) {
                playerOffhand = buffer.readUShort();
            } else if (opcode == 7) {
                playerMainhand = buffer.readUShort();
            } else if (opcode == 8) {
                maximumLoops = buffer.readUnsignedByte();
            } else if (opcode == 9) {
                animatingPrecedence = buffer.readUnsignedByte();
            } else if (opcode == 10) {
                priority = buffer.readUnsignedByte();
            } else if (opcode == 11) {
                replayMode = buffer.readUnsignedByte();
            } else if (opcode == 12) {
                int len = buffer.readUnsignedByte();

                for (int i = 0; i < len; i++) {
                    buffer.readUShort();
                }

                for (int i = 0; i < len; i++) {
                    buffer.readUShort();
                }
            } else if (opcode == 13) {
                int len = buffer.readUnsignedByte();

                for (int i = 0; i < len; i++) {
                    buffer.read24Int();
                }
            } else if (opcode == 14) {
                skeletalId = buffer.readInt();
            } else if (opcode == 15) {
                skeletalSounds = new HashMap<Integer, Integer>();
                int count = buffer.readUShort();
                for (int index = 0; index < count; index++) {
                    skeletalSounds.put(buffer.readUShort(), buffer.read24Int());
                }
            } else if (opcode == 16) {
                skeletalRangeBegin = buffer.readUShort();
                skeletalRangeEnd = buffer.readUShort();
            } else if (opcode == 17) {
                masks = new boolean[256];
                Arrays.fill(masks, false);
                int count = buffer.readUnsignedByte();
                for (int index = 0; index < count; index++) {
                    masks[buffer.readUnsignedByte()] = true;
                }
            } else if (opcode == 18 || opcode == 19 || opcode == 35) {
                // Newer cache feature flags with no payload for this client.
            } else {
                if (loggedUnknownOpcodes.add(opcode)) {
                    System.out.println("seq invalid opcode: " + opcode + " (resyncing entry)");
                }
                // Resync this sequence entry by scanning until its 0 terminator so
                // we don't desync the whole seq.dat stream.
                while (buffer.currentPosition < buffer.payload.length) {
                    if (buffer.readUnsignedByte() == 0) {
                        break;
                    }
                }
                break;
            }
        }
        if (frameCount == 0) {
            frameCount = 1;
            primaryFrames = new int[1];
            primaryFrames[0] = -1;
            secondaryFrames = new int[1];
            secondaryFrames[0] = -1;
            durations = new int[1];
            durations[0] = -1;
        }

        if (animatingPrecedence == -1) {
            animatingPrecedence = (interleaveOrder == null) ? 0 : 2;
        }

        if (priority == -1) {
            priority = (interleaveOrder == null) ? 0 : 2;
        }
    }

}
