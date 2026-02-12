package com.elvarg.game.content.combat.method.impl.npcs;

import com.elvarg.game.World;
import com.elvarg.game.collision.RegionManager;
import com.elvarg.game.content.PrayerHandler;
import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.CombatType;
import com.elvarg.game.content.combat.hit.HitDamage;
import com.elvarg.game.content.combat.hit.HitMask;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.npc.impl.ZulrahSnakeling;
import com.elvarg.game.entity.impl.object.GameObject;
import com.elvarg.game.entity.impl.object.MapObjects;
import com.elvarg.game.entity.impl.object.ObjectManager;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.Projectile;
import com.elvarg.game.model.areas.impl.PrivateArea;
import com.elvarg.game.model.areas.impl.ZulrahArea;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;
import com.elvarg.game.task.impl.CombatPoisonEffect.PoisonType;
import com.elvarg.util.Misc;
import com.elvarg.util.NpcIdentifiers;
import com.elvarg.util.ObjectIdentifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles Zulrah's phase-based combat.
 */
public class ZulrahCombatMethod extends CombatMethod {

    private static final int TOXIC_CLOUD_OBJECT_ID = 11700;
    private static final int CLOUD_LIFETIME_TICKS = 25;
    private static final int MAX_SNAKELINGS = 3;
    private static final int MAGMA_ATTACKS_PER_PHASE = 2;
    private static final int DEFAULT_ATTACKS_PER_PHASE = 8;
    private static final int JAD_ATTACKS_PER_PHASE = 12;
    private static final int MAGMA_WINDUP_TICKS = 4;
    private static final PendingHit[] NO_HITS = new PendingHit[0];

    private static final Animation SUBMERGE_ANIMATION = new Animation(5072);
    private static final Animation EMERGE_ANIMATION = new Animation(5073);
    private static final Animation ATTACK_ANIMATION = new Animation(5069);
    private static final Animation MAGMA_ATTACK_ANIMATION = new Animation(5806);

    private static final Projectile RANGED_PROJECTILE = new Projectile(1044, 65, 20, 20, 85);
    private static final Projectile MAGIC_PROJECTILE = new Projectile(1046, 65, 20, 20, 85);
    private static final Projectile FUMES_PROJECTILE = new Projectile(1045, 65, 0, 20, 90);
    private static final Projectile SNAKELING_PROJECTILE = new Projectile(1047, 65, 0, 20, 90);

    private static final int[][] FUME_OFFSETS = {
            {-4, 3}, {-4, 0}, {-4, -3},
            {-1, -4}, {2, -4}, {5, -4},
            {6, -1}, {6, 2}, {6, 5}
    };

    private enum Form {
        NORMAL(NpcIdentifiers.ZULRAH),
        MAGMA(NpcIdentifiers.ZULRAH_2),
        TANZANITE(NpcIdentifiers.ZULRAH_3),
        JAD(NpcIdentifiers.ZULRAH);

        private final int npcId;

        Form(int npcId) {
            this.npcId = npcId;
        }
    }

    private enum PhasePosition {
        CENTER(0, 0),
        SOUTH(0, -9),
        EAST(10, -1),
        WEST(-10, -1);

        private final int xOffset;
        private final int yOffset;

