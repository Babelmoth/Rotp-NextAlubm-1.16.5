package com.nextalubm.rotp_nextalbum.util;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;

import net.minecraft.entity.LivingEntity;

public final class StandSummonUtil {
    private StandSummonUtil() {
    }

    public static boolean ensureSummoned(@Nullable LivingEntity user, @Nullable IStandPower power) {
        if (power == null || !power.hasPower()) {
            return false;
        }
        if (power.isActive()) {
            return true;
        }
        if (user != null && !user.level.isClientSide()) {
            power.getType().summon(user, power, true);
        }
        return power.isActive();
    }
}
