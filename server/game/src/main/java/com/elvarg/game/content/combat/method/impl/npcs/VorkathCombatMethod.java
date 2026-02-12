package com.elvarg.game.content.combat.method.impl.npcs;

import com.elvarg.game.World;
import com.elvarg.game.collision.RegionManager;
import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.HitDamage;
import com.elvarg.game.content.combat.hit.HitMask;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.npc.impl.Vorkath;
import com.elvarg.game.entity.impl.npc.impl.ZombifiedSpawn;
import com.elvarg.game.entity.impl.object.GameObject;
import com.elvarg.game.entity.impl.object.ObjectManager;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.Projectile;
import com.elvarg.game.model.Skill;
import com.elvarg.game.model.areas.impl.PrivateArea;
import com.elvarg.game.model.areas.impl.VorkathArea;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;
import com.elvarg.game.task.impl.CombatPoisonEffect.PoisonType;
import com.elvarg.util.Misc;
import com.elvarg.util.timers.TimerKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Vorkath combat loop:
 * 6 regular attacks then alternating special attacks (acid then zombified spawn).
 */
public class VorkathCombatMethod extends CombatMethod {

    private static final int ZOMBIFIED_SPAWN_ID = 8062;
    private static final int ACID_POOL_OBJECT_ID = 30032;

    private static final int BASIC_ATTACKS_PER_SPECIAL = 6;
    private static final int ACID_PHASE_TICKS = 28;
    private static final int SPAWN_PHASE_TICKS = 20;
    private static final int ACID_POOL_COUNT = 24;

    private static final int VORKATH_MELEE_ANIMATION = 7951;
    private static final int VORKATH_ATTACK_ANIMATION = 7952;
    private static final int VORKATH_FIRE_BOMB_OR_SPAWN_ANIMATION = 7960;
    private static final int VORKATH_ACID_ANIMATION = 7957;

    private static final Projectile DRAGON_BREATH_PROJECTILE = new Projectile(393, 42, 20, 25, 48);
    private static final Projectile MAGIC_PROJECTILE = new Projectile(395, 42, 20, 25, 48);
    private static final Projectile RANGED_PROJECTILE = new Projectile(394, 42, 20, 25, 48);
    private static final Projectile ACID_SPIT_PROJECTILE = new Projectile(1045, 45, 0, 20, 65);
    private static final Projectile FIREBALL_PROJECTILE = new Projectile(396, 35, 20, 25, 40);

    private static final PendingHit[] NO_HITS = new PendingHit[0];

    private enum SpecialPhase {
        NONE,
        ACID,
        SPAWN
    }

    private enum AttackStyle {
        MELEE,
        RANGED,
        MAGIC,
        DRAGONFIRE
    }

    private SpecialPhase specialPhase = SpecialPhase.NONE;
    private AttackStyle currentAttackStyle = AttackStyle.MAGIC;
    private CombatType currentAttackType = CombatType.MAGIC;
    private int currentHitDelay = 2;
    private boolean skipHit;

    private int basicAttacks;
    private boolean nextSpecialAcid = true;
    private int specialTicksRemaining;
    private final List<GameObject> acidPools = new ArrayList<>();
    private final Set<Location> freshAcidPoolTiles = new HashSet<>();
    private NPC zombifiedSpawn;
    private Player specialTarget;

    @Override
    public void onCombatEnded(Mobile character, Mobile target) {
        clearSpecialState();
    }

    @Override
    public boolean canAttack(Mobile character, Mobile target) {
        if (!character.isNpc() || !target.isPlayer()) {
            return false;
        }

        NPC npc = character.getAsNpc();
        if (npc instanceof Vorkath vorkath && vorkath.isAsleep()) {
            return false;
        }
        Player player = target.getAsPlayer();
        return npc.getOwner() == null || npc.getOwner() == player;
    }

