package com.runescape.cache.def;

import com.runescape.cache.FileArchive;
import com.runescape.graphics.sprite.Sprite;
import com.runescape.collection.ReferenceCache;
import com.runescape.draw.Rasterizer2D;
import com.runescape.draw.Rasterizer3D;
import com.runescape.entity.model.Model;
import com.runescape.io.Buffer;
import com.runescape.sign.SignLink;
import com.runescape.util.FileUtils;
import com.runescape.cache.MoonModelLoader;

public final class ItemDefinition {

    public static ReferenceCache sprites = new ReferenceCache(100);
    public static ReferenceCache models = new ReferenceCache(50);
    public static boolean isMembers = true;
    public static int totalItems;
    private static ItemDefinition[] cache;
    private static int cacheIndex;
    private static Buffer item_data;
    private static int[] streamIndices;
    public int value;
    public int[] modified_model_colors;
    public int id;
    public int[] original_model_colors;
    public boolean is_members_only;
    public int noted_item_id;
    public int equipped_model_female_2;
    public int equipped_model_male_1;
    public String[] groundActions;
    public int translate_x;
    public String name;
    public int inventory_model;
    public int equipped_model_male_dialogue_1;
    public boolean stackable;
    public int unnoted_item_id;
    public int modelZoom;
    public int equipped_model_male_2;
    public String[] actions;
    public int rotation_y;
    public int[] stack_variant_id;
    public int translate_yz;//
    public int equipped_model_female_dialogue_1;
    public int rotation_x;
    public int equipped_model_female_1;
    public int[] stack_variant_size;
    public int team;
    public int rotation_z;
    private byte equipped_model_female_translation_y;
    private int equipped_model_female_3;
    private int equipped_model_male_dialogue_2;
    private int model_scale_x;
    private int equipped_model_female_dialogue_2;
    private int light_mag;
    private int equipped_model_male_3;
    private int model_scale_z;
    private int model_scale_y;
    private int light_intensity;
    private byte equipped_model_male_translation_y;

    private ItemDefinition() {
        id = -1;
    }

    public static void clear() {
        models = null;
        sprites = null;
        streamIndices = null;
        cache = null;
        item_data = null;
    }

    public static void init(FileArchive archive) {
        item_data = new Buffer(FileUtils.readFile(SignLink.findcachedir() + "obj.dat"));
        Buffer stream = new Buffer(FileUtils.readFile(SignLink.findcachedir() + "obj.idx"));

        totalItems = stream.readUShort();
        // Extend totalItems to include custom moon equipment IDs if needed
        int originalTotalItems = totalItems;
        if (totalItems < 29013) {
            totalItems = 29013; // Ensure we have space for moon equipment (28991 / 29000+)
        }
        streamIndices = new int[totalItems];
        int offset = 2;

        for (int _ctr = 0; _ctr < originalTotalItems; _ctr++) {
            streamIndices[_ctr] = offset;
            offset += stream.readUShort();
        }
        
        // Initialize remaining indices for moon equipment items
        for (int _ctr = originalTotalItems; _ctr < totalItems; _ctr++) {
            streamIndices[_ctr] = -1; // Mark as no cache data available
        }

        cache = new ItemDefinition[10];

        for (int _ctr = 0; _ctr < 10; _ctr++) {
            cache[_ctr] = new ItemDefinition();
        }

        System.out.println("Loaded: " + totalItems + " items");
    }

