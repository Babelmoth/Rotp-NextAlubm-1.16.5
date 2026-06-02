package com.nextalubm.rotp_nextalbum.util;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

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