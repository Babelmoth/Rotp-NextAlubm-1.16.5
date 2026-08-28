package com.nextalubm.rotp_nextalbum.util;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class SexPistolsAimUtil {
    public static final double AIM_MAX_DISTANCE = 128.0D;
    public static final double AIM_FOV_HALF_ANGLE_DEG = 50.0D;
    private static final double AIM_FOV_MIN_DOT = Math.cos(Math.toRadians(AIM_FOV_HALF_ANGLE_DEG));

    private SexPistolsAimUtil() {
    }

    @Nullable
    public static LivingEntity findAimTarget(LivingEntity user, IStandPower power) {
        return findAimTarget(SexPistolsStandUtil.getAimLookSource(user, power), user);
    }

    @Nullable
    public static LivingEntity findAimTarget(LivingEntity lookSource, LivingEntity owner) {
        if (lookSource == null || lookSource.level == null) {
            return null;
        }
        World world = lookSource.level;
        Vector3d eye = lookSource.getEyePosition(1.0F);
        Vector3d look = lookSource.getLookAngle();
        if (look.lengthSqr() <= 1.0E-6D) {
            return null;
        }
        look = look.normalize();
        AxisAlignedBB box = lookSource.getBoundingBox().inflate(AIM_MAX_DISTANCE);
        LivingEntity best = null;
        double bestDot = AIM_FOV_MIN_DOT;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, box, candidate -> isAimCandidate(owner, lookSource, candidate))) {
            Vector3d center = entity.getBoundingBox().getCenter();
            Vector3d toEntity = center.subtract(eye);
            double dist = toEntity.length();
            if (dist < 0.4D || dist > AIM_MAX_DISTANCE) {
                continue;
            }
            double dot = toEntity.scale(1.0D / dist).dot(look);
            if (dot < AIM_FOV_MIN_DOT) {
                continue;
            }
            if (!hasLineOfSight(world, lookSource, eye, center)) {
                continue;
            }
            if (dot > bestDot + 1.0E-4D || (Math.abs(dot - bestDot) <= 1.0E-4D && dist < bestDist)) {
                bestDot = dot;
                bestDist = dist;
                best = entity;
            }
        }
        return best;
    }

    private static boolean isAimCandidate(LivingEntity owner, LivingEntity lookSource, LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity == owner || entity == lookSource) {
            return false;
        }
        if (entity instanceof StandEntity) {
            return false;
        }
        if (entity instanceof PlayerEntity && ((PlayerEntity) entity).isSpectator()) {
            return false;
        }
        return !(entity instanceof com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity
                && ((com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity) entity).getUser() == owner);
    }

    private static boolean hasLineOfSight(World world, LivingEntity lookSource, Vector3d start, Vector3d end) {
        RayTraceResult result = world.clip(new RayTraceContext(start, end,
                RayTraceContext.BlockMode.VISUAL, RayTraceContext.FluidMode.NONE, lookSource));
        return result.getType() == RayTraceResult.Type.MISS;
    }
}