    public static ItemDefinition lookup(int itemId) {
        for (int count = 0; count < 10; count++)
            if (cache[count].id == itemId)
                return cache[count];

        cacheIndex = (cacheIndex + 1) % 10;
        ItemDefinition itemDef = cache[cacheIndex];
        if (itemId > 0 && itemId < streamIndices.length) {
            item_data.currentPosition = streamIndices[itemId];
        } else {
            item_data.currentPosition = -1; // Skip cache read for items beyond cache size
            System.out.println("Item ID " + itemId + " is beyond cache size (" + streamIndices.length + "), using defaults");
        }
        itemDef.id = itemId;
        itemDef.setDefaults();
        if (item_data.currentPosition != -1) {
            itemDef.readValues(item_data);
        }

        if (itemDef.noted_item_id != -1)
            itemDef.toNote();

        if (itemId == 4724 || itemId == 4726 || itemId == 4728 || itemId == 4730 || itemId == 4753 || itemId == 4755 || itemId == 4757 || itemId == 4759 || itemId == 4745 || itemId == 4747 || itemId == 4749 || itemId == 4751 ||
                itemId == 4708 || itemId == 4710 || itemId == 4712 || itemId == 4714 || itemId == 4732 || itemId == 4734 || itemId == 4736 || itemId == 4738 || itemId == 4716 || itemId == 4718 || itemId == 4720 || itemId == 4722) {
            itemDef.actions[2] = "Set";
        }

        //System.out.println("Item: "+itemDef.name+", equip models: "+itemDef.equipped_model_male_1+", "+itemDef.equipped_model_male_2+", "+itemDef.equipped_model_male_3);

        /**
         * Place customs here
         */
        switch (itemId) {
            case 13302:
                itemDef.name = "Crate key";
                itemDef.original_model_colors = new int[]{8128};
                itemDef.modified_model_colors = new int[]{5231};
                break;
            case 5022:
                itemDef.name = "Spin ticket";
                itemDef.original_model_colors = new int[]{100};
                itemDef.modified_model_colors = new int[]{10562};
                //[10458,0,0],"newModelColor":[10562
                break;
            case 5509:
            case 5510:
            case 5512:
            case 5514:
                itemDef.actions = new String[5];
                itemDef.actions[0] = "Fill";
                itemDef.actions[1] = null;
                itemDef.actions[2] = "Empty";
                itemDef.actions[3] = "Check";
                break;
            case 12881:
            case 12875:
            case 12873:
            case 12883:
            case 12879:
            case 12877:
                itemDef.actions = new String[5];
                itemDef.actions[0] = "Open";
                break;

            case 8013:
                itemDef.name = "Home teleport";
                break;
            case 2542:
                itemDef.copy(lookup(1505));
                itemDef.actions = new String[5];
                itemDef.actions[0] = "Read";
                itemDef.name = "Preserve scroll";
                itemDef.stackable = false;
                break;
            case 2543:
                itemDef.copy(lookup(1505));
                itemDef.actions = new String[5];
                itemDef.actions[0] = "Read";
                itemDef.name = "Rigour scroll";
                itemDef.stackable = false;
                break;
            case 2544:
                itemDef.copy(lookup(1505));
                itemDef.actions = new String[5];
                itemDef.actions[0] = "Read";
                itemDef.name = "Augury scroll";
                itemDef.stackable = false;
                break;
            case 2545:
                itemDef.copy(lookup(12846));
                itemDef.actions = new String[5];
                itemDef.actions[0] = "Read";
                itemDef.name = "Target-teleport scroll";
                itemDef.stackable = false;
                break;
            case 12006:
                itemDef.actions = new String[5];
                itemDef.actions[1] = "Wield";
                break;
            case 12926:
                itemDef.actions = new String[5];
                itemDef.actions[1] = "Wield";
                itemDef.actions[2] = "Check";
                break;
            case 24423:
                // Harmonised nightmare staff: fallback inventory model while 39070 is missing in this cache.
                itemDef.inventory_model = 39073;
                break;

            case 29010: // Eclipse moon helm
                itemDef.copy(lookup(1149)); // Helmet base behavior
                itemDef.name = "Eclipse moon helm";
                itemDef.actions = new String[5];
                itemDef.actions[1] = "Wear";
                itemDef.inventory_model = MoonModelLoader.getMoonModel(itemId, 52255);
                itemDef.equipped_model_male_1 = 50873;
                itemDef.equipped_model_female_1 = 51075;
                itemDef.equipped_model_male_dialogue_1 = 52727;
                itemDef.equipped_model_female_dialogue_1 = 52727;
                break;
            case 29004: // Eclipse moon chestplate
                itemDef.copy(lookup(1127)); // Platebody base behavior
                itemDef.name = "Eclipse moon chestplate";
                itemDef.actions = new String[5];
                itemDef.actions[1] = "Wear";
                itemDef.inventory_model = MoonModelLoader.getMoonModel(itemId, 52238);
                itemDef.equipped_model_male_1 = 50901;
                itemDef.equipped_model_female_1 = 51102;
                break;
            case 29007: // Eclipse moon tassets
                itemDef.copy(lookup(1079)); // Platelegs base behavior
                itemDef.name = "Eclipse moon tassets";
                itemDef.actions = new String[5];
                itemDef.actions[1] = "Wear";
                itemDef.inventory_model = MoonModelLoader.getMoonModel(itemId, 52245);
                itemDef.equipped_model_male_1 = 50851;
                itemDef.equipped_model_female_1 = 51057;
                break;
            case 29000: // Eclipse atlatl
                itemDef.copy(lookup(859)); // Ranged weapon base behavior
                itemDef.name = "Eclipse atlatl";
                itemDef.actions = new String[5];
                itemDef.actions[1] = "Wield";
                itemDef.inventory_model = MoonModelLoader.getMoonModel(itemId, 52263);
                itemDef.equipped_model_male_1 = 51175;
                itemDef.equipped_model_female_1 = 51138;
                break;
            case 28991: // Atlatl dart
                itemDef.copy(lookup(806)); // Dart base behavior
                itemDef.name = "Atlatl dart";
                itemDef.actions = new String[5];
                itemDef.actions[1] = "Wield";
                itemDef.stackable = true;
                itemDef.inventory_model = MoonModelLoader.getMoonModel(itemId, 52270);
                break;
            case 27690: // Voidwaker
                itemDef.copy(lookup(1305)); // Dragon longsword base behavior
                itemDef.name = "Voidwaker";
                itemDef.actions = new String[5];
                itemDef.actions[1] = "Wield";
                itemDef.inventory_model = 47422;
                itemDef.equipped_model_male_1 = 47212;
                itemDef.equipped_model_female_1 = 47217;
                itemDef.equipped_model_male_2 = -1;
                itemDef.equipped_model_female_2 = -1;
                itemDef.equipped_model_male_3 = -1;
                itemDef.equipped_model_female_3 = -1;
                itemDef.modelZoom = 1723;
                itemDef.rotation_x = 539;
                itemDef.rotation_y = 678;
                itemDef.rotation_z = 0;
                itemDef.translate_x = 1;
                itemDef.translate_yz = -1;
                itemDef.original_model_colors = null;
                itemDef.modified_model_colors = null;
                break;
            case 27691: // Voidwaker (noted)
                itemDef.copy(lookup(1306)); // Dragon longsword note base behavior
                itemDef.name = "Voidwaker";
                break;
            case 29801: // Amulet of rancour
                itemDef.copy(lookup(19553)); // Amulet of torture base behavior
                itemDef.name = "Amulet of rancour";
                itemDef.actions = new String[5];
                itemDef.actions[1] = "Wear";
                // Use only explicit rancour models to avoid inheriting unrelated equip parts.
                itemDef.inventory_model = 54296;
                itemDef.equipped_model_male_1 = 54326;
                itemDef.equipped_model_female_1 = 54322;
                itemDef.equipped_model_male_2 = -1;
                itemDef.equipped_model_female_2 = -1;
                itemDef.equipped_model_male_3 = -1;
                itemDef.equipped_model_female_3 = -1;
                itemDef.equipped_model_male_dialogue_1 = -1;
                itemDef.equipped_model_male_dialogue_2 = -1;
                itemDef.equipped_model_female_dialogue_1 = -1;
                itemDef.equipped_model_female_dialogue_2 = -1;
                // 2446 rancour face colours decode too dark/invisible in this renderer.
                itemDef.original_model_colors = new int[]{-10366, -11122, -11126, 26444, 26448, 26419, 29084};
                itemDef.modified_model_colors = new int[]{33, 24, 16, 103, 96, 90, 61};
                // Use known-good amulet icon transforms for this client's software rasterizer.
                itemDef.modelZoom = 620;
                itemDef.rotation_x = 68;
                itemDef.rotation_y = 424;
                itemDef.rotation_z = 0;
                itemDef.translate_x = 1;
                itemDef.translate_yz = 16;
                break;
            case 29802: // Amulet of rancour (noted)
                itemDef.copy(lookup(19554)); // Amulet of torture note base behavior
                itemDef.name = "Amulet of rancour";
                break;
            case 29803: // Amulet of rancour (placeholder)
                itemDef.copy(lookup(19555)); // Placeholder-style base behavior
                itemDef.name = "Amulet of rancour";
                break;
            case 29796: // Noxious halberd
                itemDef.copy(lookup(3204)); // Dragon halberd base behavior
                itemDef.name = "Noxious halberd";
                itemDef.actions = new String[5];
                itemDef.actions[1] = "Wield";
                itemDef.inventory_model = 54299;
                itemDef.equipped_model_male_1 = 54316;
                itemDef.equipped_model_female_1 = 54315;
                itemDef.modelZoom = 1840;
                itemDef.rotation_x = 524;
                itemDef.rotation_y = 120;
                itemDef.rotation_z = 0;
                itemDef.translate_x = -4;
                itemDef.translate_yz = 5;
                itemDef.original_model_colors = new int[] {61};
                itemDef.modified_model_colors = new int[] {0};
                break;
            case 29797: // Noxious halberd (noted)
                itemDef.copy(lookup(3205)); // Dragon halberd noted base behavior
                itemDef.name = "Noxious halberd";
                break;
            case 29798: // Noxious halberd (placeholder)
                itemDef.copy(lookup(19555)); // Placeholder-style base behavior
                itemDef.name = "Noxious halberd";
                break;
            case 29799: // Araxyte fang
                itemDef.copy(lookup(4155)); // Gem-like material base behavior
                itemDef.name = "Araxyte fang";
                itemDef.actions = new String[5];
                itemDef.actions[2] = "Take";
                itemDef.inventory_model = 54304;
                itemDef.modelZoom = 800;
                itemDef.rotation_x = 545;
                itemDef.rotation_y = 12;
                itemDef.rotation_z = 0;
                itemDef.translate_x = 0;
                itemDef.translate_yz = 5;
                break;
            case 29800: // Araxyte fang (placeholder)
                itemDef.copy(lookup(19555)); // Placeholder-style base behavior
                itemDef.name = "Araxyte fang";
                break;
            case 29790: // Noxious point
                itemDef.copy(lookup(4155)); // Material base behavior
                itemDef.name = "Noxious point";
                itemDef.actions = new String[5];
                itemDef.actions[2] = "Take";
                itemDef.inventory_model = 54300;
                itemDef.modelZoom = 1349;
                itemDef.rotation_x = 576;
                itemDef.rotation_y = 2015;
                itemDef.rotation_z = 0;
                itemDef.translate_x = 3;
                itemDef.translate_yz = 2;
                break;
            case 29791: // Noxious point (placeholder)
                itemDef.copy(lookup(19555)); // Placeholder-style base behavior
                itemDef.name = "Noxious point";
                break;
            case 29792: // Noxious blade
                itemDef.copy(lookup(4155)); // Material base behavior
                itemDef.name = "Noxious blade";
                itemDef.actions = new String[5];
                itemDef.actions[2] = "Take";
                itemDef.inventory_model = 54301;
                itemDef.modelZoom = 1360;
                itemDef.rotation_x = 465;
                itemDef.rotation_y = 114;
                itemDef.rotation_z = 0;
                itemDef.translate_x = 0;
                itemDef.translate_yz = 6;
                break;
            case 29793: // Noxious blade (placeholder)
                itemDef.copy(lookup(19555)); // Placeholder-style base behavior
                itemDef.name = "Noxious blade";
                break;
            case 29794: // Noxious pommel
                itemDef.copy(lookup(4155)); // Material base behavior
                itemDef.name = "Noxious pommel";
                itemDef.actions = new String[5];
                itemDef.actions[2] = "Take";
                itemDef.inventory_model = 54298;
                itemDef.modelZoom = 848;
                itemDef.rotation_x = 501;
                itemDef.rotation_y = 1958;
                itemDef.rotation_z = 0;
                itemDef.translate_x = 2;
                itemDef.translate_yz = 8;
                break;
            case 29795: // Noxious pommel (placeholder)
                itemDef.copy(lookup(19555)); // Placeholder-style base behavior
                itemDef.name = "Noxious pommel";
                break;
        }
        return itemDef;
    }

