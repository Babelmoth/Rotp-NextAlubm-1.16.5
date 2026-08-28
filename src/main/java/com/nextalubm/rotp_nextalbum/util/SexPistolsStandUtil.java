package com.nextalubm.rotp_nextalbum.util;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mc.MCUtil;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;

public final class SexPistolsStandUtil {
    private SexPistolsStandUtil() {
    }

    public static boolean isSexPistolsUser(Entity entity) {
        return entity instanceof LivingEntity && isSexPistolsUser((LivingEntity) entity);
    }

    public static boolean isSexPistolsUser(LivingEntity entity) {
        return IStandPower.getStandPowerOptional(entity)
                .map(power -> power.hasPower() && power.getType() == InitStands.STAND_SEX_PISTOLS.get())
                .orElse(false);
    }

    public static LivingEntity getLookSource(LivingEntity user, IStandPower power) {
        if (power != null && power.getStandManifestation() instanceof StandEntity) {
            StandEntity stand = (StandEntity) power.getStandManifestation();
            if (stand.isManuallyControlled()) {
                return stand;
            }
        }
        return user;
    }

    public static LivingEntity getAimLookSource(LivingEntity user, IStandPower power) {
        SexPistolsEntity stay = findLowestNumberStayStand(user);
        return stay != null ? stay : getLookSource(user, power);
    }

    public static SexPistolsEntity findLowestNumberStayStand(LivingEntity user) {
        if (user == null || user.level == null) {
            return null;
        }
        SexPistolsEntity best = null;
        int bestIndex = Integer.MAX_VALUE;
        AxisAlignedBB box = user.getBoundingBox().inflate(SexPistolsAimUtil.AIM_MAX_DISTANCE);
        for (SexPistolsEntity stand : user.level.getEntitiesOfClass(SexPistolsEntity.class, box,
                candidate -> candidate.isAlive()
                        && candidate.getUser() == user
                        && candidate.isRemotePositionFixed())) {
            if (stand.getPistolIndex() < bestIndex) {
                bestIndex = stand.getPistolIndex();
                best = stand;
            }
        }
        return best;
    }

    public static ActionTarget resolveLookTarget(LivingEntity user, IStandPower power, double range) {
        LivingEntity lookSource = getLookSource(user, power);
        RayTraceResult ray = JojoModUtil.rayTrace(lookSource, Math.max(range, MCUtil.getPickRange(lookSource)),
                entity -> entity != null
                        && entity.isAlive()
                        && !entity.is(lookSource)
                        && !entity.is(user)
                        && !(entity instanceof StandEntity && user.equals(((StandEntity) entity).getUser())),
                0.15D);
        return ray == null ? ActionTarget.EMPTY : ActionTarget.fromRayTraceResult(ray);
    }

    public static ActionTarget resolveForcedLookTarget(LivingEntity user, IStandPower power) {
        ActionTarget ray = resolveLookTarget(user, power, SexPistolsAimUtil.AIM_MAX_DISTANCE);
        if (ray.getType() == ActionTarget.TargetType.ENTITY) {
            return ray;
        }
        LivingEntity aim = SexPistolsAimUtil.findAimTarget(getLookSource(user, power), user);
        if (aim != null) {
            return new ActionTarget(aim);
        }
        return ray;
    }

    public static boolean isSexPistolsRemoteControlState(StandEntity stand) {
        return stand != null && (stand.isManuallyControlled() || stand.isRemotePositionFixed());
    }

    public static boolean canProjectileHitStand(Entity projectile, LivingEntity owner, StandEntity stand) {
        if (projectile == null || owner == null || stand == null || !stand.isAlive()) {
            return false;
        }
        LivingEntity standUser = stand.getUser();
        return standUser != null
                && standUser != owner
                && stand != projectile
                && standUser != projectile;
    }
}