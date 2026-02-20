package com.runescape.cache.anim;

import com.google.gson.Gson;
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class Animation {
    private static final boolean VERBOSE_ANIM_LOGS = Boolean.getBoolean("client.verboseAnimLogs");
    private static final Set<Integer> ALLOWED_2446_OVERRIDES = new HashSet<>(Arrays.asList(
            8139, 8140, 8141, 8142,
            8531, 8532,
            10815, 10818, 10819,
            11051, 11052, 11053, 11055,
            11057, 11058, 11059, 11060, 11061, 11062, 11063, 11064,
            11240, 11275, 11463, 11464,
            12095, 12096, 12097, 12098, 12099, 12100, 12101, 12102, 12103, 12104,
            12105, 12106, 12107, 12108, 12109, 12110, 12111, 12112, 12113, 12114,
            12115, 12116, 12117, 12118, 12119, 12120, 12121, 12128, 12129, 12130,
            12138, 12149, 12152, 12169, 12170, 12171, 12172, 12173, 12174, 12175,
            12176, 12196,
            12142, 12144, 12145, 12146, 12147, 12148, 12156, 12160, 12161,
            // Yama true ids from 2446 export are retained as slots; these entries are
            // skeletal/maya in this client and may fallback if not directly playable.
            12140, 12141
    ));
    private static final Set<Integer> NIGHTMARE_STAFF_SEQUENCE_IDS = new HashSet<>(Arrays.asList(
            8139, 8140, 8141, 8142, 8531, 8532
    ));
    private static final Set<Integer> OPCODE14_USHORT_2446 = new HashSet<>(Arrays.asList(
            11338, 11339, 11342, 11345, 11350, 11355, 11358
    ));
    private static boolean decodeOpcode14AsUShort = false;
    private static final Set<Integer> loggedUnknownOpcodes = new HashSet<>();
    private static final Gson GSON = new Gson();

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
        loadAraxxorOverridesFromExt();
        materializeAnimayaFallbacks();
        ensureEclipseSlots();

        System.out.println("Loaded: " + length + " animations");
        dumpEclipseAnimationCandidates();
    }

    private static void materializeAnimayaFallbacks() {
        if (animations == null || Frame.animationlist == null) {
            return;
        }
        int converted = 0;
        int[] yamaProbeIds = {12140, 12141, 12144, 12145, 12146, 12148};
        for (int id = 0; id < animations.length; id++) {
            Animation anim = animations[id];
            if (anim == null || anim.hasClassicFrames() || anim.skeletalId == -1) {
                continue;
            }

            int packed = anim.skeletalId;
            int sourceGroup = packed >>> 16;
            int baseFile = packed & 0xFFFF;
            if (sourceGroup <= 0) {
                continue;
            }

            int aliasGroup = 24000 + sourceGroup;
            Frame.register2446AliasGroup(aliasGroup, sourceGroup);
            Frame.method531((aliasGroup << 16) | Math.max(0, baseFile));

            if (aliasGroup >= Frame.animationlist.length || Frame.animationlist[aliasGroup] == null) {
                if (contains(yamaProbeIds, id)) {
                    if (VERBOSE_ANIM_LOGS) {
                        System.out.println("Animaya fallback skipped seq " + id + " (group " + sourceGroup + " not loadable)");
                    }
                }
                continue;
            }

            Frame[] groupFrames = Frame.animationlist[aliasGroup];
            int available = 0;
            for (int i = 0; i < groupFrames.length; i++) {
                if (groupFrames[i] != null) {
                    available++;
                }
            }
            if (available == 0) {
                if (contains(yamaProbeIds, id)) {
                    if (VERBOSE_ANIM_LOGS) {
                        System.out.println("Animaya fallback skipped seq " + id + " (group " + sourceGroup + " has 0 classic frames)");
                    }
                }
                continue;
            }

            int start = Math.max(0, baseFile);
            int end = groupFrames.length;
            if (anim.skeletalRangeEnd > anim.skeletalRangeBegin && anim.skeletalRangeBegin >= 0) {
                start = Math.max(start, anim.skeletalRangeBegin);
                end = Math.min(end, anim.skeletalRangeEnd);
            }
            if (start >= end) {
                start = Math.max(0, baseFile);
                end = groupFrames.length;
            }

            int frameCount = 0;
            for (int i = start; i < end; i++) {
                if (i < groupFrames.length && groupFrames[i] != null) {
                    frameCount++;
                }
            }
            if (frameCount == 0) {
                if (contains(yamaProbeIds, id)) {
                    if (VERBOSE_ANIM_LOGS) {
                        System.out.println("Animaya fallback skipped seq " + id + " (no frames in selected range)");
                    }
                }
                continue;
            }

            anim.frameCount = frameCount;
            anim.primaryFrames = new int[frameCount];
            anim.secondaryFrames = new int[frameCount];
            anim.durations = new int[frameCount];
            int write = 0;
            for (int i = start; i < end; i++) {
                if (i >= groupFrames.length || groupFrames[i] == null) {
                    continue;
                }
                anim.primaryFrames[write] = (aliasGroup << 16) | i;
                anim.secondaryFrames[write] = -1;
                anim.durations[write] = 1;
                write++;
            }
            anim.clearSkeletalFlags();
            converted++;
            if (contains(yamaProbeIds, id)) {
                if (VERBOSE_ANIM_LOGS) {
                    System.out.println("Animaya fallback materialized seq " + id + " from group " + sourceGroup + " using " + frameCount + " frames");
                }
            }
        }
        if (VERBOSE_ANIM_LOGS && converted > 0) {
            System.out.println("Materialized Animaya fallbacks: " + converted);
        }
    }

    private static boolean contains(int[] arr, int value) {
        for (int x : arr) {
            if (x == value) {
                return true;
            }
        }
        return false;
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
                    decode2446Sequence(id, override, new Buffer(data));
                    remapLowFrameGroupsToAliases(override, id);
                    sanitizeNightmareStaffSequence(override, id);
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

        if (VERBOSE_ANIM_LOGS && loaded > 0) {
            System.out.println("Loaded 2446 sequence overrides: " + loaded);
        }
    }

    private static void loadAraxxorOverridesFromExt() {
        Path jsonPath = Paths.get("").toAbsolutePath().normalize().resolve("_ext").resolve("AraxxorAnimations.json");
        if (!Files.exists(jsonPath)) {
            return;
        }
        int loaded = 0;
        try {
            String json = new String(Files.readAllBytes(jsonPath));
            AraxxorAnimEntry[] entries = GSON.fromJson(json, AraxxorAnimEntry[].class);
            if (entries == null) {
                return;
            }
            for (AraxxorAnimEntry entry : entries) {
                if (entry == null || entry.id <= 0 || entry.frameIDs == null || entry.frameIDs.length == 0) {
                    continue;
                }
                if (animations == null) {
                    return;
                }
                if (entry.id >= animations.length) {
                    animations = Arrays.copyOf(animations, entry.id + 1);
                }
                Animation a = new Animation();
                int count = entry.frameIDs.length;
                a.frameCount = count;
                a.primaryFrames = Arrays.copyOf(entry.frameIDs, count);
                a.secondaryFrames = new int[count];
                Arrays.fill(a.secondaryFrames, -1);
                a.durations = new int[count];
                if (entry.frameLengths != null && entry.frameLengths.length == count) {
                    for (int i = 0; i < count; i++) {
                        a.durations[i] = Math.max(1, entry.frameLengths[i]);
                    }
                } else {
                    Arrays.fill(a.durations, 3);
                }
                if (entry.frameLoop != null) {
                    a.loopOffset = entry.frameLoop;
                }
                if (entry.priority != null) {
                    a.priority = entry.priority;
                }
                if (entry.forcedPriority != null) {
                    a.forcedPriority = entry.forcedPriority;
                }
                if (entry.maxLoops != null) {
                    a.maximumLoops = entry.maxLoops;
                }
                if (entry.stretches != null) {
                    a.stretches = entry.stretches;
                }
                remapLowFrameGroupsToAliases(a, entry.id);
                animations[entry.id] = a;
                loaded++;
            }
        } catch (Exception ex) {
            System.out.println("Failed loading Araxxor animation overrides: " + ex.getMessage());
            return;
        }
        if (VERBOSE_ANIM_LOGS && loaded > 0) {
            System.out.println("Loaded Araxxor animation overrides: " + loaded);
        }
    }

    private static final class AraxxorAnimEntry {
        int id;
        int[] frameLengths;
        int[] frameIDs;
        Integer frameLoop;
        Integer priority;
        Integer forcedPriority;
        Integer maxLoops;
        Boolean stretches;
    }

    private static void decode2446Sequence(int id, Animation out, Buffer buffer) {
        boolean old = decodeOpcode14AsUShort;
        decodeOpcode14AsUShort = OPCODE14_USHORT_2446.contains(id);
        try {
            out.decode(buffer);
        } finally {
            decodeOpcode14AsUShort = old;
        }
    }

    private static void ensureEclipseSlots() {
        final int[] eclipseIds = {
                10815, 10818, 10819,
                11051, 11052, 11053, 11055, 11057, 11058, 11059, 11060, 11061, 11062, 11063, 11064,
                11240, 11275,
                11463, 11464,
                12095, 12096, 12097, 12098, 12099, 12100, 12101, 12102, 12103, 12104,
                12105, 12106, 12107, 12108, 12109, 12110, 12111, 12112, 12113, 12114,
                12115, 12116, 12117, 12118, 12119, 12120, 12121, 12128, 12129, 12130,
                12138, 12149, 12152, 12169, 12170, 12171, 12172, 12173, 12174, 12175,
                12176, 12196,
                12142, 12144, 12145, 12146, 12147, 12148, 12156, 12160, 12161,
                12140, 12141
        };
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
                    if (VERBOSE_ANIM_LOGS) System.out.println("ECLIPSE_ANIM_CANDIDATE seq=" + i
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
                    if (VERBOSE_ANIM_LOGS) System.out.println("HAND_ANIM_HIGH_MODEL seq=" + i
                            + " mainhand=" + a.playerMainhand
                            + " offhand=" + a.playerOffhand
                            + " frames=" + a.frameCount);
                }
            }
        }
        if (VERBOSE_ANIM_LOGS) {
            System.out.println("HAND_ANIM_TOTAL=" + explicitHandAnimations);
        }
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

    public boolean hasClassicFrames() {
        return primaryFrames != null && primaryFrames.length > 0 && primaryFrames[0] != -1;
    }

    public boolean isSkeletalSequence() {
        return !hasClassicFrames() && (skeletalId != -1 || (masks != null) || skeletalRangeBegin != -1 || skeletalRangeEnd != -1);
    }

    public int getSkeletalId() {
        return skeletalId;
    }

    public int getSkeletalRangeBegin() {
        return skeletalRangeBegin;
    }

    public int getSkeletalRangeEnd() {
        return skeletalRangeEnd;
    }

    public void clearSkeletalFlags() {
        skeletalId = -1;
        skeletalRangeBegin = -1;
        skeletalRangeEnd = -1;
        masks = null;
        skeletalSounds = null;
    }

    private static void remapLowFrameGroupsToAliases(Animation animation, int sequenceId) {
        if (animation == null || animation.primaryFrames == null) {
            return;
        }
        Set<Integer> remappedGroups = new LinkedHashSet<>();
        for (int i = 0; i < animation.primaryFrames.length; i++) {
            int frame = animation.primaryFrames[i];
            if (frame < 0) {
                continue;
            }
            int group = frame >>> 16;
            int file = frame & 0xFFFF;
            // Yama VFX overrides commonly use low-id frame groups that collide with legacy 317 groups.
            // Move them into a high alias range and have Frame load the real source group on demand.
            if (group > 0 && group < 4000) {
                int aliasGroup = 20000 + group;
                Frame.register2446AliasGroup(aliasGroup, group);
                animation.primaryFrames[i] = (aliasGroup << 16) | file;
                remappedGroups.add(group);
                if (animation.secondaryFrames != null && i < animation.secondaryFrames.length && animation.secondaryFrames[i] >= 0) {
                    int secFile = animation.secondaryFrames[i] & 0xFFFF;
                    animation.secondaryFrames[i] = (aliasGroup << 16) | secFile;
                }
            }
        }
        for (int group : remappedGroups) {
            int aliasGroup = 20000 + group;
            if (VERBOSE_ANIM_LOGS) {
                System.out.println("Remapped 2446 low frame group " + group + " -> " + aliasGroup + " for seq " + sequenceId);
            }
        }
    }

    private static void sanitizeNightmareStaffSequence(Animation animation, int id) {
        if (animation == null || !NIGHTMARE_STAFF_SEQUENCE_IDS.contains(id)) {
            return;
        }
        // 2446 sequences can include modern hand model swaps that don't map cleanly in this client,
        // which causes warped/broken playback when wielding the staff.
        animation.playerMainhand = -1;
        animation.playerOffhand = -1;
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
                skeletalId = decodeOpcode14AsUShort ? buffer.readUShort() : buffer.readInt();
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
                if (VERBOSE_ANIM_LOGS && loggedUnknownOpcodes.add(opcode)) {
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