    public static Sprite getSprite(int itemId, int stackSize, int outlineColor) {
        if (outlineColor == 0) {
            Sprite sprite = (Sprite) sprites.get(itemId);
            if (sprite != null && sprite.maxHeight != stackSize && sprite.maxHeight != -1) {

                sprite.unlink();
                sprite = null;
            }
            if (sprite != null)
                return sprite;
        }
        ItemDefinition itemDef = lookup(itemId);
        if (itemDef.stack_variant_id == null)
            stackSize = -1;
        if (stackSize > 1) {
            int stack_item_id = -1;
            for (int j1 = 0; j1 < 10; j1++)
                if (stackSize >= itemDef.stack_variant_size[j1] && itemDef.stack_variant_size[j1] != 0)
                    stack_item_id = itemDef.stack_variant_id[j1];

            if (stack_item_id != -1)
                itemDef = lookup(stack_item_id);
        }
        Model model = itemDef.getModel(1);
        if (model == null)
            return null;
        Sprite sprite = null;
        if (itemDef.noted_item_id != -1) {
            sprite = getSprite(itemDef.unnoted_item_id, 10, -1);
            if (sprite == null)
                return null;
        }
        Sprite enabledSprite = new Sprite(32, 32);
        int centerX = Rasterizer3D.originViewX;
        int centerY = Rasterizer3D.originViewY;
        int[] lineOffsets = Rasterizer3D.scanOffsets;
        int[] pixels = Rasterizer2D.pixels;
        int width = Rasterizer2D.width;
        int height = Rasterizer2D.height;
        int vp_left = Rasterizer2D.leftX;
        int vp_right = Rasterizer2D.bottomX;
        int vp_top = Rasterizer2D.topY;
        int vp_bottom = Rasterizer2D.bottomY;
        Rasterizer3D.aBoolean1464 = false;
        Rasterizer2D.initDrawingArea(32, 32, enabledSprite.myPixels);
        Rasterizer2D.drawBox(0, 0, 32, 32, 0);
        Rasterizer3D.useViewport();
        int k3 = itemDef.modelZoom;
        if (outlineColor == -1)
            k3 = (int) ((double) k3 * 1.5D);
        if (outlineColor > 0)
            k3 = (int) ((double) k3 * 1.04D);
        int l3 = Rasterizer3D.anIntArray1470[itemDef.rotation_y] * k3 >> 16;
        int i4 = Rasterizer3D.COSINE[itemDef.rotation_y] * k3 >> 16;
        model.method482(itemDef.rotation_x, itemDef.rotation_z, itemDef.rotation_y, itemDef.translate_x,
                l3 + model.modelBaseY / 2 + itemDef.translate_yz, i4 + itemDef.translate_yz);

        enabledSprite.outline(1);
        if (outlineColor > 0) {
            enabledSprite.outline(16777215);
        }
        if (outlineColor == 0) {
            enabledSprite.shadow(3153952);
        }

        Rasterizer2D.initDrawingArea(32, 32, enabledSprite.myPixels);

        if (itemDef.noted_item_id != -1) {
            int old_w = sprite.maxWidth;
            int old_h = sprite.maxHeight;
            sprite.maxWidth = 32;
            sprite.maxHeight = 32;
            sprite.drawSprite(0, 0);
            sprite.maxWidth = old_w;
            sprite.maxHeight = old_h;
        }
        if (outlineColor == 0)
            sprites.put(enabledSprite, itemId);
        Rasterizer2D.initDrawingArea(height, width, pixels);
        Rasterizer2D.setDrawingArea(vp_bottom, vp_left, vp_right, vp_top);
        Rasterizer3D.originViewX = centerX;
        Rasterizer3D.originViewY = centerY;
        Rasterizer3D.scanOffsets = lineOffsets;
        Rasterizer3D.aBoolean1464 = true;
        if (itemDef.stackable)
            enabledSprite.maxWidth = 33;
        else
            enabledSprite.maxWidth = 32;
        enabledSprite.maxHeight = stackSize;
        return enabledSprite;
    }

