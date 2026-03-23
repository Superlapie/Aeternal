package com.elvarg.game.content.combat.magic;

import com.elvarg.game.World;
import com.elvarg.game.GameConstants;
import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.content.skill.skillable.impl.Prayer.BuriableBone;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.GraphicHeight;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.MagicSpellbook;
import com.elvarg.game.model.Skill;
import com.elvarg.game.model.teleportation.TeleportHandler;
import com.elvarg.game.model.teleportation.TeleportType;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;
import com.elvarg.util.ItemIdentifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ArceuusSpells {

    private ArceuusSpells() {}

    // Spell IDs here represent the clicked button id. If your cache differs, update these constants.
    public static final int ARCEUUS_LIBRARY_TELEPORT = 30517;
    public static final int DRAYNOR_MANOR_TELEPORT = 30521;
    public static final int BATTLEFRONT_TELEPORT = 30525;
    public static final int MIND_ALTAR_TELEPORT = 30529;
    public static final int SALVE_GRAVEYARD_TELEPORT = 30533;
    public static final int FENKENSTRAINS_CASTLE_TELEPORT = 30537;
    public static final int WEST_ARDOUGNE_TELEPORT = 30541;
    public static final int HARMONY_ISLAND_TELEPORT = 30545;
    public static final int CEMETERY_TELEPORT = 30549;
    public static final int BARROWS_TELEPORT = 30553;
    public static final int APE_ATOLL_TELEPORT = 30557;
    public static final int RESPAWN_TELEPORT = 30531;

    public static final int BASIC_REANIMATION = 30601;
    public static final int ADEPT_REANIMATION = 30605;
    public static final int EXPERT_REANIMATION = 30609;
    public static final int MASTER_REANIMATION = 30613;
    public static final int RESURRECT_CROPS = 30617;

    public static final int RESURRECT_LESSER_GHOST = 30621;
    public static final int RESURRECT_LESSER_SKELETON = 30625;
    public static final int RESURRECT_LESSER_ZOMBIE = 30629;
    public static final int RESURRECT_SUPERIOR_GHOST = 30663;
    public static final int RESURRECT_SUPERIOR_SKELETON = 30667;
    public static final int RESURRECT_SUPERIOR_ZOMBIE = 30671;
    public static final int RESURRECT_GREATER_GHOST = 30675;
    public static final int RESURRECT_GREATER_SKELETON = 30679;
    public static final int RESURRECT_GREATER_ZOMBIE = 30683;
    public static final int MARK_OF_DARKNESS = 30685;
    public static final int WARD_OF_ARCEUUS = 30689;
    public static final int DEMONIC_OFFERING = 30693;
    public static final int SINISTER_OFFERING = 30697;
    public static final int DEGRIME = 30701;
    public static final int SHADOW_VEIL = 30705;
    public static final int VILE_VIGOUR = 30709;
    public static final int DARK_LURE = 30713;
    public static final int DEATH_CHARGE = 30717;

    private record CastVisual(int animationId, int graphicId, GraphicHeight height) {}

    private static final CastVisual REANIMATION_VISUAL = new CastVisual(7198, 1288, GraphicHeight.HIGH);
    private static final CastVisual RESURRECT_LESSER_GHOST_VISUAL = new CastVisual(8970, 1873, GraphicHeight.HIGH);
    private static final CastVisual RESURRECT_LESSER_SKELETON_VISUAL = new CastVisual(8970, 1874, GraphicHeight.HIGH);
    private static final CastVisual RESURRECT_LESSER_ZOMBIE_VISUAL = new CastVisual(8970, 1875, GraphicHeight.HIGH);
    private static final CastVisual RESURRECT_SUPERIOR_GHOST_VISUAL = new CastVisual(8970, 1873, GraphicHeight.HIGH);
    private static final CastVisual RESURRECT_SUPERIOR_SKELETON_VISUAL = new CastVisual(8970, 1874, GraphicHeight.HIGH);
    private static final CastVisual RESURRECT_SUPERIOR_ZOMBIE_VISUAL = new CastVisual(8970, 1875, GraphicHeight.HIGH);
    private static final CastVisual RESURRECT_GREATER_GHOST_VISUAL = new CastVisual(8970, 1873, GraphicHeight.HIGH);
    private static final CastVisual RESURRECT_GREATER_SKELETON_VISUAL = new CastVisual(8970, 1874, GraphicHeight.HIGH);
    private static final CastVisual RESURRECT_GREATER_ZOMBIE_VISUAL = new CastVisual(8970, 1875, GraphicHeight.HIGH);
    private static final CastVisual RESURRECT_CROPS_VISUAL = new CastVisual(7118, 1297, GraphicHeight.HIGH);
    private static final CastVisual MARK_OF_DARKNESS_VISUAL = new CastVisual(8985, 1852, GraphicHeight.HIGH);
    private static final CastVisual WARD_OF_ARCEUUS_VISUAL = new CastVisual(8973, 1851, GraphicHeight.HIGH);
    private static final CastVisual DEMONIC_OFFERING_VISUAL = new CastVisual(8973, 1858, GraphicHeight.HIGH);
    private static final CastVisual SINISTER_OFFERING_VISUAL = new CastVisual(8973, 1858, GraphicHeight.HIGH);
    private static final CastVisual DEGRIME_VISUAL = new CastVisual(8973, 1858, GraphicHeight.HIGH);
    private static final CastVisual SHADOW_VEIL_VISUAL = new CastVisual(8973, 1881, GraphicHeight.HIGH);
    private static final CastVisual VILE_VIGOUR_VISUAL = new CastVisual(8978, 1876, GraphicHeight.HIGH);
    private static final CastVisual DARK_LURE_VISUAL = new CastVisual(8973, 1858, GraphicHeight.HIGH);
    private static final CastVisual DEATH_CHARGE_VISUAL = new CastVisual(8983, 1854, GraphicHeight.HIGH);

    private static final int THRALL_GHOST_NPC = 10884;
    private static final int THRALL_SKELETON_NPC = 10885;
    private static final int THRALL_ZOMBIE_NPC = 10886;
    private static final int THRALL_PRAYER_COST = 6;
    private static final int THRALL_COOLDOWN_TICKS = 16;
    private static final int THRALL_LIFETIME_TICKS = 100; // 60 seconds
    private static final long THRALL_COOLDOWN_MS = (long) THRALL_COOLDOWN_TICKS * GameConstants.GAME_ENGINE_PROCESSING_CYCLE_RATE;
    private static final long THRALL_LIFETIME_MS = (long) THRALL_LIFETIME_TICKS * GameConstants.GAME_ENGINE_PROCESSING_CYCLE_RATE;
    private static final long OFFERING_COOLDOWN_MS = 5_400L;
    private static final int OFFERING_MAX_ITEMS_PER_CAST = 3;
    private static final int SUPERIOR_DRAGON_BONE_MIN_PRAYER = 70;
    private static final String THRALL_MAX_HIT_ATTR = "thrall_max_hit";
    private static final String THRALL_STICKY_TARGET_ATTR = "thrall_sticky_target";
    private static final String THRALL_EXPIRE_AT_ATTR = "thrall_expire_at";
    private static final String MARK_OF_DARKNESS_ATTR = "mark_of_darkness_expire_at";
    private static final String WARD_OF_ARCEUUS_ATTR = "ward_of_arceuus_expire_at";
    private static final String SHADOW_VEIL_ATTR = "shadow_veil_expire_at";
    private static final String DEATH_CHARGE_ATTR = "death_charge_expire_at";

    private record TeleportSpell(int level, Location location, int experience, Item[] runes) {}
    private record ReanimationSpell(int level, int experience, Item[] runes, int[] allowedHeads, CastVisual visual) {}
    private record ThrallSpell(int level, int npcId, int maxHit, Item[] runes, CastVisual visual) {}
    private record UtilitySpell(int level, int experience, Item[] runes, CastVisual visual) {}
    private record OfferingTarget(double basePrayerXp, int prayerRestore, int requiredPrayerLevel) {}
    private record OfferingSelection(int itemId, OfferingTarget target) {}

    private static final Map<Integer, TeleportSpell> TELEPORTS = new HashMap<>();
    private static final Map<Integer, ReanimationSpell> REANIMATION_SPELLS = new HashMap<>();
    private static final Map<Integer, Integer> REANIMATED_NPCS = new HashMap<>();
    private static final Map<Integer, ThrallSpell> THRALL_SPELLS = new HashMap<>();
    private static final Map<Integer, UtilitySpell> UTILITY_SPELLS = new HashMap<>();
    private static final Map<Integer, OfferingTarget> DEMONIC_ASHES = new HashMap<>();
    private static final Map<Integer, OfferingTarget> SINISTER_BONES = new HashMap<>();
    private static final Map<Player, NPC> ACTIVE_THRALLS = new ConcurrentHashMap<>();
    private static final Map<Player, Long> THRALL_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<Player, Long> OFFERING_COOLDOWNS = new ConcurrentHashMap<>();

    static {
        Item[] basicTeleportRunes = new Item[]{
                new Item(ItemIdentifiers.LAW_RUNE, 1),
                new Item(ItemIdentifiers.SOUL_RUNE, 1),
                new Item(ItemIdentifiers.EARTH_RUNE, 1)
        };
        TELEPORTS.put(ARCEUUS_LIBRARY_TELEPORT, new TeleportSpell(6, new Location(1634, 3836, 0), 10, basicTeleportRunes));
        TELEPORTS.put(DRAYNOR_MANOR_TELEPORT, new TeleportSpell(17, new Location(3107, 3352, 0), 18, basicTeleportRunes));
        TELEPORTS.put(BATTLEFRONT_TELEPORT, new TeleportSpell(23, new Location(1349, 3734, 0), 24, basicTeleportRunes));
        TELEPORTS.put(MIND_ALTAR_TELEPORT, new TeleportSpell(28, new Location(2979, 3510, 0), 31, basicTeleportRunes));
        TELEPORTS.put(RESPAWN_TELEPORT, new TeleportSpell(34, playerSpawnOrDefault(), 35, basicTeleportRunes));
        TELEPORTS.put(SALVE_GRAVEYARD_TELEPORT, new TeleportSpell(40, new Location(3433, 3460, 0), 40, basicTeleportRunes));
        TELEPORTS.put(FENKENSTRAINS_CASTLE_TELEPORT, new TeleportSpell(48, new Location(3547, 3528, 0), 50, basicTeleportRunes));
        TELEPORTS.put(WEST_ARDOUGNE_TELEPORT, new TeleportSpell(61, new Location(2500, 3290, 0), 68, basicTeleportRunes));
        TELEPORTS.put(HARMONY_ISLAND_TELEPORT, new TeleportSpell(65, new Location(3797, 2828, 0), 74, basicTeleportRunes));
        TELEPORTS.put(CEMETERY_TELEPORT, new TeleportSpell(71, new Location(2978, 3763, 0), 82, basicTeleportRunes));
        TELEPORTS.put(BARROWS_TELEPORT, new TeleportSpell(83, new Location(3565, 3306, 0), 90, basicTeleportRunes));
        TELEPORTS.put(APE_ATOLL_TELEPORT, new TeleportSpell(90, new Location(2769, 2703, 0), 100, basicTeleportRunes));

        REANIMATION_SPELLS.put(BASIC_REANIMATION, new ReanimationSpell(16, 32,
                new Item[]{new Item(ItemIdentifiers.BODY_RUNE, 4), new Item(ItemIdentifiers.NATURE_RUNE, 2), new Item(ItemIdentifiers.SOUL_RUNE, 1)},
                new int[]{ItemIdentifiers.ENSOULED_GOBLIN_HEAD, ItemIdentifiers.ENSOULED_MONKEY_HEAD, ItemIdentifiers.ENSOULED_IMP_HEAD}, REANIMATION_VISUAL));
        REANIMATION_SPELLS.put(ADEPT_REANIMATION, new ReanimationSpell(41, 80,
                new Item[]{new Item(ItemIdentifiers.BODY_RUNE, 4), new Item(ItemIdentifiers.NATURE_RUNE, 3), new Item(ItemIdentifiers.SOUL_RUNE, 1)},
                new int[]{ItemIdentifiers.ENSOULED_MINOTAUR_HEAD, ItemIdentifiers.ENSOULED_SCORPION_HEAD}, REANIMATION_VISUAL));
        REANIMATION_SPELLS.put(EXPERT_REANIMATION, new ReanimationSpell(72, 138,
                new Item[]{new Item(ItemIdentifiers.BODY_RUNE, 4), new Item(ItemIdentifiers.NATURE_RUNE, 4), new Item(ItemIdentifiers.SOUL_RUNE, 1)},
                new int[]{ItemIdentifiers.ENSOULED_DEMON_HEAD}, REANIMATION_VISUAL));
        REANIMATION_SPELLS.put(MASTER_REANIMATION, new ReanimationSpell(90, 170,
                new Item[]{new Item(ItemIdentifiers.BODY_RUNE, 4), new Item(ItemIdentifiers.NATURE_RUNE, 5), new Item(ItemIdentifiers.SOUL_RUNE, 1)},
                new int[]{ItemIdentifiers.ENSOULED_DRAGON_HEAD}, REANIMATION_VISUAL));

        Item[] thrallRunes = new Item[]{
                new Item(ItemIdentifiers.BLOOD_RUNE, 1),
                new Item(ItemIdentifiers.COSMIC_RUNE, 1),
                new Item(ItemIdentifiers.EARTH_RUNE, 10),
                new Item(ItemIdentifiers.FIRE_RUNE, 5)
        };
        THRALL_SPELLS.put(RESURRECT_LESSER_GHOST, new ThrallSpell(52, THRALL_GHOST_NPC, 1, thrallRunes, RESURRECT_LESSER_GHOST_VISUAL));
        THRALL_SPELLS.put(RESURRECT_LESSER_SKELETON, new ThrallSpell(56, THRALL_SKELETON_NPC, 1, thrallRunes, RESURRECT_LESSER_SKELETON_VISUAL));
        THRALL_SPELLS.put(RESURRECT_LESSER_ZOMBIE, new ThrallSpell(60, THRALL_ZOMBIE_NPC, 1, thrallRunes, RESURRECT_LESSER_ZOMBIE_VISUAL));
        THRALL_SPELLS.put(RESURRECT_SUPERIOR_GHOST, new ThrallSpell(76, THRALL_GHOST_NPC, 2, thrallRunes, RESURRECT_SUPERIOR_GHOST_VISUAL));
        THRALL_SPELLS.put(RESURRECT_SUPERIOR_SKELETON, new ThrallSpell(80, THRALL_SKELETON_NPC, 2, thrallRunes, RESURRECT_SUPERIOR_SKELETON_VISUAL));
        THRALL_SPELLS.put(RESURRECT_SUPERIOR_ZOMBIE, new ThrallSpell(84, THRALL_ZOMBIE_NPC, 2, thrallRunes, RESURRECT_SUPERIOR_ZOMBIE_VISUAL));
        THRALL_SPELLS.put(RESURRECT_GREATER_GHOST, new ThrallSpell(88, THRALL_GHOST_NPC, 3, thrallRunes, RESURRECT_GREATER_GHOST_VISUAL));
        THRALL_SPELLS.put(RESURRECT_GREATER_SKELETON, new ThrallSpell(92, THRALL_SKELETON_NPC, 3, thrallRunes, RESURRECT_GREATER_SKELETON_VISUAL));
        THRALL_SPELLS.put(RESURRECT_GREATER_ZOMBIE, new ThrallSpell(96, THRALL_ZOMBIE_NPC, 3, thrallRunes, RESURRECT_GREATER_ZOMBIE_VISUAL));

        UTILITY_SPELLS.put(RESURRECT_CROPS, new UtilitySpell(78, 65,
                new Item[]{new Item(ItemIdentifiers.BLOOD_RUNE, 8), new Item(ItemIdentifiers.NATURE_RUNE, 12), new Item(ItemIdentifiers.SOUL_RUNE, 2)}, RESURRECT_CROPS_VISUAL));
        UTILITY_SPELLS.put(MARK_OF_DARKNESS, new UtilitySpell(59, 45,
                new Item[]{new Item(ItemIdentifiers.BLOOD_RUNE, 1), new Item(ItemIdentifiers.SOUL_RUNE, 1), new Item(ItemIdentifiers.COSMIC_RUNE, 1)}, MARK_OF_DARKNESS_VISUAL));
        UTILITY_SPELLS.put(WARD_OF_ARCEUUS, new UtilitySpell(73, 80,
                new Item[]{new Item(ItemIdentifiers.BLOOD_RUNE, 1), new Item(ItemIdentifiers.SOUL_RUNE, 1), new Item(ItemIdentifiers.COSMIC_RUNE, 1)}, WARD_OF_ARCEUUS_VISUAL));
        UTILITY_SPELLS.put(SHADOW_VEIL, new UtilitySpell(47, 58,
                new Item[]{new Item(ItemIdentifiers.SOUL_RUNE, 1), new Item(ItemIdentifiers.EARTH_RUNE, 1), new Item(ItemIdentifiers.WATER_RUNE, 1)}, SHADOW_VEIL_VISUAL));
        UTILITY_SPELLS.put(VILE_VIGOUR, new UtilitySpell(66, 76,
                new Item[]{new Item(ItemIdentifiers.SOUL_RUNE, 1), new Item(ItemIdentifiers.BLOOD_RUNE, 1), new Item(ItemIdentifiers.EARTH_RUNE, 2)}, VILE_VIGOUR_VISUAL));
        UTILITY_SPELLS.put(DARK_LURE, new UtilitySpell(50, 60,
                new Item[]{new Item(ItemIdentifiers.SOUL_RUNE, 1), new Item(ItemIdentifiers.COSMIC_RUNE, 1), new Item(ItemIdentifiers.EARTH_RUNE, 1)}, DARK_LURE_VISUAL));
        UTILITY_SPELLS.put(DEATH_CHARGE, new UtilitySpell(85, 90,
                new Item[]{new Item(ItemIdentifiers.BLOOD_RUNE, 1), new Item(ItemIdentifiers.SOUL_RUNE, 1), new Item(ItemIdentifiers.COSMIC_RUNE, 1)}, DEATH_CHARGE_VISUAL));
        UTILITY_SPELLS.put(DEMONIC_OFFERING, new UtilitySpell(84, 175,
                new Item[]{new Item(ItemIdentifiers.SOUL_RUNE, 1), new Item(ItemIdentifiers.WRATH_RUNE, 1)}, DEMONIC_OFFERING_VISUAL));
        UTILITY_SPELLS.put(SINISTER_OFFERING, new UtilitySpell(92, 180,
                new Item[]{new Item(ItemIdentifiers.BLOOD_RUNE, 1), new Item(ItemIdentifiers.WRATH_RUNE, 1)}, SINISTER_OFFERING_VISUAL));
        UTILITY_SPELLS.put(DEGRIME, new UtilitySpell(70, 83,
                new Item[]{new Item(ItemIdentifiers.COSMIC_RUNE, 2), new Item(ItemIdentifiers.SOUL_RUNE, 2), new Item(ItemIdentifiers.WATER_RUNE, 15)}, DEGRIME_VISUAL));

        // Demonic Offering (OSRS wiki parity): first 3 demonic ashes, infernal restores 2 prayer.
        registerDemonicAsh(ItemIdentifiers.FIENDISH_ASHES, 10.0, 1);
        registerDemonicAsh(ItemIdentifiers.VILE_ASHES, 25.0, 1);
        registerDemonicAsh(ItemIdentifiers.MALICIOUS_ASHES, 65.0, 1);
        registerDemonicAsh(ItemIdentifiers.ABYSSAL_ASHES, 85.0, 1);
        registerDemonicAsh(ItemIdentifiers.INFERNAL_ASHES, 110.0, 2);

        // Sinister Offering (OSRS wiki parity): first 3 bones, special 2-point restores below.
        registerSinisterBone(ItemIdentifiers.BONES, 13.5, 1, 0);
        registerSinisterBone(ItemIdentifiers.MONKEY_BONES, 15.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.BAT_BONES, 15.9, 1, 0);
        registerSinisterBone(ItemIdentifiers.BIG_BONES, 45.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.JOGRE_BONES, 45.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.ZOGRE_BONES, 67.5, 1, 0);
        registerSinisterBone(ItemIdentifiers.SHAIKAHAN_BONES, 75.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.BABYDRAGON_BONES, 90.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.WYRM_BONES, 50.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.DRAGON_BONES, 72.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.WYVERN_BONES, 72.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.WYVERN_BONES_2, 72.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.DRAKE_BONES, 80.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.FAYRG_BONES, 84.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.LAVA_DRAGON_BONES, 85.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.RAURG_BONES, 96.0, 1, 0);
        registerSinisterBone(ItemIdentifiers.HYDRA_BONES, 110.0, 2, 0);
        registerSinisterBone(ItemIdentifiers.DAGANNOTH_BONES, 125.0, 2, 0);
        registerSinisterBone(ItemIdentifiers.OURG_BONES, 140.0, 2, 0);
        registerSinisterBone(14793, 140.0, 2, 0); // Legacy Ourg id used in older definitions.
        registerSinisterBone(ItemIdentifiers.SUPERIOR_DRAGON_BONES, 150.0, 2, SUPERIOR_DRAGON_BONE_MIN_PRAYER);

        // Ensouled heads -> reanimated NPCs
        REANIMATED_NPCS.put(ItemIdentifiers.ENSOULED_GOBLIN_HEAD, 7018);
        REANIMATED_NPCS.put(ItemIdentifiers.ENSOULED_MONKEY_HEAD, 7019);
        REANIMATED_NPCS.put(ItemIdentifiers.ENSOULED_IMP_HEAD, 7020);
        REANIMATED_NPCS.put(ItemIdentifiers.ENSOULED_MINOTAUR_HEAD, 7021);
        REANIMATED_NPCS.put(ItemIdentifiers.ENSOULED_SCORPION_HEAD, 7022);
        REANIMATED_NPCS.put(ItemIdentifiers.ENSOULED_DEMON_HEAD, 7026);
        REANIMATED_NPCS.put(ItemIdentifiers.ENSOULED_DRAGON_HEAD, 7027);
    }

    public static boolean handleButton(Player player, int button) {
        if (player.getSpellbook() != MagicSpellbook.ARCEUUS) {
            return false;
        }
        TeleportSpell teleport = TELEPORTS.get(button);
        if (teleport != null) {
            if (!player.getClickDelay().elapsed(1200)) {
                return true;
            }
            if (!checkMagicLevel(player, teleport.level())) {
                return true;
            }
            Item[] runes = suppressRunes(player, teleport.runes());
            if (!hasRunes(player, runes)) {
                player.getPacketSender().sendMessage("You do not have the required runes to cast this spell.");
                return true;
            }
            if (!TeleportHandler.checkReqs(player, teleport.location())) {
                return true;
            }
            consumeRunes(player, runes);
            TeleportHandler.teleport(player, teleport.location(), TeleportType.ARCEUUS, true);
            player.getSkillManager().addExperience(Skill.MAGIC, teleport.experience());
            player.getClickDelay().reset();
            return true;
        }

        ThrallSpell thrallSpell = THRALL_SPELLS.get(button);
        if (thrallSpell != null) {
            return summonThrall(player, thrallSpell);
        }

        UtilitySpell utilitySpell = UTILITY_SPELLS.get(button);
        if (utilitySpell != null) {
            return castUtilitySpell(player, button, utilitySpell);
        }
        return false;
    }

    public static boolean handleMagicOnItem(Player player, int spellId, Item item) {
        if (player.getSpellbook() != MagicSpellbook.ARCEUUS) {
            return false;
        }
        ReanimationSpell spell = REANIMATION_SPELLS.get(spellId);
        if (spell != null) {
            if (!player.getClickDelay().elapsed(1200)) {
                return true;
            }
            if (!checkMagicLevel(player, spell.level())) {
                return true;
            }
            if (!isAllowedHead(spell, item.getId())) {
                player.getPacketSender().sendMessage("That head cannot be reanimated with this spell tier.");
                return true;
            }
            Integer npcId = REANIMATED_NPCS.get(item.getId());
            if (npcId == null) {
                player.getPacketSender().sendMessage("This spell can only be cast on an ensouled head.");
                return true;
            }
            Item[] runes = suppressRunes(player, spell.runes());
            if (!hasRunes(player, runes)) {
                player.getPacketSender().sendMessage("You do not have the required runes to cast this spell.");
                return true;
            }

            consumeRunes(player, runes);
            playCastVisual(player, spell.visual());
            player.getInventory().delete(item.getId(), 1);
            spawnReanimatedNpc(player, npcId);
            player.getSkillManager().addExperience(Skill.MAGIC, spell.experience());
            player.getClickDelay().reset();
            return true;
        }

        return handleUtilityMagicOnItem(player, spellId, item);
    }

    private static void spawnReanimatedNpc(Player player, int npcId) {
        NPC npc = NPC.create(npcId, player.getLocation().clone());
        npc.setOwner(player);
        npc.setArea(player.getArea());
        World.getAddNPCQueue().add(npc);
        npc.getCombat().attack(player);
    }

    private static boolean summonThrall(Player player, ThrallSpell spell) {
        if (!player.getClickDelay().elapsed(1200)) {
            return true;
        }
        if (!checkMagicLevel(player, spell.level())) {
            return true;
        }
        long now = System.currentTimeMillis();
        long nextAllowed = THRALL_COOLDOWNS.getOrDefault(player, 0L);
        if (now < nextAllowed) {
            int seconds = (int) Math.ceil((nextAllowed - now) / 1000.0D);
            player.getPacketSender().sendMessage("You can cast another resurrection spell in " + seconds + "s.");
            return true;
        }
        if (player.getSkillManager().getCurrentLevel(Skill.PRAYER) < THRALL_PRAYER_COST) {
            player.getPacketSender().sendMessage("You need at least " + THRALL_PRAYER_COST + " Prayer points.");
            return true;
        }
        Item[] runes = suppressRunes(player, spell.runes());
        if (!hasRunes(player, runes)) {
            player.getPacketSender().sendMessage("You do not have the required runes to cast this spell.");
            return true;
        }

        consumeRunes(player, runes);
        player.getSkillManager().setCurrentLevel(Skill.PRAYER,
                player.getSkillManager().getCurrentLevel(Skill.PRAYER) - THRALL_PRAYER_COST);

        NPC existing = ACTIVE_THRALLS.remove(player);
        if (existing != null) {
            World.getRemoveNPCQueue().add(existing);
        }
        if (player.getCurrentPet() != null) {
            player.setCurrentPet(null);
        }

        playCastVisual(player, spell.visual());

        NPC thrall = NPC.create(spell.npcId(), player.getLocation().clone());
        thrall.setOwner(player);
        thrall.setPet(true);
        thrall.setUntargetable(true);
        thrall.setAttribute(THRALL_MAX_HIT_ATTR, spell.maxHit());
        thrall.setAttribute(THRALL_EXPIRE_AT_ATTR, now + THRALL_LIFETIME_MS);
        thrall.setFollowing(player);
        thrall.setMobileInteraction(player);
        thrall.setArea(player.getArea());
        World.getAddNPCQueue().add(thrall);
        player.setCurrentPet(thrall);
        ACTIVE_THRALLS.put(player, thrall);
        THRALL_COOLDOWNS.put(player, now + THRALL_COOLDOWN_MS);

        TaskManager.submit(new Task(1, player, true) {
            @Override
            protected void execute() {
                NPC active = ACTIVE_THRALLS.get(player);
                if (active == null || player == null || !player.isRegistered()) {
                    stop();
                    return;
                }
                if (!active.isRegistered()) {
                    return;
                }
                if (player.getHitpoints() <= 0 || player.getPrivateArea() != active.getPrivateArea()) {
                    cleanupThrall(player);
                    stop();
                    return;
                }
                long expiresAt = (long) active.getAttribute(THRALL_EXPIRE_AT_ATTR, 0L);
                if (expiresAt > 0L && System.currentTimeMillis() >= expiresAt) {
                    cleanupThrall(player);
                    stop();
                    return;
                }

                Mobile target = resolveThrallTarget(player, active);
                if (target == null || !target.isRegistered() || target == active || target == player) {
                    active.getCombat().reset();
                    active.setFollowing(player);
                    active.setMobileInteraction(player);
                    followMobileStep(active, player, 1);
                    return;
                }

                active.setFollowing(target);
                active.setCombatFollowing(target);
                active.setMobileInteraction(target);
                CombatMethod method = CombatFactory.getMethod(active);
                boolean canReach = CombatFactory.canReach(active, method, target);
                if (!canReach) {
                    // Force-close distance when blocked from attacking (especially projectile LoS dead-zones).
                    followMobileStep(active, target, 1);
                } else {
                    followMobileStep(active, target, isZombieThrall(active.getId()) ? 1 : 6);
                }

                if (active.getCombat().getTarget() != target) {
                    active.getCombat().attack(target);
                }
            }
        });

        player.getClickDelay().reset();
        return true;
    }

    public static void cleanupThrall(Player player) {
        NPC active = ACTIVE_THRALLS.remove(player);
        if (active != null && active.isRegistered()) {
            World.getRemoveNPCQueue().add(active);
        }
        if (player != null && player.getCurrentPet() == active) {
            player.setCurrentPet(null);
        }
    }

    private static Mobile resolveThrallTarget(Player player, NPC thrall) {
        Mobile explicitTarget = player.getCombat().getTarget();
        if (isValidThrallTarget(explicitTarget, player, thrall)) {
            thrall.setAttribute(THRALL_STICKY_TARGET_ATTR, explicitTarget);
            return explicitTarget;
        }

        Mobile sticky = (Mobile) thrall.getAttribute(THRALL_STICKY_TARGET_ATTR);
        if (isValidThrallTarget(sticky, player, thrall)) {
            Mobile playerAttacker = player.getCombat().getAttacker();
            if (playerAttacker == null || playerAttacker == sticky || playerAttacker == thrall || playerAttacker == player) {
                return sticky;
            }
        }

        Mobile attackerTarget = player.getCombat().getAttacker();
        if (isValidThrallTarget(attackerTarget, player, thrall)) {
            thrall.setAttribute(THRALL_STICKY_TARGET_ATTR, attackerTarget);
            return attackerTarget;
        }
        return null;
    }

    private static boolean isValidThrallTarget(Mobile target, Player player, NPC thrall) {
        if (target == null || target == thrall || target == player || !target.isRegistered()) {
            return false;
        }
        return target.getPrivateArea() == player.getPrivateArea();
    }

    private static void followMobileStep(NPC thrall, Mobile mobile, int desiredDistance) {
        if (thrall.getLocation().getZ() != mobile.getLocation().getZ()) {
            return;
        }
        if (thrall.getLocation().getDistance(mobile.getLocation()) <= desiredDistance) {
            return;
        }

        Location from = thrall.getLocation();
        Location to = mobile.getLocation();

        int dx = Integer.compare(to.getX(), from.getX());
        int dy = Integer.compare(to.getY(), from.getY());

        if (dx != 0 && dy != 0) {
            if (thrall.getMovementQueue().canWalk(dx, dy)) {
                thrall.getMovementQueue().walkStep(dx, dy);
                return;
            }
            if (thrall.getMovementQueue().canWalk(dx, 0)) {
                thrall.getMovementQueue().walkStep(dx, 0);
                return;
            }
            if (thrall.getMovementQueue().canWalk(0, dy)) {
                thrall.getMovementQueue().walkStep(0, dy);
                return;
            }
            return;
        }

        if (thrall.getMovementQueue().canWalk(dx, dy)) {
            thrall.getMovementQueue().walkStep(dx, dy);
        }
    }

    private static boolean checkMagicLevel(Player player, int level) {
        if (player.getSkillManager().getCurrentLevel(Skill.MAGIC) < level) {
            player.getPacketSender().sendMessage("You need a Magic level of " + level + " to cast this spell.");
            return false;
        }
        return true;
    }

    private static Item[] suppressRunes(Player player, Item[] runes) {
        return PlayerMagicStaff.suppressRunes(player, runes);
    }

    private static boolean hasRunes(Player player, Item[] runes) {
        return player.getInventory().containsAll(runes);
    }

    private static void consumeRunes(Player player, Item[] runes) {
        Arrays.stream(runes).forEach(player.getInventory()::delete);
    }

    private static boolean isAllowedHead(ReanimationSpell spell, int headId) {
        for (int allowedHead : spell.allowedHeads()) {
            if (allowedHead == headId) {
                return true;
            }
        }
        return false;
    }

    private static boolean isZombieThrall(int npcId) {
        return npcId == THRALL_ZOMBIE_NPC;
    }

    private static void playCastVisual(Player player, CastVisual visual) {
        player.performAnimation(new Animation(visual.animationId()));
        player.performGraphic(new Graphic(visual.graphicId(), visual.height()));
    }

    private static boolean handleUtilityMagicOnItem(Player player, int spellId, Item item) {
        UtilitySpell spell = UTILITY_SPELLS.get(spellId);
        if (spell == null) {
            return false;
        }
        if (spellId != DEMONIC_OFFERING && spellId != SINISTER_OFFERING && spellId != DEGRIME) {
            return false;
        }
        if (!player.getClickDelay().elapsed(1200)) {
            return true;
        }
        if (!checkMagicLevel(player, spell.level())) {
            return true;
        }
        Item[] runes = suppressRunes(player, spell.runes());
        if (!hasRunes(player, runes)) {
            player.getPacketSender().sendMessage("You do not have the required runes to cast this spell.");
            return true;
        }

        if (spellId == DEMONIC_OFFERING || spellId == SINISTER_OFFERING) {
            return castOfferingSpell(player, spellId, spell, item, runes);
        }

        String name = item.getDefinition() != null ? item.getDefinition().getName().toLowerCase() : "";
        if (spellId == DEGRIME) {
            if (!name.startsWith("grimy ")) {
                player.getPacketSender().sendMessage("You can only cast Degrime on grimy herbs.");
                return true;
            }
            consumeRunes(player, runes);
            playCastVisual(player, spell.visual());
            int cleanedId = item.getId() + 1;
            player.getInventory().delete(item.getId(), 1).add(cleanedId, 1);
            player.getSkillManager().addExperience(Skill.MAGIC, spell.experience());
            player.getClickDelay().reset();
            return true;
        }
        return false;
    }

    private static boolean castUtilitySpell(Player player, int spellId, UtilitySpell spell) {
        if (!player.getClickDelay().elapsed(1200)) {
            return true;
        }
        if (!checkMagicLevel(player, spell.level())) {
            return true;
        }
        Item[] runes = suppressRunes(player, spell.runes());
        if (!hasRunes(player, runes)) {
            player.getPacketSender().sendMessage("You do not have the required runes to cast this spell.");
            return true;
        }
        if ((spellId == DEMONIC_OFFERING || spellId == SINISTER_OFFERING || spellId == DEGRIME)) {
            player.getPacketSender().sendMessage("Use this spell on an item in your inventory.");
            return true;
        }

        long now = System.currentTimeMillis();
        if (spellId == VILE_VIGOUR) {
            int currentPrayer = player.getSkillManager().getCurrentLevel(Skill.PRAYER);
            if (currentPrayer < 3) {
                player.getPacketSender().sendMessage("You need at least 3 Prayer points to cast this spell.");
                return true;
            }
        }

        consumeRunes(player, runes);
        playCastVisual(player, spell.visual());

        switch (spellId) {
            case RESURRECT_CROPS -> player.getPacketSender().sendMessage("Your resurrection magic revitalises nearby crops.");
            case MARK_OF_DARKNESS -> {
                player.setAttribute(MARK_OF_DARKNESS_ATTR, now + 60_000L);
                player.getPacketSender().sendMessage("You are cloaked in Mark of Darkness.");
            }
            case WARD_OF_ARCEUUS -> {
                player.setAttribute(WARD_OF_ARCEUUS_ATTR, now + 60_000L);
                player.getPacketSender().sendMessage("A ward of dark energy surrounds you.");
            }
            case SHADOW_VEIL -> {
                player.setAttribute(SHADOW_VEIL_ATTR, now + 60_000L);
                player.getPacketSender().sendMessage("You blend into the shadows.");
            }
            case VILE_VIGOUR -> {
                int currentPrayer = player.getSkillManager().getCurrentLevel(Skill.PRAYER);
                player.getSkillManager().setCurrentLevel(Skill.PRAYER, currentPrayer - 3);
                player.getSkillManager().updateSkill(Skill.PRAYER);
                player.setRunEnergy(Math.min(100, player.getRunEnergy() + 30));
                player.getPacketSender().sendRunEnergy();
            }
            case DARK_LURE -> player.getPacketSender().sendMessage("Dark Lure requires a valid nearby target.");
            case DEATH_CHARGE -> {
                player.setAttribute(DEATH_CHARGE_ATTR, now + 60_000L);
                player.getPacketSender().sendMessage("You are empowered by Death Charge.");
            }
            default -> { }
        }
        player.getSkillManager().addExperience(Skill.MAGIC, spell.experience());
        player.getClickDelay().reset();
        return true;
    }

    private static boolean castOfferingSpell(Player player, int spellId, UtilitySpell spell, Item clickedItem, Item[] runes) {
        boolean demonic = spellId == DEMONIC_OFFERING;
        int currentPrayer = player.getSkillManager().getCurrentLevel(Skill.PRAYER);
        OfferingTarget clickedTarget = demonic
                ? resolveDemonicAsh(clickedItem.getId())
                : resolveSinisterBone(clickedItem.getId());
        if (clickedTarget == null) {
            player.getPacketSender().sendMessage(demonic
                    ? "You can only cast this spell on demonic ashes."
                    : "You can only cast this spell on bones.");
            return true;
        }
        if (!demonic && clickedTarget.requiredPrayerLevel() > 0 && currentPrayer < clickedTarget.requiredPrayerLevel()) {
            player.getPacketSender().sendMessage("You need at least " + clickedTarget.requiredPrayerLevel()
                    + " Prayer points to offer superior dragon bones.");
            return true;
        }

        long now = System.currentTimeMillis();
        long nextAllowed = OFFERING_COOLDOWNS.getOrDefault(player, 0L);
        if (now < nextAllowed) {
            int seconds = (int) Math.ceil((nextAllowed - now) / 1000.0D);
            player.getPacketSender().sendMessage("You can cast another offering spell in " + seconds + "s.");
            return true;
        }

        List<OfferingSelection> toConsume = new ArrayList<>(OFFERING_MAX_ITEMS_PER_CAST);
        boolean blockedByPrayerRequirement = false;
        Item[] inventory = player.getInventory().getItems();
        for (Item stack : inventory) {
            if (stack == null || stack.getId() <= 0) {
                continue;
            }
            OfferingTarget target = demonic ? resolveDemonicAsh(stack.getId()) : resolveSinisterBone(stack.getId());
            if (target == null) {
                continue;
            }
            if (!demonic && target.requiredPrayerLevel() > 0 && currentPrayer < target.requiredPrayerLevel()) {
                blockedByPrayerRequirement = true;
                continue;
            }
            int stackAmount = Math.max(1, stack.getAmount());
            for (int i = 0; i < stackAmount && toConsume.size() < OFFERING_MAX_ITEMS_PER_CAST; i++) {
                toConsume.add(new OfferingSelection(stack.getId(), target));
            }
            if (toConsume.size() >= OFFERING_MAX_ITEMS_PER_CAST) {
                break;
            }
        }

        if (toConsume.isEmpty()) {
            if (!demonic && blockedByPrayerRequirement) {
                player.getPacketSender().sendMessage("You need at least " + SUPERIOR_DRAGON_BONE_MIN_PRAYER
                        + " Prayer points to offer superior dragon bones.");
            } else {
                player.getPacketSender().sendMessage(demonic
                        ? "You have no demonic ashes to offer."
                        : "You have no bones to offer.");
            }
            return true;
        }

        consumeRunes(player, runes);
        playCastVisual(player, spell.visual());

        double basePrayerExperience = 0.0D;
        int prayerRestore = 0;
        for (OfferingSelection selection : toConsume) {
            player.getInventory().delete(selection.itemId(), 1);
            basePrayerExperience += selection.target().basePrayerXp();
            prayerRestore += selection.target().prayerRestore();
        }

        int prayerExperience = (int) Math.round(basePrayerExperience * 3.0D);
        if (prayerExperience > 0) {
            player.getSkillManager().addExperience(Skill.PRAYER, prayerExperience);
        }
        int maxPrayer = player.getSkillManager().getMaxLevel(Skill.PRAYER);
        player.getSkillManager().setCurrentLevel(Skill.PRAYER, Math.min(maxPrayer, currentPrayer + prayerRestore));
        player.getSkillManager().updateSkill(Skill.PRAYER);
        player.getSkillManager().addExperience(Skill.MAGIC, spell.experience());
        OFFERING_COOLDOWNS.put(player, now + OFFERING_COOLDOWN_MS);
        player.getClickDelay().reset();
        return true;
    }

    private static OfferingTarget resolveDemonicAsh(int itemId) {
        return DEMONIC_ASHES.get(itemId);
    }

    private static OfferingTarget resolveSinisterBone(int itemId) {
        if (itemId == ItemIdentifiers.LONG_BONE || itemId == ItemIdentifiers.CURVED_BONE) {
            return null;
        }
        OfferingTarget direct = SINISTER_BONES.get(itemId);
        if (direct != null) {
            return direct;
        }
        Optional<BuriableBone> fallback = BuriableBone.forId(itemId);
        if (!fallback.isPresent()) {
            return null;
        }
        return new OfferingTarget(fallback.get().getXp(), 1, 0);
    }

    private static void registerDemonicAsh(int itemId, double basePrayerXp, int prayerRestore) {
        DEMONIC_ASHES.put(itemId, new OfferingTarget(basePrayerXp, prayerRestore, 0));
    }

    private static void registerSinisterBone(int itemId, double basePrayerXp, int prayerRestore, int requiredPrayerLevel) {
        SINISTER_BONES.put(itemId, new OfferingTarget(basePrayerXp, prayerRestore, requiredPrayerLevel));
    }

    private static Location playerSpawnOrDefault() {
        return GameConstants.DEFAULT_LOCATION.clone();
    }
}
