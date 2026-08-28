package com.nextalubm.rotp_nextalbum.util;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.stand.BeachBoySettingsEffect;

import net.minecraft.entity.LivingEntity;

public final class BeachBoySettings {
    private BeachBoySettings() {
    }

    public static boolean isBeachBoy(@Nullable IStandPower power) {
        return power != null && power.hasPower() && power.getType() == InitStands.STAND_BEACH_BOY.get();
    }

    public static void ensure(LivingEntity user) {
        IStandPower power = power(user);
        if (power != null) {
            BeachBoySettingsEffect.getOrCreate(power);
        }
    }

    public static boolean isPathLine(@Nullable IStandPower power) {
        if (!isBeachBoy(power)) {
            return false;
        }
        BeachBoySettingsEffect effect = BeachBoySettingsEffect.get(power).orElse(null);
        if (effect != null) {
            return effect.isPathLine();
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(power);
        return stand != null && stand.hasLineBlockCollision();
    }

    public static boolean isCrushDamage(@Nullable IStandPower power) {
        if (!isBeachBoy(power)) {
            return false;
        }
        BeachBoySettingsEffect effect = BeachBoySettingsEffect.get(power).orElse(null);
        if (effect != null) {
            return effect.isCrushDamage();
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(power);
        return stand == null || stand.isCrushDamageEnabled();
    }

    public static boolean isSoundSense(@Nullable IStandPower power) {
        if (!isBeachBoy(power)) {
            return false;
        }
        BeachBoySettingsEffect effect = BeachBoySettingsEffect.get(power).orElse(null);
        if (effect != null) {
            return effect.isSoundSense();
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(power);
        return stand == null || stand.isSoundSenseEnabled();
    }

    public static float getMaxLength(@Nullable IStandPower power) {
        BeachBoySettingsEffect effect = BeachBoySettingsEffect.get(power).orElse(null);
        if (effect != null) {
            return effect.getMaxLength();
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(power);
        return stand != null ? stand.getMaxLineLength() : BeachBoyLine.DEFAULT_LENGTH;
    }

    public static void togglePathLine(LivingEntity user) {
        BeachBoySettingsEffect effect = effectForToggle(user);
        if (effect != null) {
            effect.setPathLine(!effect.isPathLine());
        }
    }

    public static void toggleCrushDamage(LivingEntity user) {
        BeachBoySettingsEffect effect = effectForToggle(user);
        if (effect != null) {
            effect.setCrushDamage(!effect.isCrushDamage());
        }
    }

    public static void toggleSoundSense(LivingEntity user) {
        BeachBoySettingsEffect effect = effectForToggle(user);
        if (effect != null) {
            effect.setSoundSense(!effect.isSoundSense());
        }
    }

    public static void setMaxLength(LivingEntity user, float length) {
        BeachBoySettingsEffect effect = effectForToggle(user);
        if (effect != null) {
            effect.setMaxLength(length);
        }
    }

    @Nullable
    private static BeachBoySettingsEffect effectForToggle(LivingEntity user) {
        IStandPower power = power(user);
        if (power == null) {
            return null;
        }
        if (user.level != null && user.level.isClientSide()) {
            return BeachBoySettingsEffect.get(power).orElse(null);
        }
        return BeachBoySettingsEffect.getOrCreate(power);
    }

    @Nullable
    private static IStandPower power(LivingEntity user) {
        if (user == null) {
            return null;
        }
        IStandPower standPower = IStandPower.getStandPowerOptional(user).orElse(null);
        return isBeachBoy(standPower) ? standPower : null;
    }
}