    public static Sprite getSprite(int itemId, int stackSize, int zoom, int outlineColor) {
        ItemDefinition itemDef = lookup(itemId);
        if (itemDef.stack_variant_id == null)
            stackSize = -1;
        if (stackSize > 1) {
            int stack_item_id = -1;
            for (int j1 = 0; j1 < 10; j1++)
                if (stackSize >= itemDef.stack_variant_size[j1] && itemDef.stack_variant_size[j1] != 0)
                    stack_item_id = itemDef.stack_variant_id[j1];

            if (stack_item_id != -1)
                itemDef = lookup(stack_item_id);
        }
        Model model = itemDef.getModel(1);
        if (model == null)
            return null;
        Sprite sprite = new Sprite(90, 90);
        int centerX = Rasterizer3D.originViewX;
        int centerY = Rasterizer3D.originViewY;
        int[] lineOffsets = Rasterizer3D.scanOffsets;
        int[] pixels = Rasterizer2D.pixels;
        int width = Rasterizer2D.width;
        int height = Rasterizer2D.height;
        int vp_left = Rasterizer2D.leftX;
        int vp_right = Rasterizer2D.bottomX;
        int vp_top = Rasterizer2D.topY;
        int vp_bottom = Rasterizer2D.bottomY;
        Rasterizer3D.aBoolean1464 = false;
        Rasterizer2D.initDrawingArea(90, 90, sprite.myPixels);
        Rasterizer2D.drawBox(0, 0, 90, 90, 0);
        Rasterizer3D.useViewport();
        int l3 = Rasterizer3D.anIntArray1470[itemDef.rotation_y] * zoom >> 15;
        int i4 = Rasterizer3D.COSINE[itemDef.rotation_y] * zoom >> 15;
        model.method482(itemDef.rotation_x, itemDef.rotation_z, itemDef.rotation_y, itemDef.translate_x,
                l3 + model.modelBaseY / 2 + itemDef.translate_yz, i4 + itemDef.translate_yz);
        sprite.outline(1);
        if (outlineColor > 0) {
            sprite.outline(16777215);
        }
        if (outlineColor == 0) {
            sprite.shadow(3153952);
        }
        Rasterizer2D.initDrawingArea(90, 90, sprite.myPixels);
        Rasterizer2D.initDrawingArea(height, width, pixels);
        Rasterizer2D.setDrawingArea(vp_bottom, vp_left, vp_right, vp_top);
        Rasterizer3D.originViewX = centerX;
        Rasterizer3D.originViewY = centerY;
        Rasterizer3D.scanOffsets = lineOffsets;
        Rasterizer3D.aBoolean1464 = true;
        if (itemDef.stackable)
            sprite.maxWidth = 33;
        else
            sprite.maxWidth = 32;
        sprite.maxHeight = stackSize;
        return sprite;
    }
    
