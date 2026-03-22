package com.elvarg.game.content.combat.magic;

import com.elvarg.game.World;
import com.elvarg.game.GameConstants;
import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.method.CombatMethod;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

    public static final int BASIC_REANIMATION = 30601;
    public static final int ADEPT_REANIMATION = 30605;
    public static final int EXPERT_REANIMATION = 30609;
    public static final int MASTER_REANIMATION = 30613;

    public static final int RESURRECT_LESSER_GHOST = 30621;
    public static final int RESURRECT_LESSER_SKELETON = 30625;
    public static final int RESURRECT_LESSER_ZOMBIE = 30629;
    public static final int RESURRECT_SUPERIOR_GHOST = 30663;
    public static final int RESURRECT_SUPERIOR_SKELETON = 30667;
    public static final int RESURRECT_SUPERIOR_ZOMBIE = 30671;
    public static final int RESURRECT_GREATER_GHOST = 30675;
    public static final int RESURRECT_GREATER_SKELETON = 30679;
    public static final int RESURRECT_GREATER_ZOMBIE = 30683;

    private static final int ARCEUUS_TELEPORT_ANIM = 753;
    private static final int ARCEUUS_TELEPORT_GFX = 1129;
    private static final int REANIMATION_ANIM = 7198;
    private static final int REANIMATION_GFX = 1288;
    private static final int THRALL_ANIM = 8970;

    private static final int THRALL_GHOST_NPC = 10884;
    private static final int THRALL_SKELETON_NPC = 10885;
    private static final int THRALL_ZOMBIE_NPC = 10886;
    private static final int THRALL_GHOST_GFX = 1873;
    private static final int THRALL_SKELETON_GFX = 1874;
    private static final int THRALL_ZOMBIE_GFX = 1875;
    private static final int THRALL_PRAYER_COST = 6;
    private static final int THRALL_COOLDOWN_TICKS = 16;
    private static final int THRALL_LIFETIME_TICKS = 100; // 60 seconds
    private static final long THRALL_COOLDOWN_MS = (long) THRALL_COOLDOWN_TICKS * GameConstants.GAME_ENGINE_PROCESSING_CYCLE_RATE;
    private static final long THRALL_LIFETIME_MS = (long) THRALL_LIFETIME_TICKS * GameConstants.GAME_ENGINE_PROCESSING_CYCLE_RATE;
    private static final String THRALL_MAX_HIT_ATTR = "thrall_max_hit";
    private static final String THRALL_STICKY_TARGET_ATTR = "thrall_sticky_target";
    private static final String THRALL_EXPIRE_AT_ATTR = "thrall_expire_at";

    private record TeleportSpell(int level, Location location, int experience, Item[] runes) {}
    private record ReanimationSpell(int level, int experience, Item[] runes, int[] allowedHeads) {}
    private record ThrallSpell(int level, int npcId, int gfxId, int maxHit, Item[] runes) {}

    private static final Map<Integer, TeleportSpell> TELEPORTS = new HashMap<>();
    private static final Map<Integer, ReanimationSpell> REANIMATION_SPELLS = new HashMap<>();
    private static final Map<Integer, Integer> REANIMATED_NPCS = new HashMap<>();
    private static final Map<Integer, ThrallSpell> THRALL_SPELLS = new HashMap<>();
    private static final Map<Player, NPC> ACTIVE_THRALLS = new ConcurrentHashMap<>();
    private static final Map<Player, Long> THRALL_COOLDOWNS = new ConcurrentHashMap<>();

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
        TELEPORTS.put(SALVE_GRAVEYARD_TELEPORT, new TeleportSpell(40, new Location(3433, 3460, 0), 40, basicTeleportRunes));
        TELEPORTS.put(FENKENSTRAINS_CASTLE_TELEPORT, new TeleportSpell(48, new Location(3547, 3528, 0), 50, basicTeleportRunes));
        TELEPORTS.put(WEST_ARDOUGNE_TELEPORT, new TeleportSpell(61, new Location(2500, 3290, 0), 68, basicTeleportRunes));
        TELEPORTS.put(HARMONY_ISLAND_TELEPORT, new TeleportSpell(65, new Location(3797, 2828, 0), 74, basicTeleportRunes));
        TELEPORTS.put(CEMETERY_TELEPORT, new TeleportSpell(71, new Location(2978, 3763, 0), 82, basicTeleportRunes));
        TELEPORTS.put(BARROWS_TELEPORT, new TeleportSpell(83, new Location(3565, 3306, 0), 90, basicTeleportRunes));
        TELEPORTS.put(APE_ATOLL_TELEPORT, new TeleportSpell(90, new Location(2769, 2703, 0), 100, basicTeleportRunes));

        REANIMATION_SPELLS.put(BASIC_REANIMATION, new ReanimationSpell(16, 32,
                new Item[]{new Item(ItemIdentifiers.BODY_RUNE, 4), new Item(ItemIdentifiers.NATURE_RUNE, 2), new Item(ItemIdentifiers.SOUL_RUNE, 1)},
                new int[]{ItemIdentifiers.ENSOULED_GOBLIN_HEAD, ItemIdentifiers.ENSOULED_MONKEY_HEAD, ItemIdentifiers.ENSOULED_IMP_HEAD}));
        REANIMATION_SPELLS.put(ADEPT_REANIMATION, new ReanimationSpell(41, 80,
                new Item[]{new Item(ItemIdentifiers.BODY_RUNE, 4), new Item(ItemIdentifiers.NATURE_RUNE, 3), new Item(ItemIdentifiers.SOUL_RUNE, 1)},
                new int[]{ItemIdentifiers.ENSOULED_MINOTAUR_HEAD, ItemIdentifiers.ENSOULED_SCORPION_HEAD}));
        REANIMATION_SPELLS.put(EXPERT_REANIMATION, new ReanimationSpell(72, 138,
                new Item[]{new Item(ItemIdentifiers.BODY_RUNE, 4), new Item(ItemIdentifiers.NATURE_RUNE, 4), new Item(ItemIdentifiers.SOUL_RUNE, 1)},
                new int[]{ItemIdentifiers.ENSOULED_DEMON_HEAD}));
        REANIMATION_SPELLS.put(MASTER_REANIMATION, new ReanimationSpell(90, 170,
                new Item[]{new Item(ItemIdentifiers.BODY_RUNE, 4), new Item(ItemIdentifiers.NATURE_RUNE, 5), new Item(ItemIdentifiers.SOUL_RUNE, 1)},
                new int[]{ItemIdentifiers.ENSOULED_DRAGON_HEAD}));

        Item[] thrallRunes = new Item[]{
                new Item(ItemIdentifiers.BLOOD_RUNE, 1),
                new Item(ItemIdentifiers.COSMIC_RUNE, 1),
                new Item(ItemIdentifiers.EARTH_RUNE, 10),
                new Item(ItemIdentifiers.FIRE_RUNE, 5)
        };
        THRALL_SPELLS.put(RESURRECT_LESSER_GHOST, new ThrallSpell(52, THRALL_GHOST_NPC, THRALL_GHOST_GFX, 1, thrallRunes));
        THRALL_SPELLS.put(RESURRECT_LESSER_SKELETON, new ThrallSpell(56, THRALL_SKELETON_NPC, THRALL_SKELETON_GFX, 1, thrallRunes));
        THRALL_SPELLS.put(RESURRECT_LESSER_ZOMBIE, new ThrallSpell(60, THRALL_ZOMBIE_NPC, THRALL_ZOMBIE_GFX, 1, thrallRunes));
        THRALL_SPELLS.put(RESURRECT_SUPERIOR_GHOST, new ThrallSpell(76, THRALL_GHOST_NPC, THRALL_GHOST_GFX, 2, thrallRunes));
        THRALL_SPELLS.put(RESURRECT_SUPERIOR_SKELETON, new ThrallSpell(80, THRALL_SKELETON_NPC, THRALL_SKELETON_GFX, 2, thrallRunes));
        THRALL_SPELLS.put(RESURRECT_SUPERIOR_ZOMBIE, new ThrallSpell(84, THRALL_ZOMBIE_NPC, THRALL_ZOMBIE_GFX, 2, thrallRunes));
        THRALL_SPELLS.put(RESURRECT_GREATER_GHOST, new ThrallSpell(88, THRALL_GHOST_NPC, THRALL_GHOST_GFX, 3, thrallRunes));
        THRALL_SPELLS.put(RESURRECT_GREATER_SKELETON, new ThrallSpell(92, THRALL_SKELETON_NPC, THRALL_SKELETON_GFX, 3, thrallRunes));
        THRALL_SPELLS.put(RESURRECT_GREATER_ZOMBIE, new ThrallSpell(96, THRALL_ZOMBIE_NPC, THRALL_ZOMBIE_GFX, 3, thrallRunes));

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
            player.performAnimation(new Animation(ARCEUUS_TELEPORT_ANIM));
            player.performGraphic(new Graphic(ARCEUUS_TELEPORT_GFX, GraphicHeight.HIGH));
            TeleportHandler.teleport(player, teleport.location(), TeleportType.ARCEUUS, true);
            player.getSkillManager().addExperience(Skill.MAGIC, teleport.experience());
            player.getClickDelay().reset();
            return true;
        }

        ThrallSpell thrallSpell = THRALL_SPELLS.get(button);
        if (thrallSpell != null) {
            return summonThrall(player, thrallSpell);
        }
        return false;
    }

    public static boolean handleMagicOnItem(Player player, int spellId, Item item) {
        if (player.getSpellbook() != MagicSpellbook.ARCEUUS) {
            return false;
        }
        ReanimationSpell spell = REANIMATION_SPELLS.get(spellId);
        if (spell == null) {
            return false;
        }
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
        player.performAnimation(new Animation(REANIMATION_ANIM));
        player.performGraphic(new Graphic(REANIMATION_GFX, GraphicHeight.HIGH));
        player.getInventory().delete(item.getId(), 1);
        spawnReanimatedNpc(player, npcId);
        player.getSkillManager().addExperience(Skill.MAGIC, spell.experience());
        player.getClickDelay().reset();
        return true;
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

        player.performAnimation(new Animation(THRALL_ANIM));
        player.performGraphic(new Graphic(spell.gfxId(), GraphicHeight.HIGH));

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
}
