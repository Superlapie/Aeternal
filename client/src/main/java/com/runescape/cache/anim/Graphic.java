package com.runescape.cache.anim;

import com.runescape.cache.FileArchive;
import com.runescape.collection.ReferenceCache;
import com.runescape.entity.model.Model;
import com.runescape.io.Buffer;
import com.runescape.sign.SignLink;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public final class Graphic {

    public static Graphic[] cache;
    public static ReferenceCache models = new ReferenceCache(30);
    private int[] originalModelColours;
    private int[] modifiedModelColours;
    public Animation animationSequence;
    public int resizeXY;
    public int resizeZ;
    public int rotation;
    public int modelBrightness;
    public int modelShadow;
    private int anInt404;
    private int modelId;
    private int animationId;

    private Graphic() {
        animationId = -1;
        originalModelColours = new int[6];
        modifiedModelColours = new int[6];
        resizeXY = 128;
        resizeZ = 128;
    }

    public static void init(FileArchive archive) {
        byte[] spotanimData = archive.readFile("spotanim.dat");
        Path extSpotanimDat = Paths.get(SignLink.findcachedir(), "spotanim.dat");
        if (Files.exists(extSpotanimDat)) {
            try {
                spotanimData = Files.readAllBytes(extSpotanimDat);
                System.out.println("Loaded external graphics: " + extSpotanimDat.toAbsolutePath());
            } catch (Exception ignored) {
            }
        }

        Buffer stream = new Buffer(spotanimData);
        int length = stream.readUShort();
        if (cache == null)
            cache = new Graphic[length + 1];
        for (int index = 0; index < length; index++) {
            if (cache[index] == null)
                cache[index] = new Graphic();
            cache[index].anInt404 = index;
            cache[index].readValues(stream);
        }

        load2446Overrides();
        ensureEclipseSlots();

        System.out.println("Loaded: " + length + " graphics");
    }

    private static void load2446Overrides() {
        Path cacheDir = Paths.get(SignLink.findcachedir());
        int loaded = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(cacheDir, "e2446_spot_*.dat")) {
            for (Path path : ds) {
                String name = path.getFileName().toString();
                int idStart = "e2446_spot_".length();
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

                byte[] data = Files.readAllBytes(path);
                Graphic override = new Graphic();
                override.anInt404 = id;
                override.readValues(new Buffer(data));
                if (override.animationId != -1
                        && (Animation.animations == null
                        || override.animationId >= Animation.animations.length
                        || Animation.animations[override.animationId] == null)) {
                    System.out.println("Skipped invalid 2446 spotanim override " + id + " (missing anim " + override.animationId + ")");
                    continue;
                }

                if (id >= cache.length) {
                    cache = Arrays.copyOf(cache, id + 1);
                }
                cache[id] = override;
                loaded++;
            }
        } catch (IOException ignored) {
        }

        if (loaded > 0) {
            System.out.println("Loaded 2446 spotanim overrides: " + loaded);
        }
    }

    private static void ensureEclipseSlots() {
        final int[] eclipseIds = {2709, 2710, 2711, 2712, 3017, 3030};
        int max = 0;
        for (int id : eclipseIds) {
            if (id > max) {
                max = id;
            }
        }
        if (cache.length <= max) {
            cache = Arrays.copyOf(cache, max + 1);
        }
    }

    public void readValues(Buffer buffer) {
        while(true) {
            final int opcode = buffer.readUnsignedByte();

            if (opcode == 0) {
                return;
            } else if (opcode == 1) {
                modelId = buffer.readUShort();
            } else if (opcode == 2) {
                animationId = buffer.readUShort();

                if (Animation.animations != null && animationId >= 0 && animationId < Animation.animations.length)
                    animationSequence = Animation.animations[animationId];
            } else if (opcode == 4) {
                resizeXY = buffer.readUShort();
            } else if (opcode == 5) {
                resizeZ = buffer.readUShort();
            } else if (opcode == 6) {
                rotation = buffer.readUShort();
            } else if (opcode == 7) {
                modelBrightness = buffer.readUnsignedByte();
            } else if (opcode == 8) {
                modelShadow = buffer.readUnsignedByte();
            } else if (opcode == 40) {
                int len = buffer.readUnsignedByte();
                originalModelColours = new int[len];
                modifiedModelColours = new int[len];
                for (int i = 0; i < len; i++) {
                    originalModelColours[i] = buffer.readUShort();
                    modifiedModelColours[i] = buffer.readUShort();
                }
            } else if (opcode == 41) { // re-texture
                int len = buffer.readUnsignedByte();

                for (int i = 0; i < len; i++) {
                    buffer.readUShort();
                    buffer.readUShort();
                }
            } else {
                System.out.println("gfx invalid opcode: " + opcode);
            }
        }
    }

    public Model getModel() {
        Model model = (Model) models.get(anInt404);
        if (model != null)
            return model;
        model = Model.getModel(modelId);
        if (model == null)
            return null;
        if (originalModelColours == null ||
                originalModelColours.length == 0 ||
                originalModelColours.length != modifiedModelColours.length) {
            return model;
        }

        for (int i = 0; i < originalModelColours.length; i++)
            if (originalModelColours[0] != 0)
                model.recolor(originalModelColours[i], modifiedModelColours[i]);

        models.put(model, anInt404);
        return model;
    }
}