    public boolean isDialogueModelCached(int gender) {
        int model_1 = equipped_model_male_dialogue_1;
        int model_2 = equipped_model_male_dialogue_2;
        if (gender == 1) {
            model_1 = equipped_model_female_dialogue_1;
            model_2 = equipped_model_female_dialogue_2;
        }
        if (model_1 == -1)
            return true;
        boolean cached = Model.isCached(model_1);
        if (model_2 != -1 && !Model.isCached(model_2))
            cached = false;
        return cached;
    }

    public Model getChatEquipModel(int gender) {
        int dialogueModel = equipped_model_male_dialogue_1;
        int dialogueHatModel = equipped_model_male_dialogue_2;
        if (gender == 1) {
            dialogueModel = equipped_model_female_dialogue_1;
            dialogueHatModel = equipped_model_female_dialogue_2;
        }
        if (dialogueModel == -1)
            return null;
        Model dialogueModel_ = Model.getModel(dialogueModel);
        if (dialogueHatModel != -1) {
            Model hatModel_ = Model.getModel(dialogueHatModel);
            Model[] models = {dialogueModel_, hatModel_};
            dialogueModel_ = new Model(2, models);
        }
        if (modified_model_colors != null) {
            for (int i1 = 0; i1 < modified_model_colors.length; i1++)
                dialogueModel_.recolor(modified_model_colors[i1], original_model_colors[i1]);

        }
        return dialogueModel_;
    }