    @Override
    public void onTick(NPC npc, Mobile target) {
        if (!(npc.getPrivateArea() instanceof VorkathArea)) {
            clearSpecialState();
            return;
        }

        if (specialPhase == SpecialPhase.ACID) {
            processAcidPoolsDamage(npc.getPrivateArea());
            freshAcidPoolTiles.clear();
            if (--specialTicksRemaining <= 0) {
                endAcidPhase();
            }
            return;
        }

        if (specialPhase == SpecialPhase.SPAWN) {
            if (zombifiedSpawn == null || !zombifiedSpawn.isRegistered() || zombifiedSpawn.getHitpoints() <= 0) {
                endSpawnPhase();
                return;
            }

            if (specialTarget == null || !specialTarget.isRegistered()
                    || specialTarget.getPrivateArea() != npc.getPrivateArea()) {
                endSpawnPhase();
                return;
            }

            if (zombifiedSpawn.getLocation().getDistance(specialTarget.getLocation()) <= 1) {
                specialTarget.getCombat().getHitQueue().addPendingDamage(
                        new HitDamage(Misc.inclusive(40, 58), HitMask.RED));
                World.getRemoveNPCQueue().add(zombifiedSpawn);
                endSpawnPhase();
                return;
            }

            if (--specialTicksRemaining <= 0) {
                World.getRemoveNPCQueue().add(zombifiedSpawn);
                endSpawnPhase();
            }
        }
    }

    @Override
    public void start(Mobile character, Mobile target) {
        skipHit = false;

        if (!character.isNpc() || !target.isPlayer()) {
            skipHit = true;
            return;
        }

        NPC npc = character.getAsNpc();
        Player player = target.getAsPlayer();

        if (specialPhase == SpecialPhase.SPAWN) {
            skipHit = true;
            return;
        }

        if (specialPhase == SpecialPhase.ACID) {
            spawnAcidUnderPlayer(npc, player);
            skipHit = true;
            return;
        }

        if (basicAttacks >= BASIC_ATTACKS_PER_SPECIAL) {
            triggerSpecial(npc, player);
            skipHit = true;
            return;
        }

        basicAttacks++;
        performBasicAttack(npc, player);
    }

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        if (skipHit) {
            return NO_HITS;
        }
        return new PendingHit[]{new PendingHit(character, target, this, currentHitDelay)};
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (!hit.isAccurate() || !hit.getTarget().isPlayer()) {
            return;
        }

        Player target = hit.getTarget().getAsPlayer();

        if (currentAttackStyle == AttackStyle.DRAGONFIRE && Misc.getRandom(99) < 25) {
            CombatFactory.poisonEntity(target, PoisonType.VENOM);
        }

