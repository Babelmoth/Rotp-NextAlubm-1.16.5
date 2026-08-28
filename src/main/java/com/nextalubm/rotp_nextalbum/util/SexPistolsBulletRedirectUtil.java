package com.nextalubm.rotp_nextalbum.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.NextAlbumConfig;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class SexPistolsBulletRedirectUtil {

    public static final double TARGET_SEARCH_RANGE =  NextAlbumConfig.getCommonConfigInstance(false).sexPistolsTargetSearchRange.get().floatValue();
    public static final double ASSIST_SEARCH_RANGE =  NextAlbumConfig.getCommonConfigInstance(false).sexPistolsTransferAssistRange.get().floatValue();

    public static double getTargetSearchRange() {
        try {
            return NextAlbumConfig.getCommonConfigInstance(false).sexPistolsTargetSearchRange.get().floatValue();
        } catch (Throwable t) {
            return TARGET_SEARCH_RANGE;
        }
    }

    public static final double STAND_HIT_RADIUS = 0.75D;
    public static final double EXIT_OFFSET = 0.35D;

    public static final double SPEED_SCALE = 1.0D;
    public static final double SCOUT_RANGE = 18.0D;
    public static final double SCOUT_WIDTH = 4.0D;
    private static final double MIN_SPEED = 0.25D;

    private static final double PATH_OBSTRUCTION_PENALTY = 72.0D;
    private static final double RICOCHET_PENALTY = 18.0D;
    
    private static final double STAND_DEFENSE_INTERCEPT_PENALTY = 360.0D;
    private static final double STAND_DEFENSE_NEAR_PATH_PENALTY = 150.0D;
    private static final double STAND_DEFENSE_TARGET_GUARD_PENALTY = 120.0D;
    private static final double STAND_DEFENSE_RADIUS = 2.15D;
    private static final double STAND_DEFENSE_HARD_RADIUS = 1.25D;

    private SexPistolsBulletRedirectUtil() {
    }

    public static RedirectResult getRedirect(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d hitPos, Vector3d incomingMotion) {
        return getRedirect(world, owner, sourceStand, hitPos, incomingMotion, SexPistolsTargetMode.ALL, null);
    }

    public static RedirectResult getRedirect(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d hitPos, Vector3d incomingMotion, SexPistolsTargetMode targetMode) {
        return getRedirect(world, owner, sourceStand, hitPos, incomingMotion, targetMode, null);
    }

    public static RedirectResult getRedirect(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d hitPos, Vector3d incomingMotion, SexPistolsTargetMode targetMode, Entity lockedTarget) {
        return getRedirect(world, owner, sourceStand, hitPos, incomingMotion, targetMode, lockedTarget, SexPistolsTransferOrder.NONE, null);
    }

    public static RedirectResult getRedirect(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d hitPos, Vector3d incomingMotion, SexPistolsTargetMode targetMode, Entity lockedTarget, SexPistolsTransferOrder transferOrder, UUID lastTransferStandId) {
        return getRedirect(world, owner, sourceStand, hitPos, incomingMotion, targetMode, lockedTarget, transferOrder, lastTransferStandId, 1);
    }

    public static RedirectResult getRedirect(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d hitPos, Vector3d incomingMotion, SexPistolsTargetMode targetMode, Entity lockedTarget, SexPistolsTransferOrder transferOrder, UUID lastTransferStandId, int remainingRedirects) {
        double speed = Math.max(MIN_SPEED, incomingMotion.length() * SPEED_SCALE);
        int hops = Math.max(1, remainingRedirects);
        RedirectResult numberTransfer = findPriorityNumberTransferRedirect(world, owner, sourceStand, hitPos, speed, transferOrder, lastTransferStandId);
        if (numberTransfer != null) {
            return numberTransfer;
        }
        RedirectResult forced = findForcedAttackPath(world, owner, sourceStand, hitPos, incomingMotion, speed, hops);
        if (forced != null) {
            RedirectResult transfer = findTransferRedirect(world, owner, sourceStand, hitPos, incomingMotion, speed, forced, transferOrder, lastTransferStandId);
            return transfer != null ? transfer : forced;
        }
        if (targetMode == SexPistolsTargetMode.AIM && lockedTarget != null) {
            RedirectResult aim = findGuidedAttackPath(world, owner, sourceStand, hitPos, incomingMotion, speed, hops, guidedFromLocked(lockedTarget), 0.0D);
            if (aim != null) {
                RedirectResult transfer = findTransferRedirect(world, owner, sourceStand, hitPos, incomingMotion, speed, aim, transferOrder, lastTransferStandId);
                return transfer != null ? transfer : aim;
            }
        }
        SexPistolsTargetMode searchMode = targetMode == SexPistolsTargetMode.AIM && lockedTarget == null
                ? SexPistolsTargetMode.ALL : targetMode;
        RedirectResult best = findBestAttackPath(world, owner, sourceStand, hitPos, incomingMotion, searchMode, speed, lockedTarget);
        if (best == null) {
            best = findBestEffortAttack(world, owner, sourceStand, hitPos, incomingMotion, searchMode, speed, lockedTarget);
        }
        if (best != null) {
            RedirectResult transfer = findTransferRedirect(world, owner, sourceStand, hitPos, incomingMotion, speed, best, transferOrder, lastTransferStandId);
            return transfer != null ? transfer : best;
        }
        return getScoutRedirect(world, owner, sourceStand, hitPos, incomingMotion, speed);
    }

    public static RedirectResult getScoutRedirect(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d hitPos, Vector3d incomingMotion) {
        double speed = Math.max(MIN_SPEED, incomingMotion.length() * SPEED_SCALE);
        return getScoutRedirect(world, owner, sourceStand, hitPos, incomingMotion, speed);
    }

    private static RedirectResult getScoutRedirect(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d hitPos, Vector3d incomingMotion, double speed) {
        Vector3d direction = findLongestScoutDirection(world, sourceStand != null ? sourceStand : owner, hitPos, incomingMotion, owner.getLookAngle());
        if (direction.lengthSqr() <= 1.0E-6D) {
            direction = getFallbackDirection(owner, sourceStand, incomingMotion);
        }
        return result(hitPos, direction, speed, null, null, true);
    }

    private static RedirectResult findForcedAttackPath(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d origin, Vector3d incomingMotion, double speed, int remainingRedirects) {
        SexPistolsEntities sexPistols = IStandPower.getStandPowerOptional(owner).resolve().flatMap(SexPistolsStandType::getSexPistolsEntities).orElse(null);
        if (sexPistols == null) {
            return null;
        }
        List<GuidedAim> aims = new ArrayList<>();
        Vector3d incomingDir = incomingMotion.lengthSqr() > 1.0E-6D ? incomingMotion.normalize() : owner.getLookAngle();
        for (LivingEntity target : sexPistols.getValidForcedEntities()) {
            aims.add(new GuidedAim(target, getTargetAimPoints(origin, incomingDir, target), null));
        }
        for (BlockPos pos : sexPistols.getValidForcedBlocks()) {
            Vector3d center = Vector3d.atCenterOf(pos);
            aims.add(new GuidedAim(null, getBlockAimPoints(origin, center), pos));
        }
        return findGuidedAttackPath(world, owner, sourceStand, origin, incomingMotion, speed, remainingRedirects, aims, -10000.0D);
    }

    private static List<GuidedAim> guidedFromLocked(Entity lockedTarget) {
        List<GuidedAim> aims = new ArrayList<>();
        if (lockedTarget instanceof LivingEntity && lockedTarget.isAlive()) {
            LivingEntity living = (LivingEntity) lockedTarget;
            aims.add(new GuidedAim(living, java.util.Collections.singletonList(living.getBoundingBox().getCenter()), null));
        }
        return aims;
    }

    private static RedirectResult findGuidedAttackPath(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d origin, Vector3d incomingMotion, double speed, int remainingRedirects, List<GuidedAim> aims, double scoreOffset) {
        if (aims == null || aims.isEmpty()) {
            return null;
        }
        List<PathCandidate> candidates = new ArrayList<>();
        Entity ignored = sourceStand != null ? sourceStand : owner;
        Vector3d incomingDir = incomingMotion.lengthSqr() > 1.0E-6D ? incomingMotion.normalize() : owner.getLookAngle();
        List<SexPistolsEntity> relays = findGuidedRelays(world, owner, sourceStand, origin);
        int maxHops = Math.max(1, remainingRedirects);
        for (GuidedAim aim : aims) {
            for (Vector3d aimPoint : aim.aimPoints) {
                boolean direct = hasLineOfSight(world, ignored, origin, aimPoint, aim.allowBlock);
                addAttackCandidate(world, owner, sourceStand, ignored, origin, incomingDir, speed, aim.target, aimPoint, candidates, scoreOffset, aim.allowBlock);
                if (!direct) {
                    addGuidedMultiHopCandidates(world, owner, sourceStand, ignored, origin, incomingDir, speed, aim.target, aimPoint, aim.allowBlock, relays, maxHops, candidates, scoreOffset);
                    addGuidedDetourCandidates(world, owner, sourceStand, ignored, origin, incomingDir, speed, aim.target, aimPoint, aim.allowBlock, maxHops, candidates, scoreOffset);
                    addNaturalRicochetCandidate(world, owner, sourceStand, ignored, origin, incomingDir, speed, aim.target, aimPoint, aim.allowBlock, candidates, scoreOffset);
                }
            }
            addGuidedBestEffort(world, owner, sourceStand, ignored, origin, incomingDir, speed, aim, candidates, scoreOffset);
        }
        return candidates.stream().min(Comparator.comparingDouble(candidate -> candidate.score)).map(candidate -> candidate.result).orElse(null);
    }

    private static List<SexPistolsEntity> findGuidedRelays(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d origin) {
        AxisAlignedBB box = new AxisAlignedBB(origin, origin).inflate(SexPistolsAimUtil.AIM_MAX_DISTANCE);
        return world.getEntitiesOfClass(SexPistolsEntity.class, box,
                stand -> stand.isAlive() && stand != sourceStand && stand.getUser() == owner && SexPistolsStandUtil.isSexPistolsRemoteControlState(stand));
    }

    private static void addGuidedMultiHopCandidates(World world, LivingEntity owner, SexPistolsEntity sourceStand, Entity ignored,
            Vector3d origin, Vector3d incomingDir, double speed, LivingEntity target, Vector3d aimPoint, @Nullable BlockPos allowBlock,
            List<SexPistolsEntity> relays, int maxHops, List<PathCandidate> candidates, double scoreOffset) {
        if (relays.isEmpty() || maxHops < 1) {
            return;
        }
        for (SexPistolsEntity first : relays) {
            Vector3d firstPos = first.getBoundingBox().getCenter();
            Vector3d toFirst = firstPos.subtract(origin);
            if (toFirst.lengthSqr() <= 1.0E-6D || !hasLineOfSight(world, ignored, origin, firstPos)) {
                continue;
            }
            int remaining = maxHops - 1;
            if (!canReachAim(world, owner, first, firstPos, aimPoint, allowBlock, target, relays, remaining)) {
                continue;
            }
            int obstructions = countPathObstructions(world, owner, sourceStand, origin, firstPos, first);
            double defenseRisk = standDefenseRisk(world, owner, sourceStand, origin, firstPos, target);
            candidates.add(new PathCandidate(result(origin, toFirst, speed, target, first, false),
                    scorePath(origin, firstPos, incomingDir, 1, obstructions, defenseRisk) + firstPos.distanceTo(aimPoint) + scoreOffset));
        }
    }

    private static boolean canReachAim(World world, LivingEntity owner, SexPistolsEntity fromStand, Vector3d from, Vector3d aimPoint,
            @Nullable BlockPos allowBlock, LivingEntity target, List<SexPistolsEntity> relays, int remainingHops) {
        if (hasLineOfSight(world, fromStand != null ? fromStand : owner, from, aimPoint, allowBlock)) {
            return true;
        }
        if (remainingHops <= 0) {
            return false;
        }
        ArrayDeque<RelayHop> queue = new ArrayDeque<>();
        Set<UUID> visited = new HashSet<>();
        if (fromStand != null) {
            visited.add(fromStand.getUUID());
        }
        queue.add(new RelayHop(fromStand, from, remainingHops));
        while (!queue.isEmpty()) {
            RelayHop hop = queue.poll();
            if (hasLineOfSight(world, hop.stand != null ? hop.stand : owner, hop.pos, aimPoint, allowBlock)) {
                return true;
            }
            if (hop.remaining <= 0) {
                continue;
            }
            for (SexPistolsEntity next : relays) {
                if (visited.contains(next.getUUID())) {
                    continue;
                }
                Vector3d nextPos = next.getBoundingBox().getCenter();
                if (!hasLineOfSight(world, hop.stand != null ? hop.stand : owner, hop.pos, nextPos)) {
                    continue;
                }
                visited.add(next.getUUID());
                queue.add(new RelayHop(next, nextPos, hop.remaining - 1));
            }
        }
        return false;
    }

    private static void addGuidedDetourCandidates(World world, LivingEntity owner, SexPistolsEntity sourceStand, Entity ignored,
            Vector3d origin, Vector3d incomingDir, double speed, LivingEntity target, Vector3d aimPoint, @Nullable BlockPos allowBlock,
            int maxHops, List<PathCandidate> candidates, double scoreOffset) {
        if (maxHops < 2 || hasLineOfSight(world, ignored, origin, aimPoint, allowBlock)) {
            return;
        }
        for (Vector3d detour : sampleDetours(world, ignored, origin, aimPoint, allowBlock)) {
            Vector3d toDetour = detour.subtract(origin);
            if (toDetour.lengthSqr() <= 1.0E-6D || !hasLineOfSight(world, ignored, origin, detour)) {
                continue;
            }
            boolean reaches = hasLineOfSight(world, ignored, detour, aimPoint, allowBlock);
            int obstructions = countPathObstructions(world, owner, sourceStand, origin, detour, target)
                    + (reaches ? countPathObstructions(world, owner, sourceStand, detour, aimPoint, target) : 2);
            double defenseRisk = standDefenseRisk(world, owner, sourceStand, origin, detour, target);
            double score = scorePath(origin, detour, incomingDir, 1, obstructions, defenseRisk)
                    + detour.distanceTo(aimPoint)
                    + (reaches ? 0.0D : 80.0D)
                    + scoreOffset;
            candidates.add(new PathCandidate(result(origin, toDetour, speed, target, null, false), score));
        }
    }

    private static void addGuidedBestEffort(World world, LivingEntity owner, SexPistolsEntity sourceStand, Entity ignored,
            Vector3d origin, Vector3d incomingDir, double speed, GuidedAim aim, List<PathCandidate> candidates, double scoreOffset) {
        Vector3d aimPoint = aim.aimPoints.isEmpty() ? origin.add(incomingDir) : aim.aimPoints.get(0);
        Vector3d direct = aimPoint.subtract(origin);
        if (direct.lengthSqr() <= 1.0E-6D) {
            return;
        }
        Vector3d direction = pickClearDirectionToward(world, ignored, origin, direct.normalize(), aim.allowBlock);
        int obstructions = countPathObstructions(world, owner, sourceStand, origin, origin.add(direction.scale(8.0D)), aim.target);
        double defenseRisk = standDefenseRisk(world, owner, sourceStand, origin, origin.add(direction.scale(8.0D)), aim.target);
        RedirectResult result = aim.target != null
                ? attackResult(origin, direction, speed, aim.target)
                : result(origin, direction, speed, null, null, false);
        candidates.add(new PathCandidate(result, 2500.0D + scorePath(origin, origin.add(direction), incomingDir, 0, obstructions, defenseRisk) + scoreOffset));
    }

    private static List<Vector3d> sampleDetours(World world, Entity ignored, Vector3d origin, Vector3d aimPoint, @Nullable BlockPos allowBlock) {
        List<Vector3d> points = new ArrayList<>();
        RayTraceResult hit = clipPath(world, ignored, origin, aimPoint);
        if (!(hit instanceof BlockRayTraceResult) || hit.getType() != RayTraceResult.Type.BLOCK) {
            return points;
        }
        BlockRayTraceResult blockHit = (BlockRayTraceResult) hit;
        if (allowBlock != null && allowBlock.equals(blockHit.getBlockPos())) {
            return points;
        }
        Vector3d hitLoc = blockHit.getLocation();
        Vector3d forward = aimPoint.subtract(origin);
        if (forward.lengthSqr() <= 1.0E-6D) {
            return points;
        }
        forward = forward.normalize();
        Vector3d right = new Vector3d(-forward.z, 0.0D, forward.x);
        if (right.lengthSqr() <= 1.0E-6D) {
            right = new Vector3d(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        double[] offsets = {1.6D, 3.2D, 5.0D};
        for (double offset : offsets) {
            addOpenDetour(world, points, hitLoc.add(right.scale(offset)).add(0.0D, 0.4D, 0.0D));
            addOpenDetour(world, points, hitLoc.subtract(right.scale(offset)).add(0.0D, 0.4D, 0.0D));
            addOpenDetour(world, points, hitLoc.add(0.0D, offset, 0.0D));
            addOpenDetour(world, points, hitLoc.add(right.scale(offset * 0.7D)).add(0.0D, offset * 0.55D, 0.0D));
            addOpenDetour(world, points, hitLoc.subtract(right.scale(offset * 0.7D)).add(0.0D, offset * 0.55D, 0.0D));
        }
        BlockPos blocked = blockHit.getBlockPos();
        for (Direction direction : Direction.values()) {
            addOpenDetour(world, points, Vector3d.atCenterOf(blocked.relative(direction, 2)));
        }
        return points;
    }

    private static void addOpenDetour(World world, List<Vector3d> points, Vector3d point) {
        BlockPos pos = new BlockPos(point);
        if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
            return;
        }
        points.add(point);
    }

    private static Vector3d pickClearDirectionToward(World world, Entity ignored, Vector3d origin, Vector3d desired, @Nullable BlockPos allowBlock) {
        double best = clearDistance(world, ignored, origin, desired, SCOUT_RANGE);
        Vector3d bestDir = desired;
        Vector3d right = new Vector3d(-desired.z, 0.0D, desired.x);
        if (right.lengthSqr() > 1.0E-6D) {
            right = right.normalize();
            double[] yaws = {0.35D, -0.35D, 0.7D, -0.7D};
            for (double yaw : yaws) {
                Vector3d dir = desired.add(right.scale(yaw)).add(0.0D, 0.15D, 0.0D);
                if (dir.lengthSqr() <= 1.0E-6D) {
                    continue;
                }
                dir = dir.normalize();
                double clear = clearDistance(world, ignored, origin, dir, SCOUT_RANGE);
                if (clear > best + 0.35D) {
                    best = clear;
                    bestDir = dir;
                }
            }
        }
        Vector3d up = desired.add(0.0D, 0.45D, 0.0D).normalize();
        if (clearDistance(world, ignored, origin, up, SCOUT_RANGE) > best + 0.35D) {
            bestDir = up;
        }
        return bestDir;
    }

    private static void addAttackCandidate(World world, LivingEntity owner, SexPistolsEntity sourceStand, Entity ignored,
            Vector3d origin, Vector3d incomingDir, double speed, LivingEntity target, Vector3d aimPoint,
            List<PathCandidate> candidates, double scoreOffset) {
        addAttackCandidate(world, owner, sourceStand, ignored, origin, incomingDir, speed, target, aimPoint, candidates, scoreOffset, null);
    }

    private static void addAttackCandidate(World world, LivingEntity owner, SexPistolsEntity sourceStand, Entity ignored,
            Vector3d origin, Vector3d incomingDir, double speed, LivingEntity target, Vector3d aimPoint,
            List<PathCandidate> candidates, double scoreOffset, @Nullable BlockPos allowBlock) {
        Vector3d direct = aimPoint.subtract(origin);
        if (direct.lengthSqr() > 1.0E-6D && hasLineOfSight(world, ignored, origin, aimPoint, allowBlock)) {
            int obstructions = countPathObstructions(world, owner, sourceStand, origin, aimPoint, target);
            double defenseRisk = standDefenseRisk(world, owner, sourceStand, origin, aimPoint, target);
            RedirectResult result = target != null
                    ? attackResult(origin, direct, speed, target)
                    : result(origin, direct, speed, null, null, false);
            candidates.add(new PathCandidate(result, scorePath(origin, aimPoint, incomingDir, 0, obstructions, defenseRisk) + scoreOffset));
        }
    }

    private static List<Vector3d> getBlockAimPoints(Vector3d origin, Vector3d blockCenter) {
        List<Vector3d> points = new ArrayList<>();
        points.add(blockCenter);
        Vector3d toOrigin = origin.subtract(blockCenter);
        if (toOrigin.lengthSqr() > 1.0E-6D) {
            points.add(blockCenter.add(toOrigin.normalize().scale(0.49D)));
        }
        return points;
    }

    private static RedirectResult findBestAttackPath(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d origin, Vector3d incomingMotion, SexPistolsTargetMode targetMode, double speed, Entity lockedTarget) {
        List<LivingEntity> targets = getCandidateTargets(world, owner, origin, targetMode, lockedTarget);
        if (targets.isEmpty()) {
            return null;
        }
        List<PathCandidate> candidates = new ArrayList<>();
        Entity ignored = sourceStand != null ? sourceStand : owner;
        Vector3d incomingDir = incomingMotion.lengthSqr() > 1.0E-6D ? incomingMotion.normalize() : owner.getLookAngle();
        for (LivingEntity target : targets) {
            for (Vector3d aimPoint : getTargetAimPoints(origin, incomingDir, target)) {
                Vector3d direct = aimPoint.subtract(origin);
                boolean hasDirect = direct.lengthSqr() > 1.0E-6D && hasLineOfSight(world, ignored, origin, aimPoint);
                if (hasDirect) {
                    int obstructions = countPathObstructions(world, owner, sourceStand, origin, aimPoint, target);
                    double defenseRisk = standDefenseRisk(world, owner, sourceStand, origin, aimPoint, target);
                    candidates.add(new PathCandidate(attackResult(origin, direct, speed, target), scorePath(origin, aimPoint, incomingDir, 0, obstructions, defenseRisk)));
                }
                else {
                    addNaturalRicochetCandidate(world, owner, sourceStand, ignored, origin, incomingDir, speed, target, aimPoint, null, candidates, 0.0D);
                }
                if (!hasDirect) {
                    SexPistolsEntity relay = findBestRelay(world, owner, sourceStand, origin, target, aimPoint);
                    if (relay != null) {
                        Vector3d relayCenter = relay.getBoundingBox().getCenter();
                        Vector3d toRelay = relayCenter.subtract(origin);
                        if (toRelay.lengthSqr() > 1.0E-6D && hasLineOfSight(world, ignored, origin, relayCenter) && hasLineOfSight(world, relay, relayCenter, aimPoint)) {
                            int obstructions = countPathObstructions(world, owner, sourceStand, origin, relayCenter, relay) + countPathObstructions(world, owner, relay, relayCenter, aimPoint, target);
                            double defenseRisk = standDefenseRisk(world, owner, sourceStand, origin, relayCenter, target) + standDefenseRisk(world, owner, relay, relayCenter, aimPoint, target);
                            candidates.add(new PathCandidate(result(origin, toRelay, speed, target, relay, false), scorePath(origin, relayCenter, incomingDir, 1, obstructions, defenseRisk) + relayCenter.distanceTo(aimPoint)));
                        }
                    }
                }
            }
        }
        return candidates.stream().min(Comparator.comparingDouble(candidate -> candidate.score)).map(candidate -> candidate.result).orElse(null);
    }

    private static RedirectResult findBestEffortAttack(World world, LivingEntity owner, SexPistolsEntity sourceStand,
            Vector3d origin, Vector3d incomingMotion, SexPistolsTargetMode targetMode, double speed, Entity lockedTarget) {
        List<LivingEntity> targets = getCandidateTargets(world, owner, origin, targetMode, lockedTarget);
        if (targets.isEmpty()) {
            return null;
        }
        LivingEntity nearest = targets.get(0);
        Vector3d aimPoint = nearest.getBoundingBox().getCenter();
        Vector3d direct = aimPoint.subtract(origin);
        if (direct.lengthSqr() <= 1.0E-6D) {
            return null;
        }
        return attackResult(origin, direct, speed, nearest);
    }

    public static boolean hasVisibleTransferRelay(World world, LivingEntity owner, int sourcePistolIndex, Vector3d origin, SexPistolsTargetMode targetMode, Entity lockedTarget, SexPistolsTransferOrder transferOrder, UUID lastTransferStandId) {
        if (transferOrder == null || transferOrder == SexPistolsTransferOrder.NONE || sourcePistolIndex < 0) {
            return false;
        }
        AxisAlignedBB box = new AxisAlignedBB(origin, origin).inflate(SCOUT_RANGE);
        switch (transferOrder) {
        case DISTANCE:
            List<LivingEntity> targets = getCandidateTargets(world, owner, origin, targetMode, lockedTarget);
            if (targets.isEmpty()) {
                return false;
            }
            for (LivingEntity target : targets) {
                Vector3d targetPos = target.getBoundingBox().getCenter();
                double sourceDistance = origin.distanceToSqr(targetPos);
                List<SexPistolsEntity> relays = world.getEntitiesOfClass(SexPistolsEntity.class, box,
                        stand -> isTransferRelayCandidate(stand, owner, null, lastTransferStandId) && stand.getPistolIndex() != sourcePistolIndex);
                for (SexPistolsEntity relay : relays) {
                    Vector3d relayCenter = relay.getBoundingBox().getCenter();
                    if (relayCenter.distanceToSqr(targetPos) + 1.0E-4D < sourceDistance && hasLineOfSight(world, owner, origin, relayCenter)) {
                        return true;
                    }
                }
            }
            return false;
        case NUMBER:
            return world.getEntitiesOfClass(SexPistolsEntity.class, box,
                    stand -> isTransferRelayCandidate(stand, owner, null, lastTransferStandId) && stand.getPistolIndex() > sourcePistolIndex).stream()
                    .anyMatch(stand -> hasLineOfSight(world, owner, origin, stand.getBoundingBox().getCenter()));
        case NONE:
        default:
            return false;
        }
    }
    private static RedirectResult findPriorityNumberTransferRedirect(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d origin, double speed, SexPistolsTransferOrder transferOrder, UUID lastTransferStandId) {
        if (sourceStand == null || transferOrder != SexPistolsTransferOrder.NUMBER) {
            return null;
        }
        SexPistolsEntity relay = findNumberTransferRelay(world, owner, sourceStand, origin, lastTransferStandId);
        if (relay == null) {
            return null;
        }
        Vector3d relayCenter = relay.getBoundingBox().getCenter();
        Vector3d toRelay = relayCenter.subtract(origin);
        if (toRelay.lengthSqr() <= 1.0E-6D) {
            return null;
        }
        return result(origin, toRelay, speed, null, relay, false);
    }
    private static RedirectResult findTransferRedirect(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d origin, Vector3d incomingMotion, double speed, RedirectResult attackRedirect, SexPistolsTransferOrder transferOrder, UUID lastTransferStandId) {
        if (sourceStand == null || transferOrder != SexPistolsTransferOrder.DISTANCE || attackRedirect == null || attackRedirect.scouting) {
            return null;
        }
        SexPistolsEntity relay = findDistanceTransferRelay(world, owner, sourceStand, origin, attackRedirect, lastTransferStandId);
        if (relay == null) {
            return null;
        }
        Vector3d relayCenter = relay.getBoundingBox().getCenter();
        Vector3d toRelay = relayCenter.subtract(origin);
        if (toRelay.lengthSqr() <= 1.0E-6D) {
            return null;
        }
        return result(origin, toRelay, speed, attackRedirect.target, relay, false);
    }

    private static SexPistolsEntity findDistanceTransferRelay(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d origin, RedirectResult attackRedirect, UUID lastTransferStandId) {
        Vector3d targetPos = attackRedirect.target != null ? attackRedirect.target.getBoundingBox().getCenter() : attackRedirect.position.add(attackRedirect.motion);
        double sourceDistance = sourceStand.getBoundingBox().getCenter().distanceToSqr(targetPos);
        AxisAlignedBB box = new AxisAlignedBB(origin, origin).inflate(ASSIST_SEARCH_RANGE);
        Entity ignored = sourceStand != null ? sourceStand : owner;
        return world.getEntitiesOfClass(SexPistolsEntity.class, box,
                stand -> isTransferRelayCandidate(stand, owner, sourceStand, lastTransferStandId)).stream()
                .filter(stand -> stand.getBoundingBox().getCenter().distanceToSqr(targetPos) + 1.0E-4D < sourceDistance)
                .filter(stand -> hasLineOfSight(world, ignored, origin, stand.getBoundingBox().getCenter()))
                .min(Comparator.comparingDouble(stand -> stand.getBoundingBox().getCenter().distanceToSqr(targetPos)))
                .orElse(null);
    }

    private static SexPistolsEntity findNumberTransferRelay(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d origin, UUID lastTransferStandId) {
        int sourceIndex = sourceStand.getPistolIndex();
        AxisAlignedBB box = new AxisAlignedBB(origin, origin).inflate(ASSIST_SEARCH_RANGE);
        Entity ignored = sourceStand != null ? sourceStand : owner;
        return world.getEntitiesOfClass(SexPistolsEntity.class, box,
                stand -> isTransferRelayCandidate(stand, owner, sourceStand, lastTransferStandId) && stand.getPistolIndex() > sourceIndex).stream()
                .filter(stand -> hasLineOfSight(world, ignored, origin, stand.getBoundingBox().getCenter()))
                .min(Comparator.comparingInt((SexPistolsEntity stand) -> stand.getPistolIndex() - sourceIndex)
                        .thenComparingDouble(stand -> stand.distanceToSqr(origin.x, origin.y, origin.z)))
                .orElse(null);
    }

    private static boolean isTransferRelayCandidate(SexPistolsEntity stand, LivingEntity owner, SexPistolsEntity sourceStand, UUID lastTransferStandId) {
        return stand != null && stand.isAlive() && stand != sourceStand && stand.getUser() == owner && SexPistolsStandUtil.isSexPistolsRemoteControlState(stand) && (lastTransferStandId == null || !stand.getUUID().equals(lastTransferStandId));
    }

    private static List<Vector3d> getTargetAimPoints(Vector3d origin, Vector3d incomingDir, LivingEntity target) {
        List<Vector3d> points = new ArrayList<>();
        Vector3d center = target.getBoundingBox().getCenter();
        points.add(center);
        Vector3d toTarget = center.subtract(origin);
        if (toTarget.lengthSqr() <= 1.0E-6D) {
            toTarget = incomingDir.lengthSqr() > 1.0E-6D ? incomingDir : new Vector3d(0.0D, 0.0D, 1.0D);
        }
        Vector3d horizontal = new Vector3d(toTarget.x, 0.0D, toTarget.z);
        if (horizontal.lengthSqr() <= 1.0E-6D) {
            horizontal = new Vector3d(incomingDir.x, 0.0D, incomingDir.z);
        }
        if (horizontal.lengthSqr() <= 1.0E-6D) {
            horizontal = new Vector3d(0.0D, 0.0D, 1.0D);
        }
        horizontal = horizontal.normalize();
        Vector3d side = new Vector3d(-horizontal.z, 0.0D, horizontal.x).normalize();
        double sideOffset = Math.max(0.18D, Math.min(0.55D, target.getBbWidth() * 0.42D));
        double verticalOffset = Math.max(0.08D, Math.min(0.35D, target.getBbHeight() * 0.18D));
        points.add(center.add(side.scale(sideOffset)));
        points.add(center.subtract(side.scale(sideOffset)));
        points.add(center.add(side.scale(sideOffset * 0.65D)).add(0.0D, verticalOffset, 0.0D));
        points.add(center.subtract(side.scale(sideOffset * 0.65D)).add(0.0D, verticalOffset, 0.0D));
        points.add(center.add(0.0D, verticalOffset, 0.0D));
        return points;
    }

    private static List<LivingEntity> getCandidateTargets(World world, LivingEntity owner, Vector3d origin, SexPistolsTargetMode targetMode, Entity lockedTarget) {
        List<LivingEntity> targets = new ArrayList<>();
        if (lockedTarget instanceof LivingEntity && lockedTarget.isAlive()) {
            LivingEntity lockedLiving = (LivingEntity) lockedTarget;
            double lockRange = targetMode == SexPistolsTargetMode.AIM ? SexPistolsAimUtil.AIM_MAX_DISTANCE : getTargetSearchRange();
            SexPistolsTargetMode lockMode = targetMode == SexPistolsTargetMode.AIM ? SexPistolsTargetMode.ALL : targetMode;
            if (lockedLiving.distanceToSqr(origin.x, origin.y, origin.z) <= lockRange * lockRange && isValidTarget(owner, lockedLiving, lockMode)) {
                targets.add(lockedLiving);
            }
        }
        if (targetMode == SexPistolsTargetMode.AIM && !targets.isEmpty()) {
            return targets;
        }
        SexPistolsTargetMode searchMode = targetMode == SexPistolsTargetMode.AIM ? SexPistolsTargetMode.ALL : targetMode;
        for (LivingEntity target : findTargets(world, owner, origin, getTargetSearchRange(), searchMode)) {
            if (!targets.contains(target)) {
                targets.add(target);
            }
        }
        return targets;
    }

    private static double scorePath(Vector3d origin, Vector3d nextPoint, Vector3d incomingDir, int ricochets, int obstructions, double defenseRisk) {
        Vector3d direction = nextPoint.subtract(origin);
        double distance = direction.length();
        double turnPenalty = 0.0D;
        if (direction.lengthSqr() > 1.0E-6D && incomingDir.lengthSqr() > 1.0E-6D) {
            turnPenalty = (1.0D - Math.max(-1.0D, Math.min(1.0D, direction.normalize().dot(incomingDir)))) * 8.0D;
        }
        return distance + turnPenalty + ricochets * RICOCHET_PENALTY + obstructions * PATH_OBSTRUCTION_PENALTY + defenseRisk;
    }

    private static RedirectResult result(Vector3d hitPos, Vector3d direction, double speed, Entity target, SexPistolsEntity relay, boolean scouting) {
        Vector3d normalized = direction.normalize();
        return new RedirectResult(hitPos.add(normalized.scale(EXIT_OFFSET)), normalized.scale(speed), target, relay, scouting);
    }

    private static RedirectResult attackResult(Vector3d hitPos, Vector3d direction, double speed, LivingEntity target) {
        Vector3d normalized = direction.normalize();
        double offset = getSafeAttackExitOffset(hitPos, normalized, target);
        return new RedirectResult(hitPos.add(normalized.scale(offset)), normalized.scale(speed), target, null, false);
    }

    private static double getSafeAttackExitOffset(Vector3d hitPos, Vector3d direction, LivingEntity target) {
        if (target == null || direction.lengthSqr() <= 1.0E-6D) {
            return EXIT_OFFSET;
        }
        AxisAlignedBB targetBox = target.getBoundingBox().inflate(0.08D);
        double distanceToTarget = targetBox.clip(hitPos, hitPos.add(direction.scale(Math.max(EXIT_OFFSET + 2.0D, target.distanceToSqr(hitPos.x, hitPos.y, hitPos.z) + 2.0D))))
                .map(hitPos::distanceTo)
                .orElse(Double.POSITIVE_INFINITY);
        if (!Double.isFinite(distanceToTarget)) {
            return EXIT_OFFSET;
        }
        return Math.max(0.02D, Math.min(EXIT_OFFSET, distanceToTarget * 0.45D));
    }

    public static SexPistolsEntity findOwnSexPistolsHit(World world, LivingEntity owner, Vector3d start, Vector3d end) {
        return findOwnSexPistolsHit(world, owner, start, end, null);
    }

    public static SexPistolsEntity findOwnSexPistolsHit(World world, LivingEntity owner, Vector3d start, Vector3d end, UUID ignoredStandId) {
        AxisAlignedBB searchBox = new AxisAlignedBB(start, end).inflate(STAND_HIT_RADIUS);
        List<SexPistolsEntity> stands = world.getEntitiesOfClass(SexPistolsEntity.class, searchBox,
                stand -> stand.isAlive() && stand.getUser() == owner && SexPistolsStandUtil.isSexPistolsRemoteControlState(stand) && (ignoredStandId == null || !stand.getUUID().equals(ignoredStandId)));
        return stands.stream()
                .filter(stand -> segmentIntersectsEntity(start, end, stand, STAND_HIT_RADIUS))
                .min(Comparator.comparingDouble(stand -> stand.distanceToSqr(start.x, start.y, start.z)))
                .orElse(null);
    }

    public static Entity findBestTarget(World world, LivingEntity owner, Vector3d origin, double range, SexPistolsTargetMode targetMode) {
        return findTargets(world, owner, origin, range, targetMode).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin.x, origin.y, origin.z)))
                .orElse(null);
    }

    public static List<Entity> findVisibleScoutEntities(World world, LivingEntity owner, Entity ignored, Vector3d origin, Vector3d motion, double range) {
        List<Entity> visible = new ArrayList<>();
        if (motion.lengthSqr() <= 1.0E-6D) {
            return visible;
        }
        Vector3d end = origin.add(motion.normalize().scale(range));
        AxisAlignedBB box = new AxisAlignedBB(origin, end).inflate(SCOUT_WIDTH);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box, entity -> entity.isAlive() && entity != owner && entity != ignored && !(entity instanceof SexPistolsEntity && ((SexPistolsEntity) entity).getUser() == owner));
        for (LivingEntity entity : entities) {
            Vector3d center = entity.getBoundingBox().getCenter();
            if (distanceToSegmentSqr(center, origin, end) <= SCOUT_WIDTH * SCOUT_WIDTH && hasLineOfSight(world, ignored, origin, center)) {
                visible.add(entity);
            }
        }
        return visible;
    }

    private static List<LivingEntity> findTargets(World world, LivingEntity owner, Vector3d origin, double range, SexPistolsTargetMode targetMode) {
        AxisAlignedBB box = new AxisAlignedBB(origin, origin).inflate(range);
        List<LivingEntity> targets = new ArrayList<>();
        targets.addAll(world.getEntitiesOfClass(LivingEntity.class, box, entity -> isValidTarget(owner, entity, targetMode)));
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin.x, origin.y, origin.z)));
        return targets;
    }

    public static boolean isValidTarget(LivingEntity owner, LivingEntity entity, SexPistolsTargetMode targetMode) {
        if (owner == null || entity == null || !entity.isAlive() || entity == owner) {
            return false;
        }
        if (entity instanceof StandEntity) {
            return false;
        }
        if (!targetMode.matches(owner, entity)) {
            return false;
        }
        if (entity instanceof SexPistolsEntity) {
            LivingEntity pistolUser = ((SexPistolsEntity) entity).getUser();
            if (pistolUser != null && (pistolUser == owner || pistolUser.getUUID().equals(owner.getUUID()))) {
                return false;
            }
        }
        return true;
    }

    private static SexPistolsEntity findBestRelay(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d origin, Entity target, Vector3d aimPoint) {
        return findBestRelay(world, owner, sourceStand, origin, target, aimPoint, null);
    }

    private static SexPistolsEntity findBestRelay(World world, LivingEntity owner, SexPistolsEntity sourceStand, Vector3d origin, Entity target, Vector3d aimPoint, @Nullable BlockPos allowBlock) {
        AxisAlignedBB box = new AxisAlignedBB(origin, origin).inflate(ASSIST_SEARCH_RANGE);
        return world.getEntitiesOfClass(SexPistolsEntity.class, box,
                stand -> stand.isAlive() && stand != sourceStand && stand.getUser() == owner && SexPistolsStandUtil.isSexPistolsRemoteControlState(stand)).stream()
                .filter(stand -> hasLineOfSight(world, stand, stand.getBoundingBox().getCenter(), aimPoint, allowBlock))
                .min(Comparator.comparingDouble(stand -> stand.distanceToSqr(origin.x, origin.y, origin.z) + standDefenseRisk(world, owner, stand, stand.getBoundingBox().getCenter(), aimPoint, target)))
                .orElse(null);
    }

    private static double standDefenseRisk(World world, LivingEntity owner, Entity ignored, Vector3d start, Vector3d end, Entity target) {
        AxisAlignedBB box = new AxisAlignedBB(start, end).inflate(STAND_DEFENSE_RADIUS);
        List<StandEntity> stands = world.getEntitiesOfClass(StandEntity.class, box, stand -> isEnemyDefensiveStand(owner, ignored, stand));
        if (stands.isEmpty()) {
            return 0.0D;
        }
        double risk = 0.0D;
        for (StandEntity stand : stands) {
            Vector3d center = stand.getBoundingBox().getCenter();
            double distanceSqr = distanceToSegmentSqr(center, start, end);
            if (segmentIntersectsEntity(start, end, stand, STAND_DEFENSE_HARD_RADIUS)) {
                risk += STAND_DEFENSE_INTERCEPT_PENALTY;
            }
            else if (distanceSqr <= STAND_DEFENSE_RADIUS * STAND_DEFENSE_RADIUS) {
                double proximity = 1.0D - Math.min(1.0D, Math.sqrt(distanceSqr) / STAND_DEFENSE_RADIUS);
                risk += STAND_DEFENSE_NEAR_PATH_PENALTY * (0.35D + proximity);
            }
            LivingEntity standUser = stand.getUser();
            if (standUser != null && target != null && (standUser == target || standUser.is(target) || standUser.isAlliedTo(target))) {
                risk += STAND_DEFENSE_TARGET_GUARD_PENALTY;
            }
        }
        return risk;
    }

    private static boolean isEnemyDefensiveStand(LivingEntity owner, Entity ignored, StandEntity stand) {
        if (stand == null || !stand.isAlive() || stand == ignored || stand instanceof SexPistolsEntity && ((SexPistolsEntity) stand).getUser() == owner) {
            return false;
        }
        LivingEntity standUser = stand.getUser();
        return standUser != null && standUser != owner && !standUser.isAlliedTo(owner);
    }

    private static Vector3d findLongestScoutDirection(World world, Entity ignored, Vector3d origin, Vector3d incomingMotion, Vector3d ownerLook) {
        List<Vector3d> directions = new ArrayList<>();
        if (incomingMotion.lengthSqr() > 1.0E-6D) {
            Vector3d forward = incomingMotion.normalize();
            directions.add(forward);
            Vector3d right = new Vector3d(-forward.z, 0.0D, forward.x);
            if (right.lengthSqr() > 1.0E-6D) {
                right = right.normalize();
                directions.add(forward.add(right.scale(0.45D)));
                directions.add(forward.subtract(right.scale(0.45D)));
            }
            directions.add(forward.add(0.0D, 0.35D, 0.0D));
            directions.add(forward.add(0.0D, -0.20D, 0.0D));
        }
        if (ownerLook.lengthSqr() > 1.0E-6D) {
            directions.add(ownerLook.normalize());
        }
        return directions.stream()
                .filter(direction -> direction.lengthSqr() > 1.0E-6D)
                .max(Comparator.comparingDouble(direction -> clearDistance(world, ignored, origin, direction.normalize(), SCOUT_RANGE)))
                .orElse(Vector3d.ZERO)
                .normalize();
    }

    private static double clearDistance(World world, Entity ignored, Vector3d origin, Vector3d direction, double range) {
        Vector3d end = origin.add(direction.scale(range));
        RayTraceResult result = world.clip(new RayTraceContext(origin, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, ignored));
        if (result.getType() == RayTraceResult.Type.BLOCK) {
            return result.getLocation().distanceTo(origin);
        }
        return range;
    }

    private static int countPathObstructions(World world, LivingEntity owner, Entity ignored, Vector3d start, Vector3d end, Entity target) {
        AxisAlignedBB box = new AxisAlignedBB(start, end).inflate(0.45D);
        List<Entity> entities = world.getEntitiesOfClass(Entity.class, box, entity -> entity.isAlive() && entity != owner && entity != ignored && entity != target && !(entity instanceof SexPistolsEntity && ((SexPistolsEntity) entity).getUser() == owner) && !(entity instanceof StandEntity));
        int count = 0;
        RayTraceResult clip = clipPath(world, ignored, start, end);
        if (clip.getType() == RayTraceResult.Type.BLOCK) {
            count++;
        }
        for (Entity entity : entities) {
            if (segmentIntersectsEntity(start, end, entity, 0.25D)) {
                count++;
            }
        }
        return count;
    }

    private static boolean segmentIntersectsEntity(Vector3d start, Vector3d end, Entity entity, double inflate) {
        EntityRayTraceResult result = entity.getBoundingBox().inflate(inflate).clip(start, end)
                .map(hit -> new EntityRayTraceResult(entity, hit))
                .orElse(null);
        return result != null;
    }

    private static double distanceToSegmentSqr(Vector3d point, Vector3d start, Vector3d end) {
        Vector3d segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr <= 1.0E-6D) {
            return point.distanceToSqr(start);
        }
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(segment) / lengthSqr));
        Vector3d projection = start.add(segment.scale(t));
        return point.distanceToSqr(projection);
    }

    private static Vector3d getFallbackDirection(LivingEntity owner, SexPistolsEntity sourceStand, Vector3d incomingMotion) {
        if (sourceStand != null) {
            Vector3d standLook = sourceStand.getLookAngle();
            if (standLook.lengthSqr() > 1.0E-6D) {
                return standLook;
            }
        }
        Vector3d ownerLook = owner.getLookAngle();
        if (ownerLook.lengthSqr() > 1.0E-6D) {
            return ownerLook;
        }
        if (incomingMotion.lengthSqr() > 1.0E-6D) {
            return incomingMotion;
        }
        return new Vector3d(0.0D, 0.0D, 1.0D);
    }

    private static RayTraceResult clipPath(World world, Entity ignored, Vector3d start, Vector3d end) {
        return world.clip(new RayTraceContext(start, end,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                ignored));
    }

    private static void addNaturalRicochetCandidate(World world, LivingEntity owner, SexPistolsEntity sourceStand, Entity ignored,
            Vector3d origin, Vector3d incomingDir, double speed, LivingEntity target, Vector3d aimPoint, @Nullable BlockPos allowBlock,
            List<PathCandidate> candidates, double scoreOffset) {
        RayTraceResult hit = clipPath(world, ignored, origin, aimPoint);
        if (!(hit instanceof BlockRayTraceResult) || hit.getType() != RayTraceResult.Type.BLOCK) {
            return;
        }
        BlockRayTraceResult blockHit = (BlockRayTraceResult) hit;
        if (allowBlock != null && allowBlock.equals(blockHit.getBlockPos())) {
            return;
        }
        Vector3d bouncePos = blockHit.getLocation().add(Vector3d.atLowerCornerOf(blockHit.getDirection().getNormal()).scale(0.06D));
        Vector3d toWall = bouncePos.subtract(origin);
        if (toWall.lengthSqr() <= 1.0E-6D) {
            return;
        }
        Vector3d toWallDir = toWall.normalize();
        if (incomingDir.lengthSqr() > 1.0E-6D && toWallDir.dot(incomingDir) < 0.12D) {
            return;
        }
        Vector3d reflected = reflectDirection(incomingDir.lengthSqr() > 1.0E-6D ? incomingDir : toWallDir, blockHit.getDirection());
        Vector3d afterBounce = bouncePos.add(reflected.scale(Math.max(2.0D, bouncePos.distanceTo(aimPoint))));
        if (!hasLineOfSight(world, ignored, bouncePos, aimPoint, allowBlock)
                && clipPath(world, ignored, bouncePos, afterBounce).getType() == RayTraceResult.Type.BLOCK) {
            return;
        }
        int obstructions = countPathObstructions(world, owner, sourceStand, origin, bouncePos, target);
        double defenseRisk = standDefenseRisk(world, owner, sourceStand, origin, bouncePos, target);
        double naturalBonus = hasLineOfSight(world, ignored, bouncePos, aimPoint, allowBlock) ? -6.0D : 4.0D;
        candidates.add(new PathCandidate(result(origin, toWall, speed, target, null, false),
                scorePath(origin, bouncePos, incomingDir, 1, obstructions, defenseRisk) + bouncePos.distanceTo(aimPoint) * 0.55D + naturalBonus + scoreOffset));
    }

    private static Vector3d reflectDirection(Vector3d incoming, Direction face) {
        Vector3d normal = Vector3d.atLowerCornerOf(face.getNormal()).normalize();
        Vector3d dir = incoming.lengthSqr() > 1.0E-6D ? incoming.normalize() : normal.reverse();
        return dir.subtract(normal.scale(2.0D * dir.dot(normal))).normalize();
    }

    private static boolean hasLineOfSight(World world, Entity ignored, Vector3d start, Vector3d end) {
        return hasLineOfSight(world, ignored, start, end, null);
    }

    private static boolean hasLineOfSight(World world, Entity ignored, Vector3d start, Vector3d end, @Nullable BlockPos allowBlock) {
        RayTraceResult result = world.clip(new RayTraceContext(start, end,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                ignored));
        if (result.getType() == RayTraceResult.Type.MISS) {
            return true;
        }
        return allowBlock != null
                && result.getType() == RayTraceResult.Type.BLOCK
                && allowBlock.equals(((BlockRayTraceResult) result).getBlockPos());
    }

    private static final class GuidedAim {
        private final LivingEntity target;
        private final List<Vector3d> aimPoints;
        private final BlockPos allowBlock;

        private GuidedAim(LivingEntity target, List<Vector3d> aimPoints, BlockPos allowBlock) {
            this.target = target;
            this.aimPoints = aimPoints;
            this.allowBlock = allowBlock;
        }
    }

    private static final class RelayHop {
        private final SexPistolsEntity stand;
        private final Vector3d pos;
        private final int remaining;

        private RelayHop(SexPistolsEntity stand, Vector3d pos, int remaining) {
            this.stand = stand;
            this.pos = pos;
            this.remaining = remaining;
        }
    }

    private static class PathCandidate {
        private final RedirectResult result;
        private final double score;

        private PathCandidate(RedirectResult result, double score) {
            this.result = result;
            this.score = score;
        }
    }

    public static class RedirectResult {
        public final Vector3d position;
        public final Vector3d motion;
        public final Entity target;
        public final SexPistolsEntity relay;
        public final boolean scouting;

        public RedirectResult(Vector3d position, Vector3d motion, Entity target, SexPistolsEntity relay) {
            this(position, motion, target, relay, false);
        }

        public RedirectResult(Vector3d position, Vector3d motion, Entity target, SexPistolsEntity relay, boolean scouting) {
            this.position = position;
            this.motion = motion;
            this.target = target;
            this.relay = relay;
            this.scouting = scouting;
        }
    }
}