    public boolean isEquippedModelCached(int gender) {
        int primaryModel = equipped_model_male_1;
        int secondaryModel = equipped_model_male_2;
        int emblem = equipped_model_male_3;
        if (gender == 1) {
            primaryModel = equipped_model_female_1;
            secondaryModel = equipped_model_female_2;
            emblem = equipped_model_female_3;
        }
        if (primaryModel == -1)
            return true;
        boolean cached = Model.isCached(primaryModel);
        if (secondaryModel != -1 && !Model.isCached(secondaryModel))
            cached = false;
        if (emblem != -1 && !Model.isCached(emblem))
            cached = false;
        return cached;
    }

    public Model getEquippedModel(int gender) {
        int primaryModel = equipped_model_male_1;
        int secondaryModel = equipped_model_male_2;
        int emblem = equipped_model_male_3;

        if (gender == 1) {
            primaryModel = equipped_model_female_1;
            secondaryModel = equipped_model_female_2;
            emblem = equipped_model_female_3;
        }

        if (primaryModel == -1)
            return null;
        Model primaryModel_ = Model.getModel(primaryModel);
        if (secondaryModel != -1)
            if (emblem != -1) {
                Model secondaryModel_ = Model.getModel(secondaryModel);
                Model emblemModel = Model.getModel(emblem);
                Model[] models = {primaryModel_, secondaryModel_, emblemModel};
                primaryModel_ = new Model(3, models);
            } else {
                Model model_2 = Model.getModel(secondaryModel);
                Model[] models = {primaryModel_, model_2};
                primaryModel_ = new Model(2, models);
            }
        if (gender == 0 && equipped_model_male_translation_y != 0)
            primaryModel_.translate(0, equipped_model_male_translation_y, 0);
        if (gender == 1 && equipped_model_female_translation_y != 0)
            primaryModel_.translate(0, equipped_model_female_translation_y, 0);

        if (modified_model_colors != null) {
            for (int i1 = 0; i1 < modified_model_colors.length; i1++)
                primaryModel_.recolor(modified_model_colors[i1], original_model_colors[i1]);

        }
        return primaryModel_;
    }