        PhasePosition(int xOffset, int yOffset) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
        }
    }

    private static final class Phase {
        private final Form form;
        private final PhasePosition position;
        private final boolean[] safeFumeSpots = new boolean[9];

        private Phase(Form form, PhasePosition position) {
            this.form = form;
            this.position = position;
        }

        private Phase westSafe() {
            safeFumeSpots[0] = true;
            safeFumeSpots[1] = true;
            safeFumeSpots[2] = true;
            return this;
        }

        private Phase centerSafe() {
            safeFumeSpots[3] = true;
            safeFumeSpots[4] = true;
            safeFumeSpots[5] = true;
            return this;
        }

        private Phase eastSafe() {
            safeFumeSpots[6] = true;
            safeFumeSpots[7] = true;
            safeFumeSpots[8] = true;
            return this;
        }

        private Phase setSafe(int index) {
            safeFumeSpots[index] = true;
            return this;
        }
    }

    private static final Phase[][] ROTATIONS = {
            {
                    new Phase(Form.NORMAL, PhasePosition.CENTER).setSafe(8),
                    new Phase(Form.MAGMA, PhasePosition.CENTER).setSafe(8),
                    new Phase(Form.TANZANITE, PhasePosition.CENTER).setSafe(8),
                    new Phase(Form.NORMAL, PhasePosition.SOUTH).westSafe(),
                    new Phase(Form.MAGMA, PhasePosition.CENTER).setSafe(1),
                    new Phase(Form.TANZANITE, PhasePosition.WEST).setSafe(1),
                    new Phase(Form.NORMAL, PhasePosition.SOUTH).eastSafe(),
                    new Phase(Form.TANZANITE, PhasePosition.SOUTH).westSafe(),
                    new Phase(Form.JAD, PhasePosition.WEST).setSafe(0),
                    new Phase(Form.MAGMA, PhasePosition.CENTER).setSafe(0).setSafe(1),
            },
            {
                    new Phase(Form.NORMAL, PhasePosition.CENTER).setSafe(8),
                    new Phase(Form.MAGMA, PhasePosition.CENTER).setSafe(8),
                    new Phase(Form.TANZANITE, PhasePosition.CENTER).setSafe(8),
                    new Phase(Form.NORMAL, PhasePosition.WEST).westSafe(),
                    new Phase(Form.TANZANITE, PhasePosition.SOUTH).setSafe(1),
                    new Phase(Form.MAGMA, PhasePosition.CENTER).setSafe(1),
                    new Phase(Form.NORMAL, PhasePosition.EAST).setSafe(4),
                    new Phase(Form.TANZANITE, PhasePosition.SOUTH).westSafe(),
                    new Phase(Form.JAD, PhasePosition.WEST).setSafe(0),
                    new Phase(Form.MAGMA, PhasePosition.CENTER).setSafe(0).setSafe(1),
            },
            {
                    new Phase(Form.NORMAL, PhasePosition.CENTER).setSafe(8),
                    new Phase(Form.NORMAL, PhasePosition.EAST).setSafe(8),
                    new Phase(Form.MAGMA, PhasePosition.CENTER).eastSafe(),
                    new Phase(Form.TANZANITE, PhasePosition.WEST).centerSafe(),
                    new Phase(Form.NORMAL, PhasePosition.SOUTH).centerSafe(),
                    new Phase(Form.TANZANITE, PhasePosition.EAST).setSafe(4).setSafe(5),
                    new Phase(Form.NORMAL, PhasePosition.CENTER).westSafe(),
                    new Phase(Form.NORMAL, PhasePosition.WEST).westSafe(),
                    new Phase(Form.TANZANITE, PhasePosition.CENTER).eastSafe(),
                    new Phase(Form.JAD, PhasePosition.EAST).eastSafe(),
                    new Phase(Form.TANZANITE, PhasePosition.CENTER).eastSafe(),
            },
            {
                    new Phase(Form.NORMAL, PhasePosition.CENTER).setSafe(8),
                    new Phase(Form.TANZANITE, PhasePosition.EAST).setSafe(8),
                    new Phase(Form.NORMAL, PhasePosition.SOUTH).setSafe(1),
                    new Phase(Form.TANZANITE, PhasePosition.WEST).setSafe(1),
                    new Phase(Form.MAGMA, PhasePosition.CENTER).eastSafe(),
                    new Phase(Form.NORMAL, PhasePosition.EAST).eastSafe(),
                    new Phase(Form.NORMAL, PhasePosition.SOUTH).centerSafe(),
                    new Phase(Form.TANZANITE, PhasePosition.WEST).westSafe(),
                    new Phase(Form.NORMAL, PhasePosition.CENTER).eastSafe(),
                    new Phase(Form.TANZANITE, PhasePosition.CENTER).eastSafe(),
                    new Phase(Form.JAD, PhasePosition.EAST).eastSafe(),
                    new Phase(Form.TANZANITE, PhasePosition.CENTER).eastSafe(),
            }
    };

    private static final boolean[] JAD_RANGE_FIRST = {true, true, false, false};

    private final NPC[] snakelings = new NPC[MAX_SNAKELINGS];
    private final List<GameObject> activeClouds = new ArrayList<>();

    private Phase[] rotation;
    private int rotationId = -1;
    private int phaseIndex;
    private int attacksInPhase;
    private boolean transitioning;
    private int transitionTicks;
    private boolean jadUseRange;

    private CombatType currentAttackType = CombatType.RANGED;
    private int currentHitDelay = 2;
    private boolean skipHit;

    @Override
    public void onCombatBegan(Mobile character, Mobile target) {
        if (!character.isNpc()) {
            return;
        }
        initializeIfNeeded(character.getAsNpc());
    }

    @Override
    public void onTick(NPC npc, Mobile target) {
        initializeIfNeeded(npc);
        clearExpiredSnakelings();

        if (transitioning && --transitionTicks <= 0) {
            finishTransition(npc);
        }
    }

    @Override
    public boolean canAttack(Mobile character, Mobile target) {
        if (!character.isNpc() || target == null || !target.isRegistered()) {
            return false;
        }

        NPC npc = character.getAsNpc();
        if (transitioning) {
            return false;
        }

        if (npc.getOwner() != null && target.isPlayer() && npc.getOwner() != target.getAsPlayer()) {
            return false;
        }

        return true;
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
        initializeIfNeeded(npc);

        if (transitioning) {
            skipHit = true;
            return;
        }

        if (attacksInPhase >= getMaxAttacksForCurrentPhase()) {
            beginTransition(npc);
            skipHit = true;
            return;
        }

        attacksInPhase++;

        // Hazard pressure that makes positioning matter during phases.
        if (getCurrentPhase().form != Form.JAD) {
            if (Misc.getRandom(99) < 20) {
                attemptSpawnSnakeling(npc, player);
            }
            if (Misc.getRandom(99) < 35) {
                attemptSpawnClouds(npc, player);
            }
        }

        switch (getCurrentPhase().form) {
            case NORMAL -> performRangedAttack(npc, target);
            case TANZANITE -> {
                if (Misc.getRandom(1) == 0) {
                    performRangedAttack(npc, target);
                } else {
                    performMagicAttack(npc, target);
                }
            }
            case JAD -> {
                if (jadUseRange) {
                    performRangedAttack(npc, target);
                } else {
                    performMagicAttack(npc, target);
                }
                jadUseRange = !jadUseRange;
            }
            case MAGMA -> {
                performMagmaLunge(npc, player);
                skipHit = true;
            }
        }
    }

    @Override
    public PendingHit[] hits(Mobile character, Mobile target) {
        if (skipHit) {
            // Important: return empty hits (not null) so Combat still applies attack delay.
            return NO_HITS;
        }
        return new PendingHit[]{new PendingHit(character, target, this, currentHitDelay)};
    }

    @Override
    public void handleAfterHitEffects(PendingHit hit) {
        if (hit.getTarget() != null && hit.getTarget().isPlayer() && hit.isAccurate() && Misc.getRandom(99) < 20) {
            CombatFactory.poisonEntity(hit.getTarget(), PoisonType.VENOM);
        }
    }

    @Override
    public int attackSpeed(Mobile character) {
        return 4;
    }

    @Override
    public int attackDistance(Mobile character) {
        if (transitioning) {
            return 1;
        }
        return 32;
    }

    @Override
    public CombatType type() {
        return currentAttackType;
    }

    @Override
    public void onDeath(NPC npc, Optional<Player> killer) {
        spawnExitObject(npc);
        despawnClouds();
        despawnSnakelings();
    }

    private void initializeIfNeeded(NPC npc) {
        if (rotation != null) {
            return;
        }
        rotationId = Misc.getRandom(ROTATIONS.length - 1);
        rotation = ROTATIONS[rotationId];
        phaseIndex = 0;
        attacksInPhase = 0;
        transitioning = false;
        transitionTicks = 0;
        jadUseRange = false;
        currentAttackType = CombatType.RANGED;
    }

    private Phase getCurrentPhase() {
        return rotation[phaseIndex % rotation.length];
    }

    private int getMaxAttacksForCurrentPhase() {
        return switch (getCurrentPhase().form) {
            case MAGMA -> MAGMA_ATTACKS_PER_PHASE;
            case JAD -> JAD_ATTACKS_PER_PHASE;
            default -> DEFAULT_ATTACKS_PER_PHASE;
        };
    }

    private void beginTransition(NPC npc) {
        transitioning = true;
        transitionTicks = 4;
        attacksInPhase = 0;
        npc.performAnimation(SUBMERGE_ANIMATION);
    }

    private void finishTransition(NPC npc) {
        transitioning = false;
        phaseIndex = (phaseIndex + 1) % rotation.length;

        Phase phase = getCurrentPhase();
        Location next = npc.getSpawnPosition().clone().add(phase.position.xOffset, phase.position.yOffset);
        npc.moveTo(next);
        transformForForm(npc, phase.form);
        npc.performAnimation(EMERGE_ANIMATION);

        if (phase.form == Form.JAD) {
            jadUseRange = JAD_RANGE_FIRST[rotationId];
        }
    }

    private void transformForForm(NPC npc, Form form) {
        if (form.npcId == npc.getRealId()) {
            npc.setNpcTransformationId(-1);
        } else {
            npc.setNpcTransformationId(form.npcId);
        }
    }

    private void performRangedAttack(NPC npc, Mobile target) {
        currentAttackType = CombatType.RANGED;
        currentHitDelay = 2;
        npc.performAnimation(ATTACK_ANIMATION);
        Projectile.sendProjectile(npc, target, RANGED_PROJECTILE);
    }

    private void performMagicAttack(NPC npc, Mobile target) {
        currentAttackType = CombatType.MAGIC;
        currentHitDelay = 2;
        npc.performAnimation(ATTACK_ANIMATION);
        Projectile.sendProjectile(npc, target, MAGIC_PROJECTILE);
    }

    private void performMagmaLunge(NPC npc, Player target) {
        currentAttackType = CombatType.MELEE;
        npc.performAnimation(MAGMA_ATTACK_ANIMATION);

        final Location hitTile = target.getLocation().clone();
        TaskManager.submit(new Task(MAGMA_WINDUP_TICKS, npc, false) {
            @Override
            protected void execute() {
                if (!npc.isRegistered() || !target.isRegistered() || target.getPrivateArea() != npc.getPrivateArea()) {
                    stop();
                    return;
                }

                // OSRS-style magma melee is a delayed tile strike: stepping off the called tile avoids it.
                if (target.getLocation().equals(hitTile)) {
                    int damage = Misc.inclusive(18, 41);
                    if (target.getPrayerActive()[PrayerHandler.PROTECT_FROM_MELEE]) {
                        damage = 0;
                    }
                    target.getCombat().getHitQueue().addPendingDamage(new HitDamage(damage, damage > 0 ? HitMask.RED : HitMask.BLUE));
                    if (damage > 0 && Misc.getRandom(99) < 35) {
                        CombatFactory.poisonEntity(target, PoisonType.VENOM);
                    }
                }
                stop();
            }
        });
    }

    private void attemptSpawnSnakeling(NPC npc, Player target) {
        int freeSlot = -1;
        for (int i = 0; i < snakelings.length; i++) {
            NPC snakeling = snakelings[i];
            if (snakeling == null || !snakeling.isRegistered() || snakeling.getHitpoints() <= 0) {
                freeSlot = i;
                break;
            }
        }

        if (freeSlot == -1) {
            return;
        }

        Location spawn = getSnakelingSpawn(npc, target);
        if (spawn == null) {
            return;
        }

        final int slot = freeSlot;
        Projectile.sendProjectile(npc, spawn, SNAKELING_PROJECTILE);
        TaskManager.submit(new Task(2, npc, false) {
            @Override
            protected void execute() {
                if (!npc.isRegistered() || !target.isRegistered()) {
                    stop();
                    return;
                }
                if (!(npc.getPrivateArea() instanceof ZulrahArea area) || area.isDestroyed()) {
                    stop();
                    return;
                }
                ZulrahSnakeling snakeling = new ZulrahSnakeling(target, area, NpcIdentifiers.SNAKELING, spawn.clone());
                snakelings[slot] = snakeling;
                World.getAddNPCQueue().add(snakeling);
                stop();
            }
        });
    }

    private Location getSnakelingSpawn(NPC npc, Player target) {
        List<Location> possible = new ArrayList<>();
        Location origin = target.getLocation();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                Location tile = origin.transform(x, y);
                if (tile.equals(origin)) {
                    continue;
                }
                if (tile.getDistance(npc.getSpawnPosition()) > 14) {
                    continue;
                }
                if (RegionManager.blocked(tile, npc.getPrivateArea())) {
                    continue;
                }
                possible.add(tile);
            }
        }
        if (possible.isEmpty()) {
            return null;
        }
        return possible.get(Misc.getRandom(possible.size() - 1));
    }

    private void attemptSpawnClouds(NPC npc, Player target) {
        Phase phase = getCurrentPhase();
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < phase.safeFumeSpots.length; i++) {
            if (phase.safeFumeSpots[i]) {
                continue;
            }
            if (!cloudSpotFree(npc, i)) {
                continue;
            }
            candidates.add(i);
        }

        if (candidates.isEmpty()) {
            return;
        }

        int spot = candidates.get(Misc.getRandom(candidates.size() - 1));
        spawnCloud(npc, target, phaseIndex, spot);

        if (candidates.contains(spot + 1)) {
            spawnCloud(npc, target, phaseIndex, spot + 1);
        }
    }

    private boolean cloudSpotFree(NPC npc, int spot) {
        Location cloudLocation = npc.getSpawnPosition().clone().add(FUME_OFFSETS[spot][0], FUME_OFFSETS[spot][1]);
        return MapObjects.get(TOXIC_CLOUD_OBJECT_ID, cloudLocation, npc.getPrivateArea()) == null;
    }

    private void spawnCloud(NPC npc, Player target, int expectedPhaseIndex, int spot) {
        Location cloudLocation = npc.getSpawnPosition().clone().add(FUME_OFFSETS[spot][0], FUME_OFFSETS[spot][1]);
        Projectile.sendProjectile(npc, cloudLocation, FUMES_PROJECTILE);

        TaskManager.submit(new Task(2, npc, false) {
            @Override
            protected void execute() {
                if (!npc.isRegistered() || target.getPrivateArea() != npc.getPrivateArea()) {
                    stop();
                    return;
                }

                GameObject cloud = new GameObject(TOXIC_CLOUD_OBJECT_ID, cloudLocation, 10, 0, npc.getPrivateArea());
                ObjectManager.register(cloud, true);
                activeClouds.add(cloud);
                startCloudDamageTask(npc, cloud, expectedPhaseIndex);
                stop();
            }
        });
    }

    private void startCloudDamageTask(NPC npc, GameObject cloud, int expectedPhaseIndex) {
        TaskManager.submit(new Task(1, npc, true) {
            int ticks;

            @Override
            protected void execute() {
                if (!npc.isRegistered() || cloud.getPrivateArea() != npc.getPrivateArea() || expectedPhaseIndex != phaseIndex) {
                    despawnCloud(cloud);
                    stop();
                    return;
                }

                damagePlayersInCloud(cloud.getLocation(), npc.getPrivateArea());

                ticks++;
                if (ticks >= CLOUD_LIFETIME_TICKS) {
                    despawnCloud(cloud);
                    stop();
                }
            }
        });
    }

    private void damagePlayersInCloud(Location cloudLocation, PrivateArea area) {
        for (Player player : World.getPlayers()) {
            if (player == null || !player.isRegistered() || player.getPrivateArea() != area) {
                continue;
            }
            if (!isInsideCloud(player.getLocation(), cloudLocation)) {
                continue;
            }

            player.getCombat().getHitQueue().addPendingDamage(new HitDamage(Misc.inclusive(1, 4), HitMask.GREEN));
            if (Misc.getRandom(99) < 30) {
                CombatFactory.poisonEntity(player, PoisonType.VENOM);
            }
        }
    }

    private boolean isInsideCloud(Location position, Location cloudBase) {
        return position.getX() >= cloudBase.getX()
                && position.getX() <= cloudBase.getX() + 2
                && position.getY() >= cloudBase.getY()
                && position.getY() <= cloudBase.getY() + 2
                && position.getZ() == cloudBase.getZ();
    }

    private void despawnCloud(GameObject cloud) {
        if (cloud == null) {
            return;
        }
        ObjectManager.deregister(cloud, true);
        activeClouds.remove(cloud);
    }

    private void despawnClouds() {
        List<GameObject> clouds = new ArrayList<>(activeClouds);
        for (GameObject cloud : clouds) {
            despawnCloud(cloud);
        }
    }

    private void clearExpiredSnakelings() {
        for (int i = 0; i < snakelings.length; i++) {
            NPC snakeling = snakelings[i];
            if (snakeling == null) {
                continue;
            }
            if (!snakeling.isRegistered() || snakeling.getHitpoints() <= 0) {
                snakelings[i] = null;
            }
        }
    }

    private void despawnSnakelings() {
        for (int i = 0; i < snakelings.length; i++) {
            NPC snakeling = snakelings[i];
            if (snakeling != null && snakeling.isRegistered()) {
                World.getRemoveNPCQueue().add(snakeling);
            }
            snakelings[i] = null;
        }
    }

    private void spawnExitObject(NPC npc) {
        if (!(npc.getPrivateArea() instanceof ZulrahArea area)) {
            return;
        }

        if (MapObjects.get(ObjectIdentifiers.ZUL_ANDRA_TELEPORT, ZulrahArea.EXIT_OBJECT_LOCATION, area) != null) {
            return;
        }

        GameObject exitObject = new GameObject(
                ObjectIdentifiers.ZUL_ANDRA_TELEPORT,
                ZulrahArea.EXIT_OBJECT_LOCATION.clone(),
                10,
                0,
                area
        );
        ObjectManager.register(exitObject, true);
    }
}