        if (currentAttackStyle == AttackStyle.MAGIC && Misc.getRandom(99) < 12) {
            int currentPrayer = target.getSkillManager().getCurrentLevel(Skill.PRAYER);
            int reduced = Math.max(0, currentPrayer - 3);
            target.getSkillManager().setCurrentLevel(Skill.PRAYER, reduced, true);
            if (reduced == 0) {
                com.elvarg.game.content.PrayerHandler.deactivatePrayers(target);
            }
            target.getPacketSender().sendMessage("Vorkath's attack drains your prayer.");
        }
    }

    @Override
    public int attackSpeed(Mobile character) {
        return specialPhase == SpecialPhase.ACID ? 1 : 4;
    }

    @Override
    public int attackDistance(Mobile character) {
        return 32;
    }

    @Override
    public CombatType type() {
        return currentAttackType;
    }

    @Override
    public void onDeath(NPC npc, Optional<Player> killer) {
        clearSpecialState();
        if (npc instanceof Vorkath vorkath) {
            vorkath.queueSleepingRespawn();
        }
    }

    private void performBasicAttack(NPC npc, Player target) {
        int distance = npc.getLocation().getDistance(target.getLocation());
        if (distance <= 1 && Misc.getRandom(99) < 35) {
            currentAttackStyle = AttackStyle.MELEE;
            currentAttackType = CombatType.MELEE;
            currentHitDelay = 1;
            npc.performAnimation(new Animation(VORKATH_MELEE_ANIMATION));
            return;
        }

        int roll = Misc.getRandom(99);
        if (roll < 25) {
            currentAttackStyle = AttackStyle.DRAGONFIRE;
            currentAttackType = CombatType.MAGIC;
            currentHitDelay = 2;
            npc.performAnimation(new Animation(VORKATH_ATTACK_ANIMATION));
            Projectile.sendProjectile(npc, target, DRAGON_BREATH_PROJECTILE);
        } else if (roll < 60) {
            currentAttackStyle = AttackStyle.MAGIC;
            currentAttackType = CombatType.MAGIC;
            currentHitDelay = 2;
            npc.performAnimation(new Animation(VORKATH_ATTACK_ANIMATION));
            Projectile.sendProjectile(npc, target, MAGIC_PROJECTILE);
        } else {
            currentAttackStyle = AttackStyle.RANGED;
            currentAttackType = CombatType.RANGED;
            currentHitDelay = 2;
            npc.performAnimation(new Animation(VORKATH_ATTACK_ANIMATION));
            Projectile.sendProjectile(npc, target, RANGED_PROJECTILE);
        }
    }

    private void triggerSpecial(NPC npc, Player target) {
        basicAttacks = 0;
        specialTarget = target;

        if (nextSpecialAcid) {
            startAcidPhase(npc, target);
        } else {
            startSpawnPhase(npc, target);
        }
        nextSpecialAcid = !nextSpecialAcid;
    }

    private void startAcidPhase(NPC npc, Player target) {
        specialPhase = SpecialPhase.ACID;
        specialTicksRemaining = ACID_PHASE_TICKS;
        npc.performAnimation(new Animation(VORKATH_ACID_ANIMATION));
        spawnAcidPools(npc, target);
        target.getPacketSender().sendMessage("Vorkath spits acid across the arena.");
    }

    private void endAcidPhase() {
        specialPhase = SpecialPhase.NONE;
        specialTicksRemaining = 0;
        despawnAcidPools();
    }

    private void startSpawnPhase(NPC npc, Player target) {
        specialPhase = SpecialPhase.SPAWN;
        specialTicksRemaining = SPAWN_PHASE_TICKS;
        npc.performAnimation(new Animation(VORKATH_FIRE_BOMB_OR_SPAWN_ANIMATION));
        CombatFactory.freeze(target, 16);
        Projectile.sendProjectile(npc, target, FIREBALL_PROJECTILE);
        target.getPacketSender().sendMessage("Vorkath summons a zombified spawn!");

        TaskManager.submit(new Task(2, npc, false) {
            @Override
            protected void execute() {
                if (!npc.isRegistered() || specialPhase != SpecialPhase.SPAWN
                        || !(npc.getPrivateArea() instanceof VorkathArea area)
                        || target.getPrivateArea() != area) {
                    stop();
                    return;
                }

                Location spawnLocation = findSpawnLocation(npc, target);
                if (spawnLocation != null) {
                    ZombifiedSpawn spawn = new ZombifiedSpawn(target, area, ZOMBIFIED_SPAWN_ID, spawnLocation);
                    zombifiedSpawn = spawn;
                    World.getAddNPCQueue().add(spawn);
                    spawn.getCombat().attack(target);
                }
                stop();
            }
        });
    }

    private void endSpawnPhase() {
        specialPhase = SpecialPhase.NONE;
        specialTicksRemaining = 0;
        if (specialTarget != null && specialTarget.isRegistered()) {
            specialTarget.getTimers().cancel(TimerKey.FREEZE);
        }
        if (zombifiedSpawn != null && zombifiedSpawn.isRegistered()) {
            World.getRemoveNPCQueue().add(zombifiedSpawn);
        }
        zombifiedSpawn = null;
    }

    private void spawnAcidUnderPlayer(NPC npc, Player target) {
        if (specialPhase != SpecialPhase.ACID) {
            return;
        }
        PrivateArea area = npc.getPrivateArea();
        if (area == null) {
            return;
        }

        npc.performAnimation(new Animation(VORKATH_ACID_ANIMATION));
        Location tile = target.getLocation().clone();
        if (RegionManager.blocked(tile, area) || containsAcidPool(tile)) {
            return;
        }

        Projectile.sendProjectile(npc, tile, ACID_SPIT_PROJECTILE);
        GameObject pool = new GameObject(ACID_POOL_OBJECT_ID, tile, 10, 0, area);
        ObjectManager.register(pool, true);
        acidPools.add(pool);
        freshAcidPoolTiles.add(tile);
    }

    private void spawnAcidPools(NPC npc, Player target) {
        despawnAcidPools();

        Location center = npc.getSpawnPosition();
        PrivateArea area = npc.getPrivateArea();
        int spawned = 0;

        for (int tries = 0; tries < 300 && spawned < ACID_POOL_COUNT; tries++) {
            int x = center.getX() - 8 + Misc.getRandom(16);
            int y = center.getY() - 8 + Misc.getRandom(16);
            Location tile = new Location(x, y, center.getZ());

            if (tile.equals(target.getLocation())) {
                continue;
            }
            if (tile.getDistance(center) > 10) {
                continue;
            }
            if (RegionManager.blocked(tile, area)) {
                continue;
            }
            if (containsAcidPool(tile)) {
                continue;
            }

            Projectile.sendProjectile(npc, tile, ACID_SPIT_PROJECTILE);
            GameObject pool = new GameObject(ACID_POOL_OBJECT_ID, tile, 10, 0, area);
            ObjectManager.register(pool, true);
            acidPools.add(pool);
            freshAcidPoolTiles.add(tile);
            spawned++;
        }
    }

    private boolean containsAcidPool(Location tile) {
        for (GameObject pool : acidPools) {
            if (pool.getLocation().equals(tile)) {
                return true;
            }
        }
        return false;
    }

    private void processAcidPoolsDamage(PrivateArea area) {
        if (acidPools.isEmpty()) {
            return;
        }

        for (Player player : World.getPlayers()) {
            if (player == null || !player.isRegistered() || player.getPrivateArea() != area) {
                continue;
            }

            for (GameObject pool : acidPools) {
                if (pool.getLocation().equals(player.getLocation())) {
                    // Grace window: newly placed acid does not damage on its first cycle.
                    if (freshAcidPoolTiles.contains(pool.getLocation())) {
                        break;
                    }
                    player.getCombat().getHitQueue().addPendingDamage(
                            new HitDamage(Misc.inclusive(2, 6), HitMask.GREEN));
                    if (Misc.getRandom(99) < 25) {
                        CombatFactory.poisonEntity(player, PoisonType.VENOM);
                    }
                    break;
                }
            }
        }
    }

    private void despawnAcidPools() {
        for (GameObject pool : new ArrayList<>(acidPools)) {
            ObjectManager.deregister(pool, true);
        }
        acidPools.clear();
        freshAcidPoolTiles.clear();
    }

    private Location findSpawnLocation(NPC npc, Player target) {
        Location origin = target.getLocation();
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) {
                        continue;
                    }
                    Location tile = origin.transform(dx, dy);
                    if (tile.getDistance(npc.getSpawnPosition()) > 12) {
                        continue;
                    }
                    if (RegionManager.blocked(tile, npc.getPrivateArea())) {
                        continue;
                    }
                    return tile;
                }
            }
        }
        return null;
    }

    private void clearSpecialState() {
        despawnAcidPools();
        if (zombifiedSpawn != null && zombifiedSpawn.isRegistered()) {
            World.getRemoveNPCQueue().add(zombifiedSpawn);
        }
        if (specialTarget != null && specialTarget.isRegistered()) {
            specialTarget.getTimers().cancel(TimerKey.FREEZE);
        }
        zombifiedSpawn = null;
        specialTarget = null;
        specialPhase = SpecialPhase.NONE;
        specialTicksRemaining = 0;
        basicAttacks = 0;
    }
}