    private void setDefaults() {
        inventory_model = 0;
        name = null;
        modified_model_colors = null;
        original_model_colors = null;
        modelZoom = 2000;
        rotation_y = 0;
        rotation_x = 0;
        rotation_z = 0;
        translate_x = 0;
        translate_yz = 0;
        stackable = false;
        value = 1;
        is_members_only = false;
        groundActions = null;
        actions = null;
        equipped_model_male_1 = -1;
        equipped_model_male_2 = -1;
        equipped_model_male_translation_y = 0;
        equipped_model_female_1 = -1;
        equipped_model_female_2 = -1;
        equipped_model_female_translation_y = 0;
        equipped_model_male_3 = -1;
        equipped_model_female_3 = -1;
        equipped_model_male_dialogue_1 = -1;
        equipped_model_male_dialogue_2 = -1;
        equipped_model_female_dialogue_1 = -1;
        equipped_model_female_dialogue_2 = -1;
        stack_variant_id = null;
        stack_variant_size = null;
        unnoted_item_id = -1;
        noted_item_id = -1;
        model_scale_x = 128;
        model_scale_y = 128;
        model_scale_z = 128;
        light_intensity = 0;
        light_mag = 0;
        team = 0;
    }

    private void copy(ItemDefinition copy) {
        rotation_x = copy.rotation_x;
        rotation_y = copy.rotation_y;
        rotation_z = copy.rotation_z;
        model_scale_x = copy.model_scale_x;
        model_scale_y = copy.model_scale_y;
        model_scale_z = copy.model_scale_z;
        modelZoom = copy.modelZoom;
        translate_x = copy.translate_x;
        translate_yz = copy.translate_yz;
        inventory_model = copy.inventory_model;
        stackable = copy.stackable;
        modified_model_colors = copy.modified_model_colors;
        original_model_colors = copy.original_model_colors;
    }

    private void toNote() {
        ItemDefinition itemDef = lookup(noted_item_id);
        inventory_model = itemDef.inventory_model;
        modelZoom = itemDef.modelZoom;
        rotation_y = itemDef.rotation_y;
        rotation_x = itemDef.rotation_x;

        rotation_z = itemDef.rotation_z;
        translate_x = itemDef.translate_x;
        translate_yz = itemDef.translate_yz;
        modified_model_colors = itemDef.modified_model_colors;
        original_model_colors = itemDef.original_model_colors;
        ItemDefinition itemDef_1 = lookup(unnoted_item_id);
        name = itemDef_1.name;
        is_members_only = itemDef_1.is_members_only;
        value = itemDef_1.value;
        stackable = true;
    }

    public Model getModel(int stack_size) {
        if (stack_variant_id != null && stack_size > 1) {
            int stack_item_id = -1;
            for (int k = 0; k < 10; k++)
                if (stack_size >= stack_variant_size[k] && stack_variant_size[k] != 0)
                    stack_item_id = stack_variant_id[k];

            if (stack_item_id != -1)
                return lookup(stack_item_id).getModel(1);
        }
        Model model = (Model) models.get(id);
        if (model != null)
            return model;
        model = Model.getModel(inventory_model);
        if (model == null)
            return null;
        if (model_scale_x != 128 || model_scale_y != 128 || model_scale_z != 128)
            model.scale(model_scale_x, model_scale_z, model_scale_y);
        if (modified_model_colors != null) {
            for (int l = 0; l < modified_model_colors.length; l++)
                model.recolor(modified_model_colors[l], original_model_colors[l]);

        }
        int lightInt = 64 + light_intensity;
        int lightMag = 768 + light_mag;
        model.light(lightInt, lightMag, -50, -10, -50, true);
        model.fits_on_single_square = true;
        models.put(model, id);
        return model;
    }

