package com.runescape.cache.anim;

import com.runescape.io.Buffer;

public final class FrameBase {

    /**
     * The type of each transformation.
     */
    public final int[] transformationType;

    public final int[][] skinList;

    public FrameBase(int[] transformationType, int[][] skinList) {
        this.transformationType = transformationType;
        this.skinList = skinList;
    }

    public FrameBase(Buffer stream) {
        int count = stream.readUShort();
        
        if (count < 0 || count > 10000) {
            throw new RuntimeException("Invalid frame base count: " + count);
        }

        transformationType = new int[count];
        skinList = new int[count][];

        for (int index = 0; index < count; index++) {
            transformationType[index] = stream.readUShort();
        }

        for (int label = 0; label < count; label++) {
            int skinCount = stream.readUShort();
            if (skinCount < 0 || skinCount > 10000) {
                throw new RuntimeException("Invalid skin count: " + skinCount);
            }
            skinList[label] = new int[skinCount];
        }

        for (int label = 0; label < count; label++) {
            for (int index = 0; index < skinList[label].length; index++) {
                skinList[label][index] = stream.readUShort();
            }
        }

    }

}
