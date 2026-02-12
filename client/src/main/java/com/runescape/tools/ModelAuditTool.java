package com.runescape.tools;

import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.def.ItemDefinition;
import com.runescape.cache.def.NpcDefinition;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Audits item/npc model references against idx1 model entries.
 *
 * Usage:
 * java com.runescape.tools.ModelAuditTool <cacheDir>
 */
public final class ModelAuditTool {

    private static final String[] KEYWORDS = {
            "nightmare", "phosani", "inquisitor",
            "araxxor", "yama", "moon", "blood moon", "blue moon", "eclipse moon",
            "vardorvis", "leviathan", "whisperer", "duke sucellus", "dt2"
    };

    private ModelAuditTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java com.runescape.tools.ModelAuditTool <cacheDir>");
            return;
        }

        String cacheDir = normalize(args[0]);
        File datFile = resolveDatFile(cacheDir);
        File idx0File = new File(cacheDir + "main_file_cache.idx0");
        File idx1File = new File(cacheDir + "main_file_cache.idx1");
        File objDatFile = new File(cacheDir + "obj.dat");
        File objIdxFile = new File(cacheDir + "obj.idx");

        if (!datFile.exists() || !idx0File.exists() || !idx1File.exists() || !objDatFile.exists() || !objIdxFile.exists()) {
            throw new IllegalStateException("Cache dir missing required files (.dat/.dat2, idx0, idx1, obj.dat, obj.idx): " + cacheDir);
        }

        byte[] idx1Bytes;
        try (RandomAccessFile idxRaf = new RandomAccessFile(idx1File, "r")) {
            idx1Bytes = new byte[(int) idxRaf.length()];
            idxRaf.readFully(idx1Bytes);
        }
        int modelEntryCount = idx1Bytes.length / 6;

        FileArchive configArchive;
        try (RandomAccessFile datRaf = new RandomAccessFile(datFile, "r");
             RandomAccessFile idx0Raf = new RandomAccessFile(idx0File, "r")) {
            FileStore index0 = new FileStore(datRaf, idx0Raf, 1);
            byte[] configBytes = index0.decompress(2);
            if (configBytes == null) {
                throw new IllegalStateException("Could not decompress config archive (index0 file=2).");
            }
            configArchive = new FileArchive(configBytes);
        }

        ItemDefinition.init(null);
        NpcDefinition.init(configArchive);

        System.out.println("== Model Audit ==");
        System.out.println("cacheDir=" + cacheDir);
        System.out.println("modelEntries=" + modelEntryCount);
        System.out.println("itemCount=" + ItemDefinition.totalItems + ", npcCount=" + NpcDefinition.TOTAL_NPCS);

        auditItems(idx1Bytes, modelEntryCount);
        auditNpcs(idx1Bytes, modelEntryCount);
    }

    private static void auditItems(byte[] idx1Bytes, int modelEntryCount) {
        int referenced = 0;
        int missingRefs = 0;
        Set<Integer> missingIds = new LinkedHashSet<>();

        List<String> keywordHits = new ArrayList<>();

        for (int itemId = 0; itemId < ItemDefinition.totalItems; itemId++) {
            ItemDefinition def = ItemDefinition.lookup(itemId);
            if (def == null || def.name == null) {
                continue;
            }

            int[] models = collectItemModelIds(def);
            for (int modelId : models) {
                if (modelId < 0) {
                    continue;
                }
                referenced++;
                if (!existsInIdx1(idx1Bytes, modelEntryCount, modelId)) {
                    missingRefs++;
                    missingIds.add(modelId);
                }
            }

            String lower = def.name.toLowerCase(Locale.ROOT);
            if (containsKeyword(lower)) {
                keywordHits.add(formatItemLine(def, models, idx1Bytes, modelEntryCount));
            }
        }

        System.out.println();
        System.out.println("[Items]");
        System.out.println("referencedModelRefs=" + referenced + ", missingRefs=" + missingRefs + ", uniqueMissing=" + missingIds.size());
        if (!missingIds.isEmpty()) {
            System.out.println("missingModelIds(sample up to 40)=" + sample(missingIds, 40));
        }
        System.out.println("keywordHits=" + keywordHits.size());
        for (String line : keywordHits) {
            System.out.println(line);
        }
    }

    private static void auditNpcs(byte[] idx1Bytes, int modelEntryCount) {
        int referenced = 0;
        int missingRefs = 0;
        Set<Integer> missingIds = new LinkedHashSet<>();

        List<String> keywordHits = new ArrayList<>();

        for (int npcId = 0; npcId < NpcDefinition.TOTAL_NPCS; npcId++) {
            NpcDefinition def = NpcDefinition.lookup(npcId);
            if (def == null || def.name == null) {
                continue;
            }

            int[] models = collectNpcModelIds(def);
            for (int modelId : models) {
                if (modelId < 0) {
                    continue;
                }
                referenced++;
                if (!existsInIdx1(idx1Bytes, modelEntryCount, modelId)) {
                    missingRefs++;
                    missingIds.add(modelId);
                }
            }

            String lower = def.name.toLowerCase(Locale.ROOT);
            if (containsKeyword(lower)) {
                keywordHits.add(formatNpcLine(def, models, idx1Bytes, modelEntryCount));
            }
        }

        System.out.println();
        System.out.println("[NPCs]");
        System.out.println("referencedModelRefs=" + referenced + ", missingRefs=" + missingRefs + ", uniqueMissing=" + missingIds.size());
        if (!missingIds.isEmpty()) {
            System.out.println("missingModelIds(sample up to 40)=" + sample(missingIds, 40));
        }
        System.out.println("keywordHits=" + keywordHits.size());
        for (String line : keywordHits) {
            System.out.println(line);
        }
    }

    private static int[] collectItemModelIds(ItemDefinition d) {
        int maleDialogue2 = intField(d, "equipped_model_male_dialogue_2");
        int femaleDialogue2 = intField(d, "equipped_model_female_dialogue_2");
        return new int[]{
                d.inventory_model,
                d.equipped_model_male_1,
                d.equipped_model_male_2,
                d.equipped_model_female_1,
                d.equipped_model_female_2,
                d.equipped_model_male_dialogue_1,
                maleDialogue2,
                d.equipped_model_female_dialogue_1,
                femaleDialogue2
        };
    }

    private static int[] collectNpcModelIds(NpcDefinition d) {
        int[] primary = d.modelId == null ? new int[0] : d.modelId;
        int[] extra = d.additionalModels == null ? new int[0] : d.additionalModels;
        int[] merged = Arrays.copyOf(primary, primary.length + extra.length);
        System.arraycopy(extra, 0, merged, primary.length, extra.length);
        return merged;
    }

    private static String formatItemLine(ItemDefinition d, int[] models, byte[] idx1Bytes, int modelEntryCount) {
        List<String> status = new ArrayList<>();
        for (int m : models) {
            if (m >= 0) {
                status.add(m + ":" + existsInIdx1(idx1Bytes, modelEntryCount, m));
            }
        }
        return "ITEM " + d.id + " \"" + d.name + "\" models=" + status;
    }

    private static String formatNpcLine(NpcDefinition d, int[] models, byte[] idx1Bytes, int modelEntryCount) {
        List<String> status = new ArrayList<>();
        for (int m : models) {
            if (m >= 0) {
                status.add(m + ":" + existsInIdx1(idx1Bytes, modelEntryCount, m));
            }
        }
        return "NPC " + d.id + " \"" + d.name + "\" models=" + status;
    }

    private static boolean containsKeyword(String lowerName) {
        for (String keyword : KEYWORDS) {
            if (lowerName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean existsInIdx1(byte[] idx1Bytes, int entryCount, int modelId) {
        if (modelId < 0 || modelId >= entryCount) {
            return false;
        }
        int o = modelId * 6;
        int size = ((idx1Bytes[o] & 0xff) << 16) | ((idx1Bytes[o + 1] & 0xff) << 8) | (idx1Bytes[o + 2] & 0xff);
        int sector = ((idx1Bytes[o + 3] & 0xff) << 16) | ((idx1Bytes[o + 4] & 0xff) << 8) | (idx1Bytes[o + 5] & 0xff);
        return size > 0 && sector > 0;
    }

    private static String sample(Set<Integer> ids, int max) {
        List<Integer> list = new ArrayList<>(ids);
        list.sort(Integer::compareTo);
        if (list.size() <= max) {
            return list.toString();
        }
        return list.subList(0, max) + "...(+" + (list.size() - max) + ")";
    }

    private static String normalize(String dir) {
        return dir.endsWith("/") || dir.endsWith("\\") ? dir : dir + File.separator;
    }

    private static File resolveDatFile(String cacheDir) {
        File dat = new File(cacheDir + "main_file_cache.dat");
        if (dat.exists()) {
            return dat;
        }
        return new File(cacheDir + "main_file_cache.dat2");
    }

    private static int intField(Object obj, String fieldName) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.getInt(obj);
        } catch (Exception ignored) {
            return -1;
        }
    }
}