    public Model getUnshadedModel(int stack_size) {
        if (stack_variant_id != null && stack_size > 1) {
            int stack_item_id = -1;
            for (int count = 0; count < 10; count++)
                if (stack_size >= stack_variant_size[count] && stack_variant_size[count] != 0)
                    stack_item_id = stack_variant_id[count];

            if (stack_item_id != -1)
                return lookup(stack_item_id).getUnshadedModel(1);
        }
        Model model = Model.getModel(inventory_model);
        if (model == null)
            return null;
        if (modified_model_colors != null) {
            for (int colorPtr = 0; colorPtr < modified_model_colors.length; colorPtr++)
                model.recolor(modified_model_colors[colorPtr], original_model_colors[colorPtr]);

        }
        return model;
    }

    public void readValues(Buffer buffer) {
        do {
            int opCode = buffer.readUnsignedByte();
            if (opCode == 0)
                return;
            if (opCode == 1)
                inventory_model = buffer.readUShort();
            else if (opCode == 2)
                name = buffer.readString();
            else if (opCode == 3)
                /*description = */buffer.readString();
            else if (opCode == 4)
                modelZoom = buffer.readUShort();
            else if (opCode == 5)
                rotation_y = buffer.readUShort();
            else if (opCode == 6)
                rotation_x = buffer.readUShort();
            else if (opCode == 7) {
                translate_x = buffer.readUShort();
                if (translate_x > 32767)
                    translate_x -= 0x10000;
            } else if (opCode == 8) {
                translate_yz = buffer.readUShort();
                if (translate_yz > 32767)
                    translate_yz -= 0x10000;
            } else if (opCode == 10)
                buffer.readUShort();
            else if (opCode == 11)
                stackable = true;
            else if (opCode == 12) {
                value = buffer.readInt();
            } else if (opCode == 16)
                is_members_only = true;
            else if (opCode == 23) {
                equipped_model_male_1 = buffer.readUShort();
                equipped_model_male_translation_y = buffer.readSignedByte();
            } else if (opCode == 24)
                equipped_model_male_2 = buffer.readUShort();
            else if (opCode == 25) {
                equipped_model_female_1 = buffer.readUShort();
                equipped_model_female_translation_y = buffer.readSignedByte();
            } else if (opCode == 26)
                equipped_model_female_2 = buffer.readUShort();
            else if (opCode >= 30 && opCode < 35) {
                if (groundActions == null)
                    groundActions = new String[5];
                groundActions[opCode - 30] = buffer.readString();
                if (groundActions[opCode - 30].equalsIgnoreCase("hidden"))
                    groundActions[opCode - 30] = null;
            } else if (opCode >= 35 && opCode < 40) {
                if (actions == null)
                    actions = new String[5];
                actions[opCode - 35] = buffer.readString();
            } else if (opCode == 40) {
                int j = buffer.readUnsignedByte();
                modified_model_colors = new int[j];
                original_model_colors = new int[j];
                for (int k = 0; k < j; k++) {
                    original_model_colors[k] = buffer.readUShort();
                    modified_model_colors[k] = buffer.readUShort();
                }
            } else if (opCode == 78)
                equipped_model_male_3 = buffer.readUShort();
            else if (opCode == 79)
                equipped_model_female_3 = buffer.readUShort();
            else if (opCode == 90)
                equipped_model_male_dialogue_1 = buffer.readUShort();
            else if (opCode == 91)
                equipped_model_female_dialogue_1 = buffer.readUShort();
            else if (opCode == 92)
                equipped_model_male_dialogue_2 = buffer.readUShort();
            else if (opCode == 93)
                equipped_model_female_dialogue_2 = buffer.readUShort();
            else if (opCode == 95)
                rotation_z = buffer.readUShort();
            else if (opCode == 97)
                unnoted_item_id = buffer.readUShort();
            else if (opCode == 98)
                noted_item_id = buffer.readUShort();
            else if (opCode >= 100 && opCode < 110) {

                if (stack_variant_id == null) {
                    stack_variant_id = new int[10];
                    stack_variant_size = new int[10];
                }
                stack_variant_id[opCode - 100] = buffer.readUShort();
                stack_variant_size[opCode - 100] = buffer.readUShort();
            } else if (opCode == 110)
                model_scale_x = buffer.readUShort();
            else if (opCode == 111)
                model_scale_y = buffer.readUShort();
            else if (opCode == 112)
                model_scale_z = buffer.readUShort();
            else if (opCode == 113)
                light_intensity = buffer.readSignedByte();
            else if (opCode == 114)
                light_mag = buffer.readSignedByte() * 5;
            else if (opCode == 115)
                team = buffer.readUnsignedByte();
        } while (true);
    }
}